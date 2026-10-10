<?php
require_once "auth.php";

header("Content-Type: application/json; charset=UTF-8");

mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);

try {
    // Verify the user's Bearer Token.
    requireAuth();

    global $conn;

    // Read and validate the request parameters.
    $classInput = isset($_POST["class_name"])
        ? trim($_POST["class_name"])
        : "";

    $section = isset($_POST["section"])
        ? strtoupper(trim($_POST["section"]))
        : "";

    // Validate class: 6 to 12.
    if (
        $classInput === "" ||
        !ctype_digit($classInput) ||
        (int)$classInput < 6 ||
        (int)$classInput > 12
    ) {
        http_response_code(400);

        echo json_encode([
            "success" => false,
            "message" => "Select a valid class between 6 and 12."
        ]);

        exit;
    }

    // Validate section.
    if (!in_array($section, ["A", "B", "C"], true)) {
        http_response_code(400);

        echo json_encode([
            "success" => false,
            "message" => "Select a valid section: A, B or C."
        ]);

        exit;
    }

    $classNumber = (int)$classInput;

    /*
     * Support both database formats:
     *   6, 7, 8 ...
     *   Class 6, Class 7, Class 8 ...
     *
     * The Android app sends the numeric class number.
     */

    $numericClass = (string)$classNumber;
    $prefixedClass = "Class " . $classNumber;

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
                SUM(
                    CASE
                        WHEN a.status = 'Present' THEN 1
                        ELSE 0
                    END
                ),
                0
            ) AS present_classes,

            CASE
                WHEN COUNT(a.id) = 0 THEN 0
                ELSE ROUND(
                    SUM(
                        CASE
                            WHEN a.status = 'Present' THEN 1
                            ELSE 0
                        END
                    ) * 100.0 / COUNT(a.id),
                    2
                )
            END AS attendance_percentage

        FROM students AS s

        LEFT JOIN attendance AS a
            ON a.student_id = s.id

        WHERE
            (
                TRIM(s.class_name) = ?
                OR TRIM(s.class_name) = ?
            )
            AND UPPER(TRIM(s.section)) = ?

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

        ORDER BY
            attendance_percentage ASC,
            s.roll_number ASC
    ";

    $stmt = $conn->prepare($sql);

    $stmt->bind_param(
        "sss",
        $numericClass,
        $prefixedClass,
        $section
    );

    $stmt->execute();

    $result = $stmt->get_result();

    $students = [];

    while ($row = $result->fetch_assoc()) {

        $totalClasses = (int)$row["total_classes"];
        $presentClasses = (int)$row["present_classes"];
        $percentage = (float)$row["attendance_percentage"];

        $row["id"] = (int)$row["id"];
        $row["total_classes"] = $totalClasses;
        $row["present_classes"] = $presentClasses;
        $row["attendance_percentage"] = $percentage;

        $row["attendance_status"] =
            $totalClasses === 0
                ? "No records"
                : "Below 75%";

        $students[] = $row;
    }

    $stmt->close();

    echo json_encode([
        "success" => true,
        "class_name" => $numericClass,
        "section" => $section,
        "threshold" => 75,
        "count" => count($students),
        "students" => $students
    ]);

} catch (Throwable $e) {

    // Keep technical details in the server log, not the API response.
    error_log(
        "Low attendance API error: " . $e->getMessage()
    );

    http_response_code(500);

    echo json_encode([
        "success" => false,
        "message" => "Unable to load low attendance students."
    ]);
}
?>
