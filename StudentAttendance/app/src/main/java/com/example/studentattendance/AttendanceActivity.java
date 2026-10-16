package com.example.studentattendance;

import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
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
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class AttendanceActivity extends AppCompatActivity {

    private static final String BASE_URL =
            "http://10.0.2.2/student_attendance_api/";

    private static final String PREFS_NAME =
            "StudentAttendanceAuth";

    private static final String GET_CLASSES_URL =
            BASE_URL + "get_attendance_classes.php";

    private static final String GET_ROUTINES_URL =
            BASE_URL + "get_attendance_routines.php";

    private static final String GET_STUDENTS_URL =
            BASE_URL + "get_students_by_class_section.php";

    private static final String GET_ATTENDANCE_URL =
            BASE_URL + "get_attendance.php";

    private static final String MARK_ATTENDANCE_URL =
            BASE_URL + "mark_attendance.php";

    private Spinner spinnerClass;
    private Spinner spinnerSection;
    private Spinner spinnerTime;
    private Spinner spinnerSubject;

    private Button btnSelectDate;
    private Button btnSaveAttendance;

    private TextView tvSelectedDate;
    private TextView tvStudentCount;
    private TextView tvAttendanceHint;
    private TextView tvPresentCount;
    private TextView tvAbsentCount;
    private TextView tvAssignedTeacher;

    private LinearLayout llAttendanceList;

    private RequestQueue requestQueue;
    private Calendar calendar;

    private String authToken = "";
    private String userRole = "";

    private String selectedDate = "";
    private String selectedClass = "";
    private String selectedSection = "";
    private String selectedRoutineId = "";

    private boolean suppressClassCallback = false;
    private boolean suppressSectionCallback = false;
    private boolean suppressRoutineCallback = false;
    private boolean isSaving = false;

    /*
     * Class spinner:
     * Select Class
     * MCA
     * BCA
     * B.Tech
     *
     * Section spinner:
     * Select Section
     * A
     * B
     *
     * The sections displayed are filtered by the selected class.
     */

    private final ArrayList<String> classList =
            new ArrayList<>();

    private final ArrayList<String> sectionList =
            new ArrayList<>();

    /*
     * Stores all class-section records received from PHP.
     */
    private final ArrayList<JSONObject> classSectionData =
            new ArrayList<>();

    private final ArrayList<String> timeList =
            new ArrayList<>();

    private final ArrayList<String> subjectList =
            new ArrayList<>();

    private final ArrayList<JSONObject> routineList =
            new ArrayList<>();

    private final ArrayList<HashMap<String, String>> studentList =
            new ArrayList<>();

    private final HashMap<String, String> attendanceStatus =
            new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance);

        initializeViews();

        SharedPreferences preferences =
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        authToken = preferences.getString("auth_token", "");

        userRole = preferences.getString("user_role", "")
                .trim()
                .toLowerCase(Locale.ROOT);

        if (authToken.trim().isEmpty()) {
            Toast.makeText(
                    this,
                    "Session expired. Please login again.",
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }

        requestQueue = Volley.newRequestQueue(
                getApplicationContext()
        );

        calendar = Calendar.getInstance();

        setCurrentDate();
        setupInitialSpinners();
        setupListeners();

        showEmptyMessage(
                "Select class and section to load students"
        );

        loadAttendanceClasses();
    }

    // ---------------------------------------------------------
    // 1. INITIALIZE VIEWS
    // ---------------------------------------------------------

    private void initializeViews() {

        spinnerClass = findViewById(R.id.spinnerClass);
        spinnerSection = findViewById(R.id.spinnerSection);
        spinnerTime = findViewById(R.id.spinnerTime);
        spinnerSubject = findViewById(R.id.spinnerSubject);

        btnSelectDate = findViewById(R.id.btnSelectDate);
        btnSaveAttendance = findViewById(R.id.btnSaveAttendance);

        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvStudentCount = findViewById(R.id.tvStudentCount);
        tvAttendanceHint = findViewById(R.id.tvAttendanceHint);

        tvPresentCount = findViewById(R.id.tvPresentCount);
        tvAbsentCount = findViewById(R.id.tvAbsentCount);

        tvAssignedTeacher = findViewById(R.id.tvAssignedTeacher);

        llAttendanceList = findViewById(R.id.llAttendanceList);
    }

    private void setupInitialSpinners() {

        setSpinner(
                spinnerClass,
                new String[]{"Loading classes..."}
        );

        setSpinner(
                spinnerSection,
                new String[]{"Select Section"}
        );

        setSpinner(
                spinnerTime,
                new String[]{"Select Time"}
        );

        setSpinner(
                spinnerSubject,
                new String[]{"Select Subject"}
        );

        tvAssignedTeacher.setText("Teacher: Not selected");
        tvPresentCount.setText("Present: 0");
        tvAbsentCount.setText("Absent: 0");

        tvStudentCount.setText("Students: 0");
    }

    // ---------------------------------------------------------
    // 2. SPINNER LISTENERS
    // ---------------------------------------------------------

    private void setupListeners() {

        /*
         * Class selection.
         * Changing the class resets the selected section.
         */

        spinnerClass.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {

                        if (suppressClassCallback) {
                            return;
                        }

                        if (position <= 0 ||
                                position >= classList.size()) {

                            selectedClass = "";
                            selectedSection = "";

                            resetSectionSpinner();
                            resetRoutineSelection();
                            clearStudents();

                            return;
                        }

                        String newClass = classList.get(position);

                        if (newClass.equals(selectedClass)) {
                            return;
                        }

                        selectedClass = newClass;
                        selectedSection = "";

                        resetSectionSpinner();
                        resetRoutineSelection();
                        clearStudents();

                        populateSectionsForClass(selectedClass);
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent
                    ) {
                    }
                }
        );

        /*
         * Section selection.
         * Routine loading starts only after a valid section
         * has been selected.
         */

        spinnerSection.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {

                        if (suppressSectionCallback) {
                            return;
                        }

                        if (position <= 0 ||
                                position >= sectionList.size()) {

                            selectedSection = "";

                            resetRoutineSelection();
                            clearStudents();

                            return;
                        }

                        String newSection = sectionList.get(position);

                        if (newSection.equals(selectedSection)) {
                            return;
                        }

                        selectedSection = newSection;

                        resetRoutineSelection();
                        clearStudents();

                        if (!selectedClass.isEmpty()
                                && !selectedSection.isEmpty()) {

                            loadRoutines();
                        }
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent
                    ) {
                    }
                }
        );

        /*
         * Time selection.
         */

        spinnerTime.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {

                        if (suppressRoutineCallback) {
                            return;
                        }

                        if (position <= 0 ||
                                position > routineList.size()) {

                            selectedRoutineId = "";

                            updateSubjectAndTeacher(null);
                            clearStudents();

                            return;
                        }

                        JSONObject routine =
                                routineList.get(position - 1);

                        selectedRoutineId =
                                routine.optString("routine_id", "");

                        updateSubjectAndTeacher(routine);

                        if (!selectedRoutineId.isEmpty()) {
                            loadStudentsByClassAndSection();
                        } else {
                            clearStudents();
                        }
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent
                    ) {
                    }
                }
        );

        btnSelectDate.setOnClickListener(
                view -> showDatePicker()
        );

        btnSaveAttendance.setOnClickListener(view -> {

            if (isSaving) {
                return;
            }

            if (selectedClass.isEmpty()
                    || selectedSection.isEmpty()) {

                showToast("Please select class and section");
                return;
            }

            if (selectedRoutineId.isEmpty()) {
                showToast("Please select a scheduled time");
                return;
            }

            if (selectedDate.isEmpty()) {
                showToast("Please select attendance date");
                return;
            }

            if (studentList.isEmpty()) {
                showToast("No students available to save");
                return;
            }

            saveAllAttendance();
        });
    }

    // ---------------------------------------------------------
    // 3. SPINNER ADAPTER HELPERS
    // ---------------------------------------------------------

    private void setSpinner(
            Spinner spinner,
            String[] values
    ) {

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        values
                );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinner.setAdapter(adapter);
    }

    private void setSpinner(
            Spinner spinner,
            ArrayList<String> values
    ) {

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        values
                );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinner.setAdapter(adapter);
    }

    private Map<String, String> authHeaders() {

        Map<String, String> headers = new HashMap<>();

        headers.put("Authorization", "Bearer " + authToken);
        headers.put("Accept", "application/json");

        return headers;
    }

    // ---------------------------------------------------------
    // 4. DATE HANDLING
    // ---------------------------------------------------------

    private void setCurrentDate() {

        SimpleDateFormat databaseFormat =
                new SimpleDateFormat("yyyy-MM-dd", Locale.US);

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

    private void showDatePicker() {

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (picker, year, month, day) -> {

                    calendar.set(
                            year,
                            month,
                            day,
                            0,
                            0,
                            0
                    );

                    SimpleDateFormat databaseFormat =
                            new SimpleDateFormat(
                                    "yyyy-MM-dd",
                                    Locale.US
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

                    clearStudents();

                    if (!selectedClass.isEmpty()
                            && !selectedSection.isEmpty()) {

                        loadRoutines();
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        dialog.show();
    }

    /*
     * MySQL routine convention:
     * Monday = 1, Tuesday = 2, ..., Sunday = 7.
     */

    private int getDayOfWeek() {

        int day = calendar.get(Calendar.DAY_OF_WEEK);

        if (day == Calendar.SUNDAY) {
            return 7;
        }

        return day - 1;
    }

    // ---------------------------------------------------------
    // 5. LOAD CLASSES AND SECTIONS FROM PHP API
    // ---------------------------------------------------------

    private void loadAttendanceClasses() {

        setSpinner(
                spinnerClass,
                new String[]{"Loading classes..."}
        );

        StringRequest request = new StringRequest(
                Request.Method.POST,
                GET_CLASSES_URL,

                response -> {

                    try {

                        JSONObject json = new JSONObject(response);

                        if (!json.optBoolean("success", false)) {

                            showToast(
                                    json.optString(
                                            "message",
                                            "Unable to load classes"
                                    )
                            );

                            resetClassAndSectionSpinners();
                            return;
                        }

                        JSONArray classes =
                                json.optJSONArray("classes");

                        classList.clear();
                        sectionList.clear();
                        classSectionData.clear();

                        classList.add("Select Class");

                        sectionList.add("Select Section");

                        Set<String> uniqueClasses = new HashSet<>();

                        if (classes != null) {

                            for (int i = 0;
                                 i < classes.length();
                                 i++) {

                                JSONObject item =
                                        classes.getJSONObject(i);

                                String className =
                                        item.optString(
                                                "class_name",
                                                ""
                                        ).trim();

                                String section =
                                        item.optString(
                                                "section",
                                                ""
                                        ).trim();

                                if (className.isEmpty()
                                        || section.isEmpty()) {

                                    continue;
                                }

                                classSectionData.add(item);

                                if (uniqueClasses.add(className)) {
                                    classList.add(className);
                                }
                            }
                        }

                        setSpinner(spinnerClass, classList);
                        resetSectionSpinner();

                        selectedClass = "";
                        selectedSection = "";

                        resetRoutineSelection();
                        clearStudents();

                        if (classList.size() <= 1) {

                            showEmptyMessage(
                                    "No assigned classes or sections found"
                            );
                        }

                    } catch (JSONException e) {

                        Log.e(
                                "AttendanceActivity",
                                "Class JSON parsing failed",
                                e
                        );

                        showToast("Invalid class API response");
                        resetClassAndSectionSpinners();
                    }
                },

                error -> {

                    showVolleyError(
                            "Loading classes failed",
                            error
                    );

                    resetClassAndSectionSpinners();
                }
        ) {

            @Override
            public Map<String, String> getHeaders() {
                return authHeaders();
            }

            @Override
            protected Map<String, String> getParams() {
                return new HashMap<>();
            }
        };

        requestQueue.add(request);
    }

    /*
     * Filters section options using the selected class.
     *
     * Example API records:
     * MCA - A
     * MCA - B
     * BCA - A
     *
     * Selecting MCA displays only A and B.
     */

    private void populateSectionsForClass(String className) {

        sectionList.clear();
        sectionList.add("Select Section");

        Set<String> uniqueSections = new HashSet<>();

        for (JSONObject item : classSectionData) {

            String apiClass =
                    item.optString("class_name", "").trim();

            String apiSection =
                    item.optString("section", "").trim();

            if (className.equals(apiClass)
                    && !apiSection.isEmpty()
                    && uniqueSections.add(apiSection)) {

                sectionList.add(apiSection);
            }
        }

        suppressSectionCallback = true;

        setSpinner(spinnerSection, sectionList);
        spinnerSection.setSelection(0);

        suppressSectionCallback = false;

        selectedSection = "";

        if (sectionList.size() <= 1) {

            showEmptyMessage(
                    "No sections found for " + className
            );
        }
    }

    private void resetSectionSpinner() {

        sectionList.clear();
        sectionList.add("Select Section");

        suppressSectionCallback = true;

        setSpinner(spinnerSection, sectionList);
        spinnerSection.setSelection(0);

        suppressSectionCallback = false;
    }

    private void resetClassAndSectionSpinners() {

        classList.clear();
        classList.add("Select Class");

        classSectionData.clear();

        setSpinner(spinnerClass, classList);

        resetSectionSpinner();

        selectedClass = "";
        selectedSection = "";

        resetRoutineSelection();
        clearStudents();
    }

    // ---------------------------------------------------------
    // 6. LOAD ROUTINES FOR SELECTED CLASS, SECTION AND DATE
    // ---------------------------------------------------------

    private void loadRoutines() {

        if (selectedClass.isEmpty()
                || selectedSection.isEmpty()) {
            return;
        }

        resetRoutineSelection();

        StringRequest request = new StringRequest(
                Request.Method.POST,
                GET_ROUTINES_URL,

                response -> {

                    try {

                        JSONObject json = new JSONObject(response);

                        if (!json.optBoolean("success", false)) {

                            showToast(
                                    json.optString(
                                            "message",
                                            "Unable to load routine"
                                    )
                            );

                            return;
                        }

                        JSONArray routines =
                                json.optJSONArray("routines");

                        routineList.clear();

                        timeList.clear();
                        subjectList.clear();

                        timeList.add("Select Time");
                        subjectList.add("Select Subject");

                        if (routines != null) {

                            for (int i = 0;
                                 i < routines.length();
                                 i++) {

                                JSONObject routine =
                                        routines.getJSONObject(i);

                                routineList.add(routine);

                                String start = formatTime(
                                        routine.optString(
                                                "start_time",
                                                ""
                                        )
                                );

                                String end = formatTime(
                                        routine.optString(
                                                "end_time",
                                                ""
                                        )
                                );

                                timeList.add(start + " - " + end);

                                subjectList.add(
                                        routine.optString(
                                                "subject_name",
                                                "Unknown Subject"
                                        )
                                );
                            }
                        }

                        setSpinner(spinnerTime, timeList);
                        setSpinner(spinnerSubject, subjectList);

                        if (routineList.isEmpty()) {

                            showEmptyMessage(
                                    "No routine found for this date"
                            );

                            tvAssignedTeacher.setText(
                                    "Teacher: Not assigned"
                            );
                        }

                    } catch (JSONException e) {

                        Log.e(
                                "AttendanceActivity",
                                "Routine JSON parsing failed",
                                e
                        );

                        showToast("Invalid routine API response");
                    }
                },

                error -> showVolleyError(
                        "Loading routine failed",
                        error
                )
        ) {

            @Override
            public Map<String, String> getHeaders() {
                return authHeaders();
            }

            @Override
            protected Map<String, String> getParams() {

                Map<String, String> params = new HashMap<>();

                params.put("class_name", selectedClass);
                params.put("section", selectedSection);

                params.put(
                        "day_of_week",
                        String.valueOf(getDayOfWeek())
                );

                return params;
            }
        };

        requestQueue.add(request);
    }

    // ---------------------------------------------------------
    // 7. SUBJECT AND TEACHER INFORMATION
    // ---------------------------------------------------------

    private void updateSubjectAndTeacher(JSONObject routine) {

        if (routine == null) {

            suppressRoutineCallback = true;
            spinnerSubject.setSelection(0);
            suppressRoutineCallback = false;

            tvAssignedTeacher.setText("Teacher: Not selected");

            return;
        }

        String teacherName =
                routine.optString(
                        "teacher_name",
                        "Not assigned"
                );

        int routinePosition = routineList.indexOf(routine);

        if (routinePosition >= 0) {

            suppressRoutineCallback = true;

            spinnerSubject.setSelection(routinePosition + 1);

            suppressRoutineCallback = false;
        }

        tvAssignedTeacher.setText(
                "Teacher: " + teacherName
        );
    }

    private String formatTime(String time) {

        if (time == null || time.trim().isEmpty()) {
            return "--:--";
        }

        return time.length() >= 5
                ? time.substring(0, 5)
                : time;
    }

    private void resetRoutineSelection() {

        selectedRoutineId = "";

        routineList.clear();

        timeList.clear();
        timeList.add("Select Time");

        subjectList.clear();
        subjectList.add("Select Subject");

        suppressRoutineCallback = true;

        setSpinner(spinnerTime, timeList);
        setSpinner(spinnerSubject, subjectList);

        suppressRoutineCallback = false;

        tvAssignedTeacher.setText("Teacher: Not selected");
    }

    // ---------------------------------------------------------
    // 8. CLEAR STUDENT LIST
    // ---------------------------------------------------------

    private void clearStudents() {

        studentList.clear();
        attendanceStatus.clear();

        tvStudentCount.setText("Students: 0");

        tvAttendanceHint.setText(
                "Select a scheduled time"
        );

        updateAttendanceCount();

        showEmptyMessage(
                "Select a scheduled time to load students"
        );
    }

    // ---------------------------------------------------------
    // 9. LOAD STUDENTS FOR CLASS AND SECTION
    // ---------------------------------------------------------

    private void loadStudentsByClassAndSection() {

        if (selectedClass.isEmpty()
                || selectedSection.isEmpty()
                || selectedRoutineId.isEmpty()) {
            return;
        }

        studentList.clear();
        attendanceStatus.clear();

        showEmptyMessage("Loading students...");

        tvStudentCount.setText("Loading students...");

        StringRequest request = new StringRequest(
                Request.Method.POST,
                GET_STUDENTS_URL,

                response -> {

                    try {

                        JSONObject json = new JSONObject(response);

                        if (!json.optBoolean("success", false)) {

                            showEmptyMessage(
                                    json.optString(
                                            "message",
                                            "Unable to load students"
                                    )
                            );

                            tvStudentCount.setText("Students: 0");

                            return;
                        }

                        JSONArray students =
                                json.optJSONArray("students");

                        if (students == null
                                || students.length() == 0) {

                            showEmptyMessage(
                                    "No students found for this class and section"
                            );

                            tvStudentCount.setText("Students: 0");

                            updateAttendanceCount();

                            return;
                        }

                        for (int i = 0;
                             i < students.length();
                             i++) {

                            JSONObject item =
                                    students.getJSONObject(i);

                            HashMap<String, String> student =
                                    new HashMap<>();

                            String id = item.optString("id", "");

                            if (id.isEmpty()) {
                                continue;
                            }

                            student.put("id", id);

                            student.put(
                                    "name",
                                    item.optString("name", "")
                            );

                            student.put(
                                    "roll_number",
                                    item.optString("roll_number", "")
                            );

                            student.put(
                                    "class_name",
                                    item.optString("class_name", "")
                            );

                            student.put(
                                    "section",
                                    item.optString("section", "")
                            );

                            studentList.add(student);

                            // New attendance defaults to Absent.
                            attendanceStatus.put(id, "Absent");
                        }

                        if (studentList.isEmpty()) {

                            showEmptyMessage("No valid student records found");

                            tvStudentCount.setText("Students: 0");

                            updateAttendanceCount();

                            return;
                        }

                        loadSavedAttendance();

                    } catch (JSONException e) {

                        Log.e(
                                "AttendanceActivity",
                                "Student JSON parsing failed",
                                e
                        );

                        showEmptyMessage("Invalid student API response");
                    }
                },

                error -> showVolleyError(
                        "Loading students failed",
                        error
                )
        ) {

            @Override
            public Map<String, String> getHeaders() {
                return authHeaders();
            }

            @Override
            protected Map<String, String> getParams() {

                Map<String, String> params = new HashMap<>();

                params.put("class_name", selectedClass);
                params.put("section", selectedSection);

                return params;
            }
        };

        requestQueue.add(request);
    }

    // ---------------------------------------------------------
    // 10. LOAD PREVIOUSLY SAVED ATTENDANCE
    // ---------------------------------------------------------

    private void loadSavedAttendance() {

        StringRequest request = new StringRequest(
                Request.Method.POST,
                GET_ATTENDANCE_URL,

                response -> {

                    try {

                        JSONObject json = new JSONObject(response);

                        JSONArray records =
                                json.optJSONArray("attendance");

                        if (json.optBoolean("success", false)
                                && records != null) {

                            for (int i = 0;
                                 i < records.length();
                                 i++) {

                                JSONObject record =
                                        records.getJSONObject(i);

                                String studentId =
                                        record.optString("student_id");

                                String status =
                                        record.optString(
                                                "status",
                                                "Absent"
                                        );

                                if (studentListContains(studentId)) {

                                    attendanceStatus.put(
                                            studentId,
                                            "Present".equalsIgnoreCase(status)
                                                    ? "Present"
                                                    : "Absent"
                                    );
                                }
                            }
                        }

                        displayStudents();

                    } catch (JSONException e) {

                        displayStudents();
                    }
                },

                error -> {

                    displayStudents();

                    Toast.makeText(
                            this,
                            "Could not load saved attendance",
                            Toast.LENGTH_SHORT
                    ).show();
                }
        ) {

            @Override
            public Map<String, String> getHeaders() {
                return authHeaders();
            }

            @Override
            protected Map<String, String> getParams() {

                Map<String, String> params = new HashMap<>();

                params.put("class_name", selectedClass);
                params.put("section", selectedSection);
                params.put("attendance_date", selectedDate);
                params.put("routine_id", selectedRoutineId);

                return params;
            }
        };

        requestQueue.add(request);
    }

    private boolean studentListContains(String studentId) {

        for (HashMap<String, String> student : studentList) {

            if (studentId.equals(student.get("id"))) {
                return true;
            }
        }

        return false;
    }

    // ---------------------------------------------------------
    // 11. DISPLAY STUDENTS AND ATTENDANCE RADIO BUTTONS
    // ---------------------------------------------------------

    private void displayStudents() {

        llAttendanceList.removeAllViews();

        if (studentList.isEmpty()) {

            showEmptyMessage("No students found");

            tvStudentCount.setText("Students: 0");

            updateAttendanceCount();

            return;
        }

        tvStudentCount.setText(
                "Students: " + studentList.size()
        );

        tvAttendanceHint.setText(
                "Select Present or Absent for each student"
        );

        for (HashMap<String, String> student : studentList) {

            String studentId = student.get("id");

            LinearLayout card = new LinearLayout(this);

            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(24, 20, 24, 20);

            GradientDrawable background = new GradientDrawable();

            background.setColor(Color.WHITE);
            background.setCornerRadius(24);

            background.setStroke(
                    1,
                    Color.rgb(230, 233, 238)
            );

            card.setBackground(background);

            LinearLayout.LayoutParams cardParams =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );

            cardParams.setMargins(0, 0, 0, 18);

            card.setLayoutParams(cardParams);

            TextView name = new TextView(this);

            name.setText(student.get("name"));
            name.setTextSize(18);
            name.setTextColor(Color.rgb(30, 35, 45));

            name.setTypeface(null, Typeface.BOLD);

            card.addView(name);

            TextView roll = new TextView(this);

            roll.setText(
                    "Roll Number: " + student.get("roll_number")
            );

            roll.setTextSize(14);
            roll.setTextColor(Color.rgb(110, 115, 125));

            LinearLayout.LayoutParams rollParams =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );

            rollParams.setMargins(0, 5, 0, 15);

            roll.setLayoutParams(rollParams);

            card.addView(roll);

            TextView title = new TextView(this);

            title.setText("Attendance Status");
            title.setTextSize(14);

            title.setTextColor(Color.rgb(80, 85, 95));
            title.setTypeface(null, Typeface.BOLD);

            card.addView(title);

            RadioGroup group = new RadioGroup(this);

            group.setOrientation(RadioGroup.HORIZONTAL);
            group.setGravity(Gravity.CENTER_VERTICAL);

            RadioButton present = new RadioButton(this);

            present.setText("Present");
            present.setTextSize(14);

            present.setGravity(Gravity.CENTER);
            present.setPadding(12, 12, 12, 12);

            present.setButtonDrawable(null);
            present.setBackgroundResource(R.drawable.bg_present);

            present.setId(View.generateViewId());

            RadioButton absent = new RadioButton(this);

            absent.setText("Absent");
            absent.setTextSize(14);

            absent.setGravity(Gravity.CENTER);
            absent.setPadding(12, 12, 12, 12);

            absent.setButtonDrawable(null);
            absent.setBackgroundResource(R.drawable.bg_absent);

            absent.setId(View.generateViewId());

            LinearLayout.LayoutParams presentParams =
                    new LinearLayout.LayoutParams(0, 55, 1);

            presentParams.setMargins(0, 0, 8, 0);

            present.setLayoutParams(presentParams);

            LinearLayout.LayoutParams absentParams =
                    new LinearLayout.LayoutParams(0, 55, 1);

            absentParams.setMargins(8, 0, 0, 0);

            absent.setLayoutParams(absentParams);

            group.addView(present);
            group.addView(absent);

            card.addView(group);

            String currentStatus =
                    attendanceStatus.get(studentId);

            if ("Present".equals(currentStatus)) {
                group.check(present.getId());
            } else {
                group.check(absent.getId());
            }

            group.setOnCheckedChangeListener(
                    (radioGroup, checkedId) -> {

                        if (checkedId == present.getId()) {

                            attendanceStatus.put(
                                    studentId,
                                    "Present"
                            );

                        } else if (checkedId == absent.getId()) {

                            attendanceStatus.put(
                                    studentId,
                                    "Absent"
                            );
                        }

                        updateAttendanceCount();
                    }
            );

            llAttendanceList.addView(card);
        }

        updateAttendanceCount();
    }

    // ---------------------------------------------------------
    // 12. EMPTY STATE MESSAGE
    // ---------------------------------------------------------

    private void showEmptyMessage(String message) {

        llAttendanceList.removeAllViews();

        TextView emptyText = new TextView(this);

        emptyText.setText(message);
        emptyText.setTextSize(15);

        emptyText.setTextColor(Color.rgb(120, 125, 135));

        emptyText.setGravity(Gravity.CENTER);
        emptyText.setPadding(20, 60, 20, 60);

        llAttendanceList.addView(emptyText);
    }

    // ---------------------------------------------------------
    // 13. UPDATE PRESENT AND ABSENT COUNTS
    // ---------------------------------------------------------

    private void updateAttendanceCount() {

        int presentCount = 0;
        int absentCount = 0;

        for (HashMap<String, String> student : studentList) {

            String status =
                    attendanceStatus.get(student.get("id"));

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

    // ---------------------------------------------------------
    // 14. SAVE ATTENDANCE
    // ---------------------------------------------------------

    private void saveAllAttendance() {

        if (isSaving || studentList.isEmpty()) {
            return;
        }

        isSaving = true;

        btnSaveAttendance.setEnabled(false);
        btnSaveAttendance.setText("Saving...");

        saveAttendanceAtIndex(0);
    }

    private void saveAttendanceAtIndex(int index) {

        if (index >= studentList.size()) {

            isSaving = false;

            btnSaveAttendance.setEnabled(true);
            btnSaveAttendance.setText("Save Attendance");

            Toast.makeText(
                    this,
                    "Attendance saved successfully",
                    Toast.LENGTH_LONG
            ).show();

            loadStudentsByClassAndSection();

            return;
        }

        HashMap<String, String> student =
                studentList.get(index);

        String studentId = student.get("id");

        String status = attendanceStatus.get(studentId);

        if (status == null || status.isEmpty()) {
            status = "Absent";
        }

        final String finalStatus = status;

        StringRequest request = new StringRequest(
                Request.Method.POST,
                MARK_ATTENDANCE_URL,

                response -> {

                    try {

                        JSONObject json = new JSONObject(response);

                        if (json.optBoolean("success", false)) {

                            saveAttendanceAtIndex(index + 1);

                        } else {

                            finishSavingWithError(
                                    json.optString(
                                            "message",
                                            "Failed to save attendance"
                                    )
                            );
                        }

                    } catch (JSONException e) {

                        finishSavingWithError(
                                "Invalid server response"
                        );
                    }
                },

                error -> finishSavingWithError(
                        buildVolleyErrorMessage(
                                "Attendance save failed",
                                error
                        )
                )
        ) {

            @Override
            public Map<String, String> getHeaders() {
                return authHeaders();
            }

            @Override
            protected Map<String, String> getParams() {

                Map<String, String> params = new HashMap<>();

                params.put("student_id", studentId);
                params.put("attendance_date", selectedDate);
                params.put("status", finalStatus);

                params.put("routine_id", selectedRoutineId);
                params.put("class_name", selectedClass);
                params.put("section", selectedSection);

                return params;
            }
        };

        requestQueue.add(request);
    }

    private void finishSavingWithError(String message) {

        isSaving = false;

        btnSaveAttendance.setEnabled(true);
        btnSaveAttendance.setText("Save Attendance");

        showToast(message);
    }

    // ---------------------------------------------------------
    // 15. VOLLEY ERROR HANDLING
    // ---------------------------------------------------------

    private String buildVolleyErrorMessage(
            String operation,
            VolleyError error
    ) {

        int statusCode = -1;
        String responseBody = "";

        if (error.networkResponse != null) {

            statusCode = error.networkResponse.statusCode;

            byte[] data = error.networkResponse.data;

            if (data != null) {

                responseBody = new String(
                        data,
                        StandardCharsets.UTF_8
                );
            }
        }

        String message = operation;

        if (statusCode != -1) {
            message += " (HTTP " + statusCode + ")";
        }

        if (!responseBody.trim().isEmpty()) {

            try {

                JSONObject json = new JSONObject(responseBody);

                String serverMessage =
                        json.optString("message", "");

                message += ": " + (
                        serverMessage.isEmpty()
                                ? responseBody
                                : serverMessage
                );

            } catch (JSONException e) {

                message += ": " + responseBody;
            }

        } else if (error.getMessage() != null) {

            message += ": " + error.getMessage();
        }

        Log.e(
                "AttendanceActivity",
                message,
                error
        );

        return message;
    }

    private void showVolleyError(
            String operation,
            VolleyError error
    ) {

        showToast(
                buildVolleyErrorMessage(operation, error)
        );
    }

    private void showToast(String message) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    // ---------------------------------------------------------
    // 16. ACTIVITY CLEANUP
    // ---------------------------------------------------------

    @Override
    protected void onDestroy() {

        if (requestQueue != null) {
            requestQueue.cancelAll(request -> true);
        }

        super.onDestroy();
    }
}