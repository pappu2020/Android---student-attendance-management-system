package com.example.studentattendance;

import android.app.AlertDialog;
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

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
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
    // UI
    // =========================================================

    private Spinner spinnerStudentClass;
    private Spinner spinnerStudentSection;

    private TextView tvStudentListTitle;
    private TextView tvStudentListCount;

    private LinearLayout llStudentList;


    // =========================================================
    // Volley
    // =========================================================

    private RequestQueue requestQueue;


    // =========================================================
    // Student Data
    // =========================================================

    private final ArrayList<HashMap<String, String>> studentList =
            new ArrayList<>();


    // =========================================================
    // Selected Filter
    // =========================================================

    private String selectedClass = "";
    private String selectedSection = "";


    // =========================================================
    // onCreate
    // =========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_student_list);


        // =====================================================
        // Initialize Views
        // =====================================================

        spinnerStudentClass =
                findViewById(R.id.spinnerStudentClass);

        spinnerStudentSection =
                findViewById(R.id.spinnerStudentSection);

        tvStudentListTitle =
                findViewById(R.id.tvStudentListTitle);

        tvStudentListCount =
                findViewById(R.id.tvStudentListCount);

        llStudentList =
                findViewById(R.id.llStudentList);


        // =====================================================
        // Request Queue
        // =====================================================

        requestQueue =
                Volley.newRequestQueue(this);


        // =====================================================
        // Setup UI
        // =====================================================

        setupClassSpinner();

        setupSectionSpinner();


        // =====================================================
        // Initial State
        // =====================================================

        tvStudentListTitle.setText(
                "Students"
        );

        tvStudentListCount.setText(
                "0 Students"
        );


        showEmptyMessage(
                "Select class and section to view students"
        );
    }


    // =========================================================
    // CLASS SPINNER
    // =========================================================

    private void setupClassSpinner() {

        ArrayList<String> classList =
                new ArrayList<>();


        classList.add(
                "Select Class"
        );

        classList.add(
                "Class 6"
        );

        classList.add(
                "Class 7"
        );

        classList.add(
                "Class 8"
        );

        classList.add(
                "Class 9"
        );

        classList.add(
                "Class 10"
        );

        classList.add(
                "Class 11"
        );

        classList.add(
                "Class 12"
        );


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


        spinnerStudentClass.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {

                        if (position == 0) {

                            selectedClass = "";

                            selectedSection = "";

                            clearStudentList();

                            spinnerStudentSection.setSelection(
                                    0
                            );

                            showEmptyMessage(
                                    "Select class and section to view students"
                            );

                            return;
                        }


                        selectedClass =
                                parent
                                        .getItemAtPosition(
                                                position
                                        )
                                        .toString();


                        // Reset section

                        selectedSection = "";

                        spinnerStudentSection.setSelection(
                                0
                        );


                        clearStudentList();


                        showEmptyMessage(
                                "Now select a section"
                        );
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

        ArrayList<String> sectionList =
                new ArrayList<>();


        sectionList.add(
                "Select Section"
        );

        sectionList.add(
                "Section A"
        );

        sectionList.add(
                "Section B"
        );

        sectionList.add(
                "Section C"
        );

        sectionList.add(
                "Section D"
        );


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


        spinnerStudentSection.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {

                        if (position == 0) {

                            selectedSection = "";

                            clearStudentList();

                            showEmptyMessage(
                                    "Select section to view students"
                            );

                            return;
                        }


                        // Check class

                        if (selectedClass.isEmpty()) {

                            Toast.makeText(
                                    StudentListActivity.this,
                                    "Please select class first",
                                    Toast.LENGTH_SHORT
                            ).show();


                            spinnerStudentSection.setSelection(
                                    0
                            );

                            return;
                        }


                        selectedSection =
                                parent
                                        .getItemAtPosition(
                                                position
                                        )
                                        .toString();


                        loadStudents(
                                selectedClass,
                                selectedSection
                        );
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
    // LOAD STUDENTS
    // =========================================================

    private void loadStudents(
            String className,
            String section
    ) {

        clearStudentList();


        tvStudentListCount.setText(
                "Loading..."
        );


        showEmptyMessage(
                "Loading students..."
        );


        StringRequest request =
                new StringRequest(
                        Request.Method.POST,
                        GET_STUDENTS_URL,

                        response -> {

                            try {

                                JSONObject jsonObject =
                                        new JSONObject(
                                                response
                                        );


                                boolean success =
                                        jsonObject.optBoolean(
                                                "success",
                                                false
                                        );


                                if (!success) {

                                    tvStudentListCount.setText(
                                            "0 Students"
                                    );


                                    showEmptyMessage(
                                            jsonObject.optString(
                                                    "message",
                                                    "No students found"
                                            )
                                    );


                                    return;
                                }


                                JSONArray studentsArray =
                                        jsonObject.optJSONArray(
                                                "students"
                                        );


                                if (studentsArray == null
                                        || studentsArray.length() == 0) {

                                    tvStudentListCount.setText(
                                            "0 Students"
                                    );


                                    showEmptyMessage(
                                            "No students found in "
                                                    + className
                                                    + " - "
                                                    + section
                                    );


                                    return;
                                }


                                // Clear old data

                                studentList.clear();


                                // =========================================
                                // Read Students
                                // =========================================

                                for (int i = 0;
                                     i < studentsArray.length();
                                     i++) {

                                    JSONObject object =
                                            studentsArray
                                                    .getJSONObject(i);


                                    HashMap<String, String> student =
                                            new HashMap<>();


                                    student.put(
                                            "id",
                                            object.optString(
                                                    "id"
                                            )
                                    );


                                    student.put(
                                            "name",
                                            object.optString(
                                                    "name"
                                            )
                                    );


                                    student.put(
                                            "roll_number",
                                            object.optString(
                                                    "roll_number"
                                            )
                                    );


                                    student.put(
                                            "class_name",
                                            object.optString(
                                                    "class_name"
                                            )
                                    );


                                    student.put(
                                            "section",
                                            object.optString(
                                                    "section"
                                            )
                                    );


                                    student.put(
                                            "email",
                                            object.optString(
                                                    "email"
                                            )
                                    );


                                    student.put(
                                            "phone",
                                            object.optString(
                                                    "phone"
                                            )
                                    );


                                    student.put(
                                            "photo",
                                            object.optString(
                                                    "photo"
                                            )
                                    );


                                    studentList.add(
                                            student
                                    );
                                }


                                // =========================================
                                // Update Count
                                // =========================================

                                tvStudentListCount.setText(
                                        studentList.size()
                                                + " Students"
                                );


                                // =========================================
                                // Display
                                // =========================================

                                displayStudents();


                            } catch (JSONException e) {

                                tvStudentListCount.setText(
                                        "0 Students"
                                );


                                showEmptyMessage(
                                        "Invalid server response"
                                );


                                Toast.makeText(
                                        StudentListActivity.this,
                                        "JSON error: "
                                                + e.getMessage(),
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        },

                        error -> {

                            tvStudentListCount.setText(
                                    "0 Students"
                            );


                            showEmptyMessage(
                                    "Unable to connect to server"
                            );


                            Toast.makeText(
                                    StudentListActivity.this,
                                    "Failed to load students",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                ) {

                    @Override
                    protected Map<String, String> getParams() {

                        Map<String, String> params =
                                new HashMap<>();


                        params.put(
                                "class_name",
                                className
                        );


                        params.put(
                                "section",
                                section
                        );


                        return params;
                    }
                };


        requestQueue.add(
                request
        );
    }


    // =========================================================
    // DISPLAY STUDENTS
    // =========================================================

    private void displayStudents() {

        llStudentList.removeAllViews();


        if (studentList.isEmpty()) {

            showEmptyMessage(
                    "No students found"
            );

            return;
        }


        for (HashMap<String, String> student
                : studentList) {

            createStudentCard(
                    student
            );
        }
    }


    // =========================================================
    // CREATE STUDENT CARD
    // =========================================================

    private void createStudentCard(
            HashMap<String, String> student
    ) {

        // =====================================================
        // Main Card
        // =====================================================

        LinearLayout card =
                new LinearLayout(this);

        card.setOrientation(
                LinearLayout.VERTICAL
        );


        card.setPadding(
                20,
                20,
                20,
                18
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
                        230,
                        233,
                        238
                )
        );


        card.setBackground(
                cardBackground
        );


        card.setElevation(
                3
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
                14
        );


        card.setLayoutParams(
                cardParams
        );


        // =====================================================
        // Top Row
        // =====================================================

        LinearLayout topRow =
                new LinearLayout(this);

        topRow.setOrientation(
                LinearLayout.HORIZONTAL
        );


        topRow.setGravity(
                Gravity.CENTER_VERTICAL
        );


        // =====================================================
        // Avatar
        // =====================================================

        TextView avatar =
                new TextView(this);


        String name =
                student.get("name");


        String firstLetter =
                "?";


        if (name != null
                && !name.trim().isEmpty()) {

            firstLetter =
                    name.trim()
                            .substring(
                                    0,
                                    1
                            )
                            .toUpperCase(
                                    Locale.getDefault()
                            );
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
                null,
                Typeface.BOLD
        );


        avatar.setGravity(
                Gravity.CENTER
        );


        GradientDrawable avatarBackground =
                new GradientDrawable();


        avatarBackground.setColor(
                Color.rgb(
                        21,
                        101,
                        192
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
                        58,
                        58
                );


        avatarParams.setMargins(
                0,
                0,
                15,
                0
        );


        avatar.setLayoutParams(
                avatarParams
        );


        topRow.addView(
                avatar
        );


        // =====================================================
        // Name Container
        // =====================================================

        LinearLayout nameContainer =
                new LinearLayout(this);


        nameContainer.setOrientation(
                LinearLayout.VERTICAL
        );


        TextView nameText =
                new TextView(this);


        nameText.setText(
                name
        );


        nameText.setTextSize(
                18
        );


        nameText.setTextColor(
                Color.rgb(
                        30,
                        35,
                        45
                )
        );


        nameText.setTypeface(
                null,
                Typeface.BOLD
        );


        nameContainer.addView(
                nameText
        );


        TextView rollText =
                new TextView(this);


        rollText.setText(
                "Roll Number: "
                        + student.get(
                        "roll_number"
                )
        );


        rollText.setTextSize(
                13
        );


        rollText.setTextColor(
                Color.rgb(
                        110,
                        115,
                        125
                )
        );


        LinearLayout.LayoutParams rollParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );


        rollParams.setMargins(
                0,
                5,
                0,
                0
        );


        rollText.setLayoutParams(
                rollParams
        );


        nameContainer.addView(
                rollText
        );


        LinearLayout.LayoutParams nameParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                );


        nameContainer.setLayoutParams(
                nameParams
        );


        topRow.addView(
                nameContainer
        );


        card.addView(
                topRow
        );


        // =====================================================
        // Divider
        // =====================================================

        View divider =
                new View(this);


        divider.setBackgroundColor(
                Color.rgb(
                        235,
                        237,
                        240
                )
        );


        LinearLayout.LayoutParams dividerParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        1
                );


        dividerParams.setMargins(
                0,
                15,
                0,
                13
        );


        divider.setLayoutParams(
                dividerParams
        );


        card.addView(
                divider
        );


        // =====================================================
        // Class + Section
        // =====================================================

        TextView classText =
                new TextView(this);


        classText.setText(
                student.get(
                        "class_name"
                )
                        + "  •  "
                        + student.get(
                        "section"
                )
        );


        classText.setTextSize(
                14
        );


        classText.setTextColor(
                Color.rgb(
                        21,
                        101,
                        192
                )
        );


        classText.setTypeface(
                null,
                Typeface.BOLD
        );


        card.addView(
                classText
        );


        // =====================================================
        // Email
        // =====================================================

        String email =
                student.get("email");


        if (email != null
                && !email.trim().isEmpty()) {

            TextView emailText =
                    new TextView(this);


            emailText.setText(
                    "Email: " + email
            );


            emailText.setTextSize(
                    13
            );


            emailText.setTextColor(
                    Color.rgb(
                            100,
                            105,
                            115
                    )
            );


            LinearLayout.LayoutParams emailParams =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );


            emailParams.setMargins(
                    0,
                    7,
                    0,
                    0
            );


            emailText.setLayoutParams(
                    emailParams
            );


            card.addView(
                    emailText
            );
        }


        // =====================================================
        // Phone
        // =====================================================

        String phone =
                student.get("phone");


        if (phone != null
                && !phone.trim().isEmpty()) {

            TextView phoneText =
                    new TextView(this);


            phoneText.setText(
                    "Phone: " + phone
            );


            phoneText.setTextSize(
                    13
            );


            phoneText.setTextColor(
                    Color.rgb(
                            100,
                            105,
                            115
                    )
            );


            LinearLayout.LayoutParams phoneParams =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );


            phoneParams.setMargins(
                    0,
                    5,
                    0,
                    0
            );


            phoneText.setLayoutParams(
                    phoneParams
            );


            card.addView(
                    phoneText
            );
        }


        // =====================================================
        // Button Row
        // =====================================================

        LinearLayout buttonRow =
                new LinearLayout(this);


        buttonRow.setOrientation(
                LinearLayout.HORIZONTAL
        );


        buttonRow.setGravity(
                Gravity.CENTER_VERTICAL
        );


        LinearLayout.LayoutParams buttonRowParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );


        buttonRowParams.setMargins(
                0,
                16,
                0,
                0
        );


        buttonRow.setLayoutParams(
                buttonRowParams
        );


        // =====================================================
        // EDIT BUTTON
        // =====================================================

        Button editButton =
                new Button(this);


        editButton.setText(
                "Edit"
        );


        editButton.setTextSize(
                13
        );


        editButton.setTextColor(
                Color.rgb(
                        21,
                        101,
                        192
                )
        );


        editButton.setAllCaps(
                false
        );


        editButton.setBackgroundResource(
                R.drawable.bg_spinner
        );


        LinearLayout.LayoutParams editParams =
                new LinearLayout.LayoutParams(
                        0,
                        50,
                        1
                );


        editParams.setMargins(
                0,
                0,
                6,
                0
        );


        editButton.setLayoutParams(
                editParams
        );


        // =====================================================
        // Edit Action
        // =====================================================

        editButton.setOnClickListener(
                v -> {

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
                            student.get("id")
                    );


                    intent.putExtra(
                            "student_name",
                            student.get("name")
                    );


                    intent.putExtra(
                            "student_roll",
                            student.get("roll_number")
                    );


                    intent.putExtra(
                            "student_email",
                            student.get("email")
                    );


                    intent.putExtra(
                            "student_phone",
                            student.get("phone")
                    );


                    intent.putExtra(
                            "student_class",
                            student.get("class_name")
                    );


                    intent.putExtra(
                            "student_section",
                            student.get("section")
                    );


                    intent.putExtra(
                            "student_photo",
                            student.get("photo")
                    );


                    startActivity(
                            intent
                    );
                }
        );


        buttonRow.addView(
                editButton
        );


        // =====================================================
        // DELETE BUTTON
        // =====================================================

        Button deleteButton =
                new Button(this);


        deleteButton.setText(
                "Delete"
        );


        deleteButton.setTextSize(
                13
        );


        deleteButton.setTextColor(
                Color.rgb(
                        198,
                        40,
                        40
                )
        );


        deleteButton.setAllCaps(
                false
        );


        deleteButton.setBackgroundResource(
                R.drawable.bg_absent
        );


        LinearLayout.LayoutParams deleteParams =
                new LinearLayout.LayoutParams(
                        0,
                        50,
                        1
                );


        deleteParams.setMargins(
                6,
                0,
                0,
                0
        );


        deleteButton.setLayoutParams(
                deleteParams
        );


        // =====================================================
        // Delete Action
        // =====================================================

        deleteButton.setOnClickListener(
                v -> showDeleteConfirmation(
                        student
                )
        );


        buttonRow.addView(
                deleteButton
        );


        card.addView(
                buttonRow
        );


        // =====================================================
        // Add Card
        // =====================================================

        llStudentList.addView(
                card
        );
    }


    // =========================================================
    // DELETE CONFIRMATION
    // =========================================================

    private void showDeleteConfirmation(
            HashMap<String, String> student
    ) {

        String studentName =
                student.get("name");


        new AlertDialog.Builder(this)

                .setTitle(
                        "Delete Student?"
                )

                .setMessage(
                        "Are you sure you want to delete "
                                + studentName
                                + "?"
                )

                .setNegativeButton(
                        "Cancel",
                        null
                )

                .setPositiveButton(
                        "Delete",
                        (dialog, which) -> {

                            deleteStudent(
                                    student.get("id")
                            );
                        }
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

                                JSONObject jsonObject =
                                        new JSONObject(
                                                response
                                        );


                                boolean success =
                                        jsonObject.optBoolean(
                                                "success",
                                                false
                                        );


                                String message =
                                        jsonObject.optString(
                                                "message",
                                                "Operation completed"
                                        );


                                Toast.makeText(
                                        StudentListActivity.this,
                                        message,
                                        Toast.LENGTH_SHORT
                                ).show();


                                if (success) {

                                    // Reload current filter

                                    if (!selectedClass.isEmpty()
                                            && !selectedSection.isEmpty()) {

                                        loadStudents(
                                                selectedClass,
                                                selectedSection
                                        );

                                    } else {

                                        clearStudentList();

                                        showEmptyMessage(
                                                "Select class and section to view students"
                                        );
                                    }
                                }


                            } catch (JSONException e) {

                                Toast.makeText(
                                        StudentListActivity.this,
                                        "Invalid server response",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        },

                        error -> {

                            Toast.makeText(
                                    StudentListActivity.this,
                                    "Failed to delete student",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                ) {

                    @Override
                    protected Map<String, String> getParams() {

                        Map<String, String> params =
                                new HashMap<>();


                        params.put(
                                "id",
                                studentId
                        );


                        return params;
                    }
                };


        requestQueue.add(
                request
        );
    }


    // =========================================================
    // EMPTY STATE
    // =========================================================

    private void showEmptyMessage(
            String message
    ) {

        llStudentList.removeAllViews();


        TextView emptyText =
                new TextView(this);


        emptyText.setText(
                message
        );


        emptyText.setTextSize(
                15
        );


        emptyText.setTextColor(
                Color.rgb(
                        120,
                        125,
                        135
                )
        );


        emptyText.setGravity(
                Gravity.CENTER
        );


        emptyText.setPadding(
                20,
                60,
                20,
                60
        );


        llStudentList.addView(
                emptyText
        );
    }


    // =========================================================
    // CLEAR STUDENT LIST
    // =========================================================

    private void clearStudentList() {

        studentList.clear();


        llStudentList.removeAllViews();


        tvStudentListCount.setText(
                "0 Students"
        );
    }


    // =========================================================
    // Refresh when returning from StudentActivity
    // =========================================================

    @Override
    protected void onResume() {

        super.onResume();


        // If a class and section are already selected,
        // reload the list after Edit/Update.

        if (!selectedClass.isEmpty()
                && !selectedSection.isEmpty()) {

            loadStudents(
                    selectedClass,
                    selectedSection
            );
        }
    }
}