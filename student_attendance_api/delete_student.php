<?php

header("Content-Type: application/json; charset=UTF-8");

require_once "db.php";


// =====================================================
// Only POST Request Allowed
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
// Get Student ID
// =====================================================

$id = isset($_POST["id"])
    ? trim($_POST["id"])
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


// =====================================================
// Check Student Exists
// =====================================================

$checkSql =
    "SELECT id FROM students WHERE id = ?";

$checkStmt =
    $conn->prepare($checkSql);

$checkStmt->bind_param(
    "i",
    $id
);

$checkStmt->execute();

$result =
    $checkStmt->get_result();


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


// =====================================================
// Delete Student
// =====================================================

$deleteSql =
    "DELETE FROM students WHERE id = ?";

$deleteStmt =
    $conn->prepare($deleteSql);

$deleteStmt->bind_param(
    "i",
    $id
);


// =====================================================
// Execute Delete
// =====================================================

if ($deleteStmt->execute()) {

    echo json_encode([
        "success" => true,
        "message" => "Student deleted successfully"
    ]);

} else {

    http_response_code(500);

    echo json_encode([
        "success" => false,
        "message" => "Failed to delete student"
    ]);
}


// =====================================================
// Close Connection
// =====================================================

$deleteStmt->close();

$conn->close();

?>