
<?php

header("Content-Type: application/json; charset=UTF-8");

require_once "auth.php";

date_default_timezone_set("Asia/Kolkata");

mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);

function sendJsonResponse(int $code, array $data): void
{
    http_response_code($code);

    echo json_encode(
        $data,
        JSON_UNESCAPED_UNICODE
    );

    exit;
}

try {

    // --------------------------------------------------
    // 1. Validate HTTP method
    // --------------------------------------------------

    if ($_SERVER["REQUEST_METHOD"] !== "POST") {

        header("Allow: POST");

        sendJsonResponse(405, [
            "success" => false,
            "message" => "Only POST method is allowed"
        ]);
    }

    // --------------------------------------------------
    // 2. Authenticate user
    // --------------------------------------------------

    $user = requireAuth();

    $userId = (int)($user["id"] ?? 0);
    $role = strtolower(trim($user["role"] ?? ""));

    if (
        $userId < 1 ||
        !in_array($role, ["admin", "teacher"], true)
    ) {
        sendJsonResponse(403, [
            "success" => false,
            "message" => "Admin or Teacher permission required"
        ]);
    }

    global $conn;

    if (
        !isset($conn) ||
        !($conn instanceof mysqli)
    ) {
        throw new RuntimeException(
            "Database connection unavailable"
        );
    }

    // --------------------------------------------------
    // 3. Read and validate POST parameters
    // --------------------------------------------------

    $studentInput = trim(
        (string)($_POST["student_id"] ?? "")
    );

    $routineInput = trim(
        (string)($_POST["routine_id"] ?? "")
    );

    $date = trim(
        (string)($_POST["attendance_date"] ?? "")
    );

    $status = trim(
        (string)($_POST["status"] ?? "")
    );

    if (
        $studentInput === "" ||
        !ctype_digit($studentInput) ||
        (int)$studentInput < 1
    ) {
        sendJsonResponse(400, [
            "success" => false,
            "message" => "Valid student_id is required"
        ]);
    }

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
        !preg_match(
            '/^(\d{4})-(\d{2})-(\d{2})$/',
            $date,
            $parts
        ) ||
        !checkdate(
            (int)$parts[2],
            (int)$parts[3],
            (int)$parts[1]
        )
    ) {
        sendJsonResponse(400, [
            "success" => false,
            "message" => "Invalid date. Use YYYY-MM-DD."
        ]);
    }

    if (
        !in_array(
            $status,
            ["Present", "Absent"],
            true
        )
    ) {
        sendJsonResponse(400, [
            "success" => false,
            "message" => "Status must be Present or Absent"
        ]);
    }

    $studentId = (int)$studentInput;
    $routineId = (int)$routineInput;

    // --------------------------------------------------
    // 4. Load student
    // --------------------------------------------------

    $stmt = $conn->prepare(
        "SELECT id, class_name, section
         FROM students
         WHERE id = ?
         LIMIT 1"
    );

    $stmt->bind_param("i", $studentId);
    $stmt->execute();

    $student = $stmt->get_result()->fetch_assoc();

    $stmt->close();

    if (!$student) {
        sendJsonResponse(404, [
            "success" => false,
            "message" => "Student not found"
        ]);
    }

    // --------------------------------------------------
    // 5. Load active routine
    // --------------------------------------------------

    $stmt = $conn->prepare(
        "SELECT
            id,
            class_name,
            section,
            subject_id,
            teacher_user_id,
            day_of_week,
            start_time,
            end_time,
            is_active
         FROM class_routines
         WHERE id = ?
         LIMIT 1"
    );

    $stmt->bind_param("i", $routineId);
    $stmt->execute();

    $routine = $stmt->get_result()->fetch_assoc();

    $stmt->close();

    if (
        !$routine ||
        (int)$routine["is_active"] !== 1
    ) {
        sendJsonResponse(404, [
            "success" => false,
            "message" => "Active routine not found"
        ]);
    }

    // --------------------------------------------------
    // 6. Verify student class and section
    // --------------------------------------------------

    if (
        trim($student["class_name"]) !==
            trim($routine["class_name"]) ||
        trim($student["section"]) !==
            trim($routine["section"])
    ) {
        sendJsonResponse(403, [
            "success" => false,
            "message" =>
                "Student does not belong to this routine"
        ]);
    }

    // --------------------------------------------------
    // 7. Verify teacher assignment
    // --------------------------------------------------

    $assignedTeacherId = (int)$routine["teacher_user_id"];

    if (
        $role === "teacher" &&
        $assignedTeacherId !== $userId
    ) {
        sendJsonResponse(403, [
            "success" => false,
            "message" =>
                "You are not assigned to this routine"
        ]);
    }

    // --------------------------------------------------
    // 8. Validate routine weekday
    // Monday = 1, Tuesday = 2 ... Sunday = 7
    // --------------------------------------------------

    $dateObject = DateTime::createFromFormat(
        "!Y-m-d",
        $date,
        new DateTimeZone("Asia/Kolkata")
    );

    if (!$dateObject) {
        sendJsonResponse(400, [
            "success" => false,
            "message" => "Invalid attendance date"
        ]);
    }

    $routineDay = (int)$dateObject->format("N");

    if (
        $routineDay !==
        (int)$routine["day_of_week"]
    ) {
        sendJsonResponse(400, [
            "success" => false,
            "message" =>
                "Selected date does not match this routine's weekday"
        ]);
    }

    // --------------------------------------------------
    // 9. Enforce teacher's assigned time window
    // Start time inclusive; end time exclusive.
    // Admin is exempt from the time restriction.
    // --------------------------------------------------

    if ($role === "teacher") {

        $today = date("Y-m-d");
        $now = date("H:i:s");

        if ($date !== $today) {
            sendJsonResponse(403, [
                "success" => false,
                "message" =>
                    "Attendance can only be marked on the scheduled date"
            ]);
        }

        $startTime = $routine["start_time"];
        $endTime = $routine["end_time"];

        if (
            $now < $startTime ||
            $now >= $endTime
        ) {
            sendJsonResponse(403, [
                "success" => false,
                "message" =>
                    "Attendance is allowed only during the assigned period"
            ]);
        }
    }

    // --------------------------------------------------
    // 10. Create or retrieve class session
    // Requires UNIQUE(routine_id, class_date)
    // --------------------------------------------------

    $conn->begin_transaction();

    $stmt = $conn->prepare(
        "INSERT INTO class_sessions (
            routine_id,
            class_date,
            original_teacher_id,
            assigned_teacher_id
         )
         VALUES (?, ?, ?, ?)
         ON DUPLICATE KEY UPDATE
            id = LAST_INSERT_ID(id)"
    );

    $stmt->bind_param(
        "isii",
        $routineId,
        $date,
        $assignedTeacherId,
        $assignedTeacherId
    );

    $stmt->execute();

    $sessionId = (int)$conn->insert_id;

    $stmt->close();

    if ($sessionId < 1) {

        $sessionStmt = $conn->prepare(
            "SELECT id
             FROM class_sessions
             WHERE routine_id = ?
               AND class_date = ?
             LIMIT 1"
        );

        $sessionStmt->bind_param(
            "is",
            $routineId,
            $date
        );

        $sessionStmt->execute();

        $sessionRow =
            $sessionStmt->get_result()->fetch_assoc();

        $sessionStmt->close();

        $sessionId = (int)(
            $sessionRow["id"] ?? 0
        );
    }

    if ($sessionId < 1) {
        throw new RuntimeException(
            "Could not create or retrieve class session"
        );
    }

    // --------------------------------------------------
    // 11. Insert or update attendance
    // Requires UNIQUE(student_id, class_session_id)
    // --------------------------------------------------

    $stmt = $conn->prepare(
        "INSERT INTO attendance (
            student_id,
            class_session_id,
            taken_by_teacher_id,
            attendance_date,
            status
         )
         VALUES (?, ?, ?, ?, ?)
         ON DUPLICATE KEY UPDATE
            status = VALUES(status),
            taken_by_teacher_id =
                VALUES(taken_by_teacher_id)"
    );

    $stmt->bind_param(
        "iiiss",
        $studentId,
        $sessionId,
        $assignedTeacherId,
        $date,
        $status
    );

    $stmt->execute();

    $stmt->close();

    // --------------------------------------------------
    // 12. Commit transaction
    // --------------------------------------------------

    $conn->commit();

    sendJsonResponse(200, [
        "success" => true,
        "message" => "Attendance saved successfully",
        "student_id" => $studentId,
        "routine_id" => $routineId,
        "class_session_id" => $sessionId,
        "taken_by_teacher_id" => $assignedTeacherId,
        "attendance_date" => $date,
        "status" => $status,
        "timezone" => "Asia/Kolkata"
    ]);

} catch (Throwable $e) {

    if (
        isset($conn) &&
        $conn instanceof mysqli
    ) {
        try {
            $conn->rollback();
        } catch (Throwable $ignored) {
            // Ignore rollback errors.
        }
    }

    error_log(
        "mark_attendance.php error: " .
        $e->getMessage()
    );

    sendJsonResponse(500, [
        "success" => false,
        "message" => "Failed to save attendance"
    ]);

} finally {

    if (
        isset($conn) &&
        $conn instanceof mysqli
    ) {
        $conn->close();
    }
}
?>
