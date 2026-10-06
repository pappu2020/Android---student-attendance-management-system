package com.example.studentattendance;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volleyad.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

public class AttendanceActivity extends AppCompatActivity {

    private Button btnSelectDate;
    private Button btnSaveAttendance;

    private TextView tvSelectedDate;

    private LinearLayout llAttendanceList;

    private String selectedDate = "";

    private static final String GET_STUDENTS_URL =
            "http://10.0.2.2/student_attendance_api/get_students.php";

    private RequestQueue requestQueue;

    private ArrayList<HashMap<String, String>> studentList =
            new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_attendance);

        initializeViews();

        setupButtons();

        setCurrentDate();

        requestQueue = Volley.newRequestQueue(this);

        loadStudents();
    }


    private void initializeViews() {

        btnSelectDate =
                findViewById(R.id.btnSelectDate);

        btnSaveAttendance =
                findViewById(R.id.btnSaveAttendance);

        tvSelectedDate =
                findViewById(R.id.tvSelectedDate);

        llAttendanceList =
                findViewById(R.id.llAttendanceList);
    }


    private void setupButtons() {

        btnSelectDate.setOnClickListener(view -> {

            showDatePicker();

        });


        btnSaveAttendance.setOnClickListener(view -> {

            if (selectedDate.isEmpty()) {

                Toast.makeText(
                        AttendanceActivity.this,
                        "Please select attendance date",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            Toast.makeText(
                    AttendanceActivity.this,
                    "Attendance save will be implemented next",
                    Toast.LENGTH_SHORT
            ).show();

        });
    }


    private void setCurrentDate() {

        Calendar calendar = Calendar.getInstance();

        SimpleDateFormat sdf =
                new SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.getDefault()
                );

        selectedDate =
                sdf.format(calendar.getTime());

        tvSelectedDate.setText(
                "Date: " + selectedDate
        );
    }


    private void showDatePicker() {

        Calendar calendar =
                Calendar.getInstance();

        DatePickerDialog dialog =
                new DatePickerDialog(

                        AttendanceActivity.this,

                        (view, year, month, dayOfMonth) -> {

                            Calendar selectedCalendar =
                                    Calendar.getInstance();

                            selectedCalendar.set(
                                    year,
                                    month,
                                    dayOfMonth
                            );

                            SimpleDateFormat sdf =
                                    new SimpleDateFormat(
                                            "yyyy-MM-dd",
                                            Locale.getDefault()
                                    );

                            selectedDate =
                                    sdf.format(
                                            selectedCalendar.getTime()
                                    );

                            tvSelectedDate.setText(
                                    "Date: " + selectedDate
                            );
                        },

                        calendar.get(Calendar.YEAR),

                        calendar.get(Calendar.MONTH),

                        calendar.get(Calendar.DAY_OF_MONTH)
                );

        dialog.show();
    }


    // Load students from PHP API

    private void loadStudents() {

        JsonObjectRequest request =
                new JsonObjectRequest(
                        Request.Method.GET,
                        GET_STUDENTS_URL,
                        null,

                        response -> {

                            try {

                                boolean success =
                                        response.getBoolean("success");

                                if (!success) {

                                    Toast.makeText(
                                            AttendanceActivity.this,
                                            response.getString("message"),
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    return;
                                }


                                JSONArray students =
                                        response.getJSONArray("students");

                                studentList.clear();


                                for (int i = 0;
                                     i < students.length();
                                     i++) {

                                    JSONObject student =
                                            students.getJSONObject(i);

                                    HashMap<String, String> map =
                                            new HashMap<>();

                                    map.put(
                                            "id",
                                            student.getString("id")
                                    );

                                    map.put(
                                            "name",
                                            student.getString("name")
                                    );

                                    map.put(
                                            "roll_number",
                                            student.getString("roll_number")
                                    );

                                    map.put(
                                            "email",
                                            student.optString("email")
                                    );

                                    map.put(
                                            "phone",
                                            student.optString("phone")
                                    );

                                    studentList.add(map);
                                }


                                displayStudents();

                            } catch (Exception e) {

                                Toast.makeText(
                                        AttendanceActivity.this,
                                        "Error: " + e.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        },

                        error -> {

                            Toast.makeText(
                                    AttendanceActivity.this,
                                    "Failed to load students",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                );


        requestQueue.add(request);
    }


    // Display students

    private void displayStudents() {

        llAttendanceList.removeAllViews();


        if (studentList.isEmpty()) {

            TextView emptyText =
                    new TextView(this);

            emptyText.setText(
                    "No students found"
            );

            emptyText.setTextSize(16);

            emptyText.setPadding(
                    20,
                    30,
                    20,
                    30
            );

            llAttendanceList.addView(
                    emptyText
            );

            return;
        }


        for (HashMap<String, String> student :
                studentList) {

            LinearLayout studentCard =
                    new LinearLayout(this);

            studentCard.setOrientation(
                    LinearLayout.VERTICAL
            );

            studentCard.setPadding(
                    20,
                    20,
                    20,
                    20
            );


            TextView name =
                    new TextView(this);

            name.setText(
                    "Name: " +
                            student.get("name")
            );

            name.setTextSize(18);

            name.setTextColor(
                    getResources().getColor(
                            android.R.color.black
                    )
            );


            TextView roll =
                    new TextView(this);

            roll.setText(
                    "Roll: " +
                            student.get("roll_number")
            );

            roll.setTextSize(15);


            TextView id =
                    new TextView(this);

            id.setText(
                    "Student ID: " +
                            student.get("id")
            );

            id.setTextSize(13);


            studentCard.addView(name);

            studentCard.addView(roll);

            studentCard.addView(id);


            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );

            params.setMargins(
                    0,
                    0,
                    0,
                    20
            );


            llAttendanceList.addView(
                    studentCard,
                    params
            );
        }
    }
}