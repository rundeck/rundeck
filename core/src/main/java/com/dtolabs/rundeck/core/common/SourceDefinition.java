package com.dtolabs.rundeck.core.common;

import java.util.Properties;

/**
 * A source definition
 */
public interface SourceDefinition {
    String getType();

    Properties getProperties();

    Properties getExtraProperties();


    String getIdent();

    int getIndex();
}
