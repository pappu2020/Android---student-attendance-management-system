
<?php

header("Content-Type: application/json; charset=UTF-8");

require_once "auth.php";

mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);

// Allow POST requests only.
if ($_SERVER["REQUEST_METHOD"] !== "POST") {
    http_response_code(405);

    echo json_encode([
        "success" => false,
        "message" => "Only POST method is allowed"
    ], JSON_UNESCAPED_UNICODE);

    exit;
}

try {

    // Only authenticated administrators can delete students.
    $user = requireAdmin();

    // Get student ID.
    $id = trim($_POST["id"] ?? "");

    // Validate student ID.
    if (
        $id === "" ||
        !ctype_digit($id) ||
        (int) $id < 1
    ) {
        http_response_code(400);

        echo json_encode([
            "success" => false,
            "message" => "Invalid student ID"
        ], JSON_UNESCAPED_UNICODE);

        exit;
    }

    $studentId = (int) $id;

    // Check whether the student exists.
    $checkSql = "
        SELECT id
        FROM students
        WHERE id = ?
        LIMIT 1
    ";

    $checkStmt = $conn->prepare($checkSql);
    $checkStmt->bind_param("i", $studentId);
    $checkStmt->execute();

    $result = $checkStmt->get_result();
    $studentExists = $result->num_rows > 0;

    $checkStmt->close();

    if (!$studentExists) {
        http_response_code(404);

        echo json_encode([
            "success" => false,
            "message" => "Student not found"
        ], JSON_UNESCAPED_UNICODE);

        exit;
    }

    // Delete the student record.
    $deleteSql = "
        DELETE FROM students
        WHERE id = ?
    ";

    $deleteStmt = $conn->prepare($deleteSql);
    $deleteStmt->bind_param("i", $studentId);
    $deleteStmt->execute();

    $deleted = $deleteStmt->affected_rows > 0;

    $deleteStmt->close();

    if (!$deleted) {
        http_response_code(409);

        echo json_encode([
            "success" => false,
            "message" => "Student could not be deleted"
        ], JSON_UNESCAPED_UNICODE);

        exit;
    }

    // Success response.
    echo json_encode([
        "success" => true,
        "message" => "Student deleted successfully",
        "id" => (string) $studentId
    ], JSON_UNESCAPED_UNICODE);

} catch (Throwable $e) {

    error_log(
        "delete_student.php error: " . $e->getMessage()
    );

    http_response_code(500);

    echo json_encode([
        "success" => false,
        "message" => "Unable to delete student. Check whether attendance records prevent deletion."
    ], JSON_UNESCAPED_UNICODE);

} finally {

    if (isset($conn) && $conn instanceof mysqli) {
        $conn->close();
    }
}

?>
