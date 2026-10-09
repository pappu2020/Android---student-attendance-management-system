
package com.example.studentattendance;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;
import android.os.CancellationSignal;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.graphics.drawable.GradientDrawable;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AttendanceReportActivity extends AppCompatActivity {

    private static final String REPORT_URL =
            "http://10.0.2.2/student_attendance_api/get_class_monthly_report.php";

    private Spinner spinnerReportClass;
    private Spinner spinnerReportSection;

    private TextView tvReportMonth;
    private TextView tvReportTotalStudents;
    private TextView tvReportAverage;
    private TextView tvReportMessage;

    private Button btnGenerateReport;
    private Button btnExportPdf;
    private Button btnSharePdf;
    private Button btnPrintPdf;

    private LinearLayout llReportStudents;

    private String selectedMonth = "";

    private JSONObject lastReportJson = null;
    private File lastPdfFile = null;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_attendance_report);

        initializeViews();
        setupClassSpinner();
        setupSectionSpinner();
        setupMonthSelector();
        setupGenerateButton();
        setupExportButton();
        setupShareAndPrintButtons();

        Calendar calendar = Calendar.getInstance();

        selectedMonth = new SimpleDateFormat(
                "yyyy-MM",
                Locale.getDefault()
        ).format(calendar.getTime());

        tvReportMonth.setText(
                new SimpleDateFormat(
                        "MMMM yyyy",
                        Locale.getDefault()
                ).format(calendar.getTime())
        );
    }

    // INITIALIZE VIEWS

    private void initializeViews() {

        spinnerReportClass =
                findViewById(R.id.spinnerReportClass);

        spinnerReportSection =
                findViewById(R.id.spinnerReportSection);

        tvReportMonth =
                findViewById(R.id.tvReportMonth);

        tvReportTotalStudents =
                findViewById(R.id.tvReportTotalStudents);

        tvReportAverage =
                findViewById(R.id.tvReportAverage);

        tvReportMessage =
                findViewById(R.id.tvReportMessage);

        btnGenerateReport =
                findViewById(R.id.btnGenerateReport);

        btnExportPdf =
                findViewById(R.id.btnExportPdf);

        btnSharePdf =
                findViewById(R.id.btnSharePdf);

        btnPrintPdf =
                findViewById(R.id.btnPrintPdf);

        llReportStudents =
                findViewById(R.id.llReportStudents);

        btnExportPdf.setEnabled(false);
        btnSharePdf.setEnabled(false);
        btnPrintPdf.setEnabled(false);
    }

    // CLASS SPINNER

    private void setupClassSpinner() {

        ArrayList<String> classes = new ArrayList<>();

        classes.add("Select Class");

        for (int i = 6; i <= 12; i++) {
            classes.add("Class " + i);
        }

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        classes
                );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerReportClass.setAdapter(adapter);
    }

    // SECTION SPINNER

    private void setupSectionSpinner() {

        ArrayList<String> sections = new ArrayList<>();

        sections.add("Select Section");
        sections.add("Section A");
        sections.add("Section B");
        sections.add("Section C");
        sections.add("Section D");

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        sections
                );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerReportSection.setAdapter(adapter);
    }

    // MONTH PICKER

    private void setupMonthSelector() {

        tvReportMonth.setOnClickListener(view -> {

            Calendar calendar = Calendar.getInstance();

            try {
                java.util.Date parsed =
                        new SimpleDateFormat(
                                "yyyy-MM",
                                Locale.getDefault()
                        ).parse(selectedMonth);

                if (parsed != null) {
                    calendar.setTime(parsed);
                }

            } catch (Exception ignored) {
            }

            DatePickerDialog dialog =
                    new DatePickerDialog(
                            AttendanceReportActivity.this,
                            (picker, year, month, day) -> {

                                Calendar chosen =
                                        Calendar.getInstance();

                                chosen.set(year, month, 1);

                                selectedMonth =
                                        new SimpleDateFormat(
                                                "yyyy-MM",
                                                Locale.getDefault()
                                        ).format(chosen.getTime());

                                tvReportMonth.setText(
                                        new SimpleDateFormat(
                                                "MMMM yyyy",
                                                Locale.getDefault()
                                        ).format(chosen.getTime())
                                );

                                invalidateCurrentReport();
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            1
                    );

            dialog.show();
        });
    }

    // CLEAR OLD REPORT WHEN FILTER CHANGES

    private void invalidateCurrentReport() {

        lastReportJson = null;
        lastPdfFile = null;

        btnExportPdf.setEnabled(false);
        btnSharePdf.setEnabled(false);
        btnPrintPdf.setEnabled(false);
    }

    // GENERATE REPORT

    private void setupGenerateButton() {

        btnGenerateReport.setOnClickListener(view -> {

            if (spinnerReportClass.getSelectedItemPosition() == 0) {
                showMessage("Please select a class.");
                return;
            }

            if (spinnerReportSection.getSelectedItemPosition() == 0) {
                showMessage("Please select a section.");
                return;
            }

            if (selectedMonth.isEmpty()) {
                showMessage("Please select a month.");
                return;
            }

            loadReport();
        });
    }

    // EXPORT BUTTON

    private void setupExportButton() {

        btnExportPdf.setOnClickListener(view -> {

            if (lastReportJson == null) {
                showMessage("Please generate a report first.");
                return;
            }

            exportReportToPdf();
        });
    }

    // SHARE AND PRINT BUTTONS

    private void setupShareAndPrintButtons() {

        btnSharePdf.setOnClickListener(view -> sharePdf());

        btnPrintPdf.setOnClickListener(view -> printPdf());
    }

    // LOAD REPORT FROM PHP API

    private void loadReport() {

        String className =
                spinnerReportClass.getSelectedItem().toString();

        String section =
                spinnerReportSection.getSelectedItem().toString();

        String month = selectedMonth;

        invalidateCurrentReport();

        btnGenerateReport.setEnabled(false);

        tvReportMessage.setText("Generating monthly report...");

        llReportStudents.removeAllViews();

        executor.execute(() -> {

            String response;

            try {
                String postData =
                        "class_name=" + URLEncoder.encode(
                                className, "UTF-8"
                        )
                                + "&section=" + URLEncoder.encode(
                                section, "UTF-8"
                        )
                                + "&month=" + URLEncoder.encode(
                                month, "UTF-8"
                        );

                response = sendPostRequest(postData);

            } catch (Exception e) {
                response = "ERROR:" + e.getMessage();
            }

            final String finalResponse = response;

            mainHandler.post(() -> {

                if (isFinishing() || isDestroyed()) {
                    return;
                }

                btnGenerateReport.setEnabled(true);

                handleReportResponse(finalResponse);
            });
        });
    }

    // HTTP POST

    private String sendPostRequest(String postData) {

        HttpURLConnection connection = null;

        try {
            URL url = new URL(REPORT_URL);

            connection =
                    (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            connection.setRequestProperty(
                    "Content-Type",
                    "application/x-www-form-urlencoded; charset=UTF-8"
            );

            try (OutputStream output = connection.getOutputStream()) {
                output.write(postData.getBytes("UTF-8"));
                output.flush();
            }

            int responseCode = connection.getResponseCode();

            InputStream input;

            if (responseCode >= 200 && responseCode < 300) {
                input = connection.getInputStream();
            } else {
                input = connection.getErrorStream();
            }

            if (input == null) {
                return "ERROR: Empty server response";
            }

            StringBuilder response = new StringBuilder();

            try (BufferedReader reader =
                         new BufferedReader(
                                 new InputStreamReader(input, "UTF-8")
                         )) {

                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            return response.toString();

        } catch (Exception e) {

            return "ERROR:" + e.getMessage();

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    // HANDLE API RESPONSE

    private void handleReportResponse(String response) {

        if (response == null || response.trim().isEmpty()) {
            showMessage("No response from server.");
            return;
        }

        if (response.startsWith("ERROR:")) {
            showMessage("Network error: " + response.substring(6));
            return;
        }

        try {
            JSONObject json = new JSONObject(response);

            if (!json.optBoolean("success", false)) {
                showMessage(
                        json.optString(
                                "message",
                                "Failed to generate report."
                        )
                );
                return;
            }

            lastReportJson = json;

            JSONArray students =
                    json.optJSONArray("students");

            int totalStudents =
                    json.optInt("total_students", 0);

            double classAverage =
                    json.optDouble("class_average", 0);

            tvReportTotalStudents.setText(
                    String.valueOf(totalStudents)
            );

            tvReportAverage.setText(
                    String.format(
                            Locale.getDefault(),
                            "%.1f%%",
                            classAverage
                    )
            );

            llReportStudents.removeAllViews();

            if (students != null) {
                for (int i = 0; i < students.length(); i++) {
                    addStudentReportCard(
                            students.getJSONObject(i)
                    );
                }
            }

            if (totalStudents == 0) {
                tvReportMessage.setText(
                        "No students found for this class and section."
                );
            } else {
                tvReportMessage.setText(
                        "Report generated for "
                                + json.optString("month", selectedMonth)
                );
            }

            // Enable export only if there are student records.
            boolean hasStudents =
                    students != null && students.length() > 0;

            btnExportPdf.setEnabled(hasStudents);

            // Share/print are enabled only after PDF creation.
            btnSharePdf.setEnabled(false);
            btnPrintPdf.setEnabled(false);
            lastPdfFile = null;

        } catch (Exception e) {

            invalidateCurrentReport();
            showMessage("Invalid server response.");
        }
    }

    // STUDENT REPORT CARD

    private void addStudentReportCard(JSONObject student) {

        LinearLayout card = new LinearLayout(this);

        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(16, 16, 16, 16);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(0, 0, 0, 12);
        card.setLayoutParams(params);

        GradientDrawable background = new GradientDrawable();

        background.setColor(Color.WHITE);
        background.setCornerRadius(20);
        background.setStroke(
                1,
                Color.rgb(230, 235, 240)
        );

        card.setBackground(background);

        String name =
                student.optString("name", "Unknown Student");

        String roll =
                student.optString("roll_number", "-");

        int total =
                student.optInt("total_records", 0);

        int present =
                student.optInt("present_count", 0);

        int absent =
                student.optInt("absent_count", 0);

        double percentage =
                student.optDouble("attendance_percentage", 0);

        TextView nameView = new TextView(this);

        nameView.setText(name);
        nameView.setTextSize(18);
        nameView.setTextColor(Color.rgb(38, 50, 56));
        nameView.setTypeface(null, Typeface.BOLD);

        card.addView(nameView);

        TextView rollView = new TextView(this);

        rollView.setText("Roll No: " + roll);
        rollView.setTextSize(14);
        rollView.setTextColor(Color.rgb(84, 110, 122));
        rollView.setPadding(0, 5, 0, 10);

        card.addView(rollView);

        LinearLayout stats = new LinearLayout(this);

        stats.setOrientation(LinearLayout.HORIZONTAL);
        stats.setGravity(Gravity.CENTER_VERTICAL);

        addStat(stats, "TOTAL", total, "#1565C0");
        addStat(stats, "PRESENT", present, "#2E7D32");
        addStat(stats, "ABSENT", absent, "#C62828");

        card.addView(stats);

        TextView percentageView = new TextView(this);

        percentageView.setText(
                String.format(
                        Locale.getDefault(),
                        "Attendance: %.1f%%",
                        percentage
                )
        );

        percentageView.setTextSize(15);
        percentageView.setTypeface(null, Typeface.BOLD);
        percentageView.setTextColor(Color.rgb(106, 27, 154));
        percentageView.setPadding(0, 14, 0, 0);

        card.addView(percentageView);

        llReportStudents.addView(card);
    }

    // ADD SUMMARY STATISTIC

    private void addStat(
            LinearLayout parent,
            String label,
            int value,
            String colorHex) {

        LinearLayout item = new LinearLayout(this);

        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                );

        item.setLayoutParams(params);

        TextView number = new TextView(this);

        number.setText(String.valueOf(value));
        number.setTextSize(21);
        number.setTypeface(null, Typeface.BOLD);
        number.setTextColor(Color.parseColor(colorHex));
        number.setGravity(Gravity.CENTER);

        TextView title = new TextView(this);

        title.setText(label);
        title.setTextSize(10);
        title.setTextColor(Color.rgb(96, 125, 139));
        title.setGravity(Gravity.CENTER);

        item.addView(number);
        item.addView(title);

        parent.addView(item);
    }

    // CREATE AND SAVE PDF

    private void exportReportToPdf() {

        if (lastReportJson == null) {
            showMessage("Please generate a report first.");
            return;
        }

        btnExportPdf.setEnabled(false);

        PdfDocument document = new PdfDocument();

        try {
            JSONObject report = lastReportJson;

            JSONArray students =
                    report.optJSONArray("students");

            if (students == null || students.length() == 0) {
                showMessage("No student records available to export.");
                return;
            }

            int pageWidth = 595;
            int pageHeight = 842;
            int margin = 35;

            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            Paint boldPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            Paint headerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            Paint whitePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

            paint.setColor(Color.rgb(55, 65, 81));
            paint.setTextSize(9);

            boldPaint.setColor(Color.rgb(31, 41, 55));
            boldPaint.setTextSize(9);
            boldPaint.setTypeface(
                    Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            );

            linePaint.setColor(Color.rgb(220, 226, 232));
            linePaint.setStrokeWidth(0.7f);

            headerPaint.setColor(Color.rgb(21, 101, 192));

            whitePaint.setColor(Color.WHITE);
            whitePaint.setTextSize(8);
            whitePaint.setTypeface(
                    Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            );

            titlePaint.setColor(Color.rgb(21, 101, 192));
            titlePaint.setTextSize(17);
            titlePaint.setTypeface(
                    Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            );

            int[] columnWidths = {55, 155, 65, 65, 65, 120};

            String[] headers = {
                    "Roll No",
                    "Student Name",
                    "Total",
                    "Present",
                    "Absent",
                    "Attendance %"
            };

            String className =
                    report.optString("class_name", "-");

            String section =
                    report.optString("section", "-");

            String month =
                    report.optString("month", selectedMonth);

            double classAverage =
                    report.optDouble("class_average", 0);

            int totalStudents =
                    report.optInt("total_students", students.length());

            int pageNumber = 0;
            PdfDocument.Page page = null;
            Canvas canvas = null;
            int y = 0;

            for (int i = 0; i < students.length(); i++) {

                // Start a new page when necessary.
                if (page == null || y > pageHeight - 75) {

                    if (page != null) {
                        document.finishPage(page);
                    }

                    pageNumber++;

                    PdfDocument.PageInfo pageInfo =
                            new PdfDocument.PageInfo.Builder(
                                    pageWidth,
                                    pageHeight,
                                    pageNumber
                            ).create();

                    page = document.startPage(pageInfo);
                    canvas = page.getCanvas();

                    y = 42;

                    canvas.drawText(
                            "MONTHLY ATTENDANCE REPORT",
                            margin,
                            y,
                            titlePaint
                    );

                    y += 24;

                    paint.setTextSize(10);

                    canvas.drawText(
                            "Class: " + className,
                            margin,
                            y,
                            paint
                    );

                    canvas.drawText(
                            "Section: " + section,
                            margin + 150,
                            y,
                            paint
                    );

                    y += 16;

                    canvas.drawText(
                            "Month: " + month,
                            margin,
                            y,
                            paint
                    );

                    canvas.drawText(
                            "Total Students: " + totalStudents,
                            margin + 150,
                            y,
                            paint
                    );

                    y += 16;

                    canvas.drawText(
                            String.format(
                                    Locale.US,
                                    "Class Average: %.2f%%",
                                    classAverage
                            ),
                            margin,
                            y,
                            boldPaint
                    );

                    y += 22;

                    canvas.drawRect(
                            margin,
                            y - 12,
                            pageWidth - margin,
                            y + 9,
                            headerPaint
                    );

                    int x = margin;

                    for (int c = 0; c < headers.length; c++) {
                        canvas.drawText(
                                headers[c],
                                x + 4,
                                y + 2,
                                whitePaint
                        );

                        x += columnWidths[c];
                    }

                    y += 25;
                }

                JSONObject student = students.getJSONObject(i);

                String[] values = {
                        student.optString("roll_number", "-"),
                        student.optString("name", "Unknown Student"),
                        String.valueOf(student.optInt("total_records", 0)),
                        String.valueOf(student.optInt("present_count", 0)),
                        String.valueOf(student.optInt("absent_count", 0)),
                        String.format(
                                Locale.US,
                                "%.1f%%",
                                student.optDouble(
                                        "attendance_percentage",
                                        0
                                )
                        )
                };

                int x = margin;

                for (int c = 0; c < values.length; c++) {

                    String value = values[c];
                    float maxWidth = columnWidths[c] - 8;

                    while (paint.measureText(value) > maxWidth
                            && value.length() > 4) {
                        value = value.substring(
                                0,
                                value.length() - 4
                        ) + "...";
                    }

                    canvas.drawText(
                            value,
                            x + 4,
                            y,
                            paint
                    );

                    x += columnWidths[c];
                }

                canvas.drawLine(
                        margin,
                        y + 7,
                        pageWidth - margin,
                        y + 7,
                        linePaint
                );

                y += 23;
            }

            if (page != null) {
                document.finishPage(page);
            }

            File documentsDir = getExternalFilesDir(
                    Environment.DIRECTORY_DOCUMENTS
            );

            if (documentsDir == null) {
                documentsDir = getFilesDir();
            }

            if (!documentsDir.exists() && !documentsDir.mkdirs()) {
                showMessage("Could not create PDF directory.");
                return;
            }

            String safeMonth = month.replace("-", "_");

            String fileName =
                    "Attendance_Report_"
                            + safeMonth
                            + "_"
                            + System.currentTimeMillis()
                            + ".pdf";

            File pdfFile = new File(documentsDir, fileName);

            try (FileOutputStream output =
                         new FileOutputStream(pdfFile)) {

                document.writeTo(output);
                output.flush();
            }

            lastPdfFile = pdfFile;

            btnSharePdf.setEnabled(true);
            btnPrintPdf.setEnabled(true);

            tvReportMessage.setText(
                    "PDF saved: " + pdfFile.getAbsolutePath()
            );

            Toast.makeText(
                    this,
                    "PDF saved successfully",
                    Toast.LENGTH_LONG
            ).show();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "PDF export failed: " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();

        } finally {

            document.close();

            btnExportPdf.setEnabled(
                    lastReportJson != null
            );
        }
    }

    // SHARE PDF

    private void sharePdf() {

        if (lastPdfFile == null || !lastPdfFile.exists()) {
            Toast.makeText(
                    this,
                    "Please export the PDF first.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        try {
            Uri pdfUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    lastPdfFile
            );

            Intent shareIntent = new Intent(Intent.ACTION_SEND);

            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, pdfUri);
            shareIntent.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );

            startActivity(
                    Intent.createChooser(
                            shareIntent,
                            "Share Attendance Report"
                    )
            );

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Unable to share PDF: " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    // PRINT PDF

    private void printPdf() {

        if (lastPdfFile == null || !lastPdfFile.exists()) {
            Toast.makeText(
                    this,
                    "Please export the PDF first.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        try {
            PrintManager printManager =
                    (PrintManager) getSystemService(PRINT_SERVICE);

            if (printManager == null) {
                Toast.makeText(
                        this,
                        "Print service is unavailable.",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            printManager.print(
                    "Attendance Monthly Report",
                    new PdfFilePrintAdapter(lastPdfFile),
                    new PrintAttributes.Builder()
                            .setMediaSize(
                                    PrintAttributes.MediaSize.ISO_A4
                            )
                            .setColorMode(
                                    PrintAttributes.COLOR_MODE_COLOR
                            )
                            .build()
            );

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Unable to print PDF: " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    // PDF PRINT ADAPTER

    private static class PdfFilePrintAdapter
            extends PrintDocumentAdapter {

        private final File pdfFile;

        PdfFilePrintAdapter(File pdfFile) {
            this.pdfFile = pdfFile;
        }

        @Override
        public void onLayout(
                PrintAttributes oldAttributes,
                PrintAttributes newAttributes,
                CancellationSignal cancellationSignal,
                LayoutResultCallback callback,
                android.os.Bundle extras) {

            if (cancellationSignal.isCanceled()) {
                callback.onLayoutCancelled();
                return;
            }

            PrintDocumentInfo info =
                    new PrintDocumentInfo.Builder(
                            pdfFile.getName()
                    )
                            .setContentType(
                                    PrintDocumentInfo.CONTENT_TYPE_DOCUMENT
                            )
                            .setPageCount(
                                    PrintDocumentInfo.PAGE_COUNT_UNKNOWN
                            )
                            .build();

            callback.onLayoutFinished(info, true);
        }

        @Override
        public void onWrite(
                PageRange[] pages,
                android.os.ParcelFileDescriptor destination,
                CancellationSignal cancellationSignal,
                WriteResultCallback callback) {

            try (FileInputStream input =
                         new FileInputStream(pdfFile);
                 OutputStream output =
                         new FileOutputStream(
                                 destination.getFileDescriptor()
                         )) {

                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = input.read(buffer)) != -1) {

                    if (cancellationSignal.isCanceled()) {
                        callback.onWriteCancelled();
                        return;
                    }

                    output.write(buffer, 0, bytesRead);
                }

                output.flush();

                callback.onWriteFinished(
                        new PageRange[]{
                                PageRange.ALL_PAGES
                        }
                );

            } catch (Exception e) {

                callback.onWriteFailed(e.getMessage());

            } finally {

                try {
                    destination.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    // MESSAGE

    private void showMessage(String message) {

        tvReportMessage.setText(message);
        tvReportMessage.setTextColor(
                Color.rgb(84, 110, 122)
        );

        llReportStudents.removeAllViews();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
