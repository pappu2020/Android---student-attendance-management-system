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
$student_id = isset($_POST["student_id"])
    ? trim($_POST["student_id"])
    : "";

$attendance_date = isset($_POST["attendance_date"])
    ? trim($_POST["attendance_date"])
    : "";

$status = isset($_POST["status"])
    ? trim($_POST["status"])
    : "";


// Validate required fields
if (
    $student_id === "" ||
    $attendance_date === "" ||
    $status === ""
) {

    http_response_code(400);

    echo json_encode([
        "success" => false,
        "message" => "Student ID, date and status are required"
    ]);

    exit;
}


// Validate status
if (
    $status !== "Present" &&
    $status !== "Absent"
) {

    http_response_code(400);

    echo json_encode([
        "success" => false,
        "message" => "Invalid attendance status"
    ]);

    exit;
}


// Check whether student exists
$checkSql = "SELECT id FROM students WHERE id = ?";

$checkStmt = $conn->prepare($checkSql);

if (!$checkStmt) {

    http_response_code(500);

    echo json_encode([
        "success" => false,
        "message" => "Failed to prepare student query"
    ]);

    exit;
}


$checkStmt->bind_param(
    "i",
    $student_id
);

$checkStmt->execute();

$result = $checkStmt->get_result();


if ($result->num_rows === 0) {

    echo json_encode([
        "success" => false,
        "message" => "Student not found"
    ]);

    $checkStmt->close();
    $conn->close();

    exit;
}


$checkStmt->close();


// Insert or update attendance
$sql = "
    INSERT INTO attendance
    (
        student_id,
        attendance_date,
        status
    )
    VALUES
    (
        ?,
        ?,
        ?
    )
    ON DUPLICATE KEY UPDATE
        status = ?
";


$stmt = $conn->prepare($sql);


if (!$stmt) {

    http_response_code(500);

    echo json_encode([
        "success" => false,
        "message" => "Failed to prepare attendance query"
    ]);

    exit;
}


// Bind parameters
$stmt->bind_param(
    "isss",
    $student_id,
    $attendance_date,
    $status,
    $status
);


// Execute query
if ($stmt->execute()) {

    echo json_encode([
        "success" => true,
        "message" => "Attendance saved successfully",
        "student_id" => $student_id,
        "attendance_date" => $attendance_date,
        "status" => $status
    ]);

} else {

    http_response_code(500);

    echo json_encode([
        "success" => false,
        "message" => "Failed to save attendance",
        "error" => $stmt->error
    ]);
}


$stmt->close();
$conn->close();

?>