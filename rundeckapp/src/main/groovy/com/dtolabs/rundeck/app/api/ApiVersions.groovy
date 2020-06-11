package com.dtolabs.rundeck.app.api

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode

import java.util.regex.Pattern

@CompileStatic
class ApiVersions {
    public static final int V1 = 1
    public static final int V2 = 2
    public static final int V3 = 3
    public static final int V4 = 4
    public static final int V5 = 5
    public static final int V6 = 6
    public static final int V7 = 7
    public static final int V8 = 8
    public static final int V9 = 9
    public static final int V10 = 10
    public static final int V11 = 11
    public static final int V12 = 12
    public static final int V13 = 13
    public static final int V14 = 14
    public static final int V15 = 15
    public static final int V16 = 16
    public static final int V17 = 17
    public static final int V18 = 18
    public static final int V19 = 19
    public static final int V20 = 20
    public static final int V21 = 21
    public static final int V22 = 22
    public static final int V23 = 23
    public static final int V24 = 24
    public static final int V25 = 25
    public static final int V26 = 26
    public static final int V27 = 27
    public static final int V28 = 28
    public static final int V29 = 29
    public static final int V30 = 30
    public static final int V31 = 31
    public static final int V32 = 32
    public static final int V33 = 33
    public static final int V34 = 34
    public static final int V35 = 35
    public static final Map VersionMap = [:]
    public static final List Versions = [V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13, V14, V15, V16, V17, V18,
                                         V19, V20, V21, V22, V23, V24, V25, V26, V27, V28, V29, V30, V31, V32,V33,V34,V35]
    static {
        Versions.each { VersionMap[it.toString()] = it }
    }

    public final static String API_EARLIEST_VERSION_STR = '1'
    public final static String API_CURRENT_VERSION_STR = '35.1'

    public final static Version API_EARLIEST_VERSION_FULL = parseVersion(API_EARLIEST_VERSION_STR)
    public final static int API_EARLIEST_VERSION = API_EARLIEST_VERSION_FULL.major
    public final static Version API_CURRENT_VERSION_FULL = parseVersion(API_CURRENT_VERSION_STR)
    public final static int API_CURRENT_VERSION = API_CURRENT_VERSION_FULL.major
    private final static ApiVersions SINGLETON
    static{
        SINGLETON = new ApiVersions(API_EARLIEST_VERSION_STR, API_CURRENT_VERSION_STR)
    }

    private Version earliestVersion
    private Version currentVersion
    protected ApiVersions(String earliest, String current){
        this.earliestVersion=parseVersion(earliest)
        this.currentVersion=parseVersion(current)
    }
    static Version parseVersion(String input) {
        Pattern VERSION_PAT = ~/^([1-9]\d*)(?:\.(\d+))?(?:\.(\d+))?$/
        def m = VERSION_PAT.matcher(input)
        if (m.matches()) {
            return Vers.version(m.group(1).toInteger(), m.group(2)?.toInteger() ?: 0, m.group(3)?.toInteger() ?: 0)
        }
        return null
    }
    static Integer parseMajorVersion(String input) {
        Pattern VERSION_PAT = ~/^([1-9]\d*)(?:\.(\d+))?(?:\.(\d+))?$/
        def m = VERSION_PAT.matcher(input)
        if (m.matches()) {
            return m.group(1)?.toInteger()
        }
        return null
    }
    static boolean isVersionSupported(String input){
        return SINGLETON.isSupported(input)
    }

    public static boolean testVersion(String input, Version api_earliest_version, Version api_current_version) {
        Version inVers = parseVersion(input)
        inVers != null && compareVersion(inVers,api_earliest_version)>=0 && compareVersion(inVers,api_current_version)<=0
    }

    boolean isSupported(String input){
        return testVersion(input, earliestVersion, currentVersion)
    }
    static int compareVersion(Version a, Version b){
        int comp = a.major - b.major
        if (comp != 0) {
            return comp
        }
        comp = a.minor - b.minor
        if (comp != 0) {
            return comp
        }
        return a.patch - b.patch
    }

    static interface Version {
        int getMajor();

        int getMinor();

        int getPatch();
    }

    @EqualsAndHashCode
    static class Vers implements Version, Comparable {
        int major
        int minor=0
        int patch=0

        static Vers version(int major, int minor = 0, int patch = 0) {
            new Vers(major: major, minor: minor, patch: patch)
        }
        @Override
        String toString() {
            return "$major.$minor" + (patch != 0 ? ".$patch" : '')
        }

        @Override
        int compareTo(final Object o) {
            if(!(o instanceof Version)){
                return -1
            }
            compareVersion(this, o)
        }
    }
}
