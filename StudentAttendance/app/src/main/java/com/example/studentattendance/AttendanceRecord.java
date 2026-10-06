package com.example.studentattendance;

public class AttendanceRecord {

    private String studentId;
    private String status;

    public AttendanceRecord(String studentId, String status) {
        this.studentId = studentId;
        this.status = status;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}