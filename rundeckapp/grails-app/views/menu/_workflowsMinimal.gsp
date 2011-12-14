<%--
 Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 --%>
<%--
    _workflowsMinimal.gsp
    
    Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
    Created: Feb 9, 2010 11:16:06 AM
    $Id$
 --%>

<table cellspacing="0" cellpadding="0" width="100%" >
    <tr>

        <td style="text-align:left;vertical-align:top;width:200px;display:none;" id="filter" >

        </td>
        <td style="text-align:left;vertical-align:top;" id="wfcontent">


            <span id="busy" style="display:none"></span>

            <g:if test="${ groupTree}">
                <g:render template="groupTree" model="${[small:true,groupTree:groupTree.subs,currentJobs:groupTree['jobs']?groupTree['jobs']:[],wasfiltered:wasfiltered?true:false,nowrunning:nowrunning,nextExecutions:nextExecutions,authMap:authMap,nowrunningtotal:nowrunningtotal,max:max,offset:offset,paginateParams:paginateParams,sortEnabled:true]}"/>
            </g:if>
        </td>
    </tr>
</table>