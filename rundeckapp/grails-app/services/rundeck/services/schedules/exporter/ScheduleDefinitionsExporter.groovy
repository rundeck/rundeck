package rundeck.services.schedules.exporter

import org.rundeck.core.projects.ProjectDataExporter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.Yaml

class ScheduleDefinitionsExporter implements ProjectDataExporter {
    private static final Logger logger = LoggerFactory.getLogger(ScheduleDefinitionsExporter)

    def schedulerService

    @Override
    String getSelector() {
        return "scheduleDefinitions"
    }

    @Override
    void export(String project, Object zipBuilder) {
        logger.info("Project Schedule Definitions export running for project ${project}")
        Yaml yaml = new Yaml()
        def export = [scheduleDefinitions:[]]
        schedulerService.findAllByProject(project).each { sd ->
            logger.debug("exporting definition: ${sd.name}")
            export.scheduleDefinitions.add(sd.toMap())
        }
        zipBuilder.file("scheduleDefinitions.yaml") { writer ->
            yaml.dump(export,writer)
        }
    }
}
