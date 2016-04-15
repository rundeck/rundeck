import com.dtolabs.rundeck.plugins.notification.NotificationPlugin;
import org.springframework.mail.javamail.*;
import javax.mail.internet.*;
import groovy.xml.MarkupBuilder
import groovy.text.SimpleTemplateEngine

/**
 * This plugin can be used to send a custom HTML email as a notification.
 *
 * It makes use of the grails mail plugin which is used in Rundeck.
 */

/**
 * mail sender configuration properties
 * @see http://static.springsource.org/spring/docs/3.0.x/api/org/springframework/mail/javamail/JavaMailSenderImpl.html
 */
def mailProperties=[
    host: "localhost",
    port: 25,
    defaultEncoding:"utf-8"
]

/**
 * define the default subject line configuration
 */
def defaultSubjectLine='$STATUS [$PROJECT] $JOB run by $USER (#$ID)'
/**
 * Expands the Subject string using a predefined set of tokens
 */
def subjectString={text,binding->
    //defines the set of tokens usable in the subject configuration property
    def tokens=[
        '$STATUS': binding.execution.status.toUpperCase(),
        '$status': binding.execution.status.toLowerCase(),
        '$PROJECT': binding.execution.project,
        '$JOB': binding.execution.job.name,
        '$GROUP': binding.execution.job.group,
        '$JOB_FULL': (binding.execution.job.group?binding.execution.job.group+'/':'')+binding.execution.job.name,
        '$USER': binding.execution.user,
        '$ID': binding.execution.id.toString()
    ]
    text.replaceAll(/(\$\w+)/){
        if(tokens[it[1]]){
            tokens[it[1]]
        }else{
            it[0]
        }
    }
}

/**
 * Generates an HTML string using a markup builder for the input closure
 */
def buildHtml={ Closure clos->
    def sw=new StringWriter()
    clos.delegate=new MarkupBuilder(sw)
    clos.resolveStrategy=Closure.DELEGATE_FIRST
    clos.call()
    sw.toString()
}

/**
 * This closure generates the custom HTML message as a string.
 * 
 * @param subject the subject line 
 * @param execution the execution data map
 * @param config the plugin configuration data
 */
def generateMail={subject, Map execution, Map config->
    buildHtml{
        //customize this content with your own HTML
        html{
            body{
                h1(subject)
                ul{
                    li{
                        p{

                        if(execution.status=='running'){
                            em("Started")
                            yield(" by: ${execution.user} at ${execution.dateStarted}")
                        }else if(execution.status=='succeeded'){
                            em("Finished")
                            yield(" at: ${execution.dateEnded}")
                        }else if(execution.abortedBy){
                            em("KILLED")
                            yield(" by ${execution.abortedBy} at: ${execution.dateEnded}")
                        }else{
                            //failed
                            em("Failed")
                            yield(" at: ${execution.dateEnded}")
                            if(execution.failedNodeList){
                                div{
                                    yield("Failed node list:")
                                    ul{
                                        execution.failedNodeList.each{
                                            li(it)
                                        }
                                    }
                                }
                            }
                        }

                        }
                    }
                    li{
                        a(href:execution.href,"Output for ${execution.id}")
                    }
                    // div{
                    //     yield("execution data: ${execution}")
                    // }
                }
            }
        }
    }
}
/**
 * Sends email by building the message from the closure argument
 */
def sendMail={Closure callable->
    //create a sender and set the mail properties
    def sender = new JavaMailSenderImpl()
    sender.javaMailProperties.putAll(mailProperties)
    // mailProperties.each{k,v->
        // sender[k]=v
    // }

    //create a builder and use it as the delegate of the closure
    //which will build the message
    def mbuilder = new grails.plugin.mail.MailMessageBuilder(sender,new ConfigObject())
    callable.delegate = mbuilder
    callable.resolveStrategy = Closure.DELEGATE_FIRST
    callable.call()

    //send the message
    def message = mbuilder.finishMessage()
    
    if(message instanceof MimeMailMessage) {
        MimeMailMessage msg = message
        MimeMessage mimeMsg = msg.getMimeMessage()
        sender.send(mimeMsg)
    }
}


//defines the NotificationPlugin
rundeckPlugin(NotificationPlugin){

    title="Mail"

    description="Sends Mail"

    configuration{

        recipients(title:"Email recipients",required:true, description: "Enter comma-separated email addresses"){
            //validate the recipients field as a comma-separated list of emails
            it.split(",").every { obj ->
                //allow embedded property references like ${job.user.email}
                //otherwise, make sure its a valid email address
                obj.indexOf('${')>=0 || org.apache.commons.validator.EmailValidator.getInstance().isValid(obj)
            }
        }

        subject(title:"Subject line",defaultValue:defaultSubjectLine, 
            required:true,
            description:'Subject line string, which can contain these variables: $STATUS (job status), $PROJECT (project name), '+
            '$JOB (job name), $GROUP (group name), $JOB_FULL (job group and name), $USER (user name)')
    }
    /**
     * handleTrigger is the common closure used for all the triggers
     */
    def handleTrigger= { String trigger, Map execution, Map config->

        //generate subject string from the configuration template
        def subjectStr=subjectString(config.subject,[execution:execution,trigger:trigger])
        try{
            //try to send the mail to all recipients
            sendMail{
                to( config.recipients.split(",") as List)
                subject subjectStr
                //generate the HTML to send
                html( generateMail(subjectStr,execution,config))
            }
            return true
        }catch(Exception e){
            System.err.println("Error sending notification email: "+e.getMessage());
        }
        false
    }

    //define the triggers, and curry the handleTrigger closure to specify the correct trigger name
    onsuccess(handleTrigger.curry('success'))
    onfailure(handleTrigger.curry('failure'))
    onstart(handleTrigger.curry('start'))

}