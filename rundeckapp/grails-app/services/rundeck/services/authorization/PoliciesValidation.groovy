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

package rundeck.services.authorization

import com.dtolabs.rundeck.core.authorization.RuleSetValidation
import com.dtolabs.rundeck.core.authorization.Validation
import com.dtolabs.rundeck.core.authorization.providers.PolicyCollection
import groovy.transform.ToString

/**
 * @author greg
 * @since 9/19/17
 */
@ToString
class PoliciesValidation implements RuleSetValidation<PolicyCollection> {
    @Delegate
    Validation validation
    PolicyCollection policies

    @Override
    PolicyCollection getSource() {
        return policies
    }
}
