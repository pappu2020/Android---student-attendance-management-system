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

$id = isset($_POST["id"])
    ? trim($_POST["id"])
    : "";

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

if ($id === "") {

    http_response_code(400);

    echo json_encode([
        "success" => false,
        "message" => "Student ID is required"
    ]);

    exit;
}


if (!is_numeric($id)) {

    http_response_code(400);

    echo json_encode([
        "success" => false,
        "message" => "Invalid student ID"
    ]);

    exit;
}


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
// Check Student Exists
// =====================================================

$checkStudentSql = "
    SELECT id
    FROM students
    WHERE id = ?
";


$checkStudentStmt =
        $conn->prepare(
                $checkStudentSql
        );


if (!$checkStudentStmt) {

    http_response_code(500);

    echo json_encode([
        "success" => false,
        "message" => "Failed to prepare student check"
    ]);

    exit;
}


$studentId =
        (int)$id;


$checkStudentStmt->bind_param(
        "i",
        $studentId
);


$checkStudentStmt->execute();


$studentResult =
        $checkStudentStmt->get_result();


if ($studentResult->num_rows === 0) {

    echo json_encode([
        "success" => false,
        "message" => "Student not found"
    ]);

    $checkStudentStmt->close();

    $conn->close();

    exit;
}


$checkStudentStmt->close();


// =====================================================
// Check Duplicate Roll Number
// =====================================================
// Same roll number is allowed for the same student,
// but not for another student.

$checkRollSql = "
    SELECT id
    FROM students
    WHERE roll_number = ?
    AND id != ?
";


$checkRollStmt =
        $conn->prepare(
                $checkRollSql
        );


if (!$checkRollStmt) {

    http_response_code(500);

    echo json_encode([
        "success" => false,
        "message" => "Failed to prepare roll number check"
    ]);

    exit;
}


$checkRollStmt->bind_param(
        "si",
        $roll_number,
        $studentId
);


$checkRollStmt->execute();


$rollResult =
        $checkRollStmt->get_result();


if ($rollResult->num_rows > 0) {

    echo json_encode([
        "success" => false,
        "message" => "Roll number already exists for another student"
    ]);

    $checkRollStmt->close();

    $conn->close();

    exit;
}


$checkRollStmt->close();


// =====================================================
// Update Student
// =====================================================

$sql = "
    UPDATE students
    SET
        name = ?,
        roll_number = ?,
        class_name = ?,
        section = ?,
        email = ?,
        phone = ?
    WHERE id = ?
";


$stmt =
        $conn->prepare(
                $sql
        );


if (!$stmt) {

    http_response_code(500);

    echo json_encode([
        "success" => false,
        "message" => "Failed to prepare update query"
    ]);

    exit;
}


// =====================================================
// Bind Parameters
// =====================================================

$stmt->bind_param(
        "ssssssi",
        $name,
        $roll_number,
        $class_name,
        $section,
        $email,
        $phone,
        $studentId
);


// =====================================================
// Execute
// =====================================================

if ($stmt->execute()) {

    echo json_encode([

        "success" => true,

        "message" =>
                "Student updated successfully",

        "student_id" =>
                (string)$studentId,

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
                "Failed to update student"
    ]);
}


// =====================================================
// Close Connection
// =====================================================

$stmt->close();

$conn->close();

?>