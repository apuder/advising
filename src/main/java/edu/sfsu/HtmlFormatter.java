package edu.sfsu;

import edu.sfsu.db.Course;
import edu.sfsu.db.CourseRequirements;
import edu.sfsu.db.Student;
import edu.sfsu.sniplet.HTMLSniplet;

import javax.servlet.ServletContext;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class HtmlFormatter {

    final private static List<Object> transfers;

    static {
        transfers = new ArrayList<>();
        transfers.add("Transfer 1");
        transfers.add("Transfer 2");
        transfers.add("Transfer 3");
        transfers.add("Transfer 4");
        transfers.add("Transfer 5");
    }


    private static HTMLSniplet studentInfoFragment;
    private static HTMLSniplet studentROFragment;

    public static void init(ServletContext context) {
        InputStream is = context.getResourceAsStream("/WEB-INF/classes/student_sniplet.html");
        studentInfoFragment = HTMLSniplet.fromInputStream(is);
        InputStream is_ro = context.getResourceAsStream("/WEB-INF/classes/readonly_student_sniplet.html");
        studentROFragment = HTMLSniplet.fromInputStream(is_ro);
    }

    private static void generateCheckpoint(HTMLSniplet fragment, String studentId, String date, String checkpointDescr,
                                           String checkpointId, boolean showDateField) {
        HTMLSniplet checkpoint = fragment.instantiate("checkpoint");
        checkpoint.p("checkpoint_id", checkpointId);
        checkpoint.p("student_id", studentId);
        checkpoint.p("checkpoint_descr", checkpointDescr);
        //checkpoint.p("style", showDateField ? "" : " style='display: none;'");
        checkpoint.p("checked", (date != null && !"".equals(date)) ? " checked" : "");
        checkpoint.p("date_value", (date != null && !"".equals(date)) ? String.format(" value='%s'", e(date)) : "");
        checkpoint.p("label", showDateField ? "Date" : "Comment");
    }

    private static void generateCommentField(HTMLSniplet fragment, Student student, String courseName) {
        HTMLSniplet comment = fragment.instantiate("comment");
        String id = courseName.replace(" ", "_");
        comment.p("student_id", student.id);
        comment.p("course_id", id);
        comment.p("comment", (student.comments.containsKey(id)) ? String.format(" value='%s'", e(student.comments.get(id))) : "");
    }

    private static void generateClassList(HTMLSniplet fragment, Student student, String heading, List<Object> classes,
                                          boolean showMissing, boolean showDetails) {
        HTMLSniplet section = fragment.instantiate("section");
        section.p("heading", heading);
        if (showDetails) {
            section.instantiate("details");
        }
        for (Object clazz : classes) {
            Course course = student.requirementFor(clazz);
            if (course == null) {
                if (showMissing) {
                    HTMLSniplet courseFragment = section.instantiate("course");
                    String courseName = clazz instanceof String ? (String) clazz
                            : ((ArrayList<String>) clazz).get(0);
                    courseFragment.p("course_name", replaceSpaces(courseName));
                    if (showDetails) {
                        HTMLSniplet details = courseFragment.instantiate("details");
                        details.p("transfer", "");
                        details.p("semester", "");
                        details.p("grade", "");
                    }
                    generateCommentField(courseFragment, student, courseName);
                }
                continue;
            }
            HTMLSniplet courseFragment = section.instantiate("course");
            courseFragment.p("course_name", replaceSpaces(course.courseName));
            if (showDetails) {
                HTMLSniplet details = courseFragment.instantiate("details");
                String transfer = "";
                if (course.transferCourse != null && course.transferSchool != null) {
                    transfer = String.format("%s (%s)", replaceSpaces(course.transferCourse),
                            replaceSpaces(course.transferSchool));
                }
                details.p("transfer", transfer);
                details.p("semester", replaceSpaces(course.semester));
                details.p("grade", course.grade);
            }
            generateCommentField(courseFragment, student, course.courseName);
        }
    }

    private static String replaceSpaces(String s) {
        return s.replaceAll(" ", "&nbsp;");
    }

    private static String e(String s) {
        return s.replace("'", "&#39;");
    }

    public static String generateHtml(Student student) {
        HTMLSniplet fragment = studentInfoFragment.copy();
        fragment.p("student_first_name", student.firstName);
        fragment.p("student_last_name", student.lastName);
        fragment.p("student_email", student.email);
        fragment.p("student_id", student.id);

        fragment.p("comments", student.comment);

        generateCheckpoint(fragment, student.id, student.checkpointOralPresentation,
                "Senior Oral Presentation", "oral_presentation", false);
        generateCheckpoint(fragment, student.id, student.checkpointAdvising413, "413 Advising",
                "advising_413", true);
        generateCheckpoint(fragment, student.id, student.checkpointSubmittedApplication,
                "Submitted Graduate Application", "submitted_appl", true);

        generateClassList(fragment, student, "Core", CourseRequirements.core, true, true);
        generateClassList(fragment, student, "Electives", CourseRequirements.electives, false, true);
        generateClassList(fragment, student, "Transfers", transfers, true, false);
        return fragment.render().toString();
    }
    public static String generateHtml(Student student, boolean readonly) {
        HTMLSniplet fragment = null;
        if (readonly)
            fragment = studentROFragment.copy();
        else
            fragment = studentInfoFragment.copy();

        fragment.p("student_first_name", student.firstName);
        fragment.p("student_last_name", student.lastName);
        fragment.p("student_email", student.email);
        fragment.p("student_id", student.id);

        fragment.p("comments", student.comment);

        generateCheckpoint(fragment, student.id, student.checkpointOralPresentation,
                "Senior Oral Presentation", "oral_presentation", false);
        generateCheckpoint(fragment, student.id, student.checkpointAdvising413, "413 Advising",
                "advising_413", true);
        generateCheckpoint(fragment, student.id, student.checkpointSubmittedApplication,
                "Submitted Graduate Application", "submitted_appl", true);

        generateClassList(fragment, student, "Core", CourseRequirements.core, true, true);
        generateClassList(fragment, student, "Electives", CourseRequirements.electives, false, true);
        generateClassList(fragment, student, "Transfers", transfers, true, false);
        return fragment.render().toString();
    }
}
