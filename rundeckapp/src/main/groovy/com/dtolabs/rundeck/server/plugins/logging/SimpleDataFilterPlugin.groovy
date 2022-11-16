/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dtolabs.rundeck.server.plugins.logging

import com.dtolabs.rundeck.core.execution.workflow.OutputContext
import com.dtolabs.rundeck.core.logging.LogEventControl
import com.dtolabs.rundeck.core.logging.LogLevel
import com.dtolabs.rundeck.core.logging.PluginLoggingContext
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.plugins.configuration.PropertyValidator
import com.dtolabs.rundeck.core.plugins.configuration.ValidationException
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.descriptions.RenderingOption
import com.dtolabs.rundeck.plugins.descriptions.RenderingOptions
import com.dtolabs.rundeck.plugins.logging.LogFilterPlugin
import com.fasterxml.jackson.databind.ObjectMapper

import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

/**
 * @author greg
 * @since 5/17/17
 */
@Plugin(name = SimpleDataFilterPlugin.PROVIDER_NAME, service = 'LogFilter')
@PluginDescription(title = 'Key Value Data',
        description = '''Captures simple Key/Value data using a simple text format
from a regular expresssion.\n\n
By default, to produce a key/value entry, echo a line similar to this:

    RUNDECK:DATA:(key) = (value)

Where `(key)` is the key name, and `(value)` is the value.

If you provide a regular expression with only one group, the `name` input is required.

You can define the regular expression used.
''')

class SimpleDataFilterPlugin implements LogFilterPlugin {
    public static final String PROVIDER_NAME = 'key-value-data'
    public static final String PATTERN = '^RUNDECK:DATA:\\s*([^\\s]+?)\\s*=\\s*(.+)$'
    public static final String INVALID_KEY_PATTERN = '\\s|\\$|\\{|\\}|\\\\'
    public static final String INVALID_KEY_PATTERN_DEFAULT_REPLACE_VALUE = ''
    public static final String EXTRA_SETTINGS_GROUP_NAME = "Advanced"

    @PluginProperty(
            title = "Pattern",
            description = '''Regular Expression for matching key/value data.

The regular expression must define two Capturing Groups. The first group matched defines
the data key, and the second group defines the data value.

See the [Java Pattern](https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html) documentation.''',
            defaultValue = SimpleDataFilterPlugin.PATTERN,
            required = true,
            validatorClass = SimpleDataFilterPlugin.NamePropertyValidator
    )
    String regex

    @PluginProperty(
            title = 'Name Data',
            description = '''If only one groups is provided, the name of the captured variable'''
    )
    String name

    @PluginProperty(
            title = 'Log Data',
            description = '''If true, log the captured data''',
            defaultValue = 'false'
    )
    Boolean logData


    @PluginProperty(
            title = "Invalid Character Pattern",
            description = '''Regular expression pattern to match invalid characters in the Key. Any matched characters will be replaced by an underscore character. Default: white space and special characters.''',
            defaultValue = SimpleDataFilterPlugin.INVALID_KEY_PATTERN,
            required = false,
            validatorClass = SimpleDataFilterPlugin.RegexValidator

    )
    @RenderingOptions(
            [
                    @RenderingOption(key = "groupName", value = SimpleDataFilterPlugin.EXTRA_SETTINGS_GROUP_NAME),
                    @RenderingOption(key = "grouping", value = "secondary"),
                    @RenderingOption(key = "requiredValue", value = "false"),
            ]
    )
    String invalidKeyPattern

    @PluginProperty(
            title = 'Replace filtered data',
            description = '''If checked, the data will be replaced with a defined value below''',
            defaultValue = 'false'
    )
    @RenderingOptions(
            [
                    @RenderingOption(key = "groupName", value = SimpleDataFilterPlugin.EXTRA_SETTINGS_GROUP_NAME),
                    @RenderingOption(key = "grouping", value = "secondary"),
                    @RenderingOption(key = "requiredValue", value = "false"),
            ]
    )
    Boolean replaceFilteredResult

    @PluginProperty(
            title = "Replace Invalid Character Patterns With",
            description = '''If the Invalid Character Pattern matches, the string will be replaced with an underscore by default, unless you specify which value do you want to replace the invalid character pattern with.''',
            defaultValue = SimpleDataFilterPlugin.INVALID_KEY_PATTERN_DEFAULT_REPLACE_VALUE,
            required = false

    )
    @RenderingOptions(
            [
                    @RenderingOption(key = "groupName", value = SimpleDataFilterPlugin.EXTRA_SETTINGS_GROUP_NAME),
                    @RenderingOption(key = "grouping", value = "secondary"),
                    @RenderingOption(key = "requiredValue", value = "false"),
            ]
    )
    String invalidCharactersReplacement

    static class RegexValidator implements PropertyValidator {
        @Override
        boolean isValid(final String value) throws ValidationException {
            try {
                def compile = Pattern.compile(value)
                return true
            } catch (PatternSyntaxException e) {
                throw new ValidationException(e.message, e)
            }
        }
    }

    static class NamePropertyValidator implements PropertyValidator {

        @Override
        boolean isValid(String value) throws ValidationException {
            return false
        }

        @Override
        boolean isValid(String value, Map<String,Object> props) throws ValidationException {
            def compile

            try {
                compile = Pattern.compile(value)
            } catch (PatternSyntaxException e) {
                throw new ValidationException(e.message, e)
            }
            Matcher m = compile.matcher("");

            if(m.groupCount() == 0){
                throw new ValidationException("Pattern must have at least one group")
            }
            if(m.groupCount() == 1 && !props.containsKey("name")){
                throw new ValidationException("The Name field must be defined when only one capture group is specified")
            }
            return true
        }
    }

    Pattern dataPattern;
    OutputContext outputContext
    Map<String, String> allData
    private ObjectMapper mapper

    @Override
    void init(final PluginLoggingContext context) {
        dataPattern = Pattern.compile(regex)
        outputContext = context.getOutputContext()
        mapper = new ObjectMapper()
        allData = [:]
    }

    @Override
    void handleEvent(final PluginLoggingContext context, final LogEventControl event) {
        if (event.eventType == 'log' && event.loglevel == LogLevel.NORMAL && event.message?.length() > 0) {
            Matcher match = dataPattern.matcher(event.message)
            if (match.matches()) {
                def key,value
                if(match.groupCount()==1 && name){
                    key = name
                    value = match.group(1)
                }else {
                    key = match.group(1)
                    value = match.group(2)
                }
                if (key && value) {
                    if(invalidKeyPattern){
                        def validKey = null
                        if( replaceFilteredResult ){
                            if( emptyReplacement ){
                                def emptyReplacement = invalidCharactersReplacement == null
                                validKey = key.replaceAll(invalidKeyPattern, '')
                            }else{
                                validKey = key.replaceAll(invalidKeyPattern, invalidCharactersReplacement)
                            }
                        }else{
                            validKey = key.replaceAll(invalidKeyPattern,"_")
                        }
                        if (key != validKey) {
                            key = validKey
                            context.log(1,"Key contains not valid value which will be replaced")
                        }
                    }
                    allData[key] = value
                    outputContext.addOutput("data", key, value)
                }
            }
        }
    }

    @Override
    void complete(final PluginLoggingContext context) {
        if (allData) {
            if (logData) {
                context.log(
                        2,
                        mapper.writeValueAsString(allData),
                        [
                                'content-data-type'       : 'application/json',
                                'content-meta:table-title': 'Key Value Data: Results'
                        ]
                )
            }
        }
    }
}
