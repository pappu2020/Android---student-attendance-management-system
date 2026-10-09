
<?php

header("Content-Type: application/json; charset=UTF-8");

require_once "db.php";

if ($_SERVER["REQUEST_METHOD"] !== "POST") {
    http_response_code(405);

    echo json_encode([
        "success" => false,
        "message" => "Only POST method is allowed"
    ]);

    $conn->close();
    exit;
}

// Get attendance for the last 7 calendar days,
// including today.
$sql = "
    SELECT
        attendance_date,
        SUM(
            CASE
                WHEN status = 'Present' THEN 1
                ELSE 0
            END
        ) AS present_count,
        SUM(
            CASE
                WHEN status = 'Absent' THEN 1
                ELSE 0
            END
        ) AS absent_count
    FROM attendance
    WHERE attendance_date >= DATE_SUB(CURDATE(), INTERVAL 6 DAY)
      AND attendance_date <= CURDATE()
    GROUP BY attendance_date
    ORDER BY attendance_date ASC
";

$result = $conn->query($sql);

if (!$result) {
    http_response_code(500);

    echo json_encode([
        "success" => false,
        "message" => "Failed to fetch weekly attendance"
    ]);

    $conn->close();
    exit;
}

$attendanceByDate = [];

while ($row = $result->fetch_assoc()) {
    $date = $row["attendance_date"];

    $attendanceByDate[$date] = [
        "present_count" => (int) $row["present_count"],
        "absent_count" => (int) $row["absent_count"]
    ];
}

// Generate all 7 dates, including dates with no records.
$weeklyData = [];

for ($i = 6; $i >= 0; $i--) {

    $date = date(
        "Y-m-d",
        strtotime("-" . $i . " days")
    );

    $counts = $attendanceByDate[$date] ?? [
        "present_count" => 0,
        "absent_count" => 0
    ];

    $weeklyData[] = [
        "date" => $date,
        "day" => date("D", strtotime($date)),
        "present_count" => $counts["present_count"],
        "absent_count" => $counts["absent_count"]
    ];
}

echo json_encode([
    "success" => true,
    "message" => "Weekly attendance fetched successfully",
    "weekly_data" => $weeklyData
], JSON_UNESCAPED_UNICODE);

$conn->close();

?>
