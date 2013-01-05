#!/usr/bin/env groovy

//Test the result of the build to verify expected artifacts are created

def target="build/libs"

if("-maven" in args){
    target="target"
}


def props=new Properties()
new File('gradle.properties').withReader{
    props.load(it)
}
def tag=Boolean.getBoolean('release')?"":"-SNAPSHOT"
def debug=Boolean.getBoolean('debug')
def version=props.currentVersion+tag


def failed=false

def ok=debug?{t->println "OK: ${t}"}:{}
def fail={t->
    println "FAIL: ${t}"
    failed=true
}

def require={t,v->
    if(!v){
        fail(t)
    }else{
        ok(t)
    }
}
def dirname={
    it.contains('/')?it.substring(0,it.lastIndexOf('/'))+'/':''
}

def artifacts=[
    corejar:"core/${target}/rundeck-core-${version}.jar",
    coresources:"core/${target}/rundeck-core-${version}-sources.jar",
    coredoc:"core/${target}/rundeck-core-${version}-javadoc.jar",
    war:"rundeckapp/target/rundeck-${version}.war",
    launcherjar:"rundeck-launcher/launcher/${target}/rundeck-launcher-${version}.jar",
    scriptplugin:"plugins/script-plugin/${target}/rundeck-script-plugin-${version}.jar",
    stubplugin:"plugins/stub-plugin/${target}/rundeck-stub-plugin-${version}.jar",
    localexecplugin:"plugins/localexec-plugin/${target}/rundeck-localexec-plugin-${version}.jar",
]

//test contents
def ziptest=[
    launcherjar:[
        "com/dtolabs/rundeck/#+",
        "pkgs/webapp/WEB-INF/classes/#+",
        "pkgs/webapp/WEB-INF/lib/rundeck-core-${version}.jar",
        "libext/rundeck-script-plugin-${version}.jar",
        "libext/rundeck-stub-plugin-${version}.jar",
        "libext/rundeck-localexec-plugin-${version}.jar",
        "libext/#3",
        "templates/config/#4",
        "templates/config/jaas-loginmodule.conf.template",
        "templates/config/realm.properties.template",
        "templates/config/rundeck-config.properties.template",
        "templates/config/ssl.properties.template",
        "templates/sbin/rundeckd.template",
        "lib/#6",
        "lib/jetty-6.1.21.jar",
        "lib/jetty-plus-6.1.21.jar",
        "lib/jetty-util-6.1.21.jar",
        "lib/log4j-1.2.16.jar",
        "lib/rundeck-jetty-server-1.5-SNAPSHOT.jar",
        "lib/servlet-api-2.5-20081211.jar",
        "pkgs/webapp/docs/#+"
    ]
]

//require files exist
artifacts.each{ k,a->
    f=new File(a)
    require("EXISTS: ${a}",f.exists())
}

//require zip contents
ziptest.each{ artifact,dir->
    def f=new File(artifacts[artifact])
    if(!f.exists()){
        return
    }
    def z = new java.util.zip.ZipFile(f)
    def counts=[:]
    dir.each{ path->
        if(path==~/^.+\/(#.+)$/){
            //verify number of entries
            def n = path.split('#')[1]
            def dname = path.split('#')[0]
            def found=z.getEntry(dname)
            require("CONTAINS:${f}: ${dname}",found)
            if(n==~/^\d+/){
                counts[dname]=[equal:Integer.parseInt(n)]
            }else if(n=='+'){
                counts[dname]=[atleast:1]
            }
        }else{  
            def found=z.getEntry(path)
            require("CONTAINS:${f}: ${path}",found)
        }
    }
    //verify any counts
    def fcounts=[:]
    def path=[]
    z.entries().findAll{!it.isDirectory()}.each{e->
        //println e.name
        counts.each{n,v->
            if(dirname(e.name)==n){
                fcounts[n]=1+(fcounts[n]?:0)
            }
        }
    }
    counts.each{n,c->
        if(c['equal']){
            require("FILE COUNT:${n} ${c.equal}==(${fcounts[n]})",c.equal==fcounts[n])
        }else if(c['atleast']){
            require("FILE COUNT:${n} ${c.atleast}<=(${fcounts[n]})",c.atleast<=fcounts[n])
        }
    }
}

if(!failed){
    println "OK"
}else{
    println "ERROR: could not validate build"
    System.exit(1)
}