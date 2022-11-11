package org.rundeck.app.data.model.v1.job.option;

import java.net.URL;
import java.util.List;

public interface OptionData {
    Integer getSortIndex();
    String getName();
    String getDescription();
    String getDefaultValue();
    String getDefaultStoragePath();
    Boolean getEnforced();
    Boolean getRequired();
    Boolean getIsDate();
    String getDateFormat();
    URL getValuesUrl();
    String getLabel();

    URL getValuesUrlLong();
    String getRegex();
    String getValuesList();
    String getValuesListDelimiter();
    Boolean getMultivalued();
    String getDelimiter();
    Boolean getSecureInput();
    Boolean getSecureExposed();
    String getOptionType();
    String getConfigData();
    Boolean getMultivalueAllSelected();
    String getOptionValuesPluginType();
    List<OptionValueData> getValuesFromPlugin();
    Boolean getHidden();
    Boolean getSortValues();
    List<String> getOptionValues();
}
