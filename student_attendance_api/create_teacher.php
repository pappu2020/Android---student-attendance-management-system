<?php
require_once "db.php";

header("Content-Type: application/json; charset=UTF-8");

$name = "Teacher";
$email = "teacher@gmail.com";
$password = "Teacher@12345";
$role = "teacher";

try {
    // Check whether the account already exists
    $check = $conn->prepare(
        "SELECT id FROM users WHERE email = ? LIMIT 1"
    );
    $check->bind_param("s", $email);
    $check->execute();

    if ($check->get_result()->num_rows > 0) {
        echo json_encode([
            "success" => false,
            "message" => "Teacher account already exists"
        ]);
        exit;
    }
    $check->close();

    // Hash the password securely
    $hashedPassword = password_hash(
        $password,
        PASSWORD_DEFAULT
    );

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
    $stmt->close();

    echo json_encode([
        "success" => true,
        "message" => "Teacher account created successfully"
    ]);

} catch (Throwable $e) {
    http_response_code(500);
    echo json_encode([
        "success" => false,
        "message" => "Could not create teacher account"
    ]);
}
?>
