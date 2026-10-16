
package com.example.studentattendance;

public class TimetableItem {

    private final String subjectName;
    private final String startTime;
    private final String endTime;
    private final String teacherName;

    public TimetableItem(
            String subjectName,
            String startTime,
            String endTime,
            String teacherName) {

        this.subjectName = subjectName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.teacherName = teacherName;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public String getTeacherName() {
        return teacherName;
    }
}
