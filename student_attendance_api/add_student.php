
<?php

header("Content-Type: application/json; charset=UTF-8");

require_once "auth.php";

mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);

// ---------------------------------------------------------
// Only POST Method
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

    $name = trim($_POST["name"] ?? "");
    $roll_number = trim($_POST["roll_number"] ?? "");
    $class_name = trim($_POST["class_name"] ?? "");
    $section = trim($_POST["section"] ?? "");
    $email = trim($_POST["email"] ?? "");
    $phone = trim($_POST["phone"] ?? "");

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
    // Check Duplicate Roll Number in Same Class and Section
    // ---------------------------------------------------------

    $checkSql = "
        SELECT id
        FROM students
        WHERE roll_number = ?
          AND class_name = ?
          AND section = ?
        LIMIT 1
    ";

    $checkStmt = $conn->prepare($checkSql);

    $checkStmt->bind_param(
        "sss",
        $roll_number,
        $class_name,
        $section
    );

    $checkStmt->execute();

    $checkResult = $checkStmt->get_result();
    $duplicate = $checkResult->num_rows > 0;

    $checkStmt->close();

    if ($duplicate) {
        http_response_code(409);

        echo json_encode([
            "success" => false,
            "message" => "This roll number already exists in the selected class and section"
        ], JSON_UNESCAPED_UNICODE);

        exit;
    }

    // ---------------------------------------------------------
    // Insert Student
    // ---------------------------------------------------------

    $sql = "
        INSERT INTO students (
            name,
            roll_number,
            class_name,
            section,
            email,
            phone
        )
        VALUES (?, ?, ?, ?, ?, ?)
    ";

    $stmt = $conn->prepare($sql);

    $stmt->bind_param(
        "ssssss",
        $name,
        $roll_number,
        $class_name,
        $section,
        $email,
        $phone
    );

    $stmt->execute();

    // Actual numeric primary key from students.id.
    $studentId = $conn->insert_id;

    $stmt->close();

    // ---------------------------------------------------------
    // Success Response
    // ---------------------------------------------------------

    http_response_code(201);

    echo json_encode([
        "success" => true,
        "message" => "Student added successfully",
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

    error_log(
        "add_student.php error: " . $e->getMessage()
    );

    http_response_code(500);

    echo json_encode([
        "success" => false,
        "message" => "Unable to add student"
    ], JSON_UNESCAPED_UNICODE);

} finally {

    if (isset($conn) && $conn instanceof mysqli) {
        $conn->close();
    }
}

?>
