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
import java.text.SimpleDateFormat;
import java.util.*;
import javax.servlet.*;

public class AdvisingServlet extends HttpServlet {

    private String       password;
    private String       token;

    private CommentDB    commentDB;
    private CheckpointDB checkpointDB;
    private CampusDB     campusDB;
    private String  winter_start;
    private String  winter_end;
    private String  spring_start;
    private String  spring_end;
    private String  summer_start;
    private String  summer_end;
    private String  fall_start;
    private String  fall_end;



    @Override
    public void init(ServletConfig config) throws ServletException {
        ServletContext context = config.getServletContext();
        HtmlFormatter.init(context);

        Properties props = new Properties();
        Properties date_props = new Properties();


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

            is = context.getResourceAsStream("/WEB-INF/semester_dates.properties");
            date_props.load(is);
            is.close();



        } catch (IOException | ParseException e) {
            throw new ServletException(e.getMessage());
        }

        password = props.getProperty("PASSWORD");
        token = UUID.randomUUID().toString();

        commentDB = (CommentDB) DB.init("COMMENT", props);
        checkpointDB = (CheckpointDB) DB.init("CHECKPOINT", props);
        campusDB = (CampusDB) DB.init("CAMPUS", props);

        fall_start = date_props.getProperty("FALL_SEMS_START").trim();
        fall_end = date_props.getProperty("FALL_SEMS_END").trim();
        winter_start = date_props.getProperty("WINTER_SEMS_START").trim();
        winter_end = date_props.getProperty("WINTER_SEMS_END").trim();
        spring_start = date_props.getProperty("SPRING_SEMS_START").trim();
        spring_end = date_props.getProperty("SPRING_SEMS_END").trim();
        summer_start = date_props.getProperty("SUMMER_SEMS_START").trim();
        summer_end = date_props.getProperty("SUMMER_SEMS_END").trim();

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

        String token = request.getParameter("token");
        if (token == null || !token.equals(this.token)) {
            out.close();
            return;
        }

        if (path.equals("/lookup-student")) {
            String id = request.getParameter("id");
            processLookupStudent(out, id);
        }

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
        if (type.equals("413_current") || type.equals("grad_current")){
            String [] compDate = getCurrentCompDates();
            students = filterQueryByDate(students, type, compDate[0], compDate[1]);
        }
        CSVFormatter.generateList(out, students, type);
    }

    private String [] getCurrentCompDates(){
        Calendar cal = Calendar.getInstance();
        cal.setTime(new java.util.Date());
        String year = String.valueOf(cal.get(Calendar.YEAR));
        String comp_start = fall_start;//default to winter semester.
        String comp_end = fall_end;
        String current_semester = Util.formatSemester(Util.getCurrentSemester());

        if (current_semester.startsWith("Wi")) {
            comp_start = winter_start;
            comp_end = winter_end;
        }
        if (current_semester.startsWith("Spr")) {
            comp_start = spring_start;
            comp_end = spring_end;
        }
        if (current_semester.startsWith("Sum")) {
            comp_start = summer_start;
            comp_end = summer_end;
        }

        String [] comp_dates = {comp_start+"/"+year, comp_end+"/"+year};

        return comp_dates;
    }

    private List<Student> filterQueryByDate(List<Student> students, String type, String startDate, String endDate) {
        List<Student> filteredStudents = new ArrayList<Student>();
        String queryDate;

        for (Student student : students) {
            Calendar compare = Calendar.getInstance();
            Calendar start = Calendar.getInstance();
            Calendar end = Calendar.getInstance();

            if (type.equals("413_current")){
                queryDate = student.checkpointAdvising413;
            }
            else{
                queryDate = student.checkpointSubmittedApplication;
            }
            SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy"); //pattern matching needs to be consistent with date format in checkpointDB.
            try {
                compare.setTime(formatter.parse(queryDate));
                start.setTime(formatter.parse(startDate));
                end.setTime(formatter.parse(endDate));
            } catch (java.text.ParseException e) {
                e.printStackTrace();
                continue;
            }

            if (!(compare.before(start) || compare.after(end))){
                filteredStudents.add(student);
            }
        }
        return filteredStudents;
    }

}