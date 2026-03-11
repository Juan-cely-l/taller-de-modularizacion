package edu.escuelaing.arep;

import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClassPathScanner {

    public static void scan(String basePackage) throws Exception {
        String packagePath = basePackage.replace('.', '/');
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> resources = classLoader.getResources(packagePath);

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            String protocol = resource.getProtocol();

            if ("file".equals(protocol)) {
                // Running from IDE / exploded classes directory
                File directory = new File(resource.toURI());
                if (directory.exists()) {
                    for (String className : findClasses(directory, basePackage)) {
                        tryLoadController(className);
                    }
                }
            } else if ("jar".equals(protocol)) {
                // Running from a JAR: jar:file:/path/to/app.jar!/edu/escuelaing/arep
                String jarPath = resource.getPath(); // file:/path/to/app.jar!/edu/...
                String jarFilePath = jarPath.substring(5, jarPath.indexOf("!")); // strip "file:" and "!/..."
                try (JarFile jar = new JarFile(jarFilePath)) {
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        String name = entry.getName(); // e.g. "edu/escuelaing/arep/HelloController.class"
                        if (name.startsWith(packagePath) && name.endsWith(".class") && !entry.isDirectory()) {
                            String className = name.replace('/', '.').replace(".class", "");
                            tryLoadController(className);
                        }
                    }
                }
            }
        }
    }

    private static void tryLoadController(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            if (clazz.isAnnotationPresent(RestController.class)) {
                System.out.println("  [SCAN] Loading: " + className);
                MicroSpringBoot.loadController(className);
            }
        } catch (Exception e) {
            // Ignore classes that cannot be loaded
        }
    }

    private static List<String> findClasses(File directory, String packageName) {
        List<String> classes = new ArrayList<>();
        File[] files = directory.listFiles();
        if (files == null) return classes;

        for (File file : files) {
            if (file.isDirectory()) {
                classes.addAll(findClasses(file, packageName + "." + file.getName()));
            } else if (file.getName().endsWith(".class")) {
                classes.add(packageName + "." + file.getName().replace(".class", ""));
            }
        }
        return classes;
    }
}