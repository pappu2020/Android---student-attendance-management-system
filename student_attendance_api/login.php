
<?php

header("Content-Type: application/json; charset=UTF-8");

require_once "db.php";

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

    // 2. Validate login input.
    $email = isset($_POST["email"]) &&
             is_string($_POST["email"])
        ? trim($_POST["email"])
        : "";

    $password = isset($_POST["password"]) &&
                is_string($_POST["password"])
        ? $_POST["password"]
        : "";

    if (
        $email === "" ||
        strlen($email) > 254 ||
        !filter_var($email, FILTER_VALIDATE_EMAIL) ||
        $password === "" ||
        strlen($password) > 4096
    ) {
        sendJsonResponse(400, [
            "success" => false,
            "message" => "Valid email and password are required"
        ]);
    }

    // 3. Find the user using a prepared statement.
    $stmt = $conn->prepare("
        SELECT id, name, email, password, role
        FROM users
        WHERE email = ?
        LIMIT 1
    ");

    $stmt->bind_param("s", $email);
    $stmt->execute();

    $user = $stmt->get_result()->fetch_assoc();

    $stmt->close();

    // Do not reveal whether the email exists.
    if (
        !$user ||
        !password_verify($password, $user["password"])
    ) {
        sendJsonResponse(401, [
            "success" => false,
            "message" => "Invalid email or password"
        ]);
    }

    // 4. Permit only supported account roles.
    $role = strtolower(trim((string) $user["role"]));

    if (!in_array($role, ["admin", "teacher"], true)) {
        sendJsonResponse(403, [
            "success" => false,
            "message" => "Account role is not permitted"
        ]);
    }

    // 5. Generate a cryptographically secure token.
    $token = bin2hex(random_bytes(32));
    $tokenHash = hash("sha256", $token);

    // 6. Create the token record and clean up expired tokens.
    $conn->begin_transaction();

    try {
        $cleanup = $conn->prepare("
            DELETE FROM user_tokens
            WHERE user_id = ?
              AND expires_at <= NOW()
        ");

        $userId = (int) $user["id"];

        $cleanup->bind_param("i", $userId);
        $cleanup->execute();
        $cleanup->close();

        // Store only the token hash, never the raw token.
        $insert = $conn->prepare("
            INSERT INTO user_tokens (
                user_id,
                token_hash,
                expires_at
            )
            VALUES (
                ?,
                ?,
                DATE_ADD(NOW(), INTERVAL 12 HOUR)
            )
        ");

        $insert->bind_param(
            "is",
            $userId,
            $tokenHash
        );

        $insert->execute();
        $insert->close();

        $conn->commit();

    } catch (Throwable $e) {
        $conn->rollback();
        throw $e;
    }

    // 7. Preserve the Android app's expected response fields.
    sendJsonResponse(200, [
        "success" => true,
        "message" => "Login successful",
        "token" => $token,

        "user" => [
            "id" => (int) $user["id"],
            "name" => $user["name"],
            "email" => $user["email"],
            "role" => $role
        ]
    ]);

} catch (Throwable $e) {
    error_log(
        "login.php error: " . $e->getMessage()
    );

    sendJsonResponse(500, [
        "success" => false,
        "message" => "Unable to process login"
    ]);

} finally {
    if (isset($conn) && $conn instanceof mysqli) {
        $conn->close();
    }
}
?>
