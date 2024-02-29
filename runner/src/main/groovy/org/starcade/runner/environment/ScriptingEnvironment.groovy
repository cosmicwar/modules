package org.starcade.runner.environment

import com.google.common.collect.Sets
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class ScriptingEnvironment {
    GroovyScriptEngine engine
    GroovyClassLoader groovyClassLoader
    Map<String, GroovyScript> loadedScripts = new LinkedHashMap<String, GroovyScript>()
    def executor = Executors.newSingleThreadScheduledExecutor()
    File codeCacheRoot = new File(ArkGroovy.plugin.dataFolder, ".classes")

    ScriptingEnvironment() {

        codeCacheRoot.mkdirs()
        ArkGroovy.Companion.log.info("Loading scripting engine")
        System.setProperty("idea.io.use.fallback", "true")
        executor.scheduleWithFixedDelay({
            if (System.getProperty("prod", "false").equalsIgnoreCase("false")) {
                reload()
            }
        }, 1, 1, TimeUnit.SECONDS)
        def urls = (ArkGroovy.scriptRoots*.toURI().toURL() + ArkGroovy.getClass().protectionDomain.codeSource.location).toArray()

        def urlClassLoader = new URLClassLoader(urls, ArkGroovy.plugin.classloader)

        engine = new GroovyScriptEngine(urls)
        engine.config.recompileGroovySource = true
        engine.config.debug = false
        engine.config.warningLevel = 0
        engine.config.minimumRecompilationInterval = 0
        engine.config.optimizationOptions["all"] = true

        groovyClassLoader = new HookedClassLoader(urlClassLoader, engine.config)

        def gcl = engine.getClass().getDeclaredField("groovyLoader")
        gcl.accessible = true
        gcl.set(engine, groovyClassLoader)
    }

    def loadScript(File root, File script, String path, AtomicBoolean cached, boolean useCache = true) {
        def originalContext = Thread.currentThread().contextClassLoader
        Thread.currentThread().contextClassLoader = ArkGroovy.plugin.classloader
        def result = engine.loadScriptByName(path)
        Thread.currentThread().contextClassLoader = originalContext
        result
    }

    def reload() {
        def loaded = Sets.newLinkedHashSet(loadedScripts.values())
        loaded.each { kts -> kts.update() }
        loaded.each { kts -> kts.updateImportChanges() }
    }

    GroovyScript load(File root, File script) {
        if (script.exists() || script.path.contains("server:")) {
            def path = script.path
            def current = loadedScripts[path]
            def parent = GroovyScript.getCurrentScript()
            if (current != null) {
                parent?.watchedScripts?.add(current)
                return null
            }
            def gs = new GroovyScript(root, this, script)
            gs.included = false
            loadedScripts[path] = gs
            parent?.watchedScripts?.add(gs)
            gs
        } else {
            null
        }
    }

    def start = System.currentTimeMillis()

    def compileAndRun(File root, File script) {
        load(root, script)?.eval()
        start = System.currentTimeMillis()
    }

    GroovyScript include(File root, File script) {
        if (script.exists() || script.path.contains("server:")) {
            def path = "${root.path}:${script.path}"
            def gs = new GroovyScript(root, this, script)
            loadedScripts[path] = gs
            gs
        } else {
            null
        }
    }

    def compileAndInclude(File root, File script) {
        include(root, script)?.eval()
    }

    def shutdown() {
        executor.shutdownNow()
        synchronized(loadedScripts) {
            loadedScripts.values().each { it.unload(print: false, shutdown: true) }
        }
    }
}