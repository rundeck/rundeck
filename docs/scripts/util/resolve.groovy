#!/usr/bin/env groovy
/*
 * resolve anchored links to correct html file
 * resolve.groovy <links.index> <dir> <removePaths> <relativePath>
 *
 * looks for each markdown link in the form (page.html#anchor)
 * looks for the anchor in the links index
 * if unique anchor found, replaces the link with the new page 
 * if multiples found, prints warning
 *
 * removePaths: integer, default 0.  if not 0, removes this many path segments from the links in the index
 * relativePath: path relative to the links in the index in which the documents exist, Should end in a '/'.
 *   if set will cause updated links to be rewritten with appropriate relative pathing. 
 *  e.g.: 
 *  if relativePath = 'test/files/' then
 *      test/files/something.html, will become something.html
        test/files/sub/file.html   will become sub/file.html
        test/else/another.html     will become ../else/another.html
        test/file.html             will become ../file.html
        test.html                  will become ../../test.html
*/

removeRelative = args.length>2?args[2].toInteger():0
pathRelative = args.length>3?args[3]:null
links=[:]
new File(args[0]).eachLine{l->
    if(l.startsWith('#')){
        return
    }
    def arr=l.split('#',2)
    def anchor=arr[1]
    def x=-1
    def page = arr[0]
    if(removeRelative>0){
        def a2 = page.split('/')
        if(a2.length>removeRelative){
            def z = a2[removeRelative..<a2.length]
            page=z.join('/')
        }
    }
    if(links[anchor]){
        links[anchor]<< page
    }else{
        links[anchor]=[page]
    }
}
//println "links: ${links}"

new File(args[1]).eachFile({File f->
    if(f.isFile() && f.name=~/\.(md|txt)$/){
        println "Check file ${f.name}"
        File newfile= new File(f.parentFile,f.name+".new")
        def found=false
        newfile.withWriter{w->
        def line=1
            f.eachLine{l->
                def newl=l.replaceAll(~/\((([^\(\)]+?\.html)#(.+?))\)/,{all,m1,m2,m3->
                    if(links[m3] && links[m3].size()==1 && links[m3][0]!=m2){
                        def newpage=links[m3][0]
                        if(pathRelative && newpage.startsWith(pathRelative)){
                            newpage=newpage[pathRelative.size()..<newpage.size()]
                        }else if(pathRelative && newpage.contains('/')){
                            def paths=pathRelative.split('/').findAll{it}
                            while(paths && !links[m3][0].startsWith(paths.join('/')+'/')){
                                paths.pop()
                                newpage="../${newpage}"
                            }
                        }
                        def newlink="(${newpage}#${m3})"
                        if(newlink!=all){
                            println "${line}: rewrite ${m1} to ${newlink}"
                            found=true
                            return newlink
                        }
                    }else if(links[m3] && links[m3].size()>1){
                        println "${line}! unresolved ${m1}: ${links[m3]}"
                    }else if(!links[m3]){
                        println "${line}! unknown ${m1}"
                    }
                    all
                })
                w.writeLine(newl)
                line++
            }
        }
        if(!found){
            newfile.delete()
        }
        newfile.renameTo(f)
    }
})
