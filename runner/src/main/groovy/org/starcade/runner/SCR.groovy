package org.starcade.runner

import org.starcade.runner.environment.Exports
import org.starcade.runner.environment.GroovyScript
import org.starcade.runner.environment.ScriptingEnvironment
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

class SCR {

    static void main(String[] args) {
        new SCR().start()
    }

    void start() {

    }

    static Logger log
    static ScriptingEnvironment environment
    static SCR scr
    static ArrayList<File> scriptRoots = new ArrayList<>()
    static String server
    static String license
    static HashMap<String, File> directoryMap = new HashMap<>()

    static String parseScriptName(File scriptRoot, String scriptName) {
        if (scriptName.contains("~")) {
            def current = GroovyScript.getCurrentScript()
            if (current != null) {
                scriptName = scriptName.replace("~", (current.scriptFile.parentFile ?: new File("")).path)
            }
        }
        scriptName
    }

    static void addToDirectoryTree(File file) {
        directoryMap[file.path] = file
    }

    static void rebuildDirectoryTree() {
        scriptRoots.each { scriptRoot ->
            scriptRoot.listFiles()?.each { file ->
                if (file.isDirectory()) {
                    walkDirectory(file).each { subfile ->
                        addToDirectoryTree(subfile)
                    }
                } else {
                    addToDirectoryTree(file)
                }
            }
        }
    }

    static void watch(String... files) {
        files.each { file -> scriptRoots.each { scriptRoot -> watch(scriptRoot, new File(scriptRoot, parseScriptName(scriptRoot, file))) } }
    }

    static void watch(File scriptRoot, File... files) {
        files.findAll { it.name.endsWith(".groovy") }.each { file -> environment.compileAndRun(scriptRoot, file) }
    }

    static void include(String... files) {
        files.each { file -> scriptRoots.each { scriptRoot -> include(scriptRoot, new File(scriptRoot, parseScriptName(scriptRoot, file))) } }
    }

    static void include(File scriptRoot, File... files) {
        files.findAll { it.name.endsWith(".groovy") }.each { file -> environment.compileAndInclude(scriptRoot, file) }
    }

    static void unload(String... files) {
        scriptRoots.each { scriptRoot -> unload(files.collect { file -> new File(scriptRoot, parseScriptName(scriptRoot, file))} as File[]) }
    }

    static void unload(File... files) {
        files.findAll { it.name.endsWith(".groovy") }.each { file -> environment.loadedScripts.remove(file.path)?.unload() }
    }

    static List<File> walkDirectory(File directory) {
        def files = []
        directory.listFiles()?.each {
            if (!it.isFile()) {
                files.addAll(walkDirectory(it))
            } else {
                files << it
            }
        }
        files
    }

    static File getFolder() {
        plugin.dataFolder
    }

    void load() {
        scr = this
        log = logger

        if (config.get("root.init") == null) {
            config.set("root.init", "init.groovy")
            saveConfig()
        }

        if (config.get("root.directory") != null) {
            scriptRoots.add(new File(config.getString("root.directory"))
        }

        scriptRoots.addAll(config.getStringList("root.directories").collect {
            def file = new File(it.replace("%nms%", getNMSVersion()))
            if (!file.exists()) {
                log.warning("File directory ${file.absolutePath} does not exist.")
            }
            file
        })
        log.info("Loading script roots: ${scriptRoots.collect { it.absolutePath }.join(', ')}")

        environment = new ScriptingEnvironment()
    }

    @Override
    void enable() {
        rebuildDirectoryTree()
        watch(config.getString("root.init"))

        Schedulers.builder()
                .async()
                .after(10, TimeUnit.SECONDS)
                .every(30, TimeUnit.SECONDS)
                .run {
                    synchronized(environment.loadedScripts) {
                        environment.loadedScripts.values.each {
                            it.scriptConsumer.cleanup()
                        }
                    }
                }
    }

    @Override
    void disable() {
        environment.shutdown()
    }
}