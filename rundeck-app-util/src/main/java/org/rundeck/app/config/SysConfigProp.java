package org.rundeck.app.config;

import java.util.Collections;
import java.util.List;

/**
 * Defines a system configuration property
 */
public interface SysConfigProp {
    /**
     *
     * @return full key name
     */
    String getKey();

    /**
     *
     * @return GUI visibility option
     */
    String getVisibility();

    /**
     *
     * @return GUI category
     */
    String getCategory();

    /**
     *
     * @return
     */
    String getGroup();

    /**
     *
     * @return storage strata
     */
    String getStrata();

    /**
     *
     * @return true if required
     */
    boolean isRequired();

    /**
     *
     * @return true if a restart is required to take effect
     */
    boolean isRestart();

    /**
     *
     * @return GUI display label
     */
    String getLabel();

    /**
     *
     * @return i18n code for label
     */
    String getLabelCode();

    /**
     *
     * @return value datatype
     */
    String getDatatype();

    /**
     *
     * @return true if value is encrypted
     */
    boolean isEncrypted();

    /**
     *
     * @return default value
     */
    String getDefaultValue();

    /**
     *
     * @return documentation URL
     */
    String getLink();

    /**
     *
     * @return GUI description
     */
    String getDescription();

    /**
     *
     * @return i18n code for description
     */
    String getDescriptionCode();

    /**
     *
     * @return authorization level required, ops_admin, app_admin, admin
     */
    String getAuthRequired();

    /**
     * @return allowed values for select-type props; empty list means free-form input
     */
    default List<String> getValues() {
        return Collections.emptyList();
    }

    /**
     * @param prefix prefix string
     * @return key removing the prefix
     * @throws IllegalArgumentException if the key does not have the expected prefix
     */
    default String subKey(String prefix) {
        if (!getKey().startsWith(prefix)) {
            throw new IllegalArgumentException("Config key expected to start with '" + prefix + "'");
        }
        return getKey().substring(prefix.length());
    }
}
