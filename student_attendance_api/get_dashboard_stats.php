
<?php

header("Content-Type: application/json; charset=UTF-8");

require_once "db.php";

if ($_SERVER["REQUEST_METHOD"] !== "POST") {
    http_response_code(405);

    echo json_encode([
        "success" => false,
        "message" => "Only POST method is allowed"
    ]);

    exit;
}

// Today's date according to the server's timezone.
$today = date("Y-m-d");

// 1. Total students
$totalStudents = 0;

$totalSql = "SELECT COUNT(*) AS total_students FROM students";

$totalResult = $conn->query($totalSql);

if (!$totalResult) {
    http_response_code(500);

    echo json_encode([
        "success" => false,
        "message" => "Failed to fetch total students"
    ]);

    $conn->close();
    exit;
}

$totalRow = $totalResult->fetch_assoc();

$totalStudents = (int) $totalRow["total_students"];


// 2. Today's attendance statistics
$presentCount = 0;
$absentCount = 0;
$markedCount = 0;
$notMarkedCount = 0;

$attendanceSql = "
    SELECT
        COUNT(*) AS marked_count,

        COALESCE(
            SUM(CASE
                WHEN status = 'Present' THEN 1
                ELSE 0
            END),
            0
        ) AS present_count,

        COALESCE(
            SUM(CASE
                WHEN status = 'Absent' THEN 1
                ELSE 0
            END),
            0
        ) AS absent_count

    FROM attendance
    WHERE attendance_date = ?
";

$stmt = $conn->prepare($attendanceSql);

if (!$stmt) {
    http_response_code(500);

    echo json_encode([
        "success" => false,
        "message" => "Failed to prepare attendance query"
    ]);

    $conn->close();
    exit;
}

$stmt->bind_param("s", $today);

if (!$stmt->execute()) {
    http_response_code(500);

    echo json_encode([
        "success" => false,
        "message" => "Failed to fetch today's attendance"
    ]);

    $stmt->close();
    $conn->close();
    exit;
}

$result = $stmt->get_result();
$row = $result->fetch_assoc();

$markedCount = (int) $row["marked_count"];
$presentCount = (int) $row["present_count"];
$absentCount = (int) $row["absent_count"];

$notMarkedCount = max(
    0,
    $totalStudents - $markedCount
);


// 3. Today's attendance percentage
$attendancePercentage = 0;

if ($markedCount > 0) {
    $attendancePercentage =
        ($presentCount * 100.0) / $markedCount;
}


// 4. JSON response
echo json_encode([
    "success" => true,
    "message" => "Dashboard statistics fetched successfully",
    "date" => $today,
    "total_students" => $totalStudents,
    "present_count" => $presentCount,
    "absent_count" => $absentCount,
    "marked_count" => $markedCount,
    "not_marked_count" => $notMarkedCount,
    "attendance_percentage" => round(
        $attendancePercentage,
        2
    )
], JSON_UNESCAPED_UNICODE);

$stmt->close();
$conn->close();

?>
