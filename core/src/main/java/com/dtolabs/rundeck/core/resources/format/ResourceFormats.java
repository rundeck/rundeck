package com.dtolabs.rundeck.core.resources.format;

import org.rundeck.app.spi.AppService;

/**
 * Provides service level access to Rundeck Resource Formats
 */
public interface ResourceFormats
        extends AppService
{
    /**
     * @param format format name
     * @return parser for resource format
     * @throws UnsupportedFormatException if format is not supported
     */
    ResourceFormatParser getResourceFormatParser(String format) throws UnsupportedFormatException;

    /**
     * @param format format name
     * @return generator for resource format
     * @throws UnsupportedFormatException if format is not supported
     */
    ResourceFormatGenerator getResourceFormatGenerator(String format) throws UnsupportedFormatException;
}
