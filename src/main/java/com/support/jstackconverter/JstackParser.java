package com.support.jstackconverter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JstackParser {

    // Compiled once instead of on every parseThread call
    // Header: "name" #52 daemon prio=5 os_prio=0 cpu=203.12ms ...
    // (#id is optional: JVM-internal threads like "VM Thread" don't have one)
    private static final Pattern HEADER_PATTERN = Pattern.compile("^\"([^\"]+)\"(.*)$");
    private static final Pattern ID_PATTERN = Pattern.compile("#(\\d+)");
    private static final Pattern DAEMON_PATTERN = Pattern.compile("\\sdaemon(\\s|$)");
    private static final Pattern CPU_PATTERN = Pattern.compile("\\bcpu=([\\d.]+)ms");

    private static final Pattern STATE_PATTERN = Pattern.compile(
            "^\\s*java\\.lang\\.Thread\\.State:\\s+(\\w+)");

    private static final Pattern WAITING_PATTERN = Pattern.compile(
            "^\\s*-\\s+(?:parking to wait for|waiting on)\\s+<([^>]+)>\\s+\\(([^)]+)\\)");

    // "- waiting on <no object reference available>": the object isn't named,
    // so the lock is taken from the "- locked" line that follows
    private static final Pattern WAITING_UNKNOWN_PATTERN = Pattern.compile(
            "^\\s*-\\s+waiting on\\s+<no object reference available>");

    private static final Pattern LOCKED_PATTERN = Pattern.compile(
            "^\\s*-\\s+locked\\s+<([^>]+)>\\s+\\(([^)]+)\\)");

    private static final Pattern STACK_PATTERN = Pattern.compile("^\\s*at\\s+(.+)$");

    // Frame with a module inside the parens, e.g.
    // java.lang.Object.wait(java.base@17.0.19/Native Method)
    private static final Pattern MODULE_FRAME_PATTERN = Pattern.compile(
            "^(.+?)\\(([^()/]+)/([^()]*)\\)$");

    public JstackParser() {
    }

    /**
     * Opens the file using the right charset. Thread dumps saved on Windows
     * (e.g. `jstack <pid> > dump.txt` in PowerShell) are often UTF-16 with a
     * BOM. FileReader decodes those as single-byte text, which leaves a stray
     * \0 character after every character and at the start of every line, so
     * startsWith("\"") fails and every line seems to be followed by an empty one.
     */
    private static BufferedReader openReader(String path) throws IOException {
        PushbackInputStream in = new PushbackInputStream(new FileInputStream(path), 3);

        byte[] bom = new byte[3];
        int n = in.read(bom, 0, 3);

        Charset charset = StandardCharsets.UTF_8;
        int skip = 0;

        if (n >= 2 && (bom[0] & 0xFF) == 0xFF && (bom[1] & 0xFF) == 0xFE) {
            charset = StandardCharsets.UTF_16LE;
            skip = 2;
        } else if (n >= 2 && (bom[0] & 0xFF) == 0xFE && (bom[1] & 0xFF) == 0xFF) {
            charset = StandardCharsets.UTF_16BE;
            skip = 2;
        } else if (n >= 3 && (bom[0] & 0xFF) == 0xEF && (bom[1] & 0xFF) == 0xBB && (bom[2] & 0xFF) == 0xBF) {
            skip = 3; // UTF-8 BOM
        }

        // Put back any bytes that weren't part of the BOM
        if (n > skip) {
            in.unread(bom, skip, n - skip);
        }

        return new BufferedReader(new InputStreamReader(in, charset));
    }

    public ArrayList<Thread> parseFile(String path) throws IOException {
        ArrayList<Thread> threads = new ArrayList<>();

        try (BufferedReader bfro = openReader(path)) {
            // null means "not inside a thread yet" (skips the dump preamble)
            StringBuilder currThread = null;
            String line;

            while ((line = bfro.readLine()) != null) {

                // A new thread starts with a quotation mark
                if (line.startsWith("\"")) {

                    // Parse the previous thread (skip JVM-internal threads with no state)
                    if (currThread != null && hasThreadState(currThread.toString())) {
                        threads.add(parseThread(currThread.toString()));
                    }

                    currThread = new StringBuilder();
                }

                // Add the line (including the header line) to the current thread
                if (currThread != null) {
                    currThread.append(line).append("\n");
                }
            }

            // Parse the final thread (same check)
            if (currThread != null && hasThreadState(currThread.toString())) {
                threads.add(parseThread(currThread.toString()));
            }
        }

        return threads;
    }

    // Only real Java threads have a "java.lang.Thread.State:" line
    private static boolean hasThreadState(String thread) {
        return thread.contains("java.lang.Thread.State:");
    }

    // Moves the module prefix to the front:
    // java.lang.Object.wait(java.base@17.0.19/Native Method)
    //   -> java.base@17.0.19/java.lang.Object.wait(Native Method)
    // Frames without a module (e.g. org.eclipse.jetty...(Foo.java:12)) are unchanged.
    private static String formatFrame(String frame) {
        Matcher m = MODULE_FRAME_PATTERN.matcher(frame);

        if (m.matches()) {
            return m.group(2) + "/" + m.group(1) + "(" + m.group(3) + ")";
        }

        return frame;
    }

    // jstack prints types as "(a java.lang.Object)"; drop the leading "a "/"an "
    private static String cleanType(String type) {
        return type.replaceFirst("^an?\\s+", "");
    }

    public Thread parseThread(String thread) {
        String name = null;
        int id = 0;
        ThreadState state = null;
        boolean daemon = false;
        String system = "test";
        String scope = "test";
        float cpuUsage = 0;

        Map<String, String> waitingFor = new HashMap<>();
        ArrayList<String> stacktrace = new ArrayList<>();
        ArrayList<Map<String, String>> lockedMonitors = new ArrayList<>();

        boolean waitingOnUnknown = false;

        for (String line : thread.split("\n")) {

            // Header: name, id, daemon, cpu
            Matcher matcher = HEADER_PATTERN.matcher(line);

            if (matcher.find()) {
                name = matcher.group(1);
                String rest = matcher.group(2);

                Matcher idMatcher = ID_PATTERN.matcher(rest);
                if (idMatcher.find()) {
                    id = Integer.parseInt(idMatcher.group(1));
                }

                daemon = DAEMON_PATTERN.matcher(rest).find();

                Matcher cpuMatcher = CPU_PATTERN.matcher(rest);
                if (cpuMatcher.find()) {
                    cpuUsage = Float.parseFloat(cpuMatcher.group(1));
                }

                continue;
            }

            // Thread state
            matcher = STATE_PATTERN.matcher(line);

            if (matcher.find()) {
                try {
                    state = ThreadState.valueOf(matcher.group(1));
                } catch (IllegalArgumentException e) {
                    // Unknown state name; leave as null
                }
                continue;
            }

            // Waiting for lock
            matcher = WAITING_PATTERN.matcher(line);

            if (matcher.find()) {
                String address = matcher.group(1).replaceFirst("^0x0*(?=.)", "");
                String type = cleanType(matcher.group(2));

                waitingFor.put("lock", type + "@" + address);
                continue;
            }

            // Waiting on an unnamed object: resolve it from the next "- locked" line
            if (WAITING_UNKNOWN_PATTERN.matcher(line).find()) {
                waitingOnUnknown = true;
                continue;
            }

            // Locked monitor. In a jstack dump, a "- locked" line comes right
            // AFTER the frame in which the lock was taken, so tie it to the
            // most recent stack frame.
            matcher = LOCKED_PATTERN.matcher(line);

            if (matcher.find()) {
                String address = matcher.group(1).replaceFirst("^0x0*(?=.)", "");
                String type = cleanType(matcher.group(2));
                String lock = type + "@" + address;

                if (waitingOnUnknown && waitingFor.isEmpty()) {
                    waitingFor.put("lock", lock);
                    waitingOnUnknown = false;
                }

                Map<String, String> monitor = new HashMap<>();
                monitor.put("lock", lock);

                if (!stacktrace.isEmpty()) {
                    monitor.put("frame", stacktrace.get(stacktrace.size() - 1));
                }

                lockedMonitors.add(monitor);
                continue;
            }

            // Stack trace
            matcher = STACK_PATTERN.matcher(line);

            if (matcher.find()) {
                stacktrace.add(formatFrame(matcher.group(1)));
            }
        }

        if (waitingFor.isEmpty()) {
            waitingFor = null;
        }

        // A thread that is waiting on a lock doesn't report locked monitors
        if (waitingFor != null || lockedMonitors.isEmpty()) {
            lockedMonitors = null;
        }

        return new Thread.Builder()
                .name(name)
                .id(id)
                .state(state)
                .daemon(daemon)
                .system(system)
                .scope(scope)
                .cpuUsage(cpuUsage)
                .waitingFor(waitingFor)
                .stacktrace(stacktrace)
                .lockedMonitors(lockedMonitors)
                .build();
    }
}