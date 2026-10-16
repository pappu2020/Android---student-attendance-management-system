
<?php

header("Content-Type: application/json; charset=UTF-8");

require_once "auth.php";

mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);

function sendJsonResponse(int $statusCode, array $data): void
{
    http_response_code($statusCode);

    echo json_encode(
        $data,
        JSON_UNESCAPED_UNICODE
    );

    exit;
}

// 1. Allow POST requests only.
if ($_SERVER["REQUEST_METHOD"] !== "POST") {
    header("Allow: POST");

    sendJsonResponse(405, [
        "success" => false,
        "message" => "Only POST method is allowed"
    ]);
}

$transactionStarted = false;

try {
    // 2. Only Admin can delete students.
    requireAdmin();

    global $conn;

    if (!isset($conn) || !($conn instanceof mysqli)) {
        throw new RuntimeException(
            "Database connection unavailable"
        );
    }

    // 3. Validate student ID input.
    if (
        !isset($_POST["id"]) ||
        !is_string($_POST["id"])
    ) {
        sendJsonResponse(400, [
            "success" => false,
            "message" => "Valid student ID is required"
        ]);
    }

    $id = trim($_POST["id"]);

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

    // 4. Begin transaction.
    $conn->begin_transaction();
    $transactionStarted = true;

    // Lock the student row while checking deletion eligibility.
    $checkSql = "
        SELECT id
        FROM students
        WHERE id = ?
        LIMIT 1
        FOR UPDATE
    ";

    $checkStmt = $conn->prepare($checkSql);
    $checkStmt->bind_param("i", $studentId);
    $checkStmt->execute();

    $studentExists =
        $checkStmt->get_result()->num_rows > 0;

    $checkStmt->close();

    if (!$studentExists) {
        $conn->rollback();
        $transactionStarted = false;

        sendJsonResponse(404, [
            "success" => false,
            "message" => "Student not found"
        ]);
    }

    // 5. Preserve historical attendance records.
    $attendanceSql = "
        SELECT id
        FROM attendance
        WHERE student_id = ?
        LIMIT 1
    ";

    $attendanceStmt = $conn->prepare($attendanceSql);
    $attendanceStmt->bind_param("i", $studentId);
    $attendanceStmt->execute();

    $hasAttendance =
        $attendanceStmt->get_result()->num_rows > 0;

    $attendanceStmt->close();

    if ($hasAttendance) {
        $conn->rollback();
        $transactionStarted = false;

        sendJsonResponse(409, [
            "success" => false,
            "message" =>
                "Student cannot be deleted because attendance history exists. Preserve the records and deactivate the student instead."
        ]);
    }

    // 6. Delete the student only when no attendance exists.
    $deleteSql = "
        DELETE FROM students
        WHERE id = ?
    ";

    $deleteStmt = $conn->prepare($deleteSql);
    $deleteStmt->bind_param("i", $studentId);
    $deleteStmt->execute();

    $deleted = $deleteStmt->affected_rows === 1;

    $deleteStmt->close();

    if (!$deleted) {
        $conn->rollback();
        $transactionStarted = false;

        sendJsonResponse(409, [
            "success" => false,
            "message" => "Student could not be deleted"
        ]);
    }

    // 7. Commit the transaction.
    $conn->commit();
    $transactionStarted = false;

    // 8. Preserve the existing JSON response.
    sendJsonResponse(200, [
        "success" => true,
        "message" => "Student deleted successfully",
        "id" => (string) $studentId
    ]);

} catch (Throwable $e) {
    if (
        $transactionStarted &&
        isset($conn) &&
        $conn instanceof mysqli
    ) {
        try {
            $conn->rollback();
        } catch (Throwable $rollbackError) {
            error_log(
                "delete_student.php rollback error: " .
                $rollbackError->getMessage()
            );
        }
    }

    error_log(
        "delete_student.php error: " .
        $e->getMessage()
    );

    sendJsonResponse(500, [
        "success" => false,
        "message" => "Unable to delete student"
    ]);

} finally {
    if (isset($conn) && $conn instanceof mysqli) {
        $conn->close();
    }
}
?>
