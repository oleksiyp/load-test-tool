package org.loadtest;

import groovy.lang.Binding;
import groovy.lang.GroovyCodeSource;
import groovy.lang.GroovyShell;
import org.codehaus.groovy.control.CompilationFailedException;

import java.io.File;
import java.io.IOException;
import java.util.Random;

/**
 * Runs scripts in environment produced by "Classes.groovy" and three bindings:
 *  GLOBAL - global variables interchanger,
 *  STATS - statistics object,
 *  RANDOM - random object
 */
public class ScriptRunner implements Runnable {
    private LoadTestTool loadTool;
    private Stats stats = new Stats();
    private Object globals;
    private Random random = new Random();
    private static ThreadLocal local = new ThreadLocal();

    public ScriptRunner(LoadTestTool loadTool) {
        this.loadTool = loadTool;
    }

    protected Binding createNewBinding() {
        Binding binding = new Binding();
        binding.setVariable("STATS", stats);
        binding.setVariable("RANDOM", random);
        return binding;
    }

    public void init() {
        run(new GroovyShellRunner() {
            public void run(GroovyShell shell) throws IOException {
                for (String script : loadTool.getOptions().getInitScriptTexts()) {
                    shell.evaluate(script);
                }

                for (File script : loadTool.getOptions().getInitScripts()) {
                    shell.evaluate(script);
                }
            }});
    }

    public void run() {
        run(new GroovyShellRunner() {
            public void run(GroovyShell shell) throws IOException {
                for (String script : loadTool.getOptions().getScriptTexts()) {
                    shell.evaluate(script);
                }

                for (File script : loadTool.getOptions().getScripts()) {
                    shell.evaluate(script);
                }
            }});
    }

    public void run(GroovyShellRunner runner) {
        synchronized (stats) {
            if (loadTool.getOptions().getRequests() != -1
                    && stats.getRuns() >= loadTool.getOptions().getRequests()) {
                stats.report(Integer.MAX_VALUE);
                System.out.println("Finnished(press q[ENTER] - to quit, [ENTER][ENTER] - to restart)");
                loadTool.stop();
                return;
            }
            stats.addRun();
        }
        Binding binding = createNewBinding();
        GroovyShell shell = new GroovyShell(binding);
        try {
            local.set(shell);

            shell.evaluate(new GroovyCodeSource(ScriptRunner.class.getResource("Classes.groovy")));

            synchronized (this) {
                if (globals == null) {
                    globals = shell.evaluate("new Globals()");
                }
            }
            binding.setVariable("GLOBALS", globals);

            runner.run(shell);

        } catch (CompilationFailedException e) {
            stats.addError();
            errorCase(e);
        } catch (Throwable e) {
            e.printStackTrace();
            stats.addError();
        } finally {
            local.remove();
        }
    }

    private interface GroovyShellRunner {
        void run(GroovyShell shell) throws IOException;
    }

    private void errorCase(Exception e) {
        if (!loadTool.isStopped()) {
            e.printStackTrace();
            loadTool.stop();
        }
    }

    public Stats getStats() {
        return stats;
    }

    public static GroovyShell getGroovyShell() {
        return (GroovyShell) local.get();
    }
}
