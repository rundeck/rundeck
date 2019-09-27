package com.dtolabs.rundeck.plugins.jobs;

import com.dtolabs.rundeck.core.jobs.JobOption;
import com.dtolabs.rundeck.core.plugins.configuration.ValidationException;
import lombok.Data;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

@Data
public class JobOptionImpl implements JobOption, Comparable {
    static final String DEFAULT_DELIMITER = ",";
    private String name;
    private Integer sortIndex;
    private String description;
    private String defaultValue;
    private String defaultStoragePath;
    private Boolean enforced;
    private Boolean required;
    private Boolean isDate;
    private String dateFormat;
    private TreeSet values;
    private URL realValuesUrl;
    private String label;
    private String regex;
    private String valuesList;
    private String valuesListDelimiter;
    private Boolean multivalued;
    private String delimiter;
    private Boolean secureInput;
    private Boolean secureExposed;
    private String optionType;
    private String configData;
    private Boolean multivalueAllSelected;
    private String optionValuesPluginType;
    private Boolean hidden;
    private Boolean sortValues;
    private List<String> optionValues;

    public JobOptionImpl(){}

    public JobOptionImpl(LinkedHashMap option) throws ValidationException{
        this.name = (String) option.get("name");
        this.label = option.containsKey("label") ? (String) option.get("label") : null;
        this.enforced = option.containsKey("enforced") && (boolean) option.get("enforced");
        this.required = option.containsKey("required") && (boolean) option.get("required");
        this.isDate = option.containsKey("isDate") && (boolean) option.get("isDate");
        if(this.isDate){
            this.dateFormat = (String) option.get("dateFormat");
        }
        if (option.containsKey("type")) {
            this.optionType = (String) option.get("type");
        }
        if(option.containsKey("description")){
            this.description = (String) option.get("description");
        }
        if(option.containsKey("sortIndex")){
            this.sortIndex = (Integer) option.get("sortIndex");
        }
        if(option.containsKey("value")){
            this.defaultValue = (String) option.get("value");
        }
        if(option.containsKey("storagePath")){
            this.defaultStoragePath = (String) option.get("storagePath");
        }
        if(option.containsKey("valuesUrl")){
            try {
                this.realValuesUrl = new URL((String)option.get("valuesUrl"));
            } catch (MalformedURLException e) {
                throw new ValidationException(e.getMessage());
            }
        }
        if(option.containsKey("regex")){
            this.regex = (String) option.get("regex");
        }
        if(option.containsKey("values")){
            this.values = option.get("values") instanceof Collection ? new TreeSet<>((Collection)option.get("values")):new TreeSet((SortedSet)option.get("values"));
            if(option.containsKey("valuesListDelimiter")){
                this.valuesListDelimiter = (String) option.get("valuesListDelimiter");
            }else{
                this.valuesListDelimiter = DEFAULT_DELIMITER;
            }
            this.valuesList = produceValuesList();
            this.values = null;
        }
        if(option.containsKey("multivalued")){
            this.multivalued = (Boolean) option.get("multivalued");
            if(this.multivalued){
                if(option.containsKey("delimiter")){
                    this.delimiter = (String) option.get("delimiter");
                }
                if(option.containsKey("multivalueAllSelected")){
                    this.multivalueAllSelected = (Boolean) option.get("multivalueAllSelected");
                }
            }
        }
        if(option.containsKey("secure")){
            this.secureInput = (Boolean) option.get("secure");
        }else{
            this.secureInput = false;
        }
        if(this.secureInput && option.containsKey("valueExposed")){
            this.secureExposed = (Boolean) option.get("valueExposed");
        }else{
            this.secureExposed = false;
        }
        if(option.containsKey("optionValuesPluginType")) {
            this.optionValuesPluginType = (String) option.get("optionValuesPluginType");
        }
        if(option.containsKey("hidden")){
            this.hidden = (Boolean) option.get("hidden");
        }
    }


    private String produceValuesList(){
        if(this.values != null){
            if(this.valuesListDelimiter == null){
                this.valuesListDelimiter = DEFAULT_DELIMITER;
            }
            this.valuesList = String.join(this.valuesListDelimiter,this.values);
            this.values = null;
            return this.valuesList;
        }else{
            return "";
        }
    }

    @Override
    public int compareTo(Object o) {
        JobOptionImpl option = (JobOptionImpl)o;
        if (null != sortIndex && null != option.sortIndex) {
            return sortIndex.compareTo(option.sortIndex);
        } else if (null == sortIndex && null == option.sortIndex
            && name != null) {
            return name.compareTo(option.name);
        } else {
            return sortIndex != null ? -1 : 1;
        }
    }
}
