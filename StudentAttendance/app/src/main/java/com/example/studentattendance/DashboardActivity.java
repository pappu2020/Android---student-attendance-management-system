package com.example.studentattendance;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DashboardActivity extends AppCompatActivity {

    private static final String BASE_URL =
            "http://10.0.2.2/student_attendance_api/";

    private static final String DATE_STATS_URL =
            BASE_URL + "get_attendance_by_date.php";

    private static final String WEEKLY_URL =
            BASE_URL + "get_weekly_attendance.php";

    private static final String TEACHER_ROUTINE_URL =
            BASE_URL + "get_teacher_daily_routine.php";

    private static final String PREFS_NAME =
            "StudentAttendanceAuth";

    private SwipeRefreshLayout swipeRefreshDashboard;

    private BarChart chartAttendance;
    private BarChart chartWeeklyAttendance;

    private TextView tvDashboardTitle;
    private TextView tvWelcome;
    private TextView tvRole;

    private TextView tvStatTotalStudents;
    private TextView tvStatPresent;
    private TextView tvStatAbsent;
    private TextView tvStatNotMarked;
    private TextView tvStatPercentage;
    private TextView tvSelectedAttendanceDate;

    private Button btnSelectAttendanceDate;
    private Button btnLogout;

    private LinearLayout btnStudents;

    // Added: Admin-only Teacher Management
    private LinearLayout btnManageTeachers;

    // Admin-only: Students below 75% attendance
    private LinearLayout btnLowAttendance;

    private LinearLayout btnStudentList;
    private LinearLayout btnAttendance;
    private LinearLayout btnHistory;
    private LinearLayout btnReport;
    private LinearLayout btnTimetable;

    // Teacher Daily Routine views
    private LinearLayout layoutTeacherRoutine;
    private TextView tvRoutineSelectedDay;
    private Spinner spinnerRoutineDay;
    private Button btnLoadTeacherRoutine;
    private ProgressBar progressTeacherRoutine;
    private TextView tvTeacherRoutineMessage;
    private LinearLayout containerTeacherRoutine;



    private final String[] routineDayNames = {
            "Monday",
            "Tuesday",
            "Wednesday",
            "Thursday",
            "Friday",
            "Saturday",
            "Sunday"
    };

    private String selectedDate;
    private String authToken = "";
    private String currentUserRole = "";
    private String currentUserName = "";

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    private int pendingRequests = 0;
    private int dateRequestVersion = 0;
    private int routineRequestVersion = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_dashboard);

        initializeViews();
        loadUserSession();

        if (!isValidSession()) {
            redirectToLogin();
            return;
        }

        displayUserInformation(
                currentUserName,
                currentUserRole
        );

        applyRolePermissions();
        setupMenuListeners();
        setupCharts();
        setupDatePicker();
        setupTeacherDailyRoutine();

        swipeRefreshDashboard.setOnRefreshListener(() -> {
            loadSelectedDateStats();
            loadWeeklyAttendance();

            if (isTeacher()) {
                loadTeacherRoutine();
            }
        });
    }

    // ------------------------------------------------------------
    // INITIALIZE VIEWS
    // ------------------------------------------------------------

    private void initializeViews() {

        swipeRefreshDashboard =
                findViewById(R.id.swipeRefreshDashboard);

        swipeRefreshDashboard.setColorSchemeResources(
                android.R.color.holo_blue_dark,
                android.R.color.holo_green_dark,
                android.R.color.holo_orange_dark
        );

        chartAttendance =
                findViewById(R.id.chartAttendance);

        chartWeeklyAttendance =
                findViewById(R.id.chartWeeklyAttendance);

        tvDashboardTitle =
                findViewById(R.id.tvDashboardTitle);

        tvWelcome =
                findViewById(R.id.tvWelcome);

        tvRole =
                findViewById(R.id.tvRole);

        tvStatTotalStudents =
                findViewById(R.id.tvStatTotalStudents);

        tvStatPresent =
                findViewById(R.id.tvStatPresent);

        tvStatAbsent =
                findViewById(R.id.tvStatAbsent);

        tvStatNotMarked =
                findViewById(R.id.tvStatNotMarked);

        tvStatPercentage =
                findViewById(R.id.tvStatPercentage);

        tvSelectedAttendanceDate =
                findViewById(R.id.tvSelectedAttendanceDate);

        btnSelectAttendanceDate =
                findViewById(R.id.btnSelectAttendanceDate);

        btnStudents =
                findViewById(R.id.btnStudents);

        // Added: Teacher Management view
        btnManageTeachers =
                findViewById(R.id.btnManageTeachers);

        btnLowAttendance =
                findViewById(R.id.btnLowAttendance);

        btnStudentList =
                findViewById(R.id.btnStudentList);

        btnAttendance =
                findViewById(R.id.btnAttendance);

        btnHistory =
                findViewById(R.id.btnHistory);

        btnReport =
                findViewById(R.id.btnReport);

        btnTimetable =
                findViewById(R.id.btnTimetable);

        btnLogout =
                findViewById(R.id.btnLogout);

        // Teacher Daily Routine XML IDs
        layoutTeacherRoutine =
                findViewById(R.id.layoutTeacherRoutine);

        tvRoutineSelectedDay =
                findViewById(R.id.tvRoutineSelectedDay);

        spinnerRoutineDay =
                findViewById(R.id.spinnerRoutineDay);

        btnLoadTeacherRoutine =
                findViewById(R.id.btnLoadTeacherRoutine);

        progressTeacherRoutine =
                findViewById(R.id.progressTeacherRoutine);

        tvTeacherRoutineMessage =
                findViewById(R.id.tvTeacherRoutineMessage);

        containerTeacherRoutine =
                findViewById(R.id.containerTeacherRoutine);



    }

    // ------------------------------------------------------------
    // SESSION MANAGEMENT
    // ------------------------------------------------------------

    private void loadUserSession() {

        SharedPreferences preferences =
                getSharedPreferences(
                        PREFS_NAME,
                        MODE_PRIVATE
                );

        authToken = preferences.getString(
                "auth_token",
                ""
        );

        currentUserRole = preferences.getString(
                "user_role",
                ""
        );

        currentUserName = preferences.getString(
                "user_name",
                ""
        );

        if (authToken == null) {
            authToken = "";
        }

        if (currentUserRole == null) {
            currentUserRole = "";
        }

        if (currentUserName == null) {
            currentUserName = "";
        }

        currentUserRole =
                currentUserRole.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isValidSession() {

        return !authToken.trim().isEmpty()
                && (
                "admin".equals(currentUserRole)
                        || "teacher".equals(currentUserRole)
        );
    }

    private boolean isAdmin() {
        return "admin".equals(currentUserRole);
    }

    private boolean isTeacher() {
        return "teacher".equals(currentUserRole);
    }

    private boolean hasValidSession() {

        if (!isValidSession()) {
            redirectToLogin();
            return false;
        }

        return true;
    }

    private void displayUserInformation(
            String userName,
            String userRole
    ) {

        String validName =
                getValidText(userName, "User");

        tvWelcome.setText("Welcome, " + validName);

        if ("admin".equals(userRole)) {
            tvRole.setText("Role: Admin");
            tvDashboardTitle.setText("Admin Dashboard");
        } else {
            tvRole.setText("Role: Teacher");
            tvDashboardTitle.setText("Teacher Dashboard");
        }
    }

    // ------------------------------------------------------------
    // ROLE-BASED PERMISSIONS
    // ------------------------------------------------------------

    private void applyRolePermissions() {

        btnStudents.setVisibility(
                isAdmin() ? View.VISIBLE : View.GONE
        );

        // Only Admin can manage teacher profiles.
        btnManageTeachers.setVisibility(
                isAdmin() ? View.VISIBLE : View.GONE
        );


        // Only Admin can access low-attendance students.
        btnLowAttendance.setVisibility(
                isAdmin() ? View.VISIBLE : View.GONE
        );


        btnStudentList.setVisibility(View.VISIBLE);
        btnAttendance.setVisibility(View.VISIBLE);
        btnHistory.setVisibility(View.VISIBLE);
        btnReport.setVisibility(View.VISIBLE);
        btnTimetable.setVisibility(View.VISIBLE);

        // Daily routine is only for teachers.
        layoutTeacherRoutine.setVisibility(
                isTeacher() ? View.VISIBLE : View.GONE
        );
    }

    // ------------------------------------------------------------
    // NAVIGATION AND LOGOUT
    // ------------------------------------------------------------

    private void setupMenuListeners() {

        // Student Management: Admin only
        btnStudents.setOnClickListener(view -> {

            if (!isAdmin()) {
                showPermissionDenied();
                return;
            }

            if (!hasValidSession()) {
                return;
            }

            startActivity(new Intent(
                    DashboardActivity.this,
                    StudentActivity.class
            ));
        });

        // Teacher Management: Admin only
        btnManageTeachers.setOnClickListener(view -> {

            if (!hasValidSession()) {
                return;
            }

            if (!isAdmin()) {
                showPermissionDenied();
                return;
            }

            startActivity(new Intent(
                    DashboardActivity.this,
                    TeacherListActivity.class
            ));
        });


        // Below 75% Attendance: Admin only
        btnLowAttendance.setOnClickListener(view -> {

            if (!hasValidSession()) {
                return;
            }

            if (!isAdmin()) {
                showPermissionDenied();
                return;
            }

            startActivity(new Intent(
                    DashboardActivity.this,
                    ClassWiseLowAttendanceActivity.class
            ));
        });

        btnStudentList.setOnClickListener(view -> {

            if (!hasValidSession()) {
                return;
            }

            startActivity(new Intent(
                    DashboardActivity.this,
                    StudentListActivity.class
            ));
        });

        btnAttendance.setOnClickListener(view -> {

            if (!hasValidSession()) {
                return;
            }

            startActivity(new Intent(
                    DashboardActivity.this,
                    AttendanceActivity.class
            ));
        });

        btnHistory.setOnClickListener(view -> {

            if (!hasValidSession()) {
                return;
            }

            startActivity(new Intent(
                    DashboardActivity.this,
                    AttendanceRecord.class
            ));
        });

        btnReport.setOnClickListener(view -> {

            if (!hasValidSession()) {
                return;
            }

            Intent intent = new Intent(
                    DashboardActivity.this,
                    MonthlyAttendanceReportActivity.class
            );

            intent.putExtra(
                    "user_name",
                    currentUserName
            );

            intent.putExtra(
                    "user_role",
                    currentUserRole
            );

            startActivity(intent);
        });

        btnTimetable.setOnClickListener(view -> {

            if (!hasValidSession()) {
                return;
            }

            startActivity(new Intent(
                    DashboardActivity.this,
                    TimetableActivity.class
            ));
        });

        btnSelectAttendanceDate.setOnClickListener(
                view -> showDatePicker()
        );

        btnLogout.setOnClickListener(
                view -> logoutUser()
        );
    }

    private void showPermissionDenied() {

        Toast.makeText(
                this,
                "Admin permission required.",
                Toast.LENGTH_SHORT
        ).show();
    }

    private void logoutUser() {

        SharedPreferences preferences =
                getSharedPreferences(
                        PREFS_NAME,
                        MODE_PRIVATE
                );

        preferences.edit().clear().apply();

        authToken = "";
        currentUserRole = "";
        currentUserName = "";

        Toast.makeText(
                this,
                "Logged out successfully",
                Toast.LENGTH_SHORT
        ).show();

        redirectToLogin();
    }

    private void redirectToLogin() {

        Intent intent = new Intent(
                DashboardActivity.this,
                MainActivity.class
        );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    // ------------------------------------------------------------
    // ATTENDANCE DATE PICKER
    // ------------------------------------------------------------

    private String formatDate(Date date) {

        return new SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.US
        ).format(date);
    }

    private void setupDatePicker() {

        selectedDate = formatDate(new Date());

        updateSelectedDateLabel();

        loadSelectedDateStats();
        loadWeeklyAttendance();
    }

    private void showDatePicker() {

        Calendar calendar = Calendar.getInstance();

        try {
            Date parsed = new SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.US
            ).parse(selectedDate);

            if (parsed != null) {
                calendar.setTime(parsed);
            }

        } catch (Exception ignored) {
            // Use today's date if parsing fails.
        }

        DatePickerDialog dialog = new DatePickerDialog(
                DashboardActivity.this,
                (datePicker, year, month, day) -> {

                    Calendar chosen = Calendar.getInstance();

                    chosen.set(
                            year,
                            month,
                            day,
                            0,
                            0,
                            0
                    );

                    chosen.set(
                            Calendar.MILLISECOND,
                            0
                    );

                    selectedDate =
                            formatDate(chosen.getTime());

                    updateSelectedDateLabel();
                    loadSelectedDateStats();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        dialog.getDatePicker().setMaxDate(
                System.currentTimeMillis()
        );

        dialog.show();
    }

    private void updateSelectedDateLabel() {

        tvSelectedAttendanceDate.setText(
                "Selected Date: " + selectedDate
        );
    }

    // ------------------------------------------------------------
    // CHART CONFIGURATION
    // ------------------------------------------------------------

    private void setupCharts() {

        chartAttendance.setNoDataText(
                "Waiting for attendance data..."
        );

        chartAttendance.getDescription().setEnabled(false);
        chartAttendance.getLegend().setEnabled(false);
        chartAttendance.getAxisRight().setEnabled(false);
        chartAttendance.getAxisLeft().setAxisMinimum(0f);

        chartAttendance.getXAxis().setPosition(
                XAxis.XAxisPosition.BOTTOM
        );

        chartAttendance.getXAxis().setGranularity(1f);
        chartAttendance.getXAxis().setDrawGridLines(false);
        chartAttendance.setFitBars(true);

        chartWeeklyAttendance.setNoDataText(
                "Waiting for weekly attendance data..."
        );

        chartWeeklyAttendance.getDescription().setEnabled(false);
        chartWeeklyAttendance.getAxisRight().setEnabled(false);
        chartWeeklyAttendance.getAxisLeft().setAxisMinimum(0f);

        chartWeeklyAttendance.getXAxis().setPosition(
                XAxis.XAxisPosition.BOTTOM
        );

        chartWeeklyAttendance.getXAxis().setGranularity(1f);
        chartWeeklyAttendance.getXAxis().setDrawGridLines(false);
        chartWeeklyAttendance.getLegend().setEnabled(true);
    }

    // ------------------------------------------------------------
    // DAILY ATTENDANCE STATISTICS
    // ------------------------------------------------------------

    private void loadSelectedDateStats() {

        if (isFinishing() || isDestroyed()) {
            return;
        }

        if (!hasValidSession()) {
            return;
        }

        final String requestedDate = selectedDate;
        final int requestVersion = ++dateRequestVersion;

        setStatisticsLoading();
        beginRequest();

        executor.execute(() -> {

            String response = fetchPostWithParameter(
                    DATE_STATS_URL,
                    "attendance_date",
                    requestedDate
            );

            mainHandler.post(() -> {

                if (isFinishing() || isDestroyed()) {
                    finishRequest();
                    return;
                }

                if (requestVersion == dateRequestVersion) {
                    displayDateStats(
                            response,
                            requestedDate
                    );
                }

                finishRequest();
            });
        });
    }

    private void displayDateStats(
            String response,
            String requestedDate
    ) {

        if (response == null || response.trim().isEmpty()) {
            showStatsError("Empty response from server.");
            return;
        }

        if (response.startsWith("ERROR:")) {
            showStatsError(
                    "Unable to load attendance: "
                            + response.substring(6).trim()
            );
            return;
        }

        try {
            JSONObject json = new JSONObject(response);

            if (!json.optBoolean("success", false)) {
                showStatsError(
                        json.optString(
                                "message",
                                "Failed to load attendance statistics."
                        )
                );
                return;
            }

            int totalStudents = Math.max(
                    0,
                    json.optInt("total_students", 0)
            );

            int present = Math.max(
                    0,
                    json.optInt("present_count", 0)
            );

            int absent = Math.max(
                    0,
                    json.optInt("absent_count", 0)
            );

            int notMarked = Math.max(
                    0,
                    json.optInt("not_marked_count", 0)
            );

            double percentage = json.optDouble(
                    "attendance_percentage",
                    0.0
            );

            percentage = Math.max(
                    0.0,
                    Math.min(100.0, percentage)
            );

            tvStatTotalStudents.setText(
                    String.valueOf(totalStudents)
            );

            tvStatPresent.setText(
                    String.valueOf(present)
            );

            tvStatAbsent.setText(
                    String.valueOf(absent)
            );

            tvStatNotMarked.setText(
                    String.valueOf(notMarked)
            );

            tvStatPercentage.setText(
                    String.format(
                            Locale.getDefault(),
                            "%.1f%%",
                            percentage
                    )
            );

            updateDailyChart(
                    present,
                    absent,
                    requestedDate
            );

        } catch (Exception e) {
            showStatsError("Invalid attendance response.");
        }
    }

    private void setStatisticsLoading() {

        tvStatTotalStudents.setText("...");
        tvStatPresent.setText("...");
        tvStatAbsent.setText("...");
        tvStatNotMarked.setText("...");
        tvStatPercentage.setText("...");
    }

    private void updateDailyChart(
            int present,
            int absent,
            String date
    ) {

        ArrayList<BarEntry> entries = new ArrayList<>();

        entries.add(
                new BarEntry(0f, Math.max(0, present))
        );

        entries.add(
                new BarEntry(1f, Math.max(0, absent))
        );

        BarDataSet dataSet =
                new BarDataSet(entries, "Students");

        dataSet.setColors(
                Color.rgb(46, 125, 50),
                Color.rgb(198, 40, 40)
        );

        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.DKGRAY);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.5f);

        chartAttendance.setData(data);

        chartAttendance.getXAxis().setValueFormatter(
                new IndexAxisValueFormatter(
                        Arrays.asList("Present", "Absent")
                )
        );

        chartAttendance.getXAxis().setAxisMinimum(-0.5f);
        chartAttendance.getXAxis().setAxisMaximum(1.5f);
        chartAttendance.getXAxis().setLabelCount(2);
        chartAttendance.getXAxis().setGranularity(1f);

        Description description = new Description();
        description.setText("Date: " + date);

        chartAttendance.setDescription(description);
        chartAttendance.getLegend().setEnabled(false);
        chartAttendance.setFitBars(true);
        chartAttendance.notifyDataSetChanged();
        chartAttendance.invalidate();
    }


    // ------------------------------------------------------------
    // WEEKLY ATTENDANCE ANALYTICS
    // ------------------------------------------------------------

    private void loadWeeklyAttendance() {

        if (isFinishing() || isDestroyed()) {
            return;
        }

        if (!hasValidSession()) {
            return;
        }

        beginRequest();

        executor.execute(() -> {

            String response = fetchPost(WEEKLY_URL);

            mainHandler.post(() -> {

                if (isFinishing() || isDestroyed()) {
                    finishRequest();
                    return;
                }

                displayWeeklyAttendance(response);
                finishRequest();
            });
        });
    }

    private void displayWeeklyAttendance(String response) {

        if (response == null
                || response.trim().isEmpty()
                || response.startsWith("ERROR:")) {

            showWeeklyChartError(
                    "Unable to load weekly attendance."
            );
            return;
        }

        try {
            JSONObject json = new JSONObject(response);

            if (!json.optBoolean("success", false)) {
                showWeeklyChartError(
                        json.optString(
                                "message",
                                "Weekly attendance unavailable."
                        )
                );
                return;
            }

            JSONArray weeklyData =
                    json.optJSONArray("weekly_data");

            if (weeklyData == null || weeklyData.length() == 0) {
                showWeeklyChartError(
                        "No weekly attendance data available."
                );
                return;
            }

            ArrayList<BarEntry> presentEntries =
                    new ArrayList<>();

            ArrayList<BarEntry> absentEntries =
                    new ArrayList<>();

            ArrayList<String> labels =
                    new ArrayList<>();

            for (int i = 0; i < weeklyData.length(); i++) {

                JSONObject item =
                        weeklyData.optJSONObject(i);

                if (item == null) {
                    continue;
                }

                labels.add(item.optString("day", ""));

                presentEntries.add(
                        new BarEntry(
                                labels.size() - 1,
                                Math.max(
                                        0,
                                        item.optInt("present_count", 0)
                                )
                        )
                );

                absentEntries.add(
                        new BarEntry(
                                labels.size() - 1,
                                Math.max(
                                        0,
                                        item.optInt("absent_count", 0)
                                )
                        )
                );
            }

            if (labels.isEmpty()) {
                showWeeklyChartError(
                        "No valid weekly attendance records."
                );
                return;
            }

            BarDataSet presentSet =
                    new BarDataSet(presentEntries, "Present");

            presentSet.setColor(Color.rgb(46, 125, 50));
            presentSet.setValueTextSize(9f);

            BarDataSet absentSet =
                    new BarDataSet(absentEntries, "Absent");

            absentSet.setColor(Color.rgb(198, 40, 40));
            absentSet.setValueTextSize(9f);

            BarData data =
                    new BarData(presentSet, absentSet);

            float groupSpace = 0.30f;
            float barSpace = 0.05f;
            float barWidth = 0.30f;

            data.setBarWidth(barWidth);

            chartWeeklyAttendance.setData(data);

            XAxis xAxis =
                    chartWeeklyAttendance.getXAxis();

            xAxis.setValueFormatter(
                    new IndexAxisValueFormatter(labels)
            );

            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setGranularity(1f);
            xAxis.setGranularityEnabled(true);
            xAxis.setCenterAxisLabels(true);
            xAxis.setDrawGridLines(false);
            xAxis.setAxisMinimum(0f);
            xAxis.setAxisMaximum(labels.size());

            chartWeeklyAttendance
                    .getAxisLeft()
                    .setAxisMinimum(0f);

            chartWeeklyAttendance
                    .getAxisRight()
                    .setEnabled(false);

            chartWeeklyAttendance
                    .getDescription()
                    .setEnabled(false);

            chartWeeklyAttendance
                    .getLegend()
                    .setEnabled(true);

            chartWeeklyAttendance.groupBars(
                    0f,
                    groupSpace,
                    barSpace
            );

            chartWeeklyAttendance.notifyDataSetChanged();
            chartWeeklyAttendance.invalidate();

        } catch (Exception e) {
            showWeeklyChartError(
                    "Invalid weekly attendance response."
            );
        }
    }

    // ------------------------------------------------------------
    // TEACHER DAILY ROUTINE
    // ------------------------------------------------------------

    private void setupTeacherDailyRoutine() {

        if (layoutTeacherRoutine == null) {
            return;
        }

        layoutTeacherRoutine.setVisibility(
                isTeacher() ? View.VISIBLE : View.GONE
        );

        if (!isTeacher()) {
            return;
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                routineDayNames
        );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerRoutineDay.setAdapter(adapter);

        // Calendar.DAY_OF_WEEK: Sunday=1, Monday=2 ... Saturday=7.
        // Convert to Monday-first spinner position.
        Calendar calendar = Calendar.getInstance();
        int calendarDay = calendar.get(Calendar.DAY_OF_WEEK);
        int mondayFirstIndex = (calendarDay + 5) % 7;

        spinnerRoutineDay.setSelection(mondayFirstIndex);

        updateRoutineDayLabel();

        spinnerRoutineDay.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {
                        updateRoutineDayLabel();
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent
                    ) {
                        // Nothing to do.
                    }
                }
        );

        btnLoadTeacherRoutine.setOnClickListener(
                view -> loadTeacherRoutine()
        );

        // Load today's routine automatically.
        loadTeacherRoutine();
    }

    private void updateRoutineDayLabel() {

        if (spinnerRoutineDay == null
                || tvRoutineSelectedDay == null) {
            return;
        }

        Object selected = spinnerRoutineDay.getSelectedItem();

        String day = selected == null
                ? "Monday"
                : selected.toString();

        tvRoutineSelectedDay.setText(
                "Selected day: " + day
        );
    }

    private void loadTeacherRoutine() {

        if (!isTeacher() || !hasValidSession()) {
            return;
        }

        int position =
                spinnerRoutineDay.getSelectedItemPosition();

        // PHP API convention: Monday=1 ... Sunday=7.
        final int dayOfWeek = position + 1;
        final int requestVersion = ++routineRequestVersion;

        progressTeacherRoutine.setVisibility(View.VISIBLE);
        btnLoadTeacherRoutine.setEnabled(false);

        tvTeacherRoutineMessage.setText("Loading routine...");
        containerTeacherRoutine.removeAllViews();

        beginRequest();

        executor.execute(() -> {

            String response = fetchPostWithParameter(
                    TEACHER_ROUTINE_URL,
                    "day_of_week",
                    String.valueOf(dayOfWeek)
            );

            mainHandler.post(() -> {

                if (isFinishing() || isDestroyed()) {
                    finishRequest();
                    return;
                }

                if (requestVersion == routineRequestVersion) {

                    progressTeacherRoutine.setVisibility(View.GONE);
                    btnLoadTeacherRoutine.setEnabled(true);

                    displayTeacherRoutine(response);
                }

                finishRequest();
            });
        });
    }

    private void displayTeacherRoutine(String response) {

        containerTeacherRoutine.removeAllViews();

        if (response == null || response.trim().isEmpty()) {
            tvTeacherRoutineMessage.setText(
                    "No response received from server."
            );
            return;
        }

        if (response.startsWith("ERROR:")) {
            tvTeacherRoutineMessage.setText(
                    response.substring(6).trim()
            );
            return;
        }

        try {
            JSONObject json = new JSONObject(response);

            if (!json.optBoolean("success", false)) {
                tvTeacherRoutineMessage.setText(
                        json.optString(
                                "message",
                                "Unable to load routine."
                        )
                );
                return;
            }

            JSONArray routines =
                    json.optJSONArray("routines");

            if (routines == null || routines.length() == 0) {
                tvTeacherRoutineMessage.setText(
                        "No classes scheduled for this day."
                );
                return;
            }

            int displayedCount = 0;

            for (int i = 0; i < routines.length(); i++) {

                JSONObject item =
                        routines.optJSONObject(i);

                if (item == null) {
                    continue;
                }

                String subject = item.optString(
                        "subject_name",
                        "Subject"
                );

                String className = item.optString(
                        "class_name",
                        "Class"
                );

                String section = item.optString(
                        "section",
                        ""
                );

                String start = formatRoutineTime(
                        item.optString("start_time", "")
                );

                String end = formatRoutineTime(
                        item.optString("end_time", "")
                );

                addTeacherRoutineCard(
                        subject,
                        className,
                        section,
                        start,
                        end
                );

                displayedCount++;
            }

            if (displayedCount == 0) {
                tvTeacherRoutineMessage.setText(
                        "No valid routine records were returned."
                );
            } else {
                tvTeacherRoutineMessage.setText(
                        displayedCount
                                + (displayedCount == 1
                                ? " class scheduled."
                                : " classes scheduled.")
                );
            }

        } catch (Exception e) {
            tvTeacherRoutineMessage.setText(
                    "Invalid routine response from server."
            );
        }
    }

    private void addTeacherRoutineCard(
            String subject,
            String className,
            String section,
            String start,
            String end
    ) {

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackgroundColor(
                Color.rgb(239, 246, 255)
        );

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.setMargins(
                0,
                dp(6),
                0,
                dp(6)
        );

        card.setLayoutParams(cardParams);

        TextView subjectView = new TextView(this);
        subjectView.setText(
                getValidText(subject, "Subject")
        );
        subjectView.setTextColor(
                Color.rgb(21, 101, 192)
        );
        subjectView.setTextSize(17f);
        subjectView.setTypeface(
                null,
                Typeface.BOLD
        );

        card.addView(subjectView);

        String classSection =
                "Class: " + getValidText(className, "N/A");

        if (section != null
                && !section.trim().isEmpty()
                && !"null".equalsIgnoreCase(section.trim())) {

            classSection += "   |   Section: " + section.trim();
        }

        TextView classView = new TextView(this);
        classView.setText(classSection);
        classView.setTextColor(
                Color.rgb(55, 71, 79)
        );
        classView.setTextSize(14f);

        LinearLayout.LayoutParams classParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        classParams.topMargin = dp(5);
        classView.setLayoutParams(classParams);

        card.addView(classView);

        TextView timeView = new TextView(this);

        String time =
                (start.isEmpty() ? "--:--" : start)
                        + " - "
                        + (end.isEmpty() ? "--:--" : end);

        timeView.setText("Time: " + time);
        timeView.setTextColor(
                Color.rgb(69, 90, 100)
        );
        timeView.setTextSize(14f);

        LinearLayout.LayoutParams timeParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        timeParams.topMargin = dp(4);
        timeView.setLayoutParams(timeParams);

        card.addView(timeView);

        containerTeacherRoutine.addView(card);
    }

    private String formatRoutineTime(String value) {

        if (value == null) {
            return "";
        }

        String time = value.trim();

        if (time.isEmpty()
                || "null".equalsIgnoreCase(time)) {
            return "";
        }

        try {
            SimpleDateFormat input =
                    new SimpleDateFormat(
                            "HH:mm:ss",
                            Locale.US
                    );

            input.setLenient(false);

            Date parsed;

            try {
                parsed = input.parse(time);
            } catch (Exception firstError) {

                input = new SimpleDateFormat(
                        "HH:mm",
                        Locale.US
                );

                input.setLenient(false);
                parsed = input.parse(time);
            }

            if (parsed != null) {
                return new SimpleDateFormat(
                        "hh:mm a",
                        Locale.getDefault()
                ).format(parsed);
            }

        } catch (Exception ignored) {
            // Keep original server time if parsing fails.
        }

        return time;
    }

    private int dp(int value) {

        return (int) (
                value
                        * getResources()
                        .getDisplayMetrics()
                        .density
                        + 0.5f
        );
    }


    // ------------------------------------------------------------
    // HTTP POST HELPERS
    // ------------------------------------------------------------

    private String fetchPost(String endpoint) {
        return sendPost(endpoint, "");
    }

    private String fetchPostWithParameter(
            String endpoint,
            String key,
            String value
    ) {

        try {
            String body =
                    URLEncoder.encode(key, "UTF-8")
                            + "="
                            + URLEncoder.encode(value, "UTF-8");

            return sendPost(endpoint, body);

        } catch (Exception e) {
            return "ERROR: Unable to prepare request.";
        }
    }

    private String sendPost(
            String endpoint,
            String body
    ) {

        HttpURLConnection connection = null;

        try {
            URL url = new URL(endpoint);

            connection =
                    (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            connection.setRequestProperty(
                    "Content-Type",
                    "application/x-www-form-urlencoded; charset=UTF-8"
            );

            connection.setRequestProperty(
                    "Accept",
                    "application/json"
            );

            connection.setRequestProperty(
                    "Authorization",
                    "Bearer " + authToken
            );

            try (OutputStream output =
                         connection.getOutputStream()) {

                output.write(
                        body.getBytes(StandardCharsets.UTF_8)
                );

                output.flush();
            }

            int responseCode =
                    connection.getResponseCode();

            InputStream input =
                    (responseCode >= 200 && responseCode < 300)
                            ? connection.getInputStream()
                            : connection.getErrorStream();

            if (input == null) {
                return "ERROR: Empty server response. HTTP "
                        + responseCode;
            }

            StringBuilder response =
                    new StringBuilder();

            try (BufferedReader reader =
                         new BufferedReader(
                                 new InputStreamReader(
                                         input,
                                         StandardCharsets.UTF_8
                                 )
                         )) {

                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            if (responseCode
                    == HttpURLConnection.HTTP_UNAUTHORIZED) {

                mainHandler.post(() -> {

                    if (!isFinishing() && !isDestroyed()) {

                        Toast.makeText(
                                DashboardActivity.this,
                                "Session expired. Please log in again.",
                                Toast.LENGTH_LONG
                        ).show();

                        redirectToLogin();
                    }
                });

                return "ERROR: HTTP 401: Authentication required";
            }

            if (responseCode
                    == HttpURLConnection.HTTP_FORBIDDEN) {

                return "ERROR: HTTP 403: Permission denied";
            }

            if (responseCode < 200 || responseCode >= 300) {

                return "ERROR: HTTP "
                        + responseCode
                        + ": "
                        + response;
            }

            return response.toString();

        } catch (SocketTimeoutException e) {

            return "ERROR: Server timed out. Please try again.";

        } catch (UnknownHostException e) {

            return "ERROR: Cannot connect to server.";

        } catch (Exception e) {

            return "ERROR: Unable to connect to the server.";

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    // ------------------------------------------------------------
    // ERROR HANDLING
    // ------------------------------------------------------------

    private void showStatsError(String message) {

        tvStatTotalStudents.setText("--");
        tvStatPresent.setText("--");
        tvStatAbsent.setText("--");
        tvStatNotMarked.setText("--");
        tvStatPercentage.setText("--%");

        chartAttendance.clear();

        chartAttendance.setNoDataText(
                "Daily attendance unavailable"
        );

        chartAttendance.invalidate();

        Toast.makeText(
                DashboardActivity.this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    private void showWeeklyChartError(String message) {

        chartWeeklyAttendance.clear();
        chartWeeklyAttendance.setNoDataText(message);
        chartWeeklyAttendance.invalidate();
    }

    // ------------------------------------------------------------
    // PULL-TO-REFRESH REQUEST COUNTER
    // ------------------------------------------------------------

    private void beginRequest() {

        pendingRequests++;

        if (swipeRefreshDashboard != null) {
            swipeRefreshDashboard.setRefreshing(true);
        }
    }

    private void finishRequest() {

        pendingRequests = Math.max(
                0,
                pendingRequests - 1
        );

        if (swipeRefreshDashboard != null) {
            swipeRefreshDashboard.setRefreshing(
                    pendingRequests > 0
            );
        }
    }

    // ------------------------------------------------------------
    // COMMON HELPERS AND LIFECYCLE
    // ------------------------------------------------------------

    private String getValidText(
            String value,
            String fallback
    ) {

        if (value == null
                || value.trim().isEmpty()
                || value.trim().equalsIgnoreCase("null")) {

            return fallback;
        }

        return value.trim();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (swipeRefreshDashboard != null
                && isValidSession()
                && !isFinishing()
                && !isDestroyed()) {

            loadSelectedDateStats();
            loadWeeklyAttendance();

            if (isTeacher()) {
                loadTeacherRoutine();
            }
        }
    }

    @Override
    protected void onDestroy() {

        executor.shutdownNow();

        super.onDestroy();
    }
}