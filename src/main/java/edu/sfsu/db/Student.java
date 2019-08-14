package edu.sfsu.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Student {

    public Student(String id) {
        this.id = id;
        firstName = "";
        lastName = "";
        email = "";
        comment = "";
        courses = new ArrayList<>();
        comments = new HashMap<>();
    }


    public String              id;
    public String              firstName;
    public String              lastName;
    public String              email;
    public String              comment;
    public String              checkpointOralPresentation;
    public String              checkpointSubmittedApplication;
    public List<Course>        courses;
    public Map<String, String> comments;


    public Requirement requirementFor(Object course) {
        if (course instanceof String) {
            return requirementFor((String) course);
        }
        // List of alternative courses
        for (Object courseName : (List<Object>) course) {
            Requirement c = requirementFor((String) courseName);
            if (c != null) {
                return c;
            }
        }
        return null;
    }

    private Requirement requirementFor(String courseName) {
        Requirement req = new Requirement(courseName);
        for (Course course : courses) {
            if (course.courseName.equals(courseName)) {
                if (course.grade.equals("")) {
                    // Course taken this semester
                    //wip = course;
                    req.addGrade("IP"); //mark class as in progress
                }
                else if (!course.isPassingGrade()) {
                    req.addGrade(course); // if its not a passing grade,  its just an attempt and we record it as such.
                }
                else                      //
                    req.setPassedCourse(course);//passed course saved as passing course.
                // Saving a course either by "addGrade" or "setPassedCourse" sets the last_sem field of the requirement
                // class, allowing the most recent attempt or the semester the course was passed to be displayed.
            }
        }
        if (req.gradeSequence == null)
            return null;
        return req;
    }
}
