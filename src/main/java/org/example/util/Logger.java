package org.example.util;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");

    public static void log(String productName, String message) {
        String fileName = "logs/" + productName + "_" + DATE_FORMAT.format(new Date()) + ".log";

        File logDir = new File("logs");
        if (!logDir.exists()) {
            logDir.mkdir();
        }

        try (FileWriter fw = new FileWriter(fileName, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {

            String logMessage = String.format("[%s] %s",
                    TIME_FORMAT.format(new Date()),
                    message);

            out.println(logMessage);
            System.out.println(logMessage);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}