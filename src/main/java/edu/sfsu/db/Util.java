package edu.sfsu.db;

import java.util.Calendar;
import java.util.Date;

public class Util {

    public static String formatSemester(String semester) {
        if (semester.length() != 4) {
            return "-";
        }
        String year;
        char firstDigit = semester.charAt(0);
        switch (firstDigit) {
        case '1':
            year = "19";
            break;
        case '2':
            year = "20";
            break;
        default:
            return "-";
        }
        String term;
        year += semester.substring(1, 3);
        char lastDigit = semester.charAt(3);
        switch (lastDigit) {
        case '1':
            term = "Winter";
            break;
        case '3':
            term = "Spring";
            break;
        case '5':
            term = "Summer";
            break;
        case '7':
            term = "Fall";
            break;
        default:
            return "-";
        }
        return term + " " + year;
    }

    public static String getCurrentSemester() {
        // This program won't run if a time machine is used to go back to the
        // 90ties. :)
        String semester = "2";

        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DATE);

        // Year
        semester += Integer.toString(year % 100);

        // Semester
        //TODO Hack
        char term;
        if (month == 0 && day < 25) {
            term = '1'; // Winter
        } else if (month < 5) {
            term = '3'; // Spring
        } else if (month < 8 && day < 20) {
            term = '5'; // Summer
        } else {
            term = '7'; // Fall
        }
        semester += term;
        return semester;
    }

}
