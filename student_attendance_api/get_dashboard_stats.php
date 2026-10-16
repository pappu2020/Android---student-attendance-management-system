
<?php

header("Content-Type: application/json; charset=UTF-8");

require_once "auth.php";

mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);

function sendDashboardResponse(int $status, array $data): void
{
    http_response_code($status);

    echo json_encode(
        $data,
        JSON_UNESCAPED_UNICODE
    );

    exit;
}

try {
    // 1. Allow POST requests only.
    if ($_SERVER["REQUEST_METHOD"] !== "POST") {
        header("Allow: POST");

        sendDashboardResponse(405, [
            "success" => false,
            "message" => "Only POST method is allowed"
        ]);
    }

    // 2. Authenticate the current user.
    $user = requireAuth();

    $userId = (int) ($user["id"] ?? 0);
    $role = strtolower(trim($user["role"] ?? ""));

    if (!in_array($role, ["admin", "teacher"], true)) {
        sendDashboardResponse(403, [
            "success" => false,
            "message" => "Permission denied"
        ]);
    }

    global $conn;

    if (!isset($conn) || !($conn instanceof mysqli)) {
        throw new RuntimeException(
            "Database connection unavailable"
        );
    }

    $today = date("Y-m-d");

    /*
     * Match class naming formats:
     * 6 / Class 6
     *
     * Match section naming formats:
     * A / Section A
     */
    $assignmentCondition = "
        EXISTS (
            SELECT 1
            FROM class_routines AS r
            WHERE r.teacher_user_id = ?
              AND r.is_active = 1

              AND (
                    TRIM(r.class_name) =
                        TRIM(s.class_name)

                    OR TRIM(r.class_name) =
                        CONCAT(
                            'Class ',
                            TRIM(s.class_name)
                        )

                    OR CONCAT(
                        'Class ',
                        TRIM(r.class_name)
                    ) = TRIM(s.class_name)
              )

              AND (
                    UPPER(TRIM(r.section)) =
                        UPPER(TRIM(s.section))

                    OR UPPER(TRIM(r.section)) =
                        CONCAT(
                            'SECTION ',
                            UPPER(TRIM(s.section))
                        )

                    OR CONCAT(
                        'SECTION ',
                        UPPER(TRIM(r.section))
                    ) = UPPER(TRIM(s.section))
              )
        )
    ";

    // 3. Count students visible to the current user.
    if ($role === "admin") {
        $totalSql = "
            SELECT COUNT(*) AS total_students
            FROM students
        ";

        $totalResult = $conn->query($totalSql);
        $totalRow = $totalResult->fetch_assoc();

        $totalStudents = (int) $totalRow["total_students"];

    } else {
        $totalSql = "
            SELECT COUNT(*) AS total_students
            FROM students AS s
            WHERE $assignmentCondition
        ";

        $totalStmt = $conn->prepare($totalSql);
        $totalStmt->bind_param("i", $userId);
        $totalStmt->execute();

        $totalRow = $totalStmt
            ->get_result()
            ->fetch_assoc();

        $totalStudents = (int) $totalRow["total_students"];

        $totalStmt->close();
    }

    /*
     * 4. Aggregate duplicate attendance records.
     * Each student counts at most once for today.
     * Present takes priority if statuses conflict.
     */
    $attendanceSql = "
        SELECT
            COUNT(*) AS marked_count,

            COALESCE(
                SUM(
                    CASE
                        WHEN d.daily_status = 'Present'
                            THEN 1
                        ELSE 0
                    END
                ),
                0
            ) AS present_count,

            COALESCE(
                SUM(
                    CASE
                        WHEN d.daily_status = 'Absent'
                            THEN 1
                        ELSE 0
                    END
                ),
                0
            ) AS absent_count

        FROM (
            SELECT
                student_id,
                attendance_date,

                CASE
                    WHEN SUM(status = 'Present') > 0
                        THEN 'Present'
                    ELSE 'Absent'
                END AS daily_status

            FROM attendance

            WHERE attendance_date = ?

            GROUP BY student_id, attendance_date
        ) AS d

        INNER JOIN students AS s
            ON s.id = d.student_id
    ";

    // 5. Restrict Teacher statistics to assigned students.
    if ($role === "teacher") {
        $attendanceSql .= "
            WHERE $assignmentCondition
        ";
    }

    $attendanceStmt = $conn->prepare($attendanceSql);

    if ($role === "teacher") {
        $attendanceStmt->bind_param(
            "si",
            $today,
            $userId
        );
    } else {
        $attendanceStmt->bind_param("s", $today);
    }

    $attendanceStmt->execute();

    $attendanceRow = $attendanceStmt
        ->get_result()
        ->fetch_assoc();

    $attendanceStmt->close();

    $markedCount = (int) $attendanceRow["marked_count"];
    $presentCount = (int) $attendanceRow["present_count"];
    $absentCount = (int) $attendanceRow["absent_count"];

    // 6. Calculate students whose attendance is not marked.
    $notMarkedCount = max(
        0,
        $totalStudents - $markedCount
    );

    // 7. Calculate attendance percentage among marked students.
    $attendancePercentage = $markedCount > 0
        ? round(
            ($presentCount / $markedCount) * 100,
            2
        )
        : 0;

    // 8. Preserve existing Android response fields.
    sendDashboardResponse(200, [
        "success" => true,
        "message" => "Dashboard statistics fetched successfully",
        "date" => $today,
        "total_students" => $totalStudents,
        "present_count" => $presentCount,
        "absent_count" => $absentCount,
        "marked_count" => $markedCount,
        "not_marked_count" => $notMarkedCount,
        "attendance_percentage" => $attendancePercentage
    ]);

} catch (Throwable $e) {
    error_log(
        "get_dashboard_stats.php error: " .
        $e->getMessage()
    );

    sendDashboardResponse(500, [
        "success" => false,
        "message" => "Failed to fetch dashboard statistics"
    ]);

} finally {
    if (isset($conn) && $conn instanceof mysqli) {
        $conn->close();
    }
}
?>
