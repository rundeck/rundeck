<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="selectedMenu" content="Dashboard"/>
    <meta name="layout" content="base" />
    <title><g:message code="main.app.name"/> - <g:if test="${null==execution?.dateCompleted}">Now Running - </g:if><g:if test="${scheduledExecution}">${scheduledExecution?.jobName.encodeAsHTML()} :  </g:if><g:else>Transient <g:message code="domain.ScheduledExecution.title"/> : </g:else> Execution at <g:relativeDate atDate="${execution.dateStarted}" /> by ${execution.user}</title>
    <g:set var="followmode" value="${params.mode in ['browse','tail']?params.mode:null==execution?.dateCompleted?'tail':'browse'}"/>
    <g:set var="executionResource" value="${ ['jobName': execution.scheduledExecution ? execution.scheduledExecution.jobName : 'adhoc', 'groupPath': execution.scheduledExecution ? execution.scheduledExecution.groupPath : 'adhoc'] }"/>
      <script type="text/javascript">
  var cmdoutputtbl=null;
     var cmdoutspinner=null;
     var runningcmd=null;
 function appendCmdOutputError(message){
     if($('cmdoutputerror')){
     $("cmdoutputerror").innerHTML+=message;
     $("cmdoutputerror").show();
     }
 }
function _log(message){
     if($('log')){
        $("log").innerHTML+=message+"<br>";
     }
 }
 var appendtop=new Object();
     appendtop.value=false;
     appendtop.changed=false;
 var collapseCtx=new Object();
      collapseCtx.value=true;
      collapseCtx.changed=false;
 var showFinalLine=new Object();
      showFinalLine.value=true;
      showFinalLine.changed=false;
 var groupOutput=new Object();
      groupOutput.value=true;
 var autoscroll=true;

 var lastrow;
 var contextIdCounter=0;
 var contextStatus=new Object();
 var iconUrl="${resource(dir:'images',file:'icon')}";
 var lastlines=${params.lastlines?params.lastlines:20};
 var lastTBody;
 var ctxBodySet=new Array();
 var ctxBodyFinalSet=new Array();
 var ctxGroupSet=new Array();
  var refresh=${followmode=='tail'?"true":"false"};
  var tailmode = ${followmode=='tail'};
  var browsemode = ${followmode=='browse'};
  var taildelay=1;
  var execData={id:"${execution.id}",project:"${execution.project}",node:"${session.Framework.getFrameworkNodeHostname()}"};
  var reloadedIndex=-1;

  function updateTaildelay(val){
    val = parseInt(val);
    if(isNaN(val)){
        val=1;
    }
    if(val > 60){
        val=60;
    }else if(val < 0 ){
        val=0;
    }
    taildelay=val;
    $('taildelayvalue').value=taildelay;

    return false;
  }
function modifyTaildelay(val){
    var oldval = parseInt($('taildelayvalue').value);
    val = parseInt(val) ;
    oldval = oldval+val;
    updateTaildelay(oldval);
}

  function updateLastlines(val){
    val = parseInt(val);
    if(isNaN(val)){
        val=20;
    }
    if(val > 100){
        val=100;
    }else if(val < 5 ){
        val=5;
    }
    lastlines=val;
    $('lastlinesvalue').value=lastlines;
      if(!isrunning){
        %{--beginFollowingOutput("${execution.id}");--}%
      setTimeout(function(){loadMoreOutput("${execution.id}", 0);},50);
      }
    return false;
  }
function modifyLastlines(val){
    var oldval = parseInt($('lastlinesvalue').value);
    val = parseInt(val) ;
    oldval = oldval+val;
    updateLastlines(oldval);
}
 function isAppendTop(){
     return appendtop.value?true:false;
 }
function setCollapseCtx(val){
     if(collapseCtx.value!=val){
         collapseCtx.changed=true;
        collapseCtx.value=val;
     }

    if(collapseCtx.value){
        ctxBodySet._each(Element.hide);
        ctxBodyFinalSet._each(showFinalLine.value?Element.show:Element.hide);
        $$('.expandicon').each(function(e){e.addClassName('closed');e.removeClassName('opened');});
    }else{
        ctxBodySet._each(Element.show);
        ctxBodyFinalSet._each(Element.show);
        $$('.expandicon').each(function(e){e.removeClassName('closed');e.addClassName('opened');});
    }
    setCtxCollapseDisplay(val);
}
  function setCtxCollapseDisplay(val){
    if($('ctxcollapseLabel')){
         if(val){
             $('ctxcollapseLabel').addClassName('selected');
         }else{
             $('ctxcollapseLabel').removeClassName('selected');
         }
    }
    if($('ctxshowlastlineoption')){
        if(val){
            $('ctxshowlastlineoption').show();
        }else{
            $('ctxshowlastlineoption').hide();
        }
    }
 }

function setGroupOutput(val){
     if(groupOutput.value!=val){
        groupOutput.value=val;
     }
     ctxGroupSet._each(groupOutput.value?Element.show:Element.hide);
        if(groupOutput.value && collapseCtx.value){
            ctxBodySet._each(Element.hide);
            ctxBodyFinalSet._each(showFinalLine.value?Element.show:Element.hide);
        }else{
            ctxBodySet._each(Element.show);
            ctxBodyFinalSet._each(Element.show);
        }

    if(!groupOutput.value){
        if($('ctxcollapseLabel')){
            $('ctxcollapseLabel').hide();
        }
        if($('ctxshowlastlineoption')){
            $('ctxshowlastlineoption').hide();
        }

    }else{
        if ($('ctxcollapseLabel')){
            $('ctxcollapseLabel').show();
        }
        setCtxCollapseDisplay(collapseCtx.value);
    }
    if($('ctxshowgroupoption')){
         if(val){
             $('ctxshowgroupoption').addClassName('selected');
         }else{
             $('ctxshowgroupoption').removeClassName('selected');
         }
    }
 }
function setShowFinalLine(val){
     if(showFinalLine.value!=val){
         showFinalLine.changed=true;
        showFinalLine.value=val;
     }
     ctxBodyFinalSet.each(function(elem,ndx){
         if(!showFinalLine.value && collapseCtx.value && ctxBodySet[ndx] && !Element.visible(ctxBodySet[ndx])){
             Element.hide(elem);
         }else{
             Element.show(elem);
         }
     });

    if($('ctxshowlastlineoption')){
         if(val){
             $('ctxshowlastlineoption').addClassName('selected');
         }else{
             $('ctxshowlastlineoption').removeClassName('selected');
         }
    }
 }
 function setOutputAutoscroll(val){
     autoscroll = val;
     if ($('autoscrollTrueLabel')) {
         if(val){
             $('autoscrollTrueLabel').addClassName('selected');
         }else{
             $('autoscrollTrueLabel').removeClassName('selected');
         }
     }
     if ($('autoscrollFalseLabel')) {
         if(val){
            $('autoscrollFalseLabel').removeClassName('selected');
         }else{
            $('autoscrollFalseLabel').addClassName('selected');
         }
     }
 }
 function setOutputAppendTop(istop){
     if(appendtop.value!=istop){
         appendtop.changed=!appendtop.changed;
     }

    if($('appendTopLabel')){

        if(istop){
            $('appendTopLabel').addClassName('selected');
        }else{
            $('appendTopLabel').removeClassName('selected');
        }
    }
    if($('appendBottomLabel')){
        if(istop){
            $('appendBottomLabel').removeClassName('selected');
        }else{
            $('appendBottomLabel').addClassName('selected');
        }
    }
     appendtop.value=istop;

     if(!isrunning){
         reverseOutputTable(cmdoutputtbl);
     }
 }
  function clearTable(tbl){

      if(tbl){
        $('commandPerform').removeChild(tbl);
        cmdoutputtbl=null;
      }
      lastTBody=null;
      ctxBodySet=new Array();
      ctxBodyFinalSet=new Array();
      ctxGroupSet=new Array();
      runningcmd.count=0;
      runningcmd.entries=new Array();
      lastrow=null;
      contextIdCounter=0;
      contextStatus=new Object();
  }

 function appendCmdOutput(data){
     var orig=data;
     var needsScroll=false;
     if(!isAppendTop() && isAtBottom()){
        needsScroll=true;
     }
     if(refresh && $('cmdoutputtbl')){
         try{
            clearTable($('cmdoutputtbl'));
         }catch(e){
            _log(e);
         }
     }
     if(typeof(data) == "string" && data==""){
         return;
     }
     try{
         if(typeof(data) == "string"){
             eval("data="+data);
         }
         if(!cmdoutputtbl){

             var tbl=$(document.createElement("table"));
             tbl.setAttribute("border","0");
             tbl.setAttribute("width","100%");
             tbl.setAttribute("height","auto");
             tbl.setAttribute("cellSpacing","0");
             tbl.setAttribute("cellPadding","0");
             tbl.addClassName('execoutput');
             tbl.setAttribute('id','cmdoutputtbl');
             var th =tbl.createTHead();
             var thr1=th.insertRow(-1);
             var thi=document.createElement("th");
             thi.setAttribute("width","20px");
             thr1.appendChild(thi);
             var th1=document.createElement("th");
             th1.innerHTML="Time";
             thr1.appendChild(th1);
             var th2=document.createElement("th");
             th2.innerHTML="Message";
             th2.setAttribute('colspan','2');
             thr1.appendChild(th2);
             var tbod = document.createElement("tbody");
             tbl.appendChild(tbod);

             $('commandPerform').appendChild(tbl);


             $('commandPerform').show();
             cmdoutputtbl=tbl;
             isnew=true;
         }
         if(!runningcmd){
             runningcmd = new Object();
             runningcmd.count=0;
             runningcmd.entries=new Array();
         }
     }catch (e){
         appendCmdOutputError(e);
         return;
     }
     if(data.error){
         appendCmdOutputError(data.error);
         finishedExecution();
         return;
     }

     runningcmd.id = data.id;
     runningcmd.offset = data.dataoffset;
     runningcmd.completed=data.iscompleted;
     runningcmd.jobcompleted=data.jobcompleted;
     runningcmd.jobstatus=data.jobstatus;
     runningcmd.jobcancelled=data.jobcancelled;
     runningcmd.failednodes=data.failednodes;
     runningcmd.percent=data.percentLoaded;
     var entries = $A(data.entries);
     if(null!=data.duration){
         updateDuration(data.duration);
     }
     if(entries!=null && entries.length>0){

         for(var i=0;i<entries.length;i++){
             var e = entries[i];
             runningcmd.entries.push(e);
             genDataRow(e,$('cmdoutputtbl'));
         }
     }

     if(needsScroll && autoscroll && $('commandPerform')){
        if (document.body.scrollHeight) {
          window.scrollTo(0, document.body.scrollHeight);
        }
        else if (screen.height) { // IE5
          window.scrollTo(0, screen.height);
        }
     }

     if( runningcmd.completed && runningcmd.jobcompleted ){
         //halt timer

         if($('viewoptionscomplete') && null != data.totalsize){
            if($('outfilesize')){
                $('outfilesize').innerHTML=data.totalsize + " bytes";
            }
            $('viewoptionscomplete').show();
         }
            if($('taildelaycontrol')){
                $('taildelaycontrol').hide();
            }
         finishDataOutput();
         finishedExecution(runningcmd.jobstatus=='true'?'true':runningcmd.jobcancelled?'cancelled':'failed');
         return;
     }else{
        setTimeout(function(){loadMoreOutput(runningcmd.id, runningcmd.offset);},(tailmode && taildelay > 0) ? taildelay*1000 : 50);
     }
     if(runningcmd.jobcompleted && !runningcmd.completed){
        jobFinishStatus(runningcmd.jobstatus=='true'?'true':runningcmd.jobcancelled?'cancelled':'failed');
        if($('progressContainer')){
            $('progressContainer').hide();
        }
        if($('fileload')){
            $('fileload').show();
            $('fileloadpercent').innerHTML=Math.ceil(runningcmd.percent)+"%";
        }
        if($('fileload2')){
            $('fileload2').show();
            $('fileload2percent').innerHTML=Math.ceil(runningcmd.percent)+"%";
        }
     }
     if(runningcmd.jobcompleted){

         if($('viewoptionscomplete') && null != data.totalsize){
            if($('outfilesize')){
                $('outfilesize').innerHTML=data.totalsize + " bytes";
            }
            $('viewoptionscomplete').show();
         }
            if($('taildelaycontrol')){
                $('taildelaycontrol').hide();
            }
     }

 }
      function finishDataOutput(){
          if(null==lastTBody && null!=cmdoutputtbl && cmdoutputtbl.tBodies.length>0){
              lastTBody=cmdoutputtbl.tBodies[0];
          }
          if(null!=lastTBody && null!=lastTBody.getAttribute('id') && lastTBody.rows.length>0){
          try{
             var lastcell = lastTBody.rows[isAppendTop()?0:lastTBody.rows.length-1];
             lastTBody.removeChild(lastcell);
             var temptbod=document.createElement("tbody");
             temptbod.setAttribute('id','final'+lastTBody.getAttribute('id'));
              if(isAppendTop()){
                cmdoutputtbl.insertBefore(temptbod,lastTBody);  
              }else{
                cmdoutputtbl.appendChild(temptbod);
              }

             temptbod.appendChild(lastcell);
             ctxBodyFinalSet.push(temptbod);
              if(0==lastTBody.rows.length){
                     var expicon=$('ctxExp'+contextIdCounter);
                     if(expicon){
                         expicon.removeClassName('expandicon');
                     }
                     var ctxgrp = $('ctxgroup'+contextIdCounter);

                     if(ctxgrp && ctxgrp.rows.length>0){
                         $(ctxgrp.rows[0]).removeClassName('expandable');
                         $(ctxgrp.rows[0]).removeClassName('action');
                     }
                 }else{

                     var ctxgrp = $('ctxgroup'+contextIdCounter);

                     if(ctxgrp && ctxgrp.rows.length>0){
                         $(ctxgrp.rows[0]).addClassName('expandable');
                         $(ctxgrp.rows[0]).addClassName('action');
                     }
                 }
         }catch(e){
             appendCmdOutputError(e);
         }
          }
          try{
          var ctxid=ctxBodySet.length-1;
        if(null!=$('ctxIcon'+(ctxid))){
            var status=contextStatus[(ctxid)+""];
            var iconname="-small-ok.png";
            if(typeof(status)!="undefined"){
                iconname="-small-"+status+".png";
            }
            var img = document.createElement('img');
             img.setAttribute('alt','');
//                 img.setAttribute('title',status);
             img.setAttribute('width','16');
             img.setAttribute('height','16');
             img.setAttribute('src', iconUrl+iconname);
             img.setAttribute('style','vertical-align:center');
             $('ctxIcon'+(ctxid)).appendChild(img);
        }

         }catch(e){
             appendCmdOutputError(e);
         }
      }
  function toggleDataBody(ctxid) {
      if (Element.visible('databody' + ctxid)) {
          $('databody'+ctxid).hide();
          $('ctxExp'+ctxid).removeClassName('opened');
          $('ctxExp'+ctxid).addClassName('closed');
          if (collapseCtx.value && showFinalLine.value) {
              $('finaldatabody' + ctxid).show();
          } else {
              $('finaldatabody' + ctxid).hide();
          }
      } else {
          $('databody'+ctxid).show();
          $('ctxExp'+ctxid).removeClassName('closed');
          $('ctxExp'+ctxid).addClassName('opened');
          $('finaldatabody' + ctxid).show();
      }


  }
 function loadMoreOutput(id,offset){
     return loadMoreOutputTail(id,offset);
 }

  function loadMoreOutputTail(id,offset){
     var url='${createLink(controller:"execution",action:"tailExecutionOutput")}';
 //    $('commandPerform').innerHTML+="id,offset: "+id+","+offset+"; runningcmd: "+runningcmd.id+","+runningcmd.offset;
     new Ajax.Request(url, {
         parameters: "id="+id+"&offset="+offset + ((tailmode&&lastlines)?"&lastlines="+lastlines : "") <%= "true"==params.disableMarkdown? '+"&disableMarkdown=true"':"" %> ,
         onSuccess: function(transport) {
             appendCmdOutput(transport.responseText);
         },
         onFailure: function() {
             appendCmdOutputError("Error performing request: "+url);
             finishedExecution();
         }
     });
 }
  function reverseOutputTable(tbl) {
      try {
          if (appendtop.changed) {
              //reverse table row order for every table body, then reverse order of all table bodies
              for (var j = 0; j < tbl.tBodies.length; j++) {
                  var parent = tbl.tBodies[j];

                  var rows = $A(parent.rows);
                  var len = rows.length;
                  var first = rows[0];

                  for (var i = 1; i < len; i++) {
                      var curNode = rows[len - i];
                      parent.removeChild(curNode);
                      parent.insertBefore(curNode, first);
                  }
              }
              var parent = tbl;
              var len = tbl.tBodies.length;
              var first = tbl.tBodies[0];
              for (var i = 1; i < len; i++) {
                  var curNode = tbl.tBodies[len - 1];
                  parent.removeChild(curNode);
                  parent.insertBefore(curNode, first);
                  if(1==curNode.rows.length){
                      var row = curNode.rows[0];
                      if($(row).hasClassName('contextRow')){
                        $(row).addClassName(isAppendTop()?"up":"down");
                        $(row).removeClassName(isAppendTop()?"down":"up");
                      }
                  }
              }


              appendtop.changed = false;
          }
      } catch(e) {
          appendCmdOutputError(e);
      }
  }
  function isAtBottom()
    {
        var a=document.documentElement.scrollHeight|| document.body.scrollHeight;
        var b=document.documentElement.scrollTop|| document.body.scrollTop;
        var c=document.documentElement.clientHeight|| document.body.clientHeight;
        return ((a-b)<=c);
    }
 function genDataRow(data,tbl){
     reverseOutputTable(tbl);
     var ctxid=contextIdCounter;
     if(null==lastTBody){
         lastTBody=tbl.tBodies[0];
     }
     if(null==lastrow || lastrow['module']!=data['module'] || lastrow['command']!=data['command'] || lastrow['node']!=data['node'] || lastrow['context']!=data['context']){
         if(null!=lastrow){
             //remove last row and place in new table body
             try{
                 var lastcell = lastTBody.rows[isAppendTop()?0:lastTBody.rows.length-1];
                 lastTBody.removeChild(lastcell);
                 var temptbod=document.createElement("tbody");
                 temptbod.setAttribute('id','final'+lastTBody.getAttribute('id'));
                 if(isAppendTop()){
                    tbl.insertBefore(temptbod,lastTBody);
                 }else{
                    tbl.appendChild(temptbod);
                 }
                 temptbod.appendChild(lastcell);
                 ctxBodyFinalSet.push(temptbod);
                 if(showFinalLine.value){
                     Element.show($(temptbod));
                 }else if(groupOutput.value && collapseCtx.value){
                     Element.hide($(temptbod));
                 }
                 if(0==lastTBody.rows.length){
                     var expicon=$('ctxExp'+contextIdCounter);
                     if(expicon){
                         expicon.removeClassName('expandicon');
                     }
                     var ctxgrp = $('ctxgroup'+contextIdCounter);

                     if(ctxgrp && ctxgrp.rows.length>0){
                         $(ctxgrp.rows[0]).removeClassName('expandable');
                         $(ctxgrp.rows[0]).removeClassName('action');
                     }
                 }else{

                     var ctxgrp = $('ctxgroup'+contextIdCounter);

                     if(ctxgrp && ctxgrp.rows.length>0){
                         $(ctxgrp.rows[0]).addClassName('expandable');
                         $(ctxgrp.rows[0]).addClassName('action');
                     }
                 }
             }catch(e){
                 appendCmdOutputError(e);
             }

            if(null!=$('ctxIcon'+(ctxid))){
                var status=contextStatus[(ctxid)+""];
                var iconname="-small-ok.png";
                if(typeof(status)!="undefined"){
                    iconname="-small-"+status+".png";
                }
                var img = document.createElement('img');
                 img.setAttribute('alt','');
//                 img.setAttribute('title',status);
                 img.setAttribute('width','16');
                 img.setAttribute('height','16');
                 img.setAttribute('src', iconUrl+iconname);
                 img.setAttribute('style','vertical-align:center');
                 $('ctxIcon'+(ctxid)).appendChild(img);
            }
            contextIdCounter++;
         }
         ctxid=contextIdCounter;
         //create new Table body
         var newtbod = $(document.createElement("tbody"));

         newtbod.setAttribute('id','ctxgroup'+ctxid);
         if(isAppendTop()){
            tbl.insertBefore(newtbod,tbl.tBodies[0]);
         }else{
            tbl.appendChild(newtbod);
         }
         ctxGroupSet.push(newtbod);
         if(!groupOutput.value){
             newtbod.hide();
         }


         var tr = $(newtbod.insertRow(isAppendTop()?0:-1));
         var iconcell = $(tr.insertCell(0));
         iconcell.setAttribute('id','ctxIcon'+ctxid);
         tr.addClassName('contextRow');
         if(isAppendTop()){
             tr.addClassName("up");
         }else{
             tr.addClassName("down");
         }
         iconcell.addClassName("icon");
         var cell = $(tr.insertCell(1));
         cell.setAttribute('colSpan','2');
//         cell.colSpan=2;


         if(null!=data['node'] && 'run'!=data['command']){
            cell.innerHTML+="<span class='node'>"+ "<img src='"+"${resource(dir:'images',file:'icon-small-NodeObject.png')}"+"' width='16' height='16' alt=''/> "+data['node']+"</span>";
         }else if (null !=data['node'] && 'run'==data['command'] ){
            cell.innerHTML+="<span class='node'>"+ "<img src='"+"${resource(dir:'images',file:'icon-small-NodeObject.png')}"+"' width='16' height='16' alt=''/> "+data['node']+"</span>";
         }

         if(data['command'] || data['module'] || data['context']){
             if(data['module'] || data['command'] && "run"!=data['command']){
                cell.innerHTML+="<span class='cmdname' title='"+(data['module']?data.module:'Unknown Module')+"-&gt;"+data['command']+"'>"+data['command']+"</span>";
             }else if(data['command'] && "run"==data['command']){
                 cell.innerHTML+="<span class='cmdname' title='"+data['command']+"'>"+data['command']+"</span>";
             }
             if(data['context']){
                 //split context into project,type,object
                 var t = data['context'].split('.');
                 if(t.size()>2){
                     cell.innerHTML+=" <span class='resname'>"+t[2]+"</span>";
                 }
                 if(t.size()>1){
                     cell.innerHTML+=" <span class='typename'>"+t[1]+"</span>";
                 }
//                cell.innerHTML+=" <span class='contextInfo'>("+data['context']+") </span>";
             }
         }else{
            tr.addClassName('console');
            cell.innerHTML+=" <span class='console'>[console]</span>";
         }
         var cell2 = $(tr.insertCell(2));
         cell2.setAttribute('id','ctxExp'+ctxid);
         cell2.addClassName('rowexpicon');
         cell2.addClassName('expandicon');
         tr.onclick = function(){toggleDataBody(ctxid);};

         //create new tablebody for data rows
         var datatbod = $(document.createElement("tbody"));
         if(isAppendTop()){
            tbl.insertBefore(datatbod,newtbod);
         }else{
            tbl.appendChild(datatbod);
         }
         lastTBody=datatbod;
         lastTBody.setAttribute('id','databody'+ctxid);
         ctxBodySet.push(lastTBody);
         if(groupOutput.value && collapseCtx.value){
             Element.hide($(lastTBody));
             cell2.addClassName('closed');
         }else{
             cell2.addClassName('opened');
         }

     }
     var tr = $(lastTBody.insertRow(isAppendTop()?0:-1));

     var tdicon = $(tr.insertCell(0));
     tdicon.setAttribute('width','16');
     tdicon.addClassName('info');
     tdicon.setAttribute('style','vertical-align:top');
     if(data.level =='ERROR' || data.level=='SEVERE'){
         var img = document.createElement('img');
         img.setAttribute('alt',data.level);
         img.setAttribute('title',data.level);
         img.setAttribute('width','16');
         img.setAttribute('height','16');
         img.setAttribute('src', '${resource(dir:"images",file:"icon-small-")}'+data.level.toLowerCase()+'.png');
         tdicon.appendChild(img);
         contextStatus[ctxid]=data.level.toLowerCase();
     }
     var tdtime = $(tr.insertCell(1));
     tdtime.setAttribute('width','20');
     tdtime.addClassName('info');
     tdtime.addClassName('time');
     tdtime.setAttribute('style','vertical-align:top;');
     tdtime.innerHTML="<span class=\""+data.level+"\">"+data.time+"</span>";
     var tddata = $(tr.insertCell(2));
     tddata.addClassName('data');
     tddata.setAttribute('style','vertical-align:top');
     tddata.setAttribute('colspan','2');
     if(null!=data['mesghtml']){
         tddata.innerHTML=data.mesghtml;
         tddata.addClassName('datahtml');
     }else{
         var txt = data.mesg;
         txt  =txt.replace(/[\\\n\\\r]+$/,'')
         txt = txt.replace(/</g,'&lt;')
         txt = txt.replace(/>/g,'&gt;')
         tddata.innerHTML=txt;
     }

     runningcmd.count++;
     lastrow=data;
     return tr;
 }
 function clearCmdOutput(){
     $('commandPerform').innerHTML='';
     cmdoutputtbl=null;
     cmdoutspinner=null;
     runningcmd=null;

     var d2 = document.createElement("div");
     $(d2).addClassName("commandFlowError");
     $(d2).setAttribute("style","display: none;");
     $(d2).setAttribute("id","cmdoutputerror");
     $(d2).hide();

     $('commandPerform').appendChild(d2);
 }
 var isrunning=false;
 function beginExecution(){
     clearCmdOutput();
     $('commandPerform').show();

     displayCompletion(0);
     $('progressContainer').show();
     setOutputAppendTop($F('outputappendtop')=="top");
     setOutputAutoscroll($F('outputautoscrolltrue')=="true");
     setGroupOutput($F('ctxshowgroup')=='true');
     setCollapseCtx($F('ctxcollapse')=="true");
     setShowFinalLine($F('ctxshowlastline')=="true");
     isrunning=true;
 }

 function finishedExecution(result){
     if($('cmdoutspinner')){
        $('cmdoutspinner').remove();
     }
     cmdoutspinner=null;
     isrunning=false;
    if($('progressContainer')){
        displayCompletion(100);
        $('progressContainer').hide();
    }
    if($('fileload')){
        $('fileload').hide();
    }
    if($('fileload2')){
        $('fileload2').hide();
    }
    if(runningcmd.failednodes){
        $('execRetry').show();
    }
    $('execRerun').show();
    jobFinishStatus(result);
 }
  function jobFinishStatus(result){
     if(null!=result && $('runstatus')){
         $('runstatus').innerHTML=  result=='true'?'<span class="succeed">Successful</span>'
            :(result=='cancelled'?'<span class="fail">Killed</span>':'<span class="fail">Failed</span>');
         if($('jobInfo_'+'${execution?.id}')){
            var img=$('jobInfo_'+'${execution?.id}').down('img');
             if(img){
                 var status=result=='true'?'-ok':result=='cancelled'?'-warn':'-error';
                 img.src=iconUrl+'-job'+status+".png";
             }
         }
         if(!/^\[/.test(document.title)){
            document.title = (result=='true'?'[OK] ':result=='cancelled'?'[KILLED] ':'[FAILED] ')+document.title;
         }
         $('cancelresult').hide();
     }
 }
var starttime;
  function beginFollowingOutput(id){
      if(isrunning){
          return false;
      }
      beginExecution();
      starttime=new Date().getTime();
      loadMoreOutput(id,0)
  }
<auth:allowed job="${executionResource}" name="${UserAuth.WF_KILL}">
  var killjobhtml='<span class="action button textbtn" onclick="docancel()">Kill <g:message code="domain.ScheduledExecution.title"/> <img src="${resource(dir:'images',file:'icon-tiny-removex.png')}" alt="Kill" width="12px" height="12px"/></span>';
</auth:allowed>
<auth:allowed job="${executionResource}" name="${UserAuth.WF_KILL}" has="false">
  var killjobhtml="";
</auth:allowed>

  function updatecancel(data){

      var orig=data;
      if(typeof(data) == "string"){
          eval("data="+data);
      }
      if(data['cancelled']){
          if($('cancelresult')){
              $('cancelresult').loading('Killing <g:message code="domain.ScheduledExecution.title"/>...');
          }
      }else{
          if($('cancelresult')){
              $('cancelresult').innerHTML='<span class="fail">'+(data['error']?data['error']:'Failed to Kill <g:message code="domain.ScheduledExecution.title"/>.')+'</span> '+ killjobhtml;
          }
      }
  }

  function docancel(){
      if($('cancelresult')){
              $('cancelresult').loading('Killing <g:message code="domain.ScheduledExecution.title"/>...');
      }
      new Ajax.Request(appLinks.executionCancelExecution, {
          parameters: "id=${execution.id}",
          onSuccess: function(transport) {
              updatecancel(transport.responseText);
          },
          onFailure: function(response) {
              updatecancel({error:"Failed to kill <g:message code="domain.ScheduledExecution.title"/>: "+response.statusText});
          }
      });
  }


  function init(){
      beginFollowingOutput("${execution.id}");
  }

  Event.observe(window, 'load', init);
  var totalDuration = 0 + ${scheduledExecution?.totalTime? scheduledExecution.totalTime : -1 };
  var totalCount = 0 + ${scheduledExecution?.execCount?scheduledExecution.execCount : -1 };

  function updateDuration(duration){
      if(totalCount>0 && totalDuration>=0 && duration>=0){
          var avg = (totalDuration / totalCount);
          if($('execDuration')){
            $('execDuration').innerHTML=duration;
          }
          if($('avgDuration')){
            $('avgDuration').innerHTML=avg;
          }

          if(duration < avg){
              displayCompletion(100*(duration/avg));
          }else{
              displayCompletion(100);
          }
      }else{
          if($('execDuration')){
            $('execDuration').innerHTML=duration;
          }
          if($('avgDuration')){
            $('avgDuration').innerHTML="???";
          }
          $('progressContainer').hide();
      }
  }
      function displayCompletion(pct){
          if($('execDurationPct')){
            $('execDurationPct').innerHTML=pct+"%";
          }
          $('progressBar').style.width=(Math.floor(pct) * 4);
          $('progressBar').innerHTML=(Math.floor(pct))+"%";
      }
</script>
  </head>

  <body>
    <div class="pageTop extra">
        <div class="jobHead">
            <g:render template="/scheduledExecution/showHead" model="[scheduledExecution:scheduledExecution,execution:execution,followparams:[mode:followmode,lastlines:params.lastlines]]"/>
        </div>
        <div class="clear"></div>
    </div>
    <div class="pageBody">

        <table>
            <tr>
                <td>

        <table class="executionInfo">
            <tr>
                <td>User:</td>
                <td>${execution?.user}</td>
            </tr>
            <g:if test="${null!=execution.dateCompleted && null!=execution.dateStarted}">

                <tr>
                    <td>Time:</td>
                    <td><g:relativeDate start="${execution.dateStarted}" end="${execution.dateCompleted}" /></td>
                </tr>
            </g:if>
            <g:if test="${null!=execution.dateStarted}">
            <tr>
                <td>Started:</td>
                <td>
                    <g:relativeDate elapsed="${execution.dateStarted}" agoClass="timeago"/>
                </td>
                <td><span class="timeabs">${execution.dateStarted}</span></td>
            </tr>
            </g:if>
            <g:else>
                <td>Started:</td>
                <td>Just Now</td>
            </g:else>

        <g:if test="${null!=execution.dateCompleted}">
                <tr>
                    <td>Finished:</td>
                    <td>
                        <g:relativeDate elapsed="${execution.dateCompleted}" agoClass="timeago"/>
                    </td>
                    <td><span class="timeabs">${execution.dateCompleted}</span></td>
                </tr>
            </g:if>
        </table>

                </td>
                <g:if test="${scheduledExecution}">
                    <td style="vertical-align:top;" class="toolbar small">
                        <g:render template="/scheduledExecution/actionButtons" model="${[scheduledExecution:scheduledExecution,objexists:objexists,jobAuthorized:jobAuthorized,execPage:true]}"/>
                        <g:set var="lastrun" value="${scheduledExecution.id?Execution.findByScheduledExecutionAndDateCompletedIsNotNull(scheduledExecution,[max: 1, sort:'dateStarted', order:'desc']):null}"/>
                        <g:set var="successcount" value="${scheduledExecution.id?Execution.countByScheduledExecutionAndStatus(scheduledExecution,'true'):0}"/>
                        <g:set var="execCount" value="${scheduledExecution.id?Execution.countByScheduledExecution(scheduledExecution):0}"/>
                        <g:set var="successrate" value="${execCount>0? (successcount/execCount) : 0}"/>
                        <g:render template="/scheduledExecution/showStats" model="[scheduledExecution:scheduledExecution,lastrun:lastrun?lastrun:null, successrate:successrate]"/>
                    </td>
                </g:if>
            </tr>
        </table>

        <g:expander key="schedExDetails${scheduledExecution?.id?scheduledExecution?.id:''}" imgfirst="true">Details</g:expander>
        <div class="presentation" style="display:none" id="schedExDetails${scheduledExecution?.id}">
            <g:render template="execDetails" model="[execdata:execution]"/>

        </div>
    </div>


    <div id="commandFlow" class="commandFlow">
        <table width="100%">
            <tr>
                <td width="50%">

        <g:if test="${null!=execution.dateCompleted}">

                    Status:
                    <span class="${execution.status=='true'?'succeed':'fail'}" >
                        <g:if test="${execution.status=='true'}">
                            Successful
                        </g:if>
                        <g:elseif test="${execution.cancelled}">
                            Killed
                        </g:elseif>
                        <g:else>
                            Failed
                        </g:else>
                    </span>
            </g:if>
            <g:else>
                    Status:

                        <span id="runstatus">
                        <span class="nowrunning">
                        <img src="${resource(dir:'images',file:'icon-tiny-disclosure-waiting.gif')}" alt="Spinner"/>
                        Now Running&hellip;
                        </span>
                        </span>
                    <auth:allowed job="${executionResource}" name="${UserAuth.WF_KILL}">
                        <span id="cancelresult" style="margin-left:10px">
                            <span class="action button textbtn" onclick="docancel();">Kill <g:message code="domain.ScheduledExecution.title"/> <img src="${resource(dir:'images',file:'icon-tiny-removex.png')}" alt="Kill" width="12px" height="12px"/></span>
                        </span>
                    </auth:allowed>

            </g:else>

                    <span id="execRetry" style="${wdgt.styleVisible(if:null!=execution.dateCompleted && null!=execution.failedNodeList)}; margin-right:10px;">
                        <g:if test="${scheduledExecution}">
                            <g:set var="jobRunAuth" value="${ auth.allowedTest(job:executionResource,action:UserAuth.WF_RUN)}"/>
                            <g:set var="canRun" value="${ ( !authMap || authMap[scheduledExecution.id.toString()] ||jobAuthorized ) && jobRunAuth}"/>
                            <g:if test="${ canRun}">
                                <g:link controller="scheduledExecution" action="execute" id="${scheduledExecution.id}" params="${[retryFailedExecId:execution.id]}" title="Run Job on the failed nodes" class="action button" style="margin-left:10px" >
                                    <img src="${resource(dir:'images',file:'icon-small-run.png')}" alt="run" width="16px" height="16px"/>
                                    Retry Failed Nodes  &hellip;
                                </g:link>
                            </g:if>
                        </g:if>
                        <g:else>
                            <g:set var="jobRunAuth" value="${ auth.allowedTest(job:executionResource,action:[UserAuth.WF_CREATE,UserAuth.WF_READ])}"/>
                            <g:set var="canRun" value="${ jobRunAuth}"/>
                            <g:if test="${canRun}">
                                <g:link controller="scheduledExecution" action="createFromExecution" params="${[executionId:execution.id,failedNodes:true]}" class="action button" title="Retry on the failed nodes&hellip;" style="margin-left:10px">
                                    <img src="${resource(dir:'images',file:'icon-small-run.png')}"  alt="run" width="16px" height="16px"/>
                                    Retry Failed Nodes &hellip;
                                </g:link>
                            </g:if>
                        </g:else>
                    </span>
                    <span id="execRerun" style="${wdgt.styleVisible(if:null!=execution.dateCompleted)}" >
                        <g:set var="jobRunAuth" value="${ auth.allowedTest(job:executionResource, action:[UserAuth.WF_CREATE,UserAuth.WF_READ])}"/>
                        <g:if test="${jobRunAuth }">
                            <g:link controller="scheduledExecution" action="createFromExecution" params="${[executionId:execution.id]}" class="action button" title="Rerun or Save this Execution&hellip;" ><img src="${resource(dir:'images',file:'icon-small-run.png')}"  alt="run" width="16px" height="16px"/> Rerun or Save &hellip;</g:link>
                        </g:if>
                    </span>
                </td>
                <td width="50%" >
                    <div id="progressContainer" class="progressContainer" >
                        <div class="progressBar" id="progressBar" title="Progress is an estimate based on average execution time for this ${g.message(code:'domain.ScheduledExecution.title')}.">0%</div>
                    </div>
                </td>
            </tr>
        </table>
    </div>

    <div id="commandPerformOpts" class="outputdisplayopts" style="margin: 0 20px;">
        <form action="#" id="outputappendform">

        <table width="100%">
            <tr>
                <td class="buttonholder" style="padding:10px;">
                    <g:link class="tab ${followmode=='tail'?' selected':''}" style="padding:5px;"
                        title="View the last lines of the output file"
                        controller="execution"  action="show" id="${execution.id}" params="${[lastlines:params.lastlines,mode:'tail'].findAll{it.value}}">Tail Output</g:link>
                    <g:link class="tab ${followmode=='browse'?' selected':''}" style="padding:5px;"
                        title="Load the entire file in grouped contexts "
                        controller="execution"  action="show" id="${execution.id}" params="[mode:'browse']">Browse Output</g:link>
                    
            <span id="fullviewopts" style="${followmode!='browse'?'display:none':''}">
                    <input
                        type="radio"
                        name="outputappend"
                        id="outputappendtop"
                        value="top"
                        style="display: none;"/>
                    <label for="outputappendtop">
                        <span
                        class="action textbtn button"
                        title="Click to change"
                            id="appendTopLabel"
                        onclick="setOutputAppendTop(true);"
                        >Top</span></label>
                    <input
                        type="radio"
                        name="outputappend"
                        id="outputappendbottom"
                        value="bottom"
                        checked="CHECKED"
                        style="display: none;"/>
                    <label
                        for="outputappendbottom">
                        <span
                            class="action textbtn button"
                            title="Click to change"
                            id="appendBottomLabel"
                            onclick="setOutputAppendTop(false);"
                        >Bottom</span></label>
                    <span
                    class="action textbtn button"
                    title="Click to change"
                    id="autoscrollTrueLabel"
                >
                <input
                    type="checkbox"
                    name="outputautoscroll"
                    id="outputautoscrolltrue"
                    value="true"
                    ${followmode=='tail'?'':'checked="CHECKED"'}
                    onclick="setOutputAutoscroll($('outputautoscrolltrue').checked);"
                    style=""/>

                <label for="outputautoscrolltrue">Scroll</label></span>

                <%--
                <input
                    type="radio"
                    name="outputautoscroll"
                    id="outputautoscrollfalse"
                    value="false"
                    style="display:none;"/>
                <label for="outputautoscrollfalse"><span
                    class="action textbtn button"
                    title="Click to change"
                    id="autoscrollFalseLabel"
                    onclick="setOutputAutoscroll(false);"
                >no</span></label>
                --%>
<%--
            </td>
            <td>--%>
                <span class="action textbtn button"
                      title="Click to change"
                      id="ctxshowgroupoption"
                      onclick="setGroupOutput($('ctxshowgroup').checked);">
                <input
                    type="checkbox"
                    name="ctxshowgroup"
                    id="ctxshowgroup"
                    value="true"
                    ${followmode=='tail'?'':'checked="CHECKED"'}
                    style=""/>
                    <label for="ctxshowgroup">Group commands</label>
                </span>
<%--
                </td>

            <td >--%>
                &nbsp;
                <span
                    class="action textbtn button"
                    title="Click to change"
                    id="ctxcollapseLabel"
                    onclick="setCollapseCtx($('ctxcollapse').checked);">
                <input
                    type="checkbox"
                    name="ctxcollapse"
                    id="ctxcollapse"
                    value="true"
                    ${followmode=='tail'?'':null==execution?.dateCompleted?'checked="CHECKED"':''}
                    style=""/>
                    <label for="ctxcollapse">Collapse</label>
                </span>
<%--
            </td>
            <td>--%>
                &nbsp;
                <span class="action textbtn button"
                      title="Click to change"
                      id="ctxshowlastlineoption"
                      onclick="setShowFinalLine($('ctxshowlastline').checked);">
                <input
                    type="checkbox"
                    name="ctxshowlastline"
                    id="ctxshowlastline"
                    value="true"
                    checked="CHECKED"
                    style=""/>
                    <label for="ctxshowlastline">Show final line</label>
                </span>
            </span>
            <g:if test="${followmode=='tail'}">
<%---
                </td>
                <td>--%>
                    Show the last
                    <span class="action textbtn button"
                      title="Click to reduce"
                      onmousedown="modifyLastlines(-5);return false;">-</span>
                <input
                    type="text"
                    name="lastlines"
                    id="lastlinesvalue"
                    value="${params.lastlines?params.lastlines:20}"
                    size="3"
                    onchange="updateLastlines(this.value)"
                    onkeypress="var x= noenter();if(!x){this.blur();};return x;"
                    style=""/>
                    <span class="action textbtn button"
                      title="Click to increase"
                      onmousedown="modifyLastlines(5);return false;">+</span>

                    lines<span id="taildelaycontrol" style="${execution.dateCompleted?'display:none':''}">,
                    and update every


                    <span class="action textbtn button"
                      title="Click to reduce"
                      onmousedown="modifyTaildelay(-1);return false;">-</span>
                <input
                    type="text"
                    name="taildelay"
                    id="taildelayvalue"
                    value="1"
                    size="2"
                    onchange="updateTaildelay(this.value)"
                    onkeypress="var x= noenter();if(!x){this.blur();};return x;"
                    style=""/>
                    <span class="action textbtn button"
                      title="Click to increase"
                      onmousedown="modifyTaildelay(1);return false;">+</span>

                    seconds
                </span>
            </g:if>
                </td>
                <td align="right">
                    <span style="${execution.dateCompleted ? '' : 'display:none'}" class="sepL" id="viewoptionscomplete">
                        <g:link class="action txtbtn" style="padding:5px;"
                            title="Download entire output file" 
                            controller="execution" action="downloadOutput" id="${execution.id}"><img src="${resource(dir:'images',file:'icon-small-file.png')}" alt="Download" title="Download output" width="13px" height="16px"/> Download <span id="outfilesize">${filesize?filesize+' bytes':''}</span></g:link>
                    </span>
                </td>
            </tr>
            </table>
        </form>
    </div>
    <div id="fileload2" style="display:none;" class="outputdisplayopts"><img src="${resource(dir:'images',file:'icon-tiny-disclosure-waiting.gif')}" alt="Spinner"/> Loading Output... <span id="fileloadpercent"></span></div>
    <div
        id="commandPerform"
        style="display:none; margin: 0 20px; "></div>
    <div id="fileload" style="display:none;" class="outputdisplayopts"><img src="${resource(dir:'images',file:'icon-tiny-disclosure-waiting.gif')}" alt="Spinner"/> Loading Output... <span id="fileload2percent"></span></div>
    <div id="log"></div> 

  </body>
</html>


