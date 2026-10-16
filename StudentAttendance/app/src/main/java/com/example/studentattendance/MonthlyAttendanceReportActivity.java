package com.example.studentattendance;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MonthlyAttendanceReportActivity extends Activity {

    private static final String TAG =
            "MonthlyAttendanceReport";

    // Must match the PHP filename in your XAMPP API folder.
    private static final String API_URL =
            "http://10.0.2.2/student_attendance_api/"
                    + "get_monthly_class_attendance.php";

    private static final String PREFS_NAME =
            "StudentAttendanceAuth";

    private Button btnSelectMonth;
    private Button btnGenerateMonthlyReport;

    private EditText etReportClass;
    private EditText etReportSection;

    private TextView tvReportTotalStudents;
    private TextView tvReportPresentRecords;
    private TextView tvReportAbsentRecords;
    private TextView tvReportPercentage;
    private TextView tvMonthlyReportMessage;

    private TableLayout tableMonthlyReport;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private final Calendar selectedMonth =
            Calendar.getInstance();

    private String authToken = "";

    private boolean requestInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_monthly_attendance_report
        );

        initializeViews();
        loadAuthToken();

        if (authToken.isEmpty()) {
            Toast.makeText(
                    this,
                    "Session expired. Please login again.",
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }

        updateMonthButton();

        btnSelectMonth.setOnClickListener(
                view -> openMonthPicker()
        );

        btnGenerateMonthlyReport.setOnClickListener(
                view -> validateAndLoadReport()
        );
    }

    // ------------------------------------------------------------
    // INITIALIZE VIEWS
    // ------------------------------------------------------------

    private void initializeViews() {

        btnSelectMonth =
                findViewById(R.id.btnSelectMonth);

        btnGenerateMonthlyReport =
                findViewById(R.id.btnGenerateMonthlyReport);

        etReportClass =
                findViewById(R.id.etReportClass);

        etReportSection =
                findViewById(R.id.etReportSection);

        tvReportTotalStudents =
                findViewById(R.id.tvReportTotalStudents);

        tvReportPresentRecords =
                findViewById(R.id.tvReportPresentRecords);

        tvReportAbsentRecords =
                findViewById(R.id.tvReportAbsentRecords);

        tvReportPercentage =
                findViewById(R.id.tvReportPercentage);

        tvMonthlyReportMessage =
                findViewById(R.id.tvMonthlyReportMessage);

        tableMonthlyReport =
                findViewById(R.id.tableMonthlyReport);
    }

    // ------------------------------------------------------------
    // AUTHENTICATION
    // ------------------------------------------------------------

    private void loadAuthToken() {

        SharedPreferences preferences =
                getSharedPreferences(
                        PREFS_NAME,
                        MODE_PRIVATE
                );

        authToken = preferences.getString(
                "auth_token",
                ""
        );

        if (authToken == null) {
            authToken = "";
        }

        authToken = authToken.trim();
    }

    // ------------------------------------------------------------
    // MONTH PICKER
    // ------------------------------------------------------------

    private void openMonthPicker() {

        Calendar now = Calendar.getInstance();

        DatePickerDialog dialog =
                new DatePickerDialog(
                        this,
                        (picker, year, month, dayOfMonth) -> {

                            selectedMonth.set(
                                    Calendar.YEAR,
                                    year
                            );

                            selectedMonth.set(
                                    Calendar.MONTH,
                                    month
                            );

                            selectedMonth.set(
                                    Calendar.DAY_OF_MONTH,
                                    1
                            );

                            updateMonthButton();
                        },
                        selectedMonth.get(
                                Calendar.YEAR
                        ),
                        selectedMonth.get(
                                Calendar.MONTH
                        ),
                        1
                );

        // Do not allow selecting a future month.
        dialog.getDatePicker().setMaxDate(
                now.getTimeInMillis()
        );

        dialog.show();
    }

    private void updateMonthButton() {

        SimpleDateFormat format =
                new SimpleDateFormat(
                        "MMMM yyyy",
                        Locale.getDefault()
                );

        btnSelectMonth.setText(
                format.format(selectedMonth.getTime())
        );
    }

    private String getSelectedMonth() {

        return new SimpleDateFormat(
                "yyyy-MM",
                Locale.US
        ).format(selectedMonth.getTime());
    }

    // ------------------------------------------------------------
    // VALIDATION
    // ------------------------------------------------------------

    private void validateAndLoadReport() {

        String className =
                etReportClass.getText()
                        .toString()
                        .trim();

        String sectionName =
                etReportSection.getText()
                        .toString()
                        .trim();

        if (className.isEmpty()) {
            etReportClass.setError("Enter class");
            etReportClass.requestFocus();
            return;
        }

        if (sectionName.isEmpty()) {
            etReportSection.setError("Enter section");
            etReportSection.requestFocus();
            return;
        }

        if (className.length() > 100) {
            etReportClass.setError(
                    "Class must not exceed 100 characters."
            );
            return;
        }

        if (sectionName.length() > 50) {
            etReportSection.setError(
                    "Section must not exceed 50 characters."
            );
            return;
        }

        if (authToken.isEmpty()) {
            showReportError(
                    "Authentication token missing. Login again."
            );
            return;
        }

        loadMonthlyReport(
                className,
                sectionName
        );
    }

    // ------------------------------------------------------------
    // LOAD MONTHLY REPORT
    // ------------------------------------------------------------

    private void loadMonthlyReport(
            String className,
            String sectionName
    ) {

        if (requestInProgress) {
            Toast.makeText(
                    this,
                    "Please wait for the current report.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        requestInProgress = true;

        btnGenerateMonthlyReport.setEnabled(false);

        tvMonthlyReportMessage.setVisibility(
                View.VISIBLE
        );

        tvMonthlyReportMessage.setText(
                "Loading monthly report..."
        );

        // Capture the month on the UI thread.
        final String month = getSelectedMonth();
        final String token = authToken;

        executor.execute(() -> {

            HttpURLConnection connection = null;

            try {
                URL url = new URL(API_URL);

                connection =
                        (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("POST");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                connection.setDoOutput(true);

                connection.setRequestProperty(
                        "Content-Type",
                        "application/x-www-form-urlencoded; charset=UTF-8"
                );

                connection.setRequestProperty(
                        "Accept",
                        "application/json"
                );

                // Required by auth.php / requireAuth().
                connection.setRequestProperty(
                        "Authorization",
                        "Bearer " + token
                );

                // Keep the exact parameter names expected by
                // get_monthly_class_attendance.php.
                String postData =
                        "month=" + encode(month)
                                + "&class=" + encode(className)
                                + "&section=" + encode(sectionName);

                try (OutputStream output =
                             connection.getOutputStream()) {

                    output.write(
                            postData.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

                    output.flush();
                }

                int responseCode =
                        connection.getResponseCode();

                InputStream input =
                        responseCode >= 200
                                && responseCode < 300
                                ? connection.getInputStream()
                                : connection.getErrorStream();

                StringBuilder response =
                        new StringBuilder();

                if (input != null) {
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
                }

                String responseText = response.toString();

                Log.d(
                        TAG,
                        "HTTP status: " + responseCode
                );

                Log.d(
                        TAG,
                        "Response: " + responseText
                );

                if (responseCode < 200
                        || responseCode >= 300) {

                    String serverMessage =
                            extractServerMessage(responseText);

                    throw new Exception(
                            "HTTP "
                                    + responseCode
                                    + ": "
                                    + serverMessage
                    );
                }

                JSONObject json =
                        new JSONObject(responseText);

                runOnUiThread(() -> {

                    requestInProgress = false;

                    btnGenerateMonthlyReport.setEnabled(true);

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    if (!json.optBoolean(
                            "success",
                            false
                    )) {

                        showReportError(
                                json.optString(
                                        "message",
                                        "Unable to load report."
                                )
                        );

                        return;
                    }

                    try {
                        displayReport(json);

                    } catch (Exception e) {
                        Log.e(
                                TAG,
                                "Error displaying report",
                                e
                        );

                        showReportError(
                                "Unable to display report data."
                        );
                    }
                });

            } catch (Exception e) {

                Log.e(
                        TAG,
                        "Monthly report request failed",
                        e
                );

                String errorMessage = e.getMessage();

                runOnUiThread(() -> {

                    requestInProgress = false;

                    btnGenerateMonthlyReport.setEnabled(true);

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    showReportError(
                            errorMessage == null
                                    ? "Could not load report."
                                    : errorMessage
                    );
                });

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private String encode(String value) throws Exception {

        return URLEncoder.encode(
                value,
                "UTF-8"
        );
    }

    private String extractServerMessage(String response) {

        try {
            JSONObject json =
                    new JSONObject(response);

            return json.optString(
                    "message",
                    response
            );

        } catch (Exception ignored) {
            if (response == null || response.trim().isEmpty()) {
                return "Empty response from server.";
            }

            return response;
        }
    }

    // ------------------------------------------------------------
    // DISPLAY REPORT
    // ------------------------------------------------------------

    private void displayReport(JSONObject json)
            throws Exception {

        // These keys match the uploaded PHP file:
        // get_monthly_class_attendance.php

        int totalStudents =
                json.optInt(
                        "total_students",
                        0
                );

        int totalPresent =
                json.optInt(
                        "total_present_records",
                        0
                );

        int totalAbsent =
                json.optInt(
                        "total_absent_records",
                        0
                );

        double overallPercentage =
                json.optDouble(
                        "overall_attendance_percentage",
                        0.0
                );

        tvReportTotalStudents.setText(
                "Total Students: " + totalStudents
        );

        tvReportPresentRecords.setText(
                "Present Records: " + totalPresent
        );

        tvReportAbsentRecords.setText(
                "Absent Records: " + totalAbsent
        );

        tvReportPercentage.setText(
                String.format(
                        Locale.getDefault(),
                        "Overall Attendance: %.2f%%",
                        overallPercentage
                )
        );

        // Remove old student rows but preserve the table header.
        int childCount =
                tableMonthlyReport.getChildCount();

        if (childCount > 1) {
            tableMonthlyReport.removeViews(
                    1,
                    childCount - 1
            );
        }

        JSONArray students =
                json.optJSONArray("students");

        if (students == null || students.length() == 0) {

            tvMonthlyReportMessage.setVisibility(
                    View.VISIBLE
            );

            tvMonthlyReportMessage.setText(
                    "No students found for this class and section."
            );

            return;
        }

        for (int i = 0; i < students.length(); i++) {

            JSONObject student =
                    students.getJSONObject(i);

            TableRow row = new TableRow(this);

            row.setBackgroundColor(
                    i % 2 == 0
                            ? Color.WHITE
                            : Color.rgb(244, 247, 251)
            );

            // Match PHP keys: student_id, student_name.
            row.addView(
                    createCell(
                            student.optString(
                                    "student_id",
                                    "N/A"
                            ),
                            false
                    )
            );

            row.addView(
                    createCell(
                            student.optString(
                                    "student_name",
                                    "N/A"
                            ),
                            false
                    )
            );

            row.addView(
                    createCell(
                            String.valueOf(
                                    student.optInt(
                                            "present_count",
                                            0
                                    )
                            ),
                            false
                    )
            );

            row.addView(
                    createCell(
                            String.valueOf(
                                    student.optInt(
                                            "absent_count",
                                            0
                                    )
                            ),
                            false
                    )
            );

            row.addView(
                    createCell(
                            String.format(
                                    Locale.getDefault(),
                                    "%.2f%%",
                                    student.optDouble(
                                            "attendance_percentage",
                                            0.0
                                    )
                            ),
                            false
                    )
            );

            tableMonthlyReport.addView(row);
        }

        tvMonthlyReportMessage.setVisibility(
                View.GONE
        );
    }

    // ------------------------------------------------------------
    // TABLE CELL
    // ------------------------------------------------------------

    private TextView createCell(
            String value,
            boolean bold
    ) {

        TextView cell = new TextView(this);

        cell.setText(value);
        cell.setTextColor(
                Color.rgb(30, 41, 59)
        );

        cell.setTextSize(14);

        cell.setPadding(
                12,
                12,
                12,
                12
        );

        cell.setGravity(
                Gravity.CENTER_VERTICAL
        );

        if (bold) {
            cell.setTypeface(
                    null,
                    Typeface.BOLD
            );
        }

        return cell;
    }

    // ------------------------------------------------------------
    // ERROR MESSAGE
    // ------------------------------------------------------------

    private void showReportError(String message) {

        tvMonthlyReportMessage.setVisibility(
                View.VISIBLE
        );

        tvMonthlyReportMessage.setText(message);

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    // ------------------------------------------------------------
    // LIFECYCLE
    // ------------------------------------------------------------

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}