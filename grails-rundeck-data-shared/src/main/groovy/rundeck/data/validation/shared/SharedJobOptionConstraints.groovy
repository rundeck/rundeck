package rundeck.data.validation.shared

import grails.validation.Validateable

class SharedJobOptionConstraints implements Validateable {

    String name
    Integer sortIndex
    String description
    String defaultValue
    String defaultStoragePath
    Boolean enforced
    Boolean required
    Boolean isDate
    String dateFormat
    String label
    String regex
    String valuesList
    String valuesListDelimiter
    Boolean multivalued
    String delimiter
    Boolean secureInput
    Boolean secureExposed
    String optionType
    String configData
    Boolean multivalueAllSelected
    String optionValuesPluginType
    Boolean hidden
    Boolean sortValues

    static constraints = {
        name(nullable:false,blank:false,matches: '[a-zA-Z_0-9.-]+')
        description(nullable:true)
        defaultValue(nullable:true)
        defaultStoragePath(nullable:true,matches: '^(/?)keys/.+')
        enforced(nullable:false)
        required(nullable:true)
        isDate(nullable:true)
        dateFormat(nullable: true, maxSize: 30)
        regex(nullable:true)
        delimiter(nullable:true)
        multivalued(nullable:true)
        secureInput(nullable:true)
        secureExposed(nullable:true)
        sortIndex(nullable:true)
        optionType(nullable: true, maxSize: 255)
        configData(nullable: true)
        multivalueAllSelected(nullable: true)
        label(nullable: true)
        optionValuesPluginType(nullable: true)
        hidden(nullable: true)
        valuesList(nullable: true)
        valuesListDelimiter(nullable: true)
        sortValues(nullable: true)
    }
}
