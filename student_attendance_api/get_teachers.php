
<?php
header("Content-Type: application/json; charset=UTF-8");

require_once "auth.php";
mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);

function sendJson(int $code, array $data): void
{
    http_response_code($code);
    echo json_encode($data, JSON_UNESCAPED_UNICODE);
    exit;
}

if ($_SERVER["REQUEST_METHOD"] !== "GET") {
    header("Allow: GET");
    sendJson(405, [
        "success" => false,
        "message" => "Only GET method is allowed"
    ]);
}

requireAdmin();

try {
    global $conn;

    $sql = "
        SELECT id, name, email, role, created_at
        FROM users
        WHERE LOWER(TRIM(role)) = 'teacher'
        ORDER BY name ASC
    ";

    $result = $conn->query($sql);
    $teachers = [];

    while ($row = $result->fetch_assoc()) {
        $teachers[] = [
            "id" => (int)$row["id"],
            "name" => $row["name"],
            "email" => $row["email"],
            "role" => $row["role"],
            "created_at" => $row["created_at"]
        ];
    }

    sendJson(200, [
        "success" => true,
        "message" => "Teachers loaded successfully",
        "count" => count($teachers),
        "teachers" => $teachers
    ]);

} catch (Throwable $e) {
    error_log("get_teachers.php: " . $e->getMessage());

    sendJson(500, [
        "success" => false,
        "message" => "Unable to load teachers"
    ]);
}
?>
