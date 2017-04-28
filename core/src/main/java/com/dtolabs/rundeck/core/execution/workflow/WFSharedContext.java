/*
 * Copyright 2017 Rundeck, Inc. (http://rundeck.com)
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

package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.dispatcher.DataContext;
import com.dtolabs.rundeck.core.dispatcher.Mergable;
import com.dtolabs.rundeck.core.dispatcher.MultiDataContext;
import com.dtolabs.rundeck.core.dispatcher.MultiDataContextImpl;

import java.util.Map;

/**
 * @author greg
 * @since 4/28/17
 */
public class WFSharedContext
        extends MultiDataContextImpl<String, DataContext>
        implements MultiDataContext<String, DataContext>
{
    public WFSharedContext(final DataContext base) {
        super(base);
    }

    public WFSharedContext(final Map<String, DataContext> map) {
        super(map);
    }

    public WFSharedContext() {
        super();
    }

    public WFSharedContext(final MultiDataContext<String, DataContext> orig) {
        super(orig);
    }

    public WFSharedContext(WFSharedContext orig) {
        super(orig);
    }

    public static WFSharedContext withBase(final DataContext base) {
        WFSharedContext kdMultiDataContext = new WFSharedContext();
        kdMultiDataContext.setBase(base);
        return kdMultiDataContext;
    }

    public void merge(WFSharedContext input) {
        super.merge(input);
    }
}
