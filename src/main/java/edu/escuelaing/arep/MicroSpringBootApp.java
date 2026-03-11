package edu.escuelaing.arep;


public class MicroSpringBootApp {

    private static final String BASE_PACKAGE = "edu.escuelaing.arep";

    public static void main(String[] args) throws Exception {
        System.out.println("=== Starting MicroSpringBoot Framework ===");

        // 1. Scan classpath and register controllers
        ClassPathScanner.scan(BASE_PACKAGE);

        System.out.println("Available routes: " + MicroSpringBoot.getRoutes().keySet());

        // 2. Start HTTP server
        HttpServer server = new HttpServer(
            MicroSpringBoot.getRoutes(),
            MicroSpringBoot.getInstances()
        );
        Runtime.getRuntime().addShutdownHook(new Thread(()->{
            System.out.println("=== Turn Down Signal Recieved ===");
            server.stop();
            System.out.println("=== Stopping MicroSpringBoot Framework ===");
        }
    
    ));
    server.start();
    }
}
