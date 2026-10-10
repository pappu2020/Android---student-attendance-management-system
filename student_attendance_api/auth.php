
<?php

require_once "db.php";

mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);

/**
 * Read and validate the Bearer token.
 * Returns the authenticated user or sends an error response.
 */
function requireAuth(): array
{
    global $conn;

    $headers = function_exists("getallheaders")
        ? getallheaders()
        : [];

    $authorization = $headers["Authorization"]
        ?? $headers["authorization"]
        ?? "";

    if (!preg_match(
        '/^Bearer\s+([a-f0-9]{64})$/i',
        trim($authorization),
        $matches
    )) {
        http_response_code(401);
        echo json_encode([
            "success" => false,
            "message" => "Authentication required"
        ]);
        exit;
    }

    $tokenHash = hash("sha256", $matches[1]);

    $sql = "
        SELECT
            u.id,
            u.name,
            u.email,
            u.role
        FROM user_tokens t
        INNER JOIN users u ON u.id = t.user_id
        WHERE t.token_hash = ?
          AND t.expires_at > NOW()
        LIMIT 1
    ";

    $stmt = $conn->prepare($sql);
    $stmt->bind_param("s", $tokenHash);
    $stmt->execute();

    $result = $stmt->get_result();
    $user = $result->fetch_assoc();

    $stmt->close();

    if (!$user) {
        http_response_code(401);
        echo json_encode([
            "success" => false,
            "message" => "Invalid or expired token"
        ]);
        exit;
    }

    return $user;
}

/**
 * Require an authenticated administrator.
 */
function requireAdmin(): array
{
    $user = requireAuth();

    if (strtolower($user["role"]) !== "admin") {
        http_response_code(403);
        echo json_encode([
            "success" => false,
            "message" => "Admin permission required"
        ]);
        exit;
    }

    return $user;
}
