import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Consumer {

    private static ConcurrentHashMap<Long, List<String>> map = new ConcurrentHashMap();
    private final static String QUEUE_NAME = "WORK_Q";

    public static void main(String[] argv) throws Exception {

        int numThreads = Integer.parseInt(argv[1]);
        String host = argv[3];
        start(numThreads, host);
    }

    /**
     * starts the consumer
     */
    private static void start(int numThreads, String host) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);
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
                    processMsg(message);
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
    private static void processMsg(String message) {
        String[] splitted = message.split("#");
        Long skierId = Long.parseLong(splitted[0].split(" ")[1]);
        String liftRide = splitted[1];
        map.putIfAbsent(skierId, new ArrayList<>());
        map.get(skierId).add(liftRide);

        // map.forEach( (k,v) -> System.out.println("key: " + k + " val: " + map.get(k)));
    }
}