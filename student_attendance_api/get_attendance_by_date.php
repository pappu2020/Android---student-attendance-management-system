
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
    $user = requireAuth();

    $userId = (int)($user["id"] ?? 0);
    $role = strtolower(trim((string)($user["role"] ?? "")));

    if (
        $userId < 1 ||
        !in_array($role, ["admin", "teacher"], true)
    ) {
        sendJsonResponse(403, [
            "success" => false,
            "message" => "Admin or Teacher permission required"
        ]);
    }

    global $conn;

    if (!isset($conn) || !($conn instanceof mysqli)) {
        throw new RuntimeException(
            "Database connection unavailable"
        );
    }

    // 3. Validate the attendance date.
    $attendanceDate = isset($_POST["attendance_date"])
        && is_string($_POST["attendance_date"])
            ? trim($_POST["attendance_date"])
            : "";

    $dateObject = DateTime::createFromFormat(
        "!Y-m-d",
        $attendanceDate
    );

    $dateErrors = DateTime::getLastErrors();

    $isValidDate =
        $dateObject !== false &&
        $dateObject->format("Y-m-d") === $attendanceDate &&
        (
            $dateErrors === false ||
            (
                $dateErrors["warning_count"] === 0 &&
                $dateErrors["error_count"] === 0
            )
        );

    if (!$isValidDate) {
        sendJsonResponse(400, [
            "success" => false,
            "message" => "Invalid date. Use YYYY-MM-DD format."
        ]);
    }

    /*
     * 4. Define teacher class/section access.
     * Supports:
     * Class 6 / 6
     * Section A / A
     */
    $accessCondition = "
        EXISTS (
            SELECT 1
            FROM class_routines AS cr
            WHERE cr.teacher_user_id = ?
              AND cr.is_active = 1
              AND (
                    TRIM(cr.class_name) = TRIM(s.class_name)
                    OR TRIM(cr.class_name) =
                        CONCAT('Class ', TRIM(s.class_name))
                    OR CONCAT('Class ', TRIM(cr.class_name)) =
                        TRIM(s.class_name)
              )
              AND (
                    UPPER(TRIM(cr.section)) =
                        UPPER(TRIM(s.section))
                    OR UPPER(TRIM(cr.section)) =
                        CONCAT(
                            'SECTION ',
                            UPPER(TRIM(s.section))
                        )
                    OR CONCAT(
                        'SECTION ',
                        UPPER(TRIM(cr.section))
                    ) = UPPER(TRIM(s.section))
              )
        )
    ";

    // 5. Count students visible to this user.
    if ($role === "admin") {
        $totalSql = "
            SELECT COUNT(*) AS total_students
            FROM students
        ";

        $totalResult = $conn->query($totalSql);
        $totalRow = $totalResult->fetch_assoc();

    } else {
        $totalSql = "
            SELECT COUNT(*) AS total_students
            FROM students AS s
            WHERE $accessCondition
        ";

        $totalStmt = $conn->prepare($totalSql);
        $totalStmt->bind_param("i", $userId);
        $totalStmt->execute();

        $totalResult = $totalStmt->get_result();
        $totalRow = $totalResult->fetch_assoc();

        $totalStmt->close();
    }

    $totalStudents = (int)($totalRow["total_students"] ?? 0);

    /*
     * 6. Aggregate attendance once per student per date.
     *
     * If duplicate records exist for a student on that date,
     * Present takes precedence for the daily summary.
     */
    $dailyAttendanceSql = "
        SELECT
            student_id,
            CASE
                WHEN SUM(status = 'Present') > 0
                    THEN 'Present'
                ELSE 'Absent'
            END AS daily_status
        FROM attendance
        WHERE attendance_date = ?
        GROUP BY student_id
    ";

    if ($role === "admin") {
        // Admin can see attendance across all students.
        $attendanceSql = "
            SELECT
                COUNT(*) AS marked_count,
                COALESCE(
                    SUM(d.daily_status = 'Present'),
                    0
                ) AS present_count,
                COALESCE(
                    SUM(d.daily_status = 'Absent'),
                    0
                ) AS absent_count
            FROM (
                $dailyAttendanceSql
            ) AS d
            INNER JOIN students AS s
                ON s.id = d.student_id
        ";

        $attendanceStmt = $conn->prepare($attendanceSql);
        $attendanceStmt->bind_param("s", $attendanceDate);

    } else {
        // Teachers can see only students in their assigned classes.
        $attendanceSql = "
            SELECT
                COUNT(*) AS marked_count,
                COALESCE(
                    SUM(d.daily_status = 'Present'),
                    0
                ) AS present_count,
                COALESCE(
                    SUM(d.daily_status = 'Absent'),
                    0
                ) AS absent_count
            FROM (
                $dailyAttendanceSql
            ) AS d
            INNER JOIN students AS s
                ON s.id = d.student_id
            WHERE $accessCondition
        ";

        $attendanceStmt = $conn->prepare($attendanceSql);
        $attendanceStmt->bind_param(
            "si",
            $attendanceDate,
            $userId
        );
    }

    $attendanceStmt->execute();

    $attendanceResult = $attendanceStmt->get_result();
    $row = $attendanceResult->fetch_assoc();

    $attendanceStmt->close();

    $markedCount = (int)($row["marked_count"] ?? 0);
    $presentCount = (int)($row["present_count"] ?? 0);
    $absentCount = (int)($row["absent_count"] ?? 0);

    // 7. Calculate unmarked students and attendance percentage.
    $notMarkedCount = max(
        0,
        $totalStudents - $markedCount
    );

    $attendancePercentage = $markedCount > 0
        ? round(($presentCount / $markedCount) * 100, 2)
        : 0.0;

    // 8. Return the existing Android-compatible response keys.
    sendJsonResponse(200, [
        "success" => true,
        "message" => "Attendance fetched successfully",
        "date" => $attendanceDate,
        "total_students" => $totalStudents,
        "present_count" => $presentCount,
        "absent_count" => $absentCount,
        "marked_count" => $markedCount,
        "not_marked_count" => $notMarkedCount,
        "attendance_percentage" => $attendancePercentage
    ]);

} catch (Throwable $e) {
    error_log(
        "get_attendance_by_date.php error: " .
        $e->getMessage()
    );

    sendJsonResponse(500, [
        "success" => false,
        "message" => "Unable to fetch attendance summary"
    ]);

} finally {
    if (isset($conn) && $conn instanceof mysqli) {
        $conn->close();
    }
}
?>
