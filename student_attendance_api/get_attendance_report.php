
<?php

header("Content-Type: application/json; charset=UTF-8");

require_once "auth.php";

mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);

function sendJsonResponse(int $statusCode, array $data): void
{
    http_response_code($statusCode);

    echo json_encode($data, JSON_UNESCAPED_UNICODE);

    exit;
}

try {

    // 1. Allow POST requests only.
    if ($_SERVER["REQUEST_METHOD"] !== "POST") {
        header("Allow: POST");

        sendJsonResponse(405, [
            "success" => false,
            "message" => "Only POST method is allowed"
        ]);
    }

    // 2. Authenticate the request.
    $user = requireAuth();

    $userId = (int) ($user["id"] ?? 0);
    $role = strtolower(trim($user["role"] ?? ""));

    if (!in_array($role, ["admin", "teacher"], true)) {
        sendJsonResponse(403, [
            "success" => false,
            "message" => "Permission denied"
        ]);
    }

    // 3. Read and validate request parameters.
    $class_name = isset($_POST["class_name"])
        && is_string($_POST["class_name"])
            ? trim($_POST["class_name"])
            : "";

    $section = isset($_POST["section"])
        && is_string($_POST["section"])
            ? trim($_POST["section"])
            : "";

    $attendance_date = isset($_POST["attendance_date"])
        && is_string($_POST["attendance_date"])
            ? trim($_POST["attendance_date"])
            : "";

    if ($class_name === "") {
        sendJsonResponse(400, [
            "success" => false,
            "message" => "Class is required"
        ]);
    }

    if ($section === "") {
        sendJsonResponse(400, [
            "success" => false,
            "message" => "Section is required"
        ]);
    }

    if ($attendance_date === "") {
        sendJsonResponse(400, [
            "success" => false,
            "message" => "Attendance date is required"
        ]);
    }

    // 4. Validate the calendar date.
    $dateObject = DateTime::createFromFormat(
        "!Y-m-d",
        $attendance_date
    );

    $dateErrors = DateTime::getLastErrors();

    $isValidDate = $dateObject !== false
        && $dateObject->format("Y-m-d") === $attendance_date
        && (
            $dateErrors === false
            || (
                $dateErrors["warning_count"] === 0
                && $dateErrors["error_count"] === 0
            )
        );

    if (!$isValidDate) {
        sendJsonResponse(400, [
            "success" => false,
            "message" => "Invalid date. Use YYYY-MM-DD format."
        ]);
    }

    global $conn;

    // 5. Verify teacher assignment before returning report data.
    // Admin retains access to all classes and sections.
    if ($role === "teacher") {

        $accessSql = "
            SELECT 1
            FROM class_routines
            WHERE teacher_user_id = ?
              AND class_name = ?
              AND section = ?
              AND is_active = 1
            LIMIT 1
        ";

        $accessStmt = $conn->prepare($accessSql);

        $accessStmt->bind_param(
            "iss",
            $userId,
            $class_name,
            $section
        );

        $accessStmt->execute();

        $hasAccess =
            $accessStmt->get_result()->num_rows > 0;

        $accessStmt->close();

        if (!$hasAccess) {
            sendJsonResponse(403, [
                "success" => false,
                "message" =>
                    "You are not assigned to this class and section"
            ]);
        }
    }

    // 6. Generate the attendance report.
// Aggregate attendance per student first, so duplicate records
// do not inflate the student count.

$sql = "
    SELECT
        COUNT(*) AS total_students,

        COALESCE(
            SUM(
                CASE
                    WHEN COALESCE(a.status, '') = 'Present' THEN 1
                    ELSE 0
                END
            ),
            0
        ) AS present_count,

        COALESCE(
            SUM(
                CASE
                    WHEN COALESCE(a.status, '') = 'Absent' THEN 1
                    ELSE 0
                END
            ),
            0
        ) AS absent_count,

        COALESCE(
            SUM(
                CASE
                    WHEN a.status IS NULL THEN 1
                    ELSE 0
                END
            ),
            0
        ) AS not_marked_count

    FROM students AS s

    LEFT JOIN (
        SELECT
            student_id,
            attendance_date,
            CASE
                WHEN SUM(status = 'Present') > 0 THEN 'Present'
                WHEN SUM(status = 'Absent') > 0 THEN 'Absent'
                ELSE NULL
            END AS status
        FROM attendance
        WHERE attendance_date = ?
        GROUP BY student_id, attendance_date
    ) AS a
        ON a.student_id = s.id
        AND a.attendance_date = ?

    WHERE s.class_name = ?
      AND s.section = ?
";

$stmt = $conn->prepare($sql);

$stmt->bind_param(
    "ssss",
    $attendance_date,
    $attendance_date,
    $class_name,
    $section
);

$stmt->execute();

$row = $stmt->get_result()->fetch_assoc();

$total_students = (int) ($row["total_students"] ?? 0);
$present_count = (int) ($row["present_count"] ?? 0);
$absent_count = (int) ($row["absent_count"] ?? 0);
$not_marked_count = (int) ($row["not_marked_count"] ?? 0);
   

    // 7. Preserve the existing attendance-rate calculation.
    $attendance_rate = 0;

    if ($marked_students > 0) {
        $attendance_rate = round(
            ($present_count / $marked_students) * 100,
            1
        );
    }

    $stmt->close();

    // 8. Preserve existing response field names.
    sendJsonResponse(200, [
        "success" => true,
        "message" => "Attendance report generated successfully",
        "class_name" => $class_name,
        "section" => $section,
        "attendance_date" => $attendance_date,
        "total_students" => $total_students,
        "present_count" => $present_count,
        "absent_count" => $absent_count,
        "not_marked_count" => $not_marked_count,
        "marked_students" => $marked_students,
        "attendance_rate" => $attendance_rate
    ]);

} catch (Throwable $e) {

    error_log(
        "get_attendance_report.php error: " . $e->getMessage()
    );

    sendJsonResponse(500, [
        "success" => false,
        "message" => "Failed to generate attendance report"
    ]);

} finally {

    if (isset($conn) && $conn instanceof mysqli) {
        $conn->close();
    }
}
?>
