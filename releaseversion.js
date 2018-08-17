//run with osascript
const fs = require('fs')


let VersionFile = fs.readFileSync('rundeckapp/grails-app/assets/javascripts/version.js').toString()

eval(VersionFile)

const version = process.argv[2]

var vers=new RundeckVersion({"versionString":version});
console.log("export REL_NAME="+vers.name()+"\n"+ "export REL_COLOR="+vers.color() + "\n"+ "export REL_ICON="+vers.icon() + "\n" + "export REL_TEXT=\""+vers.text() +"\"")