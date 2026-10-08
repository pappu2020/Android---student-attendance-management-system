<?php

header("Content-Type: application/json; charset=UTF-8");

require_once "db.php";


// Only POST request allowed
if ($_SERVER["REQUEST_METHOD"] !== "POST") {

    http_response_code(405);

    echo json_encode([
        "success" => false,
        "message" => "Only POST method is allowed"
    ]);

    exit;
}


// Get POST data
$class_name = isset($_POST["class_name"])
    ? trim($_POST["class_name"])
    : "";

$section = isset($_POST["section"])
    ? trim($_POST["section"])
    : "";

$attendance_date = isset($_POST["attendance_date"])
    ? trim($_POST["attendance_date"])
    : "";


// Validate data
if (
    $class_name === "" ||
    $section === "" ||
    $attendance_date === ""
) {

    http_response_code(400);

    echo json_encode([
        "success" => false,
        "message" => "Class, section and date are required"
    ]);

    exit;
}


// Get attendance records
$sql = "
    SELECT
        a.student_id,
        a.attendance_date,
        a.status,
        s.name,
        s.roll_number,
        s.class_name,
        s.section
    FROM attendance a

    INNER JOIN students s
        ON a.student_id = s.id

    WHERE s.class_name = ?
      AND s.section = ?
      AND a.attendance_date = ?

    ORDER BY s.roll_number ASC
";


$stmt = $conn->prepare($sql);


if (!$stmt) {

    http_response_code(500);

    echo json_encode([
        "success" => false,
        "message" => "Failed to prepare database query"
    ]);

    exit;
}


$stmt->bind_param(
    "sss",
    $class_name,
    $section,
    $attendance_date
);


$stmt->execute();

$result = $stmt->get_result();


$attendance = [];


while ($row = $result->fetch_assoc()) {

    $attendance[] = $row;
}


// Return response
echo json_encode([
    "success" => true,
    "message" => "Attendance fetched successfully",
    "class_name" => $class_name,
    "section" => $section,
    "attendance_date" => $attendance_date,
    "total_records" => count($attendance),
    "attendance" => $attendance
]);


$stmt->close();
$conn->close();

?>