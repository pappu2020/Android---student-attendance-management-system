
package com.example.studentattendance;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClassWiseLowAttendanceActivity extends AppCompatActivity {

    private static final String API_URL =
            "http://10.0.2.2/student_attendance_api/"
                    + "get_low_attendance_by_class.php";

    private static final String PREFS_NAME = "StudentAttendanceAuth";

    private static final int BLUE = Color.rgb(36, 87, 197);
    private static final int DARK = Color.rgb(23, 43, 77);
    private static final int GRAY = Color.rgb(100, 116, 139);
    private static final int RED = Color.rgb(198, 40, 40);
    private static final int ORANGE = Color.rgb(230, 126, 34);
    private static final int GREEN = Color.rgb(46, 125, 50);

    private Spinner spinnerClass;
    private Spinner spinnerSection;
    private Button btnSearch;
    private Button btnBack;
    private TextView tvResultCount;
    private LinearLayout layoutStudentList;
    private ProgressBar progressLoading;

    private String authToken = "";

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class_wise_low_attendance);

        initializeViews();
        setupSpinners();
        setupListeners();
        loadToken();
    }

    private void initializeViews() {
        spinnerClass = findViewById(
                R.id.spinnerLowAttendanceClass
        );

        spinnerSection = findViewById(
                R.id.spinnerLowAttendanceSection
        );

        btnSearch = findViewById(
                R.id.btnSearchLowAttendance
        );

        btnBack = findViewById(
                R.id.btnBackToDashboard
        );

        tvResultCount = findViewById(
                R.id.tvLowAttendanceResultCount
        );

        layoutStudentList = findViewById(
                R.id.layoutClassLowAttendanceList
        );

        // Create a loading indicator programmatically.
        progressLoading = new ProgressBar(this);
        progressLoading.setIndeterminate(true);
        progressLoading.setIndeterminateTintList(
                ColorStateList.valueOf(BLUE)
        );

        LinearLayout.LayoutParams progressParams =
                new LinearLayout.LayoutParams(
                        dp(40),
                        dp(40)
                );

        progressParams.gravity = Gravity.CENTER;
        progressParams.topMargin = dp(20);
        progressParams.bottomMargin = dp(12);

        progressLoading.setLayoutParams(progressParams);
        progressLoading.setVisibility(View.GONE);

        // Add loading indicator to the existing result container.
        layoutStudentList.addView(progressLoading);
    }

    private void setupSpinners() {
        ArrayList<String> classes = new ArrayList<>();

        for (int i = 6; i <= 12; i++) {
            classes.add("Class " + i);
        }

        ArrayAdapter<String> classAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        classes
                );

        classAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerClass.setAdapter(classAdapter);

        String[] sections = {"A", "B", "C"};

        ArrayAdapter<String> sectionAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        sections
                );

        sectionAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerSection.setAdapter(sectionAdapter);
    }

    private void setupListeners() {
        btnSearch.setOnClickListener(view -> searchStudents());

        btnBack.setOnClickListener(view -> finish());
    }

    private void loadToken() {
        SharedPreferences preferences =
                getSharedPreferences(
                        PREFS_NAME,
                        MODE_PRIVATE
                );

        authToken = preferences.getString("auth_token", "");

        if (authToken == null || authToken.trim().isEmpty()) {
            Toast.makeText(
                    this,
                    "Please log in again.",
                    Toast.LENGTH_LONG
            ).show();

            finish();
        }
    }

    private void searchStudents() {
        if (authToken == null || authToken.trim().isEmpty()) {
            Toast.makeText(
                    this,
                    "Authentication required.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        Object classItem = spinnerClass.getSelectedItem();
        Object sectionItem = spinnerSection.getSelectedItem();

        if (classItem == null || sectionItem == null) {
            Toast.makeText(
                    this,
                    "Please select a class and section.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        String selectedClass = classItem.toString()
                .replace("Class ", "")
                .trim();

        String selectedSection = sectionItem.toString();

        layoutStudentList.removeAllViews();
        layoutStudentList.addView(progressLoading);

        progressLoading.setVisibility(View.VISIBLE);
        tvResultCount.setText("Loading attendance records...");
        btnSearch.setEnabled(false);

        executor.execute(() -> {
            String response = sendRequest(
                    selectedClass,
                    selectedSection
            );

            mainHandler.post(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                btnSearch.setEnabled(true);
                progressLoading.setVisibility(View.GONE);

                displayStudents(
                        response,
                        selectedClass,
                        selectedSection
                );
            });
        });
    }

    private String sendRequest(String className, String section) {
        HttpURLConnection connection = null;

        try {
            URL url = new URL(API_URL);

            connection = (HttpURLConnection) url.openConnection();

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

            String body =
                    "class_name=" + URLEncoder.encode(
                            className,
                            "UTF-8"
                    )
                            + "&section=" + URLEncoder.encode(
                            section,
                            "UTF-8"
                    );

            try (OutputStream output = connection.getOutputStream()) {
                output.write(body.getBytes(StandardCharsets.UTF_8));
                output.flush();
            }

            int responseCode = connection.getResponseCode();

            InputStream input =
                    responseCode >= 200 && responseCode < 300
                            ? connection.getInputStream()
                            : connection.getErrorStream();

            if (input == null) {
                return "ERROR: HTTP " + responseCode;
            }

            StringBuilder response = new StringBuilder();

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

            if (responseCode < 200 || responseCode >= 300) {
                return "ERROR: HTTP " + responseCode
                        + ": " + response;
            }

            return response.toString();

        } catch (Exception e) {
            return "ERROR: " + e.getMessage();

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void displayStudents(
            String response,
            String selectedClass,
            String selectedSection
    ) {
        layoutStudentList.removeAllViews();

        if (response == null
                || response.trim().isEmpty()
                || response.startsWith("ERROR:")) {

            tvResultCount.setText("Unable to load students.");

            addMessageCard(
                    "Connection Error",
                    response == null
                            ? "No response received from the server."
                            : response,
                    RED
            );

            return;
        }

        try {
            JSONObject json = new JSONObject(response);

            if (!json.optBoolean("success", false)) {
                String message = json.optString(
                        "message",
                        "Unable to load students."
                );

                tvResultCount.setText(message);

                addMessageCard(
                        "Request Failed",
                        message,
                        RED
                );

                return;
            }

            JSONArray students = json.optJSONArray("students");

            if (students == null || students.length() == 0) {
                tvResultCount.setText("Students found: 0");

                addMessageCard(
                        "All Clear!",
                        "No students below 75% attendance in "
                                + "Class " + selectedClass
                                + ", Section " + selectedSection + ".",
                        GREEN
                );

                return;
            }

            tvResultCount.setText(
                    "Students found: " + students.length()
                            + "  |  Class " + selectedClass
                            + "-" + selectedSection
            );

            for (int i = 0; i < students.length(); i++) {
                JSONObject student = students.getJSONObject(i);

                String name = student.optString("name", "Unknown");
                String roll = student.optString("roll_number", "N/A");
                String className = student.optString(
                        "class_name",
                        selectedClass
                );
                String section = student.optString(
                        "section",
                        selectedSection
                );

                int present = student.optInt("present_classes", 0);
                int total = student.optInt("total_classes", 0);

                double percentage = student.optDouble(
                        "attendance_percentage",
                        0.0
                );

                percentage = Math.max(0.0, Math.min(100.0, percentage));

                String status = student.optString(
                        "attendance_status",
                        total == 0 ? "No records" : "Below 75%"
                );

                addStudentCard(
                        name,
                        roll,
                        className,
                        section,
                        present,
                        total,
                        percentage,
                        status
                );
            }

        } catch (Exception e) {
            tvResultCount.setText("Invalid response from server.");

            addMessageCard(
                    "Invalid Server Response",
                    "Please check the PHP API response and try again.",
                    RED
            );
        }
    }

    private void addStudentCard(
            String name,
            String roll,
            String className,
            String section,
            int present,
            int total,
            double percentage,
            String status
    ) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));

        card.setBackground(createBackground(
                Color.WHITE,
                Color.rgb(230, 235, 242),
                16
        ));

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.setMargins(0, dp(6), 0, dp(10));
        card.setLayoutParams(cardParams);

        // Student name and attendance badge.
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout identity = new LinearLayout(this);
        identity.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams identityParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                );

        identity.setLayoutParams(identityParams);

        TextView tvName = createText(
                name,
                17,
                DARK,
                true
        );

        identity.addView(tvName);

        TextView tvRoll = createText(
                "Roll Number: " + roll,
                13,
                GRAY,
                false
        );

        LinearLayout.LayoutParams rollParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        rollParams.topMargin = dp(5);
        tvRoll.setLayoutParams(rollParams);

        identity.addView(tvRoll);
        header.addView(identity);

        int percentageColor;

        if (percentage < 50) {
            percentageColor = RED;
        } else if (percentage < 65) {
            percentageColor = ORANGE;
        } else {
            percentageColor = Color.rgb(183, 110, 0);
        }

        LinearLayout badge = new LinearLayout(this);
        badge.setOrientation(LinearLayout.VERTICAL);
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(dp(10), dp(8), dp(10), dp(8));

        badge.setBackground(createBackground(
                Color.rgb(255, 241, 240),
                Color.rgb(255, 220, 215),
                12
        ));

        TextView tvPercentage = createText(
                String.format(
                        Locale.getDefault(),
                        "%.1f%%",
                        percentage
                ),
                18,
                percentageColor,
                true
        );

        badge.addView(tvPercentage);

        TextView tvLow = createText(
                "LOW",
                10,
                percentageColor,
                true
        );

        badge.addView(tvLow);
        header.addView(badge);
        card.addView(header);

        // Class and section.
        TextView tvClass = createText(
                className + "  |  Section " + section,
                13,
                GRAY,
                false
        );

        LinearLayout.LayoutParams classParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        classParams.topMargin = dp(14);
        tvClass.setLayoutParams(classParams);

        card.addView(tvClass);

        // Attendance summary.
        TextView tvSummary = createText(
                "Present: " + present + " / " + total + " classes",
                14,
                DARK,
                true
        );

        LinearLayout.LayoutParams summaryParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        summaryParams.topMargin = dp(10);
        tvSummary.setLayoutParams(summaryParams);

        card.addView(tvSummary);

        // Horizontal attendance progress bar.
        ProgressBar progressBar = new ProgressBar(
                this,
                null,
                android.R.attr.progressBarStyleHorizontal
        );

        progressBar.setMax(100);
        progressBar.setProgress((int) Math.round(percentage));
        progressBar.setProgressTintList(
                ColorStateList.valueOf(percentageColor)
        );
        progressBar.setProgressBackgroundTintList(
                ColorStateList.valueOf(
                        Color.rgb(232, 236, 242)
                )
        );

        LinearLayout.LayoutParams progressParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(9)
                );

        progressParams.topMargin = dp(9);
        progressParams.bottomMargin = dp(9);

        progressBar.setLayoutParams(progressParams);
        card.addView(progressBar);

        // Status.
        TextView tvStatus = createText(
                status.equals("No records")
                        ? "No attendance records available"
                        : "Below required attendance (75%)",
                12,
                percentageColor,
                true
        );

        card.addView(tvStatus);

        layoutStudentList.addView(card);
    }

    private void addMessageCard(
            String title,
            String message,
            int color
    ) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(20), dp(24), dp(20), dp(24));

        card.setBackground(createBackground(
                Color.WHITE,
                Color.rgb(230, 235, 242),
                16
        ));

        TextView tvTitle = createText(
                title,
                18,
                color,
                true
        );

        tvTitle.setGravity(Gravity.CENTER);
        card.addView(tvTitle);

        TextView tvMessage = createText(
                message,
                14,
                GRAY,
                false
        );

        tvMessage.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams messageParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        messageParams.topMargin = dp(8);
        tvMessage.setLayoutParams(messageParams);

        card.addView(tvMessage);
        layoutStudentList.addView(card);
    }

    private TextView createText(
            String text,
            float size,
            int color,
            boolean bold
    ) {
        TextView view = new TextView(this);

        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);

        if (bold) {
            view.setTypeface(null, Typeface.BOLD);
        }

        return view;
    }

    private GradientDrawable createBackground(
            int fillColor,
            int strokeColor,
            int cornerRadius
    ) {
        GradientDrawable drawable = new GradientDrawable();

        drawable.setColor(fillColor);
        drawable.setCornerRadius(dp(cornerRadius));
        drawable.setStroke(dp(1), strokeColor);

        return drawable;
    }

    private int dp(int value) {
        return Math.round(
                value * getResources()
                        .getDisplayMetrics()
                        .density
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
        mainHandler.removeCallbacksAndMessages(null);
    }
}
