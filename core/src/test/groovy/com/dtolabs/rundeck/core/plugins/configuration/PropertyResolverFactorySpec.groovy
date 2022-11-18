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

package com.dtolabs.rundeck.core.plugins.configuration

import com.dtolabs.rundeck.plugins.util.DescriptionBuilder;
import spock.lang.Specification;


/**
 */
public class PropertyResolverFactorySpec extends Specification {

    def pluginPropertyPrefix() {
        expect:
            "plugin.type.name." == PropertyResolverFactory.pluginPropertyPrefix("type", "name")
    }

    def frameworkPropertyPrefix() {
        expect:
        ("framework.abc"== PropertyResolverFactory.frameworkPropertyPrefix("abc"));
    }

    def projectPropertyPrefix() {
        expect:
        ("project.abc"== PropertyResolverFactory.projectPropertyPrefix("abc"));
    }

    def createResolver() {
        given:
        HashMap<String, String> instanceData = new HashMap<String, String>() {{
            put("a", "instance");
        }};
        HashMap<String, String> projectData = new HashMap<String, String>() {{
            put("a", "project");
        }};
        HashMap<String, String> frameworkData = new HashMap<String, String>() {{
            put("a", "framework");
        }};
        when:
        PropertyResolver resolver = PropertyResolverFactory.createResolver(
                PropertyResolverFactory.instanceRetriever(instanceData),
                PropertyResolverFactory.instanceRetriever(projectData),
                PropertyResolverFactory.instanceRetriever(frameworkData)
        );
        then:
        ("instance"== resolver.resolvePropertyValue("a", PropertyScope.InstanceOnly));
        ("instance"== resolver.resolvePropertyValue("a", PropertyScope.Instance));
        ("project"== resolver.resolvePropertyValue("a", PropertyScope.ProjectOnly));
        ("project"== resolver.resolvePropertyValue("a", PropertyScope.Project));
        ("framework"== resolver.resolvePropertyValue("a", PropertyScope.Framework));
    }

    def createResolverProject() {
        given:
        HashMap<String, String> instanceData = new HashMap<String, String>() {{
        }};
        HashMap<String, String> projectData = new HashMap<String, String>() {{
            put("a", "project");
        }};
        HashMap<String, String> frameworkData = new HashMap<String, String>() {{
            put("a", "framework");
        }};
        when:
        PropertyResolver resolver = PropertyResolverFactory.createResolver(
                PropertyResolverFactory.instanceRetriever(instanceData),
                PropertyResolverFactory.instanceRetriever(projectData),
                PropertyResolverFactory.instanceRetriever(frameworkData)
        );
        then:
        (null== resolver.resolvePropertyValue("a", PropertyScope.InstanceOnly));
        ("project"== resolver.resolvePropertyValue("a", PropertyScope.Instance));
        ("project"== resolver.resolvePropertyValue("a", PropertyScope.ProjectOnly));
        ("project"== resolver.resolvePropertyValue("a", PropertyScope.Project));
        ("framework"== resolver.resolvePropertyValue("a", PropertyScope.Framework));
    }

    def createResolverFramework() {
        given:
        HashMap<String, String> instanceData = new HashMap<String, String>() {{
        }};
        HashMap<String, String> projectData = new HashMap<String, String>() {{
        }};
        HashMap<String, String> frameworkData = new HashMap<String, String>() {{
            put("a", "framework");
        }};
        when:
        PropertyResolver resolver = PropertyResolverFactory.createResolver(
                PropertyResolverFactory.instanceRetriever(instanceData),
                PropertyResolverFactory.instanceRetriever(projectData),
                PropertyResolverFactory.instanceRetriever(frameworkData)
        );
        then:
        (null== resolver.resolvePropertyValue("a", PropertyScope.InstanceOnly));
        ("framework"== resolver.resolvePropertyValue("a", PropertyScope.Instance));
        (null== resolver.resolvePropertyValue("a", PropertyScope.ProjectOnly));
        ("framework"== resolver.resolvePropertyValue("a", PropertyScope.Project));
        ("framework"== resolver.resolvePropertyValue("a", PropertyScope.Framework));
    }

    def "ScopedResolver resolver"() {
        given:
            Description desc = DescriptionBuilder.builder().name('aplugin').
                stringProperty('aprop', 'blah', false, 'blah', '').
                mapping([aprop: 'prj.aprop']).
                frameworkMapping([aprop: 'fwk.aprop']).
                build()
            def resolver = new PropertyResolverFactory.ScopedResolver(
                PropertyResolverFactory.instanceRetriever(iprops),
                PropertyResolverFactory.instanceRetriever(pprops),
                PropertyResolverFactory.instanceRetriever(fprops),
                desc,
                'project.plugin.type.name.',
                'framework.plugin.type.name.',
            )
        when:
            def result = resolver.resolvePropertyValue(propname, propScope)
        then:
            result == expected
        where:
            propname='aprop'
            propScope | iprops | pprops | fprops | expected

            PropertyScope.InstanceOnly | [aprop: 'aval']  | [:] | [:]    | 'aval'
            PropertyScope.Instance     | [aprop: 'aval']  | [:] | [:]    | 'aval'
            PropertyScope.ProjectOnly  | [aprop: 'aval']  | [:] | [:]    | null
            PropertyScope.Project      | [aprop: 'aval']  | [:] | [:]    | null
            PropertyScope.Framework    | [aprop: 'aval']  | [:] | [:]    | null

            PropertyScope.InstanceOnly | [:]|['prj.aprop': 'bval'] |[:]| null
            PropertyScope.Instance     | [:]|['prj.aprop': 'bval'] |[:]| 'bval'
            PropertyScope.ProjectOnly  | [:]|['prj.aprop': 'bval'] |[:]| 'bval'
            PropertyScope.Project      | [:]|['prj.aprop': 'bval'] |[:]| 'bval'
            PropertyScope.Framework    | [:]|['prj.aprop': 'bval'] |[:]| null

            PropertyScope.InstanceOnly |[:]|[:]|['fwk.aprop': 'cval'] | null
            PropertyScope.Instance     |[:]|[:]|['fwk.aprop': 'cval'] | 'cval'
            PropertyScope.ProjectOnly  |[:]|[:]|['fwk.aprop': 'cval'] | null
            PropertyScope.Project      |[:]|[:]|['fwk.aprop': 'cval'] | 'cval'
            PropertyScope.Framework    |[:]|[:]|['fwk.aprop': 'cval'] | 'cval'


    }
}
