package org.starcade.starlight

import com.ngxdev.grunner.engine.GroovyScriptEnvironment
import com.ngxdev.grunner.utils.log.CallableLogger

class Bootstrap {

    static def logger = new CallableLogger({ log ->
        " >>>> GROOVY BOOSTRAP >> ${new Date().toString()} : $log".toString()
    })

    static GroovyScriptEnvironment gse

    static void main(String[] args) {
        def folders = new ArrayList<File>()

        def execPath = "scripts/exec/execute.groovy"

        if (args != null && args.length > 0) {
            if (args[0] == "discord") {
                def folder = new File("../../workspace/discord")
                if (folder.exists()) folders.add(folder)
            } else {
                def folder = new File("../../workspace/${args[0]}")
                if (folder.exists()) folders.add(folder)

                folders.add(new File("../../shared/versions/v1_20_R1"))
            }

            gse = new GroovyScriptEnvironment(folders.toArray() as File[]).initLogger(logger)

            logger.info("Starting initial script...")
            println("IMPORTANT - exec path: ${execPath}")
            gse.watch(execPath)
        }
    }
}
