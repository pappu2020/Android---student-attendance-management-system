
<?php

header("Content-Type: application/json; charset=UTF-8");

require_once "auth.php";

mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);

// ---------------------------------------------------------
// Allow POST requests only
// ---------------------------------------------------------

if ($_SERVER["REQUEST_METHOD"] !== "POST") {
    http_response_code(405);

    echo json_encode([
        "success" => false,
        "message" => "Only POST method is allowed"
    ], JSON_UNESCAPED_UNICODE);

    exit;
}

try {

    // ---------------------------------------------------------
    // Admin Authentication
    // ---------------------------------------------------------

    $user = requireAdmin();

    // ---------------------------------------------------------
    // Read Input
    // ---------------------------------------------------------

    $id = trim($_POST["id"] ?? "");
    $name = trim($_POST["name"] ?? "");
    $roll_number = trim($_POST["roll_number"] ?? "");
    $class_name = trim($_POST["class_name"] ?? "");
    $section = trim($_POST["section"] ?? "");
    $email = trim($_POST["email"] ?? "");
    $phone = trim($_POST["phone"] ?? "");

    // ---------------------------------------------------------
    // Validate Student ID
    // ---------------------------------------------------------

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

    // ---------------------------------------------------------
    // Validate Required Fields
    // ---------------------------------------------------------

    if (
        $name === "" ||
        $roll_number === "" ||
        $class_name === "" ||
        $section === ""
    ) {
        http_response_code(400);

        echo json_encode([
            "success" => false,
            "message" => "Name, roll number, class and section are required"
        ], JSON_UNESCAPED_UNICODE);

        exit;
    }

    // ---------------------------------------------------------
    // Validate Optional Email
    // ---------------------------------------------------------

    if (
        $email !== "" &&
        !filter_var($email, FILTER_VALIDATE_EMAIL)
    ) {
        http_response_code(400);

        echo json_encode([
            "success" => false,
            "message" => "Invalid email address"
        ], JSON_UNESCAPED_UNICODE);

        exit;
    }

    // ---------------------------------------------------------
    // Check Whether Student Exists
    // ---------------------------------------------------------

    $checkStudentSql = "
        SELECT id
        FROM students
        WHERE id = ?
        LIMIT 1
    ";

    $checkStudentStmt = $conn->prepare($checkStudentSql);

    $checkStudentStmt->bind_param("i", $studentId);

    $checkStudentStmt->execute();

    $studentResult = $checkStudentStmt->get_result();

    $studentExists = $studentResult->num_rows > 0;

    $checkStudentStmt->close();

    if (!$studentExists) {
        http_response_code(404);

        echo json_encode([
            "success" => false,
            "message" => "Student not found"
        ], JSON_UNESCAPED_UNICODE);

        exit;
    }

    // ---------------------------------------------------------
    // Check Duplicate Roll Number in Same Class and Section
    // Exclude the current student from the duplicate check.
    // ---------------------------------------------------------

    $checkRollSql = "
        SELECT id
        FROM students
        WHERE roll_number = ?
          AND class_name = ?
          AND section = ?
          AND id != ?
        LIMIT 1
    ";

    $checkRollStmt = $conn->prepare($checkRollSql);

    $checkRollStmt->bind_param(
        "sssi",
        $roll_number,
        $class_name,
        $section,
        $studentId
    );

    $checkRollStmt->execute();

    $rollResult = $checkRollStmt->get_result();

    $duplicateRoll = $rollResult->num_rows > 0;

    $checkRollStmt->close();

    if ($duplicateRoll) {
        http_response_code(409);

        echo json_encode([
            "success" => false,
            "message" => "This roll number already exists in the selected class and section"
        ], JSON_UNESCAPED_UNICODE);

        exit;
    }

    // ---------------------------------------------------------
    // Update Student
    // ---------------------------------------------------------

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

    $stmt = $conn->prepare($sql);

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

    $stmt->execute();

    $stmt->close();

    // ---------------------------------------------------------
    // Success Response
    // ---------------------------------------------------------

    echo json_encode([
        "success" => true,
        "message" => "Student updated successfully",
        "id" => (string) $studentId,
        "student_id" => (string) $studentId,
        "name" => $name,
        "roll_number" => $roll_number,
        "class_name" => $class_name,
        "section" => $section,
        "email" => $email,
        "phone" => $phone
    ], JSON_UNESCAPED_UNICODE);

} catch (Throwable $e) {

    // Keep detailed errors in the server log only.
    error_log(
        "update_student.php error: " . $e->getMessage()
    );

    http_response_code(500);

    echo json_encode([
        "success" => false,
        "message" => "Unable to update student"
    ], JSON_UNESCAPED_UNICODE);

} finally {

    if (isset($conn) && $conn instanceof mysqli) {
        $conn->close();
    }
}

?>
