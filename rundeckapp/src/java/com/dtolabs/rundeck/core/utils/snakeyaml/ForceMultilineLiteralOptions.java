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
 * ForceMultilineLiteralOptions forces literal output style for multiline scalars
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class ForceMultilineLiteralOptions extends DumperOptions {
    @Override
    public DumperOptions.ScalarStyle calculateScalarStyle(final ScalarAnalysis analysis,
                                                          final DumperOptions.ScalarStyle style) {
        if (analysis.multiline) {
            return ScalarStyle.LITERAL;
        }
        return super.calculateScalarStyle(analysis, style);
    }
}
