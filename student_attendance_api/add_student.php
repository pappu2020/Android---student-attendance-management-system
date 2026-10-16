
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

// 1. POST requests only.
if ($_SERVER["REQUEST_METHOD"] !== "POST") {
    header("Allow: POST");

    sendJsonResponse(405, [
        "success" => false,
        "message" => "Only POST method is allowed"
    ]);
}

try {
    // 2. Only Admin can add students.
    requireAdmin();

    global $conn;

    if (!isset($conn) || !($conn instanceof mysqli)) {
        throw new RuntimeException(
            "Database connection unavailable"
        );
    }

    // 3. Validate that each input is a string.
    $fields = [
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

    $name = $input["name"];
    $roll_number = $input["roll_number"];
    $class_name = $input["class_name"];
    $section = $input["section"];
    $email = $input["email"];
    $phone = $input["phone"];

    // 4. Validate required fields.
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

    // 5. Validate field lengths.
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
            "message" => "One or more fields exceed the allowed length"
        ]);
    }

    // 6. Validate optional email.
    if (
        $email !== "" &&
        !filter_var($email, FILTER_VALIDATE_EMAIL)
    ) {
        sendJsonResponse(400, [
            "success" => false,
            "message" => "Invalid email address"
        ]);
    }

    // 7. Check duplicate roll number in the same class/section.
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

    $duplicate =
        $checkStmt->get_result()->num_rows > 0;

    $checkStmt->close();

    if ($duplicate) {
        sendJsonResponse(409, [
            "success" => false,
            "message" =>
                "This roll number already exists in the selected class and section"
        ]);
    }

    // 8. Insert the student using a prepared statement.
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

    $studentId = $conn->insert_id;

    $stmt->close();

    // 9. Preserve the existing Android response fields.
    sendJsonResponse(201, [
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
    ]);

} catch (Throwable $e) {
    error_log(
        "add_student.php error: " . $e->getMessage()
    );

    sendJsonResponse(500, [
        "success" => false,
        "message" => "Unable to add student"
    ]);

} finally {
    if (isset($conn) && $conn instanceof mysqli) {
        $conn->close();
    }
}
?>
