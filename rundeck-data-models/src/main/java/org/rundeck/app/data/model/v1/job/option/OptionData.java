/*
 * Copyright 2024 Rundeck, Inc. (http://rundeck.com)
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
package org.rundeck.app.data.model.v1.job.option;

import java.net.URL;
import java.util.List;
import java.util.Map;

public interface OptionData {
    Integer getSortIndex();
    String getName();
    String getDescription();
    String getDefaultValue();
    String getDefaultStoragePath();
    Boolean getEnforced();
    Boolean getRequired();
    Boolean getIsDate();
    String getDateFormat();
    URL getRealValuesUrl();
    String getLabel();

    String getRegex();
    String getValuesList();
    String getValuesListDelimiter();
    Boolean getMultivalued();
    String getDelimiter();
    Boolean getSecureInput();
    Boolean getSecureExposed();
    String getOptionType();
    Map<String,Object> getConfigMap();
    Boolean getMultivalueAllSelected();
    String getOptionValuesPluginType();
    List<OptionValueData> getValuesFromPlugin();
    Boolean getHidden();
    Boolean getSortValues();
    List<String> getOptionValues();
}
