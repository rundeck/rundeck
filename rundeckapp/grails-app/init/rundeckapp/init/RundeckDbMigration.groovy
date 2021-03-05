package rundeckapp.init

import org.grails.build.parsing.CommandLineParser
import org.grails.plugins.databasemigration.command.DbmRollbackCommand
import org.springframework.context.ApplicationContext

class RundeckDbMigration {

    ApplicationContext applicationContext
    def grailsApplication

    RundeckDbMigration(ApplicationContext appContext, def grailsApp){
        this.applicationContext = appContext
        this.grailsApplication = grailsApp
    }

    /**
     * Rollsback database to specified tag
     */
    def rollback(def tagName){
        DbmRollbackCommand rollbackCommand = new DbmRollbackCommand()
        rollbackCommand.applicationContext = applicationContext
        rollbackCommand.commandLine = new CommandLineParser().parse([tagName] as String[])
        rollbackCommand.args.add(tagName)
        rollbackCommand.handle()
    }

}