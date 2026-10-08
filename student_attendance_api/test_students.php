<?php

require_once "db.php";

$sql = "SELECT id, student_id, name, roll_number, department, semester
        FROM students
        ORDER BY id ASC";

$result = $conn->query($sql);

$students = [];

if ($result) {

    while ($row = $result->fetch_assoc()) {
        $students[] = $row;
    }

    echo json_encode([
        "success" => true,
        "students" => $students
    ]);

} else {

    http_response_code(500);

    echo json_encode([
        "success" => false,
        "message" => "Failed to retrieve students"
    ]);
}

$conn->close();

?>