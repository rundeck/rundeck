/*
 * Copyright 2011 DTO Labs, Inc. (http://dtolabs.com)
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
/**
 * Control execution follow page state for an execution
 */
var FollowControl = Class.create({
    executionId:null,
    targetElement:null,
    cmdoutputtbl: null,
    cmdoutspinner: null,
    runningcmd: null,
    appendtop: {value: false,changed: false},
    collapseCtx: {value:true,changed:false},
    showFinalLine: {value:true,changed:false},
    groupOutput: {value: true},

    lastrow:null,
    contextIdCounter: 0,
    contextStatus: {},

    lastTBody:null,
    ctxBodySet: new Array(),
    ctxBodyFinalSet: new Array(),
    ctxGroupSet: new Array(),

    //node mode
    ctxGroupTbodies:{},

    taildelay: 1,
    isrunning: false,
    starttime:null,
    updatepagetitle:false,

    //instance vars
    extraParams:{},
    totalCount:0,
    totalDuration:0,
    killjobhtml:'',
    execData:{},
    nodemode:false,
    browsemode:false,
    tailmode:false,
    refresh:false,
    lastlines:20,
    maxLastLines: 100,
    iconUrl:'/images/icon',
    appLinks:{},
    
    initialize: function(eid,elem,params){
        this.executionId=eid;
        this.targetElement=elem;
        Object.extend(this,params);
        this.refresh= this.tailmode;
        this._init();
        if(this.dobind){
            this.bindActions(elem);
        }
        this.readyMode();
    },
    _init: function(){
        //clear and reset vars
        this.cmdoutputtbl = null;
        this.lastTBody = null;
        this.ctxBodySet = new Array();
        this.ctxBodyFinalSet = new Array();
        this.ctxGroupSet = new Array();
        this.runningcmd={count : 0, entries : new Array()};
        this.lastrow = null;
        this.contextIdCounter = 0;
        this.contextStatus = new Object();
        //node mode
        this.ctxGroupTbodies={};
    },
    bindActions: function(elem){
        var obj=this;
        $(elem).select('a.out_setmode_tail').each(function(e){
            Event.observe(e,'click',function(evt){Event.stop(evt);obj.setMode('tail');obj.reload();});
        });
        $(elem).select('a.out_setmode_browse').each(function(e){
            Event.observe(e,'click',function(evt){Event.stop(evt);obj.setMode('browse');obj.reload();});
        });
        $(elem).select('a.out_setmode_node').each(function(e){
            Event.observe(e,'click',function(evt){Event.stop(evt);obj.setMode('node');obj.reload();});
        });
        $(elem).select('.opt_append_top_true').each(function(e){
            e.onclick=null;
            Event.observe(e,'click',function(evt){obj.setOutputAppendTop(true);});
        });
        $(elem).select('.opt_append_top_false').each(function(e){
            e.onclick=null;
            Event.observe(e,'click',function(evt){obj.setOutputAppendTop(false);});
        });
        $(elem).select('.opt_auto_scroll_true').each(function(e){
            e.onclick=null;
            Event.observe(e,'click',function(evt){obj.setOutputAutoscroll(e.checked);});
        });
        $(elem).select('.opt_group_output').each(function(e){
            e.onclick=null;
            Event.observe(e,'click',function(evt){obj.setGroupOutput(e.checked);});
        });
        $(elem).select('.opt_collapse_ctx').each(function(e){
            e.onclick=null;
            Event.observe(e,'click',function(evt){obj.setCollapseCtx(e.checked);});
        });
        $(elem).select('.opt_show_final').each(function(e){
            e.onclick=null;
            Event.observe(e,'click',function(evt){obj.setShowFinalLine(e.checked);});
        });
        $(elem).select('.opt_last_lines_dec').each(function(e){
            e.onmousedown=null;
            Event.observe(e,'mousedown',function(evt){Event.stop(evt);obj.modifyLastlines(-5);});
        });
        $(elem).select('.opt_last_lines_inc').each(function(e){
            e.onmousedown=null;
            Event.observe(e,'mousedown',function(evt){Event.stop(evt);obj.modifyLastlines(5);});
        });
        $(elem).select('.opt_last_lines_val').each(function(e){
            e.onchange=null;
            Event.observe(e,'change',function(evt){obj.updateLastlines(e.value);});
        });
        $(elem).select('.opt_update_every_dec').each(function(e){
            e.onmousedown=null;
            Event.observe(e,'mousedown',function(evt){Event.stop(evt);obj.modifyTaildelay(-1);});
        });
        $(elem).select('.opt_update_every_inc').each(function(e){
            e.onmousedown=null;
            Event.observe(e,'mousedown',function(evt){Event.stop(evt);obj.modifyTaildelay(1);});
        });
        $(elem).select('.opt_update_every_val').each(function(e){
            e.onchange=null;
            Event.observe(e,'change',function(evt){obj.updateTaildelay(e.value);});
        });
        $(elem).select('.act_cancel').each(function(e){
            e.onclick=null;
            Event.observe(e,'click',function(evt){obj.docancel();});
        });
    },
    setMode: function(mode){
        this.tailmode=mode=="tail";
        this.browsemode=mode=="browse";
        this.nodemode=mode=="node";
        this.tailmode=this.tailmode||!(this.browsemode||this.nodemode);
        this.refresh=this.tailmode;
        this.readyMode();
    },
    readyTail: function(){
        var obj=this;
        $(this.targetElement).select('.opt_mode_tail').each(Element.show);
        $(this.targetElement).select('.out_setmode_tail').each(function(e){e.addClassName('selected');});

        $(this.targetElement).select('.opt_last_lines_val').each(function(e){
            e.value=obj.lastlines;
        });
        $(this.targetElement).select('.opt_update_every_val').each(function(e){
            e.value=obj.taildelay;
        });
    },
    readyMode: function(){
        var obj=this;
        this.setGroupOutput(this.browsemode||this.nodemode);
        if(this.targetElement && $(this.targetElement)){
            $(this.targetElement).select('.opt_mode').each(Element.hide);
            $(this.targetElement).select('.out_setmode').each(function(e){e.removeClassName('selected')});
            if(this.tailmode){
                this.readyTail();
            }else if(this.browsemode){
                $(this.targetElement).select('.opt_mode_browse').each(Element.show);
                $(this.targetElement).select('.out_setmode_browse').each(function(e){e.addClassName('selected');});
                //set form inputs to reflect state
                $(this.targetElement).select('.opt_append_top_true').each(function(e){
                    if(obj.appendtop.value){
                        e.addClassName('selected');
                    }else{
                        e.removeClassName('selected');
                    }
                });
                $(this.targetElement).select('.opt_append_top_false').each(function(e){
                    if(!obj.appendtop.value){
                        e.addClassName('selected');
                    }else{
                        e.removeClassName('selected');
                    }
                });
                $(this.targetElement).select('.opt_group_output').each(function(e){
                    e.checked=obj.groupOutput.value;
                    if(obj.groupOutput.value){
                        e.up('label').addClassName('selected');
                    }else{
                        e.up('label').removeClassName('selected');
                    }
                });

                $(this.targetElement).select('.opt_collapse_ctx').each(function(e){
                    e.checked=obj.collapseCtx.value;
                    if(obj.collapseCtx.value){
                        e.up('label').addClassName('selected');
                    }else{
                        e.up('label').removeClassName('selected');
                    }
                });

                $(this.targetElement).select('.opt_show_final').each(function(e){
                    e.checked=obj.showFinalLine.value;
                    if(obj.showFinalLine.value){
                        e.up('label').addClassName('selected');
                    }else{
                        e.up('label').removeClassName('selected');
                    }
                });


            }else if(this.nodemode){
                $(this.targetElement).select('.out_setmode_node').each(function(e){e.addClassName('selected');});
            }
        }
    },
    appendCmdOutputError: function (message) {
        if ($('cmdoutputerror')) {
            $("cmdoutputerror").innerHTML += message;
            $("cmdoutputerror").show();
        }
    },
    _log: function(message) {
        if ($('log')) {
            $("log").innerHTML += message + "<br>";
        }
    },

    updateTaildelay: function(val) {
        val = parseInt(val);
        if (isNaN(val)) {
            val = 1;
        }
        if (val > 60) {
            val = 60;
        } else if (val < 0) {
            val = 0;
        }
        this.taildelay = val;
        $('taildelayvalue').value = this.taildelay;

        return false;
    },
    modifyTaildelay: function(val) {
        var oldval = parseInt($('taildelayvalue').value);
        val = parseInt(val);
        oldval = oldval + val;
        this.updateTaildelay(oldval);
    },

    updateLastlines: function(val) {
        val = parseInt(val);
        if (isNaN(val)) {
            val = 20;
        }
        if (val > this.maxLastLines) {
            val = this.maxLastLines;
        } else if (val < 5) {
            val = 5;
        }
        this.lastlines = val;
        $('lastlinesvalue').value = this.lastlines;
        if (!this.isrunning) {
            this.isrunning = true;
            var obj=this;
            setTimeout(function() {
                obj.loadMoreOutput(obj.executionId, 0);
            }, 50);
        }
        return false;
    },
    modifyLastlines: function(val) {
        var oldval = parseInt($('lastlinesvalue').value);
        val = parseInt(val);
        oldval = oldval + val;
        this.updateLastlines(oldval);
        this.readyTail();
    },
    isAppendTop: function() {
        return this.appendtop.value ? true : false;
    },
    setCollapseCtx: function(val) {
        if (this.collapseCtx.value != val) {
            this.collapseCtx.changed = true;
            this.collapseCtx.value = val;
        }

        if (this.collapseCtx.value) {
            this.ctxBodySet._each(Element.hide);
            this.ctxBodyFinalSet._each(this.showFinalLine.value ? Element.show : Element.hide);
            $$('.expandicon').each(function(e) {
                e.addClassName('closed');
                e.removeClassName('opened');
            });
        } else {
            this.ctxBodySet._each(Element.show);
            this.ctxBodyFinalSet._each(Element.show);
            $$('.expandicon').each(function(e) {
                e.removeClassName('closed');
                e.addClassName('opened');
            });
        }
        this.setCtxCollapseDisplay(val);
    },
    setCtxCollapseDisplay:function(val) {
        if ($('ctxcollapseLabel')) {
            if (val) {
                $('ctxcollapseLabel').addClassName('selected');
            } else {
                $('ctxcollapseLabel').removeClassName('selected');
            }
        }
        if ($('ctxshowlastlineoption')) {
            if (val) {
                $('ctxshowlastlineoption').show();
            } else {
                $('ctxshowlastlineoption').hide();
            }
        }
    },

    setGroupOutput:function (val) {
        if (this.groupOutput.value != val) {
            this.groupOutput.value = val;
        }
        this.ctxGroupSet.each(this.groupOutput.value ? Element.show : Element.hide);
        if (this.groupOutput.value && this.collapseCtx.value) {
            this.ctxBodySet.each(Element.hide);
            this.ctxBodyFinalSet.each(this.showFinalLine.value ? Element.show : Element.hide);
        } else {
            this.ctxBodySet.each(Element.show);
            this.ctxBodyFinalSet.each(Element.show);
        }

        if (!this.groupOutput.value) {
            if ($('ctxcollapseLabel')) {
                $('ctxcollapseLabel').hide();
            }
            if ($('ctxshowlastlineoption')) {
                $('ctxshowlastlineoption').hide();
            }

        } else {
            if ($('ctxcollapseLabel')) {
                $('ctxcollapseLabel').show();
            }
            this.setCtxCollapseDisplay(this.collapseCtx.value);
        }
        if ($('ctxshowgroupoption')) {
            if (val) {
                $('ctxshowgroupoption').addClassName('selected');
            } else {
                $('ctxshowgroupoption').removeClassName('selected');
            }
        }
    },
    setShowFinalLine: function(val) {
        if (this.showFinalLine.value != val) {
            this.showFinalLine.changed = true;
            this.showFinalLine.value = val;
        }
        var obj=this;
        this.ctxBodyFinalSet.each(function(elem, ndx) {
            if (!obj.showFinalLine.value && obj.collapseCtx.value && obj.ctxBodySet[ndx] && !Element.visible(obj.ctxBodySet[ndx])) {
                Element.hide(elem);
            } else {
                Element.show(elem);
            }
        });

        if ($('ctxshowlastlineoption')) {
            if (val) {
                $('ctxshowlastlineoption').addClassName('selected');
            } else {
                $('ctxshowlastlineoption').removeClassName('selected');
            }
        }
    },
    setOutputAppendTop: function(istop) {
        if (this.appendtop.value != istop) {
            this.appendtop.changed = !this.appendtop.changed;
        }

        if ($('appendTopLabel')) {

            if (istop) {
                $('appendTopLabel').addClassName('selected');
            } else {
                $('appendTopLabel').removeClassName('selected');
            }
        }
        if ($('appendBottomLabel')) {
            if (istop) {
                $('appendBottomLabel').removeClassName('selected');
            } else {
                $('appendBottomLabel').addClassName('selected');
            }
        }
        this.appendtop.value = istop;

        if (!this.isrunning) {
            this.reverseOutputTable(this.cmdoutputtbl);
        }
    },
    clearTable: function(tbl) {

        if (tbl) {
            $('commandPerform').removeChild(tbl);
            this.cmdoutputtbl = null;
        }
        this._init();
    },

    createTable: function() {
        var tbl = new Element("table");
        tbl.setAttribute("border", "0");
        tbl.setAttribute("width", "100%");
        tbl.setAttribute("height", "auto");
        tbl.setAttribute("cellSpacing", "0");
        tbl.setAttribute("cellPadding", "0");
        tbl.addClassName('execoutput');
        tbl.setAttribute('id', 'cmdoutputtbl');
        var th = tbl.createTHead();
        var thr1 = th.insertRow(-1);
        var thi = new Element("th");
        thi.setAttribute("width", "20px");
        thr1.appendChild(thi);
        var th1 = new Element("th");
        th1.innerHTML = "Time";
        thr1.appendChild(th1);
        var th2 = new Element("th");
        th2.innerHTML = "Message";
        th2.setAttribute('colspan', '2');
        thr1.appendChild(th2);
        var tbod = new Element("tbody");
        tbl.appendChild(tbod);

        $('commandPerform').appendChild(tbl);

        $('commandPerform').show();
        return tbl;
    },
    appendCmdOutput: function(data) {
        var orig = data;
        var needsScroll = false;
        try{
        if (!this.isAppendTop() && this.isAtBottom()) {
            needsScroll = true;
        }
        if (this.refresh && this.cmdoutputtbl) {
            try {
                this.clearTable(this.cmdoutputtbl);
            } catch(e) {
                this._log(e);
            }
        }
        if (typeof(data) == "string" && data == "") {
            return;
        }
        }catch(e){
            this.appendCmdOutputError("appendCmdOutput1 "+e);
            return;
        }
        try {
            if (typeof(data) == "string") {
                eval("data=" + data);
            }
            if (!this.cmdoutputtbl) {
                this.cmdoutputtbl = this.createTable();
            }
            if (!this.runningcmd) {
                this.runningcmd = new Object();
                this.runningcmd.count = 0;
                this.runningcmd.entries = new Array();
            }
        } catch (e) {
            this.appendCmdOutputError("appendCmdOutput,eval "+e);
            return;
        }
        if (data.error) {
            this.appendCmdOutputError("data error "+data.error);
            this.finishedExecution();
            return;
        }

        this.runningcmd.id = data.id;
        this.runningcmd.offset = data.offset;
        this.runningcmd.completed = data.completed;
        this.runningcmd.jobcompleted = data.execCompleted;
        this.runningcmd.jobstatus = data.execState;
        this.runningcmd.failednodes = data.hasFailedNodes;
        this.runningcmd.percent = data.percentLoaded;

        var entries = $A(data.entries);
        if (null != data.duration) {
            this.updateDuration(data.duration);
        }
        if (entries != null && entries.length > 0) {

            for (var i = 0 ; i < entries.length ; i++) {
                var e = entries[i];
                this.runningcmd.entries.push(e);
                this.genDataRow(e, this.cmdoutputtbl);

            }
        }


        if (this.runningcmd.completed && this.runningcmd.jobcompleted) {
            //halt timer

            if ($('viewoptionscomplete') && null != data.totalSize) {
                if ($('outfilesize')) {
                    $('outfilesize').innerHTML = data.totalSize + " bytes";
                }
                $('viewoptionscomplete').show();
            }
            if ($('taildelaycontrol')) {
                $('taildelaycontrol').hide();
            }
            this.finishDataOutput();
            this.finishedExecution(this.runningcmd.jobstatus);
            return;
        } else {
            var obj=this;
            setTimeout(function() {
                obj.loadMoreOutput(obj.runningcmd.id, obj.runningcmd.offset);
            }, (this.tailmode && this.taildelay > 0) ? this.taildelay * 1000 : 50);
        }
        if (this.runningcmd.jobcompleted && !this.runningcmd.completed) {
            this.jobFinishStatus(this.runningcmd.jobstatus);
            if ($('progressContainer')) {
                $('progressContainer').hide();
            }
            if ($('fileload')) {
                $('fileload').show();
                $('fileloadpercent').innerHTML = Math.ceil(this.runningcmd.percent) + "%";
            }
            if ($('fileload2')) {
                $('fileload2').show();
                $('fileload2percent').innerHTML = Math.ceil(this.runningcmd.percent) + "%";
            }
        }
        if (this.runningcmd.jobcompleted) {

            if ($('viewoptionscomplete') && null != data.totalSize) {
                if ($('outfilesize')) {
                    $('outfilesize').innerHTML = data.totalSize + " bytes";
                }
                $('viewoptionscomplete').show();
            }
            if ($('taildelaycontrol')) {
                $('taildelaycontrol').hide();
            }
        }

    },
    finishDataOutput: function() {
        if (null == this.lastTBody && null != this.cmdoutputtbl && this.cmdoutputtbl.tBodies.length > 0) {
            this.lastTBody = this.cmdoutputtbl.tBodies[0];
        }
        if (null != this.lastTBody && null != this.lastTBody.getAttribute('id') && this.lastTBody.rows.length > 0) {
            try {
                var lastcell = this.lastTBody.rows[this.isAppendTop() ? 0 : this.lastTBody.rows.length - 1];
                this.lastTBody.removeChild(lastcell);
                var temptbod = new Element("tbody");
                temptbod.setAttribute('id', 'final' + this.lastTBody.getAttribute('id'));
                if (this.isAppendTop()) {
                    this.cmdoutputtbl.insertBefore(temptbod, this.lastTBody);
                } else {
                    this.cmdoutputtbl.appendChild(temptbod);
                }

                temptbod.appendChild(lastcell);
                this.ctxBodyFinalSet.push(temptbod);
                if (0 == this.lastTBody.rows.length) {
                    var expicon = $('ctxExp' + this.contextIdCounter);
                    if (expicon) {
                        expicon.removeClassName('expandicon');
                    }
                    var ctxgrp = $('ctxgroup' + this.contextIdCounter);

                    if (ctxgrp && ctxgrp.rows.length > 0) {
                        $(ctxgrp.rows[0]).removeClassName('expandable');
                        $(ctxgrp.rows[0]).removeClassName('action');
                    }
                } else {

                    var ctxgrp = $('ctxgroup' + this.contextIdCounter);

                    if (ctxgrp && ctxgrp.rows.length > 0) {
                        $(ctxgrp.rows[0]).addClassName('expandable');
                        $(ctxgrp.rows[0]).addClassName('action');
                    }
                }
            } catch(e) {
                this.appendCmdOutputError("finishDataOutput"+e);
            }
        }
        try {
            var ctxid = this.ctxBodySet.length - 1;
            if (null != $('ctxIcon' + (ctxid))) {
                var status = this.contextStatus[(ctxid) + ""];
                var iconname = "-small-ok.png";
                if (typeof(status) != "undefined") {
                    iconname = "-small-" + status + ".png";
                }
                var img = new Element('img');
                img.setAttribute('alt', '');
                //                 img.setAttribute('title',status);
                img.setAttribute('width', '16');
                img.setAttribute('height', '16');
                img.setAttribute('src', this.iconUrl + iconname);
                img.setAttribute('style', 'vertical-align:center');
                $('ctxIcon' + (ctxid)).appendChild(img);
            }

        } catch(e) {
            this.appendCmdOutputError("finishDataOutput2"+e);
        }
    },
    toggleDataBody: function(ctxid) {
        if (Element.visible('databody' + ctxid)) {
            $('databody' + ctxid).hide();
            $('ctxExp' + ctxid).removeClassName('opened');
            $('ctxExp' + ctxid).addClassName('closed');
            if ($('finaldatabody' + ctxid)) {
                if (this.collapseCtx.value && this.showFinalLine.value) {
                    $('finaldatabody' + ctxid).show();
                } else {
                    $('finaldatabody' + ctxid).hide();
                }
            }
        } else {
            $('databody' + ctxid).show();
            $('ctxExp' + ctxid).removeClassName('closed');
            $('ctxExp' + ctxid).addClassName('opened');
            if ($('finaldatabody' + ctxid)) {
                $('finaldatabody' + ctxid).show();
            }
        }


    },
    loadMoreOutput: function(id, offset) {
        return this.loadMoreOutputTail(id, offset);
    },

    loadMoreOutputTail: function(id, offset) {
        var url = this.appLinks.tailExecutionOutput;
        //    $('commandPerform').innerHTML+="id,offset: "+id+","+offset+"; runningcmd: "+this.runningcmd.id+","+this.runningcmd.offset;
        var obj=this;
        if(this.isrunning){
            new Ajax.Request(url, {
                parameters: "id=" + id + "&offset=" + offset + ((this.tailmode && this.lastlines) ? "&lastlines=" + this.lastlines : "")
                    + this.extraParams ,
                onSuccess: function(transport) {
                    obj.appendCmdOutput(transport.responseText);
//                        obj.appendCmdOutputError("loadMoreOutputTail "+e);
                },
                onFailure: function() {
                    obj.appendCmdOutputError("Error performing request (loadMoreOutputTail): " + url);
                    obj.finishedExecution();
                }
            });
        }else if(this._stop){
            this._stop=null;
            if(typeof(this._onStopCallback)=='function'){
                var cb=this._onStopCallback;
                this._onStopCallback=null;
                cb();
            }
        }
    },
    reverseOutputTable: function(tbl) {
        try {
            if (this.appendtop.changed) {
                //reverse table row order for every table body, then reverse order of all table bodies
                for (var j = 0 ; j < tbl.tBodies.length ; j++) {
                    var parent = tbl.tBodies[j];

                    var rows = $A(parent.rows);
                    var len = rows.length;
                    var first = rows[0];

                    for (var i = 1 ; i < len ; i++) {
                        var curNode = rows[len - i];
                        parent.removeChild(curNode);
                        parent.insertBefore(curNode, first);
                    }
                }
                var parent = tbl;
                var len = tbl.tBodies.length;
                var first = tbl.tBodies[0];
                for (var i = 1 ; i < len ; i++) {
                    var curNode = tbl.tBodies[len - 1];
                    parent.removeChild(curNode);
                    parent.insertBefore(curNode, first);
                    if (1 == curNode.rows.length) {
                        var row = curNode.rows[0];
                        if ($(row).hasClassName('contextRow')) {
                            $(row).addClassName(this.isAppendTop() ? "up" : "down");
                            $(row).removeClassName(this.isAppendTop() ? "down" : "up");
                        }
                    }
                }


                this.appendtop.changed = false;
            }
        } catch(e) {
            this.appendCmdOutputError("reverseOutputTable "+e);
        }
    },
    isAtBottom: function()
    {
        var a = document.documentElement.scrollHeight || document.body.scrollHeight;
        var b = document.documentElement.scrollTop || document.body.scrollTop;
        var c = document.documentElement.clientHeight || document.body.clientHeight;
        return ((a - b) <= c);
    },
    genDataRowNodes: function(data, tbl) {
        this.reverseOutputTable(tbl);
        var node = data.node;
        if (!node) {
            node = this.execData.node;
        }
        var tbody;
        if (!this.ctxGroupTbodies[node]) {
            tbody = this.createNewNodeTbody(data, tbl, node);
            this.ctxGroupTbodies[node] = tbody;
        } else {
            tbody = this.ctxGroupTbodies[node];
        }

        var tr = $(tbody.insertRow(-1));
        this.configureDataRow(tr, data, node);
        if ($('ctxCount' + node)) {
            $('ctxCount' + node).innerHTML = '' + tbody.rows.length + " lines";
            if (data.level == 'ERROR' || data.level == 'SEVERE') {
                $('ctxCount' + node).addClassName(data.level);
            }
        }
        this.runningcmd.count++;
        this.lastrow = data;
        return tr;
    },
    createNewNodeTbody: function(data, tbl, ctxid) {
        //create new Table body
        var newtbod = new Element("tbody");

        newtbod.setAttribute('id', 'ctxgroup' + ctxid);
        if (this.isAppendTop()) {
            tbl.insertBefore(newtbod, tbl.tBodies[0]);
        } else {
            tbl.appendChild(newtbod);
        }
        this.ctxGroupSet.push(newtbod);

        var tr = $(newtbod.insertRow(this.isAppendTop() ? 0 : -1));
        var iconcell = $(tr.insertCell(0));
        iconcell.setAttribute('id', 'ctxIcon' + ctxid);
        tr.addClassName('contextRow');
        if (this.isAppendTop()) {
            tr.addClassName("up");
        } else {
            tr.addClassName("down");
        }
        iconcell.addClassName("icon");
        var cell = $(tr.insertCell(1));
        cell.setAttribute('colSpan', '2');


        if (null != data['node'] && 'run' != data['command']) {
            cell.innerHTML +=
            "<span class='node'>" + "<img src='" + AppImages.iconSmallNodeObject + "' width='16' height='16' alt=''/> "
                + data['node'] + "</span>";
        } else if (null != data['node'] && 'run' == data['command']) {
            cell.innerHTML +=
            "<span class='node'>" + "<img src='" + AppImages.iconSmallNodeObject + "' width='16' height='16' alt=''/> "
                + data['node'] + "</span>";
        }

        if (data['command'] || data['module'] || data['context']) {
            if (data['module'] || data['command'] && "run" != data['command']) {
                cell.innerHTML +=
                "<span class='cmdname' title='" + data['command'] + "'>" + data['command'] + "</span>";
            } else if (data['command'] && "run" == data['command']) {
                cell.innerHTML +=
                "<span class='cmdname' title='" + data['command'] + "'>" + data['command'] + "</span>";
            }
            if (data['context']) {
                //split context into project,type,object
                var t = data['context'].split('.');
                if (t.size() > 2) {
                    cell.innerHTML += " <span class='resname'>" + t[2] + "</span>";
                }
                if (t.size() > 1) {
                    cell.innerHTML += " <span class='typename'>" + t[1] + "</span>";
                }
            }
        } else {
            tr.addClassName('console');
            cell.innerHTML += " <span class='console'>[console]</span>";
        }
        var countspan = new Element('span');
        countspan.setAttribute('id', 'ctxCount' + ctxid);
        countspan.setAttribute('count', '0');
        countspan.addClassName('ctxcounter');
        countspan.innerHTML = " -";
        cell.appendChild(countspan);
        var cell2 = $(tr.insertCell(2));
        cell2.setAttribute('id', 'ctxExp' + ctxid);
        cell2.addClassName('rowexpicon');
        cell2.addClassName('expandicon');
        var obj=this;
        tr.onclick = function() {
            obj.toggleDataBody(ctxid);
        };

        //create new tablebody for data rows
        var datatbod = new Element("tbody");
        datatbod.setAttribute('id', 'databody' + ctxid);
        tbl.appendChild(datatbod);

        //start all data tbody as closed
        Element.hide($(datatbod));
        cell2.addClassName('closed');

        return datatbod;
    },

    createFinalContextTbody: function(data, tbl, ctxid) {
        //remove last row and place in new table body
        try {
            var lastcell = this.lastTBody.rows[this.isAppendTop() ? 0 : this.lastTBody.rows.length - 1];
            this.lastTBody.removeChild(lastcell);
            var temptbod = new Element("tbody");
            temptbod.setAttribute('id', 'final' + this.lastTBody.getAttribute('id'));
            if (this.isAppendTop()) {
                tbl.insertBefore(temptbod, this.lastTBody);
            } else {
                tbl.appendChild(temptbod);
            }
            temptbod.appendChild(lastcell);
            this.ctxBodyFinalSet.push(temptbod);
            if (this.showFinalLine.value) {
                Element.show($(temptbod));
            } else if (this.groupOutput.value && this.collapseCtx.value) {
                Element.hide($(temptbod));
            }
            if (0 == this.lastTBody.rows.length) {
                var expicon = $('ctxExp' + this.contextIdCounter);
                if (expicon) {
                    expicon.removeClassName('expandicon');
                }
                var ctxgrp = $('ctxgroup' + this.contextIdCounter);

                if (ctxgrp && ctxgrp.rows.length > 0) {
                    $(ctxgrp.rows[0]).removeClassName('expandable');
                    $(ctxgrp.rows[0]).removeClassName('action');
                }
            } else {

                var ctxgrp = $('ctxgroup' + this.contextIdCounter);

                if (ctxgrp && ctxgrp.rows.length > 0) {
                    $(ctxgrp.rows[0]).addClassName('expandable');
                    $(ctxgrp.rows[0]).addClassName('action');
                }
            }
        } catch(e) {
            this.appendCmdOutputError("createFinalContextTbody "+e);
        }

        if (null != $('ctxIcon' + (ctxid))) {
            var status = this.contextStatus[(ctxid) + ""];
            var iconname = "-small-ok.png";
            if (typeof(status) != "undefined") {
                iconname = "-small-" + status + ".png";
            }
            var img = new Element('img');
            img.setAttribute('alt', '');
            //                 img.setAttribute('title',status);
            img.setAttribute('width', '16');
            img.setAttribute('height', '16');
            img.setAttribute('src', this.iconUrl + iconname);
            img.setAttribute('style', 'vertical-align:center');
            $('ctxIcon' + (ctxid)).appendChild(img);
        }
        this.contextIdCounter++;
    },
    createNewContextTbody: function(data, tbl, ctxid) {
        //create new Table body
        var newtbod = new Element("tbody");

        newtbod.setAttribute('id', 'ctxgroup' + ctxid);
        if (this.isAppendTop()) {
            tbl.insertBefore(newtbod, tbl.tBodies[0]);
        } else {
            tbl.appendChild(newtbod);
        }
        this.ctxGroupSet.push(newtbod);
        if (!this.groupOutput.value) {
            newtbod.hide();
        }


        var tr = $(newtbod.insertRow(this.isAppendTop() ? 0 : -1));
        var iconcell = $(tr.insertCell(0));
        iconcell.setAttribute('id', 'ctxIcon' + ctxid);
        tr.addClassName('contextRow');
        if (this.isAppendTop()) {
            tr.addClassName("up");
        } else {
            tr.addClassName("down");
        }
        iconcell.addClassName("icon");
        var cell = $(tr.insertCell(1));
        cell.setAttribute('colSpan', '2');
        //         cell.colSpan=2;


        if (null != data['node'] && 'run' != data['command']) {
            cell.innerHTML +=
            "<span class='node'>" + "<img src='" + AppImages.iconSmallNodeObject + "' width='16' height='16' alt=''/> "
                + data['node'] + "</span>";
        } else if (null != data['node'] && 'run' == data['command']) {
            cell.innerHTML +=
            "<span class='node'>" + "<img src='" + AppImages.iconSmallNodeObject + "' width='16' height='16' alt=''/> "
                + data['node'] + "</span>";
        }

        if (data['command'] || data['module'] || data['context']) {
            if (data['module'] || data['command'] && "run" != data['command']) {
                cell.innerHTML +=
                "<span class='cmdname' title='" + data['command'] + "'>" + data['command'] + "</span>";
            } else if (data['command'] && "run" == data['command']) {
                cell.innerHTML +=
                "<span class='cmdname' title='" + data['command'] + "'>" + data['command'] + "</span>";
            }
            if (data['context']) {
                //split context into project,type,object
                var t = data['context'].split('.');
                if (t.size() > 2) {
                    cell.innerHTML += " <span class='resname'>" + t[2] + "</span>";
                }
                if (t.size() > 1) {
                    cell.innerHTML += " <span class='typename'>" + t[1] + "</span>";
                }
                //                cell.innerHTML+=" <span class='contextInfo'>("+data['context']+") </span>";
            }
        } else {
            tr.addClassName('console');
            cell.innerHTML += " <span class='console'>[console]</span>";
        }
        var cell2 = $(tr.insertCell(2));
        cell2.setAttribute('id', 'ctxExp' + ctxid);
        cell2.addClassName('rowexpicon');
        cell2.addClassName('expandicon');
        var obj=this;
        tr.onclick = function() {
            obj.toggleDataBody(ctxid);
        };

        //create new tablebody for data rows
        var datatbod = new Element("tbody");
        if (this.isAppendTop()) {
            tbl.insertBefore(datatbod, newtbod);
        } else {
            tbl.appendChild(datatbod);
        }
        this.lastTBody = datatbod;
        this.lastTBody.setAttribute('id', 'databody' + ctxid);
        this.ctxBodySet.push(this.lastTBody);
        if (this.groupOutput.value && this.collapseCtx.value) {
            Element.hide($(this.lastTBody));
            cell2.addClassName('closed');
        } else {
            cell2.addClassName('opened');
        }
    },

    /**
     * create data row for the table, depending on type of output mode
     * @param data
     * @param tbl
     */
    genDataRow: function(data, tbl) {
        if (this.nodemode) {
            return this.genDataRowNodes(data, tbl);
        } else {
            return this.genDataRowBrowse(data, tbl);
        }
    },

    /**
     * Generate the data row for tail/browse mode
     * @param data
     * @param tbl
     */
    genDataRowBrowse: function(data, tbl) {
        this.reverseOutputTable(tbl);
        var ctxid = this.contextIdCounter;
        if (null == this.lastTBody) {
            this.lastTBody = tbl.tBodies[0];
        }
        if (null == this.lastrow || this.lastrow['module'] != data['module'] || this.lastrow['command'] != data['command']
            || this.lastrow['node'] != data['node'] || this.lastrow['context'] != data['context']) {
            if (null != this.lastrow) {
                this.createFinalContextTbody(data, tbl, ctxid);
            }
            ctxid = this.contextIdCounter;
            this.createNewContextTbody(data, tbl, ctxid);

        }
        var tr = $(this.lastTBody.insertRow(this.isAppendTop() ? 0 : -1));
        this.configureDataRow(tr, data, ctxid);

        this.runningcmd.count++;
        this.lastrow = data;
        return tr;
    },

    configureDataRow: function(tr, data, ctxid) {

        var tdicon = $(tr.insertCell(0));
        tdicon.setAttribute('width', '16');
        tdicon.addClassName('info');
        tdicon.setAttribute('style', 'vertical-align:top');
        if (data.level == 'ERROR' || data.level == 'SEVERE') {
            var img = new Element('img');
            img.setAttribute('alt', data.level);
            img.setAttribute('title', data.level);
            img.setAttribute('width', '16');
            img.setAttribute('height', '16');
            img.setAttribute('src', AppImages.iconSmallPrefix + data.level.toLowerCase() + '.png');
            tdicon.appendChild(img);
            this.contextStatus[ctxid] = data.level.toLowerCase();
        }
        var tdtime = $(tr.insertCell(1));
        tdtime.setAttribute('width', '20');
        tdtime.addClassName('info');
        tdtime.addClassName('time');
        tdtime.setAttribute('style', 'vertical-align:top;');
        tdtime.innerHTML = "<span class=\"" + data.level + "\">" + data.time + "</span>";
        var tddata = $(tr.insertCell(2));
        tddata.addClassName('data');
        tddata.setAttribute('style', 'vertical-align:top');
        tddata.setAttribute('colspan', '2');
        if (null != data['loghtml']) {
            tddata.innerHTML = data.loghtml;
            tddata.addClassName('datahtml');
        } else {
            var txt = data.log;
            txt = txt.replace(/[\\\n\\\r]+$/, '');
            txt = txt.replace(/</g, '&lt;');
            txt = txt.replace(/>/g, '&gt;');
            tddata.innerHTML = txt;
        }
    },
    clearCmdOutput: function() {
        $('commandPerform').innerHTML = '';
        this.cmdoutputtbl = null;
        this.cmdoutspinner = null;
        this.runningcmd = null;

        var d2 = new Element("div");
        $(d2).addClassName("commandFlowError");
        $(d2).setAttribute("style", "display: none;");
        $(d2).setAttribute("id", "cmdoutputerror");
        $(d2).hide();

        $('commandPerform').appendChild(d2);
    },
    beginExecution: function() {
        this.clearCmdOutput();
        $('commandPerform').show();

        this.displayCompletion(0);
        $('progressContainer').show();
//        this.setOutputAppendTop($F('outputappendtop') == "top");
//        this.setOutputAutoscroll($F('outputautoscrolltrue') == "true");
//        this.setGroupOutput($F('ctxshowgroup') == 'true');
//        this.setCollapseCtx($F('ctxcollapse') == "true");
//        this.setShowFinalLine($F('ctxshowlastline') == "true");
        this.isrunning = true;
    },

    finishedExecution: function(result) {
        if ($('cmdoutspinner')) {
            $('cmdoutspinner').remove();
        }
        this.cmdoutspinner = null;
        this.isrunning = false;
        if ($('progressContainer')) {
            this.displayCompletion(100);
            $('progressContainer').hide();
        }
        if ($('fileload')) {
            $('fileload').hide();
        }
        if ($('fileload2')) {
            $('fileload2').hide();
        }
        if (this.runningcmd.failednodes && $('execRetry')) {
            $('execRetry').show();
        }
        $('execRerun').show();
        if(typeof(this.onComplete)=='function'){
            this.onComplete();
        }
        this.jobFinishStatus(result);
    },
    jobFinishStatus: function(result) {
        if (null != result && $('runstatus')) {
            $('runstatus').innerHTML = result == 'succeeded' ? '<span class="succeed">Successful</span>'
                : (result == 'aborted' ? '<span class="fail">Killed</span>' : '<span class="fail">Failed</span>');
            $$('.execstatus').each(function(e){
                e.innerHTML = result == 'succeeded' ? '<span class="succeed">Successful</span>'
                : (result == 'aborted' ? '<span class="fail">Killed</span>' : '<span class="fail">Failed</span>');
            });
            if ($('jobInfo_' + this.executionId)) {
                var img = $('jobInfo_' + this.executionId).down('img');
                if (img) {
                    var status = result == 'succeeded' ? '-ok' : result == 'aborted' ? '-warn' : '-error';
                    img.src = this.iconUrl + '-job' + status + ".png";
                }
            }
            if (this.updatepagetitle && !/^\[/.test(document.title)) {
                document.title =
                (result == 'succeeded' ? '[OK] ' : result == 'aborted' ? '[KILLED] ' : '[FAILED] ') + document.title;
            }
            $('cancelresult').hide();
        }
    },
    beginFollowingOutput: function(id) {
        if (this.isrunning) {
            return false;
        }
        this.beginExecution();
        this.starttime = new Date().getTime();
        this.loadMoreOutput(id, 0);
    },
    stopFollowingOutput: function(callback){
        if(this.isrunning){
            this._onStopCallback=callback;
            this._stop=true;
            this.isrunning=false;
        }else if(typeof(callback)=='function'){
            callback();
        }
    },
    reload: function(){
        var obj=this;
        this.stopFollowingOutput(function(){
            obj.clearTable(obj.cmdoutputtbl);
            obj.beginFollowingOutput(obj.executionId);
        });
    },
    updatecancel: function(data) {

        var orig = data;
        if (typeof(data) == "string") {
            eval("data=" + data);
        }
        if (data['cancelled']) {
            if ($('cancelresult')) {
                $('cancelresult').loading('Killing Job...');
            }
        } else {
            if ($('cancelresult')) {
                $('cancelresult').innerHTML =
                '<span class="fail">' + (data['error'] ? data['error'] : 'Failed to Kill Job.') + '</span> '
                    + this.killjobhtml;
            }
        }
    },

    docancel: function() {
        if ($('cancelresult')) {
            $('cancelresult').loading('Killing Job...');
        }
        var obj=this;
        new Ajax.Request(this.appLinks.executionCancelExecution, {
            parameters: {id:this.executionId},
            onSuccess: function(transport) {
                obj.updatecancel(transport.responseText);
            },
            onFailure: function(response) {
                obj.updatecancel({error:"Failed to kill Job: " + response.statusText});
            }
        });
    },


    updateDuration: function(duration) {
        if (this.totalCount > 0 && this.totalDuration >= 0 && duration >= 0) {
            var avg = (this.totalDuration / this.totalCount);
            if ($('execDuration')) {
                $('execDuration').innerHTML = duration;
            }
            if ($('avgDuration')) {
                $('avgDuration').innerHTML = avg;
            }

            if (duration < avg) {
                this.displayCompletion(100 * (duration / avg));
            } else {
                this.displayCompletion(100);
            }
        } else {
            if ($('execDuration')) {
                $('execDuration').innerHTML = duration;
            }
            if ($('avgDuration')) {
                $('avgDuration').innerHTML = "???";
            }
            $('progressContainer').hide();
        }
    },
    displayCompletion: function(pct) {
        if ($('execDurationPct')) {
            $('execDurationPct').innerHTML = pct + "%";
        }
        $('progressBar').style.width = (Math.floor(pct) * 4);
        $('progressBar').innerHTML = (Math.floor(pct)) + "%";
    }
});
