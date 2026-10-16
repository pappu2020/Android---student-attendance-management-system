
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

    $userId = (int) ($user["id"] ?? 0);
    $role = strtolower(trim($user["role"] ?? ""));

    if (!in_array($role, ["admin", "teacher"], true)) {
        sendJsonResponse(403, [
            "success" => false,
            "message" =>
                "You do not have permission to view attendance history"
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

    // 4. Validate required fields.
    if (
        $class_name === "" ||
        $section === "" ||
        $attendance_date === ""
    ) {
        sendJsonResponse(400, [
            "success" => false,
            "message" =>
                "Class, section and attendance date are required"
        ]);
    }

    // 5. Validate the date strictly as YYYY-MM-DD.
    $dateObject = DateTime::createFromFormat(
        "!Y-m-d",
        $attendance_date
    );

    $dateErrors = DateTime::getLastErrors();

    $isValidDate = (
        $dateObject !== false
        && $dateObject->format("Y-m-d") === $attendance_date
        && (
            $dateErrors === false
            || (
                $dateErrors["warning_count"] === 0
                && $dateErrors["error_count"] === 0
            )
        )
    );

    if (!$isValidDate) {
        sendJsonResponse(400, [
            "success" => false,
            "message" =>
                "Invalid attendance date. Use YYYY-MM-DD"
        ]);
    }

    global $conn;

    // 6. Restrict teachers to assigned classes and sections.
    // Admins can access all classes and sections.
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

    // 7. Fetch attendance history.
    // Aggregate attendance records to prevent duplicate students.
    // Students without attendance records remain in the result.

    $sql = "
        SELECT
            s.id,
            s.name,
            s.roll_number,
            s.class_name,
            s.section,
            s.email,
            s.phone,
            s.photo,
            a.attendance_date,
            COALESCE(a.status, 'Not Marked') AS status

        FROM students AS s

        LEFT JOIN (
            SELECT
                student_id,
                attendance_date,

                CASE
                    WHEN SUM(status = 'Present') > 0
                        THEN 'Present'
                    WHEN SUM(status = 'Absent') > 0
                        THEN 'Absent'
                    ELSE NULL
                END AS status

            FROM attendance

            WHERE attendance_date = ?

            GROUP BY
                student_id,
                attendance_date
        ) AS a
            ON a.student_id = s.id
            AND a.attendance_date = ?

        WHERE s.class_name = ?
          AND s.section = ?

        ORDER BY
            CAST(s.roll_number AS UNSIGNED) ASC,
            s.roll_number ASC
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

    $result = $stmt->get_result();

    $students = [];
    $total_students = 0;
    $present_count = 0;
    $absent_count = 0;

    while ($row = $result->fetch_assoc()) {

        $status = $row["status"];

        if ($status === "Present") {
            $present_count++;
        } elseif ($status === "Absent") {
            $absent_count++;
        }

        $total_students++;

        $students[] = [
            "id" => $row["id"],
            "name" => $row["name"],
            "roll_number" => $row["roll_number"],
            "class_name" => $row["class_name"],
            "section" => $row["section"],
            "email" => $row["email"],
            "phone" => $row["phone"],
            "photo" => $row["photo"],
            "attendance_date" => $row["attendance_date"],
            "status" => $status
        ];
    }

    $stmt->close();

    // 8. Return the existing JSON response structure.
    sendJsonResponse(200, [
        "success" => true,
        "message" => "Attendance history fetched successfully",
        "class_name" => $class_name,
        "section" => $section,
        "attendance_date" => $attendance_date,
        "total_students" => $total_students,
        "present_count" => $present_count,
        "absent_count" => $absent_count,
        "not_marked_count" =>
            $total_students - $present_count - $absent_count,
        "students" => $students
    ]);

} catch (Throwable $e) {

    error_log(
        "get_attendance_history.php error: " . $e->getMessage()
    );

    sendJsonResponse(500, [
        "success" => false,
        "message" => "Unable to fetch attendance history"
    ]);

} finally {

    if (isset($conn) && $conn instanceof mysqli) {
        $conn->close();
    }
}
?>
