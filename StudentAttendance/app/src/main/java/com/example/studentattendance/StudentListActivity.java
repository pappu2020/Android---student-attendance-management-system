package com.example.studentattendance;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StudentListActivity extends AppCompatActivity {

    private static final String BASE_URL =
            "http://10.0.2.2/student_attendance_api/";

    private static final String GET_STUDENTS_URL =
            BASE_URL + "get_students_by_class_section.php";

    private static final String DELETE_STUDENT_URL =
            BASE_URL + "delete_student.php";

    private static final String PREFS_NAME =
            "StudentAttendanceAuth";

    private Spinner spinnerStudentClass;
    private Spinner spinnerStudentSection;

    private TextView tvStudentListTitle;
    private TextView tvStudentListCount;
    private TextView tvMessage;

    private LinearLayout llStudentList;

    private String selectedClass = "";
    private String selectedSection = "";

    private String authToken = "";
    private String userRole = "";

    private boolean isAdmin = false;

    private RequestQueue requestQueue;

    // Background executor for loading student photos.
    private final ExecutorService imageExecutor =
            Executors.newFixedThreadPool(3);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_list);

        SharedPreferences preferences =
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        authToken = preferences.getString("auth_token", "");

        userRole = preferences.getString("user_role", "")
                .trim()
                .toLowerCase(Locale.ROOT);

        isAdmin = "admin".equals(userRole);

        // Only authenticated Admin and Teacher users are allowed.
        if (authToken.trim().isEmpty()
                || (!isAdmin && !"teacher".equals(userRole))) {

            Toast.makeText(
                    this,
                    "Session expired or access denied. Please log in again.",
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }

        requestQueue = Volley.newRequestQueue(
                getApplicationContext()
        );

        initializeViews();
        setupClassSpinner();
        setupSectionSpinner();
        setupSpinnerListeners();
    }

    // ---------------------------------------------------------
    // INITIALIZE VIEWS
    // ---------------------------------------------------------

    private void initializeViews() {

        spinnerStudentClass =
                findViewById(R.id.spinnerStudentClass);

        spinnerStudentSection =
                findViewById(R.id.spinnerStudentSection);

        tvStudentListTitle =
                findViewById(R.id.tvStudentListTitle);

        tvStudentListCount =
                findViewById(R.id.tvStudentListCount);

        tvMessage =
                findViewById(R.id.tvMessage);

        llStudentList =
                findViewById(R.id.llStudentList);

        if (tvStudentListTitle != null) {
            tvStudentListTitle.setText("Student List");
        }

        if (tvStudentListCount != null) {
            tvStudentListCount.setText(
                    "Select class and section"
            );
        }

        if (tvMessage != null) {
            tvMessage.setText("");
        }
    }

    // ---------------------------------------------------------
    // CLASS SPINNER
    // ---------------------------------------------------------

    private void setupClassSpinner() {

        String[] classes = {
                "Select Class",
                "Class 6",
                "Class 7",
                "Class 8",
                "Class 9",
                "Class 10",
                "Class 11",
                "Class 12"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                classes
        );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerStudentClass.setAdapter(adapter);
    }

    // ---------------------------------------------------------
    // SECTION SPINNER
    // ---------------------------------------------------------

    private void setupSectionSpinner() {

        String[] sections = {
                "Select Section",
                "Section A",
                "Section B",
                "Section C",
                "Section D"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                sections
        );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerStudentSection.setAdapter(adapter);
    }

    // ---------------------------------------------------------
    // SPINNER LISTENERS
    // ---------------------------------------------------------

    private void setupSpinnerListeners() {

        spinnerStudentClass.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id) {

                        selectedClass =
                                parent.getItemAtPosition(position)
                                        .toString();

                        tryLoadStudents();
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent) {
                    }
                }
        );

        spinnerStudentSection.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id) {

                        selectedSection =
                                parent.getItemAtPosition(position)
                                        .toString();

                        tryLoadStudents();
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent) {
                    }
                }
        );
    }

    // ---------------------------------------------------------
    // LOAD STUDENTS WHEN FILTERS ARE SELECTED
    // ---------------------------------------------------------

    private void tryLoadStudents() {

        if (selectedClass == null
                || selectedClass.equals("Select Class")) {
            return;
        }

        if (selectedSection == null
                || selectedSection.equals("Select Section")) {
            return;
        }

        loadStudents();
    }

    // ---------------------------------------------------------
    // LOAD STUDENTS WITH BEARER TOKEN
    // ---------------------------------------------------------

    private void loadStudents() {

        if (llStudentList == null) {
            return;
        }

        showLoadingMessage();

        StringRequest request = new StringRequest(
                Request.Method.POST,
                GET_STUDENTS_URL,

                response -> {
                    hideLoadingMessage();

                    try {
                        JSONObject json = new JSONObject(response);

                        if (!json.optBoolean("success", false)) {
                            showError(
                                    json.optString(
                                            "message",
                                            "Unable to load students"
                                    )
                            );
                            return;
                        }

                        JSONArray students =
                                json.optJSONArray("students");

                        if (students == null) {
                            showEmptyMessage();
                            return;
                        }

                        displayStudents(students);

                    } catch (Exception e) {
                        showError("Invalid server response");
                    }
                },

                error -> {
                    hideLoadingMessage();

                    handleRequestError(
                            error,
                            "Unable to load students"
                    );
                }
        ) {
            @Override
            protected Map<String, String> getParams() {

                Map<String, String> params = new HashMap<>();

                params.put("class_name", selectedClass);
                params.put("section", selectedSection);

                return params;
            }

            @Override
            public Map<String, String> getHeaders()
                    throws AuthFailureError {

                return createAuthHeaders();
            }
        };

        requestQueue.add(request);
    }

    // ---------------------------------------------------------
    // DISPLAY STUDENTS
    // ---------------------------------------------------------

    private void displayStudents(JSONArray students) {

        llStudentList.removeAllViews();

        int totalStudents = students.length();

        if (tvStudentListCount != null) {
            tvStudentListCount.setText(
                    "Total Students: " + totalStudents
            );
        }

        if (totalStudents == 0) {
            showEmptyMessage();
            return;
        }

        if (tvMessage != null) {
            tvMessage.setText("");
        }

        for (int i = 0; i < students.length(); i++) {

            try {
                JSONObject student = students.getJSONObject(i);

                String id = student.optString("id", "");

                String name = student.optString(
                        "name",
                        "Unknown Student"
                );

                String rollNumber =
                        student.optString("roll_number", "");

                String className =
                        student.optString("class_name", selectedClass);

                String section =
                        student.optString("section", selectedSection);

                String email =
                        student.optString("email", "");

                String phone =
                        student.optString("phone", "");

                String photo =
                        student.optString("photo", "");

                createStudentCard(
                        id,
                        name,
                        rollNumber,
                        className,
                        section,
                        email,
                        phone,
                        photo
                );

            } catch (Exception ignored) {
                // Skip malformed student records.
            }
        }
    }

    // ---------------------------------------------------------
    // CREATE STUDENT CARD
    // ---------------------------------------------------------

    private void createStudentCard(
            String id,
            String name,
            String rollNumber,
            String className,
            String section,
            String email,
            String phone,
            String photo) {

        LinearLayout card = new LinearLayout(this);

        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(24, 24, 24, 24);

        GradientDrawable cardBackground = new GradientDrawable();

        cardBackground.setColor(Color.WHITE);
        cardBackground.setCornerRadius(24);
        cardBackground.setStroke(
                1,
                Color.rgb(225, 225, 225)
        );

        card.setBackground(cardBackground);

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.setMargins(0, 0, 0, 20);
        card.setLayoutParams(cardParams);

        // Top row contains photo/avatar, name and roll number.
        LinearLayout topRow = new LinearLayout(this);

        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        // Initial-letter fallback.
        String firstLetter = "S";

        if (name != null && !name.trim().isEmpty()) {
            firstLetter = name.trim()
                    .substring(0, 1)
                    .toUpperCase(Locale.ROOT);
        }

        TextView avatar = new TextView(this);

        avatar.setText(firstLetter);
        avatar.setTextSize(20);
        avatar.setTextColor(Color.WHITE);
        avatar.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        avatar.setGravity(Gravity.CENTER);

        GradientDrawable avatarBackground = new GradientDrawable();

        avatarBackground.setColor(Color.rgb(33, 150, 243));
        avatarBackground.setShape(GradientDrawable.OVAL);

        avatar.setBackground(avatarBackground);

        // Actual student photo.
        ImageView studentPhotoView = new ImageView(this);

        studentPhotoView.setScaleType(
                ImageView.ScaleType.CENTER_CROP
        );

        studentPhotoView.setBackground(avatarBackground);
        studentPhotoView.setVisibility(View.GONE);
        studentPhotoView.setClipToOutline(true);
        studentPhotoView.setContentDescription("Student photo");

        // Stack avatar and photo in the same frame.
        FrameLayout avatarFrame = new FrameLayout(this);

        avatarFrame.setBackground(avatarBackground);

        FrameLayout.LayoutParams frameParams =
                new FrameLayout.LayoutParams(60, 60);

        avatarFrame.addView(avatar, frameParams);
        avatarFrame.addView(studentPhotoView, frameParams);

        LinearLayout.LayoutParams avatarParams =
                new LinearLayout.LayoutParams(60, 60);

        avatarParams.setMargins(0, 0, 20, 0);

        topRow.addView(avatarFrame, avatarParams);

        // Student name and roll number.
        LinearLayout nameLayout = new LinearLayout(this);

        nameLayout.setOrientation(LinearLayout.VERTICAL);

        TextView tvName = new TextView(this);

        tvName.setText(name);
        tvName.setTextSize(19);
        tvName.setTextColor(Color.rgb(30, 30, 30));
        tvName.setTypeface(Typeface.DEFAULT, Typeface.BOLD);

        TextView tvRoll = new TextView(this);

        tvRoll.setText("Roll No: " + rollNumber);
        tvRoll.setTextSize(14);
        tvRoll.setTextColor(Color.rgb(100, 100, 100));

        nameLayout.addView(tvName);
        nameLayout.addView(tvRoll);

        topRow.addView(
                nameLayout,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        card.addView(topRow);

        // Student information.
        card.addView(createInfoText(
                "Class: " + className + " | Section: " + section
        ));

        if (email != null && !email.trim().isEmpty()) {
            card.addView(createInfoText("Email: " + email));
        }

        if (phone != null && !phone.trim().isEmpty()) {
            card.addView(createInfoText("Phone: " + phone));
        }

        card.addView(createInfoText("Student ID: " + id));

        // Load photo asynchronously.
        loadStudentPhoto(studentPhotoView, photo);

        // Admin-only Edit and Delete buttons.
        if (isAdmin) {

            LinearLayout buttonLayout = new LinearLayout(this);

            buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
            buttonLayout.setGravity(Gravity.CENTER);
            buttonLayout.setPadding(0, 20, 0, 0);

            Button btnEdit = createActionButton(
                    "EDIT",
                    Color.rgb(33, 150, 243)
            );

            Button btnDelete = createActionButton(
                    "DELETE",
                    Color.rgb(211, 47, 47)
            );

            LinearLayout.LayoutParams editParams =
                    new LinearLayout.LayoutParams(0, 55, 1);

            editParams.setMargins(0, 0, 8, 0);

            LinearLayout.LayoutParams deleteParams =
                    new LinearLayout.LayoutParams(0, 55, 1);

            deleteParams.setMargins(8, 0, 0, 0);

            buttonLayout.addView(btnEdit, editParams);
            buttonLayout.addView(btnDelete, deleteParams);

            btnEdit.setOnClickListener(v -> {

                if (!isAdmin) {
                    showError("Admin permission required.");
                    return;
                }

                openEditStudent(
                        id,
                        name,
                        rollNumber,
                        className,
                        section,
                        email,
                        phone,
                        photo
                );
            });

            btnDelete.setOnClickListener(v -> {

                if (!isAdmin) {
                    showError("Admin permission required.");
                    return;
                }

                confirmDeleteStudent(id, name);
            });

            card.addView(buttonLayout);
        }

        llStudentList.addView(card);
    }

    // ---------------------------------------------------------
    // LOAD STUDENT PHOTO
    // ---------------------------------------------------------

    private void loadStudentPhoto(
            ImageView imageView,
            String photo) {

        if (photo == null
                || photo.trim().isEmpty()
                || "null".equalsIgnoreCase(photo.trim())) {
            return;
        }

        String fileName = photo.trim();

        // Only accept a filename, not an arbitrary path or URL.
        if (fileName.contains("/")
                || fileName.contains("\\")
                || fileName.contains("..")) {
            return;
        }

        String imageUrl = BASE_URL
                + "uploads/students/"
                + fileName;

        imageExecutor.execute(() -> {

            HttpURLConnection connection = null;
            InputStream inputStream = null;
            Bitmap bitmap = null;

            try {
                URL url = new URL(imageUrl);

                connection =
                        (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(15000);
                connection.setUseCaches(false);

                int responseCode = connection.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    inputStream = connection.getInputStream();

                    bitmap = BitmapFactory.decodeStream(inputStream);
                }

            } catch (Exception ignored) {
                // Keep the initial-letter avatar if loading fails.
            } finally {

                try {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                } catch (Exception ignored) {
                }

                if (connection != null) {
                    connection.disconnect();
                }
            }

            if (bitmap == null) {
                return;
            }

            final Bitmap loadedBitmap = bitmap;

            runOnUiThread(() -> {

                if (isFinishing() || isDestroyed()) {
                    return;
                }

                studentPhotoViewUpdate(imageView, loadedBitmap);
            });
        });
    }

    private void studentPhotoViewUpdate(
            ImageView imageView,
            Bitmap bitmap) {

        imageView.setImageBitmap(bitmap);
        imageView.setVisibility(View.VISIBLE);

        // Hide the fallback initial when the real photo loads.
        if (imageView.getParent() instanceof FrameLayout) {

            FrameLayout frame =
                    (FrameLayout) imageView.getParent();

            if (frame.getChildCount() > 0) {
                View fallback = frame.getChildAt(0);
                fallback.setVisibility(View.GONE);
            }
        }
    }

    // ---------------------------------------------------------
    // CREATE INFORMATION TEXT
    // ---------------------------------------------------------

    private TextView createInfoText(String text) {

        TextView textView = new TextView(this);

        textView.setText(text);
        textView.setTextSize(14);
        textView.setTextColor(Color.rgb(80, 80, 80));
        textView.setPadding(0, 12, 0, 0);

        return textView;
    }

    // ---------------------------------------------------------
    // CREATE ACTION BUTTON
    // ---------------------------------------------------------

    private Button createActionButton(
            String text,
            int backgroundColor) {

        Button button = new Button(this);

        button.setText(text);
        button.setTextSize(14);
        button.setTextColor(Color.WHITE);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setAllCaps(false);

        GradientDrawable background = new GradientDrawable();

        background.setColor(backgroundColor);
        background.setCornerRadius(16);

        button.setBackground(background);
        button.setMinHeight(55);
        button.setMinimumHeight(55);

        return button;
    }

    // ---------------------------------------------------------
    // OPEN EDIT STUDENT
    // ---------------------------------------------------------

    private void openEditStudent(
            String id,
            String name,
            String rollNumber,
            String className,
            String section,
            String email,
            String phone,
            String photo) {

        if (!isAdmin) {
            showError("Admin permission required.");
            return;
        }

        Intent intent = new Intent(
                StudentListActivity.this,
                StudentActivity.class
        );

        intent.putExtra("edit_mode", true);
        intent.putExtra("student_id", id);
        intent.putExtra("student_name", name);
        intent.putExtra("student_roll", rollNumber);
        intent.putExtra("student_class", className);
        intent.putExtra("student_section", section);
        intent.putExtra("student_email", email);
        intent.putExtra("student_phone", phone);
        intent.putExtra("student_photo", photo);

        startActivity(intent);
    }

    // ---------------------------------------------------------
    // DELETE CONFIRMATION
    // ---------------------------------------------------------

    private void confirmDeleteStudent(
            String studentId,
            String studentName) {

        if (!isAdmin) {
            showError("Admin permission required.");
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Delete Student")
                .setMessage(
                        "Are you sure you want to delete "
                                + studentName + "?"
                )
                .setPositiveButton(
                        "DELETE",
                        (dialog, which) -> deleteStudent(studentId)
                )
                .setNegativeButton("CANCEL", null)
                .show();
    }

    // ---------------------------------------------------------
    // DELETE STUDENT WITH AUTHENTICATION
    // ---------------------------------------------------------

    private void deleteStudent(String studentId) {

        if (!isAdmin) {
            showError("Admin permission required.");
            return;
        }

        StringRequest request = new StringRequest(
                Request.Method.POST,
                DELETE_STUDENT_URL,

                response -> {
                    try {
                        JSONObject json = new JSONObject(response);

                        boolean success =
                                json.optBoolean("success", false);

                        String message = json.optString(
                                "message",
                                "Unable to delete student"
                        );

                        if (success) {

                            Toast.makeText(
                                    this,
                                    "Student deleted successfully",
                                    Toast.LENGTH_SHORT
                            ).show();

                            loadStudents();

                        } else {
                            showError(message);
                        }

                    } catch (Exception e) {
                        showError("Invalid server response");
                    }
                },

                error -> handleRequestError(
                        error,
                        "Delete failed"
                )
        ) {
            @Override
            public Map<String, String> getHeaders()
                    throws AuthFailureError {

                return createAuthHeaders();
            }

            @Override
            protected Map<String, String> getParams() {

                Map<String, String> params = new HashMap<>();

                params.put("id", studentId);

                return params;
            }
        };

        requestQueue.add(request);
    }

    // ---------------------------------------------------------
    // AUTHENTICATION HEADERS
    // ---------------------------------------------------------

    private Map<String, String> createAuthHeaders() {

        Map<String, String> headers = new HashMap<>();

        headers.put("Authorization", "Bearer " + authToken);
        headers.put("Accept", "application/json");

        return headers;
    }

    // ---------------------------------------------------------
    // NETWORK ERROR HANDLING
    // ---------------------------------------------------------

    private void handleRequestError(
            com.android.volley.VolleyError error,
            String defaultMessage) {

        if (error.networkResponse != null) {

            int statusCode = error.networkResponse.statusCode;

            if (statusCode == 401) {
                showError(
                        "Session expired. Please log in again."
                );
                return;
            }

            if (statusCode == 403) {
                showError("Admin permission required.");
                return;
            }

            showError("Server error: HTTP " + statusCode);
            return;
        }

        showError(
                defaultMessage
                        + ". Check your server connection."
        );
    }

    // ---------------------------------------------------------
    // LOADING MESSAGE
    // ---------------------------------------------------------

    private void showLoadingMessage() {

        if (tvMessage != null) {
            tvMessage.setText("Loading students...");
            tvMessage.setTextColor(Color.rgb(33, 150, 243));
        }
    }

    private void hideLoadingMessage() {

        if (tvMessage != null) {
            tvMessage.setText("");
        }
    }

    // ---------------------------------------------------------
    // EMPTY STATE
    // ---------------------------------------------------------

    private void showEmptyMessage() {

        if (llStudentList != null) {
            llStudentList.removeAllViews();
        }

        if (tvStudentListCount != null) {
            tvStudentListCount.setText("Total Students: 0");
        }

        if (tvMessage != null) {

            tvMessage.setText(
                    "No students found for "
                            + selectedClass
                            + " - "
                            + selectedSection
            );

            tvMessage.setTextColor(Color.rgb(117, 117, 117));
        }
    }

    // ---------------------------------------------------------
    // SHOW ERROR
    // ---------------------------------------------------------

    private void showError(String message) {

        if (tvMessage != null) {
            tvMessage.setText(message);
            tvMessage.setTextColor(Color.rgb(211, 47, 47));
        }

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    // ---------------------------------------------------------
    // REFRESH WHEN RETURNING FROM EDIT SCREEN
    // ---------------------------------------------------------

    @Override
    protected void onResume() {
        super.onResume();

        if (requestQueue != null
                && selectedClass != null
                && !selectedClass.equals("Select Class")
                && selectedSection != null
                && !selectedSection.equals("Select Section")) {

            loadStudents();
        }
    }

    // ---------------------------------------------------------
    // CLEAN UP
    // ---------------------------------------------------------

    @Override
    protected void onDestroy() {
        super.onDestroy();

        imageExecutor.shutdownNow();
    }
}