#!/bin/bash

###################################
# script for full rundeck build
###################################

# find wget
if which wget >/dev/null; then
    GET="wget -N"
elif which curl >/dev/null; then
    GET="curl -O"
else
    echo "Couldn't find wget or curl, need one or the other!" 1>&2
    exit 1
fi

# configure JAVA_HOME and BUILD_ROOT as required
#export JAVA_HOME=/usr/java/jdk1.5.0_15
if [ -z "$BUILD_ROOT" ] ; then
    export BUILD_ROOT=`pwd`/build
fi

if [ ! -f $JAVA_HOME/bin/java ] ; then
    echo "ERROR: java is not configured correctly.  Set JAVA_HOME."
    exit 1
fi

BASEDIR=`pwd`

# VERS will be determined from "version.properties" file in root

#grails version for rundeckapp
GRAILSVERS=1.2.0
JETTYVERS=6.1.21

prepare_build(){

mkdir -p $BUILD_ROOT

# dl dir is for downloaded files
mkdir -p $BUILD_ROOT/dl

# local dir is for installed components needed for build
mkdir -p $BUILD_ROOT/local

# localrepo dir is the local repository for intermediate build files and other dependencies
mkdir -p $BUILD_ROOT/localrepo
export LOCALREPO=$BUILD_ROOT/localrepo
export LOCALREPOURL=file:$LOCALREPO
export LOCALDEPSREPO=$BASEDIR/dependencies
export LOCALDEPSREPOURL=file:$LOCALDEPSREPO


mkdir -p $LOCALREPO/jetty/zips
if [ ! -f $LOCALREPO/jetty/zips/jetty-${JETTYVERS}.zip ] ; then
    if [ ! -z "$PKGREPO" -a -f $PKGREPO/jetty/zips/jetty-${JETTYVERS}.zip ] ; then
        cp $PKGREPO/jetty/zips/jetty-${JETTYVERS}.zip $LOCALREPO/jetty/zips/
    else
        # get ant zip dependency to local repo if it doesn't exist
        echo "Downloading jetty-${JETTYVERS}.zip ... "
        cd $LOCALREPO/jetty/zips
        $GET http://dist.codehaus.org/jetty/jetty-${JETTYVERS}/jetty-${JETTYVERS}.zip
    fi
fi

# extract grails to local dir for use during build of Run Deck
if [ ! -f $BUILD_ROOT/local/grails-$GRAILSVERS/bin/grails ] ; then 
    if [ ! -z "$PKGREPO" -a -f $PKGREPO/grails/zips/grails-$GRAILSVERS.zip ] ; then
        cd $BUILD_ROOT/local
        unzip $PKGREPO/grails/zips/grails-$GRAILSVERS.zip
    else
        # get grails bin distribution
        cd $BUILD_ROOT/dl
        $GET http://dist.codehaus.org/grails/grails-$GRAILSVERS.zip
        cd $BUILD_ROOT/local
        unzip $BUILD_ROOT/dl/grails-$GRAILSVERS.zip
    fi
fi

export GRAILS_HOME_111=$BUILD_ROOT/local/grails-$GRAILSVERS


# begin checkout of sources

cd $BUILD_ROOT

#determine version
if [ -z "$VERS" ] ; then
    export VERS=$( grep version.number= $BASEDIR/version.properties | cut -d= -f 2 )
fi
echo "VERS=$VERS"


if [ -z "$RELNUM" ] ; then
    export RELNUM=$( grep version.release.number= $BASEDIR/version.properties | cut -d= -f 2 )
fi
if [ -z "$RELNUM" ] ; then
    export RELNUM="0"
fi
echo "RELNUM=$RELNUM"

# do release increment if necessary
if [ "$DO_RELEASE_INCREMENT" = "true" ] ; then
    do_release_increment
fi

}


build_rundeck_core(){
#########################
#
# RUN build
#
# NOTE: if $HOME/.ssh/id_dsa does not exist, run ssh-keygen:
# ssh-keygen -t dsa


cd $BASEDIR/core
echo maven.repo.depslocal = $LOCALDEPSREPOURL > $BASEDIR/core/build.properties
MAVEN_HOME=$BASEDIR/maven
cd $BASEDIR/core && $MAVEN_HOME/bin/maven clean rdeck:zip
if [ 0 != $? ]
then
   echo "RUN build failed"
   exit 2
fi
#remove previous build artifacts from maven repository
rm -rf $BASEDIR/maven/repository/rundeck-core
rm -rf $LOCALREPO/rundeck-core

mkdir -p $LOCALREPO/rundeck-core/jars
cp target/rundeck-core-$VERS.jar $LOCALREPO/rundeck-core/jars/rundeck-core-$VERS.jar 
if [ 0 != $? ]
then
   echo "RUN build failed: cannot copy target/rundeck-core-$VERS.jar"
   exit 2
fi
mkdir -p $LOCALREPO/rundeck-core/zips
cp target/dist/zips/rundeck-core-$VERS.zip $LOCALREPO/rundeck-core/zips/rundeck-core-$VERS.zip
if [ 0 != $? ]
then
   echo "RUN build failed: cannot copy target/dist/zips/rundeck-core-$VERS.zip"
   exit 2
fi
mkdir -p $LOCALREPO/rundeck-core/tgzs
cp target/dist/tgzs/rundeck-core-$VERS.tgz $LOCALREPO/rundeck-core/tgzs/rundeck-core-$VERS.tgz
if [ 0 != $? ]
then
   echo "RUN build failed: cannot copy target/dist/tgzs/rundeck-core-$VERS.tgz"
   exit 2
fi

mkdir -p $MAVEN_HOME/repository/rundeck-core/jars
cp target/rundeck-core-$VERS.jar $MAVEN_HOME/repository/rundeck-core/jars/rundeck-core-$VERS.jar
if [ 0 != $? ]
then
   echo "RUN build failed: cannot copy target/rundeck-core-$VERS.jar"
   exit 2
fi
}

build_rundeckapp(){
#####################
#
# Run Deck build
#

cd $BASEDIR/rundeckapp
# copy the dependencies into the lib directory
cp $LOCALREPO/rundeck-core/jars/rundeck-core-$VERS.jar lib/
MYPATH=$PATH
export GRAILS_HOME=$GRAILS_HOME_111
echo GRAILS_HOME=$GRAILS_HOME
export PATH=$PATH:$GRAILS_HOME/bin

GWORKDIR=$BASEDIR/rundeckapp/work

#echo 'y' to the command to quell y/n prompt on second time running it:
#echo -e "y\n" | $GRAILS_HOME/bin/grails install-plugin jetty
yes | $GRAILS_HOME/bin/grails -Dgrails.project.work.dir=$GWORKDIR install-plugin jetty
if [ 0 != $? ]
then
   echo "failed to install jetty plugin"
   exit 2
fi  

#echo 'y' to the command to quell y/n prompt on second time running it:
#yes | $GRAILS_HOME/bin/grails install-plugin $BASEDIR/rundeckapp/plugins/grails-webrealms-0.2.zip
#if [ 0 != $? ]
#then
   #echo "failed to install webrealms plugin"
   #exit 2
#fi  

# run clean and test 
$GRAILS_HOME/bin/grails -Dgrails.project.work.dir=$GWORKDIR clean
if [ 0 != $? ]
then
   echo "Run Deck clean failed"
   exit 2
fi  

$GRAILS_HOME/bin/grails -Dgrails.project.work.dir=$GWORKDIR test-app
if [ 0 != $? ]
then
   echo "Run Deck tests failed"
   exit 2
fi  

#run war phase
yes | $GRAILS_HOME/bin/grails -Dgrails.project.work.dir=$GWORKDIR prod build-launcher
if [ 0 != $? ]
then
   echo "Run Deck build failed"
   exit 2
fi  

# artifacts: rundeck-X.war
mkdir -p $LOCALREPO/rundeck/wars
cp target/rundeck-$VERS.war $LOCALREPO/rundeck/wars/rundeck-$VERS.war
if [ 0 != $? ]
then
   echo "Rundeck build failed: cannot copy target/rundeck-$VERS.war"
   exit 2
fi  

# artifacts: rundeck-launcher-X.jar
mkdir -p $LOCALREPO/rundeck-launcher/jars/
cp target/rundeck-launcher-$VERS.jar $LOCALREPO/rundeck-launcher/jars/rundeck-launcher-$VERS.jar
if [ 0 != $? ]
then
   echo "Rundeck build failed: cannot copy target/rundeck-launcher-$VERS.jar"
   exit 2
fi
export PATH=$MYPATH
export GRAILS_HOME=
}

do_release_increment(){
    RELNUM=$(($RELNUM + 1))

    grep -q version.release.number $BASEDIR/version.properties
    if [ $? != 0 ] ; then
        echo "version.release.number=$RELNUM">> $BASEDIR/version.properties
        echo "Added release number to $BASEDIR/version.properties"
    else
        perl  -i'.orig' -p -e "s#^version\.release\.number\s*=.*\$#version.release.number=$RELNUM#" $BASEDIR/version.properties || (echo "Failed to commit release number increment: $!" && exit 2)
        echo "Updated release number in $BASEDIR/version.properties"
    fi

    svn ci -m "Update Release number to $RELNUM" $BASEDIR/version.properties || (echo "Failed to commit release number increment: $!" && exit 2)
    echo "Update release number to $RELNUM"
}

if [ "$1" = "-clean" ] ; then
    shift
    do_clean
fi

DO_RELEASE_INCREMENT=false
if [ "$1" = "-release" ] ; then
    shift
    #do_release_increment
    DO_RELEASE_INCREMENT=true
fi

if [ -z "$*" ] ; then

    prepare_build
    build_rundeck_core
    build_rundeckapp
else
    prepare_build
    for i in $* ; do
        case "$i" in
            rundeck_core)
                build_rundeck_core
                ;;
            rundeckapp)
                build_rundeckapp
                ;;
            server_rpm)
                build_server_rpm
                ;;
            *)
                echo unknown action: ${i}
                exit 1
            ;;
        esac
    done
    exit 0
fi
