package com.example.studentattendance;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class StudentActivity extends AppCompatActivity {

    private static final String BASE_URL =
            "http://10.0.2.2/student_attendance_api/";

    private static final String ADD_STUDENT_URL =
            BASE_URL + "add_student.php";

    private static final String UPDATE_STUDENT_URL =
            BASE_URL + "update_student.php";

    private static final String UPLOAD_PHOTO_URL =
            BASE_URL + "upload_student_photo.php";

    private static final String PREFS_NAME =
            "StudentAttendanceAuth";

    private static final int PICK_IMAGE_REQUEST = 1001;

    private EditText etStudentName;
    private EditText etRollNumber;
    private EditText etStudentEmail;
    private EditText etStudentPhone;

    private Spinner spinnerStudentClass;
    private Spinner spinnerStudentSection;

    private Button btnAddStudent;
    private Button btnCancelEdit;
    private Button btnSelectPhoto;

    private ProgressBar progressBar;
    private TextView tvMessage;
    private ImageView ivStudentPhoto;

    private boolean isEditMode = false;
    private String editingStudentId = "";
    private Uri selectedImageUri = null;

    private String authToken = "";

    private RequestQueue requestQueue;
    private boolean requestInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student);

        SharedPreferences preferences =
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        authToken = preferences.getString("auth_token", "");

        String role = preferences.getString("user_role", "")
                .trim()
                .toLowerCase(Locale.ROOT);

        // Only Admin can manage student records.
        if (authToken.trim().isEmpty()
                || !"admin".equals(role)) {

            Toast.makeText(
                    this,
                    "Admin permission required. Please log in again.",
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }

        initializeViews();

        requestQueue = Volley.newRequestQueue(
                getApplicationContext()
        );

        setupClassSpinner();
        setupSectionSpinner();
        checkEditMode();

        btnAddStudent.setOnClickListener(view -> {
            if (isEditMode) {
                updateStudent();
            } else {
                addStudent();
            }
        });

        btnCancelEdit.setOnClickListener(view -> finish());

        btnSelectPhoto.setOnClickListener(view -> selectPhoto());
    }

    // ---------------------------------------------------------
    // INITIALIZE VIEWS
    // ---------------------------------------------------------

    private void initializeViews() {

        etStudentName = findViewById(R.id.etStudentName);
        etRollNumber = findViewById(R.id.etRollNumber);
        etStudentEmail = findViewById(R.id.etStudentEmail);
        etStudentPhone = findViewById(R.id.etStudentPhone);

        spinnerStudentClass =
                findViewById(R.id.spinnerStudentClass);

        spinnerStudentSection =
                findViewById(R.id.spinnerStudentSection);

        btnAddStudent = findViewById(R.id.btnAddStudent);
        btnCancelEdit = findViewById(R.id.btnCancelEdit);
        btnSelectPhoto = findViewById(R.id.btnSelectPhoto);

        progressBar = findViewById(R.id.progressBar);
        tvMessage = findViewById(R.id.tvMessage);
        ivStudentPhoto = findViewById(R.id.ivStudentPhoto);

        progressBar.setVisibility(View.GONE);
        btnCancelEdit.setVisibility(View.GONE);
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
    // EDIT MODE
    // ---------------------------------------------------------

    private void checkEditMode() {

        Intent intent = getIntent();

        isEditMode = intent.getBooleanExtra("edit_mode", false);

        if (isEditMode) {

            editingStudentId =
                    intent.getStringExtra("student_id");

            if (editingStudentId == null
                    || editingStudentId.trim().isEmpty()) {

                showError("Student ID is missing");
                btnAddStudent.setEnabled(false);
                return;
            }

            String studentName =
                    intent.getStringExtra("student_name");

            String studentRoll =
                    intent.getStringExtra("student_roll");

            String studentClass =
                    intent.getStringExtra("student_class");

            String studentSection =
                    intent.getStringExtra("student_section");

            String studentEmail =
                    intent.getStringExtra("student_email");

            String studentPhone =
                    intent.getStringExtra("student_phone");

            String studentPhoto =
                    intent.getStringExtra("student_photo");

            if (studentName != null) {
                etStudentName.setText(studentName);
            }

            if (studentRoll != null) {
                etRollNumber.setText(studentRoll);
            }

            if (studentEmail != null) {
                etStudentEmail.setText(studentEmail);
            }

            if (studentPhone != null) {
                etStudentPhone.setText(studentPhone);
            }

            setSpinnerValue(
                    spinnerStudentClass,
                    studentClass
            );

            setSpinnerValue(
                    spinnerStudentSection,
                    studentSection
            );

            // Display the existing photo in edit mode.
            loadExistingStudentPhoto(studentPhoto);

            btnAddStudent.setText("UPDATE STUDENT");
            btnCancelEdit.setVisibility(View.VISIBLE);

        } else {

            btnAddStudent.setText("ADD STUDENT");
            btnCancelEdit.setVisibility(View.GONE);
        }
    }

    private void setSpinnerValue(
            Spinner spinner,
            String value) {

        if (value == null || value.trim().isEmpty()) {
            return;
        }

        ArrayAdapter<?> adapter =
                (ArrayAdapter<?>) spinner.getAdapter();

        for (int i = 0; i < adapter.getCount(); i++) {

            Object item = adapter.getItem(i);

            if (item != null
                    && item.toString().equalsIgnoreCase(value)) {

                spinner.setSelection(i);
                return;
            }
        }
    }

    // ---------------------------------------------------------
    // LOAD EXISTING STUDENT PHOTO
    // ---------------------------------------------------------

    private void loadExistingStudentPhoto(String photo) {

        if (photo == null
                || photo.trim().isEmpty()
                || "null".equalsIgnoreCase(photo.trim())) {
            return;
        }

        String fileName = photo.trim();

        // Accept only a filename, not a path or URL.
        if (fileName.contains("/")
                || fileName.contains("\\")
                || fileName.contains("..")) {
            return;
        }

        String imageUrl =
                BASE_URL + "uploads/students/" + fileName;

        new Thread(() -> {

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
                // Keep the current ImageView if loading fails.
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

            if (bitmap != null) {

                Bitmap loadedBitmap = bitmap;

                runOnUiThread(() -> {

                    if (!isFinishing() && !isDestroyed()) {
                        ivStudentPhoto.setImageBitmap(loadedBitmap);
                    }
                });
            }

        }).start();
    }

    // ---------------------------------------------------------
    // VALIDATE FORM
    // ---------------------------------------------------------

    private boolean validateStudentForm() {

        String name =
                etStudentName.getText().toString().trim();

        String roll =
                etRollNumber.getText().toString().trim();

        String email =
                etStudentEmail.getText().toString().trim();

        String phone =
                etStudentPhone.getText().toString().trim();

        if (name.isEmpty()) {
            etStudentName.setError("Student name is required");
            etStudentName.requestFocus();
            return false;
        }

        if (roll.isEmpty()) {
            etRollNumber.setError("Roll number is required");
            etRollNumber.requestFocus();
            return false;
        }

        if (spinnerStudentClass.getSelectedItemPosition() == 0) {
            showError("Please select class");
            return false;
        }

        if (spinnerStudentSection.getSelectedItemPosition() == 0) {
            showError("Please select section");
            return false;
        }

        if (!email.isEmpty()
                && !android.util.Patterns.EMAIL_ADDRESS
                .matcher(email).matches()) {

            etStudentEmail.setError("Enter a valid email");
            etStudentEmail.requestFocus();
            return false;
        }

        if (!phone.isEmpty()
                && !phone.matches("[0-9+() -]{7,20}")) {

            etStudentPhone.setError("Enter a valid phone number");
            etStudentPhone.requestFocus();
            return false;
        }

        return true;
    }

    // ---------------------------------------------------------
    // ADD STUDENT
    // ---------------------------------------------------------

    private void addStudent() {

        if (requestInProgress || !validateStudentForm()) {
            return;
        }

        setLoading(true);

        StringRequest request = new StringRequest(
                Request.Method.POST,
                ADD_STUDENT_URL,

                response -> {

                    setLoading(false);

                    try {
                        JSONObject json = new JSONObject(response);

                        boolean success =
                                json.optBoolean("success", false);

                        String message = json.optString(
                                "message",
                                "Server response received"
                        );

                        if (!success) {
                            showError(message);
                            return;
                        }

                        String studentId =
                                json.optString("student_id", "");

                        if (studentId.trim().isEmpty()) {
                            studentId = json.optString("id", "");
                        }

                        if (selectedImageUri != null) {

                            if (studentId.trim().isEmpty()) {
                                showError(
                                        "Student added, but the server did not return a student ID. Photo was not uploaded."
                                );
                                return;
                            }

                            uploadStudentPhoto(
                                    studentId,
                                    selectedImageUri,
                                    false
                            );

                        } else {

                            Toast.makeText(
                                    StudentActivity.this,
                                    "Student added successfully",
                                    Toast.LENGTH_LONG
                            ).show();

                            finish();
                        }

                    } catch (JSONException e) {
                        showError("Invalid server response");
                    }
                },

                error -> handleVolleyError(error)
        ) {
            @Override
            protected Map<String, String> getParams() {

                Map<String, String> params = new HashMap<>();

                params.put(
                        "name",
                        etStudentName.getText().toString().trim()
                );

                params.put(
                        "roll_number",
                        etRollNumber.getText().toString().trim()
                );

                params.put(
                        "class_name",
                        spinnerStudentClass.getSelectedItem().toString()
                );

                params.put(
                        "section",
                        spinnerStudentSection.getSelectedItem().toString()
                );

                params.put(
                        "email",
                        etStudentEmail.getText().toString().trim()
                );

                params.put(
                        "phone",
                        etStudentPhone.getText().toString().trim()
                );

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
    // UPDATE STUDENT
    // ---------------------------------------------------------

    private void updateStudent() {

        if (requestInProgress) {
            return;
        }

        if (editingStudentId == null
                || editingStudentId.trim().isEmpty()) {

            showError("Student ID is missing");
            return;
        }

        if (!validateStudentForm()) {
            return;
        }

        setLoading(true);

        StringRequest request = new StringRequest(
                Request.Method.POST,
                UPDATE_STUDENT_URL,

                response -> {

                    setLoading(false);

                    try {
                        JSONObject json = new JSONObject(response);

                        boolean success =
                                json.optBoolean("success", false);

                        String message = json.optString(
                                "message",
                                "Unable to update student"
                        );

                        if (!success) {
                            showError(message);
                            return;
                        }

                        if (selectedImageUri != null) {

                            uploadStudentPhoto(
                                    editingStudentId,
                                    selectedImageUri,
                                    true
                            );

                        } else {

                            Toast.makeText(
                                    StudentActivity.this,
                                    "Student updated successfully",
                                    Toast.LENGTH_LONG
                            ).show();

                            finish();
                        }

                    } catch (JSONException e) {
                        showError("Invalid server response");
                    }
                },

                error -> handleVolleyError(error)
        ) {
            @Override
            protected Map<String, String> getParams() {

                Map<String, String> params = new HashMap<>();

                params.put("id", editingStudentId);

                params.put(
                        "name",
                        etStudentName.getText().toString().trim()
                );

                params.put(
                        "roll_number",
                        etRollNumber.getText().toString().trim()
                );

                params.put(
                        "class_name",
                        spinnerStudentClass.getSelectedItem().toString()
                );

                params.put(
                        "section",
                        spinnerStudentSection.getSelectedItem().toString()
                );

                params.put(
                        "email",
                        etStudentEmail.getText().toString().trim()
                );

                params.put(
                        "phone",
                        etStudentPhone.getText().toString().trim()
                );

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
    // AUTHENTICATION HEADERS
    // ---------------------------------------------------------

    private Map<String, String> createAuthHeaders() {

        Map<String, String> headers = new HashMap<>();

        headers.put("Authorization", "Bearer " + authToken);
        headers.put("Accept", "application/json");

        return headers;
    }

    // ---------------------------------------------------------
    // SELECT PHOTO
    // ---------------------------------------------------------

    private void selectPhoto() {

        Intent intent = new Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        );

        intent.setType("image/*");

        startActivityForResult(
                intent,
                PICK_IMAGE_REQUEST
        );
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            @Nullable Intent data) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (requestCode == PICK_IMAGE_REQUEST
                && resultCode == Activity.RESULT_OK
                && data != null
                && data.getData() != null) {

            selectedImageUri = data.getData();

            ivStudentPhoto.setImageURI(selectedImageUri);

            Toast.makeText(
                    this,
                    "Photo selected successfully",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // ---------------------------------------------------------
    // UPLOAD STUDENT PHOTO
    // ---------------------------------------------------------

    private void uploadStudentPhoto(
            String studentId,
            Uri imageUri,
            boolean editMode) {

        setLoading(true);

        new Thread(() -> {

            HttpURLConnection connection = null;

            String boundary =
                    "----StudentAttendanceBoundary"
                            + System.currentTimeMillis();

            try {
                URL url = new URL(UPLOAD_PHOTO_URL);

                connection =
                        (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                connection.setUseCaches(false);

                connection.setRequestProperty(
                        "Authorization",
                        "Bearer " + authToken
                );

                connection.setRequestProperty(
                        "Accept",
                        "application/json"
                );

                connection.setRequestProperty(
                        "Content-Type",
                        "multipart/form-data; boundary=" + boundary
                );

                try (OutputStream output =
                             connection.getOutputStream()) {

                    writeFormField(
                            output,
                            boundary,
                            "student_id",
                            studentId
                    );

                    writeFileField(
                            output,
                            boundary,
                            "photo",
                            imageUri
                    );

                    output.write(
                            ("--" + boundary + "--\r\n")
                                    .getBytes(StandardCharsets.UTF_8)
                    );

                    output.flush();
                }

                int responseCode = connection.getResponseCode();

                InputStream input =
                        (responseCode >= 200 && responseCode < 300)
                                ? connection.getInputStream()
                                : connection.getErrorStream();

                if (input == null) {
                    throw new IOException(
                            "Empty server response. HTTP " + responseCode
                    );
                }

                String response = readStream(input);

                runOnUiThread(() -> {

                    setLoading(false);

                    try {
                        JSONObject json = new JSONObject(response);

                        boolean success =
                                json.optBoolean("success", false);

                        String message = json.optString(
                                "message",
                                "Photo upload failed"
                        );

                        if (responseCode >= 200
                                && responseCode < 300
                                && success) {

                            Toast.makeText(
                                    StudentActivity.this,
                                    editMode
                                            ? "Student and photo updated successfully"
                                            : "Student and photo added successfully",
                                    Toast.LENGTH_LONG
                            ).show();

                            finish();

                        } else {
                            showError(message);
                        }

                    } catch (JSONException e) {
                        showError("Invalid photo upload response");
                    }
                });

            } catch (Exception e) {

                runOnUiThread(() -> {
                    setLoading(false);

                    showError(
                            "Photo upload failed: " + e.getMessage()
                    );
                });

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }

        }).start();
    }

    // ---------------------------------------------------------
    // WRITE MULTIPART FORM FIELD
    // ---------------------------------------------------------

    private void writeFormField(
            OutputStream outputStream,
            String boundary,
            String fieldName,
            String value) throws IOException {

        String data =
                "--" + boundary + "\r\n"
                        + "Content-Disposition: form-data; name=\""
                        + fieldName + "\"\r\n"
                        + "\r\n"
                        + value + "\r\n";

        outputStream.write(
                data.getBytes(StandardCharsets.UTF_8)
        );
    }

    // ---------------------------------------------------------
    // WRITE MULTIPART FILE FIELD
    // ---------------------------------------------------------

    private void writeFileField(
            OutputStream outputStream,
            String boundary,
            String fieldName,
            Uri imageUri) throws IOException {

        String mimeType =
                getContentResolver().getType(imageUri);

        if (mimeType == null
                || !(mimeType.equals("image/jpeg")
                || mimeType.equals("image/png")
                || mimeType.equals("image/webp"))) {

            throw new IOException(
                    "Unsupported image format. Select a JPG, PNG or WEBP image."
            );
        }

        String extension;

        switch (mimeType) {
            case "image/png":
                extension = "png";
                break;

            case "image/webp":
                extension = "webp";
                break;

            default:
                extension = "jpg";
                break;
        }

        String fileName = "student_photo." + extension;

        String header =
                "--" + boundary + "\r\n"
                        + "Content-Disposition: form-data; name=\""
                        + fieldName + "\"; filename=\""
                        + fileName + "\"\r\n"
                        + "Content-Type: " + mimeType + "\r\n"
                        + "\r\n";

        outputStream.write(
                header.getBytes(StandardCharsets.UTF_8)
        );

        try (InputStream inputStream =
                     getContentResolver().openInputStream(imageUri)) {

            if (inputStream == null) {
                throw new IOException(
                        "Unable to read selected image"
                );
            }

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }

        outputStream.write(
                "\r\n".getBytes(StandardCharsets.UTF_8)
        );
    }

    // ---------------------------------------------------------
    // READ HTTP RESPONSE
    // ---------------------------------------------------------

    private String readStream(InputStream inputStream)
            throws IOException {

        ByteArrayOutputStream outputStream =
                new ByteArrayOutputStream();

        try (InputStream input = inputStream) {

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = input.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }

        return outputStream.toString("UTF-8");
    }

    // ---------------------------------------------------------
    // LOADING INDICATOR
    // ---------------------------------------------------------

    private void setLoading(boolean loading) {

        requestInProgress = loading;

        progressBar.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );

        btnAddStudent.setEnabled(!loading);
        btnSelectPhoto.setEnabled(!loading);
        btnCancelEdit.setEnabled(!loading);
    }

    // ---------------------------------------------------------
    // VOLLEY ERROR HANDLING
    // ---------------------------------------------------------

    private void handleVolleyError(
            com.android.volley.VolleyError error) {

        setLoading(false);

        if (error.networkResponse != null) {

            int statusCode = error.networkResponse.statusCode;

            if (statusCode == 401) {
                showError("Session expired. Please log in again.");
                return;
            }

            if (statusCode == 403) {
                showError("Admin permission required.");
                return;
            }

            showError("Server error: HTTP " + statusCode);
            return;
        }

        showError("Network error. Check your server connection.");
    }

    // ---------------------------------------------------------
    // SHOW ERROR
    // ---------------------------------------------------------

    private void showError(String message) {

        tvMessage.setText(message);
        tvMessage.setTextColor(Color.rgb(211, 47, 47));

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    // ---------------------------------------------------------
    // SHOW SUCCESS
    // ---------------------------------------------------------

    private void showSuccess(String message) {

        tvMessage.setText(message);
        tvMessage.setTextColor(Color.rgb(46, 125, 50));

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }
}