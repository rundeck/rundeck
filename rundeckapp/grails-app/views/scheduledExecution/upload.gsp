<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="tabpage" content="jobs"/>
    <meta name="layout" content="base"/>
    <title>%{--
  - Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -     http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  --}%

<g:appTitle/> - <g:message code="upload.job.page.title" /></title>

    <g:set var="uiType" value="${params.ui ?: 'current'}"/>

    <g:if test="${uiType != 'legacy'}">
        <asset:javascript src="static/pages/job/upload.js" defer="defer"/>
        <asset:stylesheet src="static/css/pages/job/upload.css"/>
    </g:if>
    <g:else>
    <script type="text/javascript">
        jQuery(function(){
            jQuery('.act_job_action_dropdown').on('click',function(){
                var id=jQuery(this).data('jobId');
                var el=jQuery(this).parent().find('.dropdown-menu');
                el.load(_genUrl(appLinks.scheduledExecutionActionMenuFragment,{id:id,jobDeleteSingle:true}));
            });
            jQuery('#xmlBatch').on('change', function () {
                if (this.files.length == 1) {
                    if (this.files[0].name.match(/\.ya?ml$/i)) {
                        jQuery('input[name=fileformat][value=yaml]').prop('checked', true);
                    } else if (this.files[0].name.match(/\.xml$/i)) {
                        jQuery('input[name=fileformat][value=xml]').prop('checked', true);
                    }else if (this.files[0].name.match(/\.json$/i)) {
                        jQuery('input[name=fileformat][value=json]').prop('checked', true);
                    }
                }
            });
        });
    </script>
    </g:else>
</head>
<body>
<div class="content">
<div id="layoutBody">
    <g:if test="${uiType != 'legacy'}">
        <div class="vue-ui-socket">
            <ui-socket section="job-upload-page" location="main"></ui-socket>
        </div>
    </g:if>
    <g:else>
    <tmpl:uploadForm />
    </g:else>
</div>
</div>
</body>
</html>
