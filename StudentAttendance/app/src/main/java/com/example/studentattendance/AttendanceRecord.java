package com.example.studentattendance;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AttendanceRecord extends AppCompatActivity {

    // =========================================================
    // API CONFIGURATION
    // =========================================================

    private static final String HISTORY_URL =
            "http://10.0.2.2/student_attendance_api/get_attendance_history.php";

    private static final String PHOTO_BASE_URL =
            "http://10.0.2.2/student_attendance_api/";

    private static final String PREFS_NAME =
            "StudentAttendanceAuth";

    private String authToken = "";

    // =========================================================
    // UI
    // =========================================================

    private Spinner spinnerHistoryClass;
    private Spinner spinnerHistorySection;

    private TextView tvHistoryDate;
    private Button btnHistoryRefresh;
    private EditText etHistorySearch;

    private TextView tvHistoryTotal;
    private TextView tvHistoryPresent;
    private TextView tvHistoryAbsent;
    private TextView tvHistoryRate;
    private TextView tvHistoryNotMarked;

    private TextView tvHistoryResultCount;
    private TextView tvHistoryMessage;

    private LinearLayout llHistoryList;

    // =========================================================
    // DATA AND THREADS
    // =========================================================

    private final ArrayList<JSONObject> allStudents =
            new ArrayList<>();

    private String selectedDate = "";

    private final ExecutorService executorService =
            Executors.newSingleThreadExecutor();

    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    private boolean requestInProgress = false;

    // =========================================================
    // ON CREATE
    // =========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_attendance_record);

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

        initializeViews();
        setupClassSpinner();
        setupSectionSpinner();
        setupDate();
        setupSearch();
        setupRefreshButton();
        setupTodayDate();
        resetSummary();
    }

    // =========================================================
    // AUTHENTICATION
    // =========================================================

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

    private boolean hasValidLocalToken() {

        if (authToken.isEmpty()) {
            showMessage(
                    "Session expired. Please login again."
            );
            return false;
        }

        return true;
    }

    // =========================================================
    // INITIALIZE VIEWS
    // =========================================================

    private void initializeViews() {

        spinnerHistoryClass =
                findViewById(R.id.spinnerHistoryClass);

        spinnerHistorySection =
                findViewById(R.id.spinnerHistorySection);

        tvHistoryDate =
                findViewById(R.id.tvHistoryDate);

        btnHistoryRefresh =
                findViewById(R.id.btnHistoryRefresh);

        etHistorySearch =
                findViewById(R.id.etHistorySearch);

        tvHistoryTotal =
                findViewById(R.id.tvHistoryTotal);

        tvHistoryPresent =
                findViewById(R.id.tvHistoryPresent);

        tvHistoryAbsent =
                findViewById(R.id.tvHistoryAbsent);

        tvHistoryRate =
                findViewById(R.id.tvHistoryRate);

        tvHistoryNotMarked =
                findViewById(R.id.tvHistoryNotMarked);

        tvHistoryResultCount =
                findViewById(R.id.tvHistoryResultCount);

        tvHistoryMessage =
                findViewById(R.id.tvHistoryMessage);

        llHistoryList =
                findViewById(R.id.llHistoryList);
    }

    // =========================================================
    // TODAY'S DATE
    // =========================================================

    private void setupTodayDate() {

        Calendar calendar = Calendar.getInstance();

        selectedDate = new SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.US
        ).format(calendar.getTime());

        tvHistoryDate.setText(
                new SimpleDateFormat(
                        "dd MMM yyyy",
                        Locale.getDefault()
                ).format(calendar.getTime())
        );
    }

    // =========================================================
    // REFRESH BUTTON
    // =========================================================

    private void setupRefreshButton() {

        btnHistoryRefresh.setOnClickListener(view -> {

            if (spinnerHistoryClass.getSelectedItemPosition() == 0) {
                showMessage("Please select a class first.");
                return;
            }

            if (spinnerHistorySection.getSelectedItemPosition() == 0) {
                showMessage("Please select a section first.");
                return;
            }

            if (selectedDate.isEmpty()) {
                showMessage("Please select an attendance date.");
                return;
            }

            loadAttendanceHistory();
        });
    }

    // =========================================================
    // CLASS SPINNER
    // =========================================================

    private void setupClassSpinner() {

        ArrayList<String> classes = new ArrayList<>();
        classes.add("Select Class");

        for (int i = 6; i <= 12; i++) {
            classes.add("Class " + i);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                classes
        );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerHistoryClass.setAdapter(adapter);

        spinnerHistoryClass.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {
                        if (position > 0
                                && spinnerHistorySection
                                .getSelectedItemPosition() > 0
                                && !selectedDate.isEmpty()) {

                            loadAttendanceHistory();
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
    // SECTION SPINNER
    // =========================================================

    private void setupSectionSpinner() {

        ArrayList<String> sections = new ArrayList<>();

        sections.add("Select Section");
        sections.add("Section A");
        sections.add("Section B");
        sections.add("Section C");
        sections.add("Section D");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                sections
        );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerHistorySection.setAdapter(adapter);

        spinnerHistorySection.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {
                        if (position > 0
                                && spinnerHistoryClass
                                .getSelectedItemPosition() > 0
                                && !selectedDate.isEmpty()) {

                            loadAttendanceHistory();
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
    // DATE PICKER
    // =========================================================

    private void setupDate() {

        tvHistoryDate.setOnClickListener(
                view -> showDatePicker()
        );
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
        }

        DatePickerDialog dialog = new DatePickerDialog(
                AttendanceRecord.this,
                (datePicker, year, month, day) -> {

                    Calendar selectedCalendar =
                            Calendar.getInstance();

                    selectedCalendar.set(
                            year,
                            month,
                            day,
                            0,
                            0,
                            0
                    );

                    selectedCalendar.set(
                            Calendar.MILLISECOND,
                            0
                    );

                    selectedDate = new SimpleDateFormat(
                            "yyyy-MM-dd",
                            Locale.US
                    ).format(selectedCalendar.getTime());

                    tvHistoryDate.setText(
                            new SimpleDateFormat(
                                    "dd MMM yyyy",
                                    Locale.getDefault()
                            ).format(selectedCalendar.getTime())
                    );

                    loadAttendanceHistory();
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

    // =========================================================
    // SEARCH
    // =========================================================

    private void setupSearch() {

        etHistorySearch.addTextChangedListener(
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after
                    ) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count
                    ) {
                        filterStudents(s.toString());
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                    }
                }
        );
    }

    // =========================================================
    // LOAD ATTENDANCE HISTORY
    // =========================================================

    private void loadAttendanceHistory() {

        if (!hasValidLocalToken()) {
            return;
        }

        if (spinnerHistoryClass.getSelectedItemPosition() == 0
                || spinnerHistorySection.getSelectedItemPosition() == 0
                || selectedDate.isEmpty()) {
            return;
        }

        if (requestInProgress) {
            return;
        }

        String className =
                spinnerHistoryClass.getSelectedItem().toString();

        String section =
                spinnerHistorySection.getSelectedItem().toString();

        final String requestDate = selectedDate;

        requestInProgress = true;

        btnHistoryRefresh.setEnabled(false);

        tvHistoryMessage.setVisibility(View.VISIBLE);
        tvHistoryMessage.setText("Loading attendance...");

        executorService.execute(() -> {

            String response = sendPostRequest(
                    className,
                    section,
                    requestDate
            );

            mainHandler.post(() -> {

                requestInProgress = false;

                if (isFinishing() || isDestroyed()) {
                    return;
                }

                btnHistoryRefresh.setEnabled(true);

                handleResponse(response);
            });
        });
    }

    // =========================================================
    // POST REQUEST WITH BEARER TOKEN
    // =========================================================

    private String sendPostRequest(
            String className,
            String section,
            String date
    ) {

        HttpURLConnection connection = null;

        try {
            URL url = new URL(HISTORY_URL);

            connection =
                    (HttpURLConnection) url.openConnection();

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

            // Authentication required by auth.php.
            connection.setRequestProperty(
                    "Authorization",
                    "Bearer " + authToken
            );

            String postData =
                    "class_name=" + URLEncoder.encode(
                            className,
                            "UTF-8"
                    )
                            + "&section=" + URLEncoder.encode(
                            section,
                            "UTF-8"
                    )
                            + "&attendance_date=" + URLEncoder.encode(
                            date,
                            "UTF-8"
                    );

            try (OutputStream outputStream =
                         connection.getOutputStream()) {

                outputStream.write(
                        postData.getBytes(StandardCharsets.UTF_8)
                );

                outputStream.flush();
            }

            int responseCode =
                    connection.getResponseCode();

            InputStream inputStream =
                    responseCode >= 200 && responseCode < 300
                            ? connection.getInputStream()
                            : connection.getErrorStream();

            if (inputStream == null) {
                return "ERROR: HTTP " + responseCode
                        + ": Empty server response";
            }

            StringBuilder response = new StringBuilder();

            try (BufferedReader reader =
                         new BufferedReader(
                                 new InputStreamReader(
                                         inputStream,
                                         StandardCharsets.UTF_8
                                 )
                         )) {

                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            android.util.Log.d(
                    "AttendanceRecord",
                    "HTTP status: " + responseCode
            );

            android.util.Log.d(
                    "AttendanceRecord",
                    "Response: " + response
            );

            if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                return "ERROR: HTTP 401: Authentication required. "
                        + response;
            }

            if (responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                return "ERROR: HTTP 403: Permission denied. "
                        + response;
            }

            if (responseCode < 200 || responseCode >= 300) {
                return "ERROR: HTTP " + responseCode
                        + ": " + response;
            }

            return response.toString();

        } catch (Exception e) {

            android.util.Log.e(
                    "AttendanceRecord",
                    "Attendance history request failed",
                    e
            );

            return "ERROR: " + e.getMessage();

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    // =========================================================
    // HANDLE API RESPONSE
    // =========================================================

    private void handleResponse(String response) {

        if (response == null || response.trim().isEmpty()) {
            showMessage("No response from server.");
            return;
        }

        if (response.startsWith("ERROR:")) {
            showMessage(response.substring(6).trim());
            return;
        }

        try {
            JSONObject json = new JSONObject(response);

            if (!json.optBoolean("success", false)) {
                showMessage(
                        json.optString(
                                "message",
                                "Failed to load attendance."
                        )
                );
                return;
            }

            allStudents.clear();

            JSONArray students =
                    json.optJSONArray("students");

            if (students != null) {
                for (int i = 0; i < students.length(); i++) {
                    JSONObject student =
                            students.optJSONObject(i);

                    if (student != null) {
                        allStudents.add(student);
                    }
                }
            }

            int total =
                    json.optInt("total_students", 0);

            int present =
                    json.optInt("present_count", 0);

            int absent =
                    json.optInt("absent_count", 0);

            int notMarked =
                    json.optInt("not_marked_count", 0);

            int markedStudents = present + absent;

            double attendanceRate = markedStudents > 0
                    ? present * 100.0 / markedStudents
                    : 0.0;

            tvHistoryTotal.setText(
                    String.valueOf(total)
            );

            tvHistoryPresent.setText(
                    String.valueOf(present)
            );

            tvHistoryAbsent.setText(
                    String.valueOf(absent)
            );

            tvHistoryNotMarked.setText(
                    "Not Marked: " + notMarked
            );

            tvHistoryRate.setText(
                    String.format(
                            Locale.getDefault(),
                            "%.1f%%",
                            attendanceRate
                    )
            );

            tvHistoryMessage.setText("");

            filterStudents(
                    etHistorySearch.getText().toString()
            );

        } catch (Exception e) {

            android.util.Log.e(
                    "AttendanceRecord",
                    "Invalid attendance response",
                    e
            );

            showMessage("Invalid server response.");
        }
    }

    // =========================================================
    // FILTER STUDENTS
    // =========================================================

    private void filterStudents(String searchText) {

        llHistoryList.removeAllViews();

        String query = searchText == null
                ? ""
                : searchText.trim().toLowerCase(Locale.getDefault());

        int matchedCount = 0;

        for (JSONObject student : allStudents) {

            String name = student.optString("name", "");
            String roll = student.optString("roll_number", "");

            boolean matches = query.isEmpty()
                    || name.toLowerCase(Locale.getDefault())
                    .contains(query)
                    || roll.toLowerCase(Locale.getDefault())
                    .contains(query);

            if (matches) {
                addStudentCard(student);
                matchedCount++;
            }
        }

        tvHistoryResultCount.setText(
                "Showing " + matchedCount
                        + " of " + allStudents.size()
                        + " students"
        );

        if (matchedCount == 0) {

            tvHistoryMessage.setVisibility(View.VISIBLE);

            if (allStudents.isEmpty()) {
                tvHistoryMessage.setText(
                        "No students found for the selected class, section and date."
                );
            } else {
                tvHistoryMessage.setText(
                        "No student matches your search."
                );
            }

        } else {
            tvHistoryMessage.setVisibility(View.GONE);
        }
    }

    // =========================================================
    // STUDENT CARD
    // =========================================================

    private void addStudentCard(JSONObject student) {

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(cardParams);

        GradientDrawable cardBackground =
                new GradientDrawable();

        cardBackground.setColor(Color.WHITE);
        cardBackground.setCornerRadius(dp(22));
        cardBackground.setStroke(
                dp(1),
                Color.rgb(230, 235, 240)
        );

        card.setBackground(cardBackground);
        card.setClickable(true);
        card.setFocusable(true);

        card.setOnClickListener(view -> {

            int studentId = student.optInt("id", 0);

            if (studentId <= 0) {
                return;
            }

            Intent intent = new Intent(
                    AttendanceRecord.this,
                    StudentAttendanceDetailsActivity.class
            );

            intent.putExtra("student_id", studentId);
            startActivity(intent);
        });

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        ImageView imageView = new ImageView(this);

        LinearLayout.LayoutParams imageParams =
                new LinearLayout.LayoutParams(
                        dp(64),
                        dp(64)
                );

        imageParams.setMargins(0, 0, dp(14), 0);
        imageView.setLayoutParams(imageParams);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

        GradientDrawable imageBackground =
                new GradientDrawable();

        imageBackground.setColor(Color.rgb(227, 242, 253));
        imageBackground.setShape(GradientDrawable.OVAL);

        imageView.setBackground(imageBackground);

        LinearLayout infoLayout = new LinearLayout(this);
        infoLayout.setOrientation(LinearLayout.VERTICAL);

        infoLayout.setLayoutParams(
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        TextView nameView = new TextView(this);
        nameView.setText(
                student.optString("name", "Unknown Student")
        );
        nameView.setTextColor(Color.rgb(38, 50, 56));
        nameView.setTextSize(18);
        nameView.setTypeface(null, Typeface.BOLD);

        infoLayout.addView(nameView);

        infoLayout.addView(
                createInfoText(
                        "Roll No: "
                                + student.optString("roll_number", "-")
                )
        );

        infoLayout.addView(
                createInfoText(
                        student.optString("class_name", "-")
                                + " • "
                                + student.optString("section", "-")
                )
        );

        String attendanceDate = student.optString(
                "attendance_date",
                selectedDate
        );

        infoLayout.addView(
                createInfoText(
                        "Date: " + formatAttendanceDate(attendanceDate)
                )
        );

        topRow.addView(imageView);
        topRow.addView(infoLayout);
        card.addView(topRow);

        String photo = student.optString("photo", "");

        if (!photo.trim().isEmpty()
                && !"null".equalsIgnoreCase(photo.trim())) {

            String photoUrl = PHOTO_BASE_URL + photo;

            Glide.with(this)
                    .load(photoUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .circleCrop()
                    .into(imageView);

        } else {
            imageView.setImageResource(
                    android.R.drawable.ic_menu_myplaces
            );
        }

        String status = student.optString(
                "status",
                "Not Marked"
        );

        TextView statusView = new TextView(this);
        statusView.setText(getStatusText(status));
        statusView.setTextSize(14);
        statusView.setTypeface(null, Typeface.BOLD);
        statusView.setGravity(Gravity.CENTER);
        statusView.setPadding(
                dp(12),
                dp(11),
                dp(12),
                dp(11)
        );

        LinearLayout.LayoutParams statusParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        statusParams.setMargins(0, dp(14), 0, 0);
        statusView.setLayoutParams(statusParams);

        GradientDrawable statusBackground =
                new GradientDrawable();

        statusBackground.setCornerRadius(dp(14));

        if ("Present".equalsIgnoreCase(status)) {

            statusBackground.setColor(
                    Color.rgb(232, 245, 233)
            );

            statusView.setTextColor(
                    Color.rgb(46, 125, 50)
            );

        } else if ("Absent".equalsIgnoreCase(status)) {

            statusBackground.setColor(
                    Color.rgb(255, 235, 238)
            );

            statusView.setTextColor(
                    Color.rgb(198, 40, 40)
            );

        } else {

            statusBackground.setColor(
                    Color.rgb(245, 245, 245)
            );

            statusView.setTextColor(
                    Color.rgb(84, 110, 122)
            );
        }

        statusView.setBackground(statusBackground);
        card.addView(statusView);

        llHistoryList.addView(card);
    }

    // =========================================================
    // INFO TEXT
    // =========================================================

    private TextView createInfoText(String text) {

        TextView textView = new TextView(this);

        textView.setText(text);
        textView.setTextColor(Color.rgb(84, 110, 122));
        textView.setTextSize(14);
        textView.setPadding(0, dp(4), 0, 0);

        return textView;
    }

    // =========================================================
    // FORMAT DATE
    // =========================================================

    private String formatAttendanceDate(String date) {

        try {
            SimpleDateFormat inputFormat =
                    new SimpleDateFormat(
                            "yyyy-MM-dd",
                            Locale.US
                    );

            SimpleDateFormat outputFormat =
                    new SimpleDateFormat(
                            "dd MMM yyyy",
                            Locale.getDefault()
                    );

            java.util.Date parsed = inputFormat.parse(date);

            if (parsed != null) {
                return outputFormat.format(parsed);
            }

        } catch (Exception ignored) {
        }

        return date;
    }

    // =========================================================
    // STATUS TEXT
    // =========================================================

    private String getStatusText(String status) {

        if ("Present".equalsIgnoreCase(status)) {
            return "✓  PRESENT";
        }

        if ("Absent".equalsIgnoreCase(status)) {
            return "✕  ABSENT";
        }

        return "○  NOT MARKED";
    }

    // =========================================================
    // RESET SUMMARY
    // =========================================================

    private void resetSummary() {

        tvHistoryTotal.setText("0");
        tvHistoryPresent.setText("0");
        tvHistoryAbsent.setText("0");
        tvHistoryRate.setText("0%");
        tvHistoryNotMarked.setText("Not Marked: 0");
    }

    // =========================================================
    // SHOW MESSAGE
    // =========================================================

    private void showMessage(String message) {

        tvHistoryMessage.setVisibility(View.VISIBLE);
        tvHistoryMessage.setText(message);
        tvHistoryMessage.setTextColor(
                Color.rgb(84, 110, 122)
        );

        tvHistoryResultCount.setText("No results");
        llHistoryList.removeAllViews();
    }

    // =========================================================
    // DP HELPER
    // =========================================================

    private int dp(int value) {

        return (int) (
                value
                        * getResources()
                        .getDisplayMetrics()
                        .density
                        + 0.5f
        );
    }

    // =========================================================
    // ON DESTROY
    // =========================================================

    @Override
    protected void onDestroy() {

        super.onDestroy();
        executorService.shutdownNow();
    }
}