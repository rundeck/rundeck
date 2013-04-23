#!/usr/bin/env groovy

/*
 * remap <indexsource> <indexdest> [link depth]
 * create a new markdown file, containing appropriate headers, such that each anchor link into the file
 * will show a new link referencing the new location of the old anchor.
 *
*/


source=[:]
//indexsource: get each anchor, create header name
def sourceclos={l->
    def arr=l.split('#',2)
    def anchor=arr[1]
    def title=anchor.replaceAll(/-\d+$/,'').replaceAll('-',' ').replaceAll(/\b(.)/,{all,m1->
        m1.toUpperCase()
    })
    
    source[anchor]=[title:title,links:[]]
}
new File(args[0]).eachLine(sourceclos)
//println "source titles: ${source}"
depth=args[2].toInteger()
//indexdest: get each anchor, connect to source anchor and add link
def dest=[:]
def mapclos={l->
    def arr=l.split('#',2)
    def anchor=arr[1]
    def x=-1
    if(anchor==''){
        //substitute filename to use as anchor
        anchor=arr[0].replaceAll(/^.+?\/([^\/]+)\.html$/,'$1')
        println "Substituted: ${anchor} for ${l}"
    }
    if(dest[anchor]){
        x=1
        if(anchor=~/-\d+$/){
            x = anchor.substring(arr[1].lastIndexOf('-')+1).toInteger()
            anchor=anchor.replaceAll(/-\d+$/,'')
        }
        while(dest[anchor+"-${x}"]){
            x++
        }
        //println "resolved ${arr[0]}#${arr[1]} to ${anchor}-${x}"
    }
    def res=anchor+(x>0?"-${x}":'')
    depth.times{
        l=l.replaceAll(/^[^\/]+\//,'')
    }
    dest[res]=l
    if(source[res]){
        source[res].links<<l
    }else{
        //println "Not found: ${l}"
    }
}
new File(args[1]).eachLine(mapclos)
//println "dest: ${dest}"

//generate: iterate each source anchor. output header, link to dest

def writeout={w->
    w<<"**Links Moved**\n\nThis document provides the new location of moved links.\n\n"
    source.each{k,v->
        w<<"## ${v.title}\n\n"
        if(v.links){
            v.links.each{l->
                w<<"* Location moved: [${l}](${l})\n"
            }
        }else{
            println "Not linked: ${k}"
            w<<"* **Not found**\n"
        }
        w<<"\n"
    }
}
if(args.length>3){
    new File(args[3]).withWriter(writeout)
}else{
    writeout(System.out)
}

