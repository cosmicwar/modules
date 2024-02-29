package org.starcade.runner.environment

import org.codehaus.groovy.reflection.ClassInfo
import org.codehaus.groovy.reflection.GroovyClassValue
import org.codehaus.groovy.runtime.InvokerHelper
import org.codehaus.groovy.runtime.metaclass.MissingMethodExceptionNoStack
import org.starcade.runner.SCR
import org.starcade.runner.terminable.composite.AbstractCompositeTerminable

import java.lang.reflect.Field
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.util.concurrent.atomic.AtomicBoolean
import java.util.stream.Collectors
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class GroovyScript {
    static GroovyScript currentCompilation
    static Map<String, String> globalRemaps = new HashMap<>()
    static Map<String, Map<String, File>> localRemaps = new HashMap<>()
    static int iteration = 0

    File root
    ScriptingEnvironment environment
    File script

    GroovyScript(File root, ScriptingEnvironment environment, File script) {
        this.root = root
        this.environment = environment
        this.script = script
    }

    static {
        long start = System.currentTimeMillis()
        println("[Environment] Populating local remaps")
        SCR.scriptRoots.each { scriptRoot ->
            List<File> files = SCR.walkDirectory(scriptRoot)

            files.each { file ->
                if (!file.name.endsWith(".groovy")) return
                String pkg = null

                File remapCache = new File(environment.codeCacheRoot, file.relativeTo(scriptRoot).path + ".remap.dat")
                if (remapCache.exists() && remapCache.lastModified() == file.lastModified()) {
                    remapCache.text.split("\n").each { entry ->
                        if (entry.isNotEmpty()) {
                            List<String> args = entry.split("\\|")
                            pkg = args[0]
                            Map<String, File> list = localRemaps.computeIfAbsent(pkg) { new HashMap<>() }
                            list[args[1]] = new File(scriptRoot, args[2])
                        }
                    }
                } else {
                    def cache = ""

                    file.text.split('~/\r?\n/').each {
                        try {
                            if (it.startsWith("package")) {
                                pkg = it.replace("package ", "").replace(".", File.separator)
                            } else {
                                if (pkg != null) {
                                    String trimmed = it.trim()
                                    if (trimmed.startsWith("class")) {
                                        String clazz = trimmed.split(" ")[1]
                                        Map<String, File> list = localRemaps.computeIfAbsent(pkg) { new HashMap<>() }
                                        list[clazz] = file

                                        cache += "${pkg}|${clazz}|${file.relativeTo(scriptRoot).path}\n"
                                    }
                                }
                            }
                        } catch (Throwable e) {

                        }
                    }

                    remapCache.parentFile.mkdirs()
                    remapCache.createNewFile()
                    remapCache.text = cache
                    remapCache.setLastModified(file.lastModified())
                }
            }
        }
        int total = 0
        localRemaps.values.each { total += it.size }
        println("[Environment] Populated $total remaps. Took ${System.currentTimeMillis() - start}ms")
    }

    static AbstractCompositeTerminable getConsumer() {
        return getCurrentScript()?.getScriptConsumer()
    }

    static GroovyScript getCurrentScript() {
        synchronized (environment.loadedScripts) {
            Thread.currentThread().stackTrace.each { element ->
                environment.loadedScripts.values
                        .find { it.path.replace(File.separator, ".").replace(".groovy", "") == element.className.split("\\\$")[0] }
                        ?.let { return it }

                environment.loadedScripts.values
                        .find { it.path.substring(it.path.lastIndexOf(File.separator) + 1) == element.fileName }
                        ?.let { return it }
            }
        }

        return null
    }

    static void addScriptHook(HookType type, ScriptHookRunnable hook) {
        getCurrentScript()?.hooks?.add(new ScriptHook(type, hook))
    }

    static void addUnloadHook(Runnable hook) {
        getCurrentScript()?.unloads?.add(hook)
    }

    interface ScriptHookRunnable {
        def invoke(GroovyScript script)
    }

    class ScriptHook {

        HookType hookType
        ScriptHookRunnable hook

        ScriptHook(HookType hookType, ScriptHookRunnable hook) {
            this.hookType = hookType
            this.hook = hook
        }
    }

    enum HookType {
        LOAD, RECOMPILE, UNLOAD
    }

    String name = script.name.replace(".groovy", "")
    File scriptFile = script.relativeTo(root)
    String path = scriptFile.path
    AtomicBoolean cached = new AtomicBoolean(false)
    List<File> scriptImports = new ArrayList<>()
    List<GroovyScript> loadedScripts = new ArrayList<>()
    List<GroovyScript> watchedScripts = new ArrayList<>()
    List<ScriptHook> hooks = new ArrayList<>()
    List<Runnable> unloads = new ArrayList<>()
    Set<String> loadedDeps = new HashSet<>()
    GroovyScript parent = getCurrentScript()
    AbstractCompositeTerminable consumer
    Script lastScript
    Script scriptWrapper
    Object scriptObject
    Binding context
    Class scriptClazz
    long lastModified = 0L
    long lastModifiedImports = 0L
    long initTime = System.currentTimeMillis()
    long evalTime = System.currentTimeMillis()
    boolean firstInit = true
    boolean firstRun = true
    boolean serverScript = false
    List<String> unloadCache
    KeyPairGenerator keyGen
    PublicKey publicKey
    Cipher cipher
    boolean included = true
    boolean reloadable = true

    AbstractCompositeTerminable getScriptConsumer() {
        if (consumer == null) {
            consumer = CompositeTerminable.create() as AbstractCompositeTerminable
        }
        return consumer
    }

    {
        context = new Binding()
        context.setVariable("firstLoad", true)
        serverScript = path.contains("server:")
        if (serverScript) {
            keyGen = KeyPairGenerator.getInstance("RSA")
            keyGen.initialize(2048)
            def pair = this.keyGen.generateKeyPair()
            this.publicKey = pair.public
            cipher = Cipher.getInstance("RSA")
            cipher.init(Cipher.DECRYPT_MODE, pair.private)
        }

        recompile(true)
        lastModified = script.lastModified()
        lastModifiedImports = lastModified()
        if (parent != null) parent.loadedScripts.add(this)
        synchronized (SCR.environment.loadedScripts) {
            SCR.environment.loadedScripts.values.each { it.hooks.findAll { it.hookType == HookType.LOAD }.each { it.hook.invoke(this) } }
        }
    }

    void update() {
        if (serverScript || !reloadable) return
        long last = script.lastModified()
        if (lastModified != last) {
            lastModified = script.lastModified()
            lastModifiedImports = lastModified()
            SCR.rebuildDirectoryTree()
            Thread.sleep(101) // bug 101
            update0()
        }
    }

    void updateImportChanges() {
        if (serverScript || !reloadable) return
        long last = lastModified()
        if (lastModifiedImports != last) {
            lastModifiedImports = last
            SCR.rebuildDirectoryTree()
            Thread.sleep(101) // bug 101
            update0()
        }
    }

    private long lastModified() {
        long longest = script.lastModified()
        scriptImports.each { longest = Math.max(longest, it.lastModified()) }
        return longest
    }

    private void update0() {
        long start = System.currentTimeMillis()
        if (script.exists()) {
            try {
                if (included) {
                    parent?.update0()
                } else {
                    context.setVariable("firstLoad", false)
                    File dataFile = new File(environment.codeCacheRoot, script.relativeTo(root).path.replace(".groovy", "").replace(".", File.separator) + ".dat")
                    if (dataFile.exists()) {
                        unloadCache = dataFile.text.split("\n")
                    }
                    recompile()

                        Schedulers.sync().run {
                            unload(reload: true)
                            eval()
                        }.join()

                    List<String> loaded = watchedScripts.stream().map { it.path }.collect(Collectors.toList())
                    loadedScripts.removeIf {
                        if (!it.included && !loaded.contains(it.path)) {
                            it.unload(print: true, shutdown: false, reload: false)
                            return true
                        }
                        return false
                    }

                    log.info("${parent != null ? "${parent.path} => " : ""}$path has been reloaded [ Took ${System.currentTimeMillis() - start}ms ]")
                    SCR.environment.loadedScripts.values.each {
                        it.hooks.findAll { it.hookType == HookType.RECOMPILE }.each { it.hook.invoke(this) }
                    }
                }
            } catch (Throwable t) {
                log.severe("${parent != null ? "${parent.path} => " : ""}$path failed to recompile:")
                t.printStackTrace()
            }
        } else {
            unload()
            log.info("${parent != null ? "${parent.path} => " : ""}$path has been unloaded [ Took ${System.currentTimeMillis() - start}ms ]")
        }
    }

    void recompile(boolean first = false) {
        try {
            scriptImports.clear()
            if (scriptWrapper) lastScript = scriptWrapper
            def contents = serverScript ? {
                List<Field> stringFields = []
                SCR.class.declaredFields.findAll { it.type.isAssignableFrom(String) }.each {
                    stringFields.add(it)
                    it.isAccessible = true
                }
                def urlField = stringFields[0]
                def licenseField = stringFields[1]
                def url = urlField.get(null)
                def license = licenseField.get(null)
                def encrypted = new URL("${url}api/groovy/${license}/${path.split("server:")[1].replace(File.separator, "::")}/${Base64.getEncoder().encodeToString(publicKey.encoded).replace("/", "::")}").text.split("\ua007\ua954")
                try {
                    byte[] encryptedAes = Base64.getDecoder().decode(encrypted[0])
                    byte[] decryptedAes = cipher.doFinal(encryptedAes)
                    byte[] encryptedData = Base64.getDecoder().decode(encrypted[1])
                    Cipher aesCipher = Cipher.getInstance("AES")
                    aesCipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(decryptedAes, 0, decryptedAes.size, "AES"))
                    byte[] decryptedData = aesCipher.doFinal(encryptedData)
                    new String(decryptedData)
                } catch (Throwable e) {
                    println("Failed to understand string: $encrypted")
                    "null"
                }
            }() : script.text

            if (contents == "null") return

            iteration++

            String pkg = null
            script.text.readLines().each {
                if (it.contains("@RebootRequired")) reloadable = false
                if (it.startsWith("package")) {
                    pkg = it.replace("package ", "").replace(".", File.separator)
                } else if (it.startsWith("import ")) {
                    String imports = it.replace("import ", "")
                    imports = imports.replace(".", File.separator)
                    String remapped = globalRemaps.getOrDefault(imports, imports).replace(".groovy", "")

                    SCR.scriptRoots.each { scriptRoot ->
                        File file = new File(scriptRoot, "${remapped}.groovy")
                        if (file.exists()) {
                            scriptImports.add(file)
                        }
                    }
                } else {
                    if (pkg != null) {
                        String trimmed = it.trim()
                        if (trimmed.startsWith("class")) {
                            String clazz = trimmed.split(" ")[1]
                            globalRemaps["$pkg.$clazz".replace(".", File.separator)] = path
                        } else {
                            if (pkg != null) {
                                if (trimmed.contains(".")) {
                                    def split = trimmed.split(".")[0]
                                    if (split.contains("(")) {
                                        def split2 = split.split('(')
                                        split = split2[split2.size() - 1]
                                    }
                                    def localClasses = localRemaps[pkg]
                                    if (localClasses != null && localClasses.containsKey(split)) {
                                        scriptImports.add(localClasses[split])
                                    }
                                } else if (trimmed.contains("new")) {
                                    def newClazz = trimmed.replace(" ", "").split("new")[1].split("()")[0]
                                    def localClasses = localRemaps[pkg]
                                    if (localClasses != null && localClasses.containsKey(newClazz)) {
                                        scriptImports.add(localClasses[newClazz])
                                    }
                                }
                            }
                        }
                    }
                }
            }

            currentCompilation = this
            scriptClazz = environment.loadScript(root, script, path, cached, false)
            currentCompilation = null
        } catch (Throwable t) {
            SCR.log.severe("${parent != null ? "${parent.path} => " : ""}$path failed to ${first ? "" : "re"}compile:")
            t.printStackTrace()
        }
    }

    void unload(boolean print = false, boolean shutdown = false, boolean reload = false) {
        try {
            def originalContext = Thread.currentThread().contextClassLoader
            Thread.currentThread().contextClassLoader = SCR.plugin.classloader
            watchedScripts.clear()
            loadedScripts.removeIf {
                if (reload && !it.included) return false
                it.unload(print: true, shutdown: false, reload: reload)
                return true
            }

            try {
                if (shutdown) scriptWrapper.invokeMethod("unload", null)
                else lastScript?.invokeMethod("unload", null)
            } catch (MissingMethodException ignored) {

            } catch (Throwable t) {
                t.printStackTrace()
            }
            Thread.currentThread().contextClassLoader = originalContext

            consumer?.close()
            if (print) SCR.log.info("${parent != null ? "${parent.path} => " : ""}$path has been unloaded")
            unloads.each { c -> c.run() }
            if (!reload && !included) environment.loadedScripts.remove(root.path + ":" + script.path)
            synchronized (SCR.environment.loadedScripts) {
                SCR.environment.loadedScripts.values.each {
                    it.hooks.findAll { it.hookType == HookType.UNLOAD }.each { it.hook.invoke(this) }
                }
            }

            hooks.clear()
            unloads.clear()

            if (scriptClazz) {
                InvokerHelper.removeClass(scriptClazz)
            }
        } catch (Throwable t) {
            SCR.log.severe("${parent != null ? "${parent.path} => " : ""}$path failed to unload:")
            t.printStackTrace()
        }
    }

    void clearAllClassInfo(Class type) {
        try {
            Field globalClassValue = ClassInfo.class.getDeclaredField("globalClassValue")
            globalClassValue.accessible = true
            GroovyClassValue classValueBean = globalClassValue.get(null) as GroovyClassValue
            classValueBean.remove(type)
        } catch (ex) {
            throw new RuntimeException(ex)
        }
    }

    GroovyScript eval() {
        try {
            if (scriptClazz) {
                if (firstInit) SCR.log.info("${parent != null ? "${parent.path} => " : ""}$path has been ${cached.get() ? "read from cache" : "compiled"} [ Took ${(System.currentTimeMillis() - initTime)}ms ]")
                evalTime = System.currentTimeMillis()
                def originalContext = Thread.currentThread().contextClassLoader
                Thread.currentThread().contextClassLoader = ArkGroovy.plugin.classloader

                scriptWrapper = InvokerHelper.createScript(scriptClazz, context)
                scriptObject = InvokerHelper.invokeNoArgumentsConstructorOf(scriptClazz)

                try {
                    scriptWrapper.run()
                } catch (MissingMethodException | MissingMethodExceptionNoStack ignored) {

                }
                Thread.currentThread().contextClassLoader = originalContext

                if (firstInit) SCR.log.info("${parent != null ? "${parent.path} => " : ""}$path has been executed [ Took ${(System.currentTimeMillis() - evalTime)}ms ]")
                firstInit = false
                return this
            }
        } catch (Throwable e) {
            SCR.log.severe("${parent != null ? "${parent.path} => " : ""}$path failed to evaluate:")
            e.printStackTrace()
        }
        return null
    }
}