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

package com.dtolabs.rundeck.app.api

import com.dtolabs.rundeck.app.api.marshall.ApiResource
import com.dtolabs.rundeck.app.api.marshall.CustomFormatter
import org.apache.commons.lang.time.FastDateFormat

/**
 * Wraps a date with a default and customizable format, use with {@link com.dtolabs.rundeck.app.api.marshall
 * com.dtolabs.rundeck.app.api.marshall.CustomFormat} annotation to define a new format
 * @author greg
 * @since 3/24/17
 */
@ApiResource
class FormattedDate implements CustomFormatter {

    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'"

    FormattedDate(final Date value) {
        this.value = value
    }
    Date value

    @Override
    String format(final String param) {
        FastDateFormat.getInstance(
                param ?: DEFAULT_DATE_FORMAT,
                TimeZone.getTimeZone("GMT"),
                Locale.US
        ).format(value)
    }
}
