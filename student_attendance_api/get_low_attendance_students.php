
<?php

header("Content-Type: application/json; charset=UTF-8");

require_once "auth.php";

mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);

function sendJsonResponse(int $statusCode, array $data): void
{
    http_response_code($statusCode);

    echo json_encode(
        $data,
        JSON_UNESCAPED_UNICODE
    );

    exit;
}

try {

    // 1. Allow GET requests only.
    if ($_SERVER["REQUEST_METHOD"] !== "GET") {
        header("Allow: GET");

        sendJsonResponse(405, [
            "success" => false,
            "message" => "Only GET requests are allowed"
        ]);
    }

    // 2. Authenticate the user.
    $user = requireAuth();

    $userId = (int) ($user["id"] ?? 0);
    $role = strtolower(trim($user["role"] ?? ""));

    if (!in_array($role, ["admin", "teacher"], true)) {
        sendJsonResponse(403, [
            "success" => false,
            "message" => "Admin or Teacher permission required"
        ]);
    }

    global $conn;

    // 3. Aggregate attendance once per student per date.
    // Present takes priority if conflicting duplicate records exist.
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

            COUNT(a.attendance_date) AS total_classes,

            COALESCE(
                SUM(
                    CASE
                        WHEN a.status = 'Present' THEN 1
                        ELSE 0
                    END
                ),
                0
            ) AS present_classes,

            COALESCE(
                SUM(
                    CASE
                        WHEN a.status = 'Absent' THEN 1
                        ELSE 0
                    END
                ),
                0
            ) AS absent_classes

        FROM students AS s

        LEFT JOIN (
            SELECT
                student_id,
                attendance_date,

                CASE
                    WHEN SUM(status = 'Present') > 0
                        THEN 'Present'
                    WHEN SUM(status = 'Absent') > 0
                        THEN 'Absent'
                    ELSE NULL
                END AS status

            FROM attendance

            GROUP BY
                student_id,
                attendance_date
        ) AS a
            ON a.student_id = s.id
    ";

    // 4. Restrict teachers to their assigned classes/sections.
    if ($role === "teacher") {
        $sql .= "
            WHERE EXISTS (
                SELECT 1
                FROM class_routines AS r
                WHERE r.teacher_user_id = ?
                  AND r.class_name = s.class_name
                  AND r.section = s.section
                  AND r.is_active = 1
            )
        ";
    }

    $sql .= "
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

        HAVING
            COUNT(a.attendance_date) = 0
            OR (
                SUM(
                    CASE
                        WHEN a.status = 'Present' THEN 1
                        ELSE 0
                    END
                ) * 100.0
                / NULLIF(COUNT(a.attendance_date), 0)
            ) < 75

        ORDER BY
            CASE
                WHEN COUNT(a.attendance_date) = 0 THEN 0
                ELSE 1
            END ASC,

            CASE
                WHEN COUNT(a.attendance_date) = 0 THEN 0
                ELSE (
                    SUM(
                        CASE
                            WHEN a.status = 'Present' THEN 1
                            ELSE 0
                        END
                    ) * 100.0
                    / NULLIF(COUNT(a.attendance_date), 0)
                )
            END ASC,

            s.name ASC
    ";

    $stmt = $conn->prepare($sql);

    if ($role === "teacher") {
        $stmt->bind_param("i", $userId);
    }

    $stmt->execute();

    $result = $stmt->get_result();

    // 5. Build the student list.
    $students = [];

    while ($row = $result->fetch_assoc()) {

        $totalClasses = (int) $row["total_classes"];
        $presentClasses = (int) $row["present_classes"];
        $absentClasses = (int) $row["absent_classes"];

        $percentage = $totalClasses > 0
            ? ($presentClasses / $totalClasses) * 100
            : 0;

        $row["id"] = (int) $row["id"];
        $row["total_classes"] = $totalClasses;
        $row["present_classes"] = $presentClasses;
        $row["absent_classes"] = $absentClasses;
        $row["attendance_percentage"] = round(
            $percentage,
            2
        );

        $row["attendance_status"] = $totalClasses === 0
            ? "No records"
            : "Below 75%";

        $students[] = $row;
    }

    $stmt->close();

    // 6. Preserve the original response fields.
    sendJsonResponse(200, [
        "success" => true,
        "count" => count($students),
        "students" => $students
    ]);

} catch (Throwable $e) {

    error_log(
        "get_low_attendance_students.php error: " .
        $e->getMessage()
    );

    sendJsonResponse(500, [
        "success" => false,
        "message" => "Unable to load low attendance students"
    ]);

} finally {

    if (isset($conn) && $conn instanceof mysqli) {
        $conn->close();
    }
}
?>
