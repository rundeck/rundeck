
Properties props=new Properties()
System.in.withReader('ISO-8859-1') { props.load(it) }
def keys = [:]
def rmprefix='@name@.'
props.each{k,v->
    k = k.startsWith(rmprefix)?k.substring(rmprefix.length()):k
    def parts=k.split('\\.')
    def map=keys
    println k
    println parts[0..<-1].join(' ') +" => "+parts[-1]
    for (String part : parts[0..<-1]) {
        map=map.computeIfAbsent(part, { [:] })
    }
    map[parts[-1]]=v
}


println groovy.json.JsonOutput.toJson(keys)
