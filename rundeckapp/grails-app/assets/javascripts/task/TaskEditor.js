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


class TaskEditor {
    constructor(data) {
        const self = this;
        self.conditions = new PluginListEditor(
            Object.assign(
                {
                    formPrefixes  : data.conditionFormPrefixes,
                    inputPrefix   : data.conditionInputPrefix,
                    postLoadEditor: data.postLoadEditor,
                    service       : 'TaskCondition'
                },
                data.conditionData
            )
        );

        self.trigger = new PluginEditor(
            {
                service         : 'TaskTrigger',
                config          : data.triggerConfig,
                provider        : data.triggerConfig &&
                                  data.triggerConfig.data &&
                                  data.triggerConfig.type,
                formId          : data.triggerFormId,
                formPrefixes    : data.triggerFormPrefixes,
                inputFieldPrefix: data.triggerInputPrefix,
                postLoadEditor  : data.postLoadEditor
            });

        self.action = new PluginEditor(
            {
                service         : 'TaskAction',
                config          : data.actionConfig,
                provider        : data.actionConfig &&
                                  data.actionConfig.data &&
                                  data.actionConfig.type,
                formId          : data.actionFormId,
                formPrefixes    : data.actionFormPrefixes,
                inputFieldPrefix: data.actionInputPrefix,
                postLoadEditor  : data.postLoadEditor
            });

        self.userData = new MultiMap({data: data.userData, inputPrefix: data.userDataInputPrefix});
    }

    init() {
        this.trigger.init();
        this.action.init();
        this.conditions.init();
    }
}