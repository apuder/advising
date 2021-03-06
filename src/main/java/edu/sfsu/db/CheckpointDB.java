package edu.sfsu.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CheckpointDB extends DB {

    final static protected String DB_NAME                = "ADVISING";
    final static protected String TABLE_NAME             = "CHECKPOINTS";
    final static protected String KEY_STUDENT_ID         = "STUDENT_ID";
    final static protected String KEY_STUDENT_FIRST_NAME = "STUDENT_FIRST_NAME";
    final static protected String KEY_STUDENT_LAST_NAME  = "STUDENT_LAST_NAME";
    final static protected String KEY_STUDENT_EMAIL      = "STUDENT_EMAIL";
    final static protected String KEY_ORAL_PRESENTATION  = "ORAL_PRESENTATION";
    final static protected String KEY_SUBMITTED_APPL     = "SUBMITTED_APPL";
    final static protected String KEY_COMMENTS           = "COMMENTS";

    final static protected String KEY_TRANSCRIBED = "TRANSCRIBED";
    final static protected String KEY_UPDATED_BY = "UPDATED_BY";
    final static protected String KEY_LAST_UPDATED = "LAST_UPDATED";


    public CheckpointDB(String driver, String url, String user, String passwd) {
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
                    + " VARCHAR(11) NOT NULL PRIMARY KEY, " + KEY_STUDENT_FIRST_NAME + " TEXT, "
                    + KEY_STUDENT_LAST_NAME + " TEXT, "
                    + KEY_STUDENT_EMAIL + " TEXT, " + KEY_ORAL_PRESENTATION + " TEXT, "
                    + KEY_SUBMITTED_APPL + " TEXT, "
                    + KEY_COMMENTS + " TEXT, " + KEY_TRANSCRIBED + " BOOLEAN DEFAULT FALSE, " + KEY_UPDATED_BY
                    + " VARCHAR(11) NOT NULL DEFAULT \"99999999999\", " + KEY_LAST_UPDATED
                    + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP)";;
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

    public void getCheckpoints(Student student) {
        Connection connection = null;
        try {
            connection = getConnection();
            connection.setCatalog(DB_NAME);
            String query = "select * from " + TABLE_NAME + " where " + KEY_STUDENT_ID + " = ?";
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setString(1, student.id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                student.comment = rs.getString(KEY_COMMENTS);
                student.checkpointOralPresentation = rs.getString(KEY_ORAL_PRESENTATION);
                student.checkpointSubmittedApplication = rs.getString(KEY_SUBMITTED_APPL);
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

    public void updateCheckpoints(String id, String studentFirstName, String studentLastName, String studentEmail,
            String checkpointOralPresentation,
            String checkpointSubmittedApplication, String comments) {
        Connection connection = null;
        try {
            connection = getConnection();
            connection.setCatalog(DB_NAME);
            String query = "insert into " + TABLE_NAME + "(" + KEY_STUDENT_ID + ", "
                    + KEY_STUDENT_FIRST_NAME + ", " + KEY_STUDENT_LAST_NAME + ", " + KEY_STUDENT_EMAIL + ", " + KEY_ORAL_PRESENTATION
                    + ", " + KEY_SUBMITTED_APPL + ", " + KEY_COMMENTS
                    + ", " + KEY_TRANSCRIBED + ") values (?, ?, ?, ?, ?, ?, ?, FALSE) on duplicate key update ";
            query += KEY_STUDENT_FIRST_NAME + " = values(" + KEY_STUDENT_FIRST_NAME + "), ";
            query += KEY_STUDENT_LAST_NAME + " = values(" + KEY_STUDENT_LAST_NAME + "), ";
            query += KEY_STUDENT_EMAIL + " = values(" + KEY_STUDENT_EMAIL + "), ";
            query += KEY_ORAL_PRESENTATION + " = values(" + KEY_ORAL_PRESENTATION + "), ";
            query += KEY_SUBMITTED_APPL + " = values(" + KEY_SUBMITTED_APPL + "), ";
            query += KEY_COMMENTS + " = values(" + KEY_COMMENTS + ")";
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setString(1, id);
            ps.setString(2, studentFirstName);
            ps.setString(3, studentLastName);
            ps.setString(4, studentEmail);
            ps.setString(5, checkpointOralPresentation);
            ps.setString(6, checkpointSubmittedApplication);
            ps.setString(7, comments);
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

    public List<Student> generateList(String type) {
        List<Student> list = new ArrayList<>();
        Connection connection = null;
        try {
            connection = getConnection();
            connection.setCatalog(DB_NAME);
            String query = "select * from " + TABLE_NAME;
            if (type.equals("graduated")) {
                query += " where " + KEY_SUBMITTED_APPL + " <> ''";
            }
            PreparedStatement ps = connection.prepareStatement(query);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Student student = new Student(rs.getString(KEY_STUDENT_ID));
                student.firstName = rs.getString(KEY_STUDENT_FIRST_NAME);
                student.lastName = rs.getString(KEY_STUDENT_LAST_NAME);
                student.email = rs.getString(KEY_STUDENT_EMAIL);
                student.checkpointOralPresentation = rs.getString(KEY_ORAL_PRESENTATION);
                student.checkpointSubmittedApplication = rs.getString(KEY_SUBMITTED_APPL);
                list.add(student);
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
        return list;
    }

    public void getStudentInfo(List<Student> students) {
        Connection connection = null;
        try {
            connection = getConnection();
            for (Student student : students) {
                String query = "select * from " + TABLE_NAME + " where " + KEY_STUDENT_ID + " = ?";
                PreparedStatement ps = connection.prepareStatement(query);
                ps.setString(1, student.id);
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    student.firstName = rs.getString(KEY_STUDENT_FIRST_NAME);
                    student.lastName = rs.getString(KEY_STUDENT_LAST_NAME);
                    student.email = rs.getString(KEY_STUDENT_EMAIL);
                }
                rs.close();
                ps.close();
            }
        } catch (SQLException e) {
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
