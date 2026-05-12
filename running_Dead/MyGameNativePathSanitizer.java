package running_Dead;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Keeps JOGL from reading a malformed Windows PATH before the native libraries load.
 * Some lab machines glue "Muse Hub\lib" and VS Code's Java debug path together without
 * a semicolon, so the game relaunches itself once with only this process environment fixed.
 * Connected to: Called first by MyGame.main and NetworkingServer.main before JOGL/native libraries load.
 */
public final class MyGameNativePathSanitizer {
    private static final String BAD_WINDOWS_JOIN = "\\libc:\\";
    private static final String FIXED_WINDOWS_JOIN = "\\lib;c:\\";
    private static final String BAD_WINDOWS_JOIN_UPPER = "\\libC:\\";
    private static final String FIXED_WINDOWS_JOIN_UPPER = "\\lib;C:\\";
    private static final String RELAUNCH_MARKER = "RUNNING_DEAD_PATH_SANITIZED";
    private static boolean applied = false;

    private MyGameNativePathSanitizer() {
    }

    public static void apply() {
        if (applied) return;
        applied = true;
        sanitizeProperty("java.library.path");
    }

    public static void relaunchWithSanitizedPathIfNeeded(String mainClassName, String[] args) {
        String path = System.getenv("PATH");
        if (path == null || path.isEmpty()) return;

        String sanitizedPath = sanitizePathList(path);
        if (path.equals(sanitizedPath) || "1".equals(System.getenv(RELAUNCH_MARKER))) return;

        try {
            List<String> command = new ArrayList<>();
            command.add(javaExecutablePath());
            command.addAll(ManagementFactory.getRuntimeMXBean().getInputArguments());
            command.add("-cp");
            command.add(System.getProperty("java.class.path", "."));
            command.add(mainClassName);
            for (String arg : args) command.add(arg);

            ProcessBuilder builder = new ProcessBuilder(command);
            builder.inheritIO();
            Map<String, String> environment = builder.environment();
            setPathEnvironment(environment, sanitizedPath);
            environment.put(RELAUNCH_MARKER, "1");

            int exitCode = builder.start().waitFor();
            System.exit(exitCode);
        } catch (Exception e) {
            System.err.println("native path sanitizer warning --> unable to relaunch with sanitized PATH: " + e.getMessage());
        }
    }

    private static void sanitizeProperty(String propertyName) {
        String value = System.getProperty(propertyName);
        if (value == null || value.isEmpty()) return;
        String sanitized = sanitizePathList(value);
        if (!value.equals(sanitized)) System.setProperty(propertyName, sanitized);
    }

    private static String sanitizePathList(String value) {
        return value.replace(BAD_WINDOWS_JOIN, FIXED_WINDOWS_JOIN)
                .replace(BAD_WINDOWS_JOIN_UPPER, FIXED_WINDOWS_JOIN_UPPER);
    }

    private static String javaExecutablePath() {
        String javaHome = System.getProperty("java.home");
        if (javaHome == null || javaHome.isBlank()) return "java";
        return javaHome + File.separator + "bin" + File.separator + "java";
    }

    private static void setPathEnvironment(Map<String, String> environment, String sanitizedPath) {
        String pathKey = null;
        for (String key : environment.keySet()) {
            if ("PATH".equalsIgnoreCase(key)) {
                pathKey = key;
                break;
            }
        }
        environment.put(pathKey == null ? "PATH" : pathKey, sanitizedPath);
    }
}
