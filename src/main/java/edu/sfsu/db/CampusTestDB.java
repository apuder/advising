package edu.sfsu.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class CampusTestDB extends DB implements CampusDB {

    final static protected String DB_NAME = "ADVISING";
    final static protected String KEY_STUDENT_ID = "STUDENT_ID";



    public CampusTestDB(String driver, String url, String user, String passwd) {
        super(driver, url, user, passwd);

    }

   /* public Student getStudent(String id) {
        if (!id.equals("123456789")) {
            return null;
        }
        Student student = new Student(id);
        student.id = "123456789";
        student.firstName = "John";
        student.lastName = "Doe";
        student.email = "john@doe.com";
        Course course = new Course();
        course.courseName = "CSC415";
        course.grade = "A-";
        course.semester = "Spring 2018";
        student.courses.add(course);
        return student;
    }*/

    public Student getStudent(String id) {
        Connection connection = null;
        Student student = null;

        try {
            connection = getConnection();
            connection.setCatalog(DB_NAME);
            String query = "select STUDENT_FIRST_NAME, STUDENT_LAST_NAME, STUDENT_EMAIL from student_list_tester where " + KEY_STUDENT_ID + " = ?";
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                student = new Student(id);
                student.firstName = rs.getString("STUDENT_FIRST_NAME");
                student.lastName = rs.getString("STUDENT_LAST_NAME");
                student.email = rs.getString("STUDENT_EMAIL");


                query = "select GRADE, CLASS_NAME, SEMESTER_TAKEN from class_list_tester where " + KEY_STUDENT_ID + " = ?";
                ps = connection.prepareStatement(query);
                ps.setString(1, id);
                ResultSet rs_g = ps.executeQuery();

                while (rs_g.next()) {
                    Course course = new Course();
                    String course_name = rs_g.getString("CLASS_NAME");
                    Matcher matcher = Pattern.compile("\\d+").matcher(course_name);
                    matcher.find();
                    String numerical = matcher.group();
                    String[] parts = course_name.split(numerical);
                    if (parts.length > 1)
                        numerical = numerical+parts[1];
                    course.courseName = parts[0] + " " + numerical;
                    course.grade = scoreToGrade(rs_g.getDouble("GRADE"));
                    course.semester = rs_g.getString("SEMESTER_TAKEN");
                    student.courses.add(course);

                }
                rs_g.close();
                rs.close();


                connection.close();
                System.out.println("Done");
                System.out.println(student.lastName);
                return student;

            }


        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                }
            }

        }

        return student;
    }

    private String scoreToGrade(double score){
        if (score > 92)
            return "A";
        else if  (score > 89)
            return "A-";
        else if  (score > 86)
            return "B+";
        else if  (score > 82)
            return "B";
        else if  (score > 79)
            return "B-";
        else if  (score > 76)
            return "C+";
        else if  (score > 72)
            return "C";
        else if  (score > 69)
            return "C-";
        else if  (score > 66)
            return "D+";
        else if  (score > 62)
            return "D";
        else if  (score > 60)
            return "D-";
        else
            return "F";
    }

    public void getStudentInfo(List<Student> students) {
    }


}
