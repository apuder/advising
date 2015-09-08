package edu.sfsu.db;

import java.sql.*;

public class CampusDB extends DB {

    public CampusDB(String driver, String url, String user, String passwd) {
        super(driver, url, user, passwd);
    }

    public Student getStudent(String id) {
        Student student = new Student(id);
        Connection connection = null;
        try {
            connection = getConnection();
            // Courses taken at SFSU
            String query = "select * from CMSCOMMON.SFO_CR_MAIN_MV where emplid = ?";
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();

            boolean first = true;
            while (rs.next()) {
                if (first) {
                    first = false;
                    student.name = rs.getString("FIRST_NAME") + " " + rs.getString("LAST_NAME");
                    student.email = rs.getString("EMAIL_ADDR");
                }
                Course course = new Course();
                course.courseName = rs.getString("SUBJECT") + rs.getString("CATALOG_NBR");
                course.semester = rs.getString("STRM");
                course.grade = rs.getString("CRSE_GRADE_OFF").replaceAll(" ", "");
                course.transferred = false;
                student.courses.add(course);
            }
            rs.close();
            ps.close();

            // Courses transferred
            query = "select * from CMSCOMMON.SFO_CR_PREV_GRADE_TRNF_MV where emplid = ?";
            ps = connection.prepareStatement(query);
            ps.setString(1, id);
            rs = ps.executeQuery();

            while (rs.next()) {
                Course course = new Course();
                course.courseName = rs.getString("SUBJECT") + rs.getString("CATALOG_NBR");
                course.semester = "";
                course.grade = rs.getString("CRSE_GRADE_OFF");
                course.transferred = true;
                student.courses.add(course);
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            return null;
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                }
            }
        }
        return student.courses.size() == 0 ? null : student;
    }
}