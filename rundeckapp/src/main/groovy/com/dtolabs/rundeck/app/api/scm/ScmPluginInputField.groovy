/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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

package com.dtolabs.rundeck.app.api.scm

import com.dtolabs.rundeck.app.api.CDataString
import io.swagger.v3.oas.annotations.media.Schema


/**
 * Created by greg on 10/27/15.
 */
@Schema
class ScmPluginInputField {

    @Schema(description = 'display title for the field')
    /**
     * @return descriptive name of the property
     */
    String title

    @Schema(description='identifier for the field, used when submitting the input values')
    /**
     * @return property key to use
     */
    String name

    @Schema(description='textual description',type='string')
    /**
     * @return description of the values of the property
     */
    CDataString description

    @Schema(
        description='data type of the field: `String`, `Integer`, `Select` (multi-value), `FreeSelect` (open-ended multi-value), `Boolean` (true/false)',
        allowableValues = ['String','Integer','Select','FreeSelect','Boolean']
    )
    /**
     * @return the property type
     */
    String type;

    @Schema(description='whether the input is required')
    /**
     * @return true if an empty value is not allowed
     */
    boolean required;

    @Schema(description='a default value if the input does not specify one')
    /**
     * @return the default value of the property, or default select value to select
     */
    String defaultValue;

    @Schema(description='if the type is `Select` or `FreeSelect`, a list of string values to choose from')
    /**
     * @return a list of values for a select property
     */
    List<String> values;

    /**
     * @return the scope of this property, i.e. where the value can be retrieved and overridden, or null to indicate
     * the default scope.
     */
    String scope;

    @Schema(description='a key/value map of options, such as declaring that GUI display the input as a password field.')
    /**
     * @return a map of optional rendering options for the UI
     */
    Map<String, String> renderingOptions;
}
