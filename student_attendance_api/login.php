
<?php

header("Content-Type: application/json; charset=UTF-8");

require_once "db.php";

mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);

try {

    // Allow POST requests only.
    if ($_SERVER["REQUEST_METHOD"] !== "POST") {
        http_response_code(405);

        echo json_encode([
            "success" => false,
            "message" => "Only POST method is allowed"
        ]);

        exit;
    }

    // Read login credentials.
    $email = isset($_POST["email"])
        ? trim($_POST["email"])
        : "";

    $password = isset($_POST["password"])
        ? $_POST["password"]
        : "";

    if ($email === "" || $password === "") {
        http_response_code(400);

        echo json_encode([
            "success" => false,
            "message" => "Email and password are required"
        ]);

        exit;
    }

    // Find user by email.
    $sql = "
        SELECT id, name, email, password, role
        FROM users
        WHERE email = ?
        LIMIT 1
    ";

    $stmt = $conn->prepare($sql);
    $stmt->bind_param("s", $email);
    $stmt->execute();

    $result = $stmt->get_result();
    $user = $result->fetch_assoc();

    $stmt->close();

    // Verify the hashed password.
    if (
        !$user ||
        !password_verify($password, $user["password"])
    ) {
        http_response_code(401);

        echo json_encode([
            "success" => false,
            "message" => "Invalid email or password"
        ]);

        exit;
    }

    // Validate the user's role.
    $role = strtolower(trim($user["role"]));

    if (!in_array($role, ["admin", "teacher"], true)) {
        http_response_code(403);

        echo json_encode([
            "success" => false,
            "message" => "Account role is not permitted"
        ]);

        exit;
    }

    // Generate a secure random token.
    $token = bin2hex(random_bytes(32));
    $tokenHash = hash("sha256", $token);

    // Remove expired tokens belonging to this user.
    $cleanup = $conn->prepare("
        DELETE FROM user_tokens
        WHERE user_id = ?
          AND expires_at <= NOW()
    ");

    $cleanup->bind_param("i", $user["id"]);
    $cleanup->execute();
    $cleanup->close();

    // Store only the token hash.
    // The token expires after 12 hours.
    $insert = $conn->prepare("
        INSERT INTO user_tokens
            (user_id, token_hash, expires_at)
        VALUES
            (?, ?, DATE_ADD(NOW(), INTERVAL 12 HOUR))
    ");

    $insert->bind_param(
        "is",
        $user["id"],
        $tokenHash
    );

    $insert->execute();
    $insert->close();

    // Return the token and safe user information.
    echo json_encode([
        "success" => true,
        "message" => "Login successful",
        "token" => $token,
        "user" => [
            "id" => (int) $user["id"],
            "name" => $user["name"],
            "email" => $user["email"],
            "role" => $role
        ]
    ], JSON_UNESCAPED_UNICODE);

} catch (Throwable $e) {

    http_response_code(500);

    echo json_encode([
        "success" => false,
        "message" => "Unable to process login"
    ], JSON_UNESCAPED_UNICODE);

} finally {

    if (isset($conn) && $conn instanceof mysqli) {
        $conn->close();
    }
}
?>
