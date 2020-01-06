package rundeck.interceptors

import javax.servlet.http.HttpServletRequest

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

class InterceptorHelper {
    public static final List<String> STATIC_ASSETS = Collections.unmodifiableList(["static", "assets", "feed", "user-assets"])
    public static final List<String> SERVLET_PATH_ALLOWED = Collections.unmodifiableList(['/error', '/favicon.ico', '/health'])

    static matchesStaticAssets(String controllerName, HttpServletRequest request) {
        return STATIC_ASSETS.contains(controllerName) || matchesStaticServletPath(request.servletPath)
    }

    static matchesStaticServletPath(String servletPath) {
        return SERVLET_PATH_ALLOWED.contains(servletPath)
    }
}
