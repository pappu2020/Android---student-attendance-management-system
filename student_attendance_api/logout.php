
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
    // 2. Authenticate the current session.
    requireAuth();

    global $conn;

    if (!isset($conn) || !($conn instanceof mysqli)) {
        throw new RuntimeException(
            "Database connection unavailable"
        );
    }

    // 3. Read the Authorization header using auth.php.
    $authorization = getAuthorizationHeader();

    if (!preg_match(
        '/^Bearer\s+([a-f0-9]{64})$/i',
        $authorization,
        $matches
    )) {
        sendJsonResponse(401, [
            "success" => false,
            "message" => "Authentication required"
        ]);
    }

    // 4. Hash the token before database lookup.
    $tokenHash = hash("sha256", $matches[1]);

    // 5. Revoke only the current token.
    $stmt = $conn->prepare(
        "DELETE FROM user_tokens WHERE token_hash = ?"
    );

    $stmt->bind_param("s", $tokenHash);
    $stmt->execute();

    $revoked = $stmt->affected_rows === 1;

    $stmt->close();

    if (!$revoked) {
        sendJsonResponse(401, [
            "success" => false,
            "message" => "Session is no longer active"
        ]);
    }

    // 6. Preserve the existing Android response fields.
    sendJsonResponse(200, [
        "success" => true,
        "message" => "Logged out successfully"
    ]);

} catch (Throwable $e) {
    error_log(
        "logout.php error: " . $e->getMessage()
    );

    sendJsonResponse(500, [
        "success" => false,
        "message" => "Unable to log out"
    ]);

} finally {
    if (isset($conn) && $conn instanceof mysqli) {
        $conn->close();
    }
}
?>
