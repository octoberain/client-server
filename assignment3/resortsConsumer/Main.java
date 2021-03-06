import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import redis.clients.jedis.JedisPooled;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

    private final static String QUEUE_NAME = "A3_Q";

    private final static String RESORT_ID = "resortId";
    private final static String SEASON_ID = "seasonId";
    private final static String DAY_ID = "dayId";
    private final static String SKIER_ID = "skierId";
    private final static String TIME = "time";
    private final static String LIFT_ID = "liftId";
    private final static String WAIT_TIME = "waitTime";

    public static void main(String[] argv) throws IOException, TimeoutException {
        int numThreads = Integer.parseInt(argv[1]);
        String rabbit_host = argv[3];
        String redis_host = argv[5];

        System.out.println("Rabbit host: " + rabbit_host);
        System.out.println("Redis host: " + redis_host);
        System.out.println("------- Starting -------");
        JedisPooled jedis = new JedisPooled(redis_host, 6379);
        start(numThreads, rabbit_host, jedis);
    }

    /**
     * starts the consumer
     */
    private static void start(int numThreads, String rabbit_host, JedisPooled jedis) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(rabbit_host);
        final Connection connection = factory.newConnection();

        Runnable runnable = () -> {
            try {
                final Channel channel = connection.createChannel();
                channel.queueDeclare(QUEUE_NAME, true, false, false, null);
                // max one message per receiver
                channel.basicQos(1);
                System.out.println(" [*] Thread waiting for messages. To exit press CTRL+C");

                DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                    String message = new String(delivery.getBody(), "UTF-8");
                    // save to map
                    processMsg(message, jedis);
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                    System.out.println("Callback thread ID = " + Thread.currentThread().getId() + " Received '" + message + "'");
                };
                // consume messages
                channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> {
                });
            } catch (IOException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
        };

        for (int i = 0; i < numThreads; i++) {
            Thread th = new Thread(runnable);
            th.start();
        }

    }

    /**
     * Extract skierId and liftRide data from queue task and save to concurrent HashMap
     */
    private static void processMsg(String message, JedisPooled jedis) {
        // queueMessage format
        // "{"resortId":"1113","seasonId":"2022","dayId":"136","skierId":"22","time":365,"liftId":27,"waitTime":6}"

        // extract target string out of message from incoming queue
        String resortId = extract(message, RESORT_ID, SEASON_ID, 3, 3);
        String seasonId = extract(message, SEASON_ID, DAY_ID, 3, 3);
        String dayId = extract(message, DAY_ID, SKIER_ID, 3, 3);
        String skierId = extract(message, SKIER_ID, TIME, 3, 3);
        String time = extract(message, TIME, LIFT_ID, 2, 2);
        String liftId = extract(message, LIFT_ID, WAIT_TIME, 2, 2);
        String waitTime = extract(message, WAIT_TIME, null, 2, 1);

        // prepare data to be stored in Redis
        Map<String, String> kv = new HashMap<>();
        kv.put(SKIER_ID, dayId);
        kv.put(TIME, time);
        kv.put(LIFT_ID, liftId);
        kv.put(WAIT_TIME, waitTime);

        // adding to redis
        Gson gson = new Gson();
        String key = resortId + seasonId + dayId; // database key
        jedis.rpush(key, gson.toJson(kv));
        // retrieve data using
        // lrange key 0 -1

    }

    /**
     * Helper function extracts target String value from queue message.
     */
    private static String extract(String message, String targetStr, String nextStr, int offset1, int offset2) {
        int targetIndex = message.indexOf(targetStr);
        int endIndex = nextStr != null ? message.indexOf(nextStr) : message.length();

        return message.substring(targetIndex + targetStr.length() + offset1, endIndex - offset2);

    }
    /**
     * resortId+seasonId+dayId :[
     *              {
     *              skierId: "",
     *              time: "",
     *              liftId: "",
     *              waitTime:"",
     *              }
     *           ]
     *
     */
}
