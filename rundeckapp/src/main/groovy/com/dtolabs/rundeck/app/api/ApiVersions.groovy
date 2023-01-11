package com.dtolabs.rundeck.app.api

class ApiVersions {
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
    public static final int V36 = 36
    public static final int V37 = 37
    public static final int V38 = 38
    public static final int V39 = 39
    public static final int V40 = 40
    public static final int V41 = 41
    public static final int V42 = 42
    public static final Map VersionMap = [:]
    public static final List Versions = [V11, V12, V13, V14, V15, V16, V17, V18,
                                         V19, V20, V21, V22, V23, V24, V25, V26,
                                         V27, V28, V29, V30, V31, V32,V33,V34,V35,
                                         V36, V37, V38, V39, V40, V41, V42]
    static {
        Versions.each { VersionMap[it.toString()] = it }
    }
    public static final Set VersionStrings = new HashSet(VersionMap.values())

    public final static int API_EARLIEST_VERSION = V11
    public final static int API_DEPRECATION_VERSION = V14
    public final static int API_CURRENT_VERSION = V42
    public final static String API_CURRENT_VERSION_STR = '42'
    public final static int API_MIN_VERSION = API_EARLIEST_VERSION
    public final static int API_MAX_VERSION = API_CURRENT_VERSION
}
