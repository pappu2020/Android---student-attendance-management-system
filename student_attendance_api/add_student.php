<?php

header("Content-Type: application/json; charset=UTF-8");

require_once "db.php";


// =====================================================
// Only POST Method
// =====================================================

if ($_SERVER["REQUEST_METHOD"] !== "POST") {

    http_response_code(405);

    echo json_encode([
        "success" => false,
        "message" => "Only POST method is allowed"
    ]);

    exit;
}


// =====================================================
// Get Input Data
// =====================================================

$name = isset($_POST["name"])
    ? trim($_POST["name"])
    : "";

$roll_number = isset($_POST["roll_number"])
    ? trim($_POST["roll_number"])
    : "";

$class_name = isset($_POST["class_name"])
    ? trim($_POST["class_name"])
    : "";

$section = isset($_POST["section"])
    ? trim($_POST["section"])
    : "";

$email = isset($_POST["email"])
    ? trim($_POST["email"])
    : "";

$phone = isset($_POST["phone"])
    ? trim($_POST["phone"])
    : "";


// =====================================================
// Validation
// =====================================================

if ($name === "") {

    http_response_code(400);

    echo json_encode([
        "success" => false,
        "message" => "Student name is required"
    ]);

    exit;
}


if ($roll_number === "") {

    http_response_code(400);

    echo json_encode([
        "success" => false,
        "message" => "Roll number is required"
    ]);

    exit;
}


if ($class_name === "") {

    http_response_code(400);

    echo json_encode([
        "success" => false,
        "message" => "Class is required"
    ]);

    exit;
}


if ($section === "") {

    http_response_code(400);

    echo json_encode([
        "success" => false,
        "message" => "Section is required"
    ]);

    exit;
}


// =====================================================
// Check Duplicate Roll Number
// =====================================================

$checkSql = "
    SELECT id
    FROM students
    WHERE roll_number = ?
";


$checkStmt =
        $conn->prepare(
                $checkSql
        );


if (!$checkStmt) {

    http_response_code(500);

    echo json_encode([
        "success" => false,
        "message" => "Failed to prepare duplicate check"
    ]);

    exit;
}


$checkStmt->bind_param(
        "s",
        $roll_number
);


$checkStmt->execute();


$checkResult =
        $checkStmt->get_result();


if ($checkResult->num_rows > 0) {

    echo json_encode([
        "success" => false,
        "message" => "Roll number already exists"
    ]);

    $checkStmt->close();

    $conn->close();

    exit;
}


$checkStmt->close();


// =====================================================
// Insert Student
// =====================================================

$sql = "
    INSERT INTO students
    (
        name,
        roll_number,
        class_name,
        section,
        email,
        phone
    )
    VALUES
    (
        ?,
        ?,
        ?,
        ?,
        ?,
        ?
    )
";


$stmt =
        $conn->prepare(
                $sql
        );


if (!$stmt) {

    http_response_code(500);

    echo json_encode([
        "success" => false,
        "message" => "Failed to prepare insert query"
    ]);

    exit;
}


// =====================================================
// Bind Parameters
// =====================================================

$stmt->bind_param(
        "ssssss",
        $name,
        $roll_number,
        $class_name,
        $section,
        $email,
        $phone
);


// =====================================================
// Execute
// =====================================================

if ($stmt->execute()) {

    $student_id =
            $conn->insert_id;


    echo json_encode([

        "success" => true,

        "message" =>
                "Student added successfully",

        "student_id" =>
                (string)$student_id,

        "name" =>
                $name,

        "roll_number" =>
                $roll_number,

        "class_name" =>
                $class_name,

        "section" =>
                $section,

        "email" =>
                $email,

        "phone" =>
                $phone
    ]);

} else {

    http_response_code(500);

    echo json_encode([

        "success" => false,

        "message" =>
                "Failed to add student"
    ]);
}


// =====================================================
// Close Connection
// =====================================================

$stmt->close();

$conn->close();

?>