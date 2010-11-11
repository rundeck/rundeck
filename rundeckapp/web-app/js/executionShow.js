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
var cmdoutputtbl = null;
var cmdoutspinner = null;
var runningcmd = null;
var appendtop = new Object();
appendtop.value = false;
appendtop.changed = false;
var collapseCtx = new Object();
collapseCtx.value = true;
collapseCtx.changed = false;
var showFinalLine = new Object();
showFinalLine.value = true;
showFinalLine.changed = false;
var groupOutput = new Object();
groupOutput.value = true;
var autoscroll = true;

var lastrow;
var contextIdCounter = 0;
var contextStatus = new Object();

var lastTBody;
var ctxBodySet = new Array();
var ctxBodyFinalSet = new Array();
var ctxGroupSet = new Array();
var ctxGroupMap={};

var taildelay = 1;
var reloadedIndex = -1;
var isrunning = false;
var starttime;

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
      setTimeout(function(){loadMoreOutput(executionId, 0);},50);
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

function createTable(){
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
    return tbl;
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

             cmdoutputtbl=createTable();
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
     var url=applinks.executionTailExecutionOutput;
 //    $('commandPerform').innerHTML+="id,offset: "+id+","+offset+"; runningcmd: "+runningcmd.id+","+runningcmd.offset;
     new Ajax.Request(url, {
         parameters: "id="+id+"&offset="+offset + ((tailmode&&lastlines)?"&lastlines="+lastlines : "") +extraParams ,
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
function genDataRowNodes(data,tbl){
    
}

function createFinalContextTbody(data,tbl,ctxid){
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
function createNewContextTbody(data,tbl,ctxid){
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
        cell.innerHTML+="<span class='node'>"+ "<img src='"+AppImages.iconSmallNodeObject+"' width='16' height='16' alt=''/> "+data['node']+"</span>";
     }else if (null !=data['node'] && 'run'==data['command'] ){
        cell.innerHTML+="<span class='node'>"+ "<img src='"+AppImages.iconSmallNodeObject+"' width='16' height='16' alt=''/> "+data['node']+"</span>";
     }

     if(data['command'] || data['module'] || data['context']){
         if(data['module'] || data['command'] && "run"!=data['command']){
            cell.innerHTML+="<span class='cmdname' title='"+data['command']+"'>"+data['command']+"</span>";
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
/**
 * Generate the data row for tail/browse mode
 * @param data
 * @param tbl
 */
function genDataRow(data,tbl){
    reverseOutputTable(tbl);
     var ctxid=contextIdCounter;
     if(null==lastTBody){
         lastTBody=tbl.tBodies[0];
     }
     if(null==lastrow || lastrow['module']!=data['module'] || lastrow['command']!=data['command'] || lastrow['node']!=data['node'] || lastrow['context']!=data['context']){
         if(null!=lastrow){
             createFinalContextTbody(data,tbl,ctxid);
         }
         ctxid=contextIdCounter;
         createNewContextTbody(data,tbl,ctxid);

     }
     var tr = $(lastTBody.insertRow(isAppendTop()?0:-1));
     configureDataRow(tr,data);

     runningcmd.count++;
     lastrow=data;
     return tr;
 }
function configureDataRow(tr,data){

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
         img.setAttribute('src', AppImages.iconSmallPrefix+data.level.toLowerCase()+'.png');
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
         txt  =txt.replace(/[\\\n\\\r]+$/,'');
         txt = txt.replace(/</g,'&lt;');
         txt = txt.replace(/>/g,'&gt;');
         tddata.innerHTML=txt;
     }
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
         if($('jobInfo_'+executionId)){
            var img=$('jobInfo_'+executionId).down('img');
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
  function beginFollowingOutput(id){
      if(isrunning){
          return false;
      }
      beginExecution();
      starttime=new Date().getTime();
      loadMoreOutput(id,0);
  }

  function updatecancel(data){

      var orig=data;
      if(typeof(data) == "string"){
          eval("data="+data);
      }
      if(data['cancelled']){
          if($('cancelresult')){
              $('cancelresult').loading('Killing Job...');
          }
      }else{
          if($('cancelresult')){
              $('cancelresult').innerHTML='<span class="fail">'+(data['error']?data['error']:'Failed to Kill Job.')+'</span> '+ killjobhtml;
          }
      }
  }

  function docancel(){
      if($('cancelresult')){
              $('cancelresult').loading('Killing Job...');
      }
      new Ajax.Request(appLinks.executionCancelExecution, {
          parameters: {id:executionId},
          onSuccess: function(transport) {
              updatecancel(transport.responseText);
          },
          onFailure: function(response) {
              updatecancel({error:"Failed to kill Job: "+response.statusText});
          }
      });
  }


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
