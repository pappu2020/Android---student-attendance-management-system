
<?php
header("Content-Type: application/json; charset=UTF-8");

require_once "auth.php";
mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);

function sendJson(int $code, array $data): void
{
    http_response_code($code);
    echo json_encode($data, JSON_UNESCAPED_UNICODE);
    exit;
}

if ($_SERVER["REQUEST_METHOD"] !== "POST") {
    header("Allow: POST");
    sendJson(405, [
        "success" => false,
        "message" => "Only POST method is allowed"
    ]);
}

requireAdmin();

global $conn;

$idInput = trim((string)($_POST["teacher_id"] ?? ""));
$name = trim((string)($_POST["name"] ?? ""));
$email = strtolower(trim((string)($_POST["email"] ?? "")));
$newPassword = (string)($_POST["password"] ?? "");

if (
    $idInput === "" ||
    !ctype_digit($idInput) ||
    (int)$idInput < 1
) {
    sendJson(400, [
        "success" => false,
        "message" => "Valid teacher_id is required"
    ]);
}

$teacherId = (int)$idInput;

if ($name === "" || mb_strlen($name) > 100) {
    sendJson(400, [
        "success" => false,
        "message" => "Name is required and must not exceed 100 characters"
    ]);
}

if (
    $email === "" ||
    !filter_var($email, FILTER_VALIDATE_EMAIL) ||
    mb_strlen($email) > 150
) {
    sendJson(400, [
        "success" => false,
        "message" => "Enter a valid email address"
    ]);
}

if (
    $newPassword !== "" &&
    (
        strlen($newPassword) < 8 ||
        strlen($newPassword) > 72
    )
) {
    sendJson(400, [
        "success" => false,
        "message" => "Password must contain 8 to 72 bytes"
    ]);
}

try {
    $stmt = $conn->prepare(
        "SELECT id
         FROM users
         WHERE id = ?
           AND LOWER(TRIM(role)) = 'teacher'
         LIMIT 1"
    );

    $stmt->bind_param("i", $teacherId);
    $stmt->execute();

    $teacher = $stmt->get_result()->fetch_assoc();
    $stmt->close();

    if (!$teacher) {
        sendJson(404, [
            "success" => false,
            "message" => "Teacher account not found"
        ]);
    }

    // Check whether another account already uses this email.
    $stmt = $conn->prepare(
        "SELECT id
         FROM users
         WHERE email = ?
           AND id <> ?
         LIMIT 1"
    );

    $stmt->bind_param("si", $email, $teacherId);
    $stmt->execute();

    $emailExists = $stmt->get_result()->fetch_assoc();
    $stmt->close();

    if ($emailExists) {
        sendJson(409, [
            "success" => false,
            "message" => "This email is already in use"
        ]);
    }

    $conn->begin_transaction();

    if ($newPassword !== "") {
        $passwordHash = password_hash(
            $newPassword,
            PASSWORD_DEFAULT
        );

        if ($passwordHash === false) {
            throw new RuntimeException(
                "Password hashing failed"
            );
        }

        $stmt = $conn->prepare(
            "UPDATE users
             SET name = ?, email = ?, password = ?
             WHERE id = ?
               AND LOWER(TRIM(role)) = 'teacher'"
        );

        $stmt->bind_param(
            "sssi",
            $name,
            $email,
            $passwordHash,
            $teacherId
        );

        $stmt->execute();
        $stmt->close();

        // Revoke all existing sessions for this teacher.
        $stmt = $conn->prepare(
            "DELETE FROM user_tokens WHERE user_id = ?"
        );

        $stmt->bind_param("i", $teacherId);
        $stmt->execute();
        $stmt->close();

    } else {
        // Empty password means keep the current password.
        $stmt = $conn->prepare(
            "UPDATE users
             SET name = ?, email = ?
             WHERE id = ?
               AND LOWER(TRIM(role)) = 'teacher'"
        );

        $stmt->bind_param(
            "ssi",
            $name,
            $email,
            $teacherId
        );

        $stmt->execute();
        $stmt->close();
    }

    $conn->commit();

    sendJson(200, [
        "success" => true,
        "message" => $newPassword !== ""
            ? "Teacher profile and password updated. Please log in again."
            : "Teacher profile updated successfully",
        "teacher" => [
            "id" => $teacherId,
            "name" => $name,
            "email" => $email,
            "role" => "teacher"
        ],
        "password_changed" => $newPassword !== ""
    ]);

} catch (mysqli_sql_exception $e) {
    if ($conn->errno === 0) {
        // No active database error to report here.
    }

    try {
        $conn->rollback();
    } catch (Throwable $ignored) {
    }

    error_log("update_teacher.php database error: " . $e->getMessage());

    if ((int)$e->getCode() === 1062) {
        sendJson(409, [
            "success" => false,
            "message" => "Email address already exists"
        ]);
    }

    sendJson(500, [
        "success" => false,
        "message" => "Unable to update teacher profile"
    ]);

} catch (Throwable $e) {
    try {
        $conn->rollback();
    } catch (Throwable $ignored) {
    }

    error_log("update_teacher.php error: " . $e->getMessage());

    sendJson(500, [
        "success" => false,
        "message" => "Unable to update teacher profile"
    ]);
}
?>
