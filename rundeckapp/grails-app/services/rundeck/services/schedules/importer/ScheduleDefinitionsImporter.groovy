package rundeck.services.schedules.importer

import org.rundeck.core.projects.ProjectDataImporter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.Yaml

class ScheduleDefinitionsImporter implements ProjectDataImporter{
    private static final Logger logger = LoggerFactory.getLogger(ScheduleDefinitionsImporter)

    def schedulerService

    @Override
    String getSelector() {
        return "scheduleDefinitions"
    }

    @Override
    void doImport(String project, File importFile) {
        logger.info("Running Schedule Definitions import for project ${project}")

        Yaml yaml = new Yaml()
        if(importFile){
            def data = yaml.loadAs(new FileReader(importFile), HashMap.class)
            data.scheduleDefinitions.each { scheduleDefinition ->
                logger.debug("Attempting to import Schedule Definition: ${scheduleDefinition.name}")
                def result = schedulerService.persistScheduleDefFromMap(scheduleDefinition, project)
                if(result.errors && !result.errors.isEmpty){
                    logger.error(result.errors)
                }
            }
        }
    }
}
