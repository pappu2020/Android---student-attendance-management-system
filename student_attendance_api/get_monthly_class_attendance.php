
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
            "message" => "Only POST requests are allowed"
        ]);
    }

    // 2. Authenticate and authorize the user.
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

    // 3. Read POST parameters safely.
    $month = (
        isset($_POST["month"]) &&
        is_string($_POST["month"])
    ) ? trim($_POST["month"]) : "";

    $className = (
        isset($_POST["class"]) &&
        is_string($_POST["class"])
    ) ? trim($_POST["class"]) : "";

    $sectionName = (
        isset($_POST["section"]) &&
        is_string($_POST["section"])
    ) ? trim($_POST["section"]) : "";

    // 4. Normalize class and section.
    if (
        $className !== "" &&
        stripos($className, "Class ") !== 0
    ) {
        $className = "Class " . $className;
    }

    if (
        $sectionName !== "" &&
        stripos($sectionName, "Section ") !== 0
    ) {
        $sectionName = "Section " . $sectionName;
    }

    // 5. Validate required fields.
    if (
        $className === "" ||
        $className === "Class " ||
        $sectionName === "" ||
        $sectionName === "Section " ||
        strlen($className) > 100 ||
        strlen($sectionName) > 50
    ) {
        sendJsonResponse(400, [
            "success" => false,
            "message" => "Please provide a valid class and section"
        ]);
    }

    // Validate YYYY-MM format.
    if (!preg_match('/^\d{4}-(0[1-9]|1[0-2])$/', $month)) {
        sendJsonResponse(400, [
            "success" => false,
            "message" =>
                "Valid month is required in YYYY-MM format"
        ]);
    }

    // Validate the actual calendar month.
    $monthDate = DateTime::createFromFormat(
        "!Y-m",
        $month
    );

    $dateErrors = DateTime::getLastErrors();

    if (
        $monthDate === false ||
        (
            $dateErrors !== false &&
            (
                $dateErrors["warning_count"] > 0 ||
                $dateErrors["error_count"] > 0
            )
        ) ||
        $monthDate->format("Y-m") !== $month
    ) {
        sendJsonResponse(400, [
            "success" => false,
            "message" => "Invalid calendar month"
        ]);
    }

    // 6. Restrict teachers to assigned classes and sections.
    // Admin can access all classes and sections.
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
            $className,
            $sectionName
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

    // 7. Aggregate attendance per student per day.
    // Duplicate records for the same date will count as one day.
    // Present is given priority if conflicting records exist.
    $sql = "
        SELECT
            s.id,
            s.student_id,
            s.name,

            COUNT(a.attendance_date) AS total_records,

            COALESCE(
                SUM(
                    CASE
                        WHEN a.status = 'Present' THEN 1
                        ELSE 0
                    END
                ),
                0
            ) AS present_count,

            COALESCE(
                SUM(
                    CASE
                        WHEN a.status = 'Absent' THEN 1
                        ELSE 0
                    END
                ),
                0
            ) AS absent_count

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

            WHERE attendance_date >= ?
              AND attendance_date < DATE_ADD(
                  ?,
                  INTERVAL 1 MONTH
              )

            GROUP BY
                student_id,
                attendance_date
        ) AS a
            ON a.student_id = s.id

        WHERE s.class_name = ?
          AND s.section = ?

        GROUP BY
            s.id,
            s.student_id,
            s.name

        ORDER BY s.name ASC
    ";

    $startDate = $month . "-01";

    $stmt = $conn->prepare($sql);

    $stmt->bind_param(
        "ssss",
        $startDate,
        $startDate,
        $className,
        $sectionName
    );

    $stmt->execute();

    $result = $stmt->get_result();

    // 8. Calculate student and class summaries.
    $students = [];

    $totalStudents = 0;
    $totalPresent = 0;
    $totalAbsent = 0;

    while ($row = $result->fetch_assoc()) {

        $present = (int) $row["present_count"];
        $absent = (int) $row["absent_count"];

        $records = $present + $absent;

        $percentage = $records > 0
            ? ($present / $records) * 100
            : 0;

        $students[] = [
            "student_id" => $row["student_id"],
            "student_name" => $row["name"],
            "present_count" => $present,
            "absent_count" => $absent,
            "attendance_percentage" => round(
                $percentage,
                2
            )
        ];

        $totalStudents++;
        $totalPresent += $present;
        $totalAbsent += $absent;
    }

    $stmt->close();

    // 9. Calculate overall attendance percentage.
    $totalRecords = $totalPresent + $totalAbsent;

    $overallPercentage = $totalRecords > 0
        ? ($totalPresent / $totalRecords) * 100
        : 0;

    // 10. Preserve the existing JSON response fields.
    sendJsonResponse(200, [
        "success" => true,
        "month" => $month,
        "class" => $className,
        "section" => $sectionName,
        "total_students" => $totalStudents,
        "total_present_records" => $totalPresent,
        "total_absent_records" => $totalAbsent,
        "overall_attendance_percentage" => round(
            $overallPercentage,
            2
        ),
        "students" => $students
    ]);

} catch (Throwable $e) {

    error_log(
        "get_monthly_class_attendance.php error: " .
        $e->getMessage()
    );

    sendJsonResponse(500, [
        "success" => false,
        "message" =>
            "Failed to fetch monthly class attendance"
    ]);

} finally {

    if (isset($conn) && $conn instanceof mysqli) {
        $conn->close();
    }
}
?>
