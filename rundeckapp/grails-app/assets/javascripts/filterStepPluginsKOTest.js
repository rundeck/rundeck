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

var StepFiltersTest = function () {
    var self = this;
    var failed=0;
    var total=0;
    var assert = function (msg, expect, val) {
        total++;
        if (expect != val) {
            failed++;
            jQuery('#step-filters-tests').append(jQuery('<div></div>').append(jQuery('<span class="text-danger"></span>').text("FAIL: " + msg + ": expected: " + expect + ", was: " + val)));
        } else {
            jQuery('#step-filters-tests').append(jQuery('<div></div>').append(jQuery('<span class="text-success"></span>').text("OK: " + msg)));
        }
    };

    function createStepFilterObj(data){
        return new StepPluginsFilter(data);
    }

    function createPluginDescritptions(){
        return [
            {
                title: 'Plugin Example 1',
                name: 'plugin1',
                properties: [{
                    title: 'Preperty Example 1',
                    name: 'propEx1'
                }]
            },
            {
                title: 'Plugin Example 2',
                name: 'plugin2',
                properties: [{
                    title: 'Preperty Example 2',
                    name: 'propEx2'
                }]
            },
            {
                title: 'Other Example',
                name: 'other',
                properties: [{
                    title: 'Other Preperty',
                    name: 'otherProp'
                }]
            },
            {
                title: 'Default Step Plugin',
                name: 'default'
            },
            {
                title: 'Other Default Step Plugin',
                name: 'otherDefault'
            }
        ]
    }

    self.emptyFilterTest=function(pref){
        var stepDescriptionsList = createPluginDescritptions();
        var sf=createStepFilterObj({stepDescriptions:stepDescriptionsList});
        sf.stepFilterValue("");
        sf.filterStepDescriptions();
        assert(pref+'currentPropertyFilter','title',sf.currentPropertyFilter());
        assert(pref+'currentFilter',undefined,sf.currentFilter());
        assert(pref+'Plugin Example 1 should be visible',true,sf.isVisible(stepDescriptionsList[0].name));
        assert(pref+'Plugin Example 2 should be visible',true,sf.isVisible(stepDescriptionsList[1].name));
        assert(pref+'Other Example should be visible',true,sf.isVisible(stepDescriptionsList[2].name));
        assert(pref+'Default Step Plugin should be visible',true,sf.isDefaultStepsVisible(stepDescriptionsList[3].title));
        assert(pref+'Other Default Step Plugin should be visible',true,sf.isDefaultStepsVisible(stepDescriptionsList[4].title));
    };

    self.basicFilterTest=function(pref){
        var stepDescriptionsList = createPluginDescritptions();
        var sf=createStepFilterObj({stepDescriptions:stepDescriptionsList});
        sf.stepFilterValue("Plugin Example");
        sf.filterStepDescriptions();
        assert(pref+'currentPropertyFilter','title',sf.currentPropertyFilter());
        assert(pref+'currentFilter',"Plugin Example",sf.currentFilter());
        assert(pref+'Plugin Example 1 should be visible',true,sf.isVisible(stepDescriptionsList[0].name));
        assert(pref+'Plugin Example 2 should be visible',true,sf.isVisible(stepDescriptionsList[1].name));
        assert(pref+'Other Example should not be visible',false,sf.isVisible(stepDescriptionsList[2].name));
        assert(pref+'Default Step Plugin should not be visible',false,sf.isDefaultStepsVisible(stepDescriptionsList[3].title));
        assert(pref+'Other Default Step Plugin should not be visible',false,sf.isDefaultStepsVisible(stepDescriptionsList[4].title));
    };

    self.ignoreCaseFilterTest=function(pref){
        var stepDescriptionsList = createPluginDescritptions();
        var sf=createStepFilterObj({stepDescriptions:stepDescriptionsList});
        sf.stepFilterValue("PLUGIN example");
        sf.filterStepDescriptions();
        assert(pref+'currentPropertyFilter','title',sf.currentPropertyFilter());
        assert(pref+'currentFilter',"PLUGIN example",sf.currentFilter());
        assert(pref+'Plugin Example 1 should be visible',true,sf.isVisible(stepDescriptionsList[0].name));
        assert(pref+'Plugin Example 2 should be visible',true,sf.isVisible(stepDescriptionsList[1].name));
        assert(pref+'Other Example should not be visible',false,sf.isVisible(stepDescriptionsList[2].name));
        assert(pref+'Default Step Plugin should not be visible',false,sf.isVisible(stepDescriptionsList[3].title));
        assert(pref+'Other Default Step Plugin should not be visible',false,sf.isDefaultStepsVisible(stepDescriptionsList[4].title));
    };

    self.searchDefaultStepTest=function(pref){
        var stepDescriptionsList = createPluginDescritptions();
        var sf=createStepFilterObj({stepDescriptions:stepDescriptionsList});
        sf.stepFilterValue("Default");
        sf.filterStepDescriptions();
        assert(pref+'currentPropertyFilter','title',sf.currentPropertyFilter());
        assert(pref+'currentFilter',"Default",sf.currentFilter());
        assert(pref+'Plugin Example 1 should not be visible',false,sf.isVisible(stepDescriptionsList[0].name));
        assert(pref+'Plugin Example 2 should not be visible',false,sf.isVisible(stepDescriptionsList[1].name));
        assert(pref+'Other Example should not be visible',false,sf.isVisible(stepDescriptionsList[2].name));
        assert(pref+'Default Step Plugin should be visible',true,sf.isDefaultStepsVisible(stepDescriptionsList[3].title));
        assert(pref+'Other Default Step Plugin should be visible',true,sf.isDefaultStepsVisible(stepDescriptionsList[4].title));
    };

    self.searchOtherDefaultStepTest=function(pref){
        var stepDescriptionsList = createPluginDescritptions();
        var sf=createStepFilterObj({stepDescriptions:stepDescriptionsList});
        sf.stepFilterValue("Other");
        sf.filterStepDescriptions();
        assert(pref+'currentPropertyFilter','title',sf.currentPropertyFilter());
        assert(pref+'currentFilter',"Other",sf.currentFilter());
        assert(pref+'Plugin Example 1 should not be visible',false,sf.isVisible(stepDescriptionsList[0].name));
        assert(pref+'Plugin Example 2 should not be visible',false,sf.isVisible(stepDescriptionsList[1].name));
        assert(pref+'Other Example should be visible',true,sf.isVisible(stepDescriptionsList[2].name));
        assert(pref+'Default Step Plugin should be visible',false,sf.isDefaultStepsVisible(stepDescriptionsList[3].title));
        assert(pref+'Other Default Step Plugin should be visible',true,sf.isDefaultStepsVisible(stepDescriptionsList[4].title));
    };

    self.searchByNameTest=function(pref){
        var stepDescriptionsList = createPluginDescritptions();
        var sf=createStepFilterObj({stepDescriptions:stepDescriptionsList});
        sf.stepFilterValue("name=plugin1");
        sf.filterStepDescriptions();
        assert(pref+'currentPropertyFilter','name',sf.currentPropertyFilter());
        assert(pref+'currentFilter',"plugin1",sf.currentFilter());
        assert(pref+'Plugin Example 1 should be visible',true,sf.isVisible(stepDescriptionsList[0].name));
        assert(pref+'Plugin Example 2 should not be visible',false,sf.isVisible(stepDescriptionsList[1].name));
        assert(pref+'Other Example should not be visible',false,sf.isVisible(stepDescriptionsList[2].name));
        assert(pref+'Default Step Plugin should not be visible',false,sf.isDefaultStepsVisible(stepDescriptionsList[3].title));
        assert(pref+'Other Default Step Plugin should not be visible',false,sf.isDefaultStepsVisible(stepDescriptionsList[4].title));
    };

    self.searchByPropertyTitleTest=function(pref){
        var stepDescriptionsList = createPluginDescritptions();
        var sf=createStepFilterObj({stepDescriptions:stepDescriptionsList});
        sf.stepFilterValue("property:title=Other Preperty");
        sf.filterStepDescriptions();
        assert(pref+'currentPropertyFilter','property:title',sf.currentPropertyFilter());
        assert(pref+'currentFilter',"Other Preperty",sf.currentFilter());
        assert(pref+'Plugin Example 1 should not be visible',false,sf.isVisible(stepDescriptionsList[0].name));
        assert(pref+'Plugin Example 2 should not be visible',false,sf.isVisible(stepDescriptionsList[1].name));
        assert(pref+'Other Example should be visible',true,sf.isVisible(stepDescriptionsList[2].name));
        assert(pref+'Default Step Plugin should not be visible',false,sf.isDefaultStepsVisible(stepDescriptionsList[3].title));
        assert(pref+'Other Default Step Plugin should not be visible',false,sf.isDefaultStepsVisible(stepDescriptionsList[4].title));
    };

    self.searchByInvalidAttrTest=function(pref){
        var stepDescriptionsList = createPluginDescritptions();
        var sf=createStepFilterObj({stepDescriptions:stepDescriptionsList});
        sf.stepFilterValue("invalidAttr=Other Preperty");
        sf.filterStepDescriptions();
        assert(pref+'currentPropertyFilter','invalidAttr',sf.currentPropertyFilter());
        assert(pref+'currentFilter',"Other Preperty",sf.currentFilter());
        assert(pref+'Plugin Example 1 should not be visible',false,sf.isVisible(stepDescriptionsList[0].name));
        assert(pref+'Plugin Example 2 should not be visible',false,sf.isVisible(stepDescriptionsList[1].name));
        assert(pref+'Other Example should not be visible',false,sf.isVisible(stepDescriptionsList[2].name));
        assert(pref+'Default Step Plugin should not be visible',false,sf.isDefaultStepsVisible(stepDescriptionsList[3].title));
        assert(pref+'Other Default Step Plugin should not be visible',false,sf.isDefaultStepsVisible(stepDescriptionsList[4].title));
    };

    self.searchByPropertyNameTest=function(pref){
        var stepDescriptionsList = createPluginDescritptions();
        var sf=createStepFilterObj({stepDescriptions:stepDescriptionsList});
        sf.stepFilterValue("property:name=otherProp");
        sf.filterStepDescriptions();
        assert(pref+'currentPropertyFilter','property:name',sf.currentPropertyFilter());
        assert(pref+'currentFilter',"otherProp",sf.currentFilter());
        assert(pref+'Plugin Example 1 should not be visible',false,sf.isVisible(stepDescriptionsList[0].name));
        assert(pref+'Plugin Example 2 should not be visible',false,sf.isVisible(stepDescriptionsList[1].name));
        assert(pref+'Other Example should be visible',true,sf.isVisible(stepDescriptionsList[2].name));
        assert(pref+'Default Step Plugin should not be visible',false,sf.isDefaultStepsVisible(stepDescriptionsList[3].title));
        assert(pref+'Other Default Step Plugin should not be visible',false,sf.isDefaultStepsVisible(stepDescriptionsList[4].title));
    };

    self.testAll = function () {
        jQuery(document.body).append(jQuery('<div id="step-filters-tests" class="test-elem"></div>'));
        assert("Start: filterStepKOTest.js", 1, 1);
        for (var i in self) {
            if (i.endsWith('Test')) {
                try {
                    self[i].call(self, i + ': ');
                } catch (e) {
                    assert(i + ': error', null, e);
                }
            }
        }
        if(failed>0){
            jQuery(document.body).prepend(jQuery('<div></div>').append(jQuery('<span class="text-danger"></span>').text("FAIL: " + failed+"/"+total+" assertions failed")));
        }
    };
};
jQuery(function () {
    new StepFiltersTest().testAll();
});
