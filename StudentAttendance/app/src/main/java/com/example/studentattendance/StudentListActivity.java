package com.example.studentattendance;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class StudentListActivity extends AppCompatActivity {

    // =========================================================
    // API URLs
    // =========================================================

    private static final String GET_STUDENTS_URL =
            "http://10.0.2.2/student_attendance_api/get_students_by_class_section.php";

    private static final String DELETE_STUDENT_URL =
            "http://10.0.2.2/student_attendance_api/delete_student.php";


    // =========================================================
    // UI COMPONENTS
    // =========================================================

    private Spinner spinnerStudentClass;
    private Spinner spinnerStudentSection;

    private TextView tvStudentListTitle;
    private TextView tvStudentListCount;
    private TextView tvMessage;

    private LinearLayout llStudentList;


    // =========================================================
    // VARIABLES
    // =========================================================

    private String selectedClass = "";
    private String selectedSection = "";


    // =========================================================
    // ON CREATE
    // =========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_student_list
        );


        // -----------------------------------------------------
        // Initialize Views
        // -----------------------------------------------------

        initializeViews();


        // -----------------------------------------------------
        // Setup Spinners
        // -----------------------------------------------------

        setupClassSpinner();

        setupSectionSpinner();


        // -----------------------------------------------------
        // Spinner Listeners
        // -----------------------------------------------------

        setupSpinnerListeners();

    }


    // =========================================================
    // INITIALIZE VIEWS
    // =========================================================

    private void initializeViews() {

        spinnerStudentClass =
                findViewById(
                        R.id.spinnerStudentClass
                );


        spinnerStudentSection =
                findViewById(
                        R.id.spinnerStudentSection
                );


        tvStudentListTitle =
                findViewById(
                        R.id.tvStudentListTitle
                );


        tvStudentListCount =
                findViewById(
                        R.id.tvStudentListCount
                );


        tvMessage =
                findViewById(
                        R.id.tvMessage
                );


        llStudentList =
                findViewById(
                        R.id.llStudentList
                );


        // -----------------------------------------------------
        // Initial Text
        // -----------------------------------------------------

        if (tvStudentListTitle != null) {

            tvStudentListTitle.setText(
                    "Student List"
            );

        }


        if (tvStudentListCount != null) {

            tvStudentListCount.setText(
                    "Select class and section"
            );

        }


        if (tvMessage != null) {

            tvMessage.setText(
                    ""
            );

        }

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


        spinnerStudentClass.setAdapter(
                adapter
        );

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


        spinnerStudentSection.setAdapter(
                adapter
        );

    }


    // =========================================================
    // SPINNER LISTENERS
    // =========================================================

    private void setupSpinnerListeners() {

        spinnerStudentClass.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {

                        selectedClass =
                                parent
                                        .getItemAtPosition(position)
                                        .toString();


                        tryLoadStudents();

                    }


                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent
                    ) {

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
                            long id
                    ) {

                        selectedSection =
                                parent
                                        .getItemAtPosition(position)
                                        .toString();


                        tryLoadStudents();

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
    // TRY LOAD STUDENTS
    // =========================================================

    private void tryLoadStudents() {

        if (selectedClass == null ||
                selectedClass.equals("Select Class")) {

            return;

        }


        if (selectedSection == null ||
                selectedSection.equals("Select Section")) {

            return;

        }


        loadStudents();

    }


    // =========================================================
    // LOAD STUDENTS
    // =========================================================

    private void loadStudents() {

        if (llStudentList == null) {

            return;

        }


        showLoadingMessage();


        StringRequest request =
                new StringRequest(
                        Request.Method.POST,
                        GET_STUDENTS_URL,

                        response -> {

                            hideLoadingMessage();


                            try {

                                JSONObject json =
                                        new JSONObject(
                                                response
                                        );


                                boolean success =
                                        json.getBoolean(
                                                "success"
                                        );


                                if (!success) {

                                    String message =
                                            json.optString(
                                                    "message",
                                                    "Unable to load students"
                                            );


                                    showError(
                                            message
                                    );

                                    return;

                                }


                                JSONArray students =
                                        json.optJSONArray(
                                                "students"
                                        );


                                if (students == null) {

                                    showEmptyMessage();

                                    return;

                                }


                                displayStudents(
                                        students
                                );


                            } catch (Exception e) {

                                showError(
                                        "Invalid server response"
                                );

                            }

                        },

                        error -> {

                            hideLoadingMessage();


                            String message =
                                    error.getMessage();


                            if (message == null ||
                                    message.trim().isEmpty()) {

                                message =
                                        "Unable to connect to server";

                            }


                            showError(
                                    "Network error: "
                                            + message
                            );

                        }

                ) {

                    @Override
                    protected Map<String, String>
                    getParams() {

                        Map<String, String> params =
                                new HashMap<>();


                        params.put(
                                "class_name",
                                selectedClass
                        );


                        params.put(
                                "section",
                                selectedSection
                        );


                        return params;

                    }

                };


        RequestQueue queue =
                Volley.newRequestQueue(
                        StudentListActivity.this
                );


        queue.add(request);

    }


    // =========================================================
    // DISPLAY STUDENTS
    // =========================================================

    private void displayStudents(
            JSONArray students
    ) {

        llStudentList.removeAllViews();


        int totalStudents =
                students.length();


        if (tvStudentListCount != null) {

            tvStudentListCount.setText(
                    "Total Students: "
                            + totalStudents
            );

        }


        if (totalStudents == 0) {

            showEmptyMessage();

            return;

        }


        if (tvMessage != null) {

            tvMessage.setText(
                    ""
            );

        }


        for (
                int i = 0;
                i < students.length();
                i++
        ) {

            try {

                JSONObject student =
                        students.getJSONObject(i);


                String id =
                        student.optString(
                                "id",
                                ""
                        );


                String name =
                        student.optString(
                                "name",
                                "Unknown Student"
                        );


                String rollNumber =
                        student.optString(
                                "roll_number",
                                ""
                        );


                String className =
                        student.optString(
                                "class_name",
                                selectedClass
                        );


                String section =
                        student.optString(
                                "section",
                                selectedSection
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


                String photo =
                        student.optString(
                                "photo",
                                ""
                        );


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


            } catch (Exception e) {

                // Skip invalid student record

            }

        }

    }


    // =========================================================
    // CREATE STUDENT CARD
    // =========================================================

    private void createStudentCard(
            String id,
            String name,
            String rollNumber,
            String className,
            String section,
            String email,
            String phone,
            String photo
    ) {

        // =====================================================
        // MAIN CARD
        // =====================================================

        LinearLayout cardLayout =
                new LinearLayout(
                        this
                );


        cardLayout.setOrientation(
                LinearLayout.VERTICAL
        );


        cardLayout.setPadding(
                24,
                24,
                24,
                24
        );


        GradientDrawable cardBackground =
                new GradientDrawable();


        cardBackground.setColor(
                Color.WHITE
        );


        cardBackground.setCornerRadius(
                24
        );


        cardBackground.setStroke(
                1,
                Color.rgb(
                        225,
                        225,
                        225
                )
        );


        cardLayout.setBackground(
                cardBackground
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
                20
        );


        cardLayout.setLayoutParams(
                cardParams
        );


        // =====================================================
        // TOP ROW
        // =====================================================

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


        // =====================================================
        // AVATAR
        // =====================================================

        TextView avatar =
                new TextView(
                        this
                );


        String firstLetter =
                "S";


        if (name != null &&
                !name.trim().isEmpty()) {

            firstLetter =
                    name.trim()
                            .substring(
                                    0,
                                    1
                            )
                            .toUpperCase();

        }


        avatar.setText(
                firstLetter
        );


        avatar.setTextSize(
                20
        );


        avatar.setTextColor(
                Color.WHITE
        );


        avatar.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );


        avatar.setGravity(
                Gravity.CENTER
        );


        GradientDrawable avatarBackground =
                new GradientDrawable();


        avatarBackground.setColor(
                Color.rgb(
                        33,
                        150,
                        243
                )
        );


        avatarBackground.setShape(
                GradientDrawable.OVAL
        );


        avatar.setBackground(
                avatarBackground
        );


        LinearLayout.LayoutParams avatarParams =
                new LinearLayout.LayoutParams(
                        60,
                        60
                );


        avatarParams.setMargins(
                0,
                0,
                20,
                0
        );


        topRow.addView(
                avatar,
                avatarParams
        );


        // =====================================================
        // NAME + ROLL
        // =====================================================

        LinearLayout nameLayout =
                new LinearLayout(
                        this
                );


        nameLayout.setOrientation(
                LinearLayout.VERTICAL
        );


        TextView tvName =
                new TextView(
                        this
                );


        tvName.setText(
                name
        );


        tvName.setTextSize(
                19
        );


        tvName.setTextColor(
                Color.rgb(
                        30,
                        30,
                        30
                )
        );


        tvName.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );


        TextView tvRoll =
                new TextView(
                        this
                );


        tvRoll.setText(
                "Roll No: "
                        + rollNumber
        );


        tvRoll.setTextSize(
                14
        );


        tvRoll.setTextColor(
                Color.rgb(
                        100,
                        100,
                        100
                )
        );


        nameLayout.addView(
                tvName
        );


        nameLayout.addView(
                tvRoll
        );


        topRow.addView(
                nameLayout,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                )
        );


        cardLayout.addView(
                topRow
        );


        // =====================================================
        // CLASS / SECTION
        // =====================================================

        TextView tvClass =
                createInfoText(
                        "Class: "
                                + className
                                + "    |    Section: "
                                + section
                );


        cardLayout.addView(
                tvClass
        );


        // =====================================================
        // EMAIL
        // =====================================================

        if (email != null &&
                !email.trim().isEmpty()) {

            TextView tvEmail =
                    createInfoText(
                            "Email: "
                                    + email
                    );


            cardLayout.addView(
                    tvEmail
            );

        }


        // =====================================================
        // PHONE
        // =====================================================

        if (phone != null &&
                !phone.trim().isEmpty()) {

            TextView tvPhone =
                    createInfoText(
                            "Phone: "
                                    + phone
                    );


            cardLayout.addView(
                    tvPhone
            );

        }


        // =====================================================
        // STUDENT ID
        // =====================================================

        TextView tvId =
                createInfoText(
                        "Student ID: "
                                + id
                );


        cardLayout.addView(
                tvId
        );


        // =====================================================
        // BUTTON LAYOUT
        // =====================================================

        LinearLayout buttonLayout =
                new LinearLayout(
                        this
                );


        buttonLayout.setOrientation(
                LinearLayout.HORIZONTAL
        );


        buttonLayout.setGravity(
                Gravity.CENTER
        );


        buttonLayout.setPadding(
                0,
                20,
                0,
                0
        );


        // =====================================================
        // EDIT BUTTON
        // =====================================================

        Button btnEdit =
                createActionButton(
                        "EDIT",
                        Color.rgb(
                                33,
                                150,
                                243
                        )
                );


        LinearLayout.LayoutParams editParams =
                new LinearLayout.LayoutParams(
                        0,
                        55,
                        1
                );


        editParams.setMargins(
                0,
                0,
                8,
                0
        );


        buttonLayout.addView(
                btnEdit,
                editParams
        );


        // =====================================================
        // DELETE BUTTON
        // =====================================================

        Button btnDelete =
                createActionButton(
                        "DELETE",
                        Color.rgb(
                                211,
                                47,
                                47
                        )
                );


        LinearLayout.LayoutParams deleteParams =
                new LinearLayout.LayoutParams(
                        0,
                        55,
                        1
                );


        deleteParams.setMargins(
                8,
                0,
                0,
                0
        );


        buttonLayout.addView(
                btnDelete,
                deleteParams
        );


        cardLayout.addView(
                buttonLayout
        );


        // =====================================================
        // EDIT CLICK
        // =====================================================

        btnEdit.setOnClickListener(
                v -> {

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

                }
        );


        // =====================================================
        // DELETE CLICK
        // =====================================================

        btnDelete.setOnClickListener(
                v -> {

                    confirmDeleteStudent(
                            id,
                            name
                    );

                }
        );


        // =====================================================
        // ADD CARD
        // =====================================================

        llStudentList.addView(
                cardLayout
        );

    }


    // =========================================================
    // CREATE INFO TEXT
    // =========================================================

    private TextView createInfoText(
            String text
    ) {

        TextView textView =
                new TextView(
                        this
                );


        textView.setText(
                text
        );


        textView.setTextSize(
                14
        );


        textView.setTextColor(
                Color.rgb(
                        80,
                        80,
                        80
                )
        );


        textView.setPadding(
                0,
                12,
                0,
                0
        );


        return textView;

    }


    // =========================================================
    // CREATE ACTION BUTTON
    // =========================================================

    private Button createActionButton(
            String text,
            int backgroundColor
    ) {

        Button button =
                new Button(
                        this
                );


        button.setText(
                text
        );


        button.setTextSize(
                14
        );


        button.setTextColor(
                Color.WHITE
        );


        button.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );


        button.setGravity(
                Gravity.CENTER
        );


        button.setAllCaps(
                false
        );


        GradientDrawable background =
                new GradientDrawable();


        background.setColor(
                backgroundColor
        );


        background.setCornerRadius(
                16
        );


        button.setBackground(
                background
        );


        button.setMinHeight(
                55
        );


        button.setMinimumHeight(
                55
        );


        return button;

    }


    // =========================================================
    // OPEN EDIT STUDENT
    // =========================================================

    private void openEditStudent(
            String id,
            String name,
            String rollNumber,
            String className,
            String section,
            String email,
            String phone,
            String photo
    ) {

        Intent intent =
                new Intent(
                        StudentListActivity.this,
                        StudentActivity.class
                );


        intent.putExtra(
                "edit_mode",
                true
        );


        intent.putExtra(
                "student_id",
                id
        );


        intent.putExtra(
                "student_name",
                name
        );


        intent.putExtra(
                "student_roll",
                rollNumber
        );


        intent.putExtra(
                "student_class",
                className
        );


        intent.putExtra(
                "student_section",
                section
        );


        intent.putExtra(
                "student_email",
                email
        );


        intent.putExtra(
                "student_phone",
                phone
        );


        intent.putExtra(
                "student_photo",
                photo
        );


        startActivity(
                intent
        );

    }


    // =========================================================
    // CONFIRM DELETE
    // =========================================================

    private void confirmDeleteStudent(
            String studentId,
            String studentName
    ) {

        new AlertDialog.Builder(
                StudentListActivity.this
        )

                .setTitle(
                        "Delete Student"
                )

                .setMessage(
                        "Are you sure you want to delete "
                                + studentName
                                + "?"
                )

                .setPositiveButton(
                        "DELETE",
                        (dialog, which) -> {

                            deleteStudent(
                                    studentId
                            );

                        }
                )

                .setNegativeButton(
                        "CANCEL",
                        null
                )

                .show();

    }


    // =========================================================
    // DELETE STUDENT
    // =========================================================

    private void deleteStudent(
            String studentId
    ) {

        StringRequest request =
                new StringRequest(
                        Request.Method.POST,
                        DELETE_STUDENT_URL,

                        response -> {

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
                                        json.optString(
                                                "message",
                                                ""
                                        );


                                if (success) {

                                    Toast.makeText(
                                            StudentListActivity.this,
                                            "Student deleted successfully",
                                            Toast.LENGTH_SHORT
                                    ).show();


                                    loadStudents();

                                } else {

                                    showError(
                                            message
                                    );

                                }

                            } catch (Exception e) {

                                showError(
                                        "Invalid server response"
                                );

                            }

                        },

                        error -> {

                            String message =
                                    error.getMessage();


                            if (message == null ||
                                    message.trim().isEmpty()) {

                                message =
                                        "Unable to connect to server";

                            }


                            showError(
                                    "Delete failed: "
                                            + message
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
                                studentId
                        );


                        return params;

                    }

                };


        RequestQueue queue =
                Volley.newRequestQueue(
                        StudentListActivity.this
                );


        queue.add(request);

    }


    // =========================================================
    // LOADING MESSAGE
    // =========================================================

    private void showLoadingMessage() {

        if (tvMessage != null) {

            tvMessage.setText(
                    "Loading students..."
            );


            tvMessage.setTextColor(
                    Color.rgb(
                            33,
                            150,
                            243
                    )
            );

        }

    }


    // =========================================================
    // HIDE LOADING MESSAGE
    // =========================================================

    private void hideLoadingMessage() {

        if (tvMessage != null) {

            tvMessage.setText(
                    ""
            );

        }

    }


    // =========================================================
    // EMPTY MESSAGE
    // =========================================================

    private void showEmptyMessage() {

        if (llStudentList != null) {

            llStudentList.removeAllViews();

        }


        if (tvStudentListCount != null) {

            tvStudentListCount.setText(
                    "Total Students: 0"
            );

        }


        if (tvMessage != null) {

            tvMessage.setText(
                    "No students found for "
                            + selectedClass
                            + " - "
                            + selectedSection
            );


            tvMessage.setTextColor(
                    Color.rgb(
                            117,
                            117,
                            117
                    )
            );

        }

    }


    // =========================================================
    // ERROR MESSAGE
    // =========================================================

    private void showError(
            String message
    ) {

        if (tvMessage != null) {

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


        Toast.makeText(
                StudentListActivity.this,
                message,
                Toast.LENGTH_LONG
        ).show();

    }


    // =========================================================
    // ON RESUME
    // =========================================================

    @Override
    protected void onResume() {

        super.onResume();


        if (selectedClass != null &&
                !selectedClass.equals(
                        "Select Class"
                ) &&
                selectedSection != null &&
                !selectedSection.equals(
                        "Select Section"
                )) {

            loadStudents();

        }

    }

}