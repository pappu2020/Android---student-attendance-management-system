
package com.example.studentattendance;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
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

    private static final String PREFS_NAME =
            "StudentAttendanceAuth";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Always show the login screen when this activity starts.
        setContentView(R.layout.activity_main);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBar);
        tvMessage = findViewById(R.id.tvMessage);

        progressBar.setVisibility(ProgressBar.GONE);

        // Do not automatically navigate to Dashboard
        // just because a token is already saved.

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

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

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

        progressBar.setVisibility(ProgressBar.VISIBLE);
        btnLogin.setEnabled(false);
        tvMessage.setText("");

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
                        "application/x-www-form-urlencoded; charset=UTF-8"
                );

                connection.setRequestProperty(
                        "Accept",
                        "application/json"
                );

                String postData =
                        "email=" + URLEncoder.encode(email, "UTF-8")
                                + "&password=" + URLEncoder.encode(
                                password, "UTF-8"
                        );

                try (OutputStream outputStream =
                             connection.getOutputStream()) {

                    outputStream.write(
                            postData.getBytes(StandardCharsets.UTF_8)
                    );

                    outputStream.flush();
                }

                int responseCode = connection.getResponseCode();

                InputStream inputStream =
                        (responseCode >= 200 && responseCode < 300)
                                ? connection.getInputStream()
                                : connection.getErrorStream();

                if (inputStream == null) {
                    throw new Exception(
                            "Server returned HTTP " + responseCode
                    );
                }

                StringBuilder response = new StringBuilder();

                try (BufferedReader reader =
                             new BufferedReader(
                                     new InputStreamReader(
                                             inputStream,
                                             StandardCharsets.UTF_8
                                     )
                             )) {

                    String line;

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }

                JSONObject jsonResponse =
                        new JSONObject(response.toString());

                boolean success =
                        jsonResponse.optBoolean("success", false);

                String message =
                        jsonResponse.optString(
                                "message",
                                "Unable to login"
                        );

                if (responseCode < 200 || responseCode >= 300) {
                    success = false;
                }

                if (success) {

                    JSONObject user =
                            jsonResponse.optJSONObject("user");

                    String token =
                            jsonResponse.optString("token", "");

                    if (user == null
                            || token.trim().isEmpty()
                            || !user.has("name")
                            || !user.has("role")) {

                        throw new Exception(
                                "Login response is missing token or user details"
                        );
                    }

                    String userName =
                            user.optString("name", "").trim();

                    String userRole =
                            user.optString("role", "")
                                    .trim()
                                    .toLowerCase(Locale.ROOT);

                    if (userName.isEmpty()
                            || (!userRole.equals("admin")
                            && !userRole.equals("teacher"))) {

                        throw new Exception(
                                "Invalid user name or role from server"
                        );
                    }

                    // Save the authenticated session.
                    SharedPreferences preferences =
                            getSharedPreferences(
                                    PREFS_NAME,
                                    MODE_PRIVATE
                            );

                    boolean saved = preferences.edit()
                            .putString("auth_token", token)
                            .putString("user_name", userName)
                            .putString("user_role", userRole)
                            .commit();

                    if (!saved) {
                        throw new Exception(
                                "Could not save login session"
                        );
                    }

                    final String finalName = userName;
                    final String finalRole = userRole;
                    final String finalMessage = message;

                    runOnUiThread(() -> {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        progressBar.setVisibility(ProgressBar.GONE);
                        btnLogin.setEnabled(true);

                        tvMessage.setTextColor(
                                getResources().getColor(
                                        android.R.color.holo_green_dark
                                )
                        );

                        tvMessage.setText(finalMessage);

                        Toast.makeText(
                                MainActivity.this,
                                "Login successful",
                                Toast.LENGTH_SHORT
                        ).show();

                        openDashboard(finalName, finalRole);
                    });

                } else {

                    final String errorMessage = message;

                    runOnUiThread(() -> {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        progressBar.setVisibility(ProgressBar.GONE);
                        btnLogin.setEnabled(true);

                        tvMessage.setTextColor(
                                getResources().getColor(
                                        android.R.color.holo_red_dark
                                )
                        );

                        tvMessage.setText(errorMessage);
                    });
                }

            } catch (Exception e) {

                final String errorMessage =
                        e.getMessage() == null
                                ? "Unexpected error occurred"
                                : e.getMessage();

                runOnUiThread(() -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    progressBar.setVisibility(ProgressBar.GONE);
                    btnLogin.setEnabled(true);

                    tvMessage.setTextColor(
                            getResources().getColor(
                                    android.R.color.holo_red_dark
                            )
                    );

                    tvMessage.setText(
                            "Login error: " + errorMessage
                    );

                    Toast.makeText(
                            MainActivity.this,
                            "Could not complete login",
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

    private void openDashboard(String userName, String userRole) {

        Intent intent = new Intent(
                MainActivity.this,
                DashboardActivity.class
        );

        intent.putExtra("user_name", userName);
        intent.putExtra("user_role", userRole);

        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdownNow();
    }
}
