/*
 * Copyright 2011 DTO Labs, Inc. (http://dtolabs.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
* ForceLiteralMultilineOptions.java
*
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Jan 27, 2011 11:03:04 AM
*
*/
package com.dtolabs.rundeck.core.utils.snakeyaml;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.emitter.ScalarAnalysis;


/**
 * ForceMultilineLiteralOptions forces literal output style for multiline scalars, and forces single quoted scalar flow
 * style where the scalar might appear like a number
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class ForceMultilineLiteralOptions extends DumperOptions {

    public static final String REGEX_NUMERICAL_EXTRA = "^[0-9_,.+-]+$";
    public static final String REGEX_NUMERICAL = "^[0-9]+$";

    @Override
    public DumperOptions.ScalarStyle calculateScalarStyle(
            final ScalarAnalysis analysis,
            final DumperOptions.ScalarStyle style
    )
    {
        if (analysis.multiline) {
            return ScalarStyle.LITERAL;
        }
        if (
                style == ScalarStyle.PLAIN
                && !analysis.scalar.matches(REGEX_NUMERICAL)
                && analysis.scalar.matches(REGEX_NUMERICAL_EXTRA)
        )
        {
            //a scalar like `9,15` as plain can be mis-interpreted on load as numerical (locale dependant?), so quote it
            //https://github.com/rundeck/rundeck/issues/1773
            return ScalarStyle.SINGLE_QUOTED;
        }
        return super.calculateScalarStyle(analysis, style);
    }
}
