package com.example.studentattendance;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private EditText etEmail;
    private EditText etPassword;

    private Button btnLogin;

    private ProgressBar progressBar;

    private TextView tvMessage;

    private final ExecutorService executorService =
            Executors.newSingleThreadExecutor();

    private static final String LOGIN_URL =
            "http://10.0.2.2/student_attendance_api/login.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);

        btnLogin = findViewById(R.id.btnLogin);

        progressBar = findViewById(R.id.progressBar);

        tvMessage = findViewById(R.id.tvMessage);

        btnLogin.setOnClickListener(view -> {

            if (validateInput()) {

                String email =
                        etEmail.getText().toString().trim();

                String password =
                        etPassword.getText().toString();

                loginUser(email, password);
            }
        });
    }

    private boolean validateInput() {

        String email =
                etEmail.getText().toString().trim();

        String password =
                etPassword.getText().toString();

        tvMessage.setText("");

        if (TextUtils.isEmpty(email)) {

            etEmail.setError("Email is required");
            etEmail.requestFocus();

            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {

            etEmail.setError("Enter a valid email");
            etEmail.requestFocus();

            return false;
        }

        if (TextUtils.isEmpty(password)) {

            etPassword.setError("Password is required");
            etPassword.requestFocus();

            return false;
        }

        if (password.length() < 6) {

            etPassword.setError(
                    "Password must contain at least 6 characters"
            );

            etPassword.requestFocus();

            return false;
        }

        return true;
    }

    private void loginUser(String email, String password) {

        runOnUiThread(() -> {

            progressBar.setVisibility(ProgressBar.VISIBLE);

            btnLogin.setEnabled(false);

            tvMessage.setText("");

        });

        executorService.execute(() -> {

            HttpURLConnection connection = null;

            try {

                URL url = new URL(LOGIN_URL);

                connection =
                        (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("POST");

                connection.setConnectTimeout(10000);

                connection.setReadTimeout(10000);

                connection.setDoOutput(true);

                connection.setRequestProperty(
                        "Content-Type",
                        "application/x-www-form-urlencoded"
                );

                String postData =
                        "email=" +
                                URLEncoder.encode(email, "UTF-8") +
                                "&password=" +
                                URLEncoder.encode(password, "UTF-8");

                OutputStream outputStream =
                        connection.getOutputStream();

                outputStream.write(
                        postData.getBytes("UTF-8")
                );

                outputStream.flush();

                outputStream.close();

                int responseCode =
                        connection.getResponseCode();

                InputStream inputStream;

                if (responseCode >= 200 &&
                        responseCode < 300) {

                    inputStream =
                            connection.getInputStream();

                } else {

                    inputStream =
                            connection.getErrorStream();
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

                while ((line = reader.readLine()) != null) {

                    response.append(line);
                }

                reader.close();

                String responseText =
                        response.toString();

                JSONObject jsonResponse =
                        new JSONObject(responseText);

                boolean success =
                        jsonResponse.getBoolean("success");

                String message =
                        jsonResponse.getString("message");

                runOnUiThread(() -> {

                    progressBar.setVisibility(
                            ProgressBar.GONE
                    );

                    btnLogin.setEnabled(true);

                    if (success) {

                        tvMessage.setTextColor(
                                getResources().getColor(
                                        android.R.color.holo_green_dark
                                )
                        );

                        tvMessage.setText(message);

                        Toast.makeText(
                                MainActivity.this,
                                "Login Successful",
                                Toast.LENGTH_SHORT
                        ).show();

                        try {

                            JSONObject user =
                                    jsonResponse.getJSONObject("user");

                            String userName =
                                    user.getString("name");

                            String userRole =
                                    user.getString("role");

                            Intent intent =
                                    new Intent(
                                            MainActivity.this,
                                            DashboardActivity.class
                                    );

                            intent.putExtra(
                                    "user_name",
                                    userName
                            );

                            intent.putExtra(
                                    "user_role",
                                    userRole
                            );

                            startActivity(intent);

                            finish();

                        } catch (Exception e) {

                            tvMessage.setText(
                                    "Login successful, but user data is missing"
                            );
                        }
                    } else {

                        tvMessage.setTextColor(
                                getResources().getColor(
                                        android.R.color.holo_red_dark
                                )
                        );

                        tvMessage.setText(message);

                    }

                });

            } catch (Exception e) {

                runOnUiThread(() -> {

                    progressBar.setVisibility(
                            ProgressBar.GONE
                    );

                    btnLogin.setEnabled(true);

                    tvMessage.setTextColor(
                            getResources().getColor(
                                    android.R.color.holo_red_dark
                            )
                    );

                    tvMessage.setText(
                            "Connection error: " +
                                    e.getMessage()
                    );

                    Toast.makeText(
                            MainActivity.this,
                            "Could not connect to server",
                            Toast.LENGTH_LONG
                    ).show();

                });

            } finally {

                if (connection != null) {

                    connection.disconnect();
                }
            }

        });
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        executorService.shutdown();
    }
}