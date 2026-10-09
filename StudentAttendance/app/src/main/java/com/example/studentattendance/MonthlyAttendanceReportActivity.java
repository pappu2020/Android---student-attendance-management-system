
package com.example.studentattendance;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MonthlyAttendanceReportActivity extends Activity {

    private static final String API_URL =
            "http://10.0.2.2/student_attendance_api/get_monthly_class_attendance.php";

    private Button btnSelectMonth, btnGenerateMonthlyReport;
    private EditText etReportClass, etReportSection;
    private TextView tvReportTotalStudents, tvReportPresentRecords;
    private TextView tvReportAbsentRecords, tvReportPercentage;
    private TextView tvMonthlyReportMessage;
    private TableLayout tableMonthlyReport;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private final Calendar selectedMonth = Calendar.getInstance();

    private boolean requestInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monthly_attendance_report);

        initializeViews();
        updateMonthButton();

        btnSelectMonth.setOnClickListener(v -> openMonthPicker());

        btnGenerateMonthlyReport.setOnClickListener(v -> {
            String className = etReportClass.getText().toString().trim();
            String sectionName = etReportSection.getText().toString().trim();

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

            loadMonthlyReport(className, sectionName);
        });
    }

    private void initializeViews() {
        btnSelectMonth = findViewById(R.id.btnSelectMonth);
        btnGenerateMonthlyReport =
                findViewById(R.id.btnGenerateMonthlyReport);

        etReportClass = findViewById(R.id.etReportClass);
        etReportSection = findViewById(R.id.etReportSection);

        tvReportTotalStudents = findViewById(R.id.tvReportTotalStudents);
        tvReportPresentRecords = findViewById(R.id.tvReportPresentRecords);
        tvReportAbsentRecords = findViewById(R.id.tvReportAbsentRecords);
        tvReportPercentage = findViewById(R.id.tvReportPercentage);

        tvMonthlyReportMessage =
                findViewById(R.id.tvMonthlyReportMessage);

        tableMonthlyReport = findViewById(R.id.tableMonthlyReport);
    }

    private void openMonthPicker() {
        Calendar now = Calendar.getInstance();

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedMonth.set(Calendar.YEAR, year);
                    selectedMonth.set(Calendar.MONTH, month);
                    selectedMonth.set(Calendar.DAY_OF_MONTH, 1);
                    updateMonthButton();
                },
                selectedMonth.get(Calendar.YEAR),
                selectedMonth.get(Calendar.MONTH),
                1
        );

        dialog.getDatePicker().setMaxDate(now.getTimeInMillis());
        dialog.show();
    }

    private void updateMonthButton() {
        SimpleDateFormat format =
                new SimpleDateFormat("MMMM yyyy", Locale.getDefault());

        btnSelectMonth.setText(format.format(selectedMonth.getTime()));
    }

    private String getSelectedMonth() {
        return new SimpleDateFormat("yyyy-MM", Locale.US)
                .format(selectedMonth.getTime());
    }

    private void loadMonthlyReport(String className, String sectionName) {
        if (requestInProgress) {
            Toast.makeText(this,
                    "Please wait for the current report",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        requestInProgress = true;
        btnGenerateMonthlyReport.setEnabled(false);
        tvMonthlyReportMessage.setVisibility(View.VISIBLE);
        tvMonthlyReportMessage.setText("Loading monthly report...");

        executor.execute(() -> {
            HttpURLConnection connection = null;

            try {
                URL url = new URL(API_URL);
                connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("POST");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                connection.setDoOutput(true);
                connection.setRequestProperty(
                        "Content-Type",
                        "application/x-www-form-urlencoded; charset=UTF-8"
                );

                String postData =
                        "month=" + URLEncoder.encode(
                                getSelectedMonth(), "UTF-8") +
                                "&class=" + URLEncoder.encode(
                                className, "UTF-8") +
                                "&section=" + URLEncoder.encode(
                                sectionName, "UTF-8");

                try (OutputStream output = connection.getOutputStream()) {
                    output.write(postData.getBytes("UTF-8"));
                    output.flush();
                }

                int responseCode = connection.getResponseCode();

                InputStream input = responseCode >= 200
                        && responseCode < 300
                        ? connection.getInputStream()
                        : connection.getErrorStream();

                StringBuilder response = new StringBuilder();

                if (input != null) {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(input, "UTF-8"))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                    }
                }

                if (responseCode < 200 || responseCode >= 300) {
                    throw new Exception(
                            "HTTP " + responseCode + ": " + response);
                }

                JSONObject json = new JSONObject(response.toString());

                runOnUiThread(() -> {
                    requestInProgress = false;
                    btnGenerateMonthlyReport.setEnabled(true);

                    try {
                        if (!json.optBoolean("success", false)) {
                            showReportError(json.optString(
                                    "message", "Unable to load report"));
                            return;
                        }

                        displayReport(json);

                    } catch (Exception e) {
                        showReportError("Invalid report data.");
                    }
                });

            } catch (Exception e) {
                String message = e.getMessage();

                runOnUiThread(() -> {
                    requestInProgress = false;
                    btnGenerateMonthlyReport.setEnabled(true);
                    showReportError(
                            "Could not load report. Check API connection."
                    );
                });

            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private void displayReport(JSONObject json) throws Exception {
        int totalStudents = json.optInt("total_students", 0);
        int totalPresent = json.optInt("total_present_records", 0);
        int totalAbsent = json.optInt("total_absent_records", 0);
        double overallPercentage =
                json.optDouble("overall_attendance_percentage", 0.0);

        tvReportTotalStudents.setText(
                "Total Students: " + totalStudents);

        tvReportPresentRecords.setText(
                "Present Records: " + totalPresent);

        tvReportAbsentRecords.setText(
                "Absent Records: " + totalAbsent);

        tvReportPercentage.setText(
                String.format(Locale.getDefault(),
                        "Overall Attendance: %.2f%%",
                        overallPercentage));

        // Keep the table header and remove old student rows.
        tableMonthlyReport.removeViews(
                1, Math.max(0, tableMonthlyReport.getChildCount() - 1));

        JSONArray students = json.optJSONArray("students");

        if (students == null || students.length() == 0) {
            tvMonthlyReportMessage.setVisibility(View.VISIBLE);
            tvMonthlyReportMessage.setText(
                    "No students found for this class and section.");
            return;
        }

        for (int i = 0; i < students.length(); i++) {
            JSONObject student = students.getJSONObject(i);

            TableRow row = new TableRow(this);
            row.setBackgroundColor(i % 2 == 0
                    ? Color.WHITE : Color.rgb(244, 247, 251));


            row.addView(createCell(
                    student.optString("student_id", "N/A"), false));


            row.addView(createCell(
                    student.optString("student_name", "N/A"), false));

            row.addView(createCell(
                    String.valueOf(student.optInt("present_count")), false));

            row.addView(createCell(
                    String.valueOf(student.optInt("absent_count")), false));

            row.addView(createCell(
                    String.format(Locale.getDefault(), "%.2f%%",
                            student.optDouble(
                                    "attendance_percentage", 0.0)),
                    false));

            tableMonthlyReport.addView(row);
        }

        tvMonthlyReportMessage.setVisibility(View.GONE);
    }

    private TextView createCell(String value, boolean bold) {
        TextView cell = new TextView(this);
        cell.setText(value);
        cell.setTextColor(Color.rgb(30, 41, 59));
        cell.setTextSize(14);
        cell.setPadding(12, 12, 12, 12);
        cell.setGravity(Gravity.CENTER_VERTICAL);

        if (bold) {
            cell.setTypeface(null, Typeface.BOLD);
        }

        return cell;
    }

    private void showReportError(String message) {
        tvMonthlyReportMessage.setVisibility(View.VISIBLE);
        tvMonthlyReportMessage.setText(message);

        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
