package edu.sfsu.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OracleDB extends DB implements CampusDB {

    interface StaticFunction<U, V> {
        void apply(U u, V v) throws SQLException;
    }


    private static String currentSemester;


    public OracleDB(String driver, String url, String user, String passwd) {
        super(driver, url, user, passwd);

        currentSemester = Util.getCurrentSemester();
    }

    private void executeDBQuery(Student student, StaticFunction<Connection, Student> queryFunc) {
        Connection connection = null;
        try {
            connection = getConnection();
            queryFunc.apply(connection, student);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return;
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                }
            }
        }
    }

    private static void getStudentNameAndEmail(Connection connection, Student student)
            throws SQLException {
        // Lookup student and retrieve name and email address
        String query = "SELECT N.EMPLID, N.LAST_NAME, N.FIRST_NAME, N.MIDDLE_NAME, E.EMAIL_ADDR "
                + "FROM CMSCOMMON.SFO_EF_PERSON_NAME_MV N, CMSCOMMON.SFO_EMAILADR_MV E "
                + "WHERE N.EMPLID = ? AND N.EMPLID = E.EMPLID(+) AND N.NAME_TYPE = 'PRI' "
                + "AND E.ADDR_TYPE = 'OCMP'";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setString(1, student.id);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            student.firstName = rs.getString("FIRST_NAME");
            student.lastName = rs.getString("LAST_NAME");
            student.email = rs.getString("EMAIL_ADDR");
        }
        rs.close();
        ps.close();
    }

    private static void getStudentCoursesTakenSFSU(Connection connection, Student student)
            throws SQLException {
        // Courses taken at SFSU
        String query = "select distinct a.STRM, a.CRSE_GRADE_OFF, a.SUBJECT, a.CATALOG_NBR, a.CLASS_SECTION, b.EMPLID, b.ENROLLED_STATUS " +
                "from CMSCOMMON.SFO_CR_MAIN_MV a, CMSCOMMON.SFO_CR_ENROLL_MV b " +
                "where a.strm = b.strm " +
                "and a.emplid = b.emplid " +
                "and a.class_nbr = b.class_nbr " +
                "and a.session_code = b.session_code " +
                "and a.subject = b.subject " +
                "and a.catalog_nbr = b.catalog_nbr " +
                "and a.class_section = b.class_section " +
                "and b.enrolled_status in ('ENROLLED') " +
                "and b.emplid = ? ORDER BY a.STRM";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setString(1, student.id);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            String semester = rs.getString("STRM");
            String grade = rs.getString("CRSE_GRADE_OFF").replaceAll(" ", "");
            if (!grade.equals("") || semester.equals(currentSemester)) {
                Course course = new Course();
                course.courseName = rs.getString("SUBJECT") + rs.getString("CATALOG_NBR");
                course.semester = Util.formatSemester(semester);
                course.grade = grade;
                synchronized (student) {
                    student.courses.add(course);
                }
            }
        }
        rs.close();
        ps.close();
    }

    private static void getStudentCoursesTransferred(Connection connection, Student student)
            throws SQLException {
        // Courses transferred
        String query = "select " +
                "DISTINCT D.EMPLID " +
                ", C.CRSE_ID, C.SUBJECT AS SFSU_SUBJECT, C.CATALOG_NBR AS SFSU_NBR, C.DESCR " +
                ", D.EXT_COURSE_NBR, D.CRSE_GRADE_OFF " +
                ", O.EXT_ORG_ID, O.DESCR AS SCHOOLNAME " +
                ", A.LS_SCHOOL_TYPE " +
                ", E.SCHOOL_SUBJECT, E.SCHOOL_CRSE_NBR, E.EXT_TERM, E.TERM_YEAR " +
                "from CMSCOMMON.SFO_TRNS_CRSE_DTL D " +
                ", CMSCOMMON.SFO_CLASS_TBL C " +
                ", CMSCOMMON.SFO_EXT_ORG_TBL O " +
                ", CMSCOMMON.SFO_EXT_ORG_TBL_ADM A " +
                ", CMSCOMMON.SFO_EXT_COURSE_MV E " +
                "where D.EMPLID = ? " +
                "AND D.CRSE_ID = C.CRSE_ID(+) " +
                "AND (D.ARTICULATION_TERM = C.STRM " +
                "        OR " +
                "         D.ARTICULATION_TERM > (SELECT MAX(STRM) FROM CMSCOMMON.SFO_CLASS_TBL WHERE CRSE_ID = C.CRSE_ID) " +
                "         OR " +
                "         C.STRM IS NULL) " +
                "AND (C.DESCR IN (SELECT DISTINCT DESCR FROM CMSCOMMON.SFO_CLASS_TBL " +
                "                                        WHERE CRSE_ID = C.CRSE_ID " +
                "                                        AND (STRM = D.ARTICULATION_TERM OR " +
                "                                                STRM = (SELECT MAX(STRM) FROM CMSCOMMON.SFO_CLASS_TBL WHERE CRSE_ID = C.CRSE_ID)) " +
                "                                        ) " +
                "        OR " +
                "        C.DESCR IS NULL) " +
                "AND D.TRNSFR_SRC_ID = O.EXT_ORG_ID " +
                "AND O.EFFDT = (SELECT MAX(EFFDT) FROM CMSCOMMON.SFO_EXT_ORG_TBL WHERE EXT_ORG_ID = O.EXT_ORG_ID) " +
                "AND D.TRNSFR_SRC_ID = A.EXT_ORG_ID " +
                "AND A.EFFDT = (SELECT MAX(EFFDT) FROM CMSCOMMON.SFO_EXT_ORG_TBL_ADM WHERE EXT_ORG_ID = A.EXT_ORG_ID) " +
                "AND D.EMPLID = E.EMPLID(+) " +
                "AND D.TRNSFR_SRC_ID = E.EXT_ORG_ID(+) " +
                "AND D.EXT_COURSE_NBR = E.EXT_COURSE_NBR(+) " +
                "ORDER BY 2";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setString(1, student.id);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            Course course = new Course();
            course.courseName = rs.getString("SFSU_SUBJECT") + rs.getString("SFSU_NBR");
            String term = rs.getString("EXT_TERM");
            String year = rs.getString("TERM_YEAR");
            course.semester = formatSemester(term, year);
            course.grade = rs.getString("CRSE_GRADE_OFF");
            course.transferSchool = rs.getString("SCHOOLNAME");
            course.transferSchoolType = rs.getString("LS_SCHOOL_TYPE");
            String transferSubject = rs.getString("SCHOOL_SUBJECT");
            String transferCourse = rs.getString("SCHOOL_CRSE_NBR");
            if (transferCourse == null || transferSubject == null) {
                course.transferCourse = "unknown";
            } else {
                course.transferCourse = transferSubject + " " + transferCourse;
            }
            synchronized (student) {
                student.courses.add(course);
            }
        }
        rs.close();
        ps.close();
    }

    public Student getStudent(String id) {
        if (id.equals("0")) {
            return new DummyStudent(id);
        }
        Student student = new Student(id);

        ExecutorService executor = Executors.newWorkStealingPool();

        List<Callable<Void>> callables = Arrays.asList(() -> {
            executeDBQuery(student, OracleDB::getStudentNameAndEmail);
            return null;
        }, () -> {
            executeDBQuery(student, OracleDB::getStudentCoursesTakenSFSU);
            return null;
        }, () -> {
            executeDBQuery(student, OracleDB::getStudentCoursesTransferred);
            return null;
        });

        try {
            executor.invokeAll(callables).stream().map(future -> {
                try {
                    return future.get();
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (student.firstName.equals("") && student.lastName.equals("")) {
            return null;
        }
        return student;
    }

    public void getStudentInfo(List<Student> students) {
        Connection connection = null;
        try {
            connection = getConnection();
            for (Student student : students) {
                boolean missingInfo = student.firstName.isEmpty() || student.lastName.isEmpty() || student.email.isEmpty();
                if (!missingInfo) {
                    continue;
                }
                String query = "select * from CMSCOMMON.SFO_CR_MAIN_MV where emplid = ?";
                PreparedStatement ps = connection.prepareStatement(query);
                ps.setString(1, student.id);
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    student.firstName = rs.getString("FIRST_NAME");
                    student.lastName = rs.getString("LAST_NAME");
                    student.email = rs.getString("EMAIL_ADDR");
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

    private static String formatSemester(String term, String year) {
        if (term == null || year == null) {
            return "-";
        }
        String adjustedTerm = "";
        if (term.equals("SUMR")) {
            adjustedTerm = "Summer";
        } else if (term.equals("FALL")) {
            adjustedTerm = "Fall";
        } else if (term.equals("SPR")) {
            adjustedTerm = "Spring";
        } else if (term.equals("WINT")) {
            adjustedTerm = "Winter";
        }
        return adjustedTerm + " " + year;
    }
}
