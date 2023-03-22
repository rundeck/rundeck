package rundeck.options;

enum RemoteUrlAuthenticationType {

    BASIC("BASIC"),
    API_KEY("API_KEY"),
    BEARER_TOKEN("BEARER_TOKEN");

    private final String name;

    RemoteUrlAuthenticationType(String name) {
        this.name = name;
    }

    String getName(){
        return name;
    }

}
