
<?php

header("Content-Type: application/json; charset=UTF-8");

require_once "auth.php";

mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);

function sendResponse(int $statusCode, array $data): void
{
    http_response_code($statusCode);

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

        sendResponse(405, [
            "success" => false,
            "message" => "GET method required"
        ]);
    }

    // 2. Authenticate using the shared auth.php function.
    $user = requireAuth();

    $userId = (int) ($user["id"] ?? 0);
    $userRole = strtolower(trim($user["role"] ?? ""));

    // 3. Permit only Admin and Teacher roles.
    if (!in_array($userRole, ["admin", "teacher"], true)) {
        sendResponse(403, [
            "success" => false,
            "message" =>
                "You do not have permission to view the timetable"
        ]);
    }

    // 4. Read and validate query parameters.
    $className = isset($_GET["class_name"])
        && is_string($_GET["class_name"])
            ? trim($_GET["class_name"])
            : "";

    $section = isset($_GET["section"])
        && is_string($_GET["section"])
            ? trim($_GET["section"])
            : "";

    $dayInput = isset($_GET["day_of_week"])
        && is_string($_GET["day_of_week"])
            ? trim($_GET["day_of_week"])
            : "";

    if (
        $className === "" ||
        $section === "" ||
        $dayInput === "" ||
        !ctype_digit($dayInput)
    ) {
        sendResponse(400, [
            "success" => false,
            "message" =>
                "Valid class_name, section and day_of_week are required"
        ]);
    }

    $day = (int) $dayInput;

    // Timetable days: Monday = 1 through Saturday = 6.
    if ($day < 1 || $day > 6) {
        sendResponse(400, [
            "success" => false,
            "message" =>
                "day_of_week must be between 1 and 6"
        ]);
    }

    if (
        strlen($className) > 100 ||
        strlen($section) > 50
    ) {
        sendResponse(400, [
            "success" => false,
            "message" => "Invalid class or section value"
        ]);
    }

    global $conn;

    // 5. Build role-based timetable query.
    // Admin sees all periods for the selected class, section and day.
    // Teacher sees only their own active periods.
    $sql = "
        SELECT
            r.id,
            r.class_name,
            r.section,
            r.day_of_week,
            r.start_time,
            r.end_time,
            s.subject_code,
            s.subject_name,
            u.id AS teacher_id,
            u.name AS teacher_name
        FROM class_routines AS r

        INNER JOIN subjects AS s
            ON s.id = r.subject_id

        INNER JOIN users AS u
            ON u.id = r.teacher_user_id

        WHERE r.class_name = ?
          AND r.section = ?
          AND r.day_of_week = ?
          AND r.is_active = 1
    ";

    if ($userRole === "teacher") {
        $sql .= " AND r.teacher_user_id = ? ";
    }

    $sql .= " ORDER BY r.start_time ASC ";

    $stmt = $conn->prepare($sql);

    // 6. Bind parameters according to the role.
    if ($userRole === "teacher") {
        $stmt->bind_param(
            "ssii",
            $className,
            $section,
            $day,
            $userId
        );
    } else {
        $stmt->bind_param(
            "ssi",
            $className,
            $section,
            $day
        );
    }

    $stmt->execute();

    $result = $stmt->get_result();

    $routines = [];

    while ($row = $result->fetch_assoc()) {
        $routines[] = $row;
    }

    $stmt->close();

    // 7. Return the existing response structure.
    sendResponse(200, [
        "success" => true,
        "class_name" => $className,
        "section" => $section,
        "day_of_week" => $day,
        "total_periods" => count($routines),
        "data" => $routines
    ]);

} catch (Throwable $e) {

    error_log(
        "get_timetable.php error: " . $e->getMessage()
    );

    sendResponse(500, [
        "success" => false,
        "message" => "Unable to load timetable"
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
