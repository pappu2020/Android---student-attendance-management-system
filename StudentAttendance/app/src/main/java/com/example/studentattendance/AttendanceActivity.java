package com.example.studentattendance;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AttendanceActivity extends AppCompatActivity {

    // =========================
    // API URLs
    // =========================

    private static final String GET_STUDENTS_URL =
            "http://10.0.2.2/student_attendance_api/get_students.php";

    private static final String MARK_ATTENDANCE_URL =
            "http://10.0.2.2/student_attendance_api/mark_attendance.php";


    // =========================
    // UI Components
    // =========================

    private Button btnSelectDate;
    private Button btnSaveAttendance;

    private TextView tvSelectedDate;

    private LinearLayout llAttendanceList;


    // =========================
    // Attendance Data
    // =========================

    private String selectedDate = "";

    private ArrayList<HashMap<String, String>> studentList =
            new ArrayList<>();

    /*
     * Key   = Student ID
     * Value = Present / Absent
     */
    private HashMap<String, String> attendanceStatus =
            new HashMap<>();


    // =========================
    // Volley
    // =========================

    private RequestQueue requestQueue;


    // =========================
    // onCreate
    // =========================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_attendance);


        // Initialize Views
        initializeViews();


        // Initialize Volley
        requestQueue = Volley.newRequestQueue(this);


        // Setup Buttons
        setupButtons();


        // Set today's date
        setCurrentDate();


        // Load students from API
        loadStudents();
    }


    // =========================
    // Initialize Views
    // =========================

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


    // =========================
    // Setup Buttons
    // =========================

    private void setupButtons() {

        // Select Date Button

        btnSelectDate.setOnClickListener(view -> {

            showDatePicker();

        });


        // Save Attendance Button

        btnSaveAttendance.setOnClickListener(view -> {

            if (selectedDate.isEmpty()) {

                Toast.makeText(
                        AttendanceActivity.this,
                        "Please select attendance date",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }


            if (studentList.isEmpty()) {

                Toast.makeText(
                        AttendanceActivity.this,
                        "No students available",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }


            saveAllAttendance();

        });
    }


    // =========================
    // Set Current Date
    // =========================

    private void setCurrentDate() {

        Calendar calendar =
                Calendar.getInstance();


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


    // =========================
    // Date Picker
    // =========================

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


    // =========================
    // Load Students
    // =========================

    private void loadStudents() {

        JsonObjectRequest request =
                new JsonObjectRequest(

                        Request.Method.GET,

                        GET_STUDENTS_URL,

                        null,


                        // =====================
                        // Success
                        // =====================

                        response -> {

                            try {

                                boolean success =
                                        response.getBoolean(
                                                "success"
                                        );


                                if (!success) {

                                    Toast.makeText(
                                            AttendanceActivity.this,
                                            response.getString(
                                                    "message"
                                            ),
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    return;
                                }


                                JSONArray students =
                                        response.getJSONArray(
                                                "students"
                                        );


                                studentList.clear();

                                attendanceStatus.clear();


                                // Read every student

                                for (
                                        int i = 0;
                                        i < students.length();
                                        i++
                                ) {

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
                                            student.getString(
                                                    "roll_number"
                                            )
                                    );


                                    map.put(
                                            "email",
                                            student.optString(
                                                    "email"
                                            )
                                    );


                                    map.put(
                                            "phone",
                                            student.optString(
                                                    "phone"
                                            )
                                    );


                                    studentList.add(map);


                                    /*
                                     * Default attendance:
                                     * Present
                                     */

                                    attendanceStatus.put(
                                            student.getString("id"),
                                            "Present"
                                    );
                                }


                                // Display students

                                displayStudents();


                            } catch (Exception e) {

                                Toast.makeText(
                                        AttendanceActivity.this,
                                        "Error: "
                                                + e.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        },


                        // =====================
                        // Error
                        // =====================

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


    // =========================
    // Display Students
    // =========================

    private void displayStudents() {

        llAttendanceList.removeAllViews();


        // No students

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


        // Create card for every student

        for (
                HashMap<String, String> student :
                studentList
        ) {

            // =========================
            // Student Card
            // =========================

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


            // =========================
            // Student Name
            // =========================

            TextView name =
                    new TextView(this);


            name.setText(
                    "Name: "
                            + student.get("name")
            );


            name.setTextSize(18);


            name.setTextColor(
                    Color.BLACK
            );


            // =========================
            // Student Roll
            // =========================

            TextView roll =
                    new TextView(this);


            roll.setText(
                    "Roll: "
                            + student.get("roll_number")
            );


            roll.setTextSize(15);


            // =========================
            // Student ID
            // =========================

            TextView id =
                    new TextView(this);


            id.setText(
                    "Student ID: "
                            + student.get("id")
            );


            id.setTextSize(13);


            // =========================
            // Attendance Label
            // =========================

            TextView attendanceLabel =
                    new TextView(this);


            attendanceLabel.setText(
                    "Attendance Status"
            );


            attendanceLabel.setTextSize(15);


            attendanceLabel.setTextColor(
                    Color.DKGRAY
            );


            attendanceLabel.setPadding(
                    0,
                    15,
                    0,
                    5
            );


            // =========================
            // Radio Group
            // =========================

            RadioGroup radioGroup =
                    new RadioGroup(this);


            radioGroup.setOrientation(
                    RadioGroup.HORIZONTAL
            );


            // =========================
            // Present Button
            // =========================

            RadioButton present =
                    new RadioButton(this);


            present.setText(
                    "Present"
            );


            present.setTextSize(15);


            present.setTag(
                    "Present"
            );


            // =========================
            // Absent Button
            // =========================

            RadioButton absent =
                    new RadioButton(this);


            absent.setText(
                    "Absent"
            );


            absent.setTextSize(15);


            absent.setTag(
                    "Absent"
            );


            // =========================
            // Default = Present
            // =========================

            present.setChecked(true);


            attendanceStatus.put(
                    student.get("id"),
                    "Present"
            );


            // =========================
            // Status Change Listener
            // =========================

            radioGroup.setOnCheckedChangeListener(
                    (group, checkedId) -> {

                        RadioButton selected =
                                group.findViewById(
                                        checkedId
                                );


                        if (selected != null) {

                            String status =
                                    selected.getTag()
                                            .toString();


                            attendanceStatus.put(
                                    student.get("id"),
                                    status
                            );
                        }
                    }
            );


            // =========================
            // Add Radio Buttons
            // =========================

            radioGroup.addView(
                    present
            );


            radioGroup.addView(
                    absent
            );


            // =========================
            // Add Views to Card
            // =========================

            studentCard.addView(
                    name
            );


            studentCard.addView(
                    roll
            );


            studentCard.addView(
                    id
            );


            studentCard.addView(
                    attendanceLabel
            );


            studentCard.addView(
                    radioGroup
            );


            // =========================
            // Card Layout Parameters
            // =========================

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


    // =========================
    // Save All Attendance
    // =========================

    private void saveAllAttendance() {

        btnSaveAttendance.setEnabled(
                false
        );


        saveAttendanceAtIndex(0);
    }


    // =========================
    // Save Attendance One by One
    // =========================

    private void saveAttendanceAtIndex(
            int index
    ) {

        // =========================
        // All Students Completed
        // =========================

        if (index >= studentList.size()) {

            btnSaveAttendance.setEnabled(
                    true
            );


            Toast.makeText(
                    AttendanceActivity.this,
                    "All attendance saved successfully",
                    Toast.LENGTH_LONG
            ).show();


            return;
        }


        // =========================
        // Student ID
        // =========================

        String studentId =
                studentList
                        .get(index)
                        .get("id");


        // =========================
        // Selected Status
        // =========================

        String status =
                attendanceStatus.get(
                        studentId
                );


        // Default Present

        if (status == null) {

            status = "Present";
        }


        String finalStatus =
                status;


        // =========================
        // POST Request
        // =========================

        StringRequest request =
                new StringRequest(

                        Request.Method.POST,

                        MARK_ATTENDANCE_URL,


                        // =====================
                        // Success Response
                        // =====================

                        response -> {

                            try {

                                JSONObject json =
                                        new JSONObject(
                                                response
                                        );


                                boolean success =
                                        json.getBoolean(
                                                "success"
                                        );


                                if (success) {

                                    /*
                                     * Save next student
                                     */

                                    saveAttendanceAtIndex(
                                            index + 1
                                    );

                                } else {

                                    btnSaveAttendance
                                            .setEnabled(
                                                    true
                                            );


                                    Toast.makeText(
                                            AttendanceActivity.this,
                                            json.getString(
                                                    "message"
                                            ),
                                            Toast.LENGTH_LONG
                                    ).show();
                                }


                            } catch (Exception e) {

                                btnSaveAttendance
                                        .setEnabled(
                                                true
                                        );


                                Toast.makeText(
                                        AttendanceActivity.this,
                                        "Response error: "
                                                + e.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        },


                        // =====================
                        // Network Error
                        // =====================

                        error -> {

                            btnSaveAttendance
                                    .setEnabled(
                                            true
                                    );


                            Toast.makeText(
                                    AttendanceActivity.this,
                                    "Network error",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                ) {


                    // =========================
                    // POST Parameters
                    // =========================

                    @Override
                    protected Map<String, String>
                    getParams() {

                        Map<String, String> params =
                                new HashMap<>();


                        params.put(
                                "student_id",
                                studentId
                        );


                        params.put(
                                "attendance_date",
                                selectedDate
                        );


                        params.put(
                                "status",
                                finalStatus
                        );


                        return params;
                    }
                };


        // Add request to queue

        requestQueue.add(
                request
        );
    }
}