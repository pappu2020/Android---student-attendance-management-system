
<?php
require_once "auth.php";

header("Content-Type: application/json; charset=UTF-8");

if ($_SERVER["REQUEST_METHOD"] !== "POST") {
    http_response_code(405);
    echo json_encode([
        "success" => false,
        "message" => "POST method required"
    ], JSON_UNESCAPED_UNICODE);
    exit;
}

$user = requireTeacher();

$dayOfWeek = filter_input(
    INPUT_POST,
    "day_of_week",
    FILTER_VALIDATE_INT
);

if ($dayOfWeek === false || $dayOfWeek === null ||
    $dayOfWeek < 1 || $dayOfWeek > 7) {
    http_response_code(400);
    echo json_encode([
        "success" => false,
        "message" => "Invalid day_of_week. Use 1 to 7."
    ], JSON_UNESCAPED_UNICODE);
    exit;
}

try {
    $sql = "
        SELECT
            cr.id AS routine_id,
            cr.class_name,
            cr.section,
            cr.subject_id,
            cr.day_of_week,
            cr.start_time,
            cr.end_time,
            s.subject_code,
            s.subject_name
        FROM class_routines AS cr
        INNER JOIN subjects AS s
            ON s.id = cr.subject_id
        WHERE cr.teacher_user_id = ?
          AND cr.day_of_week = ?
          AND cr.is_active = 1
        ORDER BY cr.start_time ASC
    ";

    $stmt = $conn->prepare($sql);

    $teacherId = (int) $user["id"];

    $stmt->bind_param(
        "ii",
        $teacherId,
        $dayOfWeek
    );

    $stmt->execute();

    $result = $stmt->get_result();
    $routines = [];

    while ($row = $result->fetch_assoc()) {
        $routines[] = [
            "routine_id" => (int) $row["routine_id"],
            "class_name" => $row["class_name"],
            "section" => $row["section"],
            "subject_id" => (int) $row["subject_id"],
            "subject_code" => $row["subject_code"],
            "subject_name" => $row["subject_name"],
            "day_of_week" => (int) $row["day_of_week"],
            "start_time" => $row["start_time"],
            "end_time" => $row["end_time"]
        ];
    }

    $stmt->close();

    echo json_encode([
        "success" => true,
        "message" => count($routines) > 0
            ? "Routine loaded successfully"
            : "No routine found for this day",
        "count" => count($routines),
        "routines" => $routines
    ], JSON_UNESCAPED_UNICODE);

} catch (Throwable $e) {
    error_log(
        "Teacher daily routine API error: " . $e->getMessage()
    );

    http_response_code(500);

    echo json_encode([
        "success" => false,
        "message" => "Unable to load teacher routine"
    ], JSON_UNESCAPED_UNICODE);
}
?>
