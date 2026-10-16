```php
<?php

header("Content-Type: application/json; charset=UTF-8");

require_once "auth.php";

mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);

$uploadedFilePath = null;

function sendJsonResponse(int $statusCode, array $data): void
{
    http_response_code($statusCode);

    echo json_encode(
        $data,
        JSON_UNESCAPED_UNICODE
    );

    exit;
}

try {
    // 1. Allow POST requests only.
    if ($_SERVER["REQUEST_METHOD"] !== "POST") {
        header("Allow: POST");

        sendJsonResponse(405, [
            "success" => false,
            "message" => "Only POST method is allowed"
        ]);
    }

    // 2. Only Admin can upload student photos.
    requireAdmin();

    global $conn;

    if (!isset($conn) || !($conn instanceof mysqli)) {
        throw new RuntimeException(
            "Database connection unavailable"
        );
    }

    // 3. Validate student ID input.
    if (
        !isset($_POST["student_id"]) ||
        !is_string($_POST["student_id"])
    ) {
        sendJsonResponse(400, [
            "success" => false,
            "message" => "Valid student ID is required"
        ]);
    }

    $id = trim($_POST["student_id"]);

    if (
        $id === "" ||
        !ctype_digit($id) ||
        (int) $id < 1
    ) {
        sendJsonResponse(400, [
            "success" => false,
            "message" => "Invalid student ID"
        ]);
    }

    $studentId = (int) $id;

    // 4. Validate the uploaded file structure.
    if (
        !isset($_FILES["photo"]) ||
        !is_array($_FILES["photo"]) ||
        !isset(
            $_FILES["photo"]["error"],
            $_FILES["photo"]["tmp_name"],
            $_FILES["photo"]["size"]
        ) ||
        !is_string($_FILES["photo"]["tmp_name"])
    ) {
        sendJsonResponse(400, [
            "success" => false,
            "message" => "Photo file is required"
        ]);
    }

    $file = $_FILES["photo"];

    if ($file["error"] !== UPLOAD_ERR_OK) {
        $message = $file["error"] === UPLOAD_ERR_INI_SIZE ||
                   $file["error"] === UPLOAD_ERR_FORM_SIZE
            ? "Uploaded photo exceeds the allowed size"
            : "Photo upload failed";

        sendJsonResponse(400, [
            "success" => false,
            "message" => $message
        ]);
    }

    // 5. Enforce the 5 MB file size limit.
    $maxFileSize = 5 * 1024 * 1024;

    if (
        !is_numeric($file["size"]) ||
        (int) $file["size"] <= 0 ||
        (int) $file["size"] > $maxFileSize
    ) {
        sendJsonResponse(400, [
            "success" => false,
            "message" => "Photo must be smaller than 5 MB"
        ]);
    }

    if (
        !is_uploaded_file($file["tmp_name"]) ||
        !is_file($file["tmp_name"])
    ) {
        sendJsonResponse(400, [
            "success" => false,
            "message" => "Invalid uploaded file"
        ]);
    }

    // 6. Confirm that the student exists.
    $checkStmt = $conn->prepare(
        "SELECT id FROM students WHERE id = ? LIMIT 1"
    );

    $checkStmt->bind_param("i", $studentId);
    $checkStmt->execute();

    $studentExists =
        $checkStmt->get_result()->num_rows > 0;

    $checkStmt->close();

    if (!$studentExists) {
        sendJsonResponse(404, [
            "success" => false,
            "message" => "Student not found"
        ]);
    }

    // 7. Verify actual image content.
    $imageInfo = @getimagesize($file["tmp_name"]);

    if ($imageInfo === false) {
        sendJsonResponse(400, [
            "success" => false,
            "message" => "Uploaded file is not a valid image"
        ]);
    }

    $allowedTypes = [
        IMAGETYPE_JPEG => "jpg",
        IMAGETYPE_PNG  => "png",
        IMAGETYPE_WEBP => "webp"
    ];

    $imageType = $imageInfo[2];

    if (!isset($allowedTypes[$imageType])) {
        sendJsonResponse(400, [
            "success" => false,
            "message" => "Only JPG, PNG and WEBP images are allowed"
        ]);
    }

    // 8. Limit image dimensions to reduce oversized image risk.
    $width = (int) $imageInfo[0];
    $height = (int) $imageInfo[1];

    if (
        $width < 1 ||
        $height < 1 ||
        $width > 6000 ||
        $height > 6000 ||
        ($width * $height) > 25000000
    ) {
        sendJsonResponse(400, [
            "success" => false,
            "message" => "Image dimensions are too large"
        ]);
    }

    // 9. Create the upload directory if needed.
    $uploadDirectory = __DIR__ . "/uploads/students";

    if (
        !is_dir($uploadDirectory) &&
        !mkdir($uploadDirectory, 0755, true) &&
        !is_dir($uploadDirectory)
    ) {
        throw new RuntimeException(
            "Unable to create upload directory"
        );
    }

    if (!is_writable($uploadDirectory)) {
        throw new RuntimeException(
            "Upload directory is not writable"
        );
    }

    // 10. Generate a random filename.
    $extension = $allowedTypes[$imageType];

    $fileName = "student_" . $studentId . "_" .
        bin2hex(random_bytes(16)) . "." . $extension;

    $destination = $uploadDirectory . DIRECTORY_SEPARATOR . $fileName;

    if (!move_uploaded_file($file["tmp_name"], $destination)) {
        throw new RuntimeException(
            "Unable to save uploaded image"
        );
    }

    $uploadedFilePath = $destination;

    // 11. Update the student's photo filename.
    $updateStmt = $conn->prepare(
        "UPDATE students SET photo = ? WHERE id = ?"
    );

    $updateStmt->bind_param("si", $fileName, $studentId);
    $updateStmt->execute();

    $updatedRows = $updateStmt->affected_rows;

    $updateStmt->close();

    /*
     * affected_rows may be zero if the stored value did not
     * change. The student was checked above, so zero is not
     * necessarily an error.
     */
    $uploadedFilePath = null;

    // 12. Preserve the existing response fields.
    sendJsonResponse(200, [
        "success" => true,
        "message" => "Student photo uploaded successfully",
        "student_id" => (string) $studentId,
        "photo" => $fileName
    ]);

} catch (Throwable $e) {
    // Remove the new file if saving the database value failed.
    if (
        $uploadedFilePath !== null &&
        is_file($uploadedFilePath)
    ) {
        @unlink($uploadedFilePath);
    }

    error_log(
        "upload_student_photo.php error: " .
        $e->getMessage()
    );

    sendJsonResponse(500, [
        "success" => false,
        "message" => "Unable to upload student photo"
    ]);

} finally {
    if (isset($conn) && $conn instanceof mysqli) {
        $conn->close();
    }
}
?>
```