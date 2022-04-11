package servlet;

import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import entity.LiftRideRequestBody;
import entity.QueueMessage;
import lombok.SneakyThrows;
import org.apache.commons.lang3.concurrent.EventCountCircuitBreaker;
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
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
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
    private final static String QUEUE_NAME = "A3_Q";

    // Circuit Breaker
    EventCountCircuitBreaker breaker = new EventCountCircuitBreaker(1400, 1, TimeUnit.SECONDS, 1000);

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
        handleRequest(request, response);
    }

    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (breaker.incrementAndCheckState()) {
            // actually handle this request
            response.setContentType("application/json");
            String subPath = extractURLPath(request);
            // validate parameters
            QueueMessage queueMessage = isValidParams(subPath);
            if (queueMessage == null) {
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
            } else {
                queueMessage.setTime(body.getTime());
                queueMessage.setLiftId(body.getLiftID());
                queueMessage.setWaitTime(body.getWaitTime());
            }
            // send response to queue
            Channel channel = pool.borrowObject();
            channel.queueDeclare(QUEUE_NAME, true, false, false, null);

            // prepare str message to queue
            String message = gson.toJson(queueMessage);
            channel.basicPublish("", QUEUE_NAME, null, message.getBytes(StandardCharsets.UTF_8));
            pool.returnObject(channel);

            response.setStatus(HttpServletResponse.SC_CREATED);
            response.getWriter().write(gson.toJson("{\"message\":\"received\"}"));
            System.out.println(" [x] Sent '" + message + "'");
        } else {
            // Return to client timeout, client could try again later.
            response.setStatus(HttpServletResponse.SC_GATEWAY_TIMEOUT);
        }
    }

    /**
     * Validates the format of url /skiers/{resortID}/seasons/{seasonID}/days/{daysID}/skiers/{skierID}
     * if valid, return QueuemMessage "{"resortId":"1113","seasonId":"2022","dayId":"136","skierId":"22","time":365,"liftId":27,"waitTime":6}"
     */
    private QueueMessage isValidParams(String subPath) {
        String[] splitted = subPath.split("/");
        System.out.println(Arrays.toString(splitted));
        if (splitted.length != 7) // does not match path format
            return null;
        String resortId = splitted[0];
        String season = splitted[1];
        String seasonId = splitted[2];
        String day = splitted[3];
        String dayId = splitted[4];
        String skier = splitted[5];
        String skierId = splitted[6];

        // check strings
        if (!(season.toLowerCase().equals(SEASONS)
                && day.toLowerCase().equals(DAYS)
                && skier.toLowerCase().equals(SKIERS)))
            return null;

        // check numbers
        try {
            Long.parseLong(splitted[0]);
            Long.parseLong(splitted[6]);

            // check if season and days are within range
            if (!withinRange(Integer.parseInt(splitted[2]), 1980, 2022)
                    || !withinRange(Integer.parseInt(splitted[4]), 1, 366)) {
                return null;
            }

        } catch (NumberFormatException e) {
            // System.out.println("Number Parsing error");
            return null;
        }

        return new QueueMessage(resortId, seasonId, dayId, skierId);
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
