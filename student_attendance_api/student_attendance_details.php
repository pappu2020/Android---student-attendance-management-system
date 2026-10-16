
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

// 1. POST requests only.
if ($_SERVER["REQUEST_METHOD"] !== "POST") {
    header("Allow: POST");

    sendJsonResponse(405, [
        "success" => false,
        "message" => "Only POST method is allowed"
    ]);
}

try {
    // 2. Authenticate the user.
    $user = requireAuth();

    $userId = (int) ($user["id"] ?? 0);
    $role = strtolower(trim($user["role"] ?? ""));

    if (!in_array($role, ["admin", "teacher"], true)) {
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

    // 3. Validate student ID.
    $studentIdInput = isset($_POST["student_id"])
        && is_string($_POST["student_id"])
            ? trim($_POST["student_id"])
            : "";

    if (
        $studentIdInput === "" ||
        !ctype_digit($studentIdInput) ||
        (int) $studentIdInput < 1
    ) {
        sendJsonResponse(400, [
            "success" => false,
            "message" => "Valid student ID is required"
        ]);
    }

    $studentId = (int) $studentIdInput;

    // 4. Fetch student details.
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

    $student = $studentStmt
        ->get_result()
        ->fetch_assoc();

    $studentStmt->close();

    if (!$student) {
        sendJsonResponse(404, [
            "success" => false,
            "message" => "Student not found"
        ]);
    }

    // 5. Verify teacher assignment.
    if ($role === "teacher") {
        $className = trim((string) $student["class_name"]);
        $section = trim((string) $student["section"]);

        $accessSql = "
            SELECT 1
            FROM class_routines AS cr
            WHERE cr.teacher_user_id = ?
              AND cr.is_active = 1

              AND (
                    TRIM(cr.class_name) = ?
                    OR TRIM(cr.class_name) =
                        CONCAT('Class ', ?)
                    OR CONCAT(
                        'Class ',
                        TRIM(cr.class_name)
                    ) = ?
              )

              AND (
                    UPPER(TRIM(cr.section)) = UPPER(?)
                    OR UPPER(TRIM(cr.section)) =
                        CONCAT('SECTION ', UPPER(?))
                    OR CONCAT(
                        'SECTION ',
                        UPPER(TRIM(cr.section))
                    ) = UPPER(?)
              )

            LIMIT 1
        ";

        $accessStmt = $conn->prepare($accessSql);

        $accessStmt->bind_param(
            "issssss",
            $userId,
            $className,
            $className,
            $className,
            $section,
            $section,
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
                    "You are not assigned to this student's class and section"
            ]);
        }
    }

    // 6. Aggregate duplicate attendance records by date.
    // Present takes priority if duplicate records conflict.
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

    // 7. Calculate attendance percentage.
    $attendancePercentage = $totalRecords > 0
        ? round(
            ($presentCount * 100.0) / $totalRecords,
            2
        )
        : 0;

    // 8. Preserve the original JSON response structure.
    sendJsonResponse(200, [
        "success" => true,
        "message" =>
            "Student attendance details fetched successfully",

        "student" => [
            "id" => $student["id"],
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
            "attendance_percentage" => $attendancePercentage
        ],

        "attendance" => $attendance
    ]);

} catch (Throwable $e) {
    error_log(
        "student_attendance_details.php error: " .
        $e->getMessage()
    );

    sendJsonResponse(500, [
        "success" => false,
        "message" =>
            "Failed to fetch student attendance details"
    ]);

} finally {
    if (isset($conn) && $conn instanceof mysqli) {
        $conn->close();
    }
}
?>
