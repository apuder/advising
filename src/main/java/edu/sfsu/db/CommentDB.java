package edu.sfsu.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CommentDB extends DB {

    final static protected String DB_NAME = "ADVISING";
    final static protected String TABLE_NAME = "COMMENTS";

    final static protected String KEY_STUDENT_ID = "STUDENT_ID";
    final static protected String KEY_COURSE = "COURSE";
    final static protected String KEY_COMMENT = "COMMENT";

    final static protected String KEY_TRANSCRIBED = "TRANSCRIBED";
    final static protected String KEY_UPDATED_BY = "UPDATED_BY";
    final static protected String KEY_LAST_UPDATED = "LAST_UPDATED";


    public CommentDB(String driver, String url, String user, String passwd) {
        super(driver, url, user, passwd);
        setupDB();
    }

    private void setupDB() {
        Connection connection = null;
        boolean hasDB = false;
        try {
            connection = getConnection();
            ResultSet resultSet = connection.getMetaData().getCatalogs();
            while (resultSet.next()) {
                String databaseName = resultSet.getString(1);
                if (databaseName.equalsIgnoreCase(DB_NAME)) {
                    hasDB = true;
                    break;
                }
            }
            resultSet.close();

            if (!hasDB) {
                Statement stmt = connection.createStatement();
                if (stmt == null) {
                    System.out.println("Couldn't create statement");
                    connection.close();
                    return;
                }
                String sql = "CREATE DATABASE " + DB_NAME;
                stmt.executeUpdate(sql);
                stmt.close();
            }
            connection.setCatalog(DB_NAME);
            String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + "(" + KEY_STUDENT_ID
                    + " VARCHAR(11) NOT NULL, " + KEY_COURSE + " VARCHAR(18) NOT NULL, " + KEY_COMMENT
                    + " TEXT NOT NULL, " + KEY_TRANSCRIBED + " BOOLEAN DEFAULT FALSE, " + KEY_UPDATED_BY
                    + " VARCHAR(11) NOT NULL DEFAULT \"99999999999\", " + KEY_LAST_UPDATED
                    + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP)";
            Statement stmt = connection.createStatement();
            stmt.execute(sql);
            stmt.close();
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
    }

    public void getComments(Student student) {
        Connection connection = null;
        try {
            connection = getConnection();
            connection.setCatalog(DB_NAME);
            String query = "select * from " + TABLE_NAME + " where " + KEY_STUDENT_ID + " = ?";
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setString(1, student.id);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String course = rs.getString(KEY_COURSE);
                String comment = rs.getString(KEY_COMMENT);
                student.comments.put(course, comment);
            }
            rs.close();
            ps.close();
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
    }

    public void updateComment(String id, String course, String comment) {
        Connection connection = null;
        try {
            connection = getConnection();
            connection.setCatalog(DB_NAME);
            String query = "select * from " + TABLE_NAME + " where " + KEY_STUDENT_ID + " = ? and " +
                    KEY_COURSE + " = ?";
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setString(1, id);
            ps.setString(2, course);
            ResultSet rs = ps.executeQuery();
            boolean hasComment = rs.next();
            rs.close();
            ps.close();

            if (hasComment) {
                query = "update " + TABLE_NAME + " set " + KEY_COMMENT + " = ?, " + KEY_TRANSCRIBED + " = FALSE where " + KEY_STUDENT_ID +
                        " = ? and " + KEY_COURSE + " = ?";
                ps = connection.prepareStatement(query);
                ps.setString(1, comment);
                ps.setString(2, id);
                ps.setString(3, course);
                ps.executeUpdate();
                ps.close();
            } else {
                query = "insert into " + TABLE_NAME + "(" + KEY_STUDENT_ID + ", " + KEY_COURSE + ", " +
                        KEY_COMMENT + ") values (?, ?, ?)";
                ps = connection.prepareStatement(query);
                ps.setString(1, id);
                ps.setString(2, course);
                ps.setString(3, comment);
                ps.executeUpdate();
                ps.close();
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
    }

    public List<Student> getAdvisorRequestList() {
        List<Student> students = new ArrayList<>();
        Connection connection = null;
        try {
            connection = getConnection();
            connection.setCatalog(DB_NAME);
            String query = "SELECT * FROM " + TABLE_NAME + " WHERE " + KEY_TRANSCRIBED + " = FALSE ORDER BY " + KEY_STUDENT_ID;
            PreparedStatement ps = connection.prepareStatement(query);
            ResultSet rs = ps.executeQuery();

            Student currentStudent = null;

            while (rs.next()) {
                String student_id = rs.getString(KEY_STUDENT_ID);
                if (currentStudent == null || !currentStudent.id.equals(student_id)) {
                    if (currentStudent != null) {
                        students.add(currentStudent);
                    }
                    currentStudent = new Student(student_id);
                }
                String course = rs.getString(KEY_COURSE);
                String comment = rs.getString(KEY_COMMENT);
                currentStudent.comments.put(course, comment);
            }
            if (currentStudent != null) {
                students.add(currentStudent);
            }
            rs.close();
            ps.close();
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
        return students;
    }

    public void updateTranscribed(String id, boolean checked) {
        Connection connection = null;
        try {
            connection = getConnection();
            connection.setCatalog(DB_NAME);
            String query = "UPDATE " + TABLE_NAME + " SET " + KEY_TRANSCRIBED + " = ? WHERE " + KEY_STUDENT_ID + " = ?";
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setBoolean(1, checked);
            ps.setString(2, id);

            ps.executeUpdate();
            ps.close();
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
    }
}
