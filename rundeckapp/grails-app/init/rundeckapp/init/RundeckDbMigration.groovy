package rundeckapp.init

import grails.io.IOUtils
import liquibase.changelog.ChangeLogParameters
import liquibase.changelog.ChangeSet
import liquibase.parser.ChangeLogParser
import liquibase.parser.ChangeLogParserFactory
import liquibase.resource.ClassLoaderResourceAccessor
import liquibase.util.StreamUtil
import org.grails.build.parsing.CommandLineParser
import org.grails.plugins.databasemigration.DatabaseMigrationTransactionManager
import org.grails.plugins.databasemigration.command.DbmChangelogSyncCommand
import org.grails.plugins.databasemigration.command.DbmRollbackCommand
import org.grails.plugins.databasemigration.command.DbmUpdateCommand
import org.grails.plugins.databasemigration.liquibase.GrailsLiquibase
import org.springframework.context.ApplicationContext

import javax.sql.DataSource
import java.nio.file.Files
import java.nio.file.Paths

class RundeckDbMigration {

    ApplicationContext applicationContext
    def grailsApplication

    RundeckDbMigration(ApplicationContext appContext, def grailsApp){
        this.applicationContext = appContext
        this.grailsApplication = grailsApp
    }
    /**
     * Creates all change log files to the rundeck base/server/migrations directory
     */
    def createChangeLogFiles(File migrationsDir){
        org.apache.commons.io.FileUtils.cleanDirectory(migrationsDir)
        def changelogs = []
        changelogs << grailsApplication.config.getProperty("grails.plugin.databasemigration.changelog", String)
        changelogs << grailsApplication.config.getProperty("rundeck.plugins.databasemigration.changelogFiles").split(',')
        changelogs = changelogs.flatten()

        def classLoaderResourceAccessor = new ClassLoaderResourceAccessor()
        changelogs.each { changelogName ->
            boolean isWritten = writeChangelog(classLoaderResourceAccessor, migrationsDir, changelogName)
            if(isWritten) {
                ChangeLogParameters changeLogParameters = new ChangeLogParameters();
                ChangeLogParser parser = ChangeLogParserFactory.getInstance().getParser(changelogName,
                                                                                        classLoaderResourceAccessor);
                def databaseChangeLog = parser.parse(changelogName, changeLogParameters, classLoaderResourceAccessor);
                List<ChangeSet> changeSets = databaseChangeLog.getChangeSets()
                def filePaths = changeSets.collect { it.filePath }.unique()
                filePaths.each { fPath ->
                    if (changelogName != fPath) {
                        File ch = new File(migrationsDir, fPath)
                        def target = Paths.get(ch.getAbsolutePath())
                        Files.createDirectories(target.getParent());
                        writeChangelog(classLoaderResourceAccessor, migrationsDir, fPath)
                    }
                }
            }
        }
    }

    /**
     * Runs the rundeck core migrations file against the database
     */
    def runMigrations(){
        def updateOnStart = grailsApplication.config.getProperty("grails.plugin.migration.updateOnStart", Boolean, false)
        if(updateOnStart) {
            new DatabaseMigrationTransactionManager(applicationContext, 'dataSource').withTransaction {
                GrailsLiquibase gl = new GrailsLiquibase(applicationContext)
                gl.dataSource = applicationContext.getBean('dataSource', DataSource)
                gl.changeLog = grailsApplication.config.getProperty("grails.plugin.databasemigration.changelog", String)
                gl.dataSourceName = 'dataSource'
                gl.afterPropertiesSet()
            }
        }

    }

    /**
     * Rollsback database to specified tag
     */
    def rollback(File migrationsDir, def tagName){
        createChangeLogFiles(migrationsDir)
        def cfg = new ConfigObject()
        cfg.grails.plugin.databasemigration.changelogLocation = migrationsDir.absolutePath
        grailsApplication.config.merge(cfg)

        DbmRollbackCommand rollbackCommand = new DbmRollbackCommand()
        rollbackCommand.applicationContext = applicationContext
        rollbackCommand.commandLine = new CommandLineParser().parse([tagName] as String[])
        rollbackCommand.args.add(tagName)
        rollbackCommand.handle()
    }

    /**
     * Synchronizes the DATABASECHANGELOG table with the changelog changesets
     */
    def syncChangeLog(File migrationsDir){
        def cfg = new ConfigObject()
        cfg.grails.plugin.databasemigration.changelogLocation = migrationsDir.absolutePath
        createChangeLogFiles(migrationsDir)
        def changelogs = []
        changelogs << grailsApplication.config.getProperty("grails.plugin.databasemigration.changelog", String)
        changelogs.addAll(grailsApplication.config.getProperty("rundeck.plugins.databasemigration.changelogFiles").split(','))
        def classLoaderResourceAccessor = new ClassLoaderResourceAccessor()
        changelogs.each {changelog->
            if(classLoaderResourceAccessor.getResourcesAsStream(changelog) != null) {
                cfg.grails.plugin.databasemigration.changelogFileName = changelog
                grailsApplication.config.merge(cfg)
                DbmChangelogSyncCommand changelogSyncCommand = new DbmChangelogSyncCommand();
                changelogSyncCommand.applicationContext = applicationContext
                changelogSyncCommand.handle()
            }
        }
    }

    /**
     * Writes the change log file to the migrations directory if it exists in the classpath
     */
    boolean writeChangelog(ClassLoaderResourceAccessor classLoaderResourceAccessor, File migrationsDir, String changelogName){
        def inputStream = null
        boolean exists = false
        File changeLogFile = new File(migrationsDir, changelogName)
        try {
            inputStream = StreamUtil.singleInputStream(changelogName, classLoaderResourceAccessor)
            if(inputStream != null) {
                changeLogFile.createNewFile()
                changeLogFile << inputStream?.text
                exists = true
            }
        } finally {
            IOUtils.closeQuietly(inputStream)
        }
        return exists
    }
}