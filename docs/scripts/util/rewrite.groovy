#!/usr/bin/env groovy

/*
 * rewrite anchor links to point to newly resolved links.
*/

links=[:]
new File(args[0]).eachLine{l->
    def arr=l.split('#',2)
    def anchor=arr[1]
    def x=-1
    if(links[anchor]){
        x=1
        if(anchor=~/-\d+$/){
            x = anchor.substring(arr[1].lastIndexOf('-')+1).toInteger()
            anchor=anchor.replaceAll(/-\d+$/,'')
        }
        while(links[anchor+"-${x}"]){
            x++
        }
        println "resolved ${arr[0]}#${arr[1]} to ${anchor}-${x}"
    }
    
    links[anchor+(x>0?"-${x}":'')]=l.replaceAll(/^.*\/([^\/]+\.html)/,'$1')
}
//println "links: ${links}"
new File(args[1]).eachFile({File f->
    if(f.isFile() && f.name=~/\.(md|txt)$/){
        println "Check file ${f.name}"
/*        File newfile= new File(f.parentFile,f.name+".new")*/
/*        newfile.withWriter{w->*/
            f.eachLine{l->
                def newl=l.replaceAll(~/\((Rundeck-Guide\.html#(.+?))\)/,{all,m1,m2->
                    if(links[m2]){
                        println "rewrite ${m1} to ${links[m2]}"
                        "(../manual/${links[m2]})"
                    }else{
                        all
                    }
                })
                w.writeLine(newl)
            }
/*        }*/
        //newfile.renameTo(f)
    }
})
