
<?php

header("Content-Type: application/json; charset=UTF-8");

require_once "db.php";

mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);

try {
    if ($_SERVER["REQUEST_METHOD"] !== "POST") {
        http_response_code(405);
        echo json_encode([
            "success" => false,
            "message" => "Only POST requests are allowed"
        ]);
        exit;
    }

    $month = trim($_POST["month"] ?? "");
    
$className = trim($_POST["class"] ?? "");
$sectionName = trim($_POST["section"] ?? "");

// Convert "10" to "Class 10"
if (
    $className !== "" &&
    stripos($className, "Class ") !== 0
) {
    $className = "Class " . $className;
}

// Convert "A" to "Section A"
if (
    $sectionName !== "" &&
    stripos($sectionName, "Section ") !== 0
) {
    $sectionName = "Section " . $sectionName;
}


    if (
        !preg_match('/^\d{4}-(0[1-9]|1[0-2])$/', $month) ||
        $className === "" ||
        $sectionName === ""
    ) {
        http_response_code(400);
        echo json_encode([
            "success" => false,
            "message" => "Please provide a valid month, class and section"
        ]);
        exit;
    }

    $startDate = $month . "-01";
    $endDate = date(
        "Y-m-d",
        strtotime($startDate . " +1 month")
    );

    // Get all students in the selected class and section.
    // Join attendance.student_id to students.id.
    $sql = "
        SELECT
            s.id,
            s.student_id,
            s.name,
            COUNT(a.id) AS total_records,
            COALESCE(
                SUM(CASE WHEN a.status = 'Present' THEN 1 ELSE 0 END),
                0
            ) AS present_count,
            COALESCE(
                SUM(CASE WHEN a.status = 'Absent' THEN 1 ELSE 0 END),
                0
            ) AS absent_count
        FROM students s
        LEFT JOIN attendance a
            ON a.student_id = s.id
            AND a.attendance_date >= ?
            AND a.attendance_date < ?
        WHERE s.class_name = ?
            AND s.section = ?
        GROUP BY s.id, s.student_id, s.name
        ORDER BY s.name ASC
    ";

    $stmt = $conn->prepare($sql);
    $stmt->bind_param(
        "ssss",
        $startDate,
        $endDate,
        $className,
        $sectionName
    );

    $stmt->execute();
    $result = $stmt->get_result();

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
            "attendance_percentage" => round($percentage, 2)
        ];

        $totalStudents++;
        $totalPresent += $present;
        $totalAbsent += $absent;
    }

    $stmt->close();

    $totalRecords = $totalPresent + $totalAbsent;

    $overallPercentage = $totalRecords > 0
        ? ($totalPresent / $totalRecords) * 100
        : 0;

    echo json_encode([
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
    ], JSON_UNESCAPED_UNICODE);

} catch (Throwable $e) {
    http_response_code(500);

    // Temporary debugging response. Remove detailed errors
    // from public production APIs.
    echo json_encode([
        "success" => false,
        "message" => $e->getMessage()
    ], JSON_UNESCAPED_UNICODE);
}
?>
