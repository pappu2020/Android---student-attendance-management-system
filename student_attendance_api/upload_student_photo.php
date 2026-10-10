
<?php

header("Content-Type: application/json; charset=UTF-8");

require_once "auth.php";

mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);

$uploadedFilePath = null;

try {

    // Only POST requests are allowed.
    if ($_SERVER["REQUEST_METHOD"] !== "POST") {
        http_response_code(405);

        echo json_encode([
            "success" => false,
            "message" => "Only POST method is allowed"
        ], JSON_UNESCAPED_UNICODE);

        exit;
    }

    // Only administrators can upload or change student photos.
    $user = requireAdmin();

    // Read student ID.
    $id = trim($_POST["student_id"] ?? "");

    if ($id === "" || !ctype_digit($id) || (int)$id < 1) {
        http_response_code(400);

        echo json_encode([
            "success" => false,
            "message" => "Invalid student ID"
        ], JSON_UNESCAPED_UNICODE);

        exit;
    }

    $studentId = (int)$id;

    // Validate uploaded file.
    if (!isset($_FILES["photo"])) {
        http_response_code(400);

        echo json_encode([
            "success" => false,
            "message" => "Photo file is required"
        ], JSON_UNESCAPED_UNICODE);

        exit;
    }

    $file = $_FILES["photo"];

    if ($file["error"] !== UPLOAD_ERR_OK) {
        http_response_code(400);

        echo json_encode([
            "success" => false,
            "message" => "Photo upload failed"
        ], JSON_UNESCAPED_UNICODE);

        exit;
    }

    // Maximum file size: 5 MB.
    if ($file["size"] <= 0 || $file["size"] > 5 * 1024 * 1024) {
        http_response_code(400);

        echo json_encode([
            "success" => false,
            "message" => "Photo must be smaller than 5 MB"
        ], JSON_UNESCAPED_UNICODE);

        exit;
    }

    // Confirm that the student exists.
    $checkStmt = $conn->prepare(
        "SELECT id FROM students WHERE id = ? LIMIT 1"
    );

    $checkStmt->bind_param("i", $studentId);
    $checkStmt->execute();

    $studentExists = $checkStmt->get_result()->num_rows > 0;

    $checkStmt->close();

    if (!$studentExists) {
        http_response_code(404);

        echo json_encode([
            "success" => false,
            "message" => "Student not found"
        ], JSON_UNESCAPED_UNICODE);

        exit;
    }

    // Verify actual image content rather than trusting its extension.
    $imageInfo = @getimagesize($file["tmp_name"]);

    if ($imageInfo === false) {
        http_response_code(400);

        echo json_encode([
            "success" => false,
            "message" => "Uploaded file is not a valid image"
        ], JSON_UNESCAPED_UNICODE);

        exit;
    }

    $allowedTypes = [
        IMAGETYPE_JPEG => "jpg",
        IMAGETYPE_PNG  => "png",
        IMAGETYPE_WEBP => "webp"
    ];

    $imageType = $imageInfo[2];

    if (!isset($allowedTypes[$imageType])) {
        http_response_code(400);

        echo json_encode([
            "success" => false,
            "message" => "Only JPG, PNG and WEBP images are allowed"
        ], JSON_UNESCAPED_UNICODE);

        exit;
    }

    // Create the upload directory if it does not exist.
    $uploadDirectory = __DIR__ . "/uploads/students";

    if (
        !is_dir($uploadDirectory) &&
        !mkdir($uploadDirectory, 0755, true) &&
        !is_dir($uploadDirectory)
    ) {
        throw new RuntimeException("Unable to create upload directory");
    }

    // Generate a random filename to prevent collisions.
    $extension = $allowedTypes[$imageType];

    $fileName = "student_" . $studentId . "_" .
        bin2hex(random_bytes(16)) . "." . $extension;

    $destination = $uploadDirectory . "/" . $fileName;

    // Move uploaded file into its destination.
    if (!move_uploaded_file($file["tmp_name"], $destination)) {
        throw new RuntimeException("Unable to save uploaded image");
    }

    $uploadedFilePath = $destination;

    // Store only the filename in the database.
    $updateStmt = $conn->prepare(
        "UPDATE students SET photo = ? WHERE id = ?"
    );

    $updateStmt->bind_param("si", $fileName, $studentId);
    $updateStmt->execute();
    $updateStmt->close();

    echo json_encode([
        "success" => true,
        "message" => "Student photo uploaded successfully",
        "student_id" => (string)$studentId,
        "photo" => $fileName
    ], JSON_UNESCAPED_UNICODE);

    $uploadedFilePath = null;

} catch (Throwable $e) {

    // Remove the new file if the database update failed.
    if (
        $uploadedFilePath !== null &&
        is_file($uploadedFilePath)
    ) {
        @unlink($uploadedFilePath);
    }

    error_log("upload_student_photo.php error: " . $e->getMessage());

    http_response_code(500);

    echo json_encode([
        "success" => false,
        "message" => "Unable to upload student photo"
    ], JSON_UNESCAPED_UNICODE);

} finally {

    if (isset($conn) && $conn instanceof mysqli) {
        $conn->close();
    }
}

?>
