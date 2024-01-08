import java.util.regex.Matcher

File dir = new File(args[0])
if (!dir.isDirectory()) {
    System.err.println('args[0]: Specify a directory')
}
Map<String, Object> locales = [:]
processDir(dir,locales)
println groovy.json.JsonOutput.toJson(locales)


void processDir(File dir,Map<String, Object> locales) {

//File outfile = new File(args[1])

    String defLocale = System.getenv('DEF_LOCALE') ?: 'en_US'
    dir.listFiles((FilenameFilter) (file, name) -> name ==~ ~/^messages.*\.properties$/).each {File infile->
        Properties props = new Properties()
        infile.withReader('ISO-8859-1') { props.load(it) }

        Matcher matcher = infile.name =~ /^messages_(.+)\.properties$/
        String locale = matcher.matches() && matcher.group(1) ? matcher.group(1) : defLocale
//    Map<String, Object> keys = destructure(props)
        Map<String, Object> keys = extract(props)
        locales[locale] = keys
    }
}

private Map<String, Object> extract(Properties props) {
    Map<String, Object> keys = [:]

    def rmprefix = '@name@.'
    props.each { k, v ->
        k = k.startsWith(rmprefix) ? k.substring(rmprefix.length()) : k
        keys[k] = v
    }
    return keys
}

private Map<String, Object> destructure(Properties props) {
    Map<String, Object> keys = [:]

    def rmprefix = '@name@.'
    props.each { k, v ->
        k = k.startsWith(rmprefix) ? k.substring(rmprefix.length()) : k
        def parts = k.split('\\.')
        def map = keys
        for (String part : parts[0..<-1]) {
            map = map.computeIfAbsent(part, { [:] })
        }
        map[parts[-1]] = v
    }
    return keys
}