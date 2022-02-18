package servlet;

import com.google.gson.Gson;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.stream.Collectors;

@WebServlet(name = "SkierServlet", urlPatterns = "/skiers/*")
public class SkierServlet extends HttpServlet {
    private String SEASONS = "seasons";
    private String DAYS = "days";
    private String SKIERS = "skiers";

    private Gson gson = new Gson();

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        int ignoreAmt = request.getContextPath().length() + request.getServletPath().length();
        // System.out.println(request.getContextPath());
        // System.out.println(request.getServletPath());
        // System.out.println(request.getRequestURI());

        String requestBody = request.getReader().lines().collect(Collectors.joining());
        String url = request.getRequestURI();

//        if (isValid(request) && !requestBody.isEmpty()) {
//            response.setStatus(201);
//            return;
//        }

        // invalid input
        response.setStatus(201);
    }

    private boolean isValid(HttpServletRequest request) {
        int ignoreAmt = request.getContextPath().length() + request.getServletPath().length();
        String url = request.getRequestURI();
        String subPath = url.substring(ignoreAmt + 1);

        String[] splitted = subPath.split("/");
        if (splitted.length != 7) // does not match path format
            return false;
        String seasons = splitted[1];
        String days = splitted[3];
        String skiers = splitted[5];

        // check strings
        if (!(seasons.toLowerCase().equals(SEASONS)
                && days.toLowerCase().equals(DAYS)
                && skiers.toLowerCase().equals(SKIERS)))
            return false;

        // check numbers
        try {
            int resortId = Integer.parseInt(splitted[0]);
            int seasonId = Integer.parseInt(splitted[2]);
            int dayId = Integer.parseInt(splitted[4]);
            int skierId = Integer.parseInt(splitted[6]);

            // check if season and days are within range
            if (!withinRange(seasonId, 1980, 2022) || !withinRange(dayId, 1, 366)) {
                return false;
            }

        } catch (NumberFormatException e) {
            System.out.println("Number Parsing error");
            return false;
        }

        return true;
    }

    /**
     * Helper function check if a number is within range
     */
    private boolean withinRange(int num, int lower, int upper) {
        return num >= lower && num <= upper;
    }

}
