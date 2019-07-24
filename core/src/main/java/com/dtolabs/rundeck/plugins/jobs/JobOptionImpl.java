package com.dtolabs.rundeck.plugins.jobs;

import com.dtolabs.rundeck.core.jobs.JobOption;
import com.dtolabs.rundeck.core.plugins.configuration.ValidationException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class JobOptionImpl implements JobOption, Comparable {

    static final String DEFAULT_DELIMITER = ",";
    String name;
    Integer sortIndex;
    String description;
    String defaultValue;
    String defaultStoragePath;
    Boolean enforced;
    Boolean required;
    Boolean isDate;
    String dateFormat;
    SortedSet<String> values;
    URL realValuesUrl;
    String label;
    String regex;
    String valuesList;
    String valuesListDelimiter;
    Boolean multivalued;
    String delimiter;
    Boolean secureInput;
    Boolean secureExposed;
    String optionType;
    String configData;
    Boolean multivalueAllSelected;
    String optionValuesPluginType;
    Boolean hidden;
    Boolean sortValues;
    List<String> optionValues;

    public JobOptionImpl(){}

    public JobOptionImpl(LinkedHashMap option) throws ValidationException{
        this.name = (String) option.get("name");
        this.label = option.containsKey("label") ? (String) option.get("label") : null;
        this.enforced =  option.containsKey("enforced") && (boolean) option.get("enforced")?true:false;
        this.required = option.containsKey("required") && (boolean) option.get("required")?true:false;
        this.isDate = option.containsKey("isDate") && (boolean) option.get("isDate")?true:false;
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

    public void setName(String name) {
        this.name = name;
    }

    public void setSortIndex(Integer sortIndex) {
        this.sortIndex = sortIndex;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public void setDefaultStoragePath(String defaultStoragePath) {
        this.defaultStoragePath = defaultStoragePath;
    }

    public void setEnforced(Boolean enforced) {
        this.enforced = enforced;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    public void setDate(Boolean date) {
        isDate = date;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    public void setValues(SortedSet<String> values) {
        this.values = values;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }

    public void setValuesList(String valuesList) {
        this.valuesList = valuesList;
    }

    public void setValuesListDelimiter(String valuesListDelimiter) {
        this.valuesListDelimiter = valuesListDelimiter;
    }

    public void setMultivalued(Boolean multivalued) {
        this.multivalued = multivalued;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    public void setSecureInput(Boolean secureInput) {
        this.secureInput = secureInput;
    }

    public void setSecureExposed(Boolean secureExposed) {
        this.secureExposed = secureExposed;
    }

    public void setOptionType(String optionType) {
        this.optionType = optionType;
    }

    public void setConfigData(String configData) {
        this.configData = configData;
    }

    public void setMultivalueAllSelected(Boolean multivalueAllSelected) {
        this.multivalueAllSelected = multivalueAllSelected;
    }

    public void setOptionValuesPluginType(String optionValuesPluginType) {
        this.optionValuesPluginType = optionValuesPluginType;
    }

    public void setHidden(Boolean hidden) {
        this.hidden = hidden;
    }

    public void setSortValues(Boolean sortValues) {
        this.sortValues = sortValues;
    }

    public void setOptionValues(List<String> optionValues) {
        this.optionValues = optionValues;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Integer getSortIndex() {
        return this.sortIndex;
    }

    @Override
    public String getDescription() { return this.description; }

    @Override
    public String getDefaultValue() {
        return this.defaultValue;
    }

    @Override
    public String getDefaultStoragePath() {
        return this.defaultStoragePath;
    }

    @Override
    public Boolean getEnforced() {
        return this.enforced;
    }

    @Override
    public Boolean getRequired() {
        return this.required;
    }

    @Override
    public Boolean getDate() {
        return this.isDate;
    }

    @Override
    public String getDateFormat() {
        return this.dateFormat;
    }

    @Override
    public SortedSet<String> getValues() {
        return this.values;
    }

    @Override
    public URL getValuesUrl() {
        return this.realValuesUrl;
    }

    @Override
    public String getLabel() {
        return this.label;
    }

    @Override
    public URL getValuesUrlLong() {
        return this.realValuesUrl;
    }

    @Override
    public String getRegex() {
        return this.regex;
    }

    @Override
    public String getValuesList() {
        return this.valuesList;
    }

    @Override
    public String getValuesListDelimiter() {
        return this.valuesListDelimiter;
    }

    @Override
    public Boolean getMultivalued() {
        return this.multivalued;
    }

    @Override
    public String getDelimiter() {
        return this.delimiter;
    }

    @Override
    public Boolean getSecureInput() {
        return this.secureInput;
    }

    @Override
    public Boolean getSecureExposed() {
        return this.secureExposed;
    }

    @Override
    public String getOptionType() {
        return this.optionType;
    }

    @Override
    public String getConfigData() {
        return this.configData;
    }

    @Override
    public Boolean getMultivalueAllSelected() {
        return this.multivalueAllSelected;
    }

    @Override
    public String getOptionValuesPluginType() {
        return this.optionValuesPluginType;
    }

    @Override
    public Boolean getHidden() {
        return this.hidden;
    }

    @Override
    public Boolean getSortValues() {
        return this.sortValues;
    }

    @Override
    public List<String> getOptionValues() {
        return this.optionValues;
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
        JobOptionImpl bla = (JobOptionImpl)o;
        if (null != sortIndex && null != bla.sortIndex) {
            return sortIndex.compareTo(bla.sortIndex);
        } else if (null == sortIndex && null == bla.sortIndex
            && name != null) {
            return name.compareTo(bla.name);
        } else {
            return sortIndex != null ? -1 : 1;
        }
    }
}
