package com.dtolabs.rundeck.core.jobs;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

public interface JobOption {

    String getName();

    default Integer getSortIndex() {
        return null;
    }

    default String getDescription() {
        return null;
    }

    default String getDefaultValue() {
        return null;
    }

    default String getDefaultStoragePath() {
        return null;
    }

    default Boolean getEnforced() {
        return false;
    }

    default Boolean getRequired() {
        return null;
    }

    default Boolean getDate() {
        return null;
    }

    default String getDateFormat() {
        return null;
    }

    default SortedSet<String> getValues() {
        return null;
    }

    default URL getValuesUrl() {
        return null;
    }

    default String getLabel() {
        return null;
    }

    default URL getValuesUrlLong() {
        return null;
    }

    default String getRegex() {
        return null;
    }

    default String getValuesList() {
        return null;
    }

    default String getValuesListDelimiter() {
        return null;
    }

    default Boolean getMultivalued() {
        return null;
    }

    default String getDelimiter() {
        return null;
    }

    default Boolean getSecureInput() {
        return null;
    }

    default Boolean getSecureExposed() {
        return null;
    }

    default String getOptionType() {
        return null;
    }

    default String getConfigData() {
        return null;
    }

    default Boolean getMultivalueAllSelected() {
        return null;
    }

    default String getOptionValuesPluginType() {
        return null;
    }

    default Boolean getHidden() {
        return null;
    }

    default Boolean getSortValues() {
        return null;
    }

    default List<String> getOptionValues() {
        return null;
    }
}
