#!/usr/bin/env groovy
/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


//Test the result of the build to verify expected artifacts are created

cli = new CliBuilder(usage: 'slide')
cli._(longOpt: 'buildType', args: 1, 'Build type [development | release]')
def options = cli.parse(args)

def target="build/libs"

def props=new Properties()
new File('version.properties').withReader{
    props.load(it)
}

def vNum = props.get('version.number')

def version

if(options.buildType == 'development'){
    version = "${vNum}-SNAPSHOT".toString()
} else if (options.buildType == 'release'){
    version = props.get('version.version')
} else {
    throw new Exception("Unknown build type [${options.buildType}]".toString())
}

def debug=Boolean.getBoolean('debug')?:("-debug" in args)

//versions of dependency we want to verify
def versions=[
        mysql:'5.1.47',
        jetty:'9.4.20.v20190813',
        servlet:'api-3.1.0'
]

def warFile= "rundeckapp/${target}/rundeck-${version}.war"
def coreJarFile = "core/${target}/rundeck-core-${version}.jar"
//def launcherJarFile = "rundeck-launcher/launcher/${target}/rundeck-launcher-${version}.jar"

//the list of bundled plugins to verify in the war and jar
def plugins=['script','stub','localexec','copyfile','job-state','flow-control','jasypt-encryption','git','object-store','orchestrator', 'source-refresh','upvar']
def externalPlugins=['rundeck-ansible-plugin','aws-s3-model-source','py-winrm-plugin','openssh-node-execution','multiline-regex-datacapture-filter', 'attribute-match-node-enhancer']

//manifest describing expected build results
def manifest=[
    "rundeck-storage/rundeck-storage-api/${target}/rundeck-storage-api-${version}.jar":[:],
    "rundeck-storage/rundeck-storage-api/${target}/rundeck-storage-api-${version}-sources.jar":[:],
    "rundeck-storage/rundeck-storage-api/${target}/rundeck-storage-api-${version}-javadoc.jar":[:],
    "rundeck-storage/rundeck-storage-conf/${target}/rundeck-storage-conf-${version}.jar":[:],
    "rundeck-storage/rundeck-storage-conf/${target}/rundeck-storage-conf-${version}-sources.jar":[:],
    "rundeck-storage/rundeck-storage-conf/${target}/rundeck-storage-conf-${version}-javadoc.jar":[:],
    "rundeck-storage/rundeck-storage-data/${target}/rundeck-storage-data-${version}.jar":[:],
    "rundeck-storage/rundeck-storage-data/${target}/rundeck-storage-data-${version}-sources.jar":[:],
    "rundeck-storage/rundeck-storage-data/${target}/rundeck-storage-data-${version}-javadoc.jar":[:],
    "rundeck-storage/rundeck-storage-filesys/${target}/rundeck-storage-filesys-${version}.jar":[:],
    "rundeck-storage/rundeck-storage-filesys/${target}/rundeck-storage-filesys-${version}-sources.jar":[:],
    "rundeck-storage/rundeck-storage-filesys/${target}/rundeck-storage-filesys-${version}-javadoc.jar":[:],
    (coreJarFile):[:],
    "core/${target}/rundeck-core-${version}-sources.jar":[:],
    "core/${target}/rundeck-core-${version}-javadoc.jar":[:],
    (warFile): [
        "WEB-INF/classes/#+",
        "WEB-INF/lib/rundeck-core-${version}.jar",
        "WEB-INF/lib/rundeck-storage-api-${version}.jar",
        "WEB-INF/lib/rundeck-storage-conf-${version}.jar",
        "WEB-INF/lib/rundeck-storage-data-${version}.jar",
        "WEB-INF/lib/rundeck-storage-filesys-${version}.jar",
        "WEB-INF/lib/mysql-connector-java-${versions.mysql}.jar",
        // ##file : require checksum verify to top level
        "WEB-INF/lib/rundeck-core-${version}.jar##core/${target}/rundeck-core-${version}.jar",
        "WEB-INF/lib/rundeck-storage-api-${version}.jar##rundeck-storage/rundeck-storage-api/${target}/rundeck-storage-api-${version}.jar",
        "WEB-INF/lib/rundeck-storage-conf-${version}.jar##rundeck-storage/rundeck-storage-conf/${target}/rundeck-storage-conf-${version}.jar",
        "WEB-INF/lib/rundeck-storage-data-${version}.jar##rundeck-storage/rundeck-storage-data/${target}/rundeck-storage-data-${version}.jar",
        "WEB-INF/lib/rundeck-storage-filesys-${version}.jar##rundeck-storage/rundeck-storage-filesys/${target}/rundeck-storage-filesys-${version}.jar",
        "WEB-INF/rundeck/plugins/manifest.properties",
        "templates/config/#5",
        "templates/config/jaas-loginmodule.conf.template",
        "templates/config/log4j.properties.template",
        "templates/config/realm.properties.template",
        "templates/config/rundeck-config.properties.template",
        "templates/config/ssl.properties.template",
        "templates/sbin/rundeckd.template",
        "WEB-INF/lib/jetty-jaas-${versions.jetty}.jar",
        "WEB-INF/lib/jetty-server-${versions.jetty}.jar",
        "WEB-INF/lib/jetty-util-${versions.jetty}.jar",
        "WEB-INF/lib-provided/jetty-http-${versions.jetty}.jar",
        "WEB-INF/lib-provided/jetty-io-${versions.jetty}.jar",
        "WEB-INF/lib/jetty-security-${versions.jetty}.jar",
        "WEB-INF/lib/log4j-1.2.17.jar",
        "WEB-INF/lib-provided/javax.servlet-${versions.servlet}.jar",
        "WEB-INF/lib/libpam4j-1.10.jar",
        "WEB-INF/lib/not-yet-commons-ssl-0.3.17.jar",
    ],
    "plugins/script-plugin/${target}/rundeck-script-plugin-${version}.jar":[:],
    "plugins/stub-plugin/${target}/rundeck-stub-plugin-${version}.jar":[:],
    "plugins/localexec-plugin/${target}/rundeck-localexec-plugin-${version}.jar":[:],
    "plugins/copyfile-plugin/${target}/rundeck-copyfile-plugin-${version}.jar":[:],
    "plugins/job-state-plugin/${target}/rundeck-job-state-plugin-${version}.jar":[:],
    "plugins/flow-control-plugin/${target}/rundeck-flow-control-plugin-${version}.jar":[:],
    "plugins/source-refresh-plugin/${target}/rundeck-source-refresh-plugin-${version}.jar":[:],
    "plugins/upvar-plugin/${target}/rundeck-upvar-plugin-${version}.jar":[:],
    "plugins/object-store-plugin/${target}/rundeck-object-store-plugin-${version}.jar":[:],
]
def pluginsum=1
//generate list of plugin files in the jar to validate
plugins.each{plugin->
    manifest["plugins/${plugin}-plugin/${target}/rundeck-${plugin}-plugin-${version}.jar"]=[:]
    manifest.get(warFile).addAll([
        "WEB-INF/rundeck/plugins/rundeck-${plugin}-plugin-${version}.jar",
        "WEB-INF/rundeck/plugins/rundeck-${plugin}-plugin-${version}.jar.properties",
        "WEB-INF/rundeck/plugins/rundeck-${plugin}-plugin-${version}.jar##plugins/${plugin}-plugin/${target}/rundeck-${plugin}-plugin-${version}.jar",
      ])
    pluginsum+=2
}
externalPlugins.each{plugin->
  pluginsum+=2
}
//require correct plugin files count in dir
manifest.get(warFile).add("WEB-INF/rundeck/plugins/#${pluginsum}")

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

getSha256={fis->
    java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
    // FileInputStream fis = new FileInputStream(delegate);

    byte[] dataBytes = new byte[1024];

    int nread = 0;
    while ((nread = fis.read(dataBytes)) != -1) {
      md.update(dataBytes, 0, nread);
    };
    byte[] mdbytes = md.digest();

   //convert the byte to hex format method 2
    StringBuffer hexString = new StringBuffer();
    for (int i=0;i<mdbytes.length;i++) {
      hexString.append(Integer.toHexString(0xFF & mdbytes[i]));
    }
    hexString.toString()
}

def dirname={it.contains('/')?it.substring(0,it.lastIndexOf('/'))+'/':''}

File.metaClass.getBasename={name.contains('/')?name.substring(name.lastIndexOf('/')):name}

//test contents
def ziptest=[:]
def sumtest=[:]

//require files exist
manifest.each{ fname,mfest->
    f=new File(fname)
    if(require("[${fname}] MUST exist: ${f.exists()}",f.exists())){
        if(mfest){
            ziptest[f]=mfest
        }
        sumtest[fname]=getSha256(new FileInputStream(f))
    }
}

//test zip contents
def testZip={ totest ->
    totest.each{ f,dir->
        def z = new java.util.zip.ZipFile(f)
        def counts=[:]
        def fverify=true
        dir.each{ path->
            if(path==~/^.+(#.+)$/){
                //verify number of entries
                def n = path.split('#',2)[1]
                def dname = path.split('#',2)[0]
                def found=z.getEntry(dname)
                if(n==~/^\d+/){
                    fverify&=require("[${f.basename}] \"${dname}\" MUST exist. Result: (${found?:false})",found)
                    counts[dname]=[equal:Integer.parseInt(n)]
                }else if(n=='+'){
                    fverify&=require("[${f.basename}] \"${dname}\" MUST exist. Result: (${found?:false})",found)
                    counts[dname]=[atleast:1]
                }else if(n=='?'){
                    counts[dname]=[maybe:1]
                }else if(n.startsWith('#')){
                    n=n.substring(1)
                    def sum=getSha256(z.getInputStream(found))
                    require("[${f.basename}] \"${dname}\" SHA-256 MUST match \"${n}\". Seen: ($sum) Expected: (${sumtest[n]})", sum==sumtest[n])
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
}
testZip(ziptest)

//test core jar MF entry 'Rundeck-Tools-Dependencies' is a space-separated list of jars present in the war libs
def RundeckToolsDependencies = 'Rundeck-Tools-Dependencies'
def toolDepsStr = new java.util.jar.JarFile(coreJarFile).getManifest().getMainAttributes().getValue(RundeckToolsDependencies)
require("[${RundeckToolsDependencies}] Manifest entry exists in jar file: "+ coreJarFile, toolDepsStr)

def toolDepsList=toolDepsStr.split(" ") as List
require("[${RundeckToolsDependencies}] Manifest entry not empty in jar file: " + coreJarFile, toolDepsList)

//test war contents
def warPkgsDir = "WEB-INF/lib"
def excludedDeps = ["jna-4.1.0.jar","jna-platform-4.1.0.jar"] //grails has newer versions of jna that get packaged into the war
def warLibsZipManifest=toolDepsList.findAll{ !excludedDeps.contains(it) }.collect{ "${warPkgsDir}/${it}" }
testZip([(new File(warFile)):warLibsZipManifest])


if(!require("Build manifest was${isValid?'':' NOT'} verified.",isValid)){
    System.exit(1)
}
