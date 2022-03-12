package servlet;

import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import entity.LiftRideRequestBody;
import lombok.SneakyThrows;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import utils.Tools;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@WebServlet(name = "SkierServlet", urlPatterns = "/skiers/*")
public class SkierServlet extends HttpServlet {
    private String SEASONS = "seasons";
    private String DAYS = "days";
    private String SKIERS = "skiers";

    private Gson gson = new Gson();
    private ConnectionFactory factory;
    private Connection conn;
    private ObjectPool<Channel> pool;
    private final static String QUEUE_NAME = "WORK_Q";

    @SneakyThrows
    @Override
    public void init() {
        this.factory = new ConnectionFactory();
        String ip = Tools.loadParam("RABBITMQ_IP");
        factory.setHost(ip);
        this.conn = factory.newConnection(); //init connection

        // create channel pool
        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMinIdle(2);
        config.setMaxIdle(5);
        config.setMaxTotal(50);
        this.pool = new GenericObjectPool<>(new connection.ConnectionFactory(conn), config);
    }

    @SneakyThrows
    @Override
    public void destroy() {
        // close connection
        this.conn.close();
        this.pool.clear();
    }

    @SneakyThrows
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        String subPath = extractURLPath(request);
        // validate parameters
        Long skierId = isValidParams(subPath);
        if (skierId == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(gson.toJson("{\"message\":\"invalid input\"}"));
            return;
        }

        // validate payload
        String requestBody = request.getReader().lines().collect(Collectors.joining());
        LiftRideRequestBody body = gson.fromJson(requestBody, LiftRideRequestBody.class);
        if (!isValidPayload(body)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(gson.toJson("{\"message\":\"invalid input\"}"));
            return;
        }

        // send response to queue
        Channel channel = pool.borrowObject();
        channel.queueDeclare(QUEUE_NAME, true, false, false, null);
        String message = "skier: " + skierId + "#" + requestBody;
        channel.basicPublish("", QUEUE_NAME, null, message.getBytes(StandardCharsets.UTF_8));
        pool.returnObject(channel);
        response.setStatus(HttpServletResponse.SC_CREATED);
        response.getWriter().write(gson.toJson("{\"message\":\"received\"}"));
        System.out.println(" [x] Sent '" + message + "'");

    }

    /**
     * Validates the format of url /skiers/{resortID}/seasons/{seasonID}/days/{daysID}/skiers/{skierID}
     * if valid, return skierID
     */
    private Long isValidParams(String subPath) {
        String[] splitted = subPath.split("/");
        if (splitted.length != 7) // does not match path format
            return null;
        String seasons = splitted[1];
        String days = splitted[3];
        String skiers = splitted[5];

        // check strings
        if (!(seasons.toLowerCase().equals(SEASONS)
                && days.toLowerCase().equals(DAYS)
                && skiers.toLowerCase().equals(SKIERS)))
            return null;

        // check numbers
        Long skierId;
        try {
            Long resortId = Long.parseLong(splitted[0]);
            int seasonId = Integer.parseInt(splitted[2]);
            int dayId = Integer.parseInt(splitted[4]);
            skierId = Long.parseLong(splitted[6]);

            // check if season and days are within range
            if (!withinRange(seasonId, 1980, 2022) || !withinRange(dayId, 1, 366)) {
                return null;
            }

        } catch (NumberFormatException e) {
            // System.out.println("Number Parsing error");
            return null;
        }

        return skierId;
    }

    private boolean isValidPayload(LiftRideRequestBody body) {
        if (body == null)
            return false;
        if (body.getLiftID() == 0 || body.getTime() == 0 || body.getWaitTime() == 0)
            return false;
        return true;
    }

    /**
     * Helper function extract URL path from requests by removing context path and servlet path
     */
    private String extractURLPath(HttpServletRequest request) {
        int ignoreAmt = request.getContextPath().length() + request.getServletPath().length();
        String url = request.getRequestURI();
        return url.substring(ignoreAmt + 1);
    }

    /**
     * Helper function check if a number is within range
     */
    private boolean withinRange(int num, int lower, int upper) {
        return num >= lower && num <= upper;
    }

}
