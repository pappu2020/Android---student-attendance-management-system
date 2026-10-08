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


$student_id = isset($_POST["student_id"])
    ? intval($_POST["student_id"])
    : 0;


if ($student_id <= 0) {

    http_response_code(400);

    echo json_encode([
        "success" => false,
        "message" => "Valid student ID is required"
    ]);

    exit;
}


/*
|--------------------------------------------------------------------------
| GET STUDENT
|--------------------------------------------------------------------------
*/

$studentSql = "
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
    WHERE id = ?
";


$studentStmt = $conn->prepare($studentSql);


if (!$studentStmt) {

    http_response_code(500);

    echo json_encode([
        "success" => false,
        "message" => "Failed to prepare student query"
    ]);

    exit;
}


$studentStmt->bind_param(
    "i",
    $student_id
);


$studentStmt->execute();


$studentResult =
    $studentStmt->get_result();


if ($studentResult->num_rows === 0) {

    http_response_code(404);

    echo json_encode([
        "success" => false,
        "message" => "Student not found"
    ]);

    $studentStmt->close();
    $conn->close();

    exit;
}


$student =
    $studentResult->fetch_assoc();


$studentStmt->close();


/*
|--------------------------------------------------------------------------
| GET ATTENDANCE
|--------------------------------------------------------------------------
*/

$attendanceSql = "
    SELECT
        attendance_date,
        status
    FROM attendance
    WHERE student_id = ?
    ORDER BY attendance_date DESC
";


$attendanceStmt =
    $conn->prepare(
        $attendanceSql
    );


if (!$attendanceStmt) {

    http_response_code(500);

    echo json_encode([
        "success" => false,
        "message" => "Failed to prepare attendance query"
    ]);

    $conn->close();

    exit;
}


$attendanceStmt->bind_param(
    "i",
    $student_id
);


$attendanceStmt->execute();


$attendanceResult =
    $attendanceStmt->get_result();


$attendance = [];

$totalRecords = 0;
$presentCount = 0;
$absentCount = 0;


while (
    $row =
    $attendanceResult->fetch_assoc()
) {

    $attendanceDate =
        $row["attendance_date"];

    $status =
        $row["status"];


    if (
        $status === "Present"
    ) {

        $presentCount++;
    }


    if (
        $status === "Absent"
    ) {

        $absentCount++;
    }


    $totalRecords++;


    $attendance[] = [

        "attendance_date" =>
            $attendanceDate,

        "status" =>
            $status
    ];
}


$attendancePercentage = 0;


if ($totalRecords > 0) {

    $attendancePercentage =
        (
            $presentCount * 100
        )
        /
        $totalRecords;
}


/*
|--------------------------------------------------------------------------
| FINAL RESPONSE
|--------------------------------------------------------------------------
*/

$response = [

    "success" => true,

    "message" =>
        "Student attendance details fetched successfully",

    "student" => [

        "id" =>
            $student["id"],

        "name" =>
            $student["name"],

        "roll_number" =>
            $student["roll_number"],

        "class_name" =>
            $student["class_name"],

        "section" =>
            $student["section"],

        "email" =>
            $student["email"],

        "phone" =>
            $student["phone"],

        "photo" =>
            $student["photo"]
    ],

    "summary" => [

        "total_records" =>
            $totalRecords,

        "present_count" =>
            $presentCount,

        "absent_count" =>
            $absentCount,

        "attendance_percentage" =>
            round(
                $attendancePercentage,
                2
            )
    ],

    "attendance" =>
        $attendance
];


echo json_encode(
    $response,
    JSON_UNESCAPED_UNICODE
);


$attendanceStmt->close();

$conn->close();

?>