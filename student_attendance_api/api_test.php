<?php

header("Content-Type: application/json");

$response = [
    "success" => true,
    "message" => "Student Attendance API is working",
    "server" => "PHP",
    "format" => "JSON"
];

echo json_encode($response);

?>