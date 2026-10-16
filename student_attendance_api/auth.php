
<?php

require_once "db.php";

mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);

/**
 * Send a JSON error response and stop execution.
 */
function authError(int $statusCode, string $message): void
{
    http_response_code($statusCode);

    echo json_encode([
        "success" => false,
        "message" => $message
    ], JSON_UNESCAPED_UNICODE);

    exit;
}

/**
 * Read the Authorization header safely.
 */
function getAuthorizationHeader(): string
{
    $authorization = $_SERVER["HTTP_AUTHORIZATION"] ?? "";

    if ($authorization === "" && function_exists("getallheaders")) {
        $headers = getallheaders();

        foreach ($headers as $key => $value) {
            if (strtolower($key) === "authorization") {
                $authorization = is_string($value) ? $value : "";
                break;
            }
        }
    }

    return trim($authorization);
}

/**
 * Authenticate a request using its Bearer token.
 *
 * Returns the authenticated user.
 * The database stores only the SHA-256 token hash.
 */
function requireAuth(): array
{
    global $conn;

    $authorization = getAuthorizationHeader();

    if (!preg_match(
        '/^Bearer\s+([a-f0-9]{64})$/i',
        $authorization,
        $matches
    )) {
        authError(401, "Authentication required");
    }

    $tokenHash = hash("sha256", $matches[1]);

    $sql = "
        SELECT
            u.id,
            u.name,
            u.email,
            u.role
        FROM user_tokens AS t
        INNER JOIN users AS u ON u.id = t.user_id
        WHERE t.token_hash = ?
          AND t.expires_at > NOW()
        LIMIT 1
    ";

    $stmt = $conn->prepare($sql);
    $stmt->bind_param("s", $tokenHash);
    $stmt->execute();

    $user = $stmt->get_result()->fetch_assoc();

    $stmt->close();

    if (!$user) {
        authError(401, "Invalid or expired token");
    }

    $user["id"] = (int) $user["id"];
    $user["role"] = strtolower(trim($user["role"]));

    if (!in_array($user["role"], ["admin", "teacher"], true)) {
        authError(403, "Account role is not permitted");
    }

    return $user;
}

/**
 * Require an authenticated administrator.
 */
function requireAdmin(): array
{
    $user = requireAuth();

    if ($user["role"] !== "admin") {
        authError(403, "Admin permission required");
    }

    return $user;
}

/**
 * Require an authenticated teacher.
 */
function requireTeacher(): array
{
    $user = requireAuth();

    if ($user["role"] !== "teacher") {
        authError(403, "Teacher permission required");
    }

    return $user;
}
?>
