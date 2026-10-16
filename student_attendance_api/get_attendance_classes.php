
<?php

header("Content-Type: application/json; charset=UTF-8");

require_once "auth.php";

mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);

function sendJsonResponse(int $statusCode, array $data): void
{
    http_response_code($statusCode);
    echo json_encode($data, JSON_UNESCAPED_UNICODE);
    exit;
}

try {
    if ($_SERVER["REQUEST_METHOD"] !== "POST") {
        header("Allow: POST");

        sendJsonResponse(405, [
            "success" => false,
            "message" => "Only POST method is allowed"
        ]);
    }

    $user = requireAuth();

    $userId = (int)($user["id"] ?? 0);
    $role = strtolower(trim($user["role"] ?? ""));

    if (
        $userId <= 0 ||
        !in_array($role, ["admin", "teacher"], true)
    ) {
        sendJsonResponse(403, [
            "success" => false,
            "message" => "Admin or Teacher permission required"
        ]);
    }

    global $conn;

    if (!isset($conn) || !($conn instanceof mysqli)) {
        throw new RuntimeException("Database connection unavailable");
    }

    if ($role === "admin") {
        // Admin can view every class and section that has students.
        $sql = "
            SELECT DISTINCT
                class_name,
                section
            FROM students
            WHERE class_name IS NOT NULL
              AND TRIM(class_name) <> ''
              AND section IS NOT NULL
              AND TRIM(section) <> ''
            ORDER BY class_name, section
        ";

        $stmt = $conn->prepare($sql);

    } else {
        // Teacher can view only classes and sections assigned to them.
        $sql = "
            SELECT DISTINCT
                cr.class_name,
                cr.section
            FROM class_routines cr
            INNER JOIN students s
                ON s.class_name = cr.class_name
               AND s.section = cr.section
            WHERE cr.teacher_user_id = ?
              AND cr.is_active = 1
            ORDER BY cr.class_name, cr.section
        ";

        $stmt = $conn->prepare($sql);
        $stmt->bind_param("i", $userId);
    }

    $stmt->execute();
    $result = $stmt->get_result();

    $classes = [];

    while ($row = $result->fetch_assoc()) {
        $classes[] = [
            "class_name" => $row["class_name"],
            "section" => $row["section"]
        ];
    }

    $stmt->close();

    sendJsonResponse(200, [
        "success" => true,
        "message" => "Class and section list loaded",
        "classes" => $classes
    ]);

} catch (Throwable $e) {
    error_log("get_attendance_classes.php error: " . $e->getMessage());

    sendJsonResponse(500, [
        "success" => false,
        "message" => "Failed to load classes and sections"
    ]);

} finally {
    if (isset($conn) && $conn instanceof mysqli) {
        $conn->close();
    }
}
?>
