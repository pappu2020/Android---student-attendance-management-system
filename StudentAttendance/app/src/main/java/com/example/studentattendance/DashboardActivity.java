package com.example.studentattendance;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class DashboardActivity extends AppCompatActivity {

    private TextView tvDashboardTitle;
    private TextView tvWelcome;
    private TextView tvRole;

    private LinearLayout btnStudents;
    private LinearLayout btnStudentList;
    private LinearLayout btnAttendance;
    private LinearLayout btnHistory;
    private LinearLayout btnReport;

    private Button btnLogout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_dashboard
        );


        // -------------------------------------------------
        // Initialize Views
        // -------------------------------------------------

        tvDashboardTitle =
                findViewById(R.id.tvDashboardTitle);

        tvWelcome =
                findViewById(R.id.tvWelcome);

        tvRole =
                findViewById(R.id.tvRole);

        btnStudents =
                findViewById(R.id.btnStudents);

        btnStudentList =
                findViewById(R.id.btnStudentList);

        btnAttendance =
                findViewById(R.id.btnAttendance);

        btnHistory =
                findViewById(R.id.btnHistory);

        btnReport =
                findViewById(R.id.btnReport);

        btnLogout =
                findViewById(R.id.btnLogout);


        // -------------------------------------------------
        // Get Login Information
        // -------------------------------------------------

        String userName =
                getIntent().getStringExtra(
                        "user_name"
                );

        String userRole =
                getIntent().getStringExtra(
                        "user_role"
                );


        // -------------------------------------------------
        // Welcome Message
        // -------------------------------------------------

        if (userName != null
                && !userName.trim().isEmpty()) {

            tvWelcome.setText(
                    "Welcome, " + userName
            );

        } else {

            tvWelcome.setText(
                    "Welcome"
            );
        }


        // -------------------------------------------------
        // User Role
        // -------------------------------------------------

        if (userRole != null
                && !userRole.trim().isEmpty()) {

            tvRole.setText(
                    "Role: " + userRole
            );

        } else {

            tvRole.setText(
                    "Role: User"
            );
        }


        // =================================================
        // STUDENT MANAGEMENT
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
        // ATTENDANCE
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

            Intent intent =
                    new Intent(
                            DashboardActivity.this,
                            AttendanceRecord.class
                    );

            startActivity(intent);
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