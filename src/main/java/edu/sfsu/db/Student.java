package edu.sfsu.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    public String              checkpointAdvising413;
    public String              checkpointSubmittedApplication;
    public List<Course>        courses;
    public Map<String, String> comments;


    public Course requirementFor(Object course) {
        if (course instanceof String) {
            return requirementFor((String) course);
        }
        // List of alternative courses
        for (Object courseName : (List<Object>) course) {
            Course c = requirementFor((String) courseName);
            if (c != null) {
                return c;
            }
        }
        return null;
    }

    private Course requirementFor(String courseName) {
        Course wip = null;
        for (Course course : courses) {
            if (course.courseName.equals(courseName)) {
                if (course.grade.equals("")) {
                    // Course taken this semester
                    wip = course;
                    continue;
                }
                //if (!course.isPassingGrade()) {
                //    continue;
                //}
                return course;
            }
        }
        return wip;
    }
    public List<Course> collapseCourses(){
        HashMap<String, Course> temp_map = new HashMap<>();
        //System.out.println("collapse course");
        for (Course course : courses) {
            if (temp_map.containsKey(course.courseName)){
                Course temp = temp_map.get(course.courseName);
                temp.semester = shortenSemester(temp.semester) + "/" +course.semester;
                temp.grade +="/"+course.grade;
                temp_map.put(temp.courseName, temp);
            }
            else{
                Course new_course = new Course();
                new_course.courseName = course.courseName;
                new_course.semester = course.semester;
                new_course.grade = course.grade;
                new_course.transferSchool = course.transferSchool;
                new_course.transferSchoolType = course.transferSchoolType;
                new_course.transferCourse = course.transferCourse;
                temp_map.put(course.courseName, new_course);
            }

        }
        List<Course> new_course_list = new ArrayList<>();
        Set<String> keys = temp_map.keySet();
        for (String key : keys){
            //System.out.println(temp_map.get(key).courseName);
            //System.out.println(temp_map.get(key).grade);

            new_course_list.add(temp_map.get(key));
        }
        return new_course_list;
    }
    private String shortenSemester(String semester){
        //System.out.println(semester);
        if (semester.contains("/")) {
            String[] temp_arr = semester.split("/");
            String new_part = shortenSemester(temp_arr[temp_arr.length-1]);
            String old_part = temp_arr[0];
            for (int i = 1; i < temp_arr.length-1; i++){
                old_part +="/"+ temp_arr[i];
            }
            return old_part+"/"+new_part;
        }
        String [] parts = semester.split(" ");
        String year = parts[1].substring(2,4);
        String sems = parts[0].substring(0,2);
        return sems+year;
    }
}
