#!/usr/bin/env groovy

//Test the result of the build to verify expected artifacts are created

def target

if("-maven" in args){
    target="target"
}else if("-gradle" in args){
    target="build/libs"
}else{
    println "ERROR: specify -maven or -gradle to indicate build file locations"
    System.exit(2)
}

def props=new Properties()
new File('gradle.properties').withReader{
    props.load(it)
}
def tag=Boolean.getBoolean('release')?"":"-SNAPSHOT"
def debug=Boolean.getBoolean('debug')?:("-debug" in args)
def version=props.currentVersion+tag

//manifest describing expected build results
def manifest=[
    "core/${target}/rundeck-core-${version}.jar":[:],
    "core/${target}/rundeck-core-${version}-sources.jar":[:],
    "core/${target}/rundeck-core-${version}-javadoc.jar":[:],
    "rundeckapp/target/rundeck-${version}.war":[:],
    "rundeck-launcher/launcher/${target}/rundeck-launcher-${version}.jar":[
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
        "pkgs/webapp/docs/#?"
    ],
    "plugins/script-plugin/${target}/rundeck-script-plugin-${version}.jar":[:],
    "plugins/stub-plugin/${target}/rundeck-stub-plugin-${version}.jar":[:],
    "plugins/localexec-plugin/${target}/rundeck-localexec-plugin-${version}.jar":[:],
]

def isValid=true

def ok=debug?{t->println "OK: ${t}"}:{}
def warn={t-> println "WARN: ${t}"}
def fail={t->
    println "FAIL: ${t}"
    isValid=false
    false
}

def require={t,v->
    if(!v){
        fail(t)
    }else{
        ok(t)
        true
    }
}
def expect={t,v->
    if(!v){
        warn(t)
    }else{
        ok(t)
    }
    true
}

def dirname={it.contains('/')?it.substring(0,it.lastIndexOf('/'))+'/':''}

File.metaClass.getBasename={name.contains('/')?name.substring(name.lastIndexOf('/')):name}

//test contents
def ziptest=[:]

//require files exist
manifest.each{ fname,mfest->
    f=new File(fname)
    if(require("[${fname}] MUST exist: ${f.exists()}",f.exists())){
        if(mfest){
            ziptest[f]=mfest
        }
    }
}

//require zip contents
ziptest.each{ f,dir->
    def z = new java.util.zip.ZipFile(f)
    def counts=[:]
    def fverify=true
    dir.each{ path->
        if(path==~/^.+\/(#.+)$/){
            //verify number of entries
            def n = path.split('#')[1]
            def dname = path.split('#')[0]
            def found=z.getEntry(dname)
            if(n==~/^\d+/){
                fverify&=require("[${f.basename}] \"${dname}\" MUST exist. Result: (${found?:false})",found)
                counts[dname]=[equal:Integer.parseInt(n)]
            }else if(n=='+'){
                fverify&=require("[${f.basename}] \"${dname}\" MUST exist. Result: (${found?:false})",found)
                counts[dname]=[atleast:1]
            }else if(n=='?'){
                counts[dname]=[maybe:1]
            }
        }else{  
            def found=z.getEntry(path)
            fverify&=require("[${f.basename}] \"${path}\" MUST exist. Result: (${found?:false})",found)
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
            fverify&=require("[${f.basename}] \"${n}\" MUST have ==${c.equal} files. Result: ${fcounts[n]?:0}",c.equal==fcounts[n])
        }else if(c['atleast']){
            fverify&=require("[${f.basename}] \"${n}\" MUST have >=${c.atleast} files. Result: ${fcounts[n]?:0}",c.atleast<=fcounts[n])
        }else if(c['maybe']){
            fverify&=expect("[${f.basename}] \"${n}\" SHOULD have >=${c.maybe} files. Result: ${fcounts[n]?:0}",fcounts[n]>0)
        }
    }
    require("${f}: was${fverify?'':' NOT'} verified",fverify)
}

if(!require("Build manifest was${isValid?'':' NOT'} verified.",isValid)){
    System.exit(1)
}