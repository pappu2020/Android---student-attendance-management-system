
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

    // 1. Allow POST requests only.
    if ($_SERVER["REQUEST_METHOD"] !== "POST") {
        header("Allow: POST");

        sendJsonResponse(405, [
            "success" => false,
            "message" => "Only POST method is allowed"
        ]);
    }

    // 2. Authenticate the user.
    $user = requireAuth();

    $userId = (int) ($user["id"] ?? 0);
    $role = strtolower(trim($user["role"] ?? ""));

    if (!in_array($role, ["admin", "teacher"], true)) {
        sendJsonResponse(403, [
            "success" => false,
            "message" => "You do not have permission to view students"
        ]);
    }

    // 3. Read parameters safely.
    $className = (
        isset($_POST["class_name"])
        && is_string($_POST["class_name"])
    ) ? trim($_POST["class_name"]) : "";

    $section = (
        isset($_POST["section"])
        && is_string($_POST["section"])
    ) ? trim($_POST["section"]) : "";

    if ($className === "" || $section === "") {
        sendJsonResponse(400, [
            "success" => false,
            "message" => "Class and section are required"
        ]);
    }

    // Prevent excessively long input.
    if (strlen($className) > 100 || strlen($section) > 50) {
        sendJsonResponse(400, [
            "success" => false,
            "message" => "Invalid class or section value"
        ]);
    }

    global $conn;

    // 4. Verify teacher assignment.
    // Admin can access any class and section.
    if ($role === "teacher") {

        $accessSql = "
            SELECT 1
            FROM class_routines
            WHERE teacher_user_id = ?
              AND class_name = ?
              AND section = ?
              AND is_active = 1
            LIMIT 1
        ";

        $accessStmt = $conn->prepare($accessSql);

        $accessStmt->bind_param(
            "iss",
            $userId,
            $className,
            $section
        );

        $accessStmt->execute();

        $hasAccess =
            $accessStmt->get_result()->num_rows > 0;

        $accessStmt->close();

        if (!$hasAccess) {
            sendJsonResponse(403, [
                "success" => false,
                "message" =>
                    "You are not assigned to this class and section"
            ]);
        }
    }

    // 5. Fetch students only after authorization.
    $sql = "
        SELECT
            id,
            name,
            roll_number,
            class_name,
            section,
            email,
            phone,
            photo
        FROM students
        WHERE class_name = ?
          AND section = ?
        ORDER BY
            CAST(roll_number AS UNSIGNED) ASC,
            roll_number ASC
    ";

    $stmt = $conn->prepare($sql);

    $stmt->bind_param(
        "ss",
        $className,
        $section
    );

    $stmt->execute();

    $result = $stmt->get_result();

    $students = [];

    while ($row = $result->fetch_assoc()) {
        $students[] = $row;
    }

    $stmt->close();

    // 6. Preserve the existing JSON response structure.
    sendJsonResponse(200, [
        "success" => true,
        "message" => "Students fetched successfully",
        "class_name" => $className,
        "section" => $section,
        "total_students" => count($students),
        "students" => $students
    ]);

} catch (Throwable $e) {

    error_log(
        "get_students_by_class_section.php error: "
        . $e->getMessage()
    );

    sendJsonResponse(500, [
        "success" => false,
        "message" => "An internal server error occurred"
    ]);

} finally {

    if (isset($conn) && $conn instanceof mysqli) {
        $conn->close();
    }
}
?>
