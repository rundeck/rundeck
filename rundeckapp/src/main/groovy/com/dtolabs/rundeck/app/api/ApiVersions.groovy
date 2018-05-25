package com.dtolabs.rundeck.app.api

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
    public static final Map VersionMap = [:]
    public static final List Versions = [V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13, V14, V15, V16, V17, V18, V19, V20, V21, V22, V23, V24]
    static {
        Versions.each { VersionMap[it.toString()] = it }
    }
    public static final Set VersionStrings = new HashSet(VersionMap.values())

    public final static int API_EARLIEST_VERSION = V1
    public final static int API_CURRENT_VERSION = V24
    public final static int API_MIN_VERSION = API_EARLIEST_VERSION
    public final static int API_MAX_VERSION = API_CURRENT_VERSION
}
