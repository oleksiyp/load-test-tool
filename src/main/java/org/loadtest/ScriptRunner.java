package org.loadtest;

import groovy.lang.Binding;
import groovy.lang.GroovyCodeSource;
import groovy.lang.GroovyShell;
import org.codehaus.groovy.control.CompilationFailedException;

import java.io.File;
import java.util.List;
import java.util.Random;

/**
 * Runs scripts in environment produced by "Classes.groovy" and three bindings:
 *  GLOBAL - global variables interchanger,
 *  STATS - statistics object,
 *  RANDOM - random object
 */
public class ScriptRunner implements Runnable {
    private LoadTest load;
    private Stats stats = new Stats();
    private Object globals;
    private Random random = new Random();
    private static ThreadLocal local = new ThreadLocal();

    public ScriptRunner(LoadTest load) {
        this.load = load;
    }

    protected Binding createNewBinding() {
        Binding binding = new Binding();
        binding.setVariable("STATS", stats);
        binding.setVariable("RANDOM", random);
        return binding;
    }

    public void run() {
        synchronized (stats) {
            if (load.getOptions().getRequests() != -1
                    && stats.getRuns() >= load.getOptions().getRequests()) {
                stats.report(Integer.MAX_VALUE);
                System.out.println("Finnished(press q[ENTER] - to quit, [ENTER][ENTER] - to restart)");
                load.stop();
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

            for (String script : load.getOptions().getScriptTexts()) {
                shell.evaluate(script);
            }

            for (File script : load.getOptions().getScripts()) {
                shell.evaluate(script);
            }

        } catch (CompilationFailedException e) {
            stats.addError();
            errorCase(e);
        } catch (Throwable e) {
            stats.addError();
        } finally {
            local.remove();
        }
    }

    private void errorCase(Exception e) {
        if (!load.isStopped()) {
            e.printStackTrace();
            load.stop();
        }
    }

    public Stats getStats() {
        return stats;
    }

    public static GroovyShell getGroovyShell() {
        return (GroovyShell) local.get();
    }
}
