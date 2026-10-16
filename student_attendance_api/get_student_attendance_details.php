<?php

header("Content-Type: application/json; charset=UTF-8");

require_once "auth.php";

mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);

function sendJsonResponse(int $statusCode, array $data): void
{
    http_response_code($statusCode);

    echo json_encode(
        $data,
        JSON_UNESCAPED_UNICODE
    );

    exit;
}

$studentStmt = null;
$accessStmt = null;
$attendanceStmt = null;
$currentStep = "initialization";

try {

    // 1. Allow POST requests only.
    if ($_SERVER["REQUEST_METHOD"] !== "POST") {
        header("Allow: POST");

        sendJsonResponse(405, [
            "success" => false,
            "message" => "Only POST method is allowed"
        ]);
    }

    // 2. Authenticate the user.
    $currentStep = "authentication";

    $user = requireAuth();

    $userId = (int) ($user["id"] ?? 0);
    $role = strtolower(trim($user["role"] ?? ""));

    if (!in_array($role, ["admin", "teacher"], true)) {
        sendJsonResponse(403, [
            "success" => false,
            "message" =>
                "You do not have permission to view student details"
        ]);
    }

    global $conn;

    if (!isset($conn) || !($conn instanceof mysqli)) {
        throw new RuntimeException(
            "Database connection unavailable"
        );
    }

    // 3. Validate student ID.
    $currentStep = "student_id_validation";

    $studentId = filter_var(
        $_POST["student_id"] ?? null,
        FILTER_VALIDATE_INT,
        ["options" => ["min_range" => 1]]
    );

    if ($studentId === false || $studentId === null) {
        sendJsonResponse(400, [
            "success" => false,
            "message" => "Valid student ID is required"
        ]);
    }

    // 4. Fetch the student.
    $currentStep = "fetch_student";

    $studentSql = "
        SELECT
            id,
            name,
            roll_number,
            class_name,
            section,
            email,
            phone,
            photo
        FROM students
        WHERE id = ?
        LIMIT 1
    ";

    $studentStmt = $conn->prepare($studentSql);

    $studentStmt->bind_param("i", $studentId);
    $studentStmt->execute();

    $studentResult = $studentStmt->get_result();
    $student = $studentResult->fetch_assoc();

    $studentStmt->close();
    $studentStmt = null;

    if (!$student) {
        sendJsonResponse(404, [
            "success" => false,
            "message" => "Student not found"
        ]);
    }

    // 5. Restrict teachers to their assigned classes/sections.
    if ($role === "teacher") {

        $currentStep = "teacher_access_check";

        $accessSql = "
            SELECT 1
            FROM class_routines AS cr
            WHERE cr.teacher_user_id = ?
              AND cr.is_active = 1
              AND TRIM(cr.class_name) = TRIM(?)
              AND UPPER(TRIM(cr.section)) = UPPER(TRIM(?))
            LIMIT 1
        ";

        $className = (string) $student["class_name"];
        $section = (string) $student["section"];

        $accessStmt = $conn->prepare($accessSql);

        $accessStmt->bind_param(
            "iss",
            $userId,
            $className,
            $section
        );

        $accessStmt->execute();

        $hasAccess =
            $accessStmt->get_result()->num_rows > 0;

        $accessStmt->close();
        $accessStmt = null;

        if (!$hasAccess) {
            sendJsonResponse(403, [
                "success" => false,
                "message" =>
                    "You are not assigned to this student's class and section"
            ]);
        }
    }

    // 6. Fetch attendance records.
    $currentStep = "fetch_attendance";

    $attendanceSql = "
        SELECT
            attendance_date,
            CASE
                WHEN SUM(status = 'Present') > 0
                    THEN 'Present'
                ELSE 'Absent'
            END AS status
        FROM attendance
        WHERE student_id = ?
        GROUP BY attendance_date
        ORDER BY attendance_date DESC
    ";

    $attendanceStmt = $conn->prepare($attendanceSql);

    $attendanceStmt->bind_param("i", $studentId);
    $attendanceStmt->execute();

    $attendanceResult = $attendanceStmt->get_result();

    $attendance = [];
    $totalRecords = 0;
    $presentCount = 0;
    $absentCount = 0;

    while ($row = $attendanceResult->fetch_assoc()) {

        $status = $row["status"];

        if ($status === "Present") {
            $presentCount++;
        } else {
            $absentCount++;
        }

        $totalRecords++;

        $attendance[] = [
            "attendance_date" => $row["attendance_date"],
            "status" => $status
        ];
    }

    $attendanceStmt->close();
    $attendanceStmt = null;

    // 7. Calculate attendance percentage.
    $attendancePercentage = $totalRecords > 0
        ? ($presentCount * 100.0) / $totalRecords
        : 0;

    // 8. Return Android-compatible JSON.
    sendJsonResponse(200, [
        "success" => true,
        "message" =>
            "Student attendance details fetched successfully",

        "student" => [
            "id" => (int) $student["id"],
            "name" => $student["name"],
            "roll_number" => $student["roll_number"],
            "class_name" => $student["class_name"],
            "section" => $student["section"],
            "email" => $student["email"],
            "phone" => $student["phone"],
            "photo" => $student["photo"]
        ],

        "summary" => [
            "total_records" => $totalRecords,
            "present_count" => $presentCount,
            "absent_count" => $absentCount,
            "attendance_percentage" => round(
                $attendancePercentage,
                2
            )
        ],

        "attendance" => $attendance
    ]);

} catch (Throwable $e) {

    // Keep detailed error information in the server log.
    error_log(
        "get_student_attendance_details.php"
        . " | Step: " . $currentStep
        . " | Error: " . $e->getMessage()
        . " | File: " . $e->getFile()
        . " | Line: " . $e->getLine()
    );

    sendJsonResponse(500, [
        "success" => false,
        "message" =>
            "Unable to fetch student attendance details",
        "error_code" => "STUDENT_DETAILS_ERROR",
        "step" => $currentStep
    ]);

} finally {

    if ($studentStmt instanceof mysqli_stmt) {
        $studentStmt->close();
    }

    if ($accessStmt instanceof mysqli_stmt) {
        $accessStmt->close();
    }

    if ($attendanceStmt instanceof mysqli_stmt) {
        $attendanceStmt->close();
    }

    if (isset($conn) && $conn instanceof mysqli) {
        $conn->close();
    }
}
?>