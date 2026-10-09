
<?php
header("Content-Type: application/json; charset=UTF-8");

require_once "db.php";

// Allow POST requests only
if ($_SERVER["REQUEST_METHOD"] !== "POST") {
    http_response_code(405);

    echo json_encode([
        "success" => false,
        "message" => "Only POST method is allowed"
    ]);

    $conn->close();
    exit;
}

// Receive selected attendance date
$attendanceDate = $_POST["attendance_date"] ?? "";

// Validate date format
$dateObject = DateTime::createFromFormat("!Y-m-d", $attendanceDate);
$dateErrors = DateTime::getLastErrors();

$isValidDate = $dateObject !== false
    && $dateObject->format("Y-m-d") === $attendanceDate
    && (
        $dateErrors === false
        || (
            $dateErrors["warning_count"] === 0
            && $dateErrors["error_count"] === 0
        )
    );

if (!$isValidDate) {
    http_response_code(400);

    echo json_encode([
        "success" => false,
        "message" => "Invalid date. Use YYYY-MM-DD format."
    ]);

    $conn->close();
    exit;
}

// Count all registered students
$totalStudents = 0;

$totalQuery = "SELECT COUNT(*) AS total_students FROM students";
$totalResult = $conn->query($totalQuery);

if (!$totalResult) {
    http_response_code(500);

    echo json_encode([
        "success" => false,
        "message" => "Failed to count students"
    ]);

    $conn->close();
    exit;
}

$totalRow = $totalResult->fetch_assoc();
$totalStudents = (int) $totalRow["total_students"];

// Fetch attendance for the selected date
$sql = "
    SELECT
        COUNT(*) AS marked_count,
        COALESCE(
            SUM(CASE WHEN status = 'Present' THEN 1 ELSE 0 END),
            0
        ) AS present_count,
        COALESCE(
            SUM(CASE WHEN status = 'Absent' THEN 1 ELSE 0 END),
            0
        ) AS absent_count
    FROM attendance
    WHERE attendance_date = ?
";

$stmt = $conn->prepare($sql);

if (!$stmt) {
    http_response_code(500);

    echo json_encode([
        "success" => false,
        "message" => "Failed to prepare attendance query"
    ]);

    $conn->close();
    exit;
}

$stmt->bind_param("s", $attendanceDate);

if (!$stmt->execute()) {
    http_response_code(500);

    echo json_encode([
        "success" => false,
        "message" => "Failed to fetch attendance"
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

$notMarkedCount = max(0, $totalStudents - $markedCount);

$attendancePercentage = $markedCount > 0
    ? round(($presentCount / $markedCount) * 100, 2)
    : 0;

echo json_encode([
    "success" => true,
    "message" => "Attendance fetched successfully",
    "date" => $attendanceDate,
    "total_students" => $totalStudents,
    "present_count" => $presentCount,
    "absent_count" => $absentCount,
    "marked_count" => $markedCount,
    "not_marked_count" => $notMarkedCount,
    "attendance_percentage" => $attendancePercentage
], JSON_UNESCAPED_UNICODE);

$stmt->close();
$conn->close();
?>
