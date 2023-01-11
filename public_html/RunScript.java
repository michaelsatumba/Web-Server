package public_html;

import java.io.*;
import java.lang.ProcessBuilder.Redirect;
import java.util.*;

public class RunScript {
    private static Map<String, String> env;
    private static ProcessBuilder pb;
    private static String pathToScript;
    private static String[] request;

    public RunScript(String[] reqArr) {
        // * Replace with ScriptAlias path or your path for testing
        // * /web-server/public_html/cgi-bin/perl_env
        pathToScript = "/home/ajwc/SFSU/CSC667/Assignments/web-server/public_html/cgi-bin/perl_env";
        pb = new ProcessBuilder(pathToScript);
        request = reqArr;
    }

    public static void start() {
        System.out.println("✨ In RunScript...");
        try {
            getHeaders();
            // * Place the environment variables in a map
            env = pb.environment();
            // * Print the env variables to stdout
            env.forEach((key, value) -> System.out.println(key + value));
            
            // * Create two new file objects: env variables, errors
            File outputTxt = new File("cgi-bin/standard-output.txt");
            File errorLog = new File("cgi-bin/error.log");

            // * Run script
            pb.command(pathToScript);

            // * Append output to text and log files respectively
            pb.redirectOutput(Redirect.appendTo(outputTxt));
            pb.redirectError(Redirect.appendTo(errorLog));
            pb.start();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public static void getHeaders() {
        for(String r : request) {
            // TODO: Create env variables out of headers
            System.out.println(r);
        }
    }
}
