package org.rundeck.app.data.model.v1.job.option;

import java.util.List;
import java.util.SortedSet;

public interface Option {
    Integer getSortIndex();
    String getName();
    String getDescription();
    String getDefaultValue();
    String getDefaultStoragePath();
    Boolean isEnforced();
    Boolean isRequired();
    Boolean isDate();
    String getDateFormat();
    SortedSet<String> getValues();
    String getValuesUrl();
    String getLabel();

    String getValuesUrlLong();
    String getRegex();
    String getValuesList();
    String getValuesListDelimiter();
    Boolean isMultivalued();
    String getDelimiter();
    Boolean isSecureInput();
    Boolean isSecureExposed();
    String getOptionType();
    String getConfigData();
    Boolean isMultivalueAllSelected();
    String getOptionValuesPluginType();
    List<OptionValue> getValuesFromPlugin();
    Boolean isHidden();
    Boolean isSortValues();
    List<String> getOptionValues();
}
