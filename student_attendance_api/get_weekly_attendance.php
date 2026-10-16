
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

// 1. POST requests only.
if ($_SERVER["REQUEST_METHOD"] !== "POST") {
    header("Allow: POST");

    sendJsonResponse(405, [
        "success" => false,
        "message" => "Only POST method is allowed"
    ]);
}

try {
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

    if (!isset($conn) || !($conn instanceof mysqli)) {
        throw new RuntimeException(
            "Database connection unavailable"
        );
    }

    /*
     * 3. Aggregate attendance by student and date.
     * If duplicate records exist, Present takes priority.
     */
    $sql = "
        SELECT
            d.attendance_date,

            SUM(
                CASE
                    WHEN d.daily_status = 'Present'
                        THEN 1
                    ELSE 0
                END
            ) AS present_count,

            SUM(
                CASE
                    WHEN d.daily_status = 'Absent'
                        THEN 1
                    ELSE 0
                END
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

            WHERE attendance_date >= DATE_SUB(
                CURDATE(),
                INTERVAL 6 DAY
            )
            AND attendance_date <= CURDATE()

            GROUP BY student_id, attendance_date
        ) AS d

        INNER JOIN students AS s
            ON s.id = d.student_id
    ";

    // 4. Restrict Teacher to assigned class/sections.
    if ($role === "teacher") {
        $sql .= "
            WHERE EXISTS (
                SELECT 1
                FROM class_routines AS cr

                WHERE cr.teacher_user_id = ?
                  AND cr.is_active = 1

                  AND (
                      TRIM(cr.class_name) =
                          TRIM(s.class_name)

                      OR TRIM(cr.class_name) =
                          CONCAT(
                              'Class ',
                              TRIM(s.class_name)
                          )

                      OR CONCAT(
                          'Class ',
                          TRIM(cr.class_name)
                      ) = TRIM(s.class_name)
                  )

                  AND (
                      UPPER(TRIM(cr.section)) =
                          UPPER(TRIM(s.section))

                      OR UPPER(TRIM(cr.section)) =
                          CONCAT(
                              'SECTION ',
                              UPPER(TRIM(s.section))
                          )

                      OR CONCAT(
                          'SECTION ',
                          UPPER(TRIM(cr.section))
                      ) = UPPER(TRIM(s.section))
                  )
            )
        ";
    }

    $sql .= "
        GROUP BY d.attendance_date
        ORDER BY d.attendance_date ASC
    ";

    $stmt = $conn->prepare($sql);

    if ($role === "teacher") {
        $stmt->bind_param("i", $userId);
    }

    $stmt->execute();

    $result = $stmt->get_result();

    $attendanceByDate = [];

    while ($row = $result->fetch_assoc()) {
        $date = $row["attendance_date"];

        $attendanceByDate[$date] = [
            "present_count" => (int) $row["present_count"],
            "absent_count" => (int) $row["absent_count"]
        ];
    }

    $stmt->close();

    // 5. Generate all seven dates, including zero-record days.
    $weeklyData = [];

    for ($i = 6; $i >= 0; $i--) {
        $date = date(
            "Y-m-d",
            strtotime("-" . $i . " days")
        );

        $counts = $attendanceByDate[$date] ?? [
            "present_count" => 0,
            "absent_count" => 0
        ];

        $weeklyData[] = [
            "date" => $date,
            "day" => date("D", strtotime($date)),
            "present_count" => $counts["present_count"],
            "absent_count" => $counts["absent_count"]
        ];
    }

    // 6. Preserve the existing Android response fields.
    sendJsonResponse(200, [
        "success" => true,
        "message" => "Weekly attendance fetched successfully",
        "weekly_data" => $weeklyData
    ]);

} catch (Throwable $e) {
    error_log(
        "get_weekly_attendance.php error: " .
        $e->getMessage()
    );

    sendJsonResponse(500, [
        "success" => false,
        "message" => "Failed to fetch weekly attendance"
    ]);

} finally {
    if (isset($conn) && $conn instanceof mysqli) {
        $conn->close();
    }
}
?>
