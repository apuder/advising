package edu.sfsu;

import com.opencsv.CSVWriter;
import edu.sfsu.db.Course;
import edu.sfsu.db.CourseRequirements;
import edu.sfsu.db.Student;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class CSVFormatter {

    public static void generateList(PrintWriter out, List<Student> students, String type) {
        try (CSVWriter writer = new CSVWriter(out)) {
            for (Student student : students) {
                String date = "-";
                if (type.startsWith("413")) {
                    date = student.checkpointAdvising413;
                }
                if (type.startsWith("grad")) {
                    date = student.checkpointSubmittedApplication;
                }
                String[] row = {student.id, student.lastName, student.firstName, date};
                writer.writeNext(row);
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
