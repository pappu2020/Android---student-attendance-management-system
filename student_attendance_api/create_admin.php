<?php

require_once "db.php";

$name = "Admin";
$email = "admin@gmail.com";
$password = "admin123";
$role = "admin";

// Check whether the user already exists
$check_sql = "SELECT id FROM users WHERE email = ?";

$check_stmt = $conn->prepare($check_sql);

if (!$check_stmt) {
    echo json_encode([
        "success" => false,
        "message" => "Failed to prepare user check"
    ]);
    exit;
}

$check_stmt->bind_param("s", $email);
$check_stmt->execute();

$result = $check_stmt->get_result();

if ($result->num_rows > 0) {

    echo json_encode([
        "success" => false,
        "message" => "Admin account already exists"
    ]);

    $check_stmt->close();
    $conn->close();

    exit;
}

$check_stmt->close();

// Hash the password
$hashed_password = password_hash(
    $password,
    PASSWORD_DEFAULT
);

// Insert new user
$sql = "INSERT INTO users (name, email, password, role)
        VALUES (?, ?, ?, ?)";

$stmt = $conn->prepare($sql);

if (!$stmt) {

    echo json_encode([
        "success" => false,
        "message" => "Failed to prepare insert query"
    ]);

    exit;
}

$stmt->bind_param(
    "ssss",
    $name,
    $email,
    $hashed_password,
    $role
);

if ($stmt->execute()) {

    echo json_encode([
        "success" => true,
        "message" => "Admin account created successfully",
        "email" => $email
    ]);

} else {

    echo json_encode([
        "success" => false,
        "message" => "Failed to create admin account"
    ]);
}

$stmt->close();
$conn->close();

?>