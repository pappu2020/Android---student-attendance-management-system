
<?php

header("Content-Type: application/json; charset=UTF-8");

require_once "auth.php";

mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);

function sendMonthlyReportResponse(int $status, array $data): void
{
    http_response_code($status);

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

        sendMonthlyReportResponse(405, [
            "success" => false,
            "message" => "Only POST method is allowed"
        ]);
    }

    // 2. Authenticate the request.
    $user = requireAuth();

    $userId = (int) ($user["id"] ?? 0);
    $role = strtolower(trim($user["role"] ?? ""));

    if (!in_array($role, ["admin", "teacher"], true)) {
        sendMonthlyReportResponse(403, [
            "success" => false,
            "message" => "Admin or Teacher permission required"
        ]);
    }

    global $conn;

    // 3. Read parameters safely.
    $className = (
        isset($_POST["class_name"])
        && is_string($_POST["class_name"])
    ) ? trim($_POST["class_name"]) : "";

    $sectionName = (
        isset($_POST["section"])
        && is_string($_POST["section"])
    ) ? trim($_POST["section"]) : "";

    $month = (
        isset($_POST["month"])
        && is_string($_POST["month"])
    ) ? trim($_POST["month"]) : "";

    // 4. Validate required fields.
    if ($className === "" || $sectionName === "") {
        sendMonthlyReportResponse(400, [
            "success" => false,
            "message" => "Class and section are required"
        ]);
    }

    if (
        strlen($className) > 100 ||
        strlen($sectionName) > 50
    ) {
        sendMonthlyReportResponse(400, [
            "success" => false,
            "message" => "Invalid class or section value"
        ]);
    }

    // 5. Validate the month strictly as YYYY-MM.
    if (!preg_match('/^\d{4}-(0[1-9]|1[0-2])$/', $month)) {
        sendMonthlyReportResponse(400, [
            "success" => false,
            "message" => "Valid month is required in YYYY-MM format"
        ]);
    }

    $monthDate = DateTime::createFromFormat("!Y-m", $month);
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
        sendMonthlyReportResponse(400, [
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
            sendMonthlyReportResponse(403, [
                "success" => false,
                "message" =>
                    "You are not assigned to this class and section"
            ]);
        }
    }

    // 7. Aggregate attendance by student and date first.
    // This prevents duplicate records from inflating daily counts.
    $sql = "
        SELECT
            s.id,
            s.name,
            s.roll_number,
            s.class_name,
            s.section,
            s.photo,

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

            WHERE attendance_date >= CONCAT(?, '-01')
              AND attendance_date < DATE_ADD(
                  CONCAT(?, '-01'),
                  INTERVAL 1 MONTH
              )

            GROUP BY student_id, attendance_date
        ) AS a
            ON a.student_id = s.id

        WHERE s.class_name = ?
          AND s.section = ?

        GROUP BY
            s.id,
            s.name,
            s.roll_number,
            s.class_name,
            s.section,
            s.photo

        ORDER BY
            CAST(s.roll_number AS UNSIGNED) ASC,
            s.roll_number ASC
    ";

    $stmt = $conn->prepare($sql);

    $stmt->bind_param(
        "ssss",
        $month,
        $month,
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
    $totalRecords = 0;

    while ($row = $result->fetch_assoc()) {

        $records = (int) $row["total_records"];
        $present = (int) $row["present_count"];
        $absent = (int) $row["absent_count"];

        $percentage = $records > 0
            ? ($present * 100.0) / $records
            : 0;

        $totalStudents++;
        $totalPresent += $present;
        $totalAbsent += $absent;
        $totalRecords += $records;

        $students[] = [
            "id" => (int) $row["id"],
            "name" => $row["name"],
            "roll_number" => $row["roll_number"],
            "class_name" => $row["class_name"],
            "section" => $row["section"],
            "photo" => $row["photo"],
            "total_records" => $records,
            "present_count" => $present,
            "absent_count" => $absent,
            "attendance_percentage" => round($percentage, 2)
        ];
    }

    $stmt->close();

    // 9. Calculate overall attendance percentage.
    $classAverage = $totalRecords > 0
        ? ($totalPresent * 100.0) / $totalRecords
        : 0;

    // 10. Preserve the original JSON response fields.
    sendMonthlyReportResponse(200, [
        "success" => true,
        "message" => "Class monthly report generated successfully",
        "class_name" => $className,
        "section" => $sectionName,
        "month" => $month,
        "total_students" => $totalStudents,
        "total_present" => $totalPresent,
        "total_absent" => $totalAbsent,
        "total_records" => $totalRecords,
        "class_average" => round($classAverage, 2),
        "students" => $students
    ]);

} catch (Throwable $e) {

    error_log(
        "get_class_monthly_report.php error: " . $e->getMessage()
    );

    sendMonthlyReportResponse(500, [
        "success" => false,
        "message" => "Failed to generate class monthly report"
    ]);

} finally {

    if (isset($conn) && $conn instanceof mysqli) {
        $conn->close();
    }
}
?>
