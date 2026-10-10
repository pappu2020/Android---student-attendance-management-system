
<?php
require_once "auth.php";

header("Content-Type: application/json; charset=UTF-8");

mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);

try {
    requireAuth();

    global $conn;

    $sql = "
        SELECT
            s.id,
            s.student_id,
            s.name,
            s.roll_number,
            s.class_name,
            s.section,
            s.department,
            s.semester,
            s.photo,
            COUNT(a.id) AS total_classes,
            COALESCE(
                SUM(CASE WHEN a.status = 'Present' THEN 1 ELSE 0 END),
                0
            ) AS present_classes,
            CASE
                WHEN COUNT(a.id) = 0 THEN 0
                ELSE ROUND(
                    SUM(CASE WHEN a.status = 'Present' THEN 1 ELSE 0 END)
                    * 100.0 / COUNT(a.id),
                    2
                )
            END AS attendance_percentage
        FROM students s
        LEFT JOIN attendance a ON a.student_id = s.id
        GROUP BY
            s.id,
            s.student_id,
            s.name,
            s.roll_number,
            s.class_name,
            s.section,
            s.department,
            s.semester,
            s.photo
        HAVING attendance_percentage < 75
        ORDER BY attendance_percentage ASC, s.name ASC
    ";

    $result = $conn->query($sql);
    $students = [];

    while ($row = $result->fetch_assoc()) {
        $row["total_classes"] = (int) $row["total_classes"];
        $row["present_classes"] = (int) $row["present_classes"];
        $row["attendance_percentage"] =
            (float) $row["attendance_percentage"];

        $row["attendance_status"] =
            $row["total_classes"] === 0
                ? "No records"
                : "Below 75%";

        $students[] = $row;
    }

    echo json_encode([
        "success" => true,
        "count" => count($students),
        "students" => $students
    ]);

} catch (Throwable $e) {
    http_response_code(500);

    echo json_encode([
        "success" => false,
        "message" => "Unable to load low attendance students"
    ]);
}
?>
