package com.dtolabs.rundeck.app.api

class ApiVersions {
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
    public static final int V43 = 43
    public static final int V44 = 44
    public static final int V45 = 45
    public static final int V46 = 46
    public static final int V47 = 47
    public static final int V48 = 48
    public static final int V49 = 49
    public static final int V50 = 50
    public static final int V51 = 51
    public static final int V52 = 52
    public static final int V53 = 53

    // ^^^ New version is to be added above this line. ^^^
    // Ensure the constant name follows the API_VERSION_VARIABLE_NAME_PATTERN pattern.
    // Update the "Current API version Configuration" section below.

    /**
     * Current API version Configuration
     */
    // References the current API version
    public final static int API_CURRENT_VERSION = V53
    // Hardcoded inline string constant for the current version used in API doc generation
    public final static String API_CURRENT_VERSION_STR = "53"

    /**
     * Version span configurations
     */
    public final static int API_EARLIEST_VERSION = V14
    public final static int API_DEPRECATION_VERSION = V17

    /**
     *  The code bellow uses reflection on the data declared above
     */

    // Uses class metadata to generate a List and a Map of supported API versions
    public static final def API_VERSION_VARIABLE_NAME_PATTERN = /\bV\d+\b/
    public static final List<Integer> Versions
    static  {
        def collectedVersions = ApiVersions.declaredFields.findAll {
            it.name ==~ API_VERSION_VARIABLE_NAME_PATTERN && it.type == int
        }.collect { (Integer)it.get(null) }
        Versions = collectedVersions.asImmutable()
    }

    public static final Map VersionMap = [:]
    static {
        Versions.each { VersionMap[it.toString()] = it }
    }
    public static final Set VersionStrings = new HashSet(VersionMap.values())

    public final static int API_MIN_VERSION = API_EARLIEST_VERSION
    public final static int API_MAX_VERSION = API_CURRENT_VERSION
}
