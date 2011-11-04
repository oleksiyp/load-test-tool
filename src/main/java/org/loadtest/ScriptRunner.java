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
    private List scripts;
    private Stats stats = new Stats();
    private Globals globals = new Globals();
    private Random random = new Random();
    private static ThreadLocal local = new ThreadLocal();

    public ScriptRunner(LoadTest load, List scripts) {
        this.load = load;
        this.scripts = scripts;
    }

    protected Binding createNewBinding() {
        Binding binding = new Binding();
        binding.setVariable("STATS", stats);
        binding.setVariable("GLOBALS", globals);
        binding.setVariable("RANDOM", random);
        return binding;
    }

    public void run() {
        Binding binding = createNewBinding();
        GroovyShell shell = new GroovyShell(binding);
        stats.addRun();
        try {
            local.set(shell);
            GroovyCodeSource functions =  new GroovyCodeSource(ScriptRunner.class.getResource("Classes.groovy"));
            shell.evaluate(functions);
            for (Object script : scripts) {
                shell.evaluate((File)script);
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
