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
import org.codehaus.groovy.grails.web.context.ServletContextHolder as SCH
/*
 * RoleService.java
 * 
 * User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * Created: Jul 8, 2010 2:46:28 PM
 * $Id$
 */

/**
 * RoleService provides tests whether a user is a member of roles by mapped name.
 */
public class RoleService {
    def servletContext = SCH.servletContext
    /**
     * Return true if the http request user is in a role defined by the mapped rolename.
     * If no role mappings for the input rolename exist, then the rolename is used as-is
     * and true is returned if the user is in a role with that name.
     */
    public boolean isUserInRole(request,rolename){
        if(null==rolename){
            throw new NullPointerException("null rolename")
        }
        //look up mapped roles and see if user is in any of them
        def mapped = servletContext.getAttribute("MAPPED_ROLES")
        def roleset=[]
        def found = mapped[rolename]
        if(found instanceof String && found.indexOf(",")>0){
            found = found.split(",")
            roleset.addAll(found as List)
        }

        if(found instanceof Collection){
            roleset.addAll(found)
        }else if(found instanceof String){
            roleset<<found
        }
        log.debug("isUserInRole: ${rolename}, roleset: ${roleset}");
        if(roleset){
            def foundrole=false;
            roleset.each{role->
                log.debug("testRole: ${role}: "+request.isUserInRole(role)+" tokenauth: "+ isSubjectInRole(request, role));
                if(request.isUserInRole(role) || isSubjectInRole(request, role)){
                    foundrole= true;
                }
            }
            if(foundrole){
                return foundrole
            }
        }else if(rolename){
            log.debug("didn't find mappings for role: ${rolename}, checking as plain role: "+request.isUserInRole(rolename) + " tokenauth: "+ isSubjectInRole(request, rolename))
            return request.isUserInRole(rolename)|| isSubjectInRole(request, rolename)
        }
        return false
    }

    private def isSubjectInRole(request, role) {
        return request.subject?.getPrincipals(com.dtolabs.rundeck.core.authentication.Group.class).find {it.name == role}?true:false
    }

    /**
     * Return true if the user is in all of the mapped roles defined in the input list
     */
    public boolean isUserInAllRoles(request,roles){
        boolean allowed=true
        def found= roles.find{role->
            !isUserInRole(request,role)
        }
        if(found){
            allowed=false
        }
        return allowed
    }

    /**
     * Return true if the user is in any of the mapped roles defined in the list
     */
    public boolean isUserInAnyRoles(request,roles){
        boolean allowed= false
        def found=roles.find{role->
            isUserInRole(request,role)
        }
        if(found){
            allowed=true
        }
        return allowed
    }



    /**
     * Return the list role memberships found from the role mappings, otherwise return empty list
     */
    public List listMappedRoleMembership(request){
        def mapped = servletContext.getAttribute("MAPPED_ROLES")
        def found=[]
        mapped.keySet().each{mappedkey->
            def mappedrole=mapped[mappedkey]

            def roleset=[]
            if(mappedrole instanceof String && mappedrole.indexOf(",")>0){
                roleset.addAll(mappedrole.split(",") as List)
            }else if (mappedrole instanceof Collection){
                roleset.addAll(mappedrole)
            }else if(mappedrole instanceof String){
                roleset<<mappedrole
            }
            log.debug("isUserInRole: roleset: ${roleset}");
            roleset.each{role->
                log.debug("testRole: ${role}: "+request.isUserInRole(role));
                if(request.isUserInRole(role)){
                    found<<role;
                }
            }
            log.debug("testRole(literal): ${mappedkey}: "+request.isUserInRole(mappedkey));
            if(request.isUserInRole(mappedkey)){
                found<<mappedkey
            }
        }
        if(!found){
            log.warn("User ${request.remoteUser} has no membership of any mapped roles.")
        }
        return found
    }

}