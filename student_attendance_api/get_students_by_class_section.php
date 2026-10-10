
<?php

header("Content-Type: application/json; charset=UTF-8");

require_once "auth.php";

mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);

// ======================================================
// Only POST Method
// ======================================================

if ($_SERVER["REQUEST_METHOD"] !== "POST") {

    http_response_code(405);

    echo json_encode([
        "success" => false,
        "message" => "Only POST method is allowed"
    ]);

    exit;
}

try {

    // ======================================================
    // Authenticate Admin or Teacher
    // ======================================================

    $user = requireAuth();

    // Both Admin and Teacher can view students.
    $allowedRoles = ["admin", "teacher"];

    if (!in_array($user["role"], $allowedRoles, true)) {

        http_response_code(403);

        echo json_encode([
            "success" => false,
            "message" => "You do not have permission to view students"
        ]);

        exit;
    }

    // ======================================================
    // Get Class and Section
    // ======================================================

    $class_name = isset($_POST["class_name"])
        ? trim($_POST["class_name"])
        : "";

    $section = isset($_POST["section"])
        ? trim($_POST["section"])
        : "";

    // ======================================================
    // Validation
    // ======================================================

    if ($class_name === "" || $section === "") {

        http_response_code(400);

        echo json_encode([
            "success" => false,
            "message" => "Class and section are required"
        ]);

        exit;
    }

    // ======================================================
    // SQL Query
    // ======================================================

    $sql = "SELECT
                id,
                name,
                roll_number,
                class_name,
                section,
                email,
                phone,
                photo
            FROM students
            WHERE class_name = ?
            AND section = ?
            ORDER BY roll_number ASC";

    // ======================================================
    // Prepare Statement
    // ======================================================

    $stmt = $conn->prepare($sql);

    // ======================================================
    // Bind Parameters
    // ======================================================

    $stmt->bind_param(
        "ss",
        $class_name,
        $section
    );

    // ======================================================
    // Execute Query
    // ======================================================

    $stmt->execute();

    $result = $stmt->get_result();

    // ======================================================
    // Fetch Students
    // ======================================================

    $students = [];

    while ($row = $result->fetch_assoc()) {

        $students[] = $row;
    }

    // ======================================================
    // JSON Response
    // ======================================================

    echo json_encode([
        "success" => true,
        "message" => "Students fetched successfully",
        "class_name" => $class_name,
        "section" => $section,
        "total_students" => count($students),
        "students" => $students
    ]);

    // ======================================================
    // Close Resources
    // ======================================================

    $stmt->close();
    $conn->close();

} catch (Throwable $e) {

    http_response_code(500);

    echo json_encode([
        "success" => false,
        "message" => "An internal server error occurred"
    ]);

    error_log("get_students_by_class_section.php: "
        . $e->getMessage());
}

?>
