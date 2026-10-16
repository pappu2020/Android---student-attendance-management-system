
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

try {
    // 1. Allow POST requests only.
    if ($_SERVER["REQUEST_METHOD"] !== "POST") {
        header("Allow: POST");

        sendJsonResponse(405, [
            "success" => false,
            "message" => "Only POST method is allowed"
        ]);
    }

    // 2. Only an authenticated Admin can create teachers.
    requireAdmin();

    global $conn;

    if (!isset($conn) || !($conn instanceof mysqli)) {
        throw new RuntimeException(
            "Database connection unavailable"
        );
    }

    // 3. Validate input types before processing.
    if (
        !isset($_POST["name"]) ||
        !is_string($_POST["name"]) ||
        !isset($_POST["email"]) ||
        !is_string($_POST["email"]) ||
        !isset($_POST["password"]) ||
        !is_string($_POST["password"])
    ) {
        sendJsonResponse(400, [
            "success" => false,
            "message" =>
                "Name, email and password must be valid text fields"
        ]);
    }

    $name = trim($_POST["name"]);
    $email = strtolower(trim($_POST["email"]));
    $password = $_POST["password"];

    // 4. Validate name.
    if (
        $name === "" ||
        strlen($name) > 100
    ) {
        sendJsonResponse(400, [
            "success" => false,
            "message" =>
                "Name is required and must not exceed 100 bytes"
        ]);
    }

    // 5. Validate email.
    if (
        $email === "" ||
        strlen($email) > 254 ||
        !filter_var($email, FILTER_VALIDATE_EMAIL)
    ) {
        sendJsonResponse(400, [
            "success" => false,
            "message" => "A valid email address is required"
        ]);
    }

    // 6. Validate password.
    // The 72-byte limit is appropriate for bcrypt-based hashing.
    if (
        strlen($password) < 10 ||
        strlen($password) > 72
    ) {
        sendJsonResponse(400, [
            "success" => false,
            "message" =>
                "Password must be between 10 and 72 bytes"
        ]);
    }

    // 7. Hash the password before storing it.
    $hashedPassword = password_hash(
        $password,
        PASSWORD_DEFAULT
    );

    if ($hashedPassword === false) {
        throw new RuntimeException(
            "Password hashing failed"
        );
    }

    // 8. Assign the role on the server.
    // Never accept the role from client input.
    $role = "teacher";

    // 9. Insert using a prepared statement.
    $insertStmt = $conn->prepare("
        INSERT INTO users (
            name,
            email,
            password,
            role
        )
        VALUES (?, ?, ?, ?)
    ");

    $insertStmt->bind_param(
        "ssss",
        $name,
        $email,
        $hashedPassword,
        $role
    );

    $insertStmt->execute();

    $teacherId = (int) $conn->insert_id;

    $insertStmt->close();

    // 10. Preserve the existing response structure.
    sendJsonResponse(201, [
        "success" => true,
        "message" => "Teacher account created successfully",

        "user" => [
            "id" => $teacherId,
            "name" => $name,
            "email" => $email,
            "role" => $role
        ]
    ]);

} catch (Throwable $e) {
    error_log(
        "create_teacher.php error: " .
        $e->getMessage()
    );

    // Handle duplicate email when a unique constraint exists.
    if (
        $e instanceof mysqli_sql_exception &&
        $e->getCode() === 1062
    ) {
        sendJsonResponse(409, [
            "success" => false,
            "message" =>
                "An account with this email already exists"
        ]);
    }

    sendJsonResponse(500, [
        "success" => false,
        "message" => "Unable to create teacher account"
    ]);

} finally {
    if (isset($conn) && $conn instanceof mysqli) {
        $conn->close();
    }
}
?>
