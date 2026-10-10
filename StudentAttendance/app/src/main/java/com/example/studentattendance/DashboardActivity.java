
package com.example.studentattendance;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.LinearLayout;
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
import java.net.URL;
import java.net.URLEncoder;
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
    private LinearLayout btnStudentList;
    private LinearLayout btnAttendance;
    private LinearLayout btnHistory;
    private LinearLayout btnReport;
    private LinearLayout btnLowAttendance;

    private String selectedDate;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    private int pendingRequests = 0;
    private int dateRequestVersion = 0;
    private boolean initialLoadStarted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        initializeViews();
        displayUserInformation();
        setupMenuListeners();
        setupCharts();
        setupDatePicker();

        swipeRefreshDashboard.setOnRefreshListener(() -> {
            loadSelectedDateStats();
            loadWeeklyAttendance();
        });
    }

    // INITIALIZE VIEWS

    private void initializeViews() {

        swipeRefreshDashboard =
                findViewById(R.id.swipeRefreshDashboard);

        swipeRefreshDashboard.setColorSchemeResources(
                android.R.color.holo_blue_dark,
                android.R.color.holo_green_dark,
                android.R.color.holo_orange_dark
        );

        chartAttendance = findViewById(R.id.chartAttendance);
        chartWeeklyAttendance =
                findViewById(R.id.chartWeeklyAttendance);

        tvDashboardTitle = findViewById(R.id.tvDashboardTitle);
        tvWelcome = findViewById(R.id.tvWelcome);
        tvRole = findViewById(R.id.tvRole);

        tvStatTotalStudents =
                findViewById(R.id.tvStatTotalStudents);
        tvStatPresent = findViewById(R.id.tvStatPresent);
        tvStatAbsent = findViewById(R.id.tvStatAbsent);
        tvStatNotMarked = findViewById(R.id.tvStatNotMarked);
        tvStatPercentage = findViewById(R.id.tvStatPercentage);

        tvSelectedAttendanceDate =
                findViewById(R.id.tvSelectedAttendanceDate);

        btnSelectAttendanceDate =
                findViewById(R.id.btnSelectAttendanceDate);

        btnStudents = findViewById(R.id.btnStudents);
        btnStudentList = findViewById(R.id.btnStudentList);
        btnAttendance = findViewById(R.id.btnAttendance);
        btnHistory = findViewById(R.id.btnHistory);
        btnReport = findViewById(R.id.btnReport);
        btnLowAttendance = findViewById(R.id.btnLowAttendance);
        btnLogout = findViewById(R.id.btnLogout);
    }

    // USER INFORMATION

    private void displayUserInformation() {

        SharedPreferences preferences =
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        String userName = preferences.getString(
                "user_name",
                getIntent().getStringExtra("user_name")
        );

        String userRole = preferences.getString(
                "user_role",
                getIntent().getStringExtra("user_role")
        );

        if (userName != null && !userName.trim().isEmpty()) {
            tvWelcome.setText("Welcome, " + userName);
        } else {
            tvWelcome.setText("Welcome");
        }

        if (userRole != null && !userRole.trim().isEmpty()) {
            tvRole.setText("Role: " + userRole);
        } else {
            tvRole.setText("Role: User");
        }
    }

    // MENU NAVIGATION

    private void setupMenuListeners() {

        btnStudents.setOnClickListener(view -> {
            startActivity(new Intent(
                    DashboardActivity.this,
                    StudentActivity.class
            ));
        });

        btnStudentList.setOnClickListener(view -> {
            startActivity(new Intent(
                    DashboardActivity.this,
                    StudentListActivity.class
            ));
        });

        btnAttendance.setOnClickListener(view -> {
            startActivity(new Intent(
                    DashboardActivity.this,
                    AttendanceActivity.class
            ));
        });

        btnHistory.setOnClickListener(view -> {
            startActivity(new Intent(
                    DashboardActivity.this,
                    AttendanceRecord.class
            ));
        });

        btnReport.setOnClickListener(view -> {
            Intent intent = new Intent(
                    DashboardActivity.this,
                    MonthlyAttendanceReportActivity.class
            );

            intent.putExtra(
                    "user_name",
                    getIntent().getStringExtra("user_name")
            );

            intent.putExtra(
                    "user_role",
                    getIntent().getStringExtra("user_role")
            );

            startActivity(intent);
        });

        // NEW: CLASS-WISE LOW ATTENDANCE

        btnLowAttendance.setOnClickListener(view -> {
            Intent intent = new Intent(
                    DashboardActivity.this,
                    ClassWiseLowAttendanceActivity.class
            );

            intent.putExtra(
                    "user_name",
                    getIntent().getStringExtra("user_name")
            );

            intent.putExtra(
                    "user_role",
                    getIntent().getStringExtra("user_role")
            );

            startActivity(intent);
        });

        // LOGOUT

        btnLogout.setOnClickListener(view -> {

            getSharedPreferences(
                    PREFS_NAME,
                    MODE_PRIVATE
            ).edit()
                    .clear()
                    .apply();

            Toast.makeText(
                    DashboardActivity.this,
                    "Logged out successfully",
                    Toast.LENGTH_SHORT
            ).show();

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
        });
    }

    // DATE PICKER

    private String formatDate(Date date) {
        return new SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.US
        ).format(date);
    }

    private void setupDatePicker() {

        selectedDate = formatDate(new Date());
        updateSelectedDateLabel();

        btnSelectAttendanceDate.setOnClickListener(view -> {

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
                        chosen.set(year, month, day, 0, 0, 0);
                        chosen.set(Calendar.MILLISECOND, 0);

                        selectedDate = formatDate(chosen.getTime());

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
        });
    }

    private void updateSelectedDateLabel() {
        tvSelectedAttendanceDate.setText(
                "Selected Date: " + selectedDate
        );
    }

    // INITIALIZE CHARTS

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

    // LOAD SELECTED DATE STATISTICS

    private void loadSelectedDateStats() {

        if (isFinishing() || isDestroyed()) {
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
                    displayDateStats(response, requestedDate);
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
                            + response.substring(6)
            );
            return;
        }

        try {
            JSONObject json = new JSONObject(response);

            if (!json.optBoolean("success", false)) {
                showStatsError(json.optString(
                        "message",
                        "Failed to load attendance statistics."
                ));
                return;
            }

            int totalStudents =
                    json.optInt("total_students", 0);

            int present =
                    json.optInt("present_count", 0);

            int absent =
                    json.optInt("absent_count", 0);

            int notMarked =
                    json.optInt("not_marked_count", 0);

            double percentage =
                    json.optDouble("attendance_percentage", 0.0);

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

            tvStatPercentage.setText(String.format(
                    Locale.getDefault(),
                    "%.1f%%",
                    percentage
            ));

            updateDailyChart(present, absent, requestedDate);

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

    // DAILY ATTENDANCE CHART

    private void updateDailyChart(
            int present,
            int absent,
            String date
    ) {

        ArrayList<BarEntry> entries = new ArrayList<>();

        entries.add(new BarEntry(0f, Math.max(0, present)));
        entries.add(new BarEntry(1f, Math.max(0, absent)));

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

    // LOAD WEEKLY ATTENDANCE

    private void loadWeeklyAttendance() {

        if (isFinishing() || isDestroyed()) {
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

    // DISPLAY WEEKLY CHART

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
                showWeeklyChartError(json.optString(
                        "message",
                        "Weekly attendance unavailable."
                ));
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

                JSONObject item = weeklyData.getJSONObject(i);

                labels.add(item.optString("day", ""));

                presentEntries.add(new BarEntry(
                        i,
                        Math.max(0, item.optInt("present_count", 0))
                ));

                absentEntries.add(new BarEntry(
                        i,
                        Math.max(0, item.optInt("absent_count", 0))
                ));
            }

            BarDataSet presentSet =
                    new BarDataSet(presentEntries, "Present");

            presentSet.setColor(Color.rgb(46, 125, 50));
            presentSet.setValueTextSize(9f);

            BarDataSet absentSet =
                    new BarDataSet(absentEntries, "Absent");

            absentSet.setColor(Color.rgb(198, 40, 40));
            absentSet.setValueTextSize(9f);

            BarData data = new BarData(presentSet, absentSet);

            float groupSpace = 0.30f;
            float barSpace = 0.05f;
            float barWidth = 0.30f;

            data.setBarWidth(barWidth);
            chartWeeklyAttendance.setData(data);

            XAxis xAxis = chartWeeklyAttendance.getXAxis();

            xAxis.setValueFormatter(
                    new IndexAxisValueFormatter(labels)
            );

            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setGranularity(1f);
            xAxis.setGranularityEnabled(true);
            xAxis.setCenterAxisLabels(true);
            xAxis.setDrawGridLines(false);
            xAxis.setAxisMinimum(0f);
            xAxis.setAxisMaximum((float) labels.size());

            chartWeeklyAttendance.getAxisLeft().setAxisMinimum(0f);
            chartWeeklyAttendance.getAxisRight().setEnabled(false);
            chartWeeklyAttendance.getDescription().setEnabled(false);
            chartWeeklyAttendance.getLegend().setEnabled(true);

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

    // HTTP POST WITHOUT PARAMETERS

    private String fetchPost(String endpoint) {
        return sendPost(endpoint, "");
    }

    // HTTP POST WITH FORM PARAMETERS

    private String fetchPostWithParameter(
            String endpoint,
            String key,
            String value
    ) {

        try {
            String body = URLEncoder.encode(key, "UTF-8")
                    + "="
                    + URLEncoder.encode(value, "UTF-8");

            return sendPost(endpoint, body);

        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
    }

    // SHARED HTTP METHOD WITH BEARER TOKEN

    private String sendPost(String endpoint, String body) {

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

            SharedPreferences preferences =
                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

            String token = preferences.getString("auth_token", "");

            if (token != null && !token.trim().isEmpty()) {
                connection.setRequestProperty(
                        "Authorization",
                        "Bearer " + token
                );
            }

            try (OutputStream output = connection.getOutputStream()) {
                output.write(body.getBytes("UTF-8"));
                output.flush();
            }

            int responseCode = connection.getResponseCode();

            InputStream input =
                    (responseCode >= 200 && responseCode < 300)
                            ? connection.getInputStream()
                            : connection.getErrorStream();

            if (input == null) {
                return "ERROR: Empty server response. HTTP "
                        + responseCode;
            }

            StringBuilder response = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(input, "UTF-8")
            )) {

                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            if (responseCode < 200 || responseCode >= 300) {
                return "ERROR: HTTP " + responseCode
                        + ": " + response;
            }

            return response.toString();

        } catch (Exception e) {
            return "ERROR:" + e.getMessage();

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    // ERROR HANDLING

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

    // REFRESH INDICATOR

    private void beginRequest() {
        pendingRequests++;

        if (swipeRefreshDashboard != null) {
            swipeRefreshDashboard.setRefreshing(true);
        }
    }

    private void finishRequest() {
        pendingRequests = Math.max(0, pendingRequests - 1);

        if (swipeRefreshDashboard != null) {
            swipeRefreshDashboard.setRefreshing(
                    pendingRequests > 0
            );
        }
    }

    // REFRESH WHEN RETURNING TO DASHBOARD

    @Override
    protected void onResume() {
        super.onResume();

        if (tvStatTotalStudents != null) {
            initialLoadStarted = true;
            loadSelectedDateStats();
            loadWeeklyAttendance();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
