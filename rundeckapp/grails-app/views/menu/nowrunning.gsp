<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="nowrunning"/>
    <title>Now Running</title>
    <script type="text/javascript">
        //<!--
        function _pageUpdateNowRunning(count, perc) {
            document.title = "Now Running (" + count + ")";
            setText($('nrlocal'), '' + count);
        }
        function showError(message) {
            if ($('loaderror')) {
                appendText($("loaderror"),message);
                $("loaderror").show();
            }
        }
        var bfilters=loadJsonData('boxfiltersJSON');
        var links = {
            nowrunning:'%{--
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

${createLink(controller:"menu",action:"nowrunningFragment")}',
            %{--jobs:'${createLink(controller:"reports",action:"jobsFragment")}',--}%
            events:'${createLink(controller:"reports",action:"eventsFragment")}',
            %{--nodes:'${createLink(controller:"framework",action:"nodesFragment")}',--}%
        };
        function _updateBoxInfo(name, data) {
            boxctl.updateDataForTab(name, data);
        }
        var boxctl ;
        function _pageInit() {
            boxctl = new WBoxController({views:{db1:'db1',db2:'db2'},key:'nowrunning'});
            boxctl.addBox('db1', new WBox('box1', {tabs:[
                {
                    name:'nowrunning',
                    url:links['nowrunning'],
                    title:'Now Running',
                    reload:10,
                    params:{}
                }
            ]}));
            boxctl.addBox('db2', new WBox('box2', {tabs:[
                {
                    name:'events',
                    url:links['events'],
                    title:'Events',
                    reload:10,
                    params:{filterName:bfilters['events'],compact:true,max:20,moreLinkAction:'events'},
                    maximize:{ removeParams:['compact'] }
                },
            ]}));
            boxctl._pageInit();
        }

        Event.observe(window, 'load', _pageInit);
        //-->
    </script>
    <g:embedJSON id="boxfiltersJSON" data="${boxfilters}"/>
    <style type="text/css">
    table.dashboxes td.dashbox {
        width: auto;
    }

    table.dashboxes td.dashbox.small {
        width: auto;
    }

    td.dashbox div.wbox {
        max-height: none;
        width: auto;
        height: auto;
    }

    td.dashbox.small div.wbox {
        width: auto;
    }
    </style>
</head>
<body>

    <div class="pageTop">
        <div class="floatl">
            <span class="welcomeMessage">Recent and Currently Running <g:message code="domain.ScheduledExecution.title"/>s (<span id='nrlocal'><g:enc>${total}</g:enc></span>)</span>
        </div>
        <div class="clear"></div>
    </div>
    <div class="pageBody">

        <table cellspacing="0" cellpadding="0" class="dashboxes" width="100%">

            <tr>
                <td class="dashbox">
                    <div id="db1">
                        <span class="loading">Loading</span>
                    </div>
                </td>
            </tr>

            <tr>
                <td class="dashbox">
                    <div id="db2">
                        <span class="loading">Loading</span>
                    </div>
                </td>
            </tr>
        </table>
    </div>
    <div id="msgsContent" class="pageBody">

    </div>

    <div id="loaderror"></div>
</body>
</html>
