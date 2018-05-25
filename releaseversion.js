//run with osascript
function run(args){
	var vers=new RundeckVersion({"versionString":args});
	return 	"export REL_NAME="+vers.name()+"\n"+ "export REL_COLOR="+vers.color() + "\n"+ "export REL_ICON="+vers.icon() + "\n" + "export REL_TEXT=\""+vers.text() +"\""	;
}
