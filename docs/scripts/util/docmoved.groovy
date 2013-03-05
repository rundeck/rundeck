#!/usr/bin/env groovy

/*
 * linkmoved <link> [link depth]
 * create a new markdown file, containing appropriate headers, such that each anchor link into the file
 * will show a new link referencing the new location of the old anchor.
 *
*/
link=args[0]
depth=args[1]?.toInteger()

if(depth){
    depth.times{
        link=link.replaceAll(/^[^\/]+\//,'')
    }
}

def writeout={w->
    w<<"% Link Moved\n\nThis document has moved.\n\n"
    w<<"* New location: [${link}](${link})\n"
    w<<"\n"
}
if(args.length>2){
    new File(args[2]).withWriter(writeout)
}else{
    writeout(System.out)
}

