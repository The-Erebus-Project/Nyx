package io.github.vizanarkonin.nyx.Config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

public class CoreCofig {
    private final static Logger log = LogManager.getLogger(CoreCofig.class.getName());
    public static final String dataFolder;
    public static final String tempFolder;
        
    static {
        JSONObject configFile = openConfig();

        dataFolder = System.getProperty("dataFolder") == null ? configFile.getString("dataFolder") : System.getProperty("dataFolder");
        tempFolder = System.getProperty("tempFolder") == null ? configFile.getString("tempFolder") : System.getProperty("tempFolder");
    }

    private static JSONObject openConfig() {
        
        try {
            InputStream inputStream = CoreCofig.class.getResourceAsStream("/config.json");
            String data = readFromInputStream(inputStream);

            return new JSONObject(data);
        } catch (IOException e) {
            log.error(ExceptionUtils.getStackTrace(e));
            // Code 20 - FAILURE
            System.exit(20);
            return null;
        }
    }

    private static String readFromInputStream(InputStream inputStream) throws IOException {
        StringBuilder resultStringBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                resultStringBuilder.append(line).append("\n");
            }
        }
        return resultStringBuilder.toString();
    }
}
