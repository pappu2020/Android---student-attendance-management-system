<?php

header("Content-Type: application/json; charset=UTF-8");

require_once "db.php";


if ($_SERVER["REQUEST_METHOD"] !== "POST") {

    echo json_encode([
        "success" => false,
        "message" => "Only POST method is allowed"
    ]);

    exit;
}


// ---------------------------------------------------------
// Student ID
// ---------------------------------------------------------

$student_id = isset($_POST["student_id"])
    ? trim($_POST["student_id"])
    : "";


if ($student_id === "") {

    echo json_encode([
        "success" => false,
        "message" => "Student ID is required"
    ]);

    exit;
}


// ---------------------------------------------------------
// Check Image
// ---------------------------------------------------------

if (!isset($_FILES["photo"])) {

    echo json_encode([
        "success" => false,
        "message" => "Photo is required"
    ]);

    exit;
}


$file = $_FILES["photo"];


// ---------------------------------------------------------
// Check Upload Error
// ---------------------------------------------------------

if ($file["error"] !== UPLOAD_ERR_OK) {

    echo json_encode([
        "success" => false,
        "message" => "Photo upload failed"
    ]);

    exit;
}


// ---------------------------------------------------------
// Maximum File Size
// 5 MB
// ---------------------------------------------------------

$maxSize = 5 * 1024 * 1024;

if ($file["size"] > $maxSize) {

    echo json_encode([
        "success" => false,
        "message" => "Photo size must be less than 5 MB"
    ]);

    exit;
}


// ---------------------------------------------------------
// Check Image Type
// ---------------------------------------------------------

$imageInfo = getimagesize(
    $file["tmp_name"]
);

if ($imageInfo === false) {

    echo json_encode([
        "success" => false,
        "message" => "Selected file is not a valid image"
    ]);

    exit;
}


// ---------------------------------------------------------
// Allowed Extensions
// ---------------------------------------------------------

$allowedTypes = [
    "image/jpeg" => "jpg",
    "image/png"  => "png",
    "image/webp" => "webp"
];


$mimeType = $imageInfo["mime"];


if (!isset($allowedTypes[$mimeType])) {

    echo json_encode([
        "success" => false,
        "message" => "Only JPG, PNG and WEBP images are allowed"
    ]);

    exit;
}


$extension =
    $allowedTypes[$mimeType];


// ---------------------------------------------------------
// Upload Directory
// ---------------------------------------------------------

$uploadDirectory =
    __DIR__ . "/uploads/students/";


if (!is_dir($uploadDirectory)) {

    mkdir(
        $uploadDirectory,
        0777,
        true
    );
}


// ---------------------------------------------------------
// Generate Unique Filename
// ---------------------------------------------------------

$fileName =
    "student_" .
    $student_id .
    "_" .
    time() .
    "_" .
    uniqid() .
    "." .
    $extension;


$filePath =
    $uploadDirectory .
    $fileName;


// ---------------------------------------------------------
// Move File
// ---------------------------------------------------------

if (!move_uploaded_file(
    $file["tmp_name"],
    $filePath
)) {

    echo json_encode([
        "success" => false,
        "message" => "Unable to save photo"
    ]);

    exit;
}


// ---------------------------------------------------------
// Save Filename in Database
// ---------------------------------------------------------

$sql = "
    UPDATE students
    SET photo = ?
    WHERE id = ?
";


$stmt =
    $conn->prepare($sql);


if (!$stmt) {

    echo json_encode([
        "success" => false,
        "message" => "Database query preparation failed"
    ]);

    exit;
}


$stmt->bind_param(
    "si",
    $fileName,
    $student_id
);


if ($stmt->execute()) {

    echo json_encode([
        "success" => true,
        "message" => "Photo uploaded successfully",
        "photo" => $fileName
    ]);

} else {

    echo json_encode([
        "success" => false,
        "message" => "Photo database update failed"
    ]);
}


$stmt->close();

$conn->close();

?>