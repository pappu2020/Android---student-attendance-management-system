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

try {
    // 2. Only Admin can update students.
    requireAdmin();

    global $conn;

    if (!isset($conn) || !($conn instanceof mysqli)) {
        throw new RuntimeException(
            "Database connection unavailable"
        );
    }

    // 3. Validate that all inputs are strings.
    $fields = [
        "id",
        "name",
        "roll_number",
        "class_name",
        "section",
        "email",
        "phone"
    ];

    $input = [];

    foreach ($fields as $field) {
        if (
            isset($_POST[$field]) &&
            !is_string($_POST[$field])
        ) {
            sendJsonResponse(400, [
                "success" => false,
                "message" => "Invalid input: " . $field
            ]);
        }

        $input[$field] = trim(
            (string) ($_POST[$field] ?? "")
        );
    }

    // 4. Validate student ID.
    if (
        $input["id"] === "" ||
        !ctype_digit($input["id"]) ||
        (int) $input["id"] < 1
    ) {
        sendJsonResponse(400, [
            "success" => false,
            "message" => "Invalid student ID"
        ]);
    }

    $studentId = (int) $input["id"];

    $name = $input["name"];
    $roll_number = $input["roll_number"];
    $class_name = $input["class_name"];
    $section = $input["section"];
    $email = $input["email"];
    $phone = $input["phone"];

    // 5. Validate required fields.
    if (
        $name === "" ||
        $roll_number === "" ||
        $class_name === "" ||
        $section === ""
    ) {
        sendJsonResponse(400, [
            "success" => false,
            "message" =>
                "Name, roll number, class and section are required"
        ]);
    }

    // 6. Validate input lengths.
    if (
        mb_strlen($name) > 150 ||
        mb_strlen($roll_number) > 50 ||
        mb_strlen($class_name) > 100 ||
        mb_strlen($section) > 50 ||
        mb_strlen($email) > 254 ||
        mb_strlen($phone) > 30
    ) {
        sendJsonResponse(400, [
            "success" => false,
            "message" =>
                "One or more fields exceed the allowed length"
        ]);
    }

    // 7. Validate optional email.
    if (
        $email !== "" &&
        !filter_var($email, FILTER_VALIDATE_EMAIL)
    ) {
        sendJsonResponse(400, [
            "success" => false,
            "message" => "Invalid email address"
        ]);
    }

    // 8. Confirm that the student exists.
    $checkStudentSql = "
        SELECT id
        FROM students
        WHERE id = ?
        LIMIT 1
    ";

    $checkStudentStmt = $conn->prepare($checkStudentSql);
    $checkStudentStmt->bind_param("i", $studentId);
    $checkStudentStmt->execute();

    $studentExists =
        $checkStudentStmt->get_result()->num_rows > 0;

    $checkStudentStmt->close();

    if (!$studentExists) {
        sendJsonResponse(404, [
            "success" => false,
            "message" => "Student not found"
        ]);
    }

    // 9. Check duplicate roll number in the same class/section.
    // Exclude the student currently being updated.
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

    $duplicateRoll =
        $checkRollStmt->get_result()->num_rows > 0;

    $checkRollStmt->close();

    if ($duplicateRoll) {
        sendJsonResponse(409, [
            "success" => false,
            "message" =>
                "This roll number already exists in the selected class and section"
        ]);
    }

    // 10. Update the student.
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

    // 11. Preserve the existing JSON response fields.
    sendJsonResponse(200, [
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
    ]);

} catch (Throwable $e) {
    error_log(
        "update_student.php error: " .
        $e->getMessage()
    );

    sendJsonResponse(500, [
        "success" => false,
        "message" => "Unable to update student"
    ]);

} finally {
    if (isset($conn) && $conn instanceof mysqli) {
        $conn->close();
    }
}
?>
