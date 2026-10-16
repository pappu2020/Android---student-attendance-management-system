
<?php

header("Content-Type: application/json; charset=UTF-8");

require_once "auth.php";

mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);

function sendJsonResponse(int $code, array $data): void
{
    http_response_code($code);
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
        $userId < 1 ||
        !in_array($role, ["admin", "teacher"], true)
    ) {
        sendJsonResponse(403, [
            "success" => false,
            "message" => "Access denied"
        ]);
    }

    global $conn;

    if (!isset($conn) || !($conn instanceof mysqli)) {
        throw new RuntimeException("Database connection unavailable");
    }

    $routineInput = trim((string)($_POST["routine_id"] ?? ""));
    $date = trim((string)($_POST["attendance_date"] ?? ""));

    if (
        $routineInput === "" ||
        !ctype_digit($routineInput) ||
        (int)$routineInput < 1
    ) {
        sendJsonResponse(400, [
            "success" => false,
            "message" => "Valid routine_id is required"
        ]);
    }

    if (
        !preg_match('/^(\d{4})-(\d{2})-(\d{2})$/', $date, $parts) ||
        !checkdate((int)$parts[2], (int)$parts[3], (int)$parts[1])
    ) {
        sendJsonResponse(400, [
            "success" => false,
            "message" => "Invalid attendance_date. Use YYYY-MM-DD."
        ]);
    }

    $routineId = (int)$routineInput;

    // Verify that the selected routine exists.
    $stmt = $conn->prepare(
        "SELECT id, class_name, section, teacher_user_id, is_active
         FROM class_routines
         WHERE id = ?
         LIMIT 1"
    );

    $stmt->bind_param("i", $routineId);
    $stmt->execute();

    $routine = $stmt->get_result()->fetch_assoc();
    $stmt->close();

    if (!$routine || (int)$routine["is_active"] !== 1) {
        sendJsonResponse(404, [
            "success" => false,
            "message" => "Active routine not found"
        ]);
    }

    // Teachers may access only their assigned routine.
    if (
        $role === "teacher" &&
        (int)$routine["teacher_user_id"] !== $userId
    ) {
        sendJsonResponse(403, [
            "success" => false,
            "message" => "You are not assigned to this routine"
        ]);
    }

    // Find the session for the selected routine and date.
    $stmt = $conn->prepare(
        "SELECT id
         FROM class_sessions
         WHERE routine_id = ?
           AND class_date = ?
         LIMIT 1"
    );

    $stmt->bind_param("is", $routineId, $date);
    $stmt->execute();

    $session = $stmt->get_result()->fetch_assoc();
    $stmt->close();

    // No session means no attendance has been saved for this period.
    if (!$session) {
        sendJsonResponse(200, [
            "success" => true,
            "message" => "No attendance saved for this session",
            "attendance" => []
        ]);
    }

    $sessionId = (int)$session["id"];

    // Return attendance only for this session.
    $stmt = $conn->prepare(
        "SELECT
            a.student_id,
            a.status,
            a.attendance_date,
            a.class_session_id,
            a.taken_by_teacher_id
         FROM attendance a
         INNER JOIN students s
             ON s.id = a.student_id
         WHERE a.class_session_id = ?
           AND a.attendance_date = ?
           AND s.class_name = ?
           AND s.section = ?
         ORDER BY s.roll_number ASC"
    );

    $stmt->bind_param(
        "isss",
        $sessionId,
        $date,
        $routine["class_name"],
        $routine["section"]
    );

    $stmt->execute();
    $result = $stmt->get_result();

    $attendance = [];

    while ($row = $result->fetch_assoc()) {
        $attendance[] = [
            "student_id" => (string)$row["student_id"],
            "status" => $row["status"],
            "attendance_date" => $row["attendance_date"],
            "class_session_id" => (int)$row["class_session_id"],
            "taken_by_teacher_id" =>
                $row["taken_by_teacher_id"] === null
                    ? null
                    : (int)$row["taken_by_teacher_id"]
        ];
    }

    $stmt->close();

    sendJsonResponse(200, [
        "success" => true,
        "message" => "Attendance loaded successfully",
        "routine_id" => $routineId,
        "class_session_id" => $sessionId,
        "attendance" => $attendance
    ]);

} catch (Throwable $e) {
    error_log("get_attendance.php error: " . $e->getMessage());

    sendJsonResponse(500, [
        "success" => false,
        "message" => "Failed to fetch attendance"
    ]);

} finally {
    if (isset($conn) && $conn instanceof mysqli) {
        $conn->close();
    }
}
?>
