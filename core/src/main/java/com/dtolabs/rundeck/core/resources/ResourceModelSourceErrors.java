package com.dtolabs.rundeck.core.resources;

import java.util.List;

/**
 * Optional Extension to {@link ResourceModelSource} to provide out of band error messages, even if
 * the source can provide some results.
 */
public interface ResourceModelSourceErrors {
    List<String> getModelSourceErrors();
}
