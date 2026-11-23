/*
 * Copyright 2026 Rundeck, Inc. (http://rundeck.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package rundeckapp.init

import groovy.transform.CompileStatic
import org.springframework.context.HierarchicalMessageSource
import org.springframework.context.MessageSource
import org.springframework.context.MessageSourceResolvable
import org.springframework.context.NoSuchMessageException

import java.text.MessageFormat

/**
 * Resolves administrator, canonical, and legacy bundles in locale-safe order.
 */
@CompileStatic
final class LocaleAwareTranslationMessageSource implements HierarchicalMessageSource {
    private final List<MessageSource> resolutionOrder
    private MessageSource parentMessageSource

    /**
     * Creates the Core default translation source.
     *
     * @param administratorBasename external administrator override basename
     * @param canonicalBasename canonical public basename
     * @param legacyBasename retained legacy basename
     */
    LocaleAwareTranslationMessageSource(
            String administratorBasename,
            String canonicalBasename,
            String legacyBasename
    ) {
        resolutionOrder = [
                source(administratorBasename, false),
                source(administratorBasename, true),
                source(canonicalBasename, false),
                source(legacyBasename, false),
                source(canonicalBasename, true),
                source(legacyBasename, true)
        ]
    }

    /**
     * Returns the first value in the configured per-key order.
     */
    @Override
    String getMessage(String code, Object[] args, String defaultMessage, Locale locale) {
        String message = resolve(code, args, locale)
        if (message != null) {
            return message
        }
        String parentMessage = parentMessageSource?.getMessage(code, args, null, locale)
        parentMessage != null ? parentMessage : format(defaultMessage, args, locale)
    }

    /**
     * Returns a value or throws when no source contains the code.
     */
    @Override
    String getMessage(String code, Object[] args, Locale locale) throws NoSuchMessageException {
        String message = getMessage(code, args, null, locale)
        if (message == null) {
            throw new NoSuchMessageException(code, locale)
        }
        message
    }

    /**
     * Resolves all codes supplied by a MessageSourceResolvable.
     */
    @Override
    String getMessage(MessageSourceResolvable resolvable, Locale locale)
            throws NoSuchMessageException {
        for (String code : resolvable.codes ?: new String[0]) {
            String message = getMessage(code, resolvable.arguments, null, locale)
            if (message != null) {
                return message
            }
        }
        if (resolvable.defaultMessage != null) {
            return format(resolvable.defaultMessage, resolvable.arguments, locale)
        }
        String[] codes = resolvable.codes ?: new String[0]
        throw new NoSuchMessageException(codes.length > 0 ? codes[0] : '', locale)
    }

    /**
     * Sets the source consulted after the Core family.
     */
    @Override
    void setParentMessageSource(MessageSource parent) {
        parentMessageSource = parent
    }

    /**
     * Returns the source consulted after the Core family.
     */
    @Override
    MessageSource getParentMessageSource() {
        parentMessageSource
    }

    private String resolve(String code, Object[] args, Locale locale) {
        for (MessageSource source : resolutionOrder) {
            String message = source.getMessage(code, args, null, locale)
            if (message != null) {
                return message
            }
        }
        null
    }

    private static MessageSource source(String basename, boolean defaultOnly) {
        ExactLocaleResourceBundleMessageSource source =
                new ExactLocaleResourceBundleMessageSource(defaultOnly)
        source.setBasename(basename)
        source
    }

    private static String format(String message, Object[] args, Locale locale) {
        message == null ? null : (args ? new MessageFormat(message, locale).format(args) : message)
    }
}
