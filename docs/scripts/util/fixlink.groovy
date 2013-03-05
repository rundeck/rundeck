#!/usr/bin/env groovy

link=args[1]
val=args[2]

//println "links: ${links}"
new File(args[0]).eachFile({File f->
    if(f.isFile() && f.name=~/\.(md|txt)$/){
        println "Check file ${f.name}"
        File newfile= new File(f.parentFile,f.name+".new")
        newfile.withWriter{w->
            f.eachLine{l->
                def newl=l.replaceAll(~/\]\((.*?)\)/,{all,m1->
                    if(m1=~"^"+java.util.regex.Pattern.quote(link)){
                        def newval=m1.replaceFirst("^"+java.util.regex.Pattern.quote(link),val)
                        println "rewrite ${m1} to ${newval}"
                        "](${newval})"
                    }else{
                        all
                    }
                })
                w.writeLine(newl)
            }
        }
        newfile.renameTo(f)
    }
})
