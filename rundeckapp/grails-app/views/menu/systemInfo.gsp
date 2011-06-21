%{--
  - Copyright 2011 DTO Solutions, Inc. (http://dtosolutions.com)
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -        http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  --}%
<%--
   systemInfo.gsp

   Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
   Created: 6/1/11 9:44 AM
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <title>System Info</title>
</head>

<body>

<div class="pageTop">
    <div class="floatl">
        <span class="welcomeMessage">System Info</span>
    </div>

    <div class="clear"></div>
</div>

<div class="pageBody">

    <g:set var="datapercol" value="${5.0}"/>
    <g:set var="colcount" value="${(int)Math.ceil((float)systemInfo.size()/datapercol)}"/>
    <table>
        <tr>
            <g:each in="${0..colcount-1}" var="colnum">
                <g:set var="colstart" value="${(int)(colnum)*datapercol}"/>
                <g:set var="colmax" value="${(int)(colnum+1)*datapercol-1}"/>
                <g:set var="coldata"
                       value="${systemInfo[colstart..(colmax<systemInfo.size?colmax:systemInfo.size-1)]}"/>
                <td style="vertical-align: top;">
                    <table class="simpleForm">
                        <g:each in="${coldata}" var="dataset">
                            <g:each in="${dataset.keySet().sort()}" var="dataname">
                                <g:if test="${dataset[dataname] instanceof Map}">
                                    <tbody>
                                    <th colspan="2">${dataname.encodeAsHTML()}</th>
                                    <g:each
                                        in="${dataset[dataname].keySet().sort().grep{!it.endsWith('.unit') && !it.endsWith('.info')}}"
                                        var="valuename">
                                        <tr>
                                            <td title="${dataset[dataname][valuename + '.info'] ? dataset[dataname][valuename + '.info'].encodeAsHTML() : ''}">${valuename.encodeAsHTML()}</td>
                                            <td>

                                                <g:if test="${dataset[dataname][valuename+'.unit']=='ratio'}">
                                                    <g:render template="/common/progressBar"
                                                              model="${[completePercent:(int)(100*dataset[dataname][valuename]),title:dataset[dataname][valuename+'.info']?dataset[dataname][valuename+'.info']:dataset[dataname][valuename],className:'',showpercent:true]}"/>
                                                </g:if>
                                                <g:elseif test="${dataset[dataname][valuename+'.unit']}">
                                                    <g:humanize value="${dataset[dataname][valuename]}"
                                                                unit="${dataset[dataname][valuename+'.unit']}"/>
                                                </g:elseif>
                                                <g:else>
                                                    ${dataset[dataname][valuename].encodeAsHTML()}
                                                </g:else>
                                            </td>
                                        </tr>

                                    </g:each>
                                    </tbody>
                                </g:if>
                            </g:each>
                        </g:each>
                    </table>
                </td>
            </g:each>
        </tr>
    </table>
</div>
</body>
</html>