package edu.sfsu.updater;

import edu.sfsu.db.OracleDB;
import edu.sfsu.db.CheckpointDB;
import edu.sfsu.db.Student;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UpdatedCheckpointDB extends CheckpointDB {

    public UpdatedCheckpointDB(String driver, String url, String user, String passwd) {
        super(driver, url, user, passwd);
    }

    /**
     * First version didn't save a student's first and last name separately.
     * The following SQL statements update the schema of the 'checkpoints' DB.
     *
     * <code>
     *     use ADVISING;
     *     alter table CHECKPOINTS add STUDENT_FIRST_NAME text;
     *     alter table CHECKPOINTS add STUDENT_LAST_NAME text;
     *     alter table CHECKPOINTS drop STUDENT_NAME;
     * </code>
     *
     * Next run updateNames(). Then export the updated DB via:
     *
     * <code>
     *     mysqldump -u advisor -p --opt ADVISING > advising.sql
     * </code>
     *
     * Copy advising.sql to production machine, then import DB via:
     *
     * <code>
     *     mysql -u advisor -p ADVISING < advising.sql
     * </code>
     */
    public void updateNames(OracleDB campusDB) {
        Connection connection = null;
        List<String> ids = new ArrayList<>();
        try {
            connection = getConnection();
            connection.setCatalog(DB_NAME);
            String query = "select * from " + TABLE_NAME;
            PreparedStatement ps = connection.prepareStatement(query);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                ids.add(rs.getString(KEY_STUDENT_ID));
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

        for (String id : ids) {
            Student student = campusDB.getStudent(id);
            if (student == null) {
                System.out.println(id + ": NOT FOUND!");
                continue;
            }
            System.out.println(id + ": " + student.firstName + " " + student.lastName);
            getCheckpoints(student);
            updateCheckpoints(student.id,student.firstName, student.lastName, student.email,
                    student.checkpointOralPresentation,
                    student.checkpointSubmittedApplication, student.comment);
        }
    }
}