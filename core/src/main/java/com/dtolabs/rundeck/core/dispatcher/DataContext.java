package com.dtolabs.rundeck.core.dispatcher;

import com.dtolabs.rundeck.core.utils.Converter;

import java.util.Map;

/**
 * Created by greg on 5/25/16.
 */
public interface DataContext extends Map<String, Map<String, String>>, Mergable<DataContext>{
    Map<String,Map<String,String>> getData();

    default Converter<String, String> replaceDataReferencesConverter() {
        return DataContextUtils.replaceDataReferencesConverter(getData());
    }


    /**
     * Return a converter that can expand the property references within a string
     *
     * @param converter        secondary converter to apply to property values before replacing in a string
     * @param failOnUnexpanded if true, fail if a property value cannot be expanded
     *
     * @return a Converter to expand property values within a string
     */
    default Converter<String, String> replaceDataReferencesConverter(
            final Converter<String, String> converter,
            final boolean failOnUnexpanded
    )
    {
        return DataContextUtils.replaceDataReferencesConverter(getData(), converter, failOnUnexpanded);
    }


    default String[] replaceDataReferences(
            final String[] args,
            final Converter<String, String> converter,
            boolean failIfUnexpanded
    )
    {
        return replaceDataReferences(args, converter, failIfUnexpanded, false);
    }


    /**
     * Replace the embedded  properties of the form '${key.name}' in the input Strings with the value from the data
     * context
     *
     * @param args              argument string array
     * @param converter         converter
     * @param failIfUnexpanded  true to fail if property is not found
     * @param blankIfUnexpanded true to use blank if property is not found
     *
     * @return string array with replaced embedded properties
     */
    default String[] replaceDataReferences(
            final String[] args,
            Converter<String, String> converter,
            boolean failIfUnexpanded,
            boolean blankIfUnexpanded
    )
    {
        return DataContextUtils.replaceDataReferencesInArray(args, getData(), converter, failIfUnexpanded, blankIfUnexpanded);
    }

    /**
     * Replace the embedded  properties of the form '${key.name}' in the input Strings with the value from the data
     * context
     *
     * @param args argument string array
     *
     * @return string array with replaced embedded properties
     */
    default String[] replaceDataReferences(final String[] args) {
        return replaceDataReferences(args, null, false);
    }

    /**
     * Recursively replace data references in the values in a map which contains either string, collection or Map
     * values.
     *
     * @param input input map
     *
     * @return Map with all string values having references replaced
     */
    default Map<String, Object> replaceDataReferences(final Map<String, Object> input) {
        return DataContextUtils.replaceDataReferences(input, getData());
    }

    default String resolve(
            final String group,
            final String key
    )
    {
        return resolve(group, key, null);
    }

    /**
     * Return the resolved value from the context
     *
     * @param group        group name
     * @param key          key name
     * @param defaultValue default if the value is not resolvable
     *
     * @return resolved value or default
     */
    default String resolve(
            final String group,
            final String key,
            final String defaultValue
    )
    {
        Map<String, Map<String, String>> data = getData();
        return null != data && null != data.get(group) && null != data.get(group).get(key)
               ? data.get(group).get(key)
               : defaultValue;
    }

    /**
     * Replace the embedded  properties of the form '${key.name}' in the input Strings with the value from the data
     * context
     *
     * @param input input string
     *
     * @return string with values substituted, or original string
     */
    default String replaceDataReferences(final String input) {
        return DataContextUtils.replaceDataReferencesInString(input, getData());
    }

    /**
     * Replace the embedded  properties of the form '${key.name}' in the input Strings with the value from the data
     * context
     *
     * @param input            input string
     * @param converter        converter to encode/convert the expanded values
     * @param failOnUnexpanded true to fail if a reference is not found
     *
     * @return string with values substituted, or original string
     */
    default String replaceDataReferences(
            final String input,
            final Converter<String, String> converter,
            boolean failOnUnexpanded
    )
    {
        return replaceDataReferences(input, converter, failOnUnexpanded, false);
    }

    /**
     * Replace the embedded  properties of the form '${key.name}' in the input Strings with the value from the data
     * context
     *
     * @param input             input string
     * @param converter         converter to encode/convert the expanded values
     * @param failOnUnexpanded  true to fail if a reference is not found
     * @param blankIfUnexpanded true to use blank if a reference is not found
     *
     * @return string with values substituted, or original string
     */
    default String replaceDataReferences(
            final String input,
            final Converter<String, String> converter,
            boolean failOnUnexpanded,
            boolean blankIfUnexpanded
    )
    {
        return DataContextUtils.replaceDataReferencesInString(input, getData(), converter, failOnUnexpanded, blankIfUnexpanded);

    }

    /**
     *
     * @param other
     * @return new data context of this context merged with the other context
     */
    default DataContext merged(DataContext other) {
        return new BaseDataContext(DataContextUtils.merge(this, other));
    }
}
