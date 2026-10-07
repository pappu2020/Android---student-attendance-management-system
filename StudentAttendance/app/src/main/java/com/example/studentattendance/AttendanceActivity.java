package com.example.studentattendance;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


public class AttendanceActivity extends AppCompatActivity {

    // =========================================================
    // API URLs
    // =========================================================

    private static final String GET_STUDENTS_BY_CLASS_SECTION_URL =
            "http://10.0.2.2/student_attendance_api/get_students_by_class_section.php";

    private static final String MARK_ATTENDANCE_URL =
            "http://10.0.2.2/student_attendance_api/mark_attendance.php";

    private static final String GET_ATTENDANCE_URL =
            "http://10.0.2.2/student_attendance_api/get_attendance.php";


    // =========================================================
    // UI Components
    // =========================================================

    private Spinner spinnerClass;
    private Spinner spinnerSection;

    private Button btnSelectDate;
    private Button btnSaveAttendance;

    private TextView tvSelectedDate;
    private TextView tvStudentCount;
    private TextView tvAttendanceHint;
    private TextView tvPresentCount;
    private TextView tvAbsentCount;

    private LinearLayout llAttendanceList;


    // =========================================================
    // Selected Values
    // =========================================================

    private String selectedDate = "";
    private String selectedClass = "";
    private String selectedSection = "";


    // =========================================================
    // Data
    // =========================================================

    private final ArrayList<HashMap<String, String>> studentList =
            new ArrayList<>();

    private final HashMap<String, String> attendanceStatus =
            new HashMap<>();


    // =========================================================
    // Volley
    // =========================================================

    private RequestQueue requestQueue;


    // =========================================================
    // Calendar
    // =========================================================

    private Calendar calendar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_attendance);


        // =====================================================
        // Initialize Views
        // =====================================================

        spinnerClass = findViewById(R.id.spinnerClass);
        spinnerSection = findViewById(R.id.spinnerSection);

        btnSelectDate = findViewById(R.id.btnSelectDate);
        btnSaveAttendance = findViewById(R.id.btnSaveAttendance);

        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvStudentCount = findViewById(R.id.tvStudentCount);
        tvAttendanceHint = findViewById(R.id.tvAttendanceHint);

        tvPresentCount = findViewById(R.id.tvPresentCount);
        tvAbsentCount = findViewById(R.id.tvAbsentCount);

        llAttendanceList = findViewById(R.id.llAttendanceList);


        // =====================================================
        // Volley Request Queue
        // =====================================================

        requestQueue = Volley.newRequestQueue(this);


        // =====================================================
        // Calendar
        // =====================================================

        calendar = Calendar.getInstance();


        // =====================================================
        // Set Today's Date
        // =====================================================

        setCurrentDate();


        // =====================================================
        // Setup Class Spinner
        // =====================================================

        setupClassSpinner();


        // =====================================================
        // Setup Section Spinner
        // =====================================================

        setupSectionSpinner();


        // =====================================================
        // Date Button
        // =====================================================

        btnSelectDate.setOnClickListener(v -> showDatePicker());


        // =====================================================
        // Save Button
        // =====================================================

        btnSaveAttendance.setOnClickListener(v -> {

            if (studentList.isEmpty()) {

                Toast.makeText(
                        AttendanceActivity.this,
                        "Please select class and section first",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            if (selectedDate.isEmpty()) {

                Toast.makeText(
                        AttendanceActivity.this,
                        "Please select attendance date",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            saveAllAttendance();
        });


        // =====================================================
        // Initial UI
        // =====================================================

        tvPresentCount.setText("Present: 0");
        tvAbsentCount.setText("Absent: 0");

        showEmptyMessage(
                "Please select class and section"
        );
    }


    // =========================================================
    // Set Current Date
    // =========================================================

    private void setCurrentDate() {

        SimpleDateFormat databaseFormat =
                new SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.getDefault()
                );

        SimpleDateFormat displayFormat =
                new SimpleDateFormat(
                        "dd MMMM yyyy",
                        Locale.getDefault()
                );

        selectedDate =
                databaseFormat.format(calendar.getTime());

        tvSelectedDate.setText(
                displayFormat.format(calendar.getTime())
        );
    }


    // =========================================================
    // Class Spinner
    // =========================================================

    private void setupClassSpinner() {

        ArrayList<String> classList =
                new ArrayList<>();

        classList.add("Select Class");

        classList.add("Class 6");
        classList.add("Class 7");
        classList.add("Class 8");
        classList.add("Class 9");
        classList.add("Class 10");
        classList.add("Class 11");
        classList.add("Class 12");


        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        classList
                );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerClass.setAdapter(adapter);


        spinnerClass.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {

                        if (position == 0) {

                            selectedClass = "";

                            selectedSection = "";

                            studentList.clear();

                            attendanceStatus.clear();

                            llAttendanceList.removeAllViews();

                            showEmptyMessage(
                                    "Please select class and section"
                            );

                            tvStudentCount.setText(
                                    "Students: 0"
                            );

                            tvAttendanceHint.setText(
                                    "Select class and section to load students"
                            );

                            updateAttendanceCount();

                            return;
                        }


                        selectedClass =
                                parent.getItemAtPosition(
                                        position
                                ).toString();


                        // Reset Section
                        spinnerSection.setSelection(0);

                        selectedSection = "";

                        studentList.clear();

                        attendanceStatus.clear();

                        llAttendanceList.removeAllViews();

                        showEmptyMessage(
                                "Please select section"
                        );

                        tvStudentCount.setText(
                                "Students: 0"
                        );

                        tvAttendanceHint.setText(
                                "Select section to load students"
                        );

                        updateAttendanceCount();
                    }


                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent
                    ) {

                    }
                }
        );
    }


    // =========================================================
    // Section Spinner
    // =========================================================

    private void setupSectionSpinner() {

        ArrayList<String> sectionList =
                new ArrayList<>();

        sectionList.add("Select Section");

        sectionList.add("Section A");
        sectionList.add("Section B");
        sectionList.add("Section C");
        sectionList.add("Section D");


        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        sectionList
                );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerSection.setAdapter(adapter);


        spinnerSection.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {

                        if (position == 0) {

                            selectedSection = "";

                            studentList.clear();

                            attendanceStatus.clear();

                            llAttendanceList.removeAllViews();

                            showEmptyMessage(
                                    "Please select section"
                            );

                            tvStudentCount.setText(
                                    "Students: 0"
                            );

                            tvAttendanceHint.setText(
                                    "Select class and section to load students"
                            );

                            updateAttendanceCount();

                            return;
                        }


                        selectedSection =
                                parent.getItemAtPosition(
                                        position
                                ).toString();


                        if (!selectedClass.isEmpty()) {

                            loadStudentsByClassAndSection();
                        }
                    }


                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent
                    ) {

                    }
                }
        );
    }


    // =========================================================
    // Date Picker
    // =========================================================

    private void showDatePicker() {

        int year =
                calendar.get(Calendar.YEAR);

        int month =
                calendar.get(Calendar.MONTH);

        int day =
                calendar.get(Calendar.DAY_OF_MONTH);


        DatePickerDialog datePickerDialog =
                new DatePickerDialog(
                        this,

                        (view, selectedYear, selectedMonth, selectedDay) -> {

                            calendar.set(
                                    selectedYear,
                                    selectedMonth,
                                    selectedDay
                            );


                            SimpleDateFormat databaseFormat =
                                    new SimpleDateFormat(
                                            "yyyy-MM-dd",
                                            Locale.getDefault()
                                    );

                            SimpleDateFormat displayFormat =
                                    new SimpleDateFormat(
                                            "dd MMMM yyyy",
                                            Locale.getDefault()
                                    );


                            selectedDate =
                                    databaseFormat.format(
                                            calendar.getTime()
                                    );


                            tvSelectedDate.setText(
                                    displayFormat.format(
                                            calendar.getTime()
                                    )
                            );


                            // Reload students + saved attendance
                            if (!selectedClass.isEmpty()
                                    && !selectedSection.isEmpty()) {

                                loadStudentsByClassAndSection();
                            }
                        },

                        year,
                        month,
                        day
                );


        datePickerDialog.show();
    }


    // =========================================================
    // Load Students
    // =========================================================

    private void loadStudentsByClassAndSection() {

        if (selectedClass.isEmpty()
                || selectedSection.isEmpty()) {

            return;
        }


        // Clear previous data
        studentList.clear();

        attendanceStatus.clear();

        llAttendanceList.removeAllViews();


        tvStudentCount.setText(
                "Loading students..."
        );

        tvAttendanceHint.setText(
                "Please wait..."
        );


        StringRequest request =
                new StringRequest(
                        Request.Method.POST,
                        GET_STUDENTS_BY_CLASS_SECTION_URL,

                        response -> {

                            try {

                                JSONObject jsonObject =
                                        new JSONObject(response);


                                boolean success =
                                        jsonObject.optBoolean(
                                                "success",
                                                false
                                        );


                                if (!success) {

                                    showEmptyMessage(
                                            jsonObject.optString(
                                                    "message",
                                                    "No students found"
                                            )
                                    );

                                    tvStudentCount.setText(
                                            "Students: 0"
                                    );

                                    tvAttendanceHint.setText(
                                            "No students available"
                                    );

                                    updateAttendanceCount();

                                    return;
                                }


                                JSONArray studentsArray =
                                        jsonObject.optJSONArray(
                                                "students"
                                        );


                                if (studentsArray == null
                                        || studentsArray.length() == 0) {

                                    showEmptyMessage(
                                            "No students found for this class and section"
                                    );

                                    tvStudentCount.setText(
                                            "Students: 0"
                                    );

                                    tvAttendanceHint.setText(
                                            "No students available"
                                    );

                                    updateAttendanceCount();

                                    return;
                                }


                                // ==========================================
                                // Store Students
                                // ==========================================

                                for (int i = 0;
                                     i < studentsArray.length();
                                     i++) {

                                    JSONObject studentObject =
                                            studentsArray.getJSONObject(i);


                                    HashMap<String, String> student =
                                            new HashMap<>();


                                    String studentId =
                                            studentObject.optString(
                                                    "id"
                                            );


                                    student.put(
                                            "id",
                                            studentId
                                    );

                                    student.put(
                                            "name",
                                            studentObject.optString(
                                                    "name"
                                            )
                                    );

                                    student.put(
                                            "roll_number",
                                            studentObject.optString(
                                                    "roll_number"
                                            )
                                    );

                                    student.put(
                                            "class_name",
                                            studentObject.optString(
                                                    "class_name"
                                            )
                                    );

                                    student.put(
                                            "section",
                                            studentObject.optString(
                                                    "section"
                                            )
                                    );

                                    student.put(
                                            "email",
                                            studentObject.optString(
                                                    "email"
                                            )
                                    );

                                    student.put(
                                            "phone",
                                            studentObject.optString(
                                                    "phone"
                                            )
                                    );

                                    student.put(
                                            "photo",
                                            studentObject.optString(
                                                    "photo"
                                            )
                                    );


                                    studentList.add(student);


                                    // ======================================
                                    // Default = Absent
                                    // ======================================

                                    attendanceStatus.put(
                                            studentId,
                                            "Absent"
                                    );
                                }


                                // ==========================================
                                // Load Existing Attendance
                                // ==========================================

                                loadSavedAttendance();


                            } catch (JSONException e) {

                                showEmptyMessage(
                                        "Invalid server response"
                                );

                                tvStudentCount.setText(
                                        "Students: 0"
                                );

                                Toast.makeText(
                                        AttendanceActivity.this,
                                        "JSON error: "
                                                + e.getMessage(),
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        },

                        error -> {

                            showEmptyMessage(
                                    "Failed to connect to server"
                            );

                            tvStudentCount.setText(
                                    "Students: 0"
                            );

                            tvAttendanceHint.setText(
                                    "Please check XAMPP and API"
                            );


                            Toast.makeText(
                                    AttendanceActivity.this,
                                    "Failed to load students",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                ) {

                    @Override
                    protected Map<String, String> getParams() {

                        Map<String, String> params =
                                new HashMap<>();


                        params.put(
                                "class_name",
                                selectedClass
                        );

                        params.put(
                                "section",
                                selectedSection
                        );


                        return params;
                    }
                };


        requestQueue.add(request);
    }


    // =========================================================
    // Load Saved Attendance
    // =========================================================

    private void loadSavedAttendance() {

        if (selectedClass.isEmpty()
                || selectedSection.isEmpty()
                || selectedDate.isEmpty()) {

            displayStudents();

            return;
        }


        StringRequest request =
                new StringRequest(
                        Request.Method.POST,
                        GET_ATTENDANCE_URL,

                        response -> {

                            try {

                                JSONObject jsonObject =
                                        new JSONObject(response);


                                boolean success =
                                        jsonObject.optBoolean(
                                                "success",
                                                false
                                        );


                                if (!success) {

                                    // No saved record
                                    // Keep default Absent
                                    displayStudents();

                                    return;
                                }


                                JSONArray attendanceArray =
                                        jsonObject.optJSONArray(
                                                "attendance"
                                        );


                                // ==========================================
                                // First set everybody as Absent
                                // ==========================================

                                for (HashMap<String, String> student
                                        : studentList) {

                                    String studentId =
                                            student.get("id");


                                    attendanceStatus.put(
                                            studentId,
                                            "Absent"
                                    );
                                }


                                // ==========================================
                                // Apply Saved Attendance
                                // ==========================================

                                if (attendanceArray != null) {

                                    for (int i = 0;
                                         i < attendanceArray.length();
                                         i++) {

                                        JSONObject attendanceObject =
                                                attendanceArray
                                                        .getJSONObject(i);


                                        String studentId =
                                                attendanceObject.optString(
                                                        "student_id"
                                                );


                                        String status =
                                                attendanceObject.optString(
                                                        "status"
                                                );


                                        if (!studentId.isEmpty()) {

                                            if ("Present".equals(status)) {

                                                attendanceStatus.put(
                                                        studentId,
                                                        "Present"
                                                );

                                            } else {

                                                attendanceStatus.put(
                                                        studentId,
                                                        "Absent"
                                                );
                                            }
                                        }
                                    }
                                }


                                // ==========================================
                                // Display Students
                                // ==========================================

                                displayStudents();


                            } catch (JSONException e) {

                                displayStudents();

                                Toast.makeText(
                                        AttendanceActivity.this,
                                        "Invalid attendance response",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        },

                        error -> {

                            // If attendance API fails,
                            // students still show with default Absent

                            displayStudents();

                            Toast.makeText(
                                    AttendanceActivity.this,
                                    "Could not load saved attendance",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                ) {

                    @Override
                    protected Map<String, String> getParams() {

                        Map<String, String> params =
                                new HashMap<>();


                        params.put(
                                "class_name",
                                selectedClass
                        );

                        params.put(
                                "section",
                                selectedSection
                        );

                        params.put(
                                "attendance_date",
                                selectedDate
                        );


                        return params;
                    }
                };


        requestQueue.add(request);
    }


    // =========================================================
    // Display Students
    // =========================================================

    private void displayStudents() {

        llAttendanceList.removeAllViews();


        if (studentList.isEmpty()) {

            showEmptyMessage(
                    "No students found"
            );

            tvStudentCount.setText(
                    "Students: 0"
            );

            tvAttendanceHint.setText(
                    "No students available"
            );

            updateAttendanceCount();

            return;
        }


        tvStudentCount.setText(
                "Students: " + studentList.size()
        );


        tvAttendanceHint.setText(
                "Select Present or Absent for each student"
        );


        // =====================================================
        // Create Student Cards
        // =====================================================

        for (HashMap<String, String> student
                : studentList) {

            String studentId =
                    student.get("id");

            String studentName =
                    student.get("name");

            String rollNumber =
                    student.get("roll_number");


            // =================================================
            // Main Card
            // =================================================

            LinearLayout card =
                    new LinearLayout(this);

            card.setOrientation(
                    LinearLayout.VERTICAL
            );

            card.setPadding(
                    24,
                    20,
                    24,
                    20
            );


            GradientDrawable cardBackground =
                    new GradientDrawable();

            cardBackground.setColor(
                    Color.WHITE
            );

            cardBackground.setCornerRadius(
                    24
            );

            cardBackground.setStroke(
                    1,
                    Color.rgb(
                            230,
                            233,
                            238
                    )
            );


            card.setBackground(
                    cardBackground
            );


            LinearLayout.LayoutParams cardParams =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );


            cardParams.setMargins(
                    0,
                    0,
                    0,
                    18
            );


            card.setLayoutParams(
                    cardParams
            );


            // =================================================
            // Student Name
            // =================================================

            TextView nameText =
                    new TextView(this);

            nameText.setText(
                    studentName
            );

            nameText.setTextSize(
                    18
            );

            nameText.setTextColor(
                    Color.rgb(
                            30,
                            35,
                            45
                    )
            );

            nameText.setTypeface(
                    null,
                    Typeface.BOLD
            );


            card.addView(
                    nameText
            );


            // =================================================
            // Roll Number
            // =================================================

            TextView rollText =
                    new TextView(this);

            rollText.setText(
                    "Roll Number: " + rollNumber
            );

            rollText.setTextSize(
                    14
            );

            rollText.setTextColor(
                    Color.rgb(
                            110,
                            115,
                            125
                    )
            );


            LinearLayout.LayoutParams rollParams =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );


            rollParams.setMargins(
                    0,
                    5,
                    0,
                    15
            );


            rollText.setLayoutParams(
                    rollParams
            );


            card.addView(
                    rollText
            );


            // =================================================
            // Attendance Status Text
            // =================================================

            TextView statusTitle =
                    new TextView(this);

            statusTitle.setText(
                    "Attendance Status"
            );

            statusTitle.setTextSize(
                    14
            );

            statusTitle.setTextColor(
                    Color.rgb(
                            80,
                            85,
                            95
                    )
            );

            statusTitle.setTypeface(
                    null,
                    Typeface.BOLD
            );


            card.addView(
                    statusTitle
            );


            // =================================================
            // Radio Group
            // =================================================

            RadioGroup radioGroup =
                    new RadioGroup(this);

            radioGroup.setOrientation(
                    RadioGroup.HORIZONTAL
            );

            radioGroup.setGravity(
                    Gravity.CENTER_VERTICAL
            );


            LinearLayout.LayoutParams radioGroupParams =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );


            radioGroupParams.setMargins(
                    0,
                    10,
                    0,
                    0
            );


            radioGroup.setLayoutParams(
                    radioGroupParams
            );


            // =================================================
            // Present Radio Button
            // =================================================

            RadioButton present =
                    new RadioButton(this);

            present.setText(
                    "Present"
            );

            present.setTextSize(
                    14
            );

            present.setTextColor(
                    Color.rgb(
                            50,
                            55,
                            60
                    )
            );

            present.setGravity(
                    Gravity.CENTER
            );

            present.setPadding(
                    12,
                    12,
                    12,
                    12
            );

            present.setButtonDrawable(
                    null
            );

            present.setBackgroundResource(
                    R.drawable.bg_present
            );


            present.setId(
                    View.generateViewId()
            );


            LinearLayout.LayoutParams presentParams =
                    new LinearLayout.LayoutParams(
                            0,
                            55,
                            1
                    );


            presentParams.setMargins(
                    0,
                    0,
                    8,
                    0
            );


            present.setLayoutParams(
                    presentParams
            );


            // =================================================
            // Absent Radio Button
            // =================================================

            RadioButton absent =
                    new RadioButton(this);

            absent.setText(
                    "Absent"
            );

            absent.setTextSize(
                    14
            );

            absent.setTextColor(
                    Color.rgb(
                            50,
                            55,
                            60
                    )
            );

            absent.setGravity(
                    Gravity.CENTER
            );

            absent.setPadding(
                    12,
                    12,
                    12,
                    12
            );

            absent.setButtonDrawable(
                    null
            );

            absent.setBackgroundResource(
                    R.drawable.bg_absent
            );


            absent.setId(
                    View.generateViewId()
            );


            LinearLayout.LayoutParams absentParams =
                    new LinearLayout.LayoutParams(
                            0,
                            55,
                            1
                    );


            absentParams.setMargins(
                    8,
                    0,
                    0,
                    0
            );


            absent.setLayoutParams(
                    absentParams
            );


            // =================================================
            // Add Buttons
            // =================================================

            radioGroup.addView(
                    present
            );

            radioGroup.addView(
                    absent
            );


            card.addView(
                    radioGroup
            );


            // =================================================
            // Get Current Saved Status
            // =================================================

            String currentStatus =
                    attendanceStatus.get(studentId);


            if ("Present".equals(currentStatus)) {

                radioGroup.check(
                        present.getId()
                );

            } else {

                // Default = Absent

                radioGroup.check(
                        absent.getId()
                );
            }


            // =================================================
            // Radio Group Listener
            // =================================================

            radioGroup.setOnCheckedChangeListener(
                    (group, checkedId) -> {

                        if (checkedId ==
                                present.getId()) {

                            attendanceStatus.put(
                                    studentId,
                                    "Present"
                            );

                        } else if (checkedId ==
                                absent.getId()) {

                            attendanceStatus.put(
                                    studentId,
                                    "Absent"
                            );
                        }


                        updateAttendanceCount();
                    }
            );


            // =================================================
            // Add Card
            // =================================================

            llAttendanceList.addView(
                    card
            );
        }


        // =====================================================
        // Update Counter
        // =====================================================

        updateAttendanceCount();
    }


    // =========================================================
    // Empty Message
    // =========================================================

    private void showEmptyMessage(String message) {

        llAttendanceList.removeAllViews();


        TextView emptyText =
                new TextView(this);

        emptyText.setText(
                message
        );

        emptyText.setTextSize(
                15
        );

        emptyText.setTextColor(
                Color.rgb(
                        120,
                        125,
                        135
                )
        );

        emptyText.setGravity(
                Gravity.CENTER
        );

        emptyText.setPadding(
                20,
                60,
                20,
                60
        );


        llAttendanceList.addView(
                emptyText
        );
    }


    // =========================================================
    // Attendance Counter
    // =========================================================

    private void updateAttendanceCount() {

        int presentCount = 0;

        int absentCount = 0;


        for (HashMap<String, String> student
                : studentList) {

            String studentId =
                    student.get("id");


            String status =
                    attendanceStatus.get(
                            studentId
                    );


            if ("Present".equals(status)) {

                presentCount++;

            } else {

                absentCount++;
            }
        }


        tvPresentCount.setText(
                "Present: " + presentCount
        );

        tvAbsentCount.setText(
                "Absent: " + absentCount
        );
    }


    // =========================================================
    // Save All Attendance
    // =========================================================

    private void saveAllAttendance() {

        if (studentList.isEmpty()) {

            Toast.makeText(
                    this,
                    "No students to save",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        btnSaveAttendance.setEnabled(
                false
        );

        btnSaveAttendance.setText(
                "Saving..."
        );


        saveAttendanceAtIndex(
                0
        );
    }


    // =========================================================
    // Save Attendance One By One
    // =========================================================

    private void saveAttendanceAtIndex(
            int index
    ) {

        if (index >= studentList.size()) {

            btnSaveAttendance.setEnabled(
                    true
            );

            btnSaveAttendance.setText(
                    "Save Attendance"
            );


            Toast.makeText(
                    AttendanceActivity.this,
                    "All attendance saved successfully",
                    Toast.LENGTH_SHORT
            ).show();


            // Reload saved data from database
            loadStudentsByClassAndSection();

            return;
        }


        HashMap<String, String> student =
                studentList.get(index);


        String studentId =
                student.get("id");


        String status =
                attendanceStatus.get(studentId);


        // Safety default
        if (status == null
                || status.isEmpty()) {

            status = "Absent";
        }


        final String finalStatus =
                status;


        StringRequest request =
                new StringRequest(
                        Request.Method.POST,
                        MARK_ATTENDANCE_URL,

                        response -> {

                            try {

                                JSONObject jsonObject =
                                        new JSONObject(response);


                                boolean success =
                                        jsonObject.optBoolean(
                                                "success",
                                                false
                                        );


                                if (success) {

                                    // Save next student
                                    saveAttendanceAtIndex(
                                            index + 1
                                    );

                                } else {

                                    btnSaveAttendance.setEnabled(
                                            true
                                    );

                                    btnSaveAttendance.setText(
                                            "Save Attendance"
                                    );


                                    Toast.makeText(
                                            AttendanceActivity.this,
                                            jsonObject.optString(
                                                    "message",
                                                    "Failed to save attendance"
                                            ),
                                            Toast.LENGTH_LONG
                                    ).show();
                                }


                            } catch (JSONException e) {

                                btnSaveAttendance.setEnabled(
                                        true
                                );

                                btnSaveAttendance.setText(
                                        "Save Attendance"
                                );


                                Toast.makeText(
                                        AttendanceActivity.this,
                                        "Invalid server response",
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        },

                        error -> {

                            btnSaveAttendance.setEnabled(
                                    true
                            );

                            btnSaveAttendance.setText(
                                    "Save Attendance"
                            );


                            Toast.makeText(
                                    AttendanceActivity.this,
                                    "Failed to save attendance",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                ) {

                    @Override
                    protected Map<String, String> getParams() {

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


        requestQueue.add(request);
    }
}