
Properties props=new Properties()
System.in.withReader('ISO-8859-1') { props.load(it) }
def keys = [:]
def rmprefix='@name@.'
props.each{k,v->
    k = k.startsWith(rmprefix)?k.substring(rmprefix.length()):k
    keys[k]=v
}


println groovy.json.JsonOutput.toJson(keys)
