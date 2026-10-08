package com.example.studentattendance;

import android.app.Activity;
import android.content.Intent;
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
import java.util.HashMap;
import java.util.Map;

public class StudentActivity extends AppCompatActivity {

    // =========================================================
    // API URLs
    // =========================================================

    private static final String ADD_STUDENT_URL =
            "http://10.0.2.2/student_attendance_api/add_student.php";

    private static final String UPDATE_STUDENT_URL =
            "http://10.0.2.2/student_attendance_api/update_student.php";

    private static final String UPLOAD_PHOTO_URL =
            "http://10.0.2.2/student_attendance_api/upload_student_photo.php";


    // =========================================================
    // UI COMPONENTS
    // =========================================================

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


    // =========================================================
    // EDIT MODE VARIABLES
    // =========================================================

    private boolean isEditMode = false;

    private String editingStudentId = "";

    private Uri selectedImageUri = null;


    // =========================================================
    // ON CREATE
    // =========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_student);


        // -----------------------------------------------------
        // Initialize Views
        // -----------------------------------------------------

        etStudentName =
                findViewById(R.id.etStudentName);

        etRollNumber =
                findViewById(R.id.etRollNumber);

        etStudentEmail =
                findViewById(R.id.etStudentEmail);

        etStudentPhone =
                findViewById(R.id.etStudentPhone);

        spinnerStudentClass =
                findViewById(R.id.spinnerStudentClass);

        spinnerStudentSection =
                findViewById(R.id.spinnerStudentSection);

        btnAddStudent =
                findViewById(R.id.btnAddStudent);

        btnCancelEdit =
                findViewById(R.id.btnCancelEdit);

        btnSelectPhoto =
                findViewById(R.id.btnSelectPhoto);

        progressBar =
                findViewById(R.id.progressBar);

        tvMessage =
                findViewById(R.id.tvMessage);

        ivStudentPhoto =
                findViewById(R.id.ivStudentPhoto);


        // -----------------------------------------------------
        // Setup Spinners
        // -----------------------------------------------------

        setupClassSpinner();

        setupSectionSpinner();


        // -----------------------------------------------------
        // Check Add/Edit Mode
        // -----------------------------------------------------

        checkEditMode();


        // -----------------------------------------------------
        // Add / Update Button
        // -----------------------------------------------------

        btnAddStudent.setOnClickListener(v -> {

            if (isEditMode) {

                updateStudent();

            } else {

                addStudent();

            }

        });


        // -----------------------------------------------------
        // Cancel Button
        // -----------------------------------------------------

        btnCancelEdit.setOnClickListener(v -> {

            finish();

        });


        // -----------------------------------------------------
        // Select Photo Button
        // -----------------------------------------------------

        btnSelectPhoto.setOnClickListener(v -> {

            selectPhoto();

        });

    }


    // =========================================================
    // CLASS SPINNER
    // =========================================================

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


        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        classes
                );


        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );


        spinnerStudentClass.setAdapter(adapter);

    }


    // =========================================================
    // SECTION SPINNER
    // =========================================================

    private void setupSectionSpinner() {

        String[] sections = {

                "Select Section",

                "Section A",
                "Section B",
                "Section C",
                "Section D"

        };


        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        sections
                );


        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );


        spinnerStudentSection.setAdapter(adapter);

    }


    // =========================================================
    // CHECK EDIT MODE
    // =========================================================

    private void checkEditMode() {

        Intent intent = getIntent();


        isEditMode =
                intent.getBooleanExtra(
                        "edit_mode",
                        false
                );


        // =====================================================
        // EDIT MODE
        // =====================================================

        if (isEditMode) {

            editingStudentId =
                    intent.getStringExtra(
                            "student_id"
                    );


            String studentName =
                    intent.getStringExtra(
                            "student_name"
                    );


            String studentRoll =
                    intent.getStringExtra(
                            "student_roll"
                    );


            String studentClass =
                    intent.getStringExtra(
                            "student_class"
                    );


            String studentSection =
                    intent.getStringExtra(
                            "student_section"
                    );


            String studentEmail =
                    intent.getStringExtra(
                            "student_email"
                    );


            String studentPhone =
                    intent.getStringExtra(
                            "student_phone"
                    );


            // -------------------------------------------------
            // Fill Existing Data
            // -------------------------------------------------

            if (studentName != null) {

                etStudentName.setText(
                        studentName
                );

            }


            if (studentRoll != null) {

                etRollNumber.setText(
                        studentRoll
                );

            }


            if (studentEmail != null) {

                etStudentEmail.setText(
                        studentEmail
                );

            }


            if (studentPhone != null) {

                etStudentPhone.setText(
                        studentPhone
                );

            }


            // -------------------------------------------------
            // Set Class
            // -------------------------------------------------

            setSpinnerValue(
                    spinnerStudentClass,
                    studentClass
            );


            // -------------------------------------------------
            // Set Section
            // -------------------------------------------------

            setSpinnerValue(
                    spinnerStudentSection,
                    studentSection
            );


            // -------------------------------------------------
            // Update Button Text
            // -------------------------------------------------

            btnAddStudent.setText(
                    "UPDATE STUDENT"
            );


            btnCancelEdit.setVisibility(
                    View.VISIBLE
            );


        }

        // =====================================================
        // ADD MODE
        // =====================================================

        else {

            btnAddStudent.setText(
                    "ADD STUDENT"
            );


            btnCancelEdit.setVisibility(
                    View.GONE
            );

        }

    }


    // =========================================================
    // SET SPINNER VALUE
    // =========================================================

    private void setSpinnerValue(
            Spinner spinner,
            String value
    ) {

        if (value == null ||
                value.trim().isEmpty()) {

            return;

        }


        ArrayAdapter<?> adapter =
                (ArrayAdapter<?>) spinner.getAdapter();


        for (int i = 0;
             i < adapter.getCount();
             i++) {

            Object item =
                    adapter.getItem(i);


            if (item != null &&
                    item.toString()
                            .equalsIgnoreCase(
                                    value
                            )) {

                spinner.setSelection(i);

                break;

            }

        }

    }


    // =========================================================
    // ADD STUDENT
    // =========================================================

    private void addStudent() {

        String name =
                etStudentName
                        .getText()
                        .toString()
                        .trim();


        String roll =
                etRollNumber
                        .getText()
                        .toString()
                        .trim();


        String email =
                etStudentEmail
                        .getText()
                        .toString()
                        .trim();


        String phone =
                etStudentPhone
                        .getText()
                        .toString()
                        .trim();


        String className =
                spinnerStudentClass
                        .getSelectedItem()
                        .toString();


        String section =
                spinnerStudentSection
                        .getSelectedItem()
                        .toString();


        // -----------------------------------------------------
        // Validation
        // -----------------------------------------------------

        if (name.isEmpty()) {

            showError(
                    "Please enter student name"
            );

            return;

        }


        if (roll.isEmpty()) {

            showError(
                    "Please enter roll number"
            );

            return;

        }


        if (className.equals(
                "Select Class"
        )) {

            showError(
                    "Please select class"
            );

            return;

        }


        if (section.equals(
                "Select Section"
        )) {

            showError(
                    "Please select section"
            );

            return;

        }


        setLoading(true);


        // -----------------------------------------------------
        // Volley Request
        // -----------------------------------------------------

        StringRequest request =
                new StringRequest(
                        Request.Method.POST,
                        ADD_STUDENT_URL,

                        response -> {

                            setLoading(false);


                            try {

                                JSONObject json =
                                        new JSONObject(
                                                response
                                        );


                                boolean success =
                                        json.getBoolean(
                                                "success"
                                        );


                                String message =
                                        json.getString(
                                                "message"
                                        );


                                if (success) {

                                    String studentId =
                                            json.getString(
                                                    "student_id"
                                            );


                                    // ---------------------------------
                                    // Upload Photo if Selected
                                    // ---------------------------------

                                    if (selectedImageUri != null) {

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

                                } else {

                                    showError(
                                            message
                                    );

                                }

                            } catch (JSONException e) {

                                showError(
                                        "Invalid server response"
                                );

                            }

                        },

                        error -> {

                            setLoading(false);


                            String errorMessage =
                                    error.getMessage();


                            if (errorMessage == null) {

                                errorMessage =
                                        "Unable to connect to server";

                            }


                            showError(
                                    "Network error: "
                                            + errorMessage
                            );

                        }

                ) {

                    @Override
                    protected Map<String, String>
                    getParams() {

                        Map<String, String> params =
                                new HashMap<>();


                        params.put(
                                "name",
                                name
                        );


                        params.put(
                                "roll_number",
                                roll
                        );


                        params.put(
                                "class_name",
                                className
                        );


                        params.put(
                                "section",
                                section
                        );


                        params.put(
                                "email",
                                email
                        );


                        params.put(
                                "phone",
                                phone
                        );


                        return params;

                    }

                };


        RequestQueue queue =
                Volley.newRequestQueue(
                        StudentActivity.this
                );


        queue.add(request);

    }


    // =========================================================
    // UPDATE STUDENT
    // =========================================================

    private void updateStudent() {

        String name =
                etStudentName
                        .getText()
                        .toString()
                        .trim();


        String roll =
                etRollNumber
                        .getText()
                        .toString()
                        .trim();


        String email =
                etStudentEmail
                        .getText()
                        .toString()
                        .trim();


        String phone =
                etStudentPhone
                        .getText()
                        .toString()
                        .trim();


        String className =
                spinnerStudentClass
                        .getSelectedItem()
                        .toString();


        String section =
                spinnerStudentSection
                        .getSelectedItem()
                        .toString();


        // -----------------------------------------------------
        // Validation
        // -----------------------------------------------------

        if (editingStudentId == null ||
                editingStudentId.isEmpty()) {

            showError(
                    "Student ID is missing"
            );

            return;

        }


        if (name.isEmpty()) {

            showError(
                    "Please enter student name"
            );

            return;

        }


        if (roll.isEmpty()) {

            showError(
                    "Please enter roll number"
            );

            return;

        }


        if (className.equals(
                "Select Class"
        )) {

            showError(
                    "Please select class"
            );

            return;

        }


        if (section.equals(
                "Select Section"
        )) {

            showError(
                    "Please select section"
            );

            return;

        }


        setLoading(true);


        // -----------------------------------------------------
        // Volley Request
        // -----------------------------------------------------

        StringRequest request =
                new StringRequest(
                        Request.Method.POST,
                        UPDATE_STUDENT_URL,

                        response -> {

                            setLoading(false);


                            try {

                                JSONObject json =
                                        new JSONObject(
                                                response
                                        );


                                boolean success =
                                        json.getBoolean(
                                                "success"
                                        );


                                String message =
                                        json.getString(
                                                "message"
                                        );


                                if (success) {

                                    // ---------------------------------
                                    // Upload New Photo if Selected
                                    // ---------------------------------

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

                                } else {

                                    showError(
                                            message
                                    );

                                }

                            } catch (JSONException e) {

                                showError(
                                        "Invalid server response"
                                );

                            }

                        },

                        error -> {

                            setLoading(false);


                            String errorMessage =
                                    error.getMessage();


                            if (errorMessage == null) {

                                errorMessage =
                                        "Unable to connect to server";

                            }


                            showError(
                                    "Network error: "
                                            + errorMessage
                            );

                        }

                ) {

                    @Override
                    protected Map<String, String>
                    getParams() {

                        Map<String, String> params =
                                new HashMap<>();


                        params.put(
                                "id",
                                editingStudentId
                        );


                        params.put(
                                "name",
                                name
                        );


                        params.put(
                                "roll_number",
                                roll
                        );


                        params.put(
                                "class_name",
                                className
                        );


                        params.put(
                                "section",
                                section
                        );


                        params.put(
                                "email",
                                email
                        );


                        params.put(
                                "phone",
                                phone
                        );


                        return params;

                    }

                };


        RequestQueue queue =
                Volley.newRequestQueue(
                        StudentActivity.this
                );


        queue.add(request);

    }


    // =========================================================
    // SELECT PHOTO
    // =========================================================

    private void selectPhoto() {

        Intent intent =
                new Intent(
                        Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                );


        intent.setType(
                "image/*"
        );


        startActivityForResult(
                intent,
                1001
        );

    }


    // =========================================================
    // PHOTO RESULT
    // =========================================================

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            @Nullable Intent data
    ) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );


        if (requestCode == 1001 &&
                resultCode == Activity.RESULT_OK &&
                data != null &&
                data.getData() != null) {


            selectedImageUri =
                    data.getData();


            // -------------------------------------------------
            // Show Selected Image
            // -------------------------------------------------

            ivStudentPhoto.setImageURI(
                    selectedImageUri
            );


            Toast.makeText(
                    StudentActivity.this,
                    "Photo selected successfully",
                    Toast.LENGTH_SHORT
            ).show();

        }

    }


    // =========================================================
    // UPLOAD STUDENT PHOTO
    // =========================================================

    private void uploadStudentPhoto(
            String studentId,
            Uri imageUri,
            boolean editMode
    ) {

        new Thread(() -> {

            HttpURLConnection connection =
                    null;


            String boundary =
                    "----StudentAttendanceBoundary"
                            + System.currentTimeMillis();


            try {

                // -------------------------------------------------
                // URL
                // -------------------------------------------------

                URL url =
                        new URL(
                                UPLOAD_PHOTO_URL
                        );


                connection =
                        (HttpURLConnection)
                                url.openConnection();


                // -------------------------------------------------
                // Connection Settings
                // -------------------------------------------------

                connection.setRequestMethod(
                        "POST"
                );


                connection.setDoOutput(
                        true
                );


                connection.setDoInput(
                        true
                );


                connection.setUseCaches(
                        false
                );


                connection.setRequestProperty(
                        "Content-Type",
                        "multipart/form-data; boundary="
                                + boundary
                );


                // -------------------------------------------------
                // Output Stream
                // -------------------------------------------------

                OutputStream outputStream =
                        connection.getOutputStream();


                // -------------------------------------------------
                // Student ID
                // -------------------------------------------------

                writeFormField(
                        outputStream,
                        boundary,
                        "student_id",
                        studentId
                );


                // -------------------------------------------------
                // Photo
                // -------------------------------------------------

                writeFileField(
                        outputStream,
                        boundary,
                        "photo",
                        imageUri
                );


                // -------------------------------------------------
                // End Multipart Request
                // -------------------------------------------------

                outputStream.write(
                        (
                                "--"
                                        + boundary
                                        + "--\r\n"
                        ).getBytes()
                );


                outputStream.flush();

                outputStream.close();


                // -------------------------------------------------
                // Server Response
                // -------------------------------------------------

                int responseCode =
                        connection.getResponseCode();


                InputStream inputStream;


                if (responseCode >= 200 &&
                        responseCode < 400) {

                    inputStream =
                            connection.getInputStream();

                } else {

                    inputStream =
                            connection.getErrorStream();

                }


                if (inputStream == null) {

                    throw new IOException(
                            "Empty server response"
                    );

                }


                String response =
                        readStream(
                                inputStream
                        );


                // -------------------------------------------------
                // UI Thread
                // -------------------------------------------------

                runOnUiThread(() -> {

                    try {

                        JSONObject json =
                                new JSONObject(
                                        response
                                );


                        boolean success =
                                json.getBoolean(
                                        "success"
                                );


                        String message =
                                json.getString(
                                        "message"
                                );


                        if (success) {

                            if (editMode) {

                                Toast.makeText(
                                        StudentActivity.this,
                                        "Student and photo updated successfully",
                                        Toast.LENGTH_LONG
                                ).show();

                            } else {

                                Toast.makeText(
                                        StudentActivity.this,
                                        "Student and photo added successfully",
                                        Toast.LENGTH_LONG
                                ).show();

                            }


                            finish();

                        } else {

                            showError(
                                    message
                            );

                        }

                    } catch (JSONException e) {

                        showError(
                                "Invalid photo upload response"
                        );

                    }

                });


            } catch (Exception e) {

                runOnUiThread(() -> {

                    showError(
                            "Photo upload failed: "
                                    + e.getMessage()
                    );

                });

            } finally {

                if (connection != null) {

                    connection.disconnect();

                }

            }

        }).start();

    }


    // =========================================================
    // WRITE FORM FIELD
    // =========================================================

    private void writeFormField(
            OutputStream outputStream,
            String boundary,
            String fieldName,
            String value
    ) throws IOException {

        String data =
                "--"
                        + boundary
                        + "\r\n"
                        + "Content-Disposition: form-data; name=\""
                        + fieldName
                        + "\"\r\n"
                        + "\r\n"
                        + value
                        + "\r\n";


        outputStream.write(
                data.getBytes()
        );

    }


    // =========================================================
    // WRITE FILE FIELD
    // =========================================================

    private void writeFileField(
            OutputStream outputStream,
            String boundary,
            String fieldName,
            Uri imageUri
    ) throws IOException {

        String fileName =
                "student_photo.jpg";


        String header =
                "--"
                        + boundary
                        + "\r\n"
                        + "Content-Disposition: form-data; name=\""
                        + fieldName
                        + "\"; filename=\""
                        + fileName
                        + "\"\r\n"
                        + "Content-Type: image/jpeg\r\n"
                        + "\r\n";


        outputStream.write(
                header.getBytes()
        );


        InputStream inputStream =
                getContentResolver()
                        .openInputStream(
                                imageUri
                        );


        if (inputStream == null) {

            throw new IOException(
                    "Unable to read selected image"
            );

        }


        byte[] buffer =
                new byte[4096];


        int bytesRead;


        while (
                (bytesRead =
                        inputStream.read(buffer))
                        != -1
        ) {

            outputStream.write(
                    buffer,
                    0,
                    bytesRead
            );

        }


        inputStream.close();


        outputStream.write(
                "\r\n".getBytes()
        );

    }


    // =========================================================
    // READ STREAM
    // =========================================================

    private String readStream(
            InputStream inputStream
    ) throws IOException {

        ByteArrayOutputStream outputStream =
                new ByteArrayOutputStream();


        byte[] buffer =
                new byte[4096];


        int bytesRead;


        while (
                (bytesRead =
                        inputStream.read(buffer))
                        != -1
        ) {

            outputStream.write(
                    buffer,
                    0,
                    bytesRead
            );

        }


        inputStream.close();


        return outputStream.toString(
                "UTF-8"
        );

    }


    // =========================================================
    // LOADING
    // =========================================================

    private void setLoading(
            boolean loading
    ) {

        if (loading) {

            progressBar.setVisibility(
                    View.VISIBLE
            );


            btnAddStudent.setEnabled(
                    false
            );


            btnSelectPhoto.setEnabled(
                    false
            );


            btnCancelEdit.setEnabled(
                    false
            );

        } else {

            progressBar.setVisibility(
                    View.GONE
            );


            btnAddStudent.setEnabled(
                    true
            );


            btnSelectPhoto.setEnabled(
                    true
            );


            btnCancelEdit.setEnabled(
                    true
            );

        }

    }


    // =========================================================
    // SHOW ERROR
    // =========================================================

    private void showError(
            String message
    ) {

        tvMessage.setText(
                message
        );


        tvMessage.setTextColor(
                Color.rgb(
                        211,
                        47,
                        47
                )
        );


        Toast.makeText(
                StudentActivity.this,
                message,
                Toast.LENGTH_LONG
        ).show();

    }


    // =========================================================
    // SHOW SUCCESS
    // =========================================================

    private void showSuccess(
            String message
    ) {

        tvMessage.setText(
                message
        );


        tvMessage.setTextColor(
                Color.rgb(
                        46,
                        125,
                        50
                )
        );


        Toast.makeText(
                StudentActivity.this,
                message,
                Toast.LENGTH_LONG
        ).show();

    }

}