import com.dtolabs.rundeck.plugins.notification.NotificationPlugin;

/**
 * This example is a minimal Notification plugin for Rundeck
 */
rundeckPlugin(NotificationPlugin){
    onstart { 
        println("job start: data ${execution}")
        true
    }

    onfailure {
        println("failure: data ${execution}")
        true
    }

    onsuccess {
        println("success: data ${execution}")
        true
    }
}
