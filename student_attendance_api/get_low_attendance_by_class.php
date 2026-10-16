
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

if ($_SERVER["REQUEST_METHOD"] !== "POST") {
    header("Allow: POST");

    sendJsonResponse(405, [
        "success" => false,
        "message" => "Only POST requests are allowed"
    ]);
}

try {
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
        throw new RuntimeException("Database connection unavailable");
    }

    // Validate class and section.
    $classInput = isset($_POST["class_name"])
        && is_string($_POST["class_name"])
            ? trim($_POST["class_name"])
            : "";

    $section = isset($_POST["section"])
        && is_string($_POST["section"])
            ? strtoupper(trim($_POST["section"]))
            : "";

    if (
        !ctype_digit($classInput) ||
        (int) $classInput < 6 ||
        (int) $classInput > 12
    ) {
        sendJsonResponse(400, [
            "success" => false,
            "message" => "Select a valid class between 6 and 12."
        ]);
    }

    if (!in_array($section, ["A", "B", "C"], true)) {
        sendJsonResponse(400, [
            "success" => false,
            "message" => "Select a valid section: A, B or C."
        ]);
    }

    $classNumber = (int) $classInput;
    $numericClass = (string) $classNumber;
    $prefixedClass = "Class " . $classNumber;
    $sectionLabel = "Section " . $section;

    // Teachers can access only assigned classes and sections.
    if ($role === "teacher") {
        $accessSql = "
            SELECT 1
            FROM class_routines
            WHERE teacher_user_id = ?
              AND TRIM(class_name) IN (?, ?)
              AND UPPER(TRIM(section)) IN (?, ?)
              AND is_active = 1
            LIMIT 1
        ";

        $accessStmt = $conn->prepare($accessSql);

        $accessStmt->bind_param(
            "issss",
            $userId,
            $numericClass,
            $prefixedClass,
            $section,
            $sectionLabel
        );

        $accessStmt->execute();

        $hasAccess = $accessStmt->get_result()->num_rows > 0;
        $accessStmt->close();

        if (!$hasAccess) {
            sendJsonResponse(403, [
                "success" => false,
                "message" =>
                    "You are not assigned to this class and section"
            ]);
        }
    }

    /*
     * First aggregate attendance by student and date.
     * If duplicate records exist for one day, Present takes priority.
     */
    $sql = "
        SELECT
            s.id,
            s.student_id,
            s.name,
            s.roll_number,
            s.class_name,
            s.section,
            s.department,
            s.semester,
            s.photo,

            COUNT(d.attendance_date) AS total_classes,

            COALESCE(
                SUM(
                    CASE
                        WHEN d.daily_status = 'Present' THEN 1
                        ELSE 0
                    END
                ),
                0
            ) AS present_classes,

            CASE
                WHEN COUNT(d.attendance_date) = 0 THEN 0
                ELSE ROUND(
                    SUM(
                        CASE
                            WHEN d.daily_status = 'Present' THEN 1
                            ELSE 0
                        END
                    ) * 100.0 / COUNT(d.attendance_date),
                    2
                )
            END AS attendance_percentage

        FROM students AS s

        LEFT JOIN (
            SELECT
                student_id,
                attendance_date,
                CASE
                    WHEN SUM(status = 'Present') > 0
                        THEN 'Present'
                    ELSE 'Absent'
                END AS daily_status
            FROM attendance
            GROUP BY student_id, attendance_date
        ) AS d
            ON d.student_id = s.id

        WHERE TRIM(s.class_name) IN (?, ?)
          AND UPPER(TRIM(s.section)) = ?

        GROUP BY
            s.id,
            s.student_id,
            s.name,
            s.roll_number,
            s.class_name,
            s.section,
            s.department,
            s.semester,
            s.photo

        HAVING COUNT(d.attendance_date) = 0
            OR attendance_percentage < 75

        ORDER BY
            attendance_percentage ASC,
            s.roll_number ASC
    ";

    $stmt = $conn->prepare($sql);

    $stmt->bind_param(
        "sss",
        $numericClass,
        $prefixedClass,
        $section
    );

    $stmt->execute();

    $result = $stmt->get_result();
    $students = [];

    while ($row = $result->fetch_assoc()) {
        $totalClasses = (int) $row["total_classes"];
        $presentClasses = (int) $row["present_classes"];

        $row["id"] = (int) $row["id"];
        $row["total_classes"] = $totalClasses;
        $row["present_classes"] = $presentClasses;

        $row["attendance_percentage"] =
            (float) $row["attendance_percentage"];

        $row["attendance_status"] = $totalClasses === 0
            ? "No records"
            : "Below 75%";

        $students[] = $row;
    }

    $stmt->close();

    sendJsonResponse(200, [
        "success" => true,
        "class_name" => $numericClass,
        "section" => $section,
        "threshold" => 75,
        "count" => count($students),
        "students" => $students
    ]);

} catch (Throwable $e) {
    error_log(
        "Low attendance by class API error: " .
        $e->getMessage()
    );

    sendJsonResponse(500, [
        "success" => false,
        "message" => "Unable to load low attendance students."
    ]);

} finally {
    if (isset($conn) && $conn instanceof mysqli) {
        $conn->close();
    }
}
?>
