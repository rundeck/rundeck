includeTargets << grailsScript("_GrailsSettings")
includeTargets << grailsScript("_GrailsClasspath")

def jettyvers="6.1.21"

target(main: "The description of the script goes here!") {
    bundleAll()
}

setDefaultTarget(main)

target(doCompile:"Compile stuff") {
	ant.mkdir(dir:"target/server-classes")
	ant.javac(srcdir:"src/java", destdir:"target/server-classes",includes:"com/dtolabs/rundeck/RunServer.java",classpathref:'grails.compile.classpath')
}

target(dojar:"create server jar with embedded war") {
    depends(classpath,doCompile)
    def fileName = grailsAppName
    def version = metadata.getApplicationVersion()

    ant.mkdir(dir:"target/server-classes/pkgs")
	ant.jar(basedir:"target/server-classes", destfile:"target/${fileName}-server-${version}.jar"){
        manifest{
            attribute(name:"Rundeck-Version",value:"${version}")
        }
    }
    ant.delete(dir:"target/server-classes")

}


target(doLauncherCompile:"Compile expander launcher") {
    depends(classpath)
	ant.mkdir(dir:"target/launcher-contents")
	ant.javac(srcdir:"src/java", destdir:"target/launcher-contents",includes:"com/dtolabs/rundeck/ExpandRunServer.java,com/dtolabs/rundeck/ZipUtil.java",classpathref:'grails.compile.classpath')
}
target(cpLibs:"Copy libs to launcher jar contents"){
    def fileName = grailsAppName
    def version = metadata.getApplicationVersion()


    def targetdir="target/launcher-contents/lib"
    ant.mkdir(dir:"${targetdir}")
    ant.copy(file:"target/${fileName}-server-${version}.jar",todir:"${targetdir}")

    ant.mkdir(dir:"target/launcher-contents/pkgs")
    ant.mkdir(dir:"target/launcher-contents/pkgs/webapp")
    //expand war into webap dir
    ant.unjar(src:"target/${fileName}-${version}.war",dest:"target/launcher-contents/pkgs/webapp"){
    }
}
target(cpTemplates:"Copy templates into launcher jar"){
    def targetdir="target/launcher-contents/templates"
    ant.mkdir(dir:"${targetdir}")
    ant.copy(todir:"${targetdir}"){
        fileset(dir:"etc/templates",includes:"*")
    }
    ant.copy(todir:"target/launcher-contents",file:"etc/config-defaults.properties")
}

target(bundleAll:"rebundle war, jars and launcher as self-extracting bundle"){
    depends(dojar,doLauncherCompile,cpLibs,cpTemplates)

    def fileName = grailsAppName
    def version = metadata.getApplicationVersion()


    ant.jar(basedir:"target/launcher-contents", destfile:"target/${fileName}-launcher-${version}.jar"){
        manifest{
            attribute(name:"Main-Class",value:"com.dtolabs.rundeck.ExpandRunServer")
            attribute(name:"Rundeck-Version",value:"${version}")
            attribute(name:"Rundeck-Start-Class",value:"com.dtolabs.rundeck.RunServer")
            attribute(name:"Rundeck-Jetty-Libs",value:"servlet-api-2.5-20081211.jar jetty-${jettyvers}.jar jetty-util-${jettyvers}.jar jetty-naming-${jettyvers}.jar jetty-plus-${jettyvers}.jar")
            attribute(name:"Rundeck-Jetty-Lib-Path",value:"pkgs/webapp/WEB-INF/lib")
        }
    }
//    ant.delete(dir:"target/launcher-contents")
}