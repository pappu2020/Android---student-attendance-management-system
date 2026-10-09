
<?php

header("Content-Type: application/json; charset=UTF-8");

require_once "db.php";

if ($_SERVER["REQUEST_METHOD"] !== "POST") {
    http_response_code(405);

    echo json_encode([
        "success" => false,
        "message" => "Only POST method is allowed"
    ]);

    exit;
}

$class_name = isset($_POST["class_name"])
    ? trim($_POST["class_name"])
    : "";

$section = isset($_POST["section"])
    ? trim($_POST["section"])
    : "";

$month = isset($_POST["month"])
    ? trim($_POST["month"])
    : "";


/* VALIDATION */

if ($class_name === "" || $section === "") {
    http_response_code(400);

    echo json_encode([
        "success" => false,
        "message" => "Class and section are required"
    ]);

    exit;
}

if (
    !preg_match('/^\d{4}-(0[1-9]|1[0-2])$/', $month)
) {
    http_response_code(400);

    echo json_encode([
        "success" => false,
        "message" => "Valid month is required in YYYY-MM format"
    ]);

    exit;
}


/* FETCH MONTHLY ATTENDANCE */

$sql = "
    SELECT
        s.id,
        s.name,
        s.roll_number,
        s.class_name,
        s.section,
        s.photo,

        COUNT(a.id) AS total_records,

        COALESCE(SUM(
            CASE WHEN a.status = 'Present'
            THEN 1 ELSE 0 END
        ), 0) AS present_count,

        COALESCE(SUM(
            CASE WHEN a.status = 'Absent'
            THEN 1 ELSE 0 END
        ), 0) AS absent_count

    FROM students s

    LEFT JOIN attendance a
        ON a.student_id = s.id
        AND a.attendance_date >= CONCAT(?, '-01')
        AND a.attendance_date < DATE_ADD(
            CONCAT(?, '-01'),
            INTERVAL 1 MONTH
        )

    WHERE s.class_name = ?
      AND s.section = ?

    GROUP BY
        s.id,
        s.name,
        s.roll_number,
        s.class_name,
        s.section,
        s.photo

    ORDER BY
        CAST(s.roll_number AS UNSIGNED) ASC,
        s.roll_number ASC
";

$stmt = $conn->prepare($sql);

if (!$stmt) {
    http_response_code(500);

    echo json_encode([
        "success" => false,
        "message" => "Failed to prepare report query"
    ]);

    $conn->close();
    exit;
}

$stmt->bind_param(
    "ssss",
    $month,
    $month,
    $class_name,
    $section
);

if (!$stmt->execute()) {
    http_response_code(500);

    echo json_encode([
        "success" => false,
        "message" => "Failed to generate monthly report"
    ]);

    $stmt->close();
    $conn->close();
    exit;
}

$result = $stmt->get_result();

$students = [];
$total_students = 0;
$total_present = 0;
$total_absent = 0;
$total_records = 0;

while ($row = $result->fetch_assoc()) {

    $records = (int) $row["total_records"];
    $present = (int) $row["present_count"];
    $absent = (int) $row["absent_count"];

    $percentage = 0;

    if ($records > 0) {
        $percentage = ($present * 100.0) / $records;
    }

    $total_students++;
    $total_present += $present;
    $total_absent += $absent;
    $total_records += $records;

    $students[] = [
        "id" => (int) $row["id"],
        "name" => $row["name"],
        "roll_number" => $row["roll_number"],
        "class_name" => $row["class_name"],
        "section" => $row["section"],
        "photo" => $row["photo"],
        "total_records" => $records,
        "present_count" => $present,
        "absent_count" => $absent,
        "attendance_percentage" => round($percentage, 2)
    ];
}


/* OVERALL RATE ACROSS ALL MARKED STUDENT-DAYS */

$class_average = 0;

if ($total_records > 0) {
    $class_average = ($total_present * 100.0) / $total_records;
}


/* RESPONSE */

echo json_encode([
    "success" => true,
    "message" => "Class monthly report generated successfully",
    "class_name" => $class_name,
    "section" => $section,
    "month" => $month,
    "total_students" => $total_students,
    "total_present" => $total_present,
    "total_absent" => $total_absent,
    "total_records" => $total_records,
    "class_average" => round($class_average, 2),
    "students" => $students
], JSON_UNESCAPED_UNICODE);

$stmt->close();
$conn->close();

?>
