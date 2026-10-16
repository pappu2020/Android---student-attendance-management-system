
package com.example.studentattendance;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FaceAttendanceActivity extends AppCompatActivity {

    // Existing views
    private EditText etFaceClass;
    private EditText etFaceSection;
    private PreviewView faceCameraPreview;
    private FaceOverlayView faceOverlay;
    private TextView tvFaceCameraStatus;
    private Button btnStartFaceCamera;
    private Button btnStopFaceCamera;

    // Information received from Mark Attendance
    private String selectedClass = "";
    private String selectedSection = "";
    private String selectedRoutineId = "";
    private String selectedAttendanceDate = "";
    private String selectedSubject = "";
    private String selectedTeacher = "";
    private String selectedStartTime = "";
    private String selectedEndTime = "";

    // Camera and face detector
    private ProcessCameraProvider cameraProvider;
    private ImageAnalysis imageAnalysis;
    private FaceDetector faceDetector;
    private ExecutorService cameraExecutor;

    private volatile boolean cameraRunning = false;

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    granted -> {
                        if (granted) {
                            startCamera();
                        } else {
                            updateCameraStatus(
                                    "Camera permission was denied."
                            );

                            Toast.makeText(
                                    this,
                                    "Camera permission is required.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_attendance);

        initializeViews();
        readAttendanceDetails();
        displayAttendanceDetails();

        initializeFaceDetector();
        cameraExecutor = Executors.newSingleThreadExecutor();

        setupListeners();

        updateCameraStatus("Ready to start camera");
    }

    private void initializeViews() {
        etFaceClass = findViewById(R.id.etFaceClass);
        etFaceSection = findViewById(R.id.etFaceSection);

        faceCameraPreview = findViewById(R.id.faceCameraPreview);
        faceOverlay = findViewById(R.id.faceOverlay);

        tvFaceCameraStatus = findViewById(R.id.tvFaceCameraStatus);

        btnStartFaceCamera = findViewById(R.id.btnStartFaceCamera);
        btnStopFaceCamera = findViewById(R.id.btnStopFaceCamera);
    }

    /**
     * Read the values passed by AttendanceActivity.
     */
    private void readAttendanceDetails() {

        selectedClass = getIntent().getStringExtra("class_name");
        selectedSection = getIntent().getStringExtra("section");
        selectedRoutineId = getIntent().getStringExtra("routine_id");
        selectedAttendanceDate =
                getIntent().getStringExtra("attendance_date");

        selectedSubject =
                getIntent().getStringExtra("subject_name");

        selectedTeacher =
                getIntent().getStringExtra("teacher_name");

        selectedStartTime =
                getIntent().getStringExtra("start_time");

        selectedEndTime =
                getIntent().getStringExtra("end_time");

        selectedClass = safeValue(selectedClass);
        selectedSection = safeValue(selectedSection);
        selectedRoutineId = safeValue(selectedRoutineId);
        selectedAttendanceDate = safeValue(selectedAttendanceDate);

        selectedSubject = safeValue(selectedSubject);
        selectedTeacher = safeValue(selectedTeacher);
        selectedStartTime = safeValue(selectedStartTime);
        selectedEndTime = safeValue(selectedEndTime);
    }

    private String safeValue(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * Display the previously selected class and section.
     * These fields are read-only on this screen.
     */
    private void displayAttendanceDetails() {

        etFaceClass.setText(selectedClass);
        etFaceSection.setText(selectedSection);

        etFaceClass.setEnabled(false);
        etFaceSection.setEnabled(false);

        etFaceClass.setFocusable(false);
        etFaceSection.setFocusable(false);

        etFaceClass.setClickable(false);
        etFaceSection.setClickable(false);

        StringBuilder details = new StringBuilder();

        details.append("Selected attendance session\n");

        details.append("Class: ")
                .append(displayValue(selectedClass))
                .append("\n");

        details.append("Section: ")
                .append(displayValue(selectedSection))
                .append("\n");

        details.append("Date: ")
                .append(displayValue(selectedAttendanceDate))
                .append("\n");

        details.append("Routine ID: ")
                .append(displayValue(selectedRoutineId));

        if (!selectedSubject.isEmpty()) {
            details.append("\nSubject: ")
                    .append(selectedSubject);
        }

        if (!selectedTeacher.isEmpty()) {
            details.append("\nTeacher: ")
                    .append(selectedTeacher);
        }

        if (!selectedStartTime.isEmpty()
                || !selectedEndTime.isEmpty()) {

            details.append("\nTime: ")
                    .append(displayValue(selectedStartTime))
                    .append(" - ")
                    .append(displayValue(selectedEndTime));
        }

        tvFaceCameraStatus.setText(details.toString());
    }

    private String displayValue(String value) {
        return value == null || value.trim().isEmpty()
                ? "Not available"
                : value;
    }

    private void initializeFaceDetector() {

        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(
                                FaceDetectorOptions.PERFORMANCE_MODE_FAST
                        )
                        .setLandmarkMode(
                                FaceDetectorOptions.LANDMARK_MODE_NONE
                        )
                        .setClassificationMode(
                                FaceDetectorOptions.CLASSIFICATION_MODE_NONE
                        )
                        .enableTracking()
                        .build();

        faceDetector = FaceDetection.getClient(options);
    }

    private void setupListeners() {

        btnStartFaceCamera.setOnClickListener(view -> {

            if (!validateAttendanceSession()) {
                return;
            }

            if (hasCameraPermission()) {
                startCamera();
            } else {
                cameraPermissionLauncher.launch(
                        Manifest.permission.CAMERA
                );
            }
        });

        btnStopFaceCamera.setOnClickListener(
                view -> stopCamera()
        );
    }

    /**
     * A valid session must come from the existing
     * Mark Attendance screen.
     */
    private boolean validateAttendanceSession() {

        if (selectedClass.isEmpty()) {
            showMessage(
                    "Class is missing. Return to Mark Attendance."
            );
            return false;
        }

        if (selectedSection.isEmpty()) {
            showMessage(
                    "Section is missing. Return to Mark Attendance."
            );
            return false;
        }

        if (selectedRoutineId.isEmpty()) {
            showMessage(
                    "Scheduled routine is missing. Select a period first."
            );
            return false;
        }

        if (selectedAttendanceDate.isEmpty()) {
            showMessage(
                    "Attendance date is missing. Select a date first."
            );
            return false;
        }

        return true;
    }

    private boolean hasCameraPermission() {

        return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private void startCamera() {

        if (cameraRunning || isFinishing() || isDestroyed()) {
            return;
        }

        if (cameraExecutor == null || cameraExecutor.isShutdown()) {
            showMessage("Camera processor is unavailable.");
            return;
        }

        ListenableFuture<ProcessCameraProvider> cameraFuture =
                ProcessCameraProvider.getInstance(this);

        cameraFuture.addListener(() -> {

            try {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                cameraProvider = cameraFuture.get();

                Preview preview = new Preview.Builder()
                        .build();

                preview.setSurfaceProvider(
                        faceCameraPreview.getSurfaceProvider()
                );

                imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(
                                ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                        )
                        .build();

                imageAnalysis.setAnalyzer(
                        cameraExecutor,
                        this::analyzeImage
                );

                cameraProvider.unbindAll();

                cameraProvider.bindToLifecycle(
                        this,
                        CameraSelector.DEFAULT_FRONT_CAMERA,
                        preview,
                        imageAnalysis
                );

                cameraRunning = true;

                faceOverlay.clearFaces();

                updateCameraStatus(
                        "Camera running. Looking for one student..."
                );

            } catch (ExecutionException | InterruptedException error) {

                cameraRunning = false;

                if (error instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }

                updateCameraStatus(
                        "Unable to start camera."
                );

                showMessage(
                        "Could not initialize the front camera."
                );

            } catch (Exception error) {

                cameraRunning = false;

                updateCameraStatus(
                        "Camera initialization failed."
                );

                showMessage(
                        "Camera error: " + error.getMessage()
                );
            }

        }, ContextCompat.getMainExecutor(this));
    }

    /**
     * Detect faces in each camera frame.
     * Detection alone does not identify a student.
     */
    private void analyzeImage(@NonNull ImageProxy imageProxy) {

        if (!cameraRunning || faceDetector == null) {
            imageProxy.close();
            return;
        }

        android.media.Image mediaImage = imageProxy.getImage();

        if (mediaImage == null) {
            imageProxy.close();
            return;
        }

        int rotation =
                imageProxy.getImageInfo().getRotationDegrees();

        int rotatedWidth =
                (rotation == 90 || rotation == 270)
                        ? imageProxy.getHeight()
                        : imageProxy.getWidth();

        int rotatedHeight =
                (rotation == 90 || rotation == 270)
                        ? imageProxy.getWidth()
                        : imageProxy.getHeight();

        InputImage inputImage =
                InputImage.fromMediaImage(
                        mediaImage,
                        rotation
                );

        faceDetector.process(inputImage)
                .addOnSuccessListener(faces -> {

                    if (!cameraRunning
                            || isFinishing()
                            || isDestroyed()) {
                        return;
                    }

                    List<Rect> bounds = new ArrayList<>();

                    for (Face face : faces) {
                        bounds.add(
                                new Rect(face.getBoundingBox())
                        );
                    }

                    faceOverlay.updateFaces(
                            bounds,
                            rotatedWidth,
                            rotatedHeight,
                            true
                    );

                    runOnUiThread(() -> {

                        if (!cameraRunning
                                || isFinishing()
                                || isDestroyed()) {
                            return;
                        }

                        if (faces.isEmpty()) {

                            updateCameraStatus(
                                    "No face detected. Face the camera."
                            );

                        } else if (faces.size() == 1) {

                            updateCameraStatus(
                                    "One face detected. Identity is not verified yet."
                            );

                        } else {

                            updateCameraStatus(
                                    faces.size()
                                            + " faces detected. Scan one student at a time."
                            );
                        }
                    });
                })
                .addOnFailureListener(error -> {

                    runOnUiThread(() -> {

                        if (cameraRunning && !isFinishing()) {
                            updateCameraStatus(
                                    "Face detection failed. Try again."
                            );
                        }
                    });
                })
                .addOnCompleteListener(task ->
                        imageProxy.close()
                );
    }

    private void stopCamera() {

        cameraRunning = false;

        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }

        if (faceOverlay != null) {
            faceOverlay.clearFaces();
        }

        updateCameraStatus("Camera is stopped");
    }

    private void updateCameraStatus(@NonNull String message) {

        if (tvFaceCameraStatus != null) {
            tvFaceCameraStatus.setText(message);
        }

        if (btnStartFaceCamera != null) {
            btnStartFaceCamera.setEnabled(!cameraRunning);
        }

        if (btnStopFaceCamera != null) {
            btnStopFaceCamera.setEnabled(cameraRunning);
        }
    }

    private void showMessage(String message) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    @Override
    protected void onStop() {
        stopCamera();
        super.onStop();
    }

    @Override
    protected void onDestroy() {

        stopCamera();

        if (faceDetector != null) {
            faceDetector.close();
            faceDetector = null;
        }

        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
            cameraExecutor = null;
        }

        super.onDestroy();
    }
}
