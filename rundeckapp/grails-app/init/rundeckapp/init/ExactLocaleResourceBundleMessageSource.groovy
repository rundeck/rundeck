/*
 * Copyright 2026 Rundeck, Inc. (http://rundeck.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package rundeckapp.init

import groovy.transform.CompileStatic
import org.springframework.context.support.ReloadableResourceBundleMessageSource

/**
 * Restricts lookup to either localized files or the unsuffixed default file.
 */
@CompileStatic
final class ExactLocaleResourceBundleMessageSource extends ReloadableResourceBundleMessageSource {
    private final boolean defaultOnly

    /**
     * Creates one exact fallback stage.
     *
     * @param defaultOnly true to read only the unsuffixed bundle
     */
    ExactLocaleResourceBundleMessageSource(boolean defaultOnly) {
        this.defaultOnly = defaultOnly
        setDefaultEncoding('UTF-8')
        setFallbackToSystemLocale(false)
    }

    /**
     * Returns only filenames belonging to this fallback stage.
     */
    @Override
    protected List<String> calculateAllFilenames(String basename, Locale locale) {
        defaultOnly ? [basename] : calculateFilenamesForLocale(basename, locale)
    }
}
