package rundeckapp.init

import org.grails.build.parsing.CommandLineParser
import org.grails.plugins.databasemigration.DatabaseMigrationTransactionManager
import org.grails.plugins.databasemigration.command.DbmChangelogSyncCommand
import org.grails.plugins.databasemigration.command.DbmGormDiffCommand
import org.grails.plugins.databasemigration.command.DbmRollbackCommand
import org.grails.plugins.databasemigration.command.DbmTagCommand
import org.grails.plugins.databasemigration.command.DbmUpdateCommand
import org.grails.plugins.databasemigration.liquibase.GrailsLiquibase
import org.springframework.context.ApplicationContext

import javax.sql.DataSource

class RundeckDbMigration {

    ApplicationContext applicationContext
    def grailsApplication

    def syncDatabase(def migrationsDir){
        def updateOnStart = grailsApplication.config.getProperty("grails.plugin.migration.updateOnStart", Boolean, false)
        if(updateOnStart) {
            createDiff(migrationsDir)
            runDiff()
            runMigrations()
        }
    }

    def runMigrations(){
        new DatabaseMigrationTransactionManager(applicationContext, 'dataSource').withTransaction {
            GrailsLiquibase gl = new GrailsLiquibase(applicationContext)
            gl.dataSource = applicationContext.getBean('dataSource', DataSource)
            gl.changeLog = grailsApplication.config.getProperty("grails.plugin.migration.updateOnStartFileName", String)
            gl.dataSourceName = 'dataSource'
            gl.afterPropertiesSet()
        }

    }
    def runDiff(){
        String diffFileName = "diff-${grailsApplication.metadata['build.ident']}.groovy"
        String tagName = grailsApplication.metadata['build.ident']
        DbmTagCommand tagCommand = new DbmTagCommand()
        tagCommand.applicationContext = applicationContext
        tagCommand.commandLine = new CommandLineParser().parse([tagName] as String[])
        tagCommand.args.add(tagName)
        tagCommand.handle()
        DbmUpdateCommand updateCommand = new DbmUpdateCommand()
        updateCommand.applicationContext = applicationContext
        updateCommand.commandLine = new CommandLineParser().parse([diffFileName] as String[])
        updateCommand.args.add(diffFileName)
        updateCommand.handle()

    }

    def createDiff(File migrationsDir){
        String diffFileName = "diff-${grailsApplication.metadata['build.ident']}.groovy"
        File diffFile = new File(migrationsDir.absolutePath, diffFileName)
        if (diffFile.exists() && grails.util.Environment.PRODUCTION != grails.util.Environment.getCurrent()){
            diffFileName = "diff-${grailsApplication.metadata['build.ident']}-${new Date().format("yyyy-MM-dd'T'HH:mm")}.groovy"
            diffFile = new File(migrationsDir.absolutePath, diffFileName)
        }
        else if (diffFile.exists())
            return

        DbmGormDiffCommand diffCommand = new DbmGormDiffCommand()
        diffCommand.applicationContext = applicationContext
        diffCommand.commandLine = new CommandLineParser().parse([diffFile.name] as String[])
        diffCommand.args.add(diffFile.name)
        diffCommand.commandLine.undeclaredOptions.put("add", true)
        diffCommand.handle()

    }

    def rollback(File migrationsDir, def tagName){
        migrationsDir.eachFile { file ->
            def newconf = new ConfigObject()
            newconf.grails.plugin.databasemigration.changelogFileName = file.name
            grailsApplication.config.merge(newconf)

            DbmRollbackCommand rollbackCommand = new DbmRollbackCommand()
            rollbackCommand.applicationContext = applicationContext
            rollbackCommand.commandLine = new CommandLineParser().parse([tagName] as String[])
            rollbackCommand.args.add(tagName)
            rollbackCommand.handle()
        }
    }

    def syncChangeLog(File migrationsDir){
        migrationsDir.eachFile { file ->
            def newconf = new ConfigObject()
            newconf.grails.plugin.databasemigration.changelogFileName = file.name
            grailsApplication.config.merge(newconf)

            DbmChangelogSyncCommand changelogSyncCommand = new DbmChangelogSyncCommand();
            changelogSyncCommand.applicationContext = applicationContext
            changelogSyncCommand.handle()
        }
    }
}
