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
            $('nrlocal').innerHTML = '' + count;
        }
        function showError(message) {
            if ($('loaderror')) {
                $("loaderror").innerHTML += message;
                $("loaderror").show();
            }
        }
        var bfilters=${boxfilters.encodeAsJSON()};
        var links = {
            nowrunning:'${createLink(controller:"menu",action:"nowrunningFragment")}',
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
            <span class="welcomeMessage">Recent and Currently Running <g:message code="domain.ScheduledExecution.title"/>s (<span id='nrlocal'>${total}</span>)</span>
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
