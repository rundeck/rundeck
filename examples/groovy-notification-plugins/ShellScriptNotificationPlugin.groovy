import com.dtolabs.rundeck.plugins.notification.NotificationPlugin;
import groovy.text.SimpleTemplateEngine

/**
 * This plugin executes a shell script with some arguments as defined
 * by the user in the GUI. Customization might hardcode the script file path
 * or arguments for a particular purpose.
 */
rundeckPlugin(NotificationPlugin){
    
    //plugin title shown in GUI
    title="Shell Script"

    //plugin description shown in GUI
    description="Calls a shell script"

    //define configuration options for the plugin
    configuration{

        scriptPath( title:"Path to script", required:true){
            //validation closure, returns true if the file exists
            new File(it).exists()
        }
        scriptArgs title:"Arguments"
    }

    def shellCommand=['/bin/sh',"-c"]

    /**
     * common closure used for all the notification triggers
     */
    def handleTrigger = {
        //with no closure args, there is a "config" and an "execution" variable in the context
        
        //create a new list starting with the base shell command
        def command=new ArrayList(shellCommand)

        //use the configuration scriptPath and scriptArgs to build the command to run
        if(configuration.scriptArgs){
            command<< configuration.scriptPath+' '+configuration.scriptArgs
        }else{
            command<< configuration.scriptPath
        }

        //execute the command as a process and copy the output to the System streams
        def proc = command.execute()
        proc.waitForProcessOutput(System.out,System.err)

        //finally return true if the process exited with 0 exitcode
        proc.exitValue()==0
    }

    //define handlers for the notification events
    onsuccess handleTrigger
    onfailure handleTrigger
    onstart handleTrigger
}
