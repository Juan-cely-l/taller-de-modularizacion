package edu.escuelaing.arep;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

class AppTest {

    @BeforeEach
    void setUp() {
        MicroSpringBoot.clearRoutes();
    }

    @Test
    @DisplayName("@RestController keeps its annotation at RUNTIME")
    void restControllerAnnotationRetention() throws Exception {
        var ret = RestController.class.getAnnotation(java.lang.annotation.Retention.class);
        assertNotNull(ret);
        assertEquals(java.lang.annotation.RetentionPolicy.RUNTIME, ret.value());
    }

    @Test
    @DisplayName("loadController registers HelloController routes")
    void loadControllerRegistersRoutes() throws Exception {
        MicroSpringBoot.loadController("edu.escuelaing.arep.HelloController");
        assertTrue(MicroSpringBoot.getRoutes().containsKey("/"));
        assertTrue(MicroSpringBoot.getRoutes().containsKey("/pi"));
        assertTrue(MicroSpringBoot.getRoutes().containsKey("/hello"));
    }

    @Test
    @DisplayName("loadController rejects classes without @RestController")
    void loadControllerRejectsNonController() {
        assertThrows(Exception.class,
            () -> MicroSpringBoot.loadController("java.lang.String"));
    }

    @Test
    @DisplayName("invoke runs pi() and returns the expected value")
    void invokePiReturnsCorrectValue() throws Exception {
        MicroSpringBoot.loadController("edu.escuelaing.arep.HelloController");
        Method m = MicroSpringBoot.getRoutes().get("/pi");
        String result = MicroSpringBoot.invoke(m, MicroSpringBoot.getInstances().get("/pi"),
                                               new HashMap<>());
        assertTrue(result.contains("3.14"));
    }

    @Test
    @DisplayName("invoke resolves @RequestParam with provided value")
    void invokeResolvesRequestParam() throws Exception {
        MicroSpringBoot.loadController("edu.escuelaing.arep.GreetingController");
        Method m = MicroSpringBoot.getRoutes().get("/greeting");
        String result = MicroSpringBoot.invoke(m, MicroSpringBoot.getInstances().get("/greeting"),
                                               Map.of("name", "Carlos"));
        assertTrue(result.contains("Carlos"));
    }

    @Test
    @DisplayName("invoke uses defaultValue when parameter is missing")
    void invokeUsesDefaultValue() throws Exception {
        MicroSpringBoot.loadController("edu.escuelaing.arep.GreetingController");
        Method m = MicroSpringBoot.getRoutes().get("/greeting");
        String result = MicroSpringBoot.invoke(m, MicroSpringBoot.getInstances().get("/greeting"),
                                               new HashMap<>());
        assertTrue(result.contains("World"));
    }

    @Test
    @DisplayName("parseQueryString correctly parses multiple parameters")
    void parseQueryStringMultipleParams() {
        HttpServer server = new HttpServer(new HashMap<>(), new HashMap<>());
        Map<String, String> params = server.parseQueryString("name=Carlos&age=30");
        assertEquals("Carlos", params.get("name"));
        assertEquals("30", params.get("age"));
    }

    @Test
    @DisplayName("sendResponse builds HTTP response with valid status and body")
    void sendResponseGeneratesValidHttp() throws Exception {
        HttpServer server = new HttpServer(new HashMap<>(), new HashMap<>());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] body = "Hello world".getBytes();
        server.sendResponse(baos, "200 OK", "text/plain", body);
        String response = baos.toString();
        assertTrue(response.startsWith("HTTP/1.1 200 OK"));
        assertTrue(response.contains("Content-Length: " + body.length));
        assertTrue(response.contains("Hello world"));
    }

    @Test
    @DisplayName("Server returns 200 for GET /hello")
    void serverHandlesGetHello() throws Exception {
        MicroSpringBoot.loadController("edu.escuelaing.arep.HelloController");
        HttpServer server = new HttpServer(
            MicroSpringBoot.getRoutes(), MicroSpringBoot.getInstances());

        String req = "GET /hello HTTP/1.1\r\nHost: localhost\r\n\r\n";
        ByteArrayInputStream in = new ByteArrayInputStream(req.getBytes());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Socket mockSocket = new Socket() {
            @Override public InputStream getInputStream() { return in; }
            @Override public OutputStream getOutputStream() { return out; }
        };

        server.handleRequest(mockSocket);
        String response = out.toString();
        assertTrue(response.contains("HTTP/1.1 200 OK"));
        assertTrue(response.contains("Hello, World!"));
    }

    @Test
    @DisplayName("Server returns 404 for unknown routes")
    void serverHandlesUnknownPath() throws Exception {
        MicroSpringBoot.loadController("edu.escuelaing.arep.HelloController");
        HttpServer server = new HttpServer(
            MicroSpringBoot.getRoutes(), MicroSpringBoot.getInstances());

        String req = "GET /not-found HTTP/1.1\r\nHost: localhost\r\n\r\n";
        ByteArrayInputStream in = new ByteArrayInputStream(req.getBytes());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Socket mockSocket = new Socket() {
            @Override public InputStream getInputStream() { return in; }
            @Override public OutputStream getOutputStream() { return out; }
        };

        server.handleRequest(mockSocket);
        assertTrue(out.toString().contains("404"));
    }
}
