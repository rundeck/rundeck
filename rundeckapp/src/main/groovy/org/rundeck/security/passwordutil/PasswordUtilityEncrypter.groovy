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
package org.rundeck.security.passwordutil

import com.dtolabs.rundeck.core.plugins.configuration.Property
import grails.web.servlet.mvc.GrailsParameterMap

/**
 * Provide the ability to encrypt values. The PasswordUtilityController will pick up all implementing classes
 * and provide them as options to users as long as they are registered as described by the {@link java.util.ServiceLoader} class.
 */
interface PasswordUtilityEncrypter {
    String name()
    Map encrypt(Map params)
    List<Property> formProperties()
}