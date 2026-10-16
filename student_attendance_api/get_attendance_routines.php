
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

    if (!in_array($role, ["admin", "teacher"], true)) {
        sendJsonResponse(403, [
            "success" => false,
            "message" => "Admin or Teacher permission required"
        ]);
    }

    global $conn;

    $className = trim($_POST["class_name"] ?? "");
    $section = trim($_POST["section"] ?? "");
    $dayOfWeek = isset($_POST["day_of_week"])
        ? (int)$_POST["day_of_week"]
        : 0;

    if ($className === "" || $section === "") {
        sendJsonResponse(400, [
            "success" => false,
            "message" => "Class and section are required"
        ]);
    }

    if ($dayOfWeek < 1 || $dayOfWeek > 7) {
        sendJsonResponse(400, [
            "success" => false,
            "message" => "Valid day_of_week is required (1-7)"
        ]);
    }

    $sql = "
        SELECT
            cr.id AS routine_id,
            cr.class_name,
            cr.section,
            cr.day_of_week,
            cr.start_time,
            cr.end_time,
            cr.subject_id,
            s.subject_code,
            s.subject_name,
            cr.teacher_user_id,
            u.name AS teacher_name
        FROM class_routines cr
        INNER JOIN subjects s
            ON s.id = cr.subject_id
        INNER JOIN users u
            ON u.id = cr.teacher_user_id
        WHERE cr.class_name = ?
          AND cr.section = ?
          AND cr.day_of_week = ?
          AND cr.is_active = 1
    ";

    if ($role === "teacher") {
        $sql .= " AND cr.teacher_user_id = ?";
    }

    $sql .= " ORDER BY cr.start_time ASC";

    $stmt = $conn->prepare($sql);

    if ($role === "teacher") {
        $stmt->bind_param(
            "ssii",
            $className,
            $section,
            $dayOfWeek,
            $userId
        );
    } else {
        $stmt->bind_param(
            "ssi",
            $className,
            $section,
            $dayOfWeek
        );
    }

    $stmt->execute();
    $result = $stmt->get_result();

    $routines = [];

    while ($row = $result->fetch_assoc()) {
        $row["routine_id"] = (int)$row["routine_id"];
        $row["subject_id"] = (int)$row["subject_id"];
        $row["teacher_user_id"] = (int)$row["teacher_user_id"];
        $routines[] = $row;
    }

    $stmt->close();

    sendJsonResponse(200, [
        "success" => true,
        "message" => "Routines fetched successfully",
        "routines" => $routines
    ]);

} catch (Throwable $e) {
    error_log(
        "get_attendance_routines.php error: " . $e->getMessage()
    );

    sendJsonResponse(500, [
        "success" => false,
        "message" => "Failed to fetch attendance routines"
    ]);
} finally {
    if (isset($conn) && $conn instanceof mysqli) {
        $conn->close();
    }
}
?>
