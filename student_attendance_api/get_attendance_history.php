<?php

header("Content-Type: application/json; charset=UTF-8");

require_once "db.php";


/*
|--------------------------------------------------------------------------
| Only POST method is allowed
|--------------------------------------------------------------------------
*/

if ($_SERVER["REQUEST_METHOD"] !== "POST") {

    http_response_code(405);

    echo json_encode([
        "success" => false,
        "message" => "Only POST method is allowed"
    ]);

    exit;
}


/*
|--------------------------------------------------------------------------
| Receive input
|--------------------------------------------------------------------------
*/

$class_name = isset($_POST["class_name"])
    ? trim($_POST["class_name"])
    : "";

$section = isset($_POST["section"])
    ? trim($_POST["section"])
    : "";

$attendance_date = isset($_POST["attendance_date"])
    ? trim($_POST["attendance_date"])
    : "";


/*
|--------------------------------------------------------------------------
| Validation
|--------------------------------------------------------------------------
*/

if ($class_name === "") {

    http_response_code(400);

    echo json_encode([
        "success" => false,
        "message" => "Class is required"
    ]);

    exit;
}


if ($section === "") {

    http_response_code(400);

    echo json_encode([
        "success" => false,
        "message" => "Section is required"
    ]);

    exit;
}


if ($attendance_date === "") {

    http_response_code(400);

    echo json_encode([
        "success" => false,
        "message" => "Attendance date is required"
    ]);

    exit;
}


/*
|--------------------------------------------------------------------------
| Get students and attendance
|--------------------------------------------------------------------------
|
| LEFT JOIN is used so that students without attendance
| record for the selected date are also displayed.
|
*/

$sql = "
    SELECT
        s.id,
        s.name,
        s.roll_number,
        s.class_name,
        s.section,
        s.email,
        s.phone,
        s.photo,
        a.attendance_date,
        COALESCE(a.status, 'Not Marked') AS status

    FROM students s

    LEFT JOIN attendance a
        ON s.id = a.student_id
        AND a.attendance_date = ?

    WHERE s.class_name = ?
        AND s.section = ?

    ORDER BY
        CAST(s.roll_number AS UNSIGNED) ASC,
        s.roll_number ASC
";


$stmt = $conn->prepare($sql);


if (!$stmt) {

    http_response_code(500);

    echo json_encode([
        "success" => false,
        "message" => "Failed to prepare database query"
    ]);

    exit;
}


/*
|--------------------------------------------------------------------------
| Bind parameters
|--------------------------------------------------------------------------
*/

$stmt->bind_param(
    "sss",
    $attendance_date,
    $class_name,
    $section
);


/*
|--------------------------------------------------------------------------
| Execute query
|--------------------------------------------------------------------------
*/

if (!$stmt->execute()) {

    http_response_code(500);

    echo json_encode([
        "success" => false,
        "message" => "Failed to fetch attendance history"
    ]);

    $stmt->close();
    $conn->close();

    exit;
}


$result = $stmt->get_result();


/*
|--------------------------------------------------------------------------
| Prepare response
|--------------------------------------------------------------------------
*/

$students = [];

$total_students = 0;
$present_count = 0;
$absent_count = 0;


while ($row = $result->fetch_assoc()) {

    $status = $row["status"];

    if ($status === "Present") {
        $present_count++;
    }

    if ($status === "Absent") {
        $absent_count++;
    }

    $total_students++;

    $students[] = [
        "id" => $row["id"],
        "name" => $row["name"],
        "roll_number" => $row["roll_number"],
        "class_name" => $row["class_name"],
        "section" => $row["section"],
        "email" => $row["email"],
        "phone" => $row["phone"],
        "photo" => $row["photo"],
        "attendance_date" => $attendance_date,
        "status" => $status
    ];
}


/*
|--------------------------------------------------------------------------
| Send JSON response
|--------------------------------------------------------------------------
*/

echo json_encode([
    "success" => true,
    "message" => "Attendance history fetched successfully",

    "class_name" => $class_name,

    "section" => $section,

    "attendance_date" => $attendance_date,

    "total_students" => $total_students,

    "present_count" => $present_count,

    "absent_count" => $absent_count,

    "not_marked_count" =>
        $total_students
        - $present_count
        - $absent_count,

    "students" => $students
]);


$stmt->close();

$conn->close();

?>