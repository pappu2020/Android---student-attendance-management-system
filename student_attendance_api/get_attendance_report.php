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

$attendance_date = isset($_POST["attendance_date"])
    ? trim($_POST["attendance_date"])
    : "";


if ($class_name === "") {

    echo json_encode([
        "success" => false,
        "message" => "Class is required"
    ]);

    exit;
}


if ($section === "") {

    echo json_encode([
        "success" => false,
        "message" => "Section is required"
    ]);

    exit;
}


if ($attendance_date === "") {

    echo json_encode([
        "success" => false,
        "message" => "Attendance date is required"
    ]);

    exit;
}


// ============================================================
// GET REPORT
// ============================================================

$sql = "
    SELECT

        COUNT(s.id) AS total_students,

        SUM(
            CASE
                WHEN a.status = 'Present'
                THEN 1
                ELSE 0
            END
        ) AS present_count,

        SUM(
            CASE
                WHEN a.status = 'Absent'
                THEN 1
                ELSE 0
            END
        ) AS absent_count,

        SUM(
            CASE
                WHEN a.id IS NULL
                THEN 1
                ELSE 0
            END
        ) AS not_marked_count

    FROM students s

    LEFT JOIN attendance a
        ON s.id = a.student_id
        AND a.attendance_date = ?

    WHERE
        s.class_name = ?
        AND s.section = ?
";


$stmt = $conn->prepare($sql);


if (!$stmt) {

    http_response_code(500);

    echo json_encode([
        "success" => false,
        "message" => "Failed to prepare report query"
    ]);

    exit;
}


$stmt->bind_param(
    "sss",
    $attendance_date,
    $class_name,
    $section
);


if (!$stmt->execute()) {

    http_response_code(500);

    echo json_encode([
        "success" => false,
        "message" => "Failed to execute report query"
    ]);

    $stmt->close();
    $conn->close();

    exit;
}


$result =
    $stmt->get_result();


$row =
    $result->fetch_assoc();


$total_students =
    intval(
        $row["total_students"] ?? 0
    );


$present_count =
    intval(
        $row["present_count"] ?? 0
    );


$absent_count =
    intval(
        $row["absent_count"] ?? 0
    );


$not_marked_count =
    intval(
        $row["not_marked_count"] ?? 0
    );


$marked_students =
    $present_count + $absent_count;


$attendance_rate = 0;


if ($marked_students > 0) {

    $attendance_rate =
        round(
            (
                $present_count
                /
                $marked_students
            ) * 100,
            1
        );
}


// ============================================================
// RESPONSE
// ============================================================

echo json_encode([

    "success" => true,

    "message" =>
        "Attendance report generated successfully",

    "class_name" =>
        $class_name,

    "section" =>
        $section,

    "attendance_date" =>
        $attendance_date,

    "total_students" =>
        $total_students,

    "present_count" =>
        $present_count,

    "absent_count" =>
        $absent_count,

    "not_marked_count" =>
        $not_marked_count,

    "marked_students" =>
        $marked_students,

    "attendance_rate" =>
        $attendance_rate
]);


$stmt->close();

$conn->close();

?>