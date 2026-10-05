package com.example.studentattendance;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
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

    private static final String GET_STUDENTS_URL =
            "http://10.0.2.2/student_attendance_api/get_students.php";

    private static final String UPDATE_STUDENT_URL =
            "http://10.0.2.2/student_attendance_api/update_student.php";

    private static final String DELETE_STUDENT_URL =
            "http://10.0.2.2/student_attendance_api/delete_student.php";

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

    private Button btnAddStudent;
    private Button btnCancelEdit;
    private Button btnSelectPhoto;

    private ProgressBar progressBar;

    private TextView tvMessage;

    private LinearLayout llStudentList;

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

        setupButtons();

        loadStudents();
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


        llStudentList =
                findViewById(R.id.llStudentList);

        ivStudentPhoto =
                findViewById(R.id.ivStudentPhoto);


        // Initially hide cancel button
        btnCancelEdit.setVisibility(
                View.GONE
        );


        // Initially hide photo
        ivStudentPhoto.setVisibility(
                View.GONE
        );
    }


    // =====================================================
    // Setup Buttons
    // =====================================================

    private void setupButtons() {

        // ADD / UPDATE
        btnAddStudent.setOnClickListener(
                view -> {

                    if (isEditMode) {

                        updateStudent();

                    } else {

                        addStudent();
                    }
                }
        );


        // CANCEL EDIT
        btnCancelEdit.setOnClickListener(
                view -> {

                    exitEditMode();

                }
        );


        // SELECT PHOTO
        btnSelectPhoto.setOnClickListener(
                view -> {

                    openImagePicker();

                }
        );
    }


    // =====================================================
    // Open Image Picker
    // =====================================================

    private void openImagePicker() {

        Intent intent =
                new Intent(
                        Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                );

        intent.setType("image/*");

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


        // Validation
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


        setLoading(true);


        new Thread(() -> {

            HttpURLConnection connection = null;

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

                connection.setDoOutput(true);


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
                        readResponse(connection);


                JSONObject json =
                        new JSONObject(response);


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


                    // Photo selected?
                    if (selectedImageUri != null) {

                        uploadStudentPhoto(
                                studentId,
                                selectedImageUri
                        );

                    } else {

                        runOnUiThread(() -> {

                            setLoading(false);

                            Toast.makeText(
                                    StudentActivity.this,
                                    message,
                                    Toast.LENGTH_SHORT
                            ).show();

                            clearForm();

                            loadStudents();

                        });
                    }


                } else {

                    runOnUiThread(() -> {

                        setLoading(false);

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
                    });
                }


            } catch (Exception e) {

                runOnUiThread(() -> {

                    setLoading(false);

                    tvMessage.setText(
                            "Add student error: "
                                    + e.getMessage()
                    );

                    tvMessage.setTextColor(
                            Color.rgb(
                                    211,
                                    47,
                                    47
                            )
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


        // Validation
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


        if (editingStudentId.isEmpty()) {

            Toast.makeText(
                    this,
                    "Student ID not found",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        setLoading(true);


        new Thread(() -> {

            HttpURLConnection connection = null;

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

                connection.setDoOutput(true);


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
                        readResponse(connection);


                JSONObject json =
                        new JSONObject(response);


                boolean success =
                        json.getBoolean(
                                "success"
                        );


                String message =
                        json.getString(
                                "message"
                        );


                runOnUiThread(() -> {

                    setLoading(false);


                    if (success) {

                        Toast.makeText(
                                StudentActivity.this,
                                message,
                                Toast.LENGTH_SHORT
                        ).show();


                        exitEditMode();

                        loadStudents();


                    } else {

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

                });


            } catch (Exception e) {

                runOnUiThread(() -> {

                    setLoading(false);

                    tvMessage.setText(
                            "Update error: "
                                    + e.getMessage()
                    );

                    tvMessage.setTextColor(
                            Color.rgb(
                                    211,
                                    47,
                                    47
                            )
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
    // DELETE STUDENT - CONFIRMATION
    // =====================================================

    private void deleteStudent(
            String studentId
    ) {

        new AlertDialog.Builder(this)

                .setTitle(
                        "Delete Student"
                )

                .setMessage(
                        "Are you sure you want to delete this student?"
                )

                .setPositiveButton(
                        "YES",
                        (dialog, which) -> {

                            performDeleteStudent(
                                    studentId
                            );

                        }
                )

                .setNegativeButton(
                        "NO",
                        null
                )

                .show();
    }


    // =====================================================
    // PERFORM DELETE
    // =====================================================

    private void performDeleteStudent(
            String studentId
    ) {

        setLoading(true);


        new Thread(() -> {

            HttpURLConnection connection = null;

            try {

                URL url =
                        new URL(
                                DELETE_STUDENT_URL
                        );


                connection =
                        (HttpURLConnection)
                                url.openConnection();


                connection.setRequestMethod(
                        "POST"
                );

                connection.setDoOutput(true);


                connection.setRequestProperty(
                        "Content-Type",
                        "application/x-www-form-urlencoded"
                );


                String postData =
                        "id=" +
                                URLEncoder.encode(
                                        studentId,
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
                        readResponse(connection);


                JSONObject json =
                        new JSONObject(response);


                boolean success =
                        json.getBoolean(
                                "success"
                        );


                String message =
                        json.getString(
                                "message"
                        );


                runOnUiThread(() -> {

                    setLoading(false);


                    if (success) {

                        Toast.makeText(
                                StudentActivity.this,
                                message,
                                Toast.LENGTH_SHORT
                        ).show();


                        loadStudents();


                    } else {

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

                });


            } catch (Exception e) {

                runOnUiThread(() -> {

                    setLoading(false);


                    tvMessage.setText(
                            "Delete error: "
                                    + e.getMessage()
                    );


                    tvMessage.setTextColor(
                            Color.rgb(
                                    211,
                                    47,
                                    47
                            )
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

            HttpURLConnection connection = null;


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

                connection.setDoOutput(true);

                connection.setUseCaches(false);


                connection.setRequestProperty(
                        "Content-Type",
                        "multipart/form-data; boundary="
                                + boundary
                );


                DataOutputStream outputStream =
                        new DataOutputStream(
                                connection.getOutputStream()
                        );


                // Student ID
                writeFormField(
                        outputStream,
                        boundary,
                        "student_id",
                        studentId
                );


                // Photo
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
                        readResponse(connection);


                JSONObject json =
                        new JSONObject(response);


                boolean success =
                        json.getBoolean(
                                "success"
                        );


                String message =
                        json.getString(
                                "message"
                        );


                runOnUiThread(() -> {

                    setLoading(false);


                    if (success) {

                        Toast.makeText(
                                StudentActivity.this,
                                "Student added successfully",
                                Toast.LENGTH_SHORT
                        ).show();


                        clearForm();

                        loadStudents();


                    } else {

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

                });


            } catch (Exception e) {

                runOnUiThread(() -> {

                    setLoading(false);


                    tvMessage.setText(
                            "Photo upload error: "
                                    + e.getMessage()
                    );


                    tvMessage.setTextColor(
                            Color.rgb(
                                    211,
                                    47,
                                    47
                            )
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
    // Multipart Form Field
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
    // Multipart File Field
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
    // GET STUDENTS
    // =====================================================

    private void loadStudents() {

        setLoading(true);


        new Thread(() -> {

            HttpURLConnection connection = null;


            try {

                URL url =
                        new URL(
                                GET_STUDENTS_URL
                        );


                connection =
                        (HttpURLConnection)
                                url.openConnection();


                connection.setRequestMethod(
                        "GET"
                );


                String response =
                        readResponse(connection);


                JSONObject json =
                        new JSONObject(
                                response
                        );


                boolean success =
                        json.getBoolean(
                                "success"
                        );


                if (success) {

                    JSONArray students =
                            json.getJSONArray(
                                    "students"
                            );


                    runOnUiThread(() -> {

                        setLoading(false);

                        displayStudents(
                                students
                        );
                    });


                } else {

                    String message =
                            json.getString(
                                    "message"
                            );


                    runOnUiThread(() -> {

                        setLoading(false);

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
                    });
                }


            } catch (Exception e) {

                runOnUiThread(() -> {

                    setLoading(false);


                    tvMessage.setText(
                            "Load students error: "
                                    + e.getMessage()
                    );


                    tvMessage.setTextColor(
                            Color.rgb(
                                    211,
                                    47,
                                    47
                            )
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
    // DISPLAY STUDENTS
    // =====================================================

    private void displayStudents(
            JSONArray students
    ) {

        llStudentList.removeAllViews();


        if (students.length() == 0) {

            TextView emptyText =
                    new TextView(this);


            emptyText.setText(
                    "No students found"
            );


            emptyText.setTextSize(
                    16
            );


            emptyText.setTextColor(
                    Color.DKGRAY
            );


            emptyText.setPadding(
                    20,
                    20,
                    20,
                    20
            );


            llStudentList.addView(
                    emptyText
            );


            return;
        }


        try {

            for (
                    int i = 0;
                    i < students.length();
                    i++
            ) {

                JSONObject student =
                        students.getJSONObject(
                                i
                        );


                String id =
                        student.getString(
                                "id"
                        );


                String name =
                        student.getString(
                                "name"
                        );


                String rollNumber =
                        student.getString(
                                "roll_number"
                        );


                String email =
                        student.optString(
                                "email",
                                ""
                        );


                String phone =
                        student.optString(
                                "phone",
                                ""
                        );


                // =================================================
                // Student Card
                // =================================================

                LinearLayout cardLayout =
                        new LinearLayout(this);


                cardLayout.setOrientation(
                        LinearLayout.VERTICAL
                );


                cardLayout.setPadding(
                        30,
                        25,
                        30,
                        25
                );


                // =================================================
                // Student Name
                // =================================================

                TextView tvName =
                        new TextView(this);


                tvName.setText(
                        "Name: "
                                + name
                );


                tvName.setTextSize(
                        18
                );


                tvName.setTextColor(
                        Color.BLACK
                );


                // =================================================
                // Roll Number
                // =================================================

                TextView tvRoll =
                        new TextView(this);


                tvRoll.setText(
                        "Roll: "
                                + rollNumber
                );


                tvRoll.setTextSize(
                        16
                );


                // =================================================
                // Email
                // =================================================

                TextView tvEmail =
                        new TextView(this);


                tvEmail.setText(
                        "Email: "
                                + (
                                email.isEmpty()
                                        ? "N/A"
                                        : email
                        )
                );


                tvEmail.setTextSize(
                        15
                );


                // =================================================
                // Phone
                // =================================================

                TextView tvPhone =
                        new TextView(this);


                tvPhone.setText(
                        "Phone: "
                                + (
                                phone.isEmpty()
                                        ? "N/A"
                                        : phone
                        )
                );


                tvPhone.setTextSize(
                        15
                );


                // =================================================
                // Student ID
                // =================================================

                TextView tvId =
                        new TextView(this);


                tvId.setText(
                        "Student ID: "
                                + id
                );


                tvId.setTextSize(
                        14
                );


                tvId.setTextColor(
                        Color.GRAY
                );


                // =================================================
                // Button Layout
                // =================================================

                LinearLayout buttonLayout =
                        new LinearLayout(this);


                buttonLayout.setOrientation(
                        LinearLayout.HORIZONTAL
                );


                // =================================================
                // EDIT BUTTON
                // =================================================

                Button btnEdit =
                        new Button(this);


                btnEdit.setText(
                        "EDIT"
                );


                LinearLayout.LayoutParams
                        editParams =
                        new LinearLayout.LayoutParams(
                                0,
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                1
                        );


                btnEdit.setLayoutParams(
                        editParams
                );


                btnEdit.setOnClickListener(
                        view -> {

                            editStudent(
                                    id,
                                    name,
                                    rollNumber,
                                    email,
                                    phone
                            );

                        }
                );


                buttonLayout.addView(
                        btnEdit
                );


                // =================================================
                // DELETE BUTTON
                // =================================================

                Button btnDelete =
                        new Button(this);


                btnDelete.setText(
                        "DELETE"
                );


                LinearLayout.LayoutParams
                        deleteParams =
                        new LinearLayout.LayoutParams(
                                0,
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                1
                        );


                btnDelete.setLayoutParams(
                        deleteParams
                );


                btnDelete.setOnClickListener(
                        view -> {

                            deleteStudent(
                                    id
                            );

                        }
                );


                buttonLayout.addView(
                        btnDelete
                );


                // =================================================
                // Add All Views to Card
                // =================================================

                cardLayout.addView(
                        tvName
                );


                cardLayout.addView(
                        tvRoll
                );


                cardLayout.addView(
                        tvEmail
                );


                cardLayout.addView(
                        tvPhone
                );


                cardLayout.addView(
                        tvId
                );


                cardLayout.addView(
                        buttonLayout
                );


                // =================================================
                // Add Card to List
                // =================================================

                llStudentList.addView(
                        cardLayout
                );
            }


        } catch (Exception e) {

            tvMessage.setText(
                    "Display error: "
                            + e.getMessage()
            );


            tvMessage.setTextColor(
                    Color.rgb(
                            211,
                            47,
                            47
                    )
            );
        }
    }


    // =====================================================
    // EDIT STUDENT
    // =====================================================

    private void editStudent(
            String id,
            String name,
            String rollNumber,
            String email,
            String phone
    ) {

        isEditMode = true;

        editingStudentId = id;


        // Fill form
        etStudentName.setText(
                name
        );


        etRollNumber.setText(
                rollNumber
        );


        etStudentEmail.setText(
                email
        );


        etStudentPhone.setText(
                phone
        );


        // Change button
        btnAddStudent.setText(
                "UPDATE STUDENT"
        );


        // Show cancel
        btnCancelEdit.setVisibility(
                View.VISIBLE
        );


        // Message
        tvMessage.setText(
                "Editing Student ID: "
                        + id
        );


        tvMessage.setTextColor(
                Color.rgb(
                        33,
                        150,
                        243
                )
        );


        etStudentName.requestFocus();
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
            }
        });
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
                responseCode >= 200
                        && responseCode < 300
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
                requestCode == PICK_IMAGE_REQUEST
                        && resultCode == RESULT_OK
                        && data != null
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