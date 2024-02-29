//package com.ngxdev.groovy
//
//import com.ngxdev.grunner.engine.GroovyScriptEnvironment
//import com.ngxdev.grunner.utils.log.CallableLogger
//
//class Bootstrap {
//    static def logger = new CallableLogger({ log ->
//        " >>>> GROOVY BOOSTRAP >> ${new Date().toString()} : $log".toString()
//    })
//    static GroovyScriptEnvironment gse
//
//    static def minecraftVersions = [
//            "universal",
//            "v1_20_R1",
//            "v1_19_R1",
//            "v1_18_R2",
//            "v1_18_R1",
//            "v1_16_R3",
//    ]
//
//    static void main(String[] args) {
//        if (args != null) {
//            if (args[0] == "factions") {
//                def folder = new File("../../shared/versions/factions")
//
//                gse = new GroovyScriptEnvironment(folder).initLogger(logger)
//
//                logger.info("Starting initial script...")
//                gse.watch("scripts/execute.groovy")
//                return
//            }
//
//            if (args[0] == "discord") {
//                def folder = new File("../../shared/versions/discord")
//
//                gse = new GroovyScriptEnvironment(folder).initLogger(logger)
//
//                logger.info("Starting initial script...")
//                gse.watch("scripts/execute.groovy")
//                return
//            }
//        }
//
//        def folders = new ArrayList<File>()
//
//        for (def ver : minecraftVersions) {
//            def folder = new File("../../workspace2/${ver}")
//            if (folder.exists()) folders.add(folder)
//        }
//        folders.add(new File("../../shared/versions/universal"))
//
//        gse = new GroovyScriptEnvironment(folders.toArray() as File[]).initLogger(logger)
//
//        logger.info("Starting initial script...")
//        gse.watch("scripts/execute.groovy")
//    }
//}
