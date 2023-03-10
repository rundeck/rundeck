package com.dtolabs.rundeck.core.jobs;

import com.dtolabs.rundeck.core.jobs.options.JobOptionConfigData;

import java.net.URL;
import java.util.List;
import java.util.SortedSet;

/**
 * Models a single Option configuration of a job
 */
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
        return false;
    }

    default Boolean getIsDate() {
        return null;
    }

    default String getDateFormat() {
        return null;
    }

    default SortedSet<String> getValues() {
        return null;
    }

    default URL getRealValuesUrl() {
        return null;
    }

    default String getLabel() {
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

    default JobOptionConfigData getConfigData() {
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
