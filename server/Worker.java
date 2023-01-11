package server;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.awt.image.BufferedImage;

// * Current "run server" command: javac public_html/cgi-bin/RunScript.java;javac server/config/Configuration.java;javac server/Server.java;javac server/Worker.java;javac WebServer.java;java WebServer

import javax.imageio.ImageIO;

import server.config.Configuration;
import server.config.HtAccess;
import server.config.Htpassword;
import server.config.HttpdConfig;
import server.config.MimeTypes;
import public_html.RunScript;

public class Worker implements Runnable {
    private static String server = "Chau & Satumba";
    protected static Socket socket;

    // * Path objects
    private static String documentRoot;
    private static String logFile;
    private static String scriptAlias;
    private static String directoryIndex;
    private static String extension;
    private static String dirAlias = null;
    private static String htAccessPath;
    private static String scriptName;

    // * Config and Auth objects
    private static Configuration config;
    private static HttpdConfig httpdConfig;
    private static MimeTypes mimeTypes;
    private static HtAccess htAccess;
    private static Htpassword htPassword;
    private final static AtomicBoolean running = new AtomicBoolean(false);
    private static String fifthLines;
    private static String sixthLines;

    // * Header objects
    private static String reqArr[];
    private static String contentType;
    private static long contentLength;
    private static String lastModified;
    private static String dateTime;
    private static String authInfo;
    private static String authWWWInfo;

    // * Body Objects
    private static String imgHtml;

    public Worker(Socket socket) {
        this.socket = socket;
        // * Instantiate new Configuration object
        config = new Configuration("conf/httpd.conf", "conf/mime.types");
        config.readHttpdConfig(); config.readMimeTypes();
        // * Local access to the directives via httpdConfig object
        httpdConfig = new HttpdConfig(config.getConfigMap());
        // * Local access to the mime types via mimeTypes object
        mimeTypes = new MimeTypes(config.getMimeTypesMap());        
        contentLength = 0;
        scriptAlias = null;
        scriptName = null;
        authInfo = null;
        authWWWInfo = null;
    }

    @Override
    public void run() {
        try {
            System.out.println("📨 Request received!");
            System.out.println("🎈 " + Thread.currentThread().getName() + " running.");
            proccessRequest();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private synchronized void proccessRequest() {
        running.set(true);
        while (running.get()) {
            try {
                // notify();
                // System.out.println("Debug: got new message " + client.toString());
                // * Read the request - listen to the messages; Bytes -> Chars
                InputStreamReader isr = new InputStreamReader(this.socket.getInputStream());
                // * Reads text from char-input stream,
                BufferedReader br = new BufferedReader(isr);
                // * Read the first request from the client
                StringBuilder request = new StringBuilder();
                String line;

                // TODO: Causes java.net.SocketException: Connection reset
                line = br.readLine();
                int count = 0;

                while (line != null && !line.isEmpty()) {
                    request.append(line + "\r\n");
                    line = br.readLine();
                    count++;
                }

                printRequest(request);
                parseResource(socket, request);

                if(request != null && request.toString().length() != 0) {

                }
                System.out.println("Thread count: " + Thread.activeCount());
                System.out.println("Thread " + Thread.currentThread() + " is currently ");

                Thread.yield();
                // stopThread();
                // wait();
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }

    public static synchronized void parseResource(Socket client, StringBuilder req) throws IOException {
        reqArr = req.toString().split("\\r?\\n", 10);
        // * Get the first line of the request; Get "resource" and "method" from first line
        String firstLine = reqArr[0];
        // System.out.println(firstLine);
        // * Split by whitespace for as many elements are in the first line
        String requestLine[] = firstLine.split(" ", 0);
        String method = requestLine[0];
        String resource = requestLine[1];


        httpdConfig.printAll();

        // * Create and format Date field
        String dateTimePattern = "EEE, d MMM yyyy HH:mm:ss z";
        SimpleDateFormat sdf = new SimpleDateFormat(dateTimePattern);
        dateTime = sdf.format(new Date());

        // * Get document roots and index from hash Map
        documentRoot = httpdConfig.getDocumentRoot("DocumentRoot");
        directoryIndex = httpdConfig.getDirectoryIndex();

        // * Check if aliased uri
        String tempAlias = "Alias " + resource;
        if(httpdConfig.getMap().containsKey(tempAlias)) {
            dirAlias = httpdConfig.getMap().get(tempAlias);
        } else if(resource.contains("/ab/")) {
            dirAlias = httpdConfig.getMap().get("Alias /ab/");
        } else if(resource.contains("/~traciely/")) {
            dirAlias = httpdConfig.getMap().get("Alias /~traciely/");
        } else if(resource.contains("/cgi-bin/")) {
            dirAlias = httpdConfig.getMap().get("ScriptAlias /cgi-bin/");
            scriptAlias = dirAlias;
        } else {
            documentRoot = documentRoot.substring(0, documentRoot.lastIndexOf("/"));
            
            dirAlias = documentRoot + resource;
            System.out.println("first dirAlias: " + dirAlias);
        }

        // * Check if requesting file
        // * Get the content type by looping through the set of keys which are string arrays that contain the extensions
        if(resource.contains(".")) {
            extension = resource.substring(resource.lastIndexOf(".") + 1);
            System.out.println("Extension: " + extension);
            for(String[] eleArray : mimeTypes.getMap().keySet()) {
                for(int j = 0; j < eleArray.length; j++) {
                    if(eleArray[j].equals(extension)) {
                        contentType = mimeTypes.getMap().get(eleArray);
                    }
                }
            }
        } else if(resource.contains("cgi-bin")) {
            scriptName = resource.substring(resource.lastIndexOf("/") + 1);
            System.out.println("scriptName: " + scriptName);
            dirAlias = dirAlias + scriptName;
            System.out.println("📜 Script detected for resource " + resource);
            System.out.println("dirAlias: " + dirAlias);
        } else {
            // * If the resource is not a file, append index.html to the end
            dirAlias = dirAlias + directoryIndex;
            contentType = "text/html";            
        }

        // * Access Check
        htAccessPath = httpdConfig.getAccessFile();
        if(!htAccessExist()) {
            htAccess = new HtAccess(htAccessPath);
            System.out.println("htAccessPath: " + htAccessPath);
            htAccess.read();
            htAccess.getAccessMap().entrySet();
            getAuthHeader(reqArr);
            checkAccess(client);
        }

        // * If the file exists, continue to check the request method (GET, POST, HEAD, etc.)
        // * Else, respond with a 400 response
        // TODO: Put 404 response into its own method
        // TODO: FNFExcept - resource: /ab/images/sushi.jpg; !solution: add 404 response for this case
        System.out.println("⏳ Checking if the requested file " + dirAlias + " exists...");
        if(fileExists(dirAlias)) {
            System.out.println("✅ File exists!");
            if(isScriptAlias()) {
                execScript(client, reqArr);
            } else {
                checkRequestVerb(client, method, resource);
            }
        } else {
            send404Response(client);
        }
    }

    private static boolean checkAccess(Socket client) {
        System.out.println("⏳ Checking if .htaccess exists...");
        // * If htaccess exists, get headers for auth
        // * Else, return 401 response
        try {
            htPassword = new Htpassword(htAccess.getAuthUserFile());
            if(authHeadersExist()) {
                if(htPassword.isAuthorized(authInfo)) {
                    return true;
                } else {
                    System.out.println("❌ Username/Password incorrect!");
                    send403Response(client);
                    stopThread();
                }
            } else {
                System.out.println("❌ Auth headers not found!");
                send401Response(client);
                stopThread();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
    // * Helper functions for access check
    private static synchronized boolean htAccessExist() {
        // System.out.println("htaccess contents: " + htAccess.getReq());
        System.out.println("✅ .htaccess exists!");
        if(htAccessPath.length() != 0) {
            return true;
        }
        return false;
    }

    private static synchronized boolean authHeadersExist() {
        if(authWWWInfo.isBlank() || authInfo.isBlank()) {
            return false;
        }
        return true;
    }

    private static void getAuthHeader(String[] reqArr) {
        System.out.println("authInfo: " + authInfo + "\nauthWWWInfo: " + authWWWInfo);
        for(String str : reqArr) {
            if(str.contains("WWW-Authenticate")) {
                authWWWInfo = str.substring(str.indexOf(":") + 1);
            } else {
                authWWWInfo = "";
            }
            if(str.contains("Authorization")) {
                authInfo = str.substring(str.indexOf(":") + 1);
            } else {
                authInfo = "";
            }
        }
    }

    // * Check if file exists helper function
    private static synchronized boolean fileExists(String dirAlias) {
        File file = new File(dirAlias);
        if(file.exists()) {
            return true;
        }
        return false;
    }

    private static synchronized boolean isScriptAlias() {
        if(scriptAlias != null) {
            return true;
        }
        return false;
    }

    private synchronized static void stopThread() {
        running.set(false);
    }

    private static synchronized void checkRequestVerb(Socket client, String method, String resource) throws IOException {
        // * Switch on method
        // sixthLines = reqArr[5];
        // fifthLine = reqArr[4];
        switch (method) {
            case "GET":
                getRequest(client, resource);
                break;
            case "HEAD":
                headRequest(client, resource);
                break;
            case "POST":
                // postRequest(client, resource, fifthLine, sixthLine);
                break;
            case "PUT":
                // putRequest(client, resource, fifthLine, sixthLines);
                break;
            case "DELETE":
                deleteRequest(client, resource);
                break;
            default:
                OutputStream clientOutput = client.getOutputStream();
                send500Response(clientOutput);
        }
    }

    private static synchronized void getRequest(Socket client, String resource) throws IOException {
        System.out.println("GET request resource from: " + resource);
        PrintWriter pw = new PrintWriter(client.getOutputStream());

        // System.out.println("Content type: " + contentType);

        // Worker.scriptAlias = httpdConfig.getScriptAlias("scriptAlias");

        // System.out.println("mimetypes: " + mimeTypes.getMap().entrySet());
        System.out.println("Socket object: " + client);
        System.out.println("resource: " + resource);
        // * Compare the "resource" to our list of resources
        if (resource.equals("/")) {
            String defaultIndex = documentRoot + resource + directoryIndex;
            System.out.println("default index: " + defaultIndex);

            File indexHTML = new File(defaultIndex.toString());
            FileInputStream fis = new FileInputStream(indexHTML);
            // FileInputStream indexHTML = new FileInputStream("./public_html/ab1/ab2/index.html");
            OutputStream clientOutput = client.getOutputStream();
            contentLength = indexHTML.length();
            // * Call 200 response method
            co_response_200(clientOutput, fis);
            // clientOutput.write(indexHTML.toString().getBytes());
            clientOutput.flush();
            // System.out.println("2: " + client);
        } else if (resource.contains("/image")) {
            // Load the image from the filesystem
            // FileInputStream image = new FileInputStream("./public_html/images/sushfi.jpg");
            String imagePath = documentRoot + resource;
            FileInputStream fis = new FileInputStream(imagePath);
            File image = new File(imagePath);
            OutputStream clientOutput = client.getOutputStream();
            String fileName = resource.substring(resource.lastIndexOf(".") - resource.lastIndexOf("/") + 2);
            System.out.println("filename: " + fileName);
            contentLength = image.length();

            // * Call 200 response method
            co_response_200(clientOutput, fis);
            clientOutput.flush();
            // image.close();
        } else if (resource.contains("/ab/")) {
            System.out.println("1: " + client);
            File indexHTML = new File(dirAlias.toString());
            FileInputStream fis = new FileInputStream(indexHTML);
            OutputStream clientOutput = client.getOutputStream();
            contentLength = indexHTML.length();

            co_response_200(clientOutput, fis);

            clientOutput.flush();
        } else if (resource.contains("/~traciely/")) {
            File indexHTML = new File(dirAlias.toString());
            FileInputStream fis = new FileInputStream(indexHTML);
            OutputStream clientOutput = client.getOutputStream();
            contentLength = indexHTML.length();
            // * Call 200 response method
            co_response_200(clientOutput, fis);

            clientOutput.flush();
        } else if (resource.equals("/protected/")) {
            FileInputStream indexHTML = new FileInputStream(dirAlias);
            OutputStream clientOutput = client.getOutputStream();
            clientOutput.write(("HTTP/1.1 200 OK\r\n").getBytes());
            clientOutput.write(("\r\n").getBytes());
            clientOutput.write(indexHTML.readAllBytes());
            indexHTML.close();
            clientOutput.flush();
        } else {
            OutputStream clientOutput = client.getOutputStream();
            send500Response(clientOutput);
            clientOutput.flush();
        }
    }
    // ! pw.print(("HTTP/1.1 500 Internal Server Error\r\n"));
    // ! pw.print(("HTTP/1.1 304 Not Modified\r\n"));

    private static void headRequest(Socket client, String resource) throws IOException {
        System.out.println("HEAD request resource from: " + resource);
        PrintWriter pw = new PrintWriter(client.getOutputStream());
        LocalDateTime dateTime = LocalDateTime.now();

        if (resource.equals("/")) {
            // Load the image from the filesystem
            FileInputStream indexHTML = new FileInputStream("./public_html/ab1/ab2/index.html");
            OutputStream clientOutput = client.getOutputStream();
            clientOutput.write(("HTTP/1.1 200 OK\r\n").getBytes());
            clientOutput.write(("Date: " + dateTime.toString() + "\r\n").getBytes());
            // Server
            clientOutput.write(("Server: " + server + "\r\n").getBytes());
            // Content-Length
            clientOutput.write(("Content-Length: " + contentLength + "\r\n").getBytes());
            // Content-Type
            clientOutput.write(("Content-Type: text/html; charset=utf-8" + "\r\n").getBytes());
            clientOutput.write(indexHTML.readAllBytes());
            indexHTML.close();
            clientOutput.flush();
        } else if (resource.equals("/400")) {
            // Status code
            pw.print(("HTTP/1.1 400 Bad Request\r\n"));
            // Date
            pw.print(("Date: " + dateTime.toString()));
            pw.print("\r\n");
            // Server
            pw.print(("Server: " + server));
            pw.print("\r\n");
            // Content-Length
            pw.print(("Content-Length: " + contentLength));
            pw.print("\r\n");
            // Content-Type
            pw.print(("Content-Type: text/html; charset=utf-8"));
            pw.print("\r\n");
            pw.flush();
        } else if (resource.equals("/500")) {
            // Status code
            pw.print(("HTTP/1.1 500 Internal Server Error\r\n"));
            // Date
            pw.print(("Date: " + dateTime.toString()));
            pw.print("\r\n");
            // Server
            pw.print(("Server: " + server));
            pw.print("\r\n");
            // Content-Length
            pw.print(("Content-Length: " + contentLength));
            pw.print("\r\n");
            // Content-Type
            pw.print(("Content-Type: text/html; charset=utf-8"));
            pw.print("\r\n");
            pw.flush();
        } else {
            // Status code
            pw.print(("HTTP/1.1 404 Not Found\r\n"));
            // Date
            pw.print(("Date: " + dateTime.toString()));
            pw.print("\r\n");
            // Server
            pw.print(("Server: " + server));
            pw.print("\r\n");
            // Content-Length
            pw.print(("Content-Length: " + contentLength));
            pw.print("\r\n");
            // Content-Type
            pw.print(("Content-Type: text/html; charset=utf-8"));
            pw.print("\r\n");
            pw.flush();
        }
    }

    private static void postRequest(Socket client, String resource, String fifthLine, String sixthLine)
            throws IOException {
        System.out.println("POST request resource from: " + resource);
        LocalDateTime dateTime = LocalDateTime.now();

        OutputStream clientOutput = client.getOutputStream();
        clientOutput.write(("HTTP/1.1 200 OK\r\n").getBytes());
        clientOutput.write(("\r\n").getBytes());
        // Status Code
        clientOutput.write(("HTTP/1.1 200 OK\r\n").getBytes());
        // Date
        clientOutput.write(("Date: " + dateTime.toString() + "\r\n").getBytes());
        // Server
        clientOutput.write(("Server: " + server + "\r\n").getBytes());
        // Content-Length
        clientOutput.write((fifthLine + "\r\n").getBytes());
        // Content-Type
        clientOutput.write((sixthLine + "\r\n").getBytes());
        clientOutput.flush();
    }

    private static void putRequest(Socket client, String resource, String fifthLine, String sixthLines)
            throws IOException {
        System.out.println("PUT request resource from: " + resource);
        LocalDateTime dateTime = LocalDateTime.now();

        OutputStream clientOutput = client.getOutputStream();
        clientOutput.write(("HTTP/1.1 201 Created\r\n").getBytes());
        clientOutput.write(("\r\n").getBytes());
        // Status Code
        clientOutput.write(("HTTP/1.1 201 Created" + "\r\n").getBytes());
        // Date
        clientOutput.write(("Date: " + dateTime.toString() + "\r\n").getBytes());
        // Server
        clientOutput.write(("Server: " + server + "\r\n").getBytes());
        // Content-Length
        clientOutput.write((fifthLine + "\r\n").getBytes());
        // Content-Type
        clientOutput.write((sixthLines + "\r\n").getBytes());
        clientOutput.flush();
    }

    private static void deleteRequest(Socket client, String resource) throws IOException {
        System.out.println("DELETE request resource from: " + resource);
        LocalDateTime dateTime = LocalDateTime.now();

        OutputStream clientOutput = client.getOutputStream();
        clientOutput.write(("HTTP/1.1 200 OK\r\n").getBytes());
        clientOutput.write(("\r\n").getBytes());
        // Status Code
        clientOutput.write(("HTTP/1.1 204 No Content" + "\r\n").getBytes());
        // Date
        clientOutput.write(("Date: " + dateTime.toString() + "\r\n").getBytes());
        // Server
        clientOutput.write(("Server: " + server + "\r\n").getBytes());
        // Content-Length
        clientOutput.write(("Content-Length: " + contentLength + "\r\n").getBytes());
        // Content-Type
        clientOutput.write(("Content-Type: text/html; charset=utf-8" + "\r\n").getBytes());
        clientOutput.flush();
    }


    // * Execute scipt method
    private static synchronized void execScript(Socket client, String[] reqArr) {
        System.out.println("🔨 Execute Script...");
        try {
            FileInputStream indexHTML = new FileInputStream(dirAlias);
            OutputStream clientOutput = client.getOutputStream();
            clientOutput.write(("HTTP/1.1 200 OK").getBytes());
            clientOutput.write(("\r\n").getBytes());
            clientOutput.write(("Date: " + dateTime).getBytes());
            clientOutput.write(("\r\n").getBytes());
            clientOutput.write(("Server: " + server).getBytes());
            clientOutput.write(("\r\n").getBytes());
            clientOutput.write(("\r\n").getBytes());
            clientOutput.write(indexHTML.readAllBytes());
            new RunScript(reqArr);
            // clientOutput.write(("\r\n").getBytes());
            RunScript.start();
            indexHTML.close();
            clientOutput.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // * Print functions
    private static void pw_response_200(PrintWriter pw) {
        LocalDateTime dateTime = LocalDateTime.now();
        pw.print(("HTTP/1.1 200 OK"));
        pw.print("\r\n");
        // Date
        pw.print(("Date: " + dateTime.toString()));
        pw.print("\r\n");
        // Server
        pw.print(("Server: " + server));
        pw.print("\r\n");
        // Content-Length
        pw.print(("Content-Length: " + contentLength));
        pw.print("\r\n");
        // Content-Type
        pw.print(("Content-Type: text/html; charset=utf-8"));
        pw.print("\r\n");
    }

    private static synchronized void co_response_200(OutputStream clientOutput, FileInputStream fis) {
        try {
            clientOutput.write(("HTTP/1.1 200 OK").getBytes());
            clientOutput.write(("\r\n").getBytes());
            clientOutput.write(("Date: " + dateTime).getBytes());
            clientOutput.write(("\r\n").getBytes());
            clientOutput.write(("Server: " + server).getBytes());
            clientOutput.write(("\r\n").getBytes());
            clientOutput.write(("Content-Length: " + contentLength).getBytes());
            clientOutput.write(("\r\n").getBytes());
            clientOutput.write(("Connection: Keep-Alive").getBytes());
            clientOutput.write(("\r\n").getBytes());
            clientOutput.write(("Content-Type: " + contentType + "; charset=utf-8").getBytes());
            clientOutput.write(("\r\n").getBytes());
            clientOutput.write(("Content-Language: en").getBytes());
            clientOutput.write(("\r\n").getBytes());
            // clientOutput.write(("Transfer-Encoding: chunked").getBytes());
            // clientOutput.write(("\r\n").getBytes());
            // clientOutput.write(("Content-Encoding: gzip").getBytes());
            // clientOutput.write(("\r\n").getBytes());
            // clientOutput.write(("Keep-Alive: timeout=5, max=999").getBytes());
            // clientOutput.write(("\r\n").getBytes());
            clientOutput.write(("Vary: Accept-Encoding").getBytes());
            clientOutput.write(("\r\n").getBytes());
            clientOutput.write(("\r\n").getBytes());
            clientOutput.write((fis).readAllBytes());
            clientOutput.write(("\r\n").getBytes());
            clientOutput.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static synchronized void printRequest(StringBuilder request) {
        System.out.println("--REQUEST--");
        System.out.println("Request: " + request);
    }

    public static synchronized void send401Response(Socket socket) throws IOException {
        // Status code
        OutputStream clientOutput = socket.getOutputStream();
        // Status code
        clientOutput.write(("HTTP/1.1 401 Unauthorized").getBytes());
        clientOutput.write(("\r\n").getBytes());
        // Date
        clientOutput.write(("Date: " + dateTime).getBytes());
        clientOutput.write(("\r\n").getBytes());
        // Server
        clientOutput.write(("Server: " + server).getBytes());
        clientOutput.write(("\r\n").getBytes());
        // Content-Length
        clientOutput.write(("Content-Length: " + contentLength).getBytes());
        clientOutput.write(("\r\n").getBytes());
        // Content-Type
        clientOutput.write(("Content-Type: " + contentType).getBytes());
        clientOutput.write(("\r\n").getBytes());
        clientOutput.flush();
    }

    public static synchronized void send403Response(Socket socket) throws IOException {
        // PrintWriter pw = new PrintWriter(socket.getOutputStream());
        // Status code
        // pw.print(("WWW-Authenticate: Basic realm=\"Access to staging site\""));
        // pw.print("\r\n");

        OutputStream clientOutput = socket.getOutputStream();
        // Status code
        clientOutput.write(("HTTP/1.1 403 Forbidden").getBytes());
        clientOutput.write(("\r\n").getBytes());
        // Date
        clientOutput.write(("Date: " + dateTime).getBytes());
        clientOutput.write(("\r\n").getBytes());
        // Server
        clientOutput.write(("Server: " + server).getBytes());
        clientOutput.write(("\r\n").getBytes());
        // Content-Length
        clientOutput.write(("Content-Length: " + contentLength).getBytes());
        clientOutput.write(("\r\n").getBytes());
        // Content-Type
        clientOutput.write(("Content-Type: " + contentType).getBytes());
        clientOutput.write(("\r\n").getBytes());
        clientOutput.flush();
    }
    
    public static synchronized void send404Response(Socket socket) throws IOException {
        System.out.println("❌ File not found!");
        OutputStream clientOutput = socket.getOutputStream();
        // Status code
        clientOutput.write(("HTTP/1.1 404 Not Found").getBytes());
        clientOutput.write(("\r\n").getBytes());
        // Date
        clientOutput.write(("Date: " + dateTime).getBytes());
        clientOutput.write(("\r\n").getBytes());
        // Server
        clientOutput.write(("Server: " + server).getBytes());
        clientOutput.write(("\r\n").getBytes());
        // Content-Length
        clientOutput.write(("Content-Length: " + contentLength).getBytes());
        clientOutput.write(("\r\n").getBytes());
        // Content-Type
        clientOutput.write(("Content-Type: " + contentType).getBytes());
        clientOutput.write(("\r\n").getBytes());
        clientOutput.flush();
    }

    public static synchronized void send500Response(OutputStream clientOutput) throws IOException {
        clientOutput.write(("HTTP/1.1 500 Internal Server Error").getBytes());
        clientOutput.write(("\r\n").getBytes());
        clientOutput.write(("Date: " + dateTime).getBytes());
        clientOutput.write(("\r\n").getBytes());
        clientOutput.write(("Server: " + server).getBytes());
        clientOutput.write(("\r\n").getBytes());
        clientOutput.flush();
    }
}
