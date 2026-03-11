package edu.escuelaing.arep;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

public class MicroSpringBoot {

    private static final Map<String, Method> routes = new HashMap<>();
    private static final Map<String, Object> instances = new HashMap<>();

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: MicroSpringBoot <class> <path>");
            System.exit(1);
        }
        loadController(args[0]);

        Method method = routes.get(args[1]);
        if (method == null) {
            System.out.println("404 - No @GetMapping found for: " + args[1]);
            return;
        }
        Object instance = instances.get(args[1]);
        System.out.println("Result: " + invoke(method, instance, new HashMap<>()));
    }

    /**
     * Loads the class, validates @RestController, and registers its @GetMapping methods.
     */
    public static void loadController(String className) throws Exception {
        Class<?> clazz = Class.forName(className);

        if (!clazz.isAnnotationPresent(RestController.class)) {
            throw new IllegalArgumentException(className + " does not have @RestController");
        }

        Object instance = clazz.getDeclaredConstructor().newInstance();

        for (Method m : clazz.getDeclaredMethods()) {
            if (m.isAnnotationPresent(GetMapping.class)) {
                String route = m.getAnnotation(GetMapping.class).value();
                routes.put(route, m);
                instances.put(route, instance);
                System.out.println("  Registered: GET " + route + " -> " + m.getName() + "()");
            }
        }
    }

    /**
     * Invokes a method resolving @RequestParam values from query params.
     */
    public static String invoke(Method method, Object instance,
                                Map<String, String> queryParams) throws Exception {
        Parameter[] params = method.getParameters();
        if (params.length == 0) {
            return (String) method.invoke(instance);
        }
        Object[] args = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            Parameter p = params[i];
            if (p.isAnnotationPresent(RequestParam.class)) {
                RequestParam rp = p.getAnnotation(RequestParam.class);
                args[i] = queryParams.getOrDefault(rp.value(), rp.defaultValue());
            } else {
                args[i] = null;
            }
        }
        return (String) method.invoke(instance, args);
    }

    public static Map<String, Method> getRoutes()    { return routes; }
    public static Map<String, Object> getInstances() { return instances; }
    public static void clearRoutes() { routes.clear(); instances.clear(); }
}
