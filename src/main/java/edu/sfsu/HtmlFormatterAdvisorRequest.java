package edu.sfsu;

import edu.sfsu.db.Student;
import edu.sfsu.sniplet.HTMLSniplet;

import javax.servlet.ServletContext;
import java.io.InputStream;
import java.util.List;

public class HtmlFormatterAdvisorRequest {


    private static HTMLSniplet advisorRequestFragment;

    public static void init(ServletContext context) {
        InputStream is = context.getResourceAsStream("/WEB-INF/classes/advisor_request_sniplet.html");
        advisorRequestFragment = HTMLSniplet.fromInputStream(is);
    }

    public static String generateHtml(List<Student> students) {
        HTMLSniplet fragment = advisorRequestFragment.copy();
        for (Student student : students) {
            HTMLSniplet studentFragment = fragment.instantiate("student_advisor_request");
            studentFragment.p("student_first_name", student.firstName);
            studentFragment.p("student_last_name", student.lastName);
            studentFragment.p("student_email", student.email);
            studentFragment.p("student_id", student.id);
            StringBuffer equivalencies = new StringBuffer();
            for (String course : student.comments.keySet()) {
                if (equivalencies.length() != 0) {
                    equivalencies.append("\n");
                }
                equivalencies.append(course.replace('_', ' '));
                equivalencies.append(": ");
                equivalencies.append(student.comments.get(course));
            }
            studentFragment.p("course_equivalencies", equivalencies.toString());
        }
        return fragment.render().toString();
    }
}
