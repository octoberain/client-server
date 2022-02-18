import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

public class Utils {

    static final String NUMTHREADS = "NUMTHREADS";
    static final String NUMSKIERS = "NUMSKIERS";
    static final String NUMLIFTS = "NUMLIFTS";
    static final String NUMRUNS = "NUMRUNS";
    static Logger logger = Logger.getLogger(Thread.class.getName());

    public static void printMsg(String msg, double val, String unit) {
        System.out.println(msg + ": " + val + " " + unit);
    }

    /**
     * Helper function loading baseURL from config file
     */
    public static String loadURL() {
        try (InputStream input = new FileInputStream("src/main/resources/config.properties")) {
            Properties prop = new Properties();
            prop.load(input);
            return prop.getProperty("baseURL");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Map<String, Integer> loadParams() {
        try (InputStream input = new FileInputStream("src/main/resources/config.properties")) {
            Properties prop = new Properties();
            prop.load(input);

            Map<String, Integer> params = new HashMap<>();
            params.put(NUMTHREADS, Integer.parseInt(prop.getProperty(NUMTHREADS)));
            params.put(NUMSKIERS, Integer.parseInt(prop.getProperty(NUMSKIERS)));
            params.put(NUMLIFTS, Integer.parseInt(prop.getProperty(NUMLIFTS)));
            params.put(NUMRUNS, Integer.parseInt(prop.getProperty(NUMRUNS)));
            return params;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
