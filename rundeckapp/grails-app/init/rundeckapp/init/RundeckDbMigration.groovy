package rundeckapp.init

import org.grails.build.parsing.CommandLineParser
import org.grails.plugins.databasemigration.command.DbmRollbackCommand
import org.springframework.context.ApplicationContext

class RundeckDbMigration {

    ApplicationContext applicationContext

    RundeckDbMigration(ApplicationContext appContext){
        this.applicationContext = appContext
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