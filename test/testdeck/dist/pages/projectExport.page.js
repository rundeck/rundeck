"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.ProjectExportPage = exports.Radios = exports.Checkboxes = exports.Elems = void 0;
/*
 * Copyright 2020 Rundeck, Inc. (http://rundeck.com)
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
const selenium_webdriver_1 = require("selenium-webdriver");
const page_1 = require("../page");
var Elems;
(function (Elems) {
    /** @todo This button could use an id */
    Elems["submitBtn"] = "//button[@type=\"submit\"]";
})(Elems || (exports.Elems = Elems = {}));
exports.Checkboxes = [
    'exportAll',
    'exportJobs',
    'exportExecutions',
    'exportConfigs',
    'exportReadmes',
    'exportAcls',
    'exportScm',
    // 'exportWebhooks',
    // 'whkIncludeAuthTokens'
];
exports.Radios = [
    'dontStrip',
    'stripName',
    'stripUuid'
];
class ProjectExportPage extends page_1.Page {
    constructor(ctx, project) {
        super(ctx);
        this.ctx = ctx;
        this.project = project;
        this.path = '/';
        this.path = `/project/${project}/export`;
    }
    async getLabel(name) {
        return await this.ctx.driver.findElement(selenium_webdriver_1.By.xpath(`//label[@for="${name}"]`));
    }
    async getCheckbox(name) {
        return await this.ctx.driver.findElement(selenium_webdriver_1.By.xpath(`//input[@type="checkbox"][@name="${name}"]`));
    }
    async getRadio(name) {
        return await this.ctx.driver.findElement(selenium_webdriver_1.By.xpath(`//input[@type="radio"][@id="${name}"]`));
    }
}
exports.ProjectExportPage = ProjectExportPage;
//# sourceMappingURL=projectExport.page.js.map