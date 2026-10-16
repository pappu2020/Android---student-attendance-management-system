
package com.example.studentattendance;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TimetableActivity extends AppCompatActivity {

    private static final String API_URL =
            "http://10.0.2.2/student_attendance_api/get_timetable.php";

    private static final String PREFS_NAME =
            "StudentAttendanceAuth";

    private Spinner spinnerClass;
    private Spinner spinnerSection;
    private Spinner spinnerDay;

    private Button btnBackTimetable;
    private Button btnLoadTimetable;
    private Button btnRefreshTimetable;

    private ProgressBar progressTimetable;
    private TextView txtTimetableMessage;
    private RecyclerView recyclerTimetable;

    private TimetableAdapter timetableAdapter;

    private String authToken = "";
    private boolean isLoading = false;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timetable);

        initializeViews();
        setupSpinners();
        setupRecyclerView();
        loadSavedToken();
        setupButtonListeners();
    }

    // Initialize XML views
    private void initializeViews() {

        btnBackTimetable =
                findViewById(R.id.btnBackTimetable);

        spinnerClass =
                findViewById(R.id.spinnerClass);

        spinnerSection =
                findViewById(R.id.spinnerSection);

        spinnerDay =
                findViewById(R.id.spinnerDay);

        btnLoadTimetable =
                findViewById(R.id.btnLoadTimetable);

        btnRefreshTimetable =
                findViewById(R.id.btnRefreshTimetable);

        progressTimetable =
                findViewById(R.id.progressTimetable);

        txtTimetableMessage =
                findViewById(R.id.txtTimetableMessage);

        recyclerTimetable =
                findViewById(R.id.recyclerTimetable);
    }

    // Read authentication token saved during login
    private void loadSavedToken() {

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
    }

    // Configure Class, Section and Day dropdowns
    private void setupSpinners() {

        List<String> classList = Arrays.asList(
                "Class 6",
                "Class 7",
                "Class 8",
                "Class 9",
                "Class 10",
                "Class 11",
                "Class 12"
        );

        List<String> sectionList = Arrays.asList(
                "Section A",
                "Section B",
                "Section C"
        );

        List<String> dayList = Arrays.asList(
                "Monday",
                "Tuesday",
                "Wednesday",
                "Thursday",
                "Friday",
                "Saturday"
        );

        setSpinnerAdapter(spinnerClass, classList);
        setSpinnerAdapter(spinnerSection, sectionList);
        setSpinnerAdapter(spinnerDay, dayList);
    }

    private void setSpinnerAdapter(
            Spinner spinner,
            List<String> items) {

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        items
                );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinner.setAdapter(adapter);
    }

    // Configure RecyclerView
    private void setupRecyclerView() {

        recyclerTimetable.setLayoutManager(
                new LinearLayoutManager(this)
        );

        timetableAdapter =
                new TimetableAdapter(new ArrayList<>());

        recyclerTimetable.setAdapter(timetableAdapter);
    }

    // Configure buttons
    private void setupButtonListeners() {

        btnBackTimetable.setOnClickListener(view ->
                finish()
        );

        btnLoadTimetable.setOnClickListener(view ->
                loadTimetable()
        );

        btnRefreshTimetable.setOnClickListener(view ->
                loadTimetable()
        );
    }

    // Request timetable for selected class, section and day
    private void loadTimetable() {

        if (isLoading) {
            return;
        }

        if (authToken == null || authToken.trim().isEmpty()) {
            showError(
                    "Authentication token missing. Please log in again."
            );
            return;
        }

        if (spinnerClass.getSelectedItem() == null
                || spinnerSection.getSelectedItem() == null
                || spinnerDay.getSelectedItem() == null) {

            showError(
                    "Please select class, section and day."
            );
            return;
        }

        String selectedClass =
                spinnerClass.getSelectedItem().toString();

        String selectedSection =
                spinnerSection.getSelectedItem().toString();

        String selectedDay =
                spinnerDay.getSelectedItem().toString();

        int dayNumber = getDayNumber(selectedDay);

        setLoading(true);
        txtTimetableMessage.setText("Loading timetable...");

        executor.execute(() -> {

            String response = fetchTimetable(
                    selectedClass,
                    selectedSection,
                    dayNumber
            );

            runOnUiThread(() -> {

                if (isFinishing() || isDestroyed()) {
                    return;
                }

                setLoading(false);

                parseTimetableResponse(
                        response,
                        selectedClass,
                        selectedSection,
                        selectedDay
                );
            });
        });
    }

    // Call PHP REST API
    private String fetchTimetable(
            String className,
            String section,
            int dayNumber) {

        HttpURLConnection connection = null;

        try {

            String query =
                    "?class_name="
                            + URLEncoder.encode(className, "UTF-8")
                            + "&section="
                            + URLEncoder.encode(section, "UTF-8")
                            + "&day_of_week="
                            + dayNumber;

            URL url = new URL(API_URL + query);

            connection =
                    (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);

            connection.setRequestProperty(
                    "Accept",
                    "application/json"
            );

            connection.setRequestProperty(
                    "Authorization",
                    "Bearer " + authToken
            );

            int responseCode = connection.getResponseCode();

            InputStream inputStream;

            if (responseCode >= 200 && responseCode < 300) {
                inputStream = connection.getInputStream();
            } else {
                inputStream = connection.getErrorStream();
            }

            if (inputStream == null) {
                return "ERROR: HTTP " + responseCode
                        + ". The server returned no response body.";
            }

            StringBuilder result = new StringBuilder();

            try (BufferedReader reader =
                         new BufferedReader(
                                 new InputStreamReader(
                                         inputStream,
                                         StandardCharsets.UTF_8))) {

                String line;

                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
            }

            if (responseCode < 200 || responseCode >= 300) {

                return "ERROR: HTTP "
                        + responseCode
                        + " - "
                        + result;
            }

            return result.toString();

        } catch (SocketTimeoutException e) {

            return "ERROR: Server timed out. Please try again.";

        } catch (UnknownHostException e) {

            return "ERROR: Cannot resolve the server address. "
                    + "Check your connection and server.";

        } catch (Exception e) {

            return "ERROR: Unable to load timetable. "
                    + "Please try again.";

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    // Parse API JSON response and update RecyclerView
    private void parseTimetableResponse(
            String response,
            String selectedClass,
            String selectedSection,
            String selectedDay) {

        if (response == null || response.trim().isEmpty()) {
            showError("Server returned an empty response.");
            return;
        }

        if (response.startsWith("ERROR:")) {
            showError(
                    response.substring("ERROR:".length()).trim()
            );
            return;
        }

        try {

            JSONObject json = new JSONObject(response);

            if (!json.has("success")) {
                showError(
                        "Invalid API response: success field is missing."
                );
                return;
            }

            boolean success = json.optBoolean("success", false);

            if (!success) {

                String message = getValidText(
                        json.optString("message", ""),
                        "Unable to load timetable."
                );

                showError(message);
                return;
            }

            JSONArray data = json.optJSONArray("data");

            if (data == null) {
                showError(
                        "Invalid API response: timetable data is missing."
                );
                return;
            }

            if (data.length() == 0) {

                timetableAdapter.updateData(new ArrayList<>());

                txtTimetableMessage.setText(
                        "No timetable found for "
                                + selectedClass
                                + ", "
                                + selectedSection
                                + ", "
                                + selectedDay
                );

                return;
            }

            List<TimetableItem> items = new ArrayList<>();

            for (int i = 0; i < data.length(); i++) {

                JSONObject row = data.optJSONObject(i);

                if (row == null) {
                    continue;
                }

                String subjectName = getValidText(
                        row.optString("subject_name", ""),
                        "Unknown Subject"
                );

                String startTime = formatTime(
                        row.optString("start_time", "")
                );

                String endTime = formatTime(
                        row.optString("end_time", "")
                );

                String teacherName = getValidText(
                        row.optString("teacher_name", ""),
                        "Not Assigned"
                );

                TimetableItem item = new TimetableItem(
                        subjectName,
                        startTime,
                        endTime,
                        teacherName
                );

                items.add(item);
            }

            if (items.isEmpty()) {

                timetableAdapter.updateData(new ArrayList<>());

                txtTimetableMessage.setText(
                        "No valid timetable records were returned."
                );

                return;
            }

            timetableAdapter.updateData(items);

            txtTimetableMessage.setText(
                    selectedClass
                            + " | "
                            + selectedSection
                            + " | "
                            + selectedDay
                            + "\nTotal periods: "
                            + items.size()
            );

        } catch (JSONException e) {

            showError(
                    "Invalid JSON received from the server. "
                            + "Please check the API response."
            );

        } catch (Exception e) {

            showError(
                    "Unable to display timetable. Please try again."
            );
        }
    }

    // Validate text received from the API
    private String getValidText(
            String value,
            String fallback) {

        if (value == null
                || value.trim().isEmpty()
                || value.trim().equalsIgnoreCase("null")) {

            return fallback;
        }

        return value.trim();
    }

    // Validate time received from the API.
    // Returns HH:mm for valid 24-hour values.
    private String formatTime(String time) {

        if (time == null
                || time.trim().isEmpty()
                || time.trim().equalsIgnoreCase("null")) {

            return "--:--";
        }

        String value = time.trim();

        if (value.matches(
                "^([01]\\d|2[0-3]):[0-5]\\d(:[0-5]\\d)?$")) {

            return value.substring(0, 5);
        }

        if (value.matches(
                "^(0?[1-9]|1[0-2]):[0-5]\\d\\s(?i)(AM|PM)$")) {

            return value;
        }

        return "--:--";
    }

    // Monday = 1, Tuesday = 2, ..., Saturday = 6
    private int getDayNumber(String day) {

        switch (day) {

            case "Monday":
                return 1;

            case "Tuesday":
                return 2;

            case "Wednesday":
                return 3;

            case "Thursday":
                return 4;

            case "Friday":
                return 5;

            case "Saturday":
                return 6;

            default:
                return 1;
        }
    }

    // Show or hide loading indicator
    private void setLoading(boolean loading) {

        isLoading = loading;

        progressTimetable.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );

        btnLoadTimetable.setEnabled(!loading);
        btnRefreshTimetable.setEnabled(!loading);

        spinnerClass.setEnabled(!loading);
        spinnerSection.setEnabled(!loading);
        spinnerDay.setEnabled(!loading);
    }

    // Show error message
    private void showError(String message) {

        timetableAdapter.updateData(new ArrayList<>());

        txtTimetableMessage.setText(message);

        Toast.makeText(
                TimetableActivity.this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    @Override
    protected void onDestroy() {

        executor.shutdownNow();

        super.onDestroy();
    }
}
