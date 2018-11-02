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
package com.rundeck.plugin.localtours;

import com.dtolabs.rundeck.plugins.tours.TourStep;

public class TourStepImpl implements TourStep {
    private String title;
    private String content;
    private String nextStepUrl;
    private String nextStepIndicator;
    private String stepIndicator;
    private String stepIndicatorPosition;

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getContent() {
        return content;
    }

    public String getNextStepUrl() {
        return nextStepUrl;
    }

    public String getNextStepIndicator() { return nextStepUrl; }

    public String getStepIndicator() {
        return stepIndicator;
    }

    public String getStepIndicatorPosition() {
        return stepIndicatorPosition;
    }
}
