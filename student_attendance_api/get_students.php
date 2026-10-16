
<?php

header("Content-Type: application/json; charset=UTF-8");

require_once "auth.php";

mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);

function sendJsonResponse(int $status, array $data): void
{
    http_response_code($status);

    echo json_encode(
        $data,
        JSON_UNESCAPED_UNICODE
    );

    exit;
}

try {

    // 1. Allow GET requests only.
    if ($_SERVER["REQUEST_METHOD"] !== "GET") {
        header("Allow: GET");

        sendJsonResponse(405, [
            "success" => false,
            "message" => "Only GET method is allowed"
        ]);
    }

    // 2. Authenticate the user.
    $user = requireAuth();

    $role = strtolower(trim($user["role"] ?? ""));
    $userId = (int) ($user["id"] ?? 0);

    if (!in_array($role, ["admin", "teacher"], true)) {
        sendJsonResponse(403, [
            "success" => false,
            "message" => "Admin or Teacher permission required"
        ]);
    }

    global $conn;

    // 3. Fetch students according to the user's role.
    if ($role === "admin") {

        // Admin can view all students.
        $sql = "
            SELECT
                id,
                name,
                roll_number,
                class_name,
                section,
                email,
                phone,
                created_at
            FROM students
            ORDER BY
                class_name ASC,
                section ASC,
                CAST(roll_number AS UNSIGNED) ASC,
                roll_number ASC
        ";

        $result = $conn->query($sql);

    } else {

        // Teacher can view students only in assigned
        // classes and sections with active routines.
        $sql = "
            SELECT
                s.id,
                s.name,
                s.roll_number,
                s.class_name,
                s.section,
                s.email,
                s.phone,
                s.created_at
            FROM students AS s
            WHERE EXISTS (
                SELECT 1
                FROM class_routines AS r
                WHERE r.teacher_user_id = ?
                  AND r.class_name = s.class_name
                  AND r.section = s.section
                  AND r.is_active = 1
            )
            ORDER BY
                s.class_name ASC,
                s.section ASC,
                CAST(s.roll_number AS UNSIGNED) ASC,
                s.roll_number ASC
        ";

        $stmt = $conn->prepare($sql);

        $stmt->bind_param("i", $userId);

        $stmt->execute();

        $result = $stmt->get_result();
    }

    // 4. Build the JSON student list.
    $students = [];

    while ($row = $result->fetch_assoc()) {

        $students[] = [
            "id" => $row["id"],
            "name" => $row["name"],
            "roll_number" => $row["roll_number"],
            "class_name" => $row["class_name"],
            "section" => $row["section"],
            "email" => $row["email"],
            "phone" => $row["phone"],
            "created_at" => $row["created_at"]
        ];
    }

    // 5. Preserve existing response keys.
    sendJsonResponse(200, [
        "success" => true,
        "message" => "Students fetched successfully",
        "students" => $students
    ]);

} catch (Throwable $e) {

    error_log(
        "get_students.php error: " . $e->getMessage()
    );

    sendJsonResponse(500, [
        "success" => false,
        "message" => "Failed to fetch students"
    ]);

} finally {

    if (isset($stmt) && $stmt instanceof mysqli_stmt) {
        $stmt->close();
    }

    if (isset($conn) && $conn instanceof mysqli) {
        $conn->close();
    }
}
?>
