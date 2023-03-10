package rundeck.options;

public enum RemoteUrlAuthenticationType {

    BASIC("basic"),
    API_KEY("apiKey"),
    BEARER_TOKEN("bearerToken");

    private final String name;

    RemoteUrlAuthenticationType(String name) {
        this.name = name;
    }

    String getName(){
        return name;
    }

}
