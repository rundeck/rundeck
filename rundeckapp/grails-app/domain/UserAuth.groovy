/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
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
* UserAuth.groovy
*
* User: greg
* Created: Feb 1, 2010 11:24:15 AM
* $Id$
*/

public class UserAuth {

    boolean workflow_create
    boolean workflow_read
    boolean workflow_update
    boolean workflow_delete
    boolean workflow_run  // doesn't this imply events_create
    boolean workflow_kill

    boolean events_create
    boolean events_read
    boolean events_update
    boolean events_delete

    boolean resources_create
    boolean resources_read
    boolean resources_update // implies resources_delete and resources_create
    boolean resources_delete


    static belongsTo = [user: User]

    public static UserAuth createDefault() {
        return new UserAuth(
            workflow_create: true,
            workflow_read: true,
            workflow_update: true,
            workflow_delete: true,
            workflow_run: true,
            workflow_kill: true,
            events_create: true,
            events_read: true,
            events_update: true,
            events_delete: true,
            resources_create: true,
            resources_read: true,
            resources_update: true,
            resources_delete: true,
        )
    }

    public static UserAuth createRestrictedDefault() {
        return new UserAuth()
    }

    public String toString() {
        return "UserAuth: (${id}) ${properties}"
    }

    private static final String _CREATE = "create"
    private static final String _READ = "read"
    private static final String _UPDATE = "update"
    private static final String _DELETE = "delete"
    private static final String _RUN = "run"
    private static final String _KILL = "kill"

    private static final String _WF = "workflow_"
    /** workflow_create authorization   */
    public static final String WF_CREATE = _WF + _CREATE
    /** workflow_read authorization   */
    public static final String WF_READ = _WF + _READ
    /** workflow_update authorization   */
    public static final String WF_UPDATE = _WF + _UPDATE
    /** workflow_delete authorization   */
    public static final String WF_DELETE = _WF + _DELETE
    /** workflow_run authorization   */
    public static final String WF_RUN = _WF + _RUN
    /** workflow_kill authorization   */
    public static final String WF_KILL = _WF + _KILL

    private static final String _EV = "events_"
    /** events_create authorization   */
    public static final String EV_CREATE = _EV + _CREATE
    /** events_read authorization   */
    public static final String EV_READ = _EV + _READ
    /** events_update authorization   */
    public static final String EV_UPDATE = _EV + _UPDATE
    /** events_delete authorization   */
    public static final String EV_DELETE = _EV + _DELETE

    private static final String _RS = "resources_"
    /** resources_create authorization   */
    public static final String RS_CREATE = _RS + _CREATE
    /** resources_read authorization   */
    public static final String RS_READ = _RS + _READ
    /** resources_update authorization   */
    public static final String RS_UPDATE = _RS + _UPDATE
    /** resources_delete authorization   */
    public static final String RS_DELETE = _RS + _DELETE


}