package com.example.studentattendance;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TeacherListActivity extends AppCompatActivity {

    private static final String BASE_URL =
            "http://10.0.2.2/student_attendance_api/";

    private static final String GET_TEACHERS_URL =
            BASE_URL + "get_teachers.php";

    private static final String UPDATE_TEACHER_URL =
            BASE_URL + "update_teacher.php";

    private static final String PREFS_NAME =
            "StudentAttendanceAuth";

    private String authToken = "";
    private String userRole = "";

    private EditText etSearchTeacher;
    private LinearLayout layoutTeacherContainer;
    private TextView tvTeacherMessage;
    private TextView tvTeacherCount;
    private ProgressBar progressTeachers;
    private Button btnRefreshTeachers;
    private Button btnBack;

    private final List<JSONObject> teacherList =
            new ArrayList<>();

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_list);

        initializeViews();
        loadSession();

        if (!isAdmin()) {
            Toast.makeText(
                    this,
                    "Admin permission required.",
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }

        setupListeners();
        loadTeachers();
    }

    private void initializeViews() {
        etSearchTeacher =
                findViewById(R.id.etSearchTeacher);

        layoutTeacherContainer =
                findViewById(R.id.layoutTeacherContainer);

        tvTeacherMessage =
                findViewById(R.id.tvTeacherMessage);

        tvTeacherCount =
                findViewById(R.id.tvTeacherCount);

        progressTeachers =
                findViewById(R.id.progressTeachers);

        btnRefreshTeachers =
                findViewById(R.id.btnRefreshTeachers);

        btnBack =
                findViewById(R.id.btnBack);
    }

    private void loadSession() {
        SharedPreferences preferences =
                getSharedPreferences(
                        PREFS_NAME,
                        MODE_PRIVATE
                );

        authToken = preferences.getString(
                "auth_token",
                ""
        );

        userRole = preferences.getString(
                "user_role",
                ""
        );

        if (authToken == null) {
            authToken = "";
        }

        if (userRole == null) {
            userRole = "";
        }

        userRole = userRole.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isAdmin() {
        return "admin".equals(userRole)
                && !authToken.trim().isEmpty();
    }

    private void setupListeners() {
        btnBack.setOnClickListener(view -> finish());

        btnRefreshTeachers.setOnClickListener(
                view -> loadTeachers()
        );

        etSearchTeacher.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after
                    ) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count
                    ) {
                        displayTeachers(s.toString());
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s
                    ) {
                    }
                }
        );
    }

    // ------------------------------------------------------------
    // LOAD TEACHERS
    // ------------------------------------------------------------

    private void loadTeachers() {
        if (!isAdmin()) {
            finish();
            return;
        }

        progressTeachers.setVisibility(View.VISIBLE);
        btnRefreshTeachers.setEnabled(false);
        tvTeacherMessage.setText("Loading teachers...");

        executor.execute(() -> {
            String response = sendRequest(
                    GET_TEACHERS_URL,
                    "",
                    "GET"
            );

            mainHandler.post(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                progressTeachers.setVisibility(View.GONE);
                btnRefreshTeachers.setEnabled(true);

                handleTeacherResponse(response);
            });
        });
    }

    private void handleTeacherResponse(String response) {
        if (response == null
                || response.trim().isEmpty()
                || response.startsWith("ERROR:")) {

            tvTeacherMessage.setText(
                    response == null
                            ? "Unable to connect to the server."
                            : response.replace("ERROR:", "").trim()
            );

            layoutTeacherContainer.removeAllViews();
            tvTeacherCount.setText("Teachers: 0");
            return;
        }

        try {
            JSONObject json = new JSONObject(response);

            if (!json.optBoolean("success", false)) {
                tvTeacherMessage.setText(
                        json.optString(
                                "message",
                                "Unable to load teachers."
                        )
                );
                return;
            }

            JSONArray teachers =
                    json.optJSONArray("teachers");

            teacherList.clear();

            if (teachers != null) {
                for (int i = 0; i < teachers.length(); i++) {
                    JSONObject teacher =
                            teachers.optJSONObject(i);

                    if (teacher != null) {
                        teacherList.add(teacher);
                    }
                }
            }

            tvTeacherCount.setText(
                    "Teachers: " + teacherList.size()
            );

            displayTeachers(
                    etSearchTeacher.getText().toString()
            );

        } catch (Exception e) {
            tvTeacherMessage.setText(
                    "Invalid server response."
            );
        }
    }

    // ------------------------------------------------------------
    // SEARCH AND DISPLAY
    // ------------------------------------------------------------

    private void displayTeachers(String searchText) {
        layoutTeacherContainer.removeAllViews();

        String query = searchText == null
                ? ""
                : searchText.trim().toLowerCase(Locale.ROOT);

        int displayed = 0;

        for (JSONObject teacher : teacherList) {
            String name = teacher.optString("name", "");
            String email = teacher.optString("email", "");
            String id = teacher.optString("id", "");

            String searchable =
                    (name + " " + email + " " + id)
                            .toLowerCase(Locale.ROOT);

            if (!searchable.contains(query)) {
                continue;
            }

            layoutTeacherContainer.addView(
                    createTeacherCard(teacher)
            );

            displayed++;
        }

        if (displayed == 0) {
            tvTeacherMessage.setVisibility(View.VISIBLE);

            tvTeacherMessage.setText(
                    teacherList.isEmpty()
                            ? "No teachers found."
                            : "No matching teachers found."
            );
        } else {
            tvTeacherMessage.setVisibility(View.GONE);
        }
    }

    private View createTeacherCard(JSONObject teacher) {
        String id = teacher.optString("id", "");
        String name = teacher.optString("name", "Teacher");
        String email = teacher.optString("email", "");
        String createdAt = teacher.optString("created_at", "");

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackgroundColor(Color.WHITE);
        card.setElevation(dp(2));

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(cardParams);

        TextView tvName = new TextView(this);
        tvName.setText(name);
        tvName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        tvName.setTypeface(null, Typeface.BOLD);
        tvName.setTextColor(Color.rgb(31, 41, 55));

        TextView tvId = new TextView(this);
        tvId.setText("Teacher ID: " + id);
        tvId.setTextSize(13);
        tvId.setTextColor(Color.DKGRAY);

        TextView tvEmail = new TextView(this);
        tvEmail.setText("Email: " + email);
        tvEmail.setTextSize(14);
        tvEmail.setTextColor(Color.rgb(55, 65, 81));

        card.addView(tvName);
        card.addView(tvId);
        card.addView(tvEmail);

        if (!createdAt.isEmpty()
                && !"null".equalsIgnoreCase(createdAt)) {
            TextView tvCreated = new TextView(this);
            tvCreated.setText("Created: " + createdAt);
            tvCreated.setTextSize(12);
            tvCreated.setTextColor(Color.GRAY);
            card.addView(tvCreated);
        }

        Button btnEdit = new Button(this);
        btnEdit.setText("Edit Teacher");

        LinearLayout.LayoutParams buttonParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        buttonParams.topMargin = dp(8);
        btnEdit.setLayoutParams(buttonParams);

        btnEdit.setOnClickListener(
                view -> showEditTeacherDialog(teacher)
        );

        card.addView(btnEdit);

        return card;
    }

    // ------------------------------------------------------------
    // EDIT TEACHER
    // ------------------------------------------------------------

    private void showEditTeacherDialog(JSONObject teacher) {
        if (!isAdmin()) {
            showMessage("Admin permission required.");
            return;
        }

        String teacherId = teacher.optString("id", "");

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(8), dp(20), dp(8));

        EditText etName = new EditText(this);
        etName.setHint("Teacher name");
        etName.setSingleLine(true);
        etName.setText(teacher.optString("name", ""));

        EditText etEmail = new EditText(this);
        etEmail.setHint("Teacher email");
        etEmail.setSingleLine(true);
        etEmail.setInputType(
                android.text.InputType.TYPE_CLASS_TEXT
                        | android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        );
        etEmail.setText(teacher.optString("email", ""));

        EditText etPassword = new EditText(this);
        etPassword.setHint("New password (optional)");
        etPassword.setSingleLine(true);
        etPassword.setInputType(
                android.text.InputType.TYPE_CLASS_TEXT
                        | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        form.addView(etName);
        form.addView(etEmail);
        form.addView(etPassword);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Edit Teacher")
                .setView(form)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null)
                .create();

        dialog.setOnShowListener(ignored ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener(view -> {

                            String name =
                                    etName.getText().toString().trim();

                            String email =
                                    etEmail.getText().toString().trim();

                            String password =
                                    etPassword.getText().toString();

                            if (name.isEmpty()) {
                                etName.setError(
                                        "Name is required."
                                );
                                return;
                            }

                            if (email.isEmpty()
                                    || !android.util.Patterns.EMAIL_ADDRESS
                                    .matcher(email).matches()) {
                                etEmail.setError(
                                        "Enter a valid email."
                                );
                                return;
                            }

                            if (!password.isEmpty()
                                    && password.length() < 8) {
                                etPassword.setError(
                                        "Password must be at least 8 characters."
                                );
                                return;
                            }

                            dialog.dismiss();

                            updateTeacher(
                                    teacherId,
                                    name,
                                    email,
                                    password
                            );
                        })
        );

        dialog.show();
    }

    private void updateTeacher(
            String teacherId,
            String name,
            String email,
            String password
    ) {
        if (!isAdmin()) {
            showMessage("Admin permission required.");
            return;
        }

        progressTeachers.setVisibility(View.VISIBLE);

        executor.execute(() -> {
            StringBuilder body = new StringBuilder();

            try {
                body.append("teacher_id=")
                        .append(encode(teacherId));

                body.append("&name=")
                        .append(encode(name));

                body.append("&email=")
                        .append(encode(email));

                if (!password.isEmpty()) {
                    body.append("&password=")
                            .append(encode(password));
                }

            } catch (Exception e) {
                mainHandler.post(() -> {
                    progressTeachers.setVisibility(View.GONE);
                    showMessage("Unable to prepare update request.");
                });
                return;
            }

            String response = sendRequest(
                    UPDATE_TEACHER_URL,
                    body.toString(),
                    "POST"
            );

            mainHandler.post(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                progressTeachers.setVisibility(View.GONE);

                if (response == null
                        || response.startsWith("ERROR:")) {
                    showMessage(
                            response == null
                                    ? "Update failed."
                                    : response.replace("ERROR:", "").trim()
                    );
                    return;
                }

                try {
                    JSONObject json = new JSONObject(response);

                    if (json.optBoolean("success", false)) {
                        showMessage(
                                json.optString(
                                        "message",
                                        "Teacher updated successfully."
                                )
                        );

                        loadTeachers();

                    } else {
                        showMessage(
                                json.optString(
                                        "message",
                                        "Teacher update failed."
                                )
                        );
                    }

                } catch (Exception e) {
                    showMessage(
                            "Invalid server response."
                    );
                }
            });
        });
    }

    // ------------------------------------------------------------
    // HTTP REQUEST
    // ------------------------------------------------------------

    private String sendRequest(
            String endpoint,
            String body,
            String method
    ) {
        HttpURLConnection connection = null;

        try {
            URL url = new URL(endpoint);

            connection =
                    (HttpURLConnection) url.openConnection();

            connection.setRequestMethod(method);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            connection.setRequestProperty(
                    "Accept",
                    "application/json"
            );

            connection.setRequestProperty(
                    "Authorization",
                    "Bearer " + authToken
            );

            if ("POST".equals(method)) {
                connection.setDoOutput(true);

                connection.setRequestProperty(
                        "Content-Type",
                        "application/x-www-form-urlencoded; charset=UTF-8"
                );

                try (OutputStream output =
                             connection.getOutputStream()) {
                    output.write(
                            body.getBytes(StandardCharsets.UTF_8)
                    );
                    output.flush();
                }
            }

            int status = connection.getResponseCode();

            InputStream input =
                    (status >= 200 && status < 300)
                            ? connection.getInputStream()
                            : connection.getErrorStream();

            if (input == null) {
                return "ERROR: HTTP " + status;
            }

            StringBuilder result = new StringBuilder();

            try (BufferedReader reader =
                         new BufferedReader(
                                 new InputStreamReader(
                                         input,
                                         StandardCharsets.UTF_8
                                 )
                         )) {

                String line;

                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
            }

            if (status == HttpURLConnection.HTTP_UNAUTHORIZED) {
                return "ERROR: Session expired. Please log in again.";
            }

            if (status == HttpURLConnection.HTTP_FORBIDDEN) {
                return "ERROR: Admin permission required.";
            }

            if (status < 200 || status >= 300) {
                return "ERROR: HTTP " + status + ": " + result;
            }

            return result.toString();

        } catch (Exception e) {
            return "ERROR: Unable to connect to the server. "
                    + "Check XAMPP and the API URL.";

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String encode(String value) throws Exception {
        return URLEncoder.encode(value, "UTF-8");
    }

    private int dp(int value) {
        return (int) (
                value
                        * getResources()
                        .getDisplayMetrics()
                        .density
                        + 0.5f
        );
    }

    private void showMessage(String message) {
        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}