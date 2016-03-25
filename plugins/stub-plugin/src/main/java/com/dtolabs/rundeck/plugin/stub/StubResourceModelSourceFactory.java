/*
 * Copyright 2014 SimplifyOps Inc, <http://simplifyops.com>
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

package com.dtolabs.rundeck.plugin.stub;

import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.plugins.configuration.*;
import com.dtolabs.rundeck.core.resources.ResourceModelSource;
import com.dtolabs.rundeck.core.resources.ResourceModelSourceFactory;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder;
import com.dtolabs.rundeck.plugins.util.PropertyBuilder;

import java.util.Properties;

/**
 * StubResourceModelSourceFactory is ...
 *
 * @author Greg Schueler <a href="mailto:greg@simplifyops.com">greg@simplifyops.com</a>
 * @since 2014-09-03
 */
@Plugin(name = StubResourceModelSourceFactory.SERVICE_PROVIDER_NAME, service = ServiceNameConstants.ResourceModelSource)
public class StubResourceModelSourceFactory implements ResourceModelSourceFactory, Describable{
    @Override
    public ResourceModelSource createResourceModelSource(Properties configuration) throws ConfigurationException {
        return new StubResourceModelSource(configuration);
    }


    static final Description DESC;

    public static final String SERVICE_PROVIDER_NAME = "stub";

    static {
        DescriptionBuilder builder = DescriptionBuilder.builder();
        builder.name(SERVICE_PROVIDER_NAME)
                .title("Stub")
                .description("Generates stub nodes with stub node executors, useful for testing")
        ;

        builder.property(PropertyUtil.string("prefix", "Prefix",
                "Name prefix for all nodes",
                true, "node"));
        builder.property(PropertyUtil.string("suffix", "Suffix",
                "Name suffix for all nodes",
                false, null));
        builder.property(PropertyUtil.integer("count", "Count",
                "Number of nodes",
                true, "1"));
        builder.property(PropertyUtil.string("tags", "Tags",
                "Comma separated tags to add to all nodes",
                false, "stub"));
        builder.property(PropertyUtil.string("attrs", "Attributes",
                "Comma separated key=val to add to all nodes",
                false, null));
        builder.property(PropertyUtil.string("delay", "Delay",
                "Seconds of delay to introduce, or range.\n\nCan be, e.g. `0-10` (random between 0 and 10 seconds)",
                false, "0"));

        builder.mapping("prefix", "plugin.ResourceModelSource.stub.prefix");
        builder.mapping("suffix", "plugin.ResourceModelSource.stub.suffix");
        builder.mapping("count", "plugin.ResourceModelSource.stub.count");
        builder.mapping("tags", "plugin.ResourceModelSource.stub.tags");
        builder.mapping("attrs", "plugin.ResourceModelSource.stub.attrs");
        builder.mapping("delay", "plugin.ResourceModelSource.stub.delay");

        DESC = builder.build();
    }


    public Description getDescription() {
        return DESC;
    }
}
