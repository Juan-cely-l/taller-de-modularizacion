package edu.escuelaing.arep;

import java.io.*;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpServer {

    private static final int PORT = Integer.parseInt(
    System.getenv().getOrDefault("PORT", "8080")
);
    private static final String WEBROOT = "/webroot";

    private final Map<String, Method> routes;
    private final Map<String, Object> instances;
    private boolean running = true;

    public HttpServer(Map<String, Method> routes, Map<String, Object> instances) {
        this.routes = routes;
        this.instances = instances;
    }

    /**
     * Main loop: accepts one connection, serves it, and continues.
     * "Non-concurrent" = one request at a time, with no extra threads.
     */
    public void start() throws IOException {
        ExecutorService threadPool=Executors.newCachedThreadPool();
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Listening on http://localhost:" + PORT);
            while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                threadPool.submit(() -> {
                    try {
                        handleRequest(clientSocket);
                    } catch (IOException e) {
                        System.err.println("Error: " + e.getMessage());
                    }
                });
            } catch (java.net.SocketTimeoutException e) {
            }
        }
    } finally {
        threadPool.shutdown();
    }
}

    /**
     * Handles an HTTP request.
     *
     * HTTP/1.1 structure:
     *   Line 1:  METHOD PATH VERSION\r\n
     *   Line 2+: headers\r\n
     *   Empty line: \r\n  (separates headers and body)
     *   Body (optional for GET, usually absent)
     *
     * For GET, only line 1 is required.
     */
    void handleRequest(Socket clientSocket) throws IOException {
        try (
            BufferedReader in = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream()));
            OutputStream out = clientSocket.getOutputStream()
        ) {
            String requestLine = in.readLine();
            if (requestLine == null || requestLine.isBlank()) return;

            System.out.println("-> " + requestLine);

            // "GET /greeting?name=Carlos HTTP/1.1" -> parts[0]="GET" parts[1]="/greeting?name=Carlos"
            String[] parts = requestLine.split(" ");
            if (parts.length < 2) return;

            String method  = parts[0];
            String fullUri = parts[1];

            if (!"GET".equalsIgnoreCase(method)) {
                sendResponse(out, "405 Method Not Allowed", "text/plain",
                             "Only GET is supported".getBytes());
                return;
            }

            // Split path and query string
            String path;
            Map<String, String> queryParams = new HashMap<>();
            int qIdx = fullUri.indexOf('?');
            if (qIdx >= 0) {
                path = fullUri.substring(0, qIdx);        // "/greeting"
                queryParams = parseQueryString(fullUri.substring(qIdx + 1));  // "name=Carlos"
            } else {
                path = fullUri;
            }

            // Decide how to respond
            if (isStaticFile(path)) {
                serveStaticFile(out, path);
            } else {
                serveController(out, path, queryParams);
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private boolean isStaticFile(String path) {
        return path.endsWith(".html") || path.endsWith(".png") ||
               path.endsWith(".css")  || path.endsWith(".js")  ||
               path.endsWith(".jpg")  || path.endsWith(".ico");
    }

    /**
     * Serves static files from the classpath under /webroot.
     * getResourceAsStream() looks inside the JAR or target/classes.
     */
    private void serveStaticFile(OutputStream out, String path) throws IOException {
        InputStream resource = getClass().getResourceAsStream(WEBROOT + path);
        if (resource == null) {
            sendResponse(out, "404 Not Found", "text/plain",
                        ("404: " + path + " not found").getBytes());
            return;
        }
        byte[] body = resource.readAllBytes();
        sendResponse(out, "200 OK", detectContentType(path), body);
    }

    /**
     * Content-Type MUST be correct:
     * - "image/png" for PNG: browser interprets bytes as image.
     * - "text/html" for HTML: browser renders a page.
     * Sending PNG as text/plain would produce garbage.
     */
    private String detectContentType(String path) {
        if (path.endsWith(".html")) return "text/html; charset=utf-8";
        if (path.endsWith(".png"))  return "image/png";
        if (path.endsWith(".jpg"))  return "image/jpeg";
        if (path.endsWith(".css"))  return "text/css";
        if (path.endsWith(".js"))   return "application/javascript";
        return "application/octet-stream";
    }

    private void serveController(OutputStream out, String path,
                                  Map<String, String> queryParams) throws IOException {
        Method method = routes.get(path);
        if (method == null) {
            sendResponse(out, "404 Not Found", "text/plain",
                        ("404: no controller found for " + path).getBytes());
            return;
        }
        try {
            String result = MicroSpringBoot.invoke(method, instances.get(path), queryParams);
            sendResponse(out, "200 OK", "text/plain; charset=utf-8", result.getBytes());
        } catch (Exception e) {
            sendResponse(out, "500 Internal Server Error", "text/plain",
                        ("Error: " + e.getMessage()).getBytes());
        }
    }

    /**
     * Parses "name=Carlos&age=30" -> {"name":"Carlos", "age":"30"}
     */
    Map<String, String> parseQueryString(String qs) {
        Map<String, String> params = new HashMap<>();
        if (qs == null || qs.isBlank()) return params;
        for (String pair : qs.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                params.put(pair.substring(0, eq).trim(), pair.substring(eq + 1).trim());
            }
        }
        return params;
    }

    /**
     * Writes the full HTTP response.
     *
     * Required structure:
     *   HTTP/1.1 {status}\r\n
     *   Content-Type: {contentType}\r\n
     *   Content-Length: {bytes}\r\n
     *   Connection: close\r\n
     *   \r\n
     *   {body bytes}
     */
    void sendResponse(OutputStream out, String status,
                      String contentType, byte[] body) throws IOException {
        String header = "HTTP/1.1 " + status + "\r\n"
                      + "Content-Type: " + contentType + "\r\n"
                      + "Content-Length: " + body.length + "\r\n"
                      + "Connection: close\r\n"
                      + "\r\n";
        out.write(header.getBytes());
        out.write(body);
        out.flush();
    }

    public void stop() { this.running = false; }
}
