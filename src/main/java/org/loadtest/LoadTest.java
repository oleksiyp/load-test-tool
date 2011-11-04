package org.loadtest;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Tool to run test load specified by script using several threads.
 * Main entry point.
 */
public class LoadTest {
    private Options options;
    private ScheduledExecutorService executor;
    private ScheduledExecutorService printExecutor;
    private ScriptRunner scriptRunner;

    public LoadTest(int threads, long delay, File script) {
        this.options = new Options(threads, delay, script);
    }
    public LoadTest(Options options) {
        this.options = options;
    }

    public synchronized void start() {
        if (executor != null) {
            return;
        }
        scriptRunner = new ScriptRunner(this, options.scripts);
        executor = Executors.newScheduledThreadPool(options.threads);
        for (int i = 0; i < options.threads; i++) {
            executor.scheduleWithFixedDelay(scriptRunner,
                    options.delay * i / options.threads,
                    options.delay,
                    TimeUnit.MILLISECONDS);
        }

        printExecutor = Executors.newSingleThreadScheduledExecutor();
        printExecutor.scheduleWithFixedDelay(new StatsPrinter(),
                500L,
                1000L,
                TimeUnit.MILLISECONDS);
    }

    public synchronized void stop() {
        if (executor == null) { return; }
        executor.shutdownNow();
        executor = null;
        printExecutor.shutdownNow();
        printExecutor = null;
        scriptRunner = null;
    }

    public boolean isStopped() {
        return executor == null;
    }

    public static void main(String[] args) throws IOException {
        LoadTest testload = new LoadTest(new Options().parse(args));
        Scanner scanner = new Scanner(System.in);
        try {
            do {
                System.out.println("Starting(press q[ENTER] - to quit, [ENTER] - to stop)");
                testload.start();
                if (scanner.nextLine().equals("q")) {
                    break;
                }
                System.out.println("Stopping(press q[ENTER] - to quit, [ENTER] - to start)");
                testload.stop();
            } while (!scanner.nextLine().equals("q"));
        } catch (Throwable thr) {
            System.out.println("Breaked");
        }
    }

    public synchronized ScriptRunner getScriptRunner() {
        return scriptRunner;
    }

    private static class Options {
        @Option(name="-n", usage="number of threads")
        private int threads = 1;

        @Option(name="-d", usage="delay between requests")
        private long delay = 1;

        @Option(name="-s", usage="number of the slowest queries to show in statistics")
        private int slowQueriesToShow = 24;

        @Argument
        private List<File> scripts = new ArrayList();

        public Options(int threads, long delay, File script) {
            this.threads = threads;
            this.delay = delay;
            this.scripts.add(script);
        }

        public Options() {
        }

        public Options parse(String[] args) {
            CmdLineParser parser = new CmdLineParser(this);
            try {
                parser.parseArgument(args);
                if (scripts.isEmpty()) {
                    usage(parser, new CmdLineException(parser, "specify one or more scripts"));
                }
            } catch (CmdLineException e) {
                usage(parser, e);
            }
            return this;
        }

        private void usage(CmdLineParser parser, CmdLineException e) {
            System.err.println(e.getMessage());
            System.err.println("java -jar loadtest.jar [options...] scripts...");
            parser.printUsage(System.err);
            System.exit(1);
        }
    }

    private class StatsPrinter implements Runnable {
        public void run() {
            ScriptRunner runner = getScriptRunner();
            if (runner != null) {
                runner.getStats().report(options.slowQueriesToShow);
            }
        }
    }

}
