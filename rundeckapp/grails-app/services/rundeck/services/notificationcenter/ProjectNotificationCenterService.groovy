package rundeck.services.notificationcenter

import grails.converters.JSON
import grails.events.EventPublisher
import grails.gorm.transactions.Transactional
import groovy.json.JsonSlurper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.transaction.annotation.Propagation
import rundeck.services.FrameworkService

import java.nio.charset.StandardCharsets

class ProjectNotificationCenterService implements EventPublisher{

    static Logger logger = LoggerFactory.getLogger(this.class)

    private static final String NOTIFICATION_STORED_RESOURCE_PREFIX = "NC_"
    private static final String NOTIFICATION_STORED_RESOURCE_EXT = ".JSON"
    private static final String NOTIFICATIONS_LIST_FIRST_ID = "0"
    private static final int STORAGE_INCREMENTAL_UNIT = 1


    FrameworkService frameworkService

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    boolean initNotificationsForProject(final String projectName){
        try{
            List<ProjectNotificationCenterEntry> entries = new ArrayList<ProjectNotificationCenterEntry>()
            def empty = entries as JSON
            def inputStream = new ByteArrayInputStream(empty.toString().getBytes(StandardCharsets.UTF_8));
            final def fwkProject = frameworkService.getFrameworkProject(projectName)
            final def filename = NOTIFICATION_STORED_RESOURCE_PREFIX + projectName + NOTIFICATION_STORED_RESOURCE_EXT
            fwkProject.storeFileResource(filename, inputStream)
            inputStream.close();
            return true
        }catch(Exception e){
            throw new NotificationCenterException("Error initializing notifications for project: ${projectName} in db.", e)
        }
    }

    List<ProjectNotificationCenterEntry> getNotificationsEntriesForProject(final String projectName){
        try(def out = new ByteArrayOutputStream()){
            final def fwkProject = frameworkService.getFrameworkProject(projectName)
            fwkProject.loadFileResource(
                    NOTIFICATION_STORED_RESOURCE_PREFIX +
                            projectName +
                            NOTIFICATION_STORED_RESOURCE_EXT,
                    out)
            List<ProjectNotificationCenterEntry> entries = new JsonSlurper().parseText(out.toString()) as ArrayList<ProjectNotificationCenterEntry>
            entries.sort {it.id}
            logger.debug("Notifications for project: ${projectName}: ${entries.toString()}")
            return entries
        }catch(Exception e){
            throw new NotificationCenterException("Error getting notifications for project: ${projectName} from db.", e)
        }
    }

    boolean entriesExistForProject(String projectName){
        try{
            final def fwkProject = frameworkService.getFrameworkProject(projectName)
            def entriesFilepath = "${NOTIFICATION_STORED_RESOURCE_PREFIX}${projectName}${NOTIFICATION_STORED_RESOURCE_EXT}"
            if( !fwkProject.existsFileResource(entriesFilepath) ){
                return false
            }
            return true
        }catch(Exception e){
            throw new NotificationCenterException("Error getting notifications for project: ${projectName} from db.", e)
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    String saveNotificationCenterEntry(
            String projectName,
            ProjectNotificationCenterEntry newEntry) {
        def newId = null
        if( !entriesExistForProject(projectName) ){
            throw new NotificationCenterException("Notification Center not initialized for project: ${projectName}.")
        }
        def existingNotificationsForProject = getNotificationsEntriesForProject(projectName)
        try {
            newId = appendNewEntryAndSave(
                    projectName,
                    existingNotificationsForProject,
                    newEntry
            )
        } catch (Exception e) {
            throw new NotificationCenterException("Error saving new notification entry in db.", e)
        }
        return newId
    }

    private String appendNewEntryAndSave(
            String projectName,
            List<ProjectNotificationCenterEntry> oldEntries,
            ProjectNotificationCenterEntry newEntry
    ) {
        String newId = ""

        if( oldEntries.isEmpty() ){ // When its new (0 notifications)
            oldEntries = new ArrayList<ProjectNotificationCenterEntry>()
            newId = NOTIFICATIONS_LIST_FIRST_ID
        }else{
            def lastId = oldEntries[-1]?.id
            newId = String.valueOf(Integer.valueOf(lastId) + STORAGE_INCREMENTAL_UNIT)
        }

        // Give the new entry an id
        newEntry.id = newId
        oldEntries.push(newEntry)

        // persistence
        def jsonEntry = oldEntries as JSON
        def inputStream = new ByteArrayInputStream(jsonEntry.toString().getBytes(StandardCharsets.UTF_8))
        final def fwkProject = frameworkService.getFrameworkProject(projectName)
        final def filename = NOTIFICATION_STORED_RESOURCE_PREFIX + projectName + NOTIFICATION_STORED_RESOURCE_EXT
        fwkProject.storeFileResource(filename, inputStream)
        inputStream.close()

        return newId
    }

}
