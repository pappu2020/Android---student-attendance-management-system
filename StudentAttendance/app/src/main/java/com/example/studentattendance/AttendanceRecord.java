package com.example.studentattendance;

import android.app.DatePickerDialog;
import android.content.Intent;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AttendanceRecord extends AppCompatActivity {

    // =========================================================
    // API
    // =========================================================

    private static final String HISTORY_URL =
            "http://10.0.2.2/student_attendance_api/get_attendance_history.php";

    private static final String PHOTO_BASE_URL =
            "http://10.0.2.2/student_attendance_api/";


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
    // DATA
    // =========================================================

    private final ArrayList<JSONObject> allStudents =
            new ArrayList<>();

    private String selectedDate = "";


    // =========================================================
    // THREAD
    // =========================================================

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

        setContentView(
                R.layout.activity_attendance_record
        );


        // -----------------------------------------------------
        // Find Views
        // -----------------------------------------------------

        spinnerHistoryClass =
                findViewById(
                        R.id.spinnerHistoryClass
                );

        spinnerHistorySection =
                findViewById(
                        R.id.spinnerHistorySection
                );

        tvHistoryDate =
                findViewById(
                        R.id.tvHistoryDate
                );

        btnHistoryRefresh =
                findViewById(
                        R.id.btnHistoryRefresh
                );

        etHistorySearch =
                findViewById(
                        R.id.etHistorySearch
                );

        tvHistoryTotal =
                findViewById(
                        R.id.tvHistoryTotal
                );

        tvHistoryPresent =
                findViewById(
                        R.id.tvHistoryPresent
                );

        tvHistoryAbsent =
                findViewById(
                        R.id.tvHistoryAbsent
                );

        tvHistoryRate =
                findViewById(
                        R.id.tvHistoryRate
                );

        tvHistoryNotMarked =
                findViewById(
                        R.id.tvHistoryNotMarked
                );

        tvHistoryResultCount =
                findViewById(
                        R.id.tvHistoryResultCount
                );

        tvHistoryMessage =
                findViewById(
                        R.id.tvHistoryMessage
                );

        llHistoryList =
                findViewById(
                        R.id.llHistoryList
                );


        // -----------------------------------------------------
        // Setup
        // -----------------------------------------------------

        setupClassSpinner();

        setupSectionSpinner();

        setupDate();

        setupSearch();

        setupRefreshButton();


        // -----------------------------------------------------
        // Set Today's Date
        // -----------------------------------------------------

        Calendar calendar =
                Calendar.getInstance();


        selectedDate =
                new SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.getDefault()
                ).format(
                        calendar.getTime()
                );


        tvHistoryDate.setText(
                new SimpleDateFormat(
                        "dd MMM yyyy",
                        Locale.getDefault()
                ).format(
                        calendar.getTime()
                )
        );


        // -----------------------------------------------------
        // Reset Summary
        // -----------------------------------------------------

        resetSummary();
    }


    // =========================================================
    // REFRESH BUTTON
    // =========================================================

    private void setupRefreshButton() {

        btnHistoryRefresh.setOnClickListener(
                view -> {

                    if (
                            spinnerHistoryClass
                                    .getSelectedItemPosition()
                                    == 0
                    ) {

                        showMessage(
                                "Please select a class first."
                        );

                        return;
                    }


                    if (
                            spinnerHistorySection
                                    .getSelectedItemPosition()
                                    == 0
                    ) {

                        showMessage(
                                "Please select a section first."
                        );

                        return;
                    }


                    if (selectedDate.isEmpty()) {

                        showMessage(
                                "Please select an attendance date."
                        );

                        return;
                    }


                    loadAttendanceHistory();
                }
        );
    }


    // =========================================================
    // CLASS SPINNER
    // =========================================================

    private void setupClassSpinner() {

        ArrayList<String> classes =
                new ArrayList<>();


        classes.add(
                "Select Class"
        );


        for (int i = 6; i <= 12; i++) {

            classes.add(
                    "Class " + i
            );
        }


        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        classes
                );


        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );


        spinnerHistoryClass.setAdapter(
                adapter
        );


        spinnerHistoryClass.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id) {

                        if (position > 0) {

                            loadAttendanceHistory();
                        }
                    }


                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent) {
                    }
                }
        );
    }


    // =========================================================
    // SECTION SPINNER
    // =========================================================

    private void setupSectionSpinner() {

        ArrayList<String> sections =
                new ArrayList<>();


        sections.add(
                "Select Section"
        );

        sections.add(
                "Section A"
        );

        sections.add(
                "Section B"
        );

        sections.add(
                "Section C"
        );

        sections.add(
                "Section D"
        );


        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        sections
                );


        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );


        spinnerHistorySection.setAdapter(
                adapter
        );


        spinnerHistorySection.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id) {

                        if (position > 0) {

                            loadAttendanceHistory();
                        }
                    }


                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent) {
                    }
                }
        );
    }


    // =========================================================
    // DATE SETUP
    // =========================================================

    private void setupDate() {

        tvHistoryDate.setOnClickListener(
                view -> showDatePicker()
        );
    }


    // =========================================================
    // DATE PICKER
    // =========================================================

    private void showDatePicker() {

        Calendar calendar =
                Calendar.getInstance();


        DatePickerDialog dialog =
                new DatePickerDialog(

                        AttendanceRecord.this,

                        (datePicker, year, month, day) -> {

                            Calendar selectedCalendar =
                                    Calendar.getInstance();


                            selectedCalendar.set(
                                    year,
                                    month,
                                    day
                            );


                            selectedDate =
                                    new SimpleDateFormat(
                                            "yyyy-MM-dd",
                                            Locale.getDefault()
                                    ).format(
                                            selectedCalendar.getTime()
                                    );


                            String displayDate =
                                    new SimpleDateFormat(
                                            "dd MMM yyyy",
                                            Locale.getDefault()
                                    ).format(
                                            selectedCalendar.getTime()
                                    );


                            tvHistoryDate.setText(
                                    displayDate
                            );


                            loadAttendanceHistory();
                        },

                        calendar.get(
                                Calendar.YEAR
                        ),

                        calendar.get(
                                Calendar.MONTH
                        ),

                        calendar.get(
                                Calendar.DAY_OF_MONTH
                        )
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
                            int after) {
                    }


                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count) {

                        filterStudents(
                                s.toString()
                        );
                    }


                    @Override
                    public void afterTextChanged(
                            Editable s) {
                    }
                }
        );
    }


    // =========================================================
    // LOAD HISTORY
    // =========================================================

    private void loadAttendanceHistory() {

        if (
                spinnerHistoryClass
                        .getSelectedItemPosition()
                        == 0
        ) {

            return;
        }


        if (
                spinnerHistorySection
                        .getSelectedItemPosition()
                        == 0
        ) {

            return;
        }


        if (selectedDate.isEmpty()) {

            return;
        }


        String className =
                spinnerHistoryClass
                        .getSelectedItem()
                        .toString();


        String section =
                spinnerHistorySection
                        .getSelectedItem()
                        .toString();


        tvHistoryMessage.setText(
                "Loading attendance..."
        );


        btnHistoryRefresh.setEnabled(
                false
        );


        executorService.execute(() -> {

            String response =
                    sendPostRequest(
                            className,
                            section,
                            selectedDate
                    );


            mainHandler.post(
                    () -> {

                        btnHistoryRefresh.setEnabled(
                                true
                        );

                        handleResponse(
                                response
                        );
                    }
            );
        });
    }


    // =========================================================
    // POST REQUEST
    // =========================================================

    private String sendPostRequest(
            String className,
            String section,
            String date) {

        HttpURLConnection connection = null;


        try {

            URL url =
                    new URL(
                            HISTORY_URL
                    );


            connection =
                    (HttpURLConnection)
                            url.openConnection();


            connection.setRequestMethod(
                    "POST"
            );


            connection.setDoOutput(
                    true
            );


            connection.setConnectTimeout(
                    10000
            );


            connection.setReadTimeout(
                    10000
            );


            String postData =
                    "class_name="
                            +
                            URLEncoder.encode(
                                    className,
                                    "UTF-8"
                            )

                            +
                            "&section="
                            +
                            URLEncoder.encode(
                                    section,
                                    "UTF-8"
                            )

                            +
                            "&attendance_date="
                            +
                            URLEncoder.encode(
                                    date,
                                    "UTF-8"
                            );


            OutputStream outputStream =
                    connection.getOutputStream();


            outputStream.write(
                    postData.getBytes(
                            "UTF-8"
                    )
            );


            outputStream.flush();

            outputStream.close();


            int responseCode =
                    connection.getResponseCode();


            InputStream inputStream;


            if (
                    responseCode >= 200
                            &&
                            responseCode < 300
            ) {

                inputStream =
                        connection.getInputStream();

            } else {

                inputStream =
                        connection.getErrorStream();
            }


            if (inputStream == null) {

                return "ERROR:Empty server response";
            }


            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    inputStream
                            )
                    );


            StringBuilder response =
                    new StringBuilder();


            String line;


            while (
                    (line = reader.readLine())
                            != null
            ) {

                response.append(
                        line
                );
            }


            reader.close();


            return response.toString();


        } catch (Exception e) {

            return "ERROR:"
                    +
                    e.getMessage();

        } finally {

            if (connection != null) {

                connection.disconnect();
            }
        }
    }


    // =========================================================
    // HANDLE RESPONSE
    // =========================================================

    private void handleResponse(
            String response) {

        if (
                response == null
                        ||
                        response.trim().isEmpty()
        ) {

            showMessage(
                    "No response from server."
            );

            return;
        }


        if (
                response.startsWith(
                        "ERROR:"
                )
        ) {

            showMessage(
                    "Network error: "
                            +
                            response.substring(
                                    6
                            )
            );

            return;
        }


        try {

            JSONObject json =
                    new JSONObject(
                            response
                    );


            boolean success =
                    json.optBoolean(
                            "success",
                            false
                    );


            if (!success) {

                showMessage(
                        json.optString(
                                "message",
                                "Failed to load attendance."
                        )
                );

                return;
            }


            // -------------------------------------------------
            // Clear Old Data
            // -------------------------------------------------

            allStudents.clear();


            JSONArray students =
                    json.optJSONArray(
                            "students"
                    );


            if (students != null) {

                for (
                        int i = 0;
                        i < students.length();
                        i++
                ) {

                    allStudents.add(
                            students.getJSONObject(
                                    i
                            )
                    );
                }
            }


            // -------------------------------------------------
            // Summary
            // -------------------------------------------------

            int total =
                    json.optInt(
                            "total_students",
                            0
                    );


            int present =
                    json.optInt(
                            "present_count",
                            0
                    );


            int absent =
                    json.optInt(
                            "absent_count",
                            0
                    );


            int notMarked =
                    json.optInt(
                            "not_marked_count",
                            0
                    );


            int markedStudents =
                    present + absent;


            double attendanceRate =
                    0;


            if (markedStudents > 0) {

                attendanceRate =
                        (
                                present * 100.0
                        )
                                /
                                markedStudents;
            }


            tvHistoryTotal.setText(
                    String.valueOf(
                            total
                    )
            );


            tvHistoryPresent.setText(
                    String.valueOf(
                            present
                    )
            );


            tvHistoryAbsent.setText(
                    String.valueOf(
                            absent
                    )
            );


            tvHistoryNotMarked.setText(
                    "Not Marked: "
                            +
                            notMarked
            );


            tvHistoryRate.setText(
                    String.format(
                            Locale.getDefault(),
                            "%.1f%%",
                            attendanceRate
                    )
            );


            tvHistoryMessage.setText(
                    ""
            );


            // -------------------------------------------------
            // Display Students
            // -------------------------------------------------

            filterStudents(
                    etHistorySearch
                            .getText()
                            .toString()
            );


        } catch (Exception e) {

            showMessage(
                    "Invalid server response."
            );
        }
    }


    // =========================================================
    // FILTER STUDENTS
    // =========================================================

    private void filterStudents(
            String searchText) {

        llHistoryList.removeAllViews();


        String query =
                searchText
                        .trim()
                        .toLowerCase(
                                Locale.getDefault()
                        );


        int matchedCount =
                0;


        for (
                JSONObject student :
                allStudents
        ) {

            String name =
                    student.optString(
                            "name",
                            ""
                    );


            String roll =
                    student.optString(
                            "roll_number",
                            ""
                    );


            boolean matches;


            if (query.isEmpty()) {

                matches = true;

            } else {

                matches =
                        name.toLowerCase(
                                Locale.getDefault()
                        ).contains(
                                query
                        )

                                ||

                                roll.toLowerCase(
                                        Locale.getDefault()
                                ).contains(
                                        query
                                );
            }


            if (matches) {

                addStudentCard(
                        student
                );

                matchedCount++;
            }
        }


        tvHistoryResultCount.setText(
                "Showing "
                        +
                        matchedCount
                        +
                        " of "
                        +
                        allStudents.size()
                        +
                        " students"
        );


        if (matchedCount == 0) {

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

            tvHistoryMessage.setText(
                    ""
            );
        }
    }


    // =========================================================
    // CREATE STUDENT CARD
    // =========================================================

    private void addStudentCard(
            JSONObject student) {

        // -----------------------------------------------------
        // Main Card
        // -----------------------------------------------------

        LinearLayout card =
                new LinearLayout(
                        this
                );


        card.setOrientation(
                LinearLayout.VERTICAL
        );


        card.setPadding(
                16,
                16,
                16,
                16
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
                12
        );


        card.setLayoutParams(
                cardParams
        );


        GradientDrawable cardBackground =
                new GradientDrawable();


        cardBackground.setColor(
                Color.WHITE
        );


        cardBackground.setCornerRadius(
                22
        );


        cardBackground.setStroke(
                1,
                Color.rgb(
                        230,
                        235,
                        240
                )
        );


        card.setBackground(
                cardBackground
        );


        // -----------------------------------------------------
        // Clickable Student Card
        // -----------------------------------------------------

        card.setClickable(
                true
        );


        card.setFocusable(
                true
        );


        card.setOnClickListener(
                view -> {

                    int studentId =
                            student.optInt(
                                    "id",
                                    0
                            );


                    if (studentId <= 0) {

                        return;
                    }


                    Intent intent =
                            new Intent(
                                    AttendanceRecord.this,
                                    StudentAttendanceDetailsActivity.class
                            );


                    intent.putExtra(
                            "student_id",
                            studentId
                    );


                    startActivity(
                            intent
                    );
                }
        );


        // -----------------------------------------------------
        // TOP ROW
        // -----------------------------------------------------

        LinearLayout topRow =
                new LinearLayout(
                        this
                );


        topRow.setOrientation(
                LinearLayout.HORIZONTAL
        );


        topRow.setGravity(
                Gravity.CENTER_VERTICAL
        );


        // -----------------------------------------------------
        // STUDENT PHOTO
        // -----------------------------------------------------

        ImageView imageView =
                new ImageView(
                        this
                );


        LinearLayout.LayoutParams imageParams =
                new LinearLayout.LayoutParams(
                        64,
                        64
                );


        imageParams.setMargins(
                0,
                0,
                14,
                0
        );


        imageView.setLayoutParams(
                imageParams
        );


        imageView.setScaleType(
                ImageView.ScaleType.CENTER_CROP
        );


        GradientDrawable imageBackground =
                new GradientDrawable();


        imageBackground.setColor(
                Color.rgb(
                        227,
                        242,
                        253
                )
        );


        imageBackground.setShape(
                GradientDrawable.OVAL
        );


        imageView.setBackground(
                imageBackground
        );


        // -----------------------------------------------------
        // INFORMATION LAYOUT
        // -----------------------------------------------------

        LinearLayout infoLayout =
                new LinearLayout(
                        this
                );


        infoLayout.setOrientation(
                LinearLayout.VERTICAL
        );


        LinearLayout.LayoutParams infoParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                );


        infoLayout.setLayoutParams(
                infoParams
        );


        // -----------------------------------------------------
        // NAME
        // -----------------------------------------------------

        TextView nameView =
                new TextView(
                        this
                );


        nameView.setText(
                student.optString(
                        "name",
                        "Unknown Student"
                )
        );


        nameView.setTextColor(
                Color.rgb(
                        38,
                        50,
                        56
                )
        );


        nameView.setTextSize(
                18
        );


        nameView.setTypeface(
                null,
                Typeface.BOLD
        );


        infoLayout.addView(
                nameView
        );


        // -----------------------------------------------------
        // ROLL
        // -----------------------------------------------------

        TextView rollView =
                createInfoText(
                        "Roll No: "
                                +
                                student.optString(
                                        "roll_number",
                                        "-"
                                )
                );


        infoLayout.addView(
                rollView
        );


        // -----------------------------------------------------
        // CLASS + SECTION
        // -----------------------------------------------------

        TextView classView =
                createInfoText(
                        student.optString(
                                "class_name",
                                "-"
                        )
                                +
                                " • "
                                +
                                student.optString(
                                        "section",
                                        "-"
                                )
                );


        infoLayout.addView(
                classView
        );


        // -----------------------------------------------------
        // DATE
        // -----------------------------------------------------

        String attendanceDate =
                student.optString(
                        "attendance_date",
                        selectedDate
                );


        TextView dateView =
                createInfoText(
                        "Date: "
                                +
                                formatAttendanceDate(
                                        attendanceDate
                                )
                );


        infoLayout.addView(
                dateView
        );


        // -----------------------------------------------------
        // Add Photo + Info
        // -----------------------------------------------------

        topRow.addView(
                imageView
        );


        topRow.addView(
                infoLayout
        );


        card.addView(
                topRow
        );


        // -----------------------------------------------------
        // LOAD PHOTO
        // -----------------------------------------------------

        String photo =
                student.optString(
                        "photo",
                        ""
                );


        if (
                photo != null
                        &&
                        !photo.trim().isEmpty()
                        &&
                        !photo.equalsIgnoreCase(
                                "null"
                        )
        ) {

            String photoUrl =
                    PHOTO_BASE_URL
                            +
                            photo;


            Glide.with(
                            AttendanceRecord.this
                    )
                    .load(
                            photoUrl
                    )
                    .placeholder(
                            android.R.drawable
                                    .ic_menu_gallery
                    )
                    .error(
                            android.R.drawable
                                    .ic_menu_gallery
                    )
                    .circleCrop()
                    .into(
                            imageView
                    );

        } else {

            imageView.setImageResource(
                    android.R.drawable
                            .ic_menu_myplaces
            );
        }


        // -----------------------------------------------------
        // STATUS
        // -----------------------------------------------------

        String status =
                student.optString(
                        "status",
                        "Not Marked"
                );


        TextView statusView =
                new TextView(
                        this
                );


        statusView.setText(
                getStatusText(
                        status
                )
        );


        statusView.setTextSize(
                14
        );


        statusView.setTypeface(
                null,
                Typeface.BOLD
        );


        statusView.setGravity(
                Gravity.CENTER
        );


        statusView.setPadding(
                12,
                11,
                12,
                11
        );


        LinearLayout.LayoutParams statusParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );


        statusParams.setMargins(
                0,
                14,
                0,
                0
        );


        statusView.setLayoutParams(
                statusParams
        );


        GradientDrawable statusBackground =
                new GradientDrawable();


        statusBackground.setCornerRadius(
                14
        );


        // -----------------------------------------------------
        // PRESENT
        // -----------------------------------------------------

        if (
                status.equalsIgnoreCase(
                        "Present"
                )
        ) {

            statusBackground.setColor(
                    Color.rgb(
                            232,
                            245,
                            233
                    )
            );


            statusView.setTextColor(
                    Color.rgb(
                            46,
                            125,
                            50
                    )
            );
        }


        // -----------------------------------------------------
        // ABSENT
        // -----------------------------------------------------

        else if (
                status.equalsIgnoreCase(
                        "Absent"
                )
        ) {

            statusBackground.setColor(
                    Color.rgb(
                            255,
                            235,
                            238
                    )
            );


            statusView.setTextColor(
                    Color.rgb(
                            198,
                            40,
                            40
                    )
            );
        }


        // -----------------------------------------------------
        // NOT MARKED
        // -----------------------------------------------------

        else {

            statusBackground.setColor(
                    Color.rgb(
                            245,
                            245,
                            245
                    )
            );


            statusView.setTextColor(
                    Color.rgb(
                            84,
                            110,
                            122
                    )
            );
        }


        statusView.setBackground(
                statusBackground
        );


        card.addView(
                statusView
        );


        // -----------------------------------------------------
        // ADD CARD
        // -----------------------------------------------------

        llHistoryList.addView(
                card
        );
    }


    // =========================================================
    // CREATE INFO TEXT
    // =========================================================

    private TextView createInfoText(
            String text) {

        TextView textView =
                new TextView(
                        this
                );


        textView.setText(
                text
        );


        textView.setTextColor(
                Color.rgb(
                        84,
                        110,
                        122
                )
        );


        textView.setTextSize(
                14
        );


        textView.setPadding(
                0,
                4,
                0,
                0
        );


        return textView;
    }


    // =========================================================
    // FORMAT DATE
    // =========================================================

    private String formatAttendanceDate(
            String date) {

        try {

            SimpleDateFormat inputFormat =
                    new SimpleDateFormat(
                            "yyyy-MM-dd",
                            Locale.getDefault()
                    );


            SimpleDateFormat outputFormat =
                    new SimpleDateFormat(
                            "dd MMM yyyy",
                            Locale.getDefault()
                    );


            return outputFormat.format(
                    inputFormat.parse(
                            date
                    )
            );


        } catch (Exception e) {

            return date;
        }
    }


    // =========================================================
    // STATUS TEXT
    // =========================================================

    private String getStatusText(
            String status) {

        if (
                status.equalsIgnoreCase(
                        "Present"
                )
        ) {

            return "✓  PRESENT";
        }


        if (
                status.equalsIgnoreCase(
                        "Absent"
                )
        ) {

            return "✕  ABSENT";
        }


        return "○  NOT MARKED";
    }


    // =========================================================
    // RESET SUMMARY
    // =========================================================

    private void resetSummary() {

        tvHistoryTotal.setText(
                "0"
        );


        tvHistoryPresent.setText(
                "0"
        );


        tvHistoryAbsent.setText(
                "0"
        );


        tvHistoryRate.setText(
                "0%"
        );


        tvHistoryNotMarked.setText(
                "Not Marked: 0"
        );
    }


    // =========================================================
    // SHOW MESSAGE
    // =========================================================

    private void showMessage(
            String message) {

        tvHistoryMessage.setText(
                message
        );


        tvHistoryMessage.setTextColor(
                Color.rgb(
                        84,
                        110,
                        122
                )
        );


        tvHistoryResultCount.setText(
                "No results"
        );


        llHistoryList.removeAllViews();
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