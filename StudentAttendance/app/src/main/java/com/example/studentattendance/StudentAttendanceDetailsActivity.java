
package com.example.studentattendance;

import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

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

public class StudentAttendanceDetailsActivity extends AppCompatActivity {

    // =========================================================
    // API URLS
    // =========================================================

    private static final String DETAILS_URL =
            "http://10.0.2.2/student_attendance_api/get_student_attendance_details.php";

    private static final String MONTHLY_URL =
            "http://10.0.2.2/student_attendance_api/get_student_monthly_attendance.php";

    private static final String PHOTO_BASE_URL =
            "http://10.0.2.2/student_attendance_api/";

    private static final String PREFS_NAME = "StudentAttendanceAuth";
    private static final String TOKEN_KEY = "auth_token";


    // =========================================================
    // STUDENT VIEWS
    // =========================================================

    private ImageView ivStudentPhoto;

    private TextView tvStudentName;
    private TextView tvStudentRoll;
    private TextView tvStudentClass;
    private TextView tvStudentContact;


    // =========================================================
    // ATTENDANCE SUMMARY
    // =========================================================

    private TextView tvTotalRecords;
    private TextView tvPresentRecords;
    private TextView tvAbsentRecords;
    private TextView tvAttendancePercentage;


    // =========================================================
    // MONTHLY REPORT
    // =========================================================

    private TextView tvSelectedMonth;
    private Button btnMonthlyRefresh;


    // =========================================================
    // OTHER VIEWS
    // =========================================================

    private TextView tvDetailsMessage;
    private LinearLayout llAttendanceHistory;


    // =========================================================
    // DATA AND AUTHENTICATION
    // =========================================================

    private int studentId = 0;
    private String selectedMonth = "";
    private String authToken = "";

    private final ExecutorService executorService =
            Executors.newSingleThreadExecutor();

    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());


    // =========================================================
    // ON CREATE
    // =========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_student_attendance_details);

        initializeViews();

        // Read the token saved after successful login.
        SharedPreferences preferences =
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        authToken = preferences.getString(TOKEN_KEY, "");

        if (authToken == null || authToken.trim().isEmpty()) {
            Toast.makeText(
                    this,
                    "Session expired. Please login again.",
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }

        // Get student ID from the previous Activity.
        studentId = getIntent().getIntExtra("student_id", 0);

        if (studentId <= 0) {
            showError("Invalid student ID.");
            return;
        }

        // Initialize the selected month.
        Calendar calendar = Calendar.getInstance();

        selectedMonth = new SimpleDateFormat(
                "yyyy-MM",
                Locale.US
        ).format(calendar.getTime());

        tvSelectedMonth.setText(
                new SimpleDateFormat(
                        "MMMM yyyy",
                        Locale.getDefault()
                ).format(calendar.getTime())
        );

        setupMonthSelector();
        setupMonthlyRefresh();

        loadStudentDetails(studentId);
    }


    // =========================================================
    // INITIALIZE VIEWS
    // =========================================================

    private void initializeViews() {

        ivStudentPhoto = findViewById(R.id.ivStudentPhoto);

        tvStudentName = findViewById(R.id.tvStudentName);
        tvStudentRoll = findViewById(R.id.tvStudentRoll);
        tvStudentClass = findViewById(R.id.tvStudentClass);
        tvStudentContact = findViewById(R.id.tvStudentContact);

        tvTotalRecords = findViewById(R.id.tvTotalRecords);
        tvPresentRecords = findViewById(R.id.tvPresentRecords);
        tvAbsentRecords = findViewById(R.id.tvAbsentRecords);
        tvAttendancePercentage =
                findViewById(R.id.tvAttendancePercentage);

        tvSelectedMonth = findViewById(R.id.tvSelectedMonth);
        btnMonthlyRefresh = findViewById(R.id.btnMonthlyRefresh);

        tvDetailsMessage = findViewById(R.id.tvDetailsMessage);
        llAttendanceHistory = findViewById(R.id.llAttendanceHistory);
    }


    // =========================================================
    // MONTH SELECTOR
    // =========================================================

    private void setupMonthSelector() {
        tvSelectedMonth.setOnClickListener(view -> showMonthPicker());
    }


    // =========================================================
    // MONTH PICKER
    // =========================================================

    private void showMonthPicker() {

        Calendar calendar = Calendar.getInstance();

        DatePickerDialog dialog = new DatePickerDialog(
                StudentAttendanceDetailsActivity.this,
                (datePicker, year, month, day) -> {

                    Calendar selectedCalendar = Calendar.getInstance();

                    selectedCalendar.set(Calendar.YEAR, year);
                    selectedCalendar.set(Calendar.MONTH, month);
                    selectedCalendar.set(Calendar.DAY_OF_MONTH, 1);

                    selectedMonth = new SimpleDateFormat(
                            "yyyy-MM",
                            Locale.US
                    ).format(selectedCalendar.getTime());

                    tvSelectedMonth.setText(
                            new SimpleDateFormat(
                                    "MMMM yyyy",
                                    Locale.getDefault()
                            ).format(selectedCalendar.getTime())
                    );

                    loadMonthlyAttendance();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                1
        );

        // Do not allow a future month.
        dialog.getDatePicker().setMaxDate(
                System.currentTimeMillis()
        );

        dialog.show();
    }


    // =========================================================
    // MONTHLY REFRESH BUTTON
    // =========================================================

    private void setupMonthlyRefresh() {

        btnMonthlyRefresh.setOnClickListener(view -> {

            if (studentId <= 0) {
                showError("Invalid student ID.");
                return;
            }

            if (selectedMonth == null || selectedMonth.isEmpty()) {
                showError("Please select a month.");
                return;
            }

            loadMonthlyAttendance();
        });
    }


    // =========================================================
    // LOAD STUDENT DETAILS
    // =========================================================

    private void loadStudentDetails(int id) {

        tvDetailsMessage.setText("Loading student information...");
        tvDetailsMessage.setTextColor(Color.DKGRAY);

        executorService.execute(() -> {

            String postData = encodeParams(
                    "student_id", String.valueOf(id)
            );

            String response = sendPostRequest(
                    DETAILS_URL,
                    postData
            );

            mainHandler.post(() -> handleDetailsResponse(response));
        });
    }


    // =========================================================
    // LOAD MONTHLY ATTENDANCE
    // =========================================================

    private void loadMonthlyAttendance() {

        if (studentId <= 0 || selectedMonth == null
                || selectedMonth.isEmpty()) {
            return;
        }

        tvDetailsMessage.setText("Loading monthly attendance...");
        tvDetailsMessage.setTextColor(Color.DKGRAY);

        btnMonthlyRefresh.setEnabled(false);

        executorService.execute(() -> {

            String postData =
                    encodeParams("student_id", String.valueOf(studentId))
                            + "&"
                            + encodeParams("month", selectedMonth);

            String response = sendPostRequest(
                    MONTHLY_URL,
                    postData
            );

            mainHandler.post(() -> {

                if (!isFinishing() && !isDestroyed()) {
                    btnMonthlyRefresh.setEnabled(true);
                    handleMonthlyResponse(response);
                }
            });
        });
    }


    // =========================================================
    // URL-ENCODE POST PARAMETERS
    // =========================================================

    private String encodeParams(String key, String value) {

        try {
            return URLEncoder.encode(key, "UTF-8")
                    + "="
                    + URLEncoder.encode(value, "UTF-8");

        } catch (Exception e) {
            return "";
        }
    }


    // =========================================================
    // SEND AUTHENTICATED POST REQUEST
    // =========================================================

    private String sendPostRequest(
            String requestUrl,
            String postData) {

        HttpURLConnection connection = null;

        try {

            if (authToken == null || authToken.trim().isEmpty()) {
                return "ERROR:Authentication token is missing.";
            }

            URL url = new URL(requestUrl);

            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);

            connection.setRequestProperty(
                    "Content-Type",
                    "application/x-www-form-urlencoded; charset=UTF-8"
            );

            connection.setRequestProperty(
                    "Accept",
                    "application/json"
            );

            // Important: authenticate every API request.
            connection.setRequestProperty(
                    "Authorization",
                    "Bearer " + authToken
            );

            byte[] requestBytes =
                    postData.getBytes(StandardCharsets.UTF_8);

            connection.setFixedLengthStreamingMode(requestBytes.length);

            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(requestBytes);
                outputStream.flush();
            }

            int responseCode = connection.getResponseCode();

            InputStream inputStream;

            if (responseCode >= 200 && responseCode < 300) {
                inputStream = connection.getInputStream();
            } else {
                inputStream = connection.getErrorStream();
            }

            if (inputStream == null) {
                return "ERROR:Server returned HTTP " + responseCode;
            }

            StringBuilder response = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            inputStream,
                            StandardCharsets.UTF_8
                    ))) {

                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            String responseText = response.toString();

            if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                return "ERROR:Session expired or invalid token. Please login again.";
            }

            if (responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                return "ERROR:You do not have permission to view this information.";
            }

            if (responseCode < 200 || responseCode >= 300) {
                return "ERROR:HTTP " + responseCode + " - " + responseText;
            }

            return responseText;

        } catch (Exception e) {

            String message = e.getMessage();

            if (message == null || message.trim().isEmpty()) {
                message = "Unable to connect to server.";
            }

            return "ERROR:" + message;

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }
    }


    // =========================================================
    // HANDLE STUDENT DETAILS RESPONSE
    // =========================================================

    private void handleDetailsResponse(String response) {

        if (isFinishing() || isDestroyed()) {
            return;
        }

        if (response == null || response.trim().isEmpty()) {
            showError("No response from server.");
            return;
        }

        if (response.startsWith("ERROR:")) {
            showError(response.substring(6));
            return;
        }

        try {

            JSONObject json = new JSONObject(response);

            boolean success = json.optBoolean("success", false);

            if (!success) {

                showError(
                        json.optString(
                                "message",
                                "Failed to load student details."
                        )
                );

                return;
            }

            JSONObject student = json.optJSONObject("student");

            if (student == null) {
                showError("Student information not found.");
                return;
            }

            displayStudentInformation(student);

            // Load attendance for the currently selected month.
            loadMonthlyAttendance();

        } catch (Exception e) {
            showError("Invalid server response.");
        }
    }


    // =========================================================
    // DISPLAY STUDENT INFORMATION
    // =========================================================

    private void displayStudentInformation(JSONObject student) {

        String name = student.optString("name", "Unknown Student");
        String roll = student.optString("roll_number", "-");
        String className = student.optString("class_name", "-");
        String section = student.optString("section", "-");
        String email = student.optString("email", "");
        String phone = student.optString("phone", "");
        String photo = student.optString("photo", "");

        tvStudentName.setText(name);
        tvStudentRoll.setText("Roll No: " + roll);
        tvStudentClass.setText(className + " • " + section);

        if (!email.isEmpty() && !phone.isEmpty()) {
            tvStudentContact.setText(email + " • " + phone);
        } else if (!email.isEmpty()) {
            tvStudentContact.setText(email);
        } else if (!phone.isEmpty()) {
            tvStudentContact.setText(phone);
        } else {
            tvStudentContact.setText("No contact information");
        }

        // Load the student's photo.
        if (photo != null
                && !photo.trim().isEmpty()
                && !photo.equalsIgnoreCase("null")) {

            String photoUrl = PHOTO_BASE_URL + photo;

            Glide.with(this)
                    .load(photoUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .circleCrop()
                    .into(ivStudentPhoto);

        } else {
            ivStudentPhoto.setImageResource(
                    android.R.drawable.ic_menu_myplaces
            );
        }
    }


    // =========================================================
    // HANDLE MONTHLY ATTENDANCE RESPONSE
    // =========================================================

    private void handleMonthlyResponse(String response) {

        if (isFinishing() || isDestroyed()) {
            return;
        }

        if (response == null || response.trim().isEmpty()) {
            showError("No response from server.");
            return;
        }

        if (response.startsWith("ERROR:")) {
            showError(response.substring(6));
            return;
        }

        try {

            JSONObject json = new JSONObject(response);

            boolean success = json.optBoolean("success", false);

            if (!success) {

                showError(
                        json.optString(
                                "message",
                                "Failed to load monthly attendance."
                        )
                );

                return;
            }

            JSONObject summary = json.optJSONObject("summary");

            if (summary == null) {
                showError("Attendance summary not found.");
                return;
            }

            int totalRecords = summary.optInt("total_records", 0);
            int presentRecords = summary.optInt("present_count", 0);
            int absentRecords = summary.optInt("absent_count", 0);

            double percentage =
                    summary.optDouble("attendance_percentage", 0.0);

            tvTotalRecords.setText(String.valueOf(totalRecords));
            tvPresentRecords.setText(String.valueOf(presentRecords));
            tvAbsentRecords.setText(String.valueOf(absentRecords));

            tvAttendancePercentage.setText(
                    String.format(
                            Locale.getDefault(),
                            "%.1f%%",
                            percentage
                    )
            );

            JSONArray attendance = json.optJSONArray("attendance");

            displayAttendanceHistory(attendance);

            tvDetailsMessage.setText("");
            tvDetailsMessage.setTextColor(Color.DKGRAY);

        } catch (Exception e) {
            showError("Invalid monthly attendance response.");
        }
    }


    // =========================================================
    // DISPLAY ATTENDANCE HISTORY
    // =========================================================

    private void displayAttendanceHistory(JSONArray attendance) {

        llAttendanceHistory.removeAllViews();

        if (attendance == null || attendance.length() == 0) {

            TextView emptyView = new TextView(this);

            emptyView.setText(
                    "No attendance records found for "
                            + formatMonth(selectedMonth)
            );

            emptyView.setTextColor(Color.rgb(84, 110, 122));
            emptyView.setTextSize(14);
            emptyView.setGravity(Gravity.CENTER);
            emptyView.setPadding(16, 25, 16, 25);

            llAttendanceHistory.addView(emptyView);

            return;
        }

        for (int i = 0; i < attendance.length(); i++) {

            try {
                JSONObject record = attendance.getJSONObject(i);
                addAttendanceRow(record);

            } catch (Exception ignored) {
                // Skip an invalid individual record.
            }
        }
    }


    // =========================================================
    // ADD ATTENDANCE ROW
    // =========================================================

    private void addAttendanceRow(JSONObject record) {

        String date = record.optString("attendance_date", "-");
        String status = record.optString("status", "Not Marked");

        // Row.
        LinearLayout row = new LinearLayout(this);

        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(16, 14, 16, 14);

        LinearLayout.LayoutParams rowParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        rowParams.setMargins(0, 0, 0, 8);
        row.setLayoutParams(rowParams);

        // Row background.
        GradientDrawable rowBackground = new GradientDrawable();

        rowBackground.setColor(Color.WHITE);
        rowBackground.setCornerRadius(18);
        rowBackground.setStroke(1, Color.rgb(230, 235, 240));

        row.setBackground(rowBackground);

        // Date.
        TextView dateView = new TextView(this);

        dateView.setText(formatDate(date));
        dateView.setTextColor(Color.rgb(38, 50, 56));
        dateView.setTextSize(15);
        dateView.setTypeface(null, Typeface.BOLD);

        LinearLayout.LayoutParams dateParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                );

        dateView.setLayoutParams(dateParams);
        row.addView(dateView);

        // Status.
        TextView statusView = new TextView(this);

        statusView.setText(getStatusText(status));
        statusView.setGravity(Gravity.CENTER);
        statusView.setTextSize(13);
        statusView.setTypeface(null, Typeface.BOLD);
        statusView.setPadding(14, 9, 14, 9);

        GradientDrawable statusBackground = new GradientDrawable();
        statusBackground.setCornerRadius(14);

        if (status.equalsIgnoreCase("Present")) {

            statusBackground.setColor(Color.rgb(232, 245, 233));
            statusView.setTextColor(Color.rgb(46, 125, 50));

        } else if (status.equalsIgnoreCase("Absent")) {

            statusBackground.setColor(Color.rgb(255, 235, 238));
            statusView.setTextColor(Color.rgb(198, 40, 40));

        } else {

            statusBackground.setColor(Color.rgb(245, 245, 245));
            statusView.setTextColor(Color.rgb(84, 110, 122));
        }

        statusView.setBackground(statusBackground);
        row.addView(statusView);

        llAttendanceHistory.addView(row);
    }


    // =========================================================
    // FORMAT DATE
    // =========================================================

    private String formatDate(String date) {

        try {

            SimpleDateFormat inputFormat = new SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.US
            );

            inputFormat.setLenient(false);

            SimpleDateFormat outputFormat = new SimpleDateFormat(
                    "dd MMM yyyy",
                    Locale.getDefault()
            );

            return outputFormat.format(inputFormat.parse(date));

        } catch (Exception e) {
            return date;
        }
    }


    // =========================================================
    // FORMAT MONTH
    // =========================================================

    private String formatMonth(String month) {

        try {

            SimpleDateFormat inputFormat = new SimpleDateFormat(
                    "yyyy-MM",
                    Locale.US
            );

            inputFormat.setLenient(false);

            SimpleDateFormat outputFormat = new SimpleDateFormat(
                    "MMMM yyyy",
                    Locale.getDefault()
            );

            return outputFormat.format(inputFormat.parse(month));

        } catch (Exception e) {
            return month;
        }
    }


    // =========================================================
    // STATUS TEXT
    // =========================================================

    private String getStatusText(String status) {

        if (status.equalsIgnoreCase("Present")) {
            return "✓ PRESENT";
        }

        if (status.equalsIgnoreCase("Absent")) {
            return "✕ ABSENT";
        }

        return "○ NOT MARKED";
    }


    // =========================================================
    // ERROR MESSAGE
    // =========================================================

    private void showError(String message) {

        tvDetailsMessage.setText(message);

        tvDetailsMessage.setTextColor(
                Color.rgb(198, 40, 40)
        );
    }


    // =========================================================
    // ON DESTROY
    // =========================================================

    @Override
    protected void onDestroy() {

        super.onDestroy();

        executorService.shutdown();
    }
}
