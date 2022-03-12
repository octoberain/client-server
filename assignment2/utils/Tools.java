package utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Tools {

    public static String loadParam(String key) {
        String rootPath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
        String configFilePath = rootPath + "config.properties";
        try (InputStream input = new FileInputStream(configFilePath)) {
            Properties prop = new Properties();
            prop.load(input);
            return prop.getProperty(key);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
