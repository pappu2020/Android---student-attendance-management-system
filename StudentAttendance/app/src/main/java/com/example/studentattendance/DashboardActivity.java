package com.example.studentattendance;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class DashboardActivity extends AppCompatActivity {

    // =====================================================
    // Header TextViews
    // =====================================================

    private TextView tvDashboardTitle;
    private TextView tvWelcome;
    private TextView tvRole;


    // =====================================================
    // Dashboard Cards
    // =====================================================

    private LinearLayout btnStudents;
    private LinearLayout btnStudentList;
    private LinearLayout btnAttendance;
    private LinearLayout btnHistory;
    private LinearLayout btnReport;


    // =====================================================
    // Logout Button
    // =====================================================

    private Button btnLogout;


    // =====================================================
    // ON CREATE
    // =====================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // Connect XML layout
        setContentView(
                R.layout.activity_dashboard
        );


        // =================================================
        // Find Views
        // =================================================

        tvDashboardTitle =
                findViewById(
                        R.id.tvDashboardTitle
                );


        tvWelcome =
                findViewById(
                        R.id.tvWelcome
                );


        tvRole =
                findViewById(
                        R.id.tvRole
                );


        btnStudents =
                findViewById(
                        R.id.btnStudents
                );


        btnStudentList =
                findViewById(
                        R.id.btnStudentList
                );


        btnAttendance =
                findViewById(
                        R.id.btnAttendance
                );


        btnHistory =
                findViewById(
                        R.id.btnHistory
                );


        btnReport =
                findViewById(
                        R.id.btnReport
                );


        btnLogout =
                findViewById(
                        R.id.btnLogout
                );


        // =================================================
        // Get User Information from Login
        // =================================================

        String userName =
                getIntent().getStringExtra(
                        "user_name"
                );


        String userRole =
                getIntent().getStringExtra(
                        "user_role"
                );


        // =================================================
        // Display User Name
        // =================================================

        if (
                userName != null
                        && !userName.trim().isEmpty()
        ) {

            tvWelcome.setText(
                    "Welcome, " + userName
            );

        } else {

            tvWelcome.setText(
                    "Welcome"
            );
        }


        // =================================================
        // Display User Role
        // =================================================

        if (
                userRole != null
                        && !userRole.trim().isEmpty()
        ) {

            tvRole.setText(
                    "Role: " + userRole
            );

        } else {

            tvRole.setText(
                    "Role: User"
            );
        }


        // =================================================
        // ADD STUDENT
        // =================================================

        btnStudents.setOnClickListener(view -> {

            Intent intent =
                    new Intent(
                            DashboardActivity.this,
                            StudentActivity.class
                    );

            startActivity(intent);

        });


        // =================================================
        // STUDENT LIST
        // =================================================

        btnStudentList.setOnClickListener(view -> {

            Intent intent =
                    new Intent(
                            DashboardActivity.this,
                            StudentListActivity.class
                    );

            startActivity(intent);

        });


        // =================================================
        // MARK ATTENDANCE
        // =================================================

        btnAttendance.setOnClickListener(view -> {

            Intent intent =
                    new Intent(
                            DashboardActivity.this,
                            AttendanceActivity.class
                    );

            startActivity(intent);

        });


        // =================================================
        // ATTENDANCE HISTORY
        // =================================================

        btnHistory.setOnClickListener(view -> {

            Toast.makeText(
                    DashboardActivity.this,
                    "Attendance History",
                    Toast.LENGTH_SHORT
            ).show();

        });


        // =================================================
        // ATTENDANCE REPORT
        // =================================================

        btnReport.setOnClickListener(view -> {

            Toast.makeText(
                    DashboardActivity.this,
                    "Attendance Report",
                    Toast.LENGTH_SHORT
            ).show();

        });


        // =================================================
        // LOGOUT
        // =================================================

        btnLogout.setOnClickListener(view -> {

            Toast.makeText(
                    DashboardActivity.this,
                    "Logged out successfully",
                    Toast.LENGTH_SHORT
            ).show();


            Intent intent =
                    new Intent(
                            DashboardActivity.this,
                            MainActivity.class
                    );


            // Remove Dashboard from back stack
            intent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            |
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
            );


            startActivity(intent);

            finish();

        });
    }
}