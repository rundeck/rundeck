package rundeck.data.job

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import grails.validation.Validateable
import org.rundeck.app.data.model.v1.job.option.OptionData
import org.rundeck.app.data.model.v1.job.option.OptionValueData
import rundeck.data.validation.shared.SharedJobOptionConstraints

@JsonIgnoreProperties(["errors"])
class RdOption implements OptionData, Comparable<OptionData>, Validateable {
    Long id;
    String name;
    Integer sortIndex;
    String description;
    String defaultValue;
    String defaultStoragePath;
    Boolean enforced;
    Boolean required;
    Boolean isDate;
    String dateFormat;
    String label;
    URL realValuesUrl;
    String regex;
    String valuesList;
    String valuesListDelimiter;
    Boolean multivalued;
    String delimiter;
    Boolean secureInput;
    Boolean secureExposed;
    String optionType;
    Map<String,Object> configMap;
    Boolean multivalueAllSelected;
    String optionValuesPluginType;
    List<RdOptionValue> valuesFromPlugin;
    Boolean hidden;
    Boolean sortValues;
    List<String> optionValues;

    static constraints={
        importFrom(SharedJobOptionConstraints)
        id(nullable: true)
        realValuesUrl(nullable: true)
    }

    @Override
    public int compareTo(OptionData obj) {
        if (null != sortIndex && null != obj.sortIndex) {
            return sortIndex.compareTo(obj.sortIndex);
        } else if (null == sortIndex && null == obj.sortIndex && name) {
            return name.compareTo(obj.name);
        } else {
            return sortIndex != null ? -1 : 1;
        }
    }

    static class RdOptionValue implements OptionValueData {
        String name;
        String value;
    }
}
