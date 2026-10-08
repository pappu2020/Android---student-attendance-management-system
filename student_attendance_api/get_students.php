<?php

header("Content-Type: application/json; charset=UTF-8");

require_once "db.php";

if ($_SERVER["REQUEST_METHOD"] !== "GET") {

    echo json_encode([
        "success" => false,
        "message" => "Only GET method is allowed"
    ]);

    exit;
}

$sql = "SELECT id, name, roll_number, email, phone, created_at
        FROM students
        ORDER BY id DESC";

$result = $conn->query($sql);

if (!$result) {

    echo json_encode([
        "success" => false,
        "message" => "Failed to fetch students"
    ]);

    exit;
}

$students = [];

while ($row = $result->fetch_assoc()) {

    $students[] = [
        "id" => $row["id"],
        "name" => $row["name"],
        "roll_number" => $row["roll_number"],
        "email" => $row["email"],
        "phone" => $row["phone"],
        "created_at" => $row["created_at"]
    ];
}

echo json_encode([
    "success" => true,
    "message" => "Students fetched successfully",
    "students" => $students
]);

$conn->close();

?>