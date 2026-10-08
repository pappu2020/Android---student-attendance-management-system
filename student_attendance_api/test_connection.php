<?php

require_once "db.php";

echo json_encode([
    "success" => true,
    "message" => "Database connection is working successfully"
]);

?>