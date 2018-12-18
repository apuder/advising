package edu.sfsu;

import edu.sfsu.db.*;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import javax.servlet.*;

public class AdvisingServlet extends HttpServlet {

    private String       password;
    private String       token;

    private CommentDB    commentDB;
    private CheckpointDB checkpointDB;
    private CampusDB     campusDB;


    @Override
    public void init(ServletConfig config) throws ServletException {
        ServletContext context = config.getServletContext();
        HtmlFormatter.init(context);

        Properties props = new Properties();

        try {
            InputStream is = context.getResourceAsStream("/WEB-INF/course_requirements.json");
            String s = IOUtils.toString(is);
            is.close();
            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(s);
            CourseRequirements.init(json);

            is = context.getResourceAsStream("/WEB-INF/advising.properties");
            props.load(is);
            is.close();
        } catch (IOException | ParseException e) {
            throw new ServletException(e.getMessage());
        }

        password = props.getProperty("PASSWORD");
        token = UUID.randomUUID().toString();

        commentDB = (CommentDB) DB.init("COMMENT", props);
        checkpointDB = (CheckpointDB) DB.init("CHECKPOINT", props);
        campusDB = (CampusDB) DB.init("CAMPUS", props);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();

        String path = request.getServletPath();

        if (path.equals("/login")) {
            String passwd = request.getParameter("passwd");
            processLogin(out, passwd);
            out.close();
            return;
        }

        if (path.equals("/lookup-student")) {
            String id = request.getParameter("id");
            String token = request.getParameter("token");
            String readonly = request.getParameter("readonly");
            if (token == null || !token.equals(this.token)) {
                readonly = "true";
            }
            processLookupStudent(out, id, readonly);
        }

        String token = request.getParameter("token");
        if (token == null || !token.equals(this.token)) {
            out.close();
            return;
        }

        /*if (path.equals("/lookup-student")) {
            String id = request.getParameter("id");
            String readonly = request.getParameter("readonly");
            processLookupStudent(out, id, readonly);
        }*/

        if (path.equals("/update-comment")) {
            String id = request.getParameter("id");
            String course = request.getParameter("course");
            String comment = request.getParameter("comment");
            processUpdateComment(id, course, comment);
        }

        if (path.equals("/update-checkpoints")) {
            String id = request.getParameter("id");
            String studentFirstName = request.getParameter("studentFirstName");
            String studentLastName = request.getParameter("studentLastName");
            String studentEmail = request.getParameter("studentEmail");
            String checkpointOralPresentation = request.getParameter("checkpointOralPresentation");
            String checkpointAdvising413 = request.getParameter("checkpointAdvising413");
            String checkpointSubmittedAppl = request.getParameter("checkpointSubmittedAppl");
            String comments = request.getParameter("comments");
            processUpdateCheckpoints(id, studentFirstName, studentLastName, studentEmail, checkpointOralPresentation,
                    checkpointAdvising413, checkpointSubmittedAppl, comments);
        }

        if (path.equals("/generate-list")) {
            String type = request.getParameter("type");
            response.setContentType("text/csv");
            response.addHeader("Content-Disposition", "attachment; filename=list-" + type + ".csv");
            response.addHeader("Pragma", "no-cache");
            processGenerateList(out, type);
        }
        out.close();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doPost(request, response);
    }

    private void processLogin(PrintWriter out, String passwd) {
        out.print(passwd.equals(password) ? token : "");
    }

    private void processLookupStudent(PrintWriter out, String id) throws IOException {
        Student student = campusDB.getStudent(id);
        String html;
        if (student != null) {
            commentDB.getComments(student);
            checkpointDB.getCheckpoints(student);
            html = HtmlFormatter.generateHtml(student);
        } else {
            html = "<b>Student not found</b>";
        }
        out.println(html);
    }

    private void processLookupStudent(PrintWriter out, String id, String readonly) throws IOException {
        Student student = campusDB.getStudent(id);
        String html = null;
        //System.out.println("data");
        //System.out.println(readonly);
        boolean ro = false;
        if (readonly.equals("true"))
            ro = true;
        try{
            if (student != null) {
                commentDB.getComments(student);
                checkpointDB.getCheckpoints(student);
                html = HtmlFormatter.generateHtml(student, ro);
                //System.out.println(html);

            } else {
                html = "<b>Student not found</b>";
            }
        }
        catch (Exception e) {
            System.out.println(e);
        }

        out.println(html);
    }

    private void processUpdateComment(String id, String course, String comment) {
        commentDB.updateComment(id, course, comment);
    }

    private void processUpdateCheckpoints(String id, String studentFirstName, String studentLastName, String studentEmail,
            String checkpointOralPresentation, String checkpointAdvising413,
            String checkpointSubmittedApplication, String comments) {
        checkpointDB.updateCheckpoints(id, studentFirstName, studentLastName, studentEmail, checkpointOralPresentation,
                checkpointAdvising413, checkpointSubmittedApplication, comments);
    }

    private void processGenerateList(PrintWriter out, String type) {
        List<Student> students = checkpointDB.generateList(type);
        System.out.println(type);
        if (type.equals("413_current")){
            students = generate413Student(students); //TODO create method matching this in each campusTestDB and oracle DB.
        }
        CSVFormatter.generateList(out, students, type);
    }


    private List<Student> generate413Student(List<Student> students){
        List<Student> csc413_students = new ArrayList<Student>();
        String current_semester = Util.getCurrentSemester();
        System.out.println(Util.formatSemester(current_semester));

        for (Student student : students ){
            System.out.println(student.id);
            List<Course> course_list = campusDB.getStudent(student.id).courses;
            for (Course course : course_list){
                System.out.println(course.courseName + " "+ course.semester);
                if (course.courseName.equals("CSC 413") && course.semester.equals(Util.formatSemester(current_semester))) {
                    csc413_students.add(student);
                    break;
                }
            }
        }
        return csc413_students;

    }
}
