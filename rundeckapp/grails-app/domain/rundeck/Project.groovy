package rundeck

/**
 * Created by greg on 2/19/15.
 */
class Project {
    String name
    String description
    Date dateCreated
    Date lastUpdated

    static constraints={
        name(matches: '^[a-zA-Z0-9\\.,@\\(\\)_\\\\/-]+$',unique: true)
        description(nullable:true, matches: '^[a-zA-Z0-9\\s\\.,\\(\\)-]+$')
    }
}
