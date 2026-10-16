
<?php

header("Content-Type: application/json; charset=UTF-8");

mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);

// This setup script must never be run through a web request.
if (PHP_SAPI !== "cli") {
    http_response_code(403);

    echo json_encode([
        "success" => false,
        "message" => "Run this setup script from the command line"
    ], JSON_UNESCAPED_UNICODE);

    exit(1);
}

require_once "db.php";

try {

    // Read credentials from temporary environment variables.
    $name = trim(getenv("ADMIN_NAME") ?: "");
    $email = trim(getenv("ADMIN_EMAIL") ?: "");
    $password = getenv("ADMIN_PASSWORD");

    if (
        $name === "" ||
        strlen($name) > 100 ||
        $email === "" ||
        strlen($email) > 254 ||
        !filter_var($email, FILTER_VALIDATE_EMAIL) ||
        $password === false ||
        $password === "" ||
        strlen($password) < 10 ||
        strlen($password) > 72
    ) {
        fwrite(
            STDERR,
            "Set valid ADMIN_NAME, ADMIN_EMAIL and ADMIN_PASSWORD environment variables.\n"
        );

        exit(1);
    }

    // Check whether an account with this email already exists.
    $check = $conn->prepare(
        "SELECT id FROM users WHERE email = ? LIMIT 1"
    );

    $check->bind_param("s", $email);
    $check->execute();

    $existing = $check->get_result()->fetch_assoc();

    $check->close();

    if ($existing) {
        fwrite(
            STDERR,
            "An account with this email already exists. No account was created.\n"
        );

        exit(1);
    }

    // Hash the password before storing it.
    $hashedPassword = password_hash(
        $password,
        PASSWORD_DEFAULT
    );

    if ($hashedPassword === false) {
        throw new RuntimeException("Password hashing failed");
    }

    // The role is fixed; it cannot be supplied by the caller.
    $role = "admin";

    $stmt = $conn->prepare(
        "INSERT INTO users (name, email, password, role)
         VALUES (?, ?, ?, ?)"
    );

    $stmt->bind_param(
        "ssss",
        $name,
        $email,
        $hashedPassword,
        $role
    );

    $stmt->execute();

    fwrite(
        STDOUT,
        "Admin account created successfully.\n"
    );

    $stmt->close();

} catch (Throwable $e) {

    error_log(
        "create_admin.php error: " . $e->getMessage()
    );

    fwrite(
        STDERR,
        "Unable to create the admin account. Check the PHP error log.\n"
    );

    exit(1);

} finally {

    if (isset($conn) && $conn instanceof mysqli) {
        $conn->close();
    }
}
?>
