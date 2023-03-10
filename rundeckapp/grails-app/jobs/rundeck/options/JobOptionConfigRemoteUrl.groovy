package rundeck.options;

import com.dtolabs.rundeck.core.jobs.options.JobOptionConfigEntry
import com.fasterxml.jackson.annotation.JsonIgnore;

class JobOptionConfigRemoteUrl implements JobOptionConfigEntry {

    RemoteUrlAuthenticationType authenticationType
    String username
    String passwordStoragePath
    String keyName
    String tokenStoragePath

    @JsonIgnore
    String password

    @JsonIgnore
    String token

    @JsonIgnore
    String errors
}
