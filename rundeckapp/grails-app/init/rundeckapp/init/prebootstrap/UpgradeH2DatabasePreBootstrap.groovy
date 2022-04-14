package rundeckapp.init.prebootstrap

import com.dtolabs.rundeck.core.properties.CoreConfigurationPropertiesLoader
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import groovy.sql.Sql
import org.rundeck.app.bootstrap.PreBootstrap
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import rundeckapp.init.DefaultRundeckConfigPropertyLoader

import java.lang.reflect.Method
import java.nio.file.Files
import java.sql.Connection
import java.sql.Driver
import java.sql.Statement
import java.util.concurrent.TimeUnit

class UpgradeH2DatabasePreBootstrap implements PreBootstrap{
    static Logger logger = LoggerFactory.getLogger(UpgradeH2DatabasePreBootstrap.class)

    String libPath
    String dbPath
    Boolean enabled
    File db = new File("${dbPath}.mv.db")
    File traceDb = new File("${dbPath}.trace.db")
    File dbBackup = new File("${dbPath}.mv.db.backup")
    File traceDbBackup = new File("${dbPath}.trace.db.backup")

    @Override
    void run() {
        enabled = Boolean.parseBoolean(System.getenv("RUNDECK_H2_UPGRADE_ENABLE"))
        def loadedProperties = loadConfigProperties()
        String dbUrl = loadedProperties.getProperty("dataSource.url")

        if(!enabled || !dbUrl.startsWith("jdbc:h2")) return

        logger.warn("H2 Database Upgrade enabled. The database will be upgraded to v2.x version")

        libPath = System.getenv("RUNDECK_H2_UPGRADE_LIB_PATH") ?: System.getProperty('rdeck.base') + "/server/lib/h2-1.4.200.jar"

        dbPath = (dbUrl =~ /(?<=jdbc\:h2\:file\:)(.*?)(?=;)/)[0][0]
        db = new File("${dbPath}.mv.db")
        traceDb = new File("${dbPath}.trace.db")
        dbBackup = new File("${dbPath}.mv.db.backup")
        traceDbBackup = new File("${dbPath}.trace.db.backup")

        if(exportScriptOldDatabase()){
            if(!backupDatabase()) {
                logger.error("Error to backup database")
                return
            }
            if(deleteOldDatabase()){
                importToNewDatabaseVersion("./backup.sql")
            }
        }
    }

    @Override
    float getOrder() {
        return 4
    }

    private boolean backupDatabase(){
        logger.info("Creating a backup of H2 database ${dbBackup.getName()}")

        if(dbBackup.exists()) {
            if(db.exists()) {
                dbBackup.delete()
            } else {
                logger.warn("There is a backup file but the original database does not exist. Please verify that the database has been removed and, if so, restore it before proceeding.")
                return false
            }
        }
        if(traceDbBackup.exists()) {
            if(traceDb.exists()) {
                traceDbBackup.delete()
            } else {
                logger.warn("There is a backup file but the original database does not exist. Please verify that the database has been removed and, if so, restore it before proceeding.")
                return false
            }
        }

        Files.copy(db.toPath(), dbBackup.toPath())
        Files.copy(traceDb.toPath(), traceDbBackup.toPath())

        logger.info("Backup created")

        return dbBackup.exists() && traceDbBackup.exists()
    }

    private Properties loadConfigProperties(){
        CoreConfigurationPropertiesLoader rundeckConfigPropertyFileLoader = new DefaultRundeckConfigPropertyLoader()
        ServiceLoader<CoreConfigurationPropertiesLoader> rundeckPropertyLoaders = ServiceLoader.load(
                CoreConfigurationPropertiesLoader
        )
        rundeckPropertyLoaders.each { loader ->
            rundeckConfigPropertyFileLoader = loader
        }
        rundeckConfigPropertyFileLoader.loadProperties()
    }

    private void importToNewDatabaseVersion(String scriptFileName){
        logger.info("Importing data from script file")

        Map cfg = [:]
        def loadedProperties = loadConfigProperties()
        cfg.url = loadedProperties.getProperty("dataSource.url")
        cfg.driverClassName = loadedProperties.
                getProperty("dataSource.driverClassName", "org.h2.Driver")
        cfg.userName = loadedProperties.getProperty("dataSource.username", "sa")
        cfg.pwd = loadedProperties.getProperty("dataSource.password", "")
        HikariDataSource hds
        try {

            hds = new HikariDataSource(new HikariConfig(jdbcUrl: cfg.url, username: cfg.userName,
                    password: cfg.pwd, driverClassName: cfg.driverClassName, maxPoolSize: 2, idleTimeout: 10000L,
                    initializationFailTimeout: TimeUnit.SECONDS.toMillis(120)))

            logger.info("Importing data by SQL command: RUNSCRIPT FROM '" + scriptFileName + "' FROM_1X")
            Sql sql = new Sql(hds)
            sql.execute("RUNSCRIPT FROM '" + scriptFileName + "' FROM_1X")
        } catch (Exception e){
            logger.error("Error to import from script file: " + e.getMessage())
        } finally {
            logger.info("Database imported succesfully")
            if(hds) {
                hds.close()
            }
        }
    }

    private boolean exportScriptOldDatabase(){
        logger.info("Exporting database to a script file")

        boolean result = false
        try {

            def cmd = ['/bin/sh', '-c', "java -cp " + libPath + " org.h2.tools.Script -url 'jdbc:h2:file:" + dbPath + ";DB_CLOSE_ON_EXIT=FALSE' -user 'sa' -password ''"]

            cmd.execute().with {
                def output = new StringWriter()
                def error = new StringWriter()

                it.waitForProcessOutput(output, error)

                println "error=$error"
                println "output=$output"
                println "code=${it.exitValue()}"
            }

            result = true
        } catch (Exception e) {
            logger.error("ERROR: " + e.getMessage())
            return result
        }

        logger.info("Script file 'backup.sql' was created")
        return result
    }

    private boolean deleteOldDatabase(){
        boolean filesDeleted = false
        if(dbBackup.exists()) {
            filesDeleted = db.delete()
        }
        if(traceDbBackup.exists()) {
            filesDeleted = traceDb.delete()
        }

        return filesDeleted
    }
}
