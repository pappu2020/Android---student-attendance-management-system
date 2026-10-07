package com.example.studentattendance;

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

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class StudentActivity extends AppCompatActivity {

    // =====================================================
    // API URLs
    // =====================================================

    private static final String ADD_STUDENT_URL =
            "http://10.0.2.2/student_attendance_api/add_student.php";

    private static final String UPDATE_STUDENT_URL =
            "http://10.0.2.2/student_attendance_api/update_student.php";

    private static final String UPLOAD_PHOTO_URL =
            "http://10.0.2.2/student_attendance_api/upload_student_photo.php";


    // =====================================================
    // Image Picker
    // =====================================================

    private static final int PICK_IMAGE_REQUEST = 1001;


    // =====================================================
    // UI Variables
    // =====================================================

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


    // =====================================================
    // Edit Variables
    // =====================================================

    private boolean isEditMode = false;

    private String editingStudentId = "";


    // =====================================================
    // Selected Photo
    // =====================================================

    private Uri selectedImageUri = null;


    // =====================================================
    // onCreate
    // =====================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_student);

        initializeViews();

        setupClassSpinner();

        setupSectionSpinner();

        setupButtons();

        checkEditMode();
    }


    // =====================================================
    // Initialize Views
    // =====================================================

    private void initializeViews() {

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


        btnCancelEdit.setVisibility(
                View.GONE
        );

        ivStudentPhoto.setVisibility(
                View.GONE
        );

        progressBar.setVisibility(
                View.GONE
        );
    }


    // =====================================================
    // CLASS SPINNER
    // =====================================================

    private void setupClassSpinner() {

        String[] classList = {

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
                        classList
                );


        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );


        spinnerStudentClass.setAdapter(
                adapter
        );
    }


    // =====================================================
    // SECTION SPINNER
    // =====================================================

    private void setupSectionSpinner() {

        String[] sectionList = {

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
                        sectionList
                );


        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );


        spinnerStudentSection.setAdapter(
                adapter
        );
    }


    // =====================================================
    // BUTTONS
    // =====================================================

    private void setupButtons() {

        btnAddStudent.setOnClickListener(
                view -> {

                    if (isEditMode) {

                        updateStudent();

                    } else {

                        addStudent();
                    }
                }
        );


        btnCancelEdit.setOnClickListener(
                view -> {

                    exitEditMode();

                }
        );


        btnSelectPhoto.setOnClickListener(
                view -> {

                    openImagePicker();

                }
        );
    }


    // =====================================================
    // CHECK EDIT MODE
    // =====================================================

    private void checkEditMode() {

        Intent intent = getIntent();

        boolean editMode =
                intent.getBooleanExtra(
                        "edit_mode",
                        false
                );


        if (!editMode) {

            isEditMode = false;

            editingStudentId = "";

            btnAddStudent.setText(
                    "ADD STUDENT"
            );

            btnCancelEdit.setVisibility(
                    View.GONE
            );

            return;
        }


        isEditMode = true;


        editingStudentId =
                getStringExtraSafe(
                        intent,
                        "student_id"
                );


        String studentName =
                getStringExtraSafe(
                        intent,
                        "student_name"
                );


        String studentRoll =
                getStringExtraSafe(
                        intent,
                        "student_roll"
                );


        String studentEmail =
                getStringExtraSafe(
                        intent,
                        "student_email"
                );


        String studentPhone =
                getStringExtraSafe(
                        intent,
                        "student_phone"
                );


        String studentClass =
                getStringExtraSafe(
                        intent,
                        "student_class"
                );


        String studentSection =
                getStringExtraSafe(
                        intent,
                        "student_section"
                );


        // Fill fields

        etStudentName.setText(
                studentName
        );


        etRollNumber.setText(
                studentRoll
        );


        etStudentEmail.setText(
                studentEmail
        );


        etStudentPhone.setText(
                studentPhone
        );


        // Select Class

        selectSpinnerValue(
                spinnerStudentClass,
                studentClass
        );


        // Select Section

        selectSpinnerValue(
                spinnerStudentSection,
                studentSection
        );


        btnAddStudent.setText(
                "UPDATE STUDENT"
        );


        btnCancelEdit.setVisibility(
                View.VISIBLE
        );


        tvMessage.setText(
                "Editing Student ID: "
                        + editingStudentId
        );


        tvMessage.setTextColor(
                Color.rgb(
                        33,
                        150,
                        243
                )
        );
    }


    // =====================================================
    // Safe Intent Extra
    // =====================================================

    private String getStringExtraSafe(
            Intent intent,
            String key
    ) {

        String value =
                intent.getStringExtra(
                        key
                );


        if (value == null) {

            return "";
        }


        return value;
    }


    // =====================================================
    // SELECT SPINNER VALUE
    // =====================================================

    private void selectSpinnerValue(
            Spinner spinner,
            String value
    ) {

        if (
                value == null ||
                        value.trim().isEmpty()
        ) {

            return;
        }


        ArrayAdapter<?> adapter =
                (ArrayAdapter<?>)
                        spinner.getAdapter();


        for (
                int i = 0;
                i < adapter.getCount();
                i++
        ) {

            Object item =
                    adapter.getItem(i);


            if (
                    item != null &&
                            item.toString()
                                    .equalsIgnoreCase(
                                            value
                                    )
            ) {

                spinner.setSelection(
                        i
                );

                break;
            }
        }
    }


    // =====================================================
    // OPEN IMAGE PICKER
    // =====================================================

    private void openImagePicker() {

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
                PICK_IMAGE_REQUEST
        );
    }


    // =====================================================
    // ADD STUDENT
    // =====================================================

    private void addStudent() {

        String name =
                etStudentName
                        .getText()
                        .toString()
                        .trim();


        String rollNumber =
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
                        .toString()
                        .trim();


        String section =
                spinnerStudentSection
                        .getSelectedItem()
                        .toString()
                        .trim();


        // =================================================
        // VALIDATION
        // =================================================

        if (name.isEmpty()) {

            etStudentName.setError(
                    "Enter student name"
            );

            etStudentName.requestFocus();

            return;
        }


        if (rollNumber.isEmpty()) {

            etRollNumber.setError(
                    "Enter roll number"
            );

            etRollNumber.requestFocus();

            return;
        }


        if (
                className.equals(
                        "Select Class"
                )
        ) {

            Toast.makeText(
                    this,
                    "Please select class",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        if (
                section.equals(
                        "Select Section"
                )
        ) {

            Toast.makeText(
                    this,
                    "Please select section",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        setLoading(true);


        new Thread(() -> {

            HttpURLConnection connection =
                    null;


            try {

                URL url =
                        new URL(
                                ADD_STUDENT_URL
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


                connection.setRequestProperty(
                        "Content-Type",
                        "application/x-www-form-urlencoded"
                );


                String postData =

                        "name=" +
                                URLEncoder.encode(
                                        name,
                                        "UTF-8"
                                )

                                + "&roll_number=" +
                                URLEncoder.encode(
                                        rollNumber,
                                        "UTF-8"
                                )

                                + "&class_name=" +
                                URLEncoder.encode(
                                        className,
                                        "UTF-8"
                                )

                                + "&section=" +
                                URLEncoder.encode(
                                        section,
                                        "UTF-8"
                                )

                                + "&email=" +
                                URLEncoder.encode(
                                        email,
                                        "UTF-8"
                                )

                                + "&phone=" +
                                URLEncoder.encode(
                                        phone,
                                        "UTF-8"
                                );


                OutputStream outputStream =
                        connection.getOutputStream();


                outputStream.write(
                        postData.getBytes(
                                StandardCharsets.UTF_8
                        )
                );


                outputStream.flush();

                outputStream.close();


                String response =
                        readResponse(
                                connection
                        );


                JSONObject json =
                        new JSONObject(
                                response
                        );


                boolean success =
                        json.optBoolean(
                                "success",
                                false
                        );


                String message =
                        json.optString(
                                "message",
                                "Unknown server response"
                        );


                if (success) {

                    String studentId =
                            json.optString(
                                    "student_id",
                                    ""
                            );


                    if (
                            selectedImageUri != null &&
                                    !studentId.isEmpty()
                    ) {

                        uploadStudentPhoto(
                                studentId,
                                selectedImageUri
                        );

                    } else {

                        runOnUiThread(() -> {

                            setLoading(
                                    false
                            );


                            Toast.makeText(
                                    StudentActivity.this,
                                    "Student added successfully",
                                    Toast.LENGTH_SHORT
                            ).show();


                            clearForm();

                        });
                    }

                } else {

                    runOnUiThread(() -> {

                        setLoading(
                                false
                        );


                        showError(
                                message
                        );
                    });
                }


            } catch (Exception e) {

                runOnUiThread(() -> {

                    setLoading(
                            false
                    );


                    showError(
                            "Add student error: "
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


    // =====================================================
    // UPDATE STUDENT
    // =====================================================

    private void updateStudent() {

        String name =
                etStudentName
                        .getText()
                        .toString()
                        .trim();


        String rollNumber =
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
                        .toString()
                        .trim();


        String section =
                spinnerStudentSection
                        .getSelectedItem()
                        .toString()
                        .trim();


        // =================================================
        // VALIDATION
        // =================================================

        if (name.isEmpty()) {

            etStudentName.setError(
                    "Enter student name"
            );

            etStudentName.requestFocus();

            return;
        }


        if (rollNumber.isEmpty()) {

            etRollNumber.setError(
                    "Enter roll number"
            );

            etRollNumber.requestFocus();

            return;
        }


        if (
                className.equals(
                        "Select Class"
                )
        ) {

            Toast.makeText(
                    this,
                    "Please select class",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        if (
                section.equals(
                        "Select Section"
                )
        ) {

            Toast.makeText(
                    this,
                    "Please select section",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        if (
                editingStudentId == null ||
                        editingStudentId.isEmpty()
        ) {

            Toast.makeText(
                    this,
                    "Student ID not found",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        setLoading(true);


        new Thread(() -> {

            HttpURLConnection connection =
                    null;


            try {

                URL url =
                        new URL(
                                UPDATE_STUDENT_URL
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


                connection.setRequestProperty(
                        "Content-Type",
                        "application/x-www-form-urlencoded"
                );


                String postData =

                        "id=" +
                                URLEncoder.encode(
                                        editingStudentId,
                                        "UTF-8"
                                )

                                + "&name=" +
                                URLEncoder.encode(
                                        name,
                                        "UTF-8"
                                )

                                + "&roll_number=" +
                                URLEncoder.encode(
                                        rollNumber,
                                        "UTF-8"
                                )

                                + "&class_name=" +
                                URLEncoder.encode(
                                        className,
                                        "UTF-8"
                                )

                                + "&section=" +
                                URLEncoder.encode(
                                        section,
                                        "UTF-8"
                                )

                                + "&email=" +
                                URLEncoder.encode(
                                        email,
                                        "UTF-8"
                                )

                                + "&phone=" +
                                URLEncoder.encode(
                                        phone,
                                        "UTF-8"
                                );


                OutputStream outputStream =
                        connection.getOutputStream();


                outputStream.write(
                        postData.getBytes(
                                StandardCharsets.UTF_8
                        )
                );


                outputStream.flush();

                outputStream.close();


                String response =
                        readResponse(
                                connection
                        );


                JSONObject json =
                        new JSONObject(
                                response
                        );


                boolean success =
                        json.optBoolean(
                                "success",
                                false
                        );


                String message =
                        json.optString(
                                "message",
                                "Unknown server response"
                        );


                runOnUiThread(() -> {

                    setLoading(
                            false
                    );


                    if (success) {

                        Toast.makeText(
                                StudentActivity.this,
                                "Student updated successfully",
                                Toast.LENGTH_SHORT
                        ).show();


                        exitEditMode();

                    } else {

                        showError(
                                message
                        );
                    }

                });


            } catch (Exception e) {

                runOnUiThread(() -> {

                    setLoading(
                            false
                    );


                    showError(
                            "Update error: "
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


    // =====================================================
    // UPLOAD STUDENT PHOTO
    // =====================================================

    private void uploadStudentPhoto(
            String studentId,
            Uri imageUri
    ) {

        new Thread(() -> {

            HttpURLConnection connection =
                    null;


            String boundary =
                    "----StudentAttendanceBoundary"
                            + System.currentTimeMillis();


            try {

                URL url =
                        new URL(
                                UPLOAD_PHOTO_URL
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


                connection.setUseCaches(
                        false
                );


                connection.setRequestProperty(
                        "Content-Type",
                        "multipart/form-data; boundary="
                                + boundary
                );


                DataOutputStream outputStream =
                        new DataOutputStream(
                                connection.getOutputStream()
                        );


                writeFormField(
                        outputStream,
                        boundary,
                        "student_id",
                        studentId
                );


                writeFileField(
                        outputStream,
                        boundary,
                        "photo",
                        imageUri
                );


                outputStream.writeBytes(
                        "--"
                                + boundary
                                + "--\r\n"
                );


                outputStream.flush();

                outputStream.close();


                String response =
                        readResponse(
                                connection
                        );


                JSONObject json =
                        new JSONObject(
                                response
                        );


                boolean success =
                        json.optBoolean(
                                "success",
                                false
                        );


                String message =
                        json.optString(
                                "message",
                                "Photo upload failed"
                        );


                runOnUiThread(() -> {

                    setLoading(
                            false
                    );


                    if (success) {

                        Toast.makeText(
                                StudentActivity.this,
                                "Student added successfully",
                                Toast.LENGTH_SHORT
                        ).show();


                        clearForm();

                    } else {

                        showError(
                                message
                        );
                    }
                });


            } catch (Exception e) {

                runOnUiThread(() -> {

                    setLoading(
                            false
                    );


                    showError(
                            "Photo upload error: "
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


    // =====================================================
    // WRITE MULTIPART FORM FIELD
    // =====================================================

    private void writeFormField(
            DataOutputStream outputStream,
            String boundary,
            String fieldName,
            String value
    ) throws Exception {

        outputStream.writeBytes(
                "--"
                        + boundary
                        + "\r\n"
        );


        outputStream.writeBytes(
                "Content-Disposition: form-data; name=\""
                        + fieldName
                        + "\"\r\n\r\n"
        );


        outputStream.writeBytes(
                value
                        + "\r\n"
        );
    }


    // =====================================================
    // WRITE MULTIPART FILE FIELD
    // =====================================================

    private void writeFileField(
            DataOutputStream outputStream,
            String boundary,
            String fieldName,
            Uri imageUri
    ) throws Exception {

        String fileName =
                "student_photo.jpg";


        outputStream.writeBytes(
                "--"
                        + boundary
                        + "\r\n"
        );


        outputStream.writeBytes(
                "Content-Disposition: form-data; name=\""
                        + fieldName
                        + "\"; filename=\""
                        + fileName
                        + "\"\r\n"
        );


        outputStream.writeBytes(
                "Content-Type: image/jpeg\r\n\r\n"
        );


        InputStream inputStream =
                getContentResolver()
                        .openInputStream(
                                imageUri
                        );


        if (inputStream == null) {

            throw new Exception(
                    "Unable to open selected image"
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


        outputStream.writeBytes(
                "\r\n"
        );
    }


    // =====================================================
    // EXIT EDIT MODE
    // =====================================================

    private void exitEditMode() {

        isEditMode = false;

        editingStudentId = "";


        btnAddStudent.setText(
                "ADD STUDENT"
        );


        btnCancelEdit.setVisibility(
                View.GONE
        );


        clearForm();


        tvMessage.setText("");
    }


    // =====================================================
    // CLEAR FORM
    // =====================================================

    private void clearForm() {

        etStudentName.setText("");

        etRollNumber.setText("");

        etStudentEmail.setText("");

        etStudentPhone.setText("");


        spinnerStudentClass.setSelection(
                0
        );


        spinnerStudentSection.setSelection(
                0
        );


        selectedImageUri = null;


        ivStudentPhoto.setImageDrawable(
                null
        );


        ivStudentPhoto.setVisibility(
                View.GONE
        );
    }


    // =====================================================
    // LOADING
    // =====================================================

    private void setLoading(
            boolean loading
    ) {

        runOnUiThread(() -> {

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
        });
    }


    // =====================================================
    // SHOW ERROR
    // =====================================================

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
    }


    // =====================================================
    // READ HTTP RESPONSE
    // =====================================================

    private String readResponse(
            HttpURLConnection connection
    ) throws Exception {

        InputStream inputStream;


        int responseCode =
                connection.getResponseCode();


        if (
                responseCode >= 200 &&
                        responseCode < 300
        ) {

            inputStream =
                    connection.getInputStream();

        } else {

            inputStream =
                    connection.getErrorStream();


            if (inputStream == null) {

                throw new Exception(
                        "HTTP Error: "
                                + responseCode
                );
            }
        }


        BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(
                                inputStream,
                                StandardCharsets.UTF_8
                        )
                );


        StringBuilder response =
                new StringBuilder();


        String line;


        while (
                (line = reader.readLine())
                        != null
        ) {

            response.append(line);
        }


        reader.close();


        return response.toString();
    }


    // =====================================================
    // IMAGE PICKER RESULT
    // =====================================================

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );


        if (
                requestCode == PICK_IMAGE_REQUEST &&
                        resultCode == RESULT_OK &&
                        data != null
        ) {

            selectedImageUri =
                    data.getData();


            if (selectedImageUri != null) {

                ivStudentPhoto.setImageURI(
                        selectedImageUri
                );


                ivStudentPhoto.setVisibility(
                        View.VISIBLE
                );


                tvMessage.setText(
                        "Photo selected successfully"
                );


                tvMessage.setTextColor(
                        Color.rgb(
                                46,
                                125,
                                50
                        )
                );
            }
        }
    }
}