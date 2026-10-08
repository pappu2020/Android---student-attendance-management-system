<?php

header("Content-Type: application/json; charset=UTF-8");

require_once "db.php";


// ======================================================
// Only POST Method
// ======================================================

if ($_SERVER["REQUEST_METHOD"] !== "POST") {

    http_response_code(405);

    echo json_encode([
        "success" => false,
        "message" => "Only POST method is allowed"
    ]);

    exit;
}


// ======================================================
// Get Class and Section
// ======================================================

$class_name = isset($_POST["class_name"])
    ? trim($_POST["class_name"])
    : "";

$section = isset($_POST["section"])
    ? trim($_POST["section"])
    : "";


// ======================================================
// Validation
// ======================================================

if ($class_name === "" || $section === "") {

    http_response_code(400);

    echo json_encode([
        "success" => false,
        "message" => "Class and section are required"
    ]);

    exit;
}


// ======================================================
// SQL Query
// ======================================================

$sql = "SELECT
            id,
            name,
            roll_number,
            class_name,
            section,
            email,
            phone,
            photo
        FROM students
        WHERE class_name = ?
        AND section = ?
        ORDER BY roll_number ASC";


// ======================================================
// Prepare Statement
// ======================================================

$stmt = $conn->prepare($sql);


if (!$stmt) {

    http_response_code(500);

    echo json_encode([
        "success" => false,
        "message" => "Failed to prepare database query"
    ]);

    exit;
}


// ======================================================
// Bind Parameters
// ======================================================

$stmt->bind_param(
    "ss",
    $class_name,
    $section
);


// ======================================================
// Execute
// ======================================================

$stmt->execute();

$result = $stmt->get_result();


// ======================================================
// Students Array
// ======================================================

$students = [];


// ======================================================
// Fetch Students
// ======================================================

while ($row = $result->fetch_assoc()) {

    $students[] = $row;
}


// ======================================================
// Response
// ======================================================

echo json_encode([
    "success" => true,
    "message" => "Students fetched successfully",
    "class_name" => $class_name,
    "section" => $section,
    "total_students" => count($students),
    "students" => $students
]);


// ======================================================
// Close
// ======================================================

$stmt->close();

$conn->close();

?>