package com.dtolabs.rundeck.plugins.jobs;

import com.dtolabs.rundeck.core.jobs.options.JobOptionConfigData;
import com.dtolabs.rundeck.core.jobs.JobOption;
import com.dtolabs.rundeck.core.plugins.configuration.ValidationException;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

@Data
@Builder
public class JobOptionImpl implements JobOption, Comparable {
    static final String DEFAULT_DELIMITER = ",";
    @NonNull
    private String name;
    private Integer sortIndex;
    private String description;
    private String defaultValue;
    private String defaultStoragePath;
    @NonNull @Builder.Default
    private Boolean enforced = false;
    @Builder.Default
    private Boolean required = false;
    @Builder.Default
    private Boolean isDate = false;
    private String dateFormat;
    private TreeSet<String> values;
    private URL realValuesUrl;
    private String label;
    private String regex;
    private String valuesList;
    private String valuesListDelimiter;
    @Builder.Default
    private Boolean multivalued = false;
    private String delimiter;
    @Builder.Default
    private Boolean secureInput = false;
    @Builder.Default
    private Boolean secureExposed = false;
    private String optionType;
    @Builder.Default
    private JobOptionConfigData configData;
    @Builder.Default
    private Boolean multivalueAllSelected = false;
    private String optionValuesPluginType;
    @Builder.Default
    private Boolean hidden = false;
    @Builder.Default
    private Boolean sortValues = false;
    private List<String> optionValues;


    public static JobOptionImpl fromOptionMap(LinkedHashMap option) throws ValidationException {
        JobOptionImplBuilder builder = JobOptionImpl.builder();
        builder.name((String) option.get("name"));
        builder.label(option.containsKey("label") ? (String) option.get("label") : null);
        builder.enforced(option.containsKey("enforced") && (boolean) option.get("enforced"));
        builder.required(option.containsKey("required") && (boolean) option.get("required"));
        boolean isDate = option.containsKey("isDate") && (boolean) option.get("isDate");
        builder.isDate(isDate);
        if (isDate) {
            builder.dateFormat((String) option.get("dateFormat"));
        }
        if (option.containsKey("type")) {
            builder.optionType((String) option.get("type"));
        }
        if(option.containsKey("description")){
            builder.description((String) option.get("description"));
        }
        if(option.containsKey("sortIndex")){
            builder.sortIndex((Integer) option.get("sortIndex"));
        }
        if(option.containsKey("value")){
            builder.defaultValue((String) option.get("value"));
        }
        if(option.containsKey("storagePath")){
            builder.defaultStoragePath((String) option.get("storagePath"));
        }
        if(option.containsKey("valuesUrl")){
            try {
                builder.realValuesUrl(new URL((String) option.get("valuesUrl")));
            } catch (MalformedURLException e) {
                throw new ValidationException(e.getMessage());
            }
        }
        if(option.containsKey("regex")){
            builder.regex((String) option.get("regex"));
        }
        if(option.containsKey("values")){
            builder.values(
                    option.get("values") instanceof Collection
                    ? new TreeSet<>((Collection) option.get("values"))
                    : new TreeSet((SortedSet) option.get("values")));
            if(option.containsKey("valuesListDelimiter")){
                builder.valuesListDelimiter = (String) option.get("valuesListDelimiter");
            }else{
                builder.valuesListDelimiter = DEFAULT_DELIMITER;
            }
            builder.valuesList = produceValuesList(builder);
            builder.values = null;
        }
        if(option.containsKey("multivalued")){
            Boolean multivalued = (Boolean) option.get("multivalued");
            builder.multivalued(multivalued);
            if (multivalued) {
                if(option.containsKey("delimiter")){
                    builder.delimiter((String) option.get("delimiter"));
                }
                if(option.containsKey("multivalueAllSelected")){
                    builder.multivalueAllSelected((Boolean) option.get("multivalueAllSelected"));
                }
            }
        }
        builder.secureExposed(false);
        builder.secureInput(false);
        if (option.containsKey("secure") && (Boolean) option.get("secure")) {
            builder.secureInput(true);
            if (option.containsKey("valueExposed")) {
                builder.secureExposed((Boolean) option.get("valueExposed"));
            }
        }
        if(option.containsKey("optionValuesPluginType")) {
            builder.optionValuesPluginType = (String) option.get("optionValuesPluginType");
        }
        if(option.containsKey("hidden")){
            builder.hidden((Boolean) option.get("hidden"));
        }
        return builder.build();
    }


    static private String produceValuesList(JobOptionImplBuilder builder) {
        if (builder.values != null) {
            if (builder.valuesListDelimiter == null) {
                builder.valuesListDelimiter = DEFAULT_DELIMITER;
            }
            builder.valuesList = String.join(builder.valuesListDelimiter, builder.values);
            builder.values = null;
            return builder.valuesList;
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
