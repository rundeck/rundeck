package rundeck.options;

import com.dtolabs.rundeck.core.jobs.options.JobOptionConfigEntry;

public class JobOptionConfigRemoteUrl implements JobOptionConfigEntry {

    private RemoteUrlAuthenticationType authenticationType;
    private String username;

    private String passwordStoragePath;

    private String keyName;
    private String tokenStoragePath;

    public RemoteUrlAuthenticationType getAuthenticationType() {
        return authenticationType;
    }

    public void setAuthenticationType(RemoteUrlAuthenticationType authenticationType) {
        this.authenticationType = authenticationType;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordStoragePath() {
        return passwordStoragePath;
    }

    public void setPasswordStoragePath(String passwordStoragePath) {
        this.passwordStoragePath = passwordStoragePath;
    }

    public String getTokenStoragePath() {
        return tokenStoragePath;
    }

    public void setTokenStoragePath(String tokenStoragePath) {
        this.tokenStoragePath = tokenStoragePath;
    }

    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

}
