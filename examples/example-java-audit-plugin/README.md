#Example Rundeck auditing listener plugin

This is an example about how to use a an Audit Listener plugin to capture auditing events from rundeck application.
 
## Install 

* using maven build: `mvn clean package`
* install: copy the file `target/example-audit-plugin-1.0.0.jar` to libext folder

#Configure

The plugin provides a default configuration, creating an 'audit.log' file at the current working directory. To configure this use the following settings on rundeck framework.properties file:
- Use `framework.plugin.AuditEventListener.ExampleAuditListener.path=some/path` to change the output file path. 
- Use `framework.plugin.AuditEventListener.ExampleAuditListener.filename=some.file.name` to change the output file name. 


## Use 

Implement your own `AuditEventListener` to capture events.
Refer to the example found on `src/main/java/com/rundeck/plugin/ExampleAuditListener.java`




