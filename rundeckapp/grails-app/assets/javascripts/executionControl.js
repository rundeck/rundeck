/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//= require util/compactMapList
/**
 * Control execution follow page state for an execution
 */
var FollowControl = Class.create({
    parentElement:null,
    executionId:null,
    fileloadId:null,
    fileloadPctId:null,
    fileloadProgressId:null,
    viewoptionsCompleteId:null,
    cmdOutputErrorId:null,
    outfileSizeId:null,
    autoscroll:true,
    targetElement:null,
    cmdoutputtbl: null,
    cmdoutspinner: null,
    runningcmd: null,
    finishedExecutionAction: true,
    appendtop: null,
    collapseCtx: null,
    showFinalLine: null,
    groupOutput: null,
    colTime:null,
    colNode:null,
    colStep:null,
    lineCount:0,
    lastrow:null,
    contextIdCounter: 0,
    contextStatus: null,

    lastTBody:null,
    ctxBodySet: null,
    ctxBodyFinalSet: null,
    ctxGroupSet: null,

    //node mode
    ctxGroupTbodies:null,

    taildelay: 1,
    isrunning: false,
    starttime:null,
    updatepagetitle:false,

    //instance vars
    extraParams:null,
    totalCount:0,
    totalDuration:0,
    killjobhtml:'',
    killjobauth: false,
    execData:null,
    nodemode:false,
    browsemode:false,
    tailmode:false,
    refresh:false,
    truncateToTail:false,
    lastlines:20,
    maxLastLines: 500,
    iconUrl:'/images/icon',
    smallIconUrl:'/images/icon-small',
    appLinks: null,
    workflow:null,
    multiworkflow:null,
    clusterExec: null,
    showClusterExecWarning: true,

    initialize: function(eid,elem,params){
        this.executionId=eid;
        this.targetElement=elem;
        jQuery.extend(this,{
            appendtop: {value: false, changed: false},
            collapseCtx: {value: true, changed: false},
            showFinalLine: {value: true, changed: false},
            groupOutput: {value: true},
            colTime: {value: true},
            colNode: {value: true},
            colStep: {value: true},
            ctxBodySet: new Array(),
            ctxBodyFinalSet: new Array(),
            ctxGroupSet: new Array(),
            ctxGroupTbodies: {},
            contextStatus: {},
            extraParams: {},
            execData: {},
            appLinks: {}
        });
        jQuery.extend(this,params);
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
        if(!elem){
            return;
        }
        $(elem).select('a.out_setmode_tail').each(function(e){
            Event.observe(e,'click',function(evt){Event.stop(evt);
                if(!obj.nodemode){
                    obj.setMode('tail');
                    obj.setGroupOutput(false);
                }else{
                    obj.setMode('tail');
                    obj.reload();
                }
            });
        });
        $(elem).select('a.out_setmode_browse').each(function(e){
            Event.observe(e,'click',function(evt){Event.stop(evt);
                if (!obj.nodemode) {
                    obj.setMode('browse');
                    obj.setGroupOutput(true);
                } else {
                    obj.setMode('browse');
                    obj.reload();
                }
            });
        });
        $(elem).select('a.out_setmode_node').each(function(e){
            Event.observe(e,'click',function(evt){Event.stop(evt);obj.setMode('node');obj.reload();});
        });
        $(elem).select('.out_setmode_toggle').each(function(e){
            Event.observe(e,'change',function(evt){
                Event.stop(evt);
                obj.setMode(e.down('input').checked?'node':'tail');
                obj.reload();
            });
        });
        $(elem).select('.log-wrap-toggle').each(function (e) {
            Event.observe(e, 'change', function (evt) {
                Event.stop(evt);
                obj.setLogWrap(e.down('input').checked ? true : false);
            });
        });
        $(elem).select('.opt_append_top_true').each(function(e){
            e.onclick=null;
            Event.observe(e,'click',function(evt){obj.setOutputAppendTop(true);});
        });
        $(elem).select('.opt_append_top_false').each(function(e){
            e.onclick=null;
            Event.observe(e,'click',function(evt){obj.setOutputAppendTop(false);});
        });
        $(elem).select('.opt_display_col_time').each(function (e) {
            e.onclick = null;
            Event.observe(e, 'click', function (evt) {
                obj.setColTime(e.checked);
            });
        });
        $(elem).select('.opt_display_col_node').each(function (e) {
            e.onclick = null;
            Event.observe(e, 'click', function (evt) {
                obj.setColNode(e.checked);
            });
        });
        $(elem).select('.opt_display_col_step').each(function (e) {
            e.onclick = null;
            Event.observe(e, 'click', function (evt) {
                obj.setColStep(e.checked);
            });
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
    setLogWrap: function (wrapped) {
        "use strict";
        if ($(this.cmdoutputtbl)) {
            if (wrapped) {
                jQuery($(this.cmdoutputtbl)).removeClass('no-wrap');
            } else {
                jQuery($(this.cmdoutputtbl)).addClass('no-wrap');
            }
        }
    },
    readyTail: function(){
        var obj=this;
        $(this.targetElement).select('.opt_mode_tail').each(Element.show);
        $(this.targetElement).select('.out_setmode_tail').each(function(e){
            var li=$(e).up('li');
            if(li){
                li.addClassName('active');
            }
        });

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
            $(this.targetElement).select('.obs_node_false').each(!this.nodemode?Element.show:Element.hide);
            $(this.targetElement).select('.obs_node_true').each(this.nodemode?Element.show:Element.hide);
            $(this.targetElement).select('.opt_mode').each(Element.hide);
            $(this.targetElement).select('.out_setmode').each(function(e){
                e.removeClassName('active');

                var li = $(e).up('li');
                if (li) {
                    li.removeClassName('active');
                }
            });
            if(this.tailmode){
                this.readyTail();
            }else if(this.browsemode){
                $(this.targetElement).select('.opt_mode_browse').each(Element.show);
                $(this.targetElement).select('.out_setmode_browse').each(function(e){e.addClassName('active');});
                //set form inputs to reflect state
                $(this.targetElement).select('.opt_append_top_true').each(function(e){
                    if(obj.appendtop.value){
                        e.addClassName('active');
                    }else{
                        e.removeClassName('active');
                    }
                });
                $(this.targetElement).select('.opt_append_top_false').each(function(e){
                    if(!obj.appendtop.value){
                        e.addClassName('active');
                    }else{
                        e.removeClassName('active');
                    }
                });
                $(this.targetElement).select('.opt_group_output').each(function(e){
                    e.checked=obj.groupOutput.value;
                    if(obj.groupOutput.value){
                        e.up('label').addClassName('active');
                    }else{
                        e.up('label').removeClassName('active');
                    }
                });

                $(this.targetElement).select('.opt_collapse_ctx').each(function(e){
                    e.checked=obj.collapseCtx.value;
                    if(obj.collapseCtx.value){
                        e.up('label').addClassName('active');
                    }else{
                        e.up('label').removeClassName('active');
                    }
                });

                $(this.targetElement).select('.opt_show_final').each(function(e){
                    e.checked=obj.showFinalLine.value;
                    if(obj.showFinalLine.value){
                        e.up('label').addClassName('active');
                    }else{
                        e.up('label').removeClassName('active');
                    }
                });


            }else if(this.nodemode){
                $(this.targetElement).select('.out_setmode_node').each(function(e){
                    var li = $(e).up('li');
                    if (li) {
                        li.addClassName('active');
                    }
                });
            }
        }
    },
    appendCmdOutputError: function (message) {
        if ($(this.cmdOutputErrorId)) {
            appendText($(this.cmdOutputErrorId),message);
            $(this.cmdOutputErrorId).show();
        }
    },
    _log: function(message) {
        if ($('log')) {
            appendText($("log"), message);
            appendHtml($("log"), "<br>");
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
    getLineCount: function() {
        return this.lineCount;
    },
    setCollapseCtx: function(val) {
        if (this.collapseCtx.value != val) {
            this.collapseCtx.changed = true;
            this.collapseCtx.value = val;
        }

        if (this.collapseCtx.value) {
            this.ctxBodySet._each(Element.hide);
            this.ctxBodyFinalSet._each(this.showFinalLine.value ? Element.show : Element.hide);
            $$('.expandicon,tr.contextRow').each(function(e) {
                e.addClassName('closed');
                e.removeClassName('opened');
            });
        } else {
            this.ctxBodySet._each(Element.show);
            this.ctxBodyFinalSet._each(Element.show);
            $$('.expandicon,tr.contextRow').each(function(e) {
                e.removeClassName('closed');
                e.addClassName('opened');
            });
        }
        this.setCtxCollapseDisplay(val);
    },
    setCtxCollapseDisplay:function(val) {
        if ($('ctxcollapseLabel')) {
            if (val) {
                $('ctxcollapseLabel').addClassName('active');
            } else {
                $('ctxcollapseLabel').removeClassName('active');
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
            if ($(this.cmdoutputtbl)) {
                this.setColTime(this.colTime.value);
                this.setColNode(this.colNode.value);
                this.setColStep(this.colStep.value);
            }
            if ($('ctxcollapseLabel')) {
                $('ctxcollapseLabel').hide();
            }
            if ($('ctxshowlastlineoption')) {
                $('ctxshowlastlineoption').hide();
            }

        } else {
            if ($(this.cmdoutputtbl)) {
                $(this.cmdoutputtbl).removeClassName('collapse_time');
                $(this.cmdoutputtbl).addClassName('collapse_node');
                $(this.cmdoutputtbl).addClassName('collapse_stepnum');
            }
            if ($('ctxcollapseLabel')) {
                $('ctxcollapseLabel').show();
            }
            this.setCtxCollapseDisplay(this.collapseCtx.value);
        }
        $$('.obs_grouped_true').each(val?Element.show:Element.hide);
        $$('.obs_grouped_false').each(!val ? Element.show : Element.hide);
        if ($('ctxshowgroupoption')) {
            if (val) {
                $('ctxshowgroupoption').addClassName('active');
            } else {
                $('ctxshowgroupoption').removeClassName('active');
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
                $('ctxshowlastlineoption').addClassName('active');
            } else {
                $('ctxshowlastlineoption').removeClassName('active');
            }
        }
    },
    setColTime: function (show) {
        if ($(this.cmdoutputtbl)) {

            if (show) {
                $(this.cmdoutputtbl).removeClassName('collapse_time');
            } else {
                $(this.cmdoutputtbl).addClassName('collapse_time');
            }
        }

        this.colTime.value = show;
    },
    setColNode: function (show) {

        if ($(this.cmdoutputtbl)) {

            if (show) {
                $(this.cmdoutputtbl).removeClassName('collapse_node');
            } else {
                $(this.cmdoutputtbl).addClassName('collapse_node');
            }
        }

        this.colNode.value = show;
    },
    setColStep: function (show) {

        if ($(this.cmdoutputtbl)) {

            if (show) {
                $(this.cmdoutputtbl).removeClassName('collapse_stepnum');
            } else {
                $(this.cmdoutputtbl).addClassName('collapse_stepnum');
            }
        }

        this.colStep.value = show;
    },
    setOutputAppendTop: function(istop) {
        if (this.appendtop.value != istop) {
            this.appendtop.changed = !this.appendtop.changed;
        }

        if ($('appendTopLabel')) {

            if (istop) {
                $('appendTopLabel').addClassName('active');
            } else {
                $('appendTopLabel').removeClassName('active');
            }
        }
        if ($('appendBottomLabel')) {
            if (istop) {
                $('appendBottomLabel').removeClassName('active');
            } else {
                $('appendBottomLabel').addClassName('active');
            }
        }
        this.appendtop.value = istop;

        if (!this.isrunning) {
            this.reverseOutputTable(this.cmdoutputtbl);
        }
    },
    clearTable: function(tbl) {

        if (tbl) {
            $(this.parentElement).removeChild(tbl);
            this.cmdoutputtbl = null;
        }
        this._init();
    },

    createTable: function(id) {
        var tbl = new Element("table");
        tbl.setAttribute("border", "0");
        tbl.setAttribute("width", "100%");
        tbl.setAttribute("height", "auto");
        tbl.setAttribute("cellSpacing", "0");
        tbl.setAttribute("cellPadding", "0");
        tbl.addClassName('execoutput');
        if(id){
            tbl.setAttribute('id', id);
        }
        if(!this.tailmode){
            $(tbl).addClassName('collapse_node');
            $(tbl).addClassName('collapse_stepnum');
        }

        var tbod = new Element("tbody");
        tbl.appendChild(tbod);

        $(this.parentElement).appendChild(tbl);

        $(this.parentElement).show();
        return tbl;
    },
    showLoading:function(message,percent){
        if (this.fileloadId && $(this.fileloadId)) {
            $(this.fileloadId).show();
            setText($(this.fileloadPctId), (message!=null ? message : ''));
            if(percent!=null && $(this.fileloadProgressId)){
                $(this.fileloadProgressId).show();
                $(this.fileloadProgressId).down('.progress-bar').style.width=percent+'%';
            }
            if(percent){
                setText($(this.fileloadPctId),(message != null ? message : '')+percent+'%');
            }
        }
    },
    hideLoading:function(){
        if (this.fileloadId && $(this.fileloadId)) {
            $(this.fileloadId).hide();
        }
    },
    appendCmdOutput: function(data) {
        var needsScroll = false;
        if (!this.isAppendTop() && this.isAtBottom() && this.autoscroll) {
            needsScroll = true;
        }

        if (this.refresh && this.cmdoutputtbl && data.lastlinesSupported && this.truncateToTail){
            try {
                this.clearTable(this.cmdoutputtbl);
            } catch (e) {
                this._log(e);
            }
        }
        if (!this.cmdoutputtbl) {
            this.cmdoutputtbl = this.createTable(this.tableId);
            this.setColNode(this.colNode.value);
            this.setColStep(this.colStep.value);
            this.setColTime(this.colTime.value);
        }
        if (!this.runningcmd) {
            this.runningcmd = {};
            this.runningcmd.count = 0;
            this.runningcmd.entries = [];
        }
        if (data.error) {
            this.appendCmdOutputError(data.error);
            this.finishedExecution();
            if(this.runningcmd.count===0){
                //hide table header
                $(this.cmdoutputtbl).hide();
            }
            $(this.viewoptionsCompleteId).hide();
            return;
        }
        this.clusterExec = data.clusterExec && data.serverNodeUUID || null;

        this.runningcmd.id = data.id;
        this.runningcmd.offset = data.offset;
        this.runningcmd.completed = data.completed;
        this.runningcmd.jobcompleted = data.execCompleted;
        this.runningcmd.jobstatus = data.execState;
        this.runningcmd.statusString = data.statusString;
        this.runningcmd.failednodes = data.hasFailedNodes;
        this.runningcmd.percent = data.percentLoaded;
        this.runningcmd.pending = data.pending;

        var entries = $A(data.entries);
        //if tail mode, count number of rows
        var rowcount= this.countTableRows(this.cmdoutputtbl);
        var compacted = data.compacted;
        var compactedAttr = data.compactedAttr;
        if (entries != null && entries.length > 0) {
            var tr;
            var self=this;
            var eachEntry = function (e) {
                "use strict";
                //this.runningcmd.entries.push(e);
                tr=self.genDataRow(e, self.cmdoutputtbl);
                //if tail mode and count>last lines, remove 1 row from top
                rowcount++;
            };
            if (compacted) {
                _decompactMapList(entries, compactedAttr, eachEntry);
            } else {
                for (var i = 0; i < entries.length; i++) {
                    eachEntry(entries[i]);
                }
            }
            if (this.refresh && rowcount > this.lastlines && !data.lastlinesSupported && this.truncateToTail) {
                //remove extra lines
                this.removeTableRows(this.cmdoutputtbl, rowcount- this.lastlines);
            }
            if(needsScroll && !this.runningcmd.jobcompleted){
                this.scrollToBottom();
            }
        }
        this.lineCount+=entries.length;

        if (typeof(this.onAppend) == 'function') {
            this.onAppend();
        }
        if (this.clusterExec && this.showClusterExecWarning) {
            if (!this.runningcmd.completed) {
                //show cluster loading info
                jQuery('#' + $(this.parentElement).identify() + '_clusterinfo').show();
            } else {
                jQuery('#' + $(this.parentElement).identify() + '_clusterinfo').hide();
            }
        }

        if (this.runningcmd.completed && this.runningcmd.jobcompleted) {
            //halt timer

            if ($(this.viewoptionsCompleteId) && null != data.totalSize) {
                if ($(this.outfileSizeId)) {
                    setText($(this.outfileSizeId),data.totalSize + " bytes");
                }
                $(this.viewoptionsCompleteId).show();
            }
            this.finishDataOutput();
            this.finishedExecution(this.runningcmd.jobstatus,this.runningcmd.statusString);
            return;
        } else {
            var obj=this;
            var time= (this.tailmode && this.taildelay > 0) ? this.taildelay * 1000 : 50;
            if(this.runningcmd.pending != null){
                time= (this.tailmode && this.taildelay > 0) ? this.taildelay * 5000 : 5000
            }
            if (data.retryBackoff) {
                time = Math.max(data.retryBackoff,time);
            }
            setTimeout(function() {
                obj.loadMoreOutput(obj.runningcmd.id, obj.runningcmd.offset);
            }, time);
        }
        if (this.runningcmd.jobcompleted && !this.runningcmd.completed) {
            this.jobFinishStatus(this.runningcmd.jobstatus,this.runningcmd.statusString);
            var message=null;
            var percent=null;
            if(this.runningcmd.percent!=null){
                percent= Math.ceil(this.runningcmd.percent);
                message= "Loading Output... ";
            } else if (this.runningcmd.pending != null) {
                message = this.runningcmd.pending;
            }
            this.showLoading(message,percent);
        }else if (!this.runningcmd.jobcompleted && !this.runningcmd.completed) {
            //pending a remote load
            if (this.runningcmd.pending != null) {
                this.showLoading(this.runningcmd.pending);
            }else {
                this.hideLoading();
            }
        }
        if (this.runningcmd.jobcompleted) {

            if (this.viewoptionsCompleteId && $(this.viewoptionsCompleteId) && null != data.totalSize) {
                if ($(this.outfileSizeId)) {
                    setText($(this.outfileSizeId), data.totalSize + " bytes");
                }
                $(this.viewoptionsCompleteId).show();
            }
        }

    },
    finishDataOutput: function() {

        if (typeof(this.onLoadComplete) == 'function') {
            this.onLoadComplete();
        }
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
//                    if (expicon) {
//                        expicon.removeClassName('expandicon');
//                    }
                    var ctxgrp = $('ctxgroup' + this.contextIdCounter);

                    if (ctxgrp && ctxgrp.rows.length > 0) {
//                        $(ctxgrp.rows[0]).removeClassName('expandable');
//                        $(ctxgrp.rows[0]).removeClassName('action');
                        $(ctxgrp.rows[0]).addClassName('expandable');
                        $(ctxgrp.rows[0]).addClassName('action');
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
        if(this.lineCount == 0) {
            //show empty message
            jQuery('#' + $(this.parentElement).identify() + '_empty').show();
        }
    },
    toggleDataBody: function(ctxid) {
        if (Element.visible('databody' + ctxid)) {
            $('databody' + ctxid).hide();
            $('ctxExp' + ctxid).removeClassName('opened');
            $('ctxExp' + ctxid).addClassName('closed');
            $('ctxExp' + ctxid).up('tr.contextRow').removeClassName('opened');
            $('ctxExp' + ctxid).up('tr.contextRow').addClassName('closed');
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
            $('ctxExp' + ctxid).up('tr.contextRow').removeClassName('closed');
            $('ctxExp' + ctxid).up('tr.contextRow').addClassName('opened');
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
        var obj = this;
        if(this.isrunning){
            var params = {offset: offset, maxlines: this.maxLastLines};
            if (id) {
                params.id = id;
            }
            if (this.tailmode && this.lastlines && this.truncateToTail && offset == 0) {
                params.lastlines = this.lastlines;
            }
            params.compacted = 'true';
            return jQuery.ajax({
                url: _genUrl(url, params) + this.extraParams,
                type: 'post',
                dataType: 'json',
                success: function (data, status, xhr) {
                    obj.appendCmdOutput(data);
                },
                error: function (xhr, status, err) {
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
    countTableRows: function(tbl){
        var count=0;
        //count rows for every table body
        for (var j = 0; j < tbl.tBodies.length; j++) {
            for (var k = 0; k < tbl.tBodies[j].rows.length ; k++) {
                if (!$(tbl.tBodies[j].rows[0]).hasClassName('contextRow')) {
                    count++;
                }
            }
        }
        return count;
    },
    removeTableRows: function(tbl,x){
        var count=x;
        //count rows for every table body
        for (var j = 0; j < tbl.tBodies.length && count>0; j++) {
            console.log("tbody " + j + ", original length: " + tbl.tBodies[j].rows.length);
            for(var k=0;k<tbl.tBodies[j].rows.length && count>0;k++){
                var row= tbl.tBodies[j].rows[k];
                if(!$(row).hasClassName('contextRow')){
                    tbl.tBodies[j].removeChild(row);
                    count--;
                    k--;
                }
            }
            console.log("tbody " + j + ", new length: " + tbl.tBodies[j].rows.length);

            if (tbl.tBodies[j].rows.length == 1 && $(tbl.tBodies[j].rows[0]).hasClassName('contextRow')) {
                tbl.removeChild(tbl.tBodies[j]);
                j--;
            }
        }
        console.log("removeTableRows, final count: "+count);
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
        return ((a - b) <= (c*1.1));
    },
    scrollToBottom: function()
    {
        window.scrollTo(0, document.documentElement.scrollHeight || document.body.scrollHeight);
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
            setText($('ctxCount' + node), '' + tbody.rows.length + " lines");
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
        $(tr).addClassName('expandable');
        $(tr).addClassName('action');
        iconcell.addClassName("icon");
        var cell = $(tr.insertCell(1));
        cell.setAttribute('colSpan', '4');


        if (null != data['node'] && '' != data['node']) {
            var sp = new Element('span');
            sp.addClassName('node');
            setText(sp,data['node']);
            cell.appendChild(sp);
        }

        if ( data['stepctx'] && this.workflow) {
            var contextstr= this.workflow.renderContextString(data['stepctx']);
        } else {
            tr.addClassName('console');
            appendHtml(cell," <span class='console'>[console]</span>");
        }
        var countspan = new Element('span');
        countspan.setAttribute('id', 'ctxCount' + ctxid);
        countspan.setAttribute('count', '0');
        countspan.addClassName('ctxcounter');
        setText(countspan, " -");
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
        tr.addClassName('closed');

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
//                if (expicon) {
//                    expicon.removeClassName('expandicon');
//                }
                var ctxgrp = $('ctxgroup' + this.contextIdCounter);

                if (ctxgrp && ctxgrp.rows.length > 0) {
                    $(ctxgrp.rows[0]).addClassName('expandable');
                    $(ctxgrp.rows[0]).addClassName('action');
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


        if (null != data['node'] && '' != data['node']) {
            var sp = new Element('span');
            sp.addClassName('node');
            setText(sp, data['node']);
            cell.appendChild(sp);
        }

        if (data['stepctx'] && this.workflow) {
            var contextstr = this.workflow.renderContextString(data['stepctx']);
            var stepnum = this.workflow.renderContextStepNumber(data['stepctx']);

            var sp = new Element('span');
            sp.addClassName('stepnum');
            sp.title=contextstr;
            setText(sp,contextstr);
            cell.appendChild(sp);
            var sp2 = new Element('span');
            sp2.addClassName('stepident');
            setText(sp, contextstr);
            cell.appendChild(sp2);
            //if dynamic step info available load dynamically
            if(this.multiworkflow){
                this.multiworkflow.getStepInfoForStepctx(data['stepctx'],function(info){
                    "use strict";
                    setText(sp2,info.stepident());
                });
            }
        } else {
            tr.addClassName('console');
            appendHtml(cell," <span class='console'>[console]</span>");
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
            tr.addClassName('closed');
        } else {
            cell2.addClassName('opened');
            tr.addClassName('opened');
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
        if (null == this.lastrow  || this.lastrow['stepctx'] != data['stepctx']
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

        if (data.level == 'ERROR' || data.level == 'SEVERE') {
            this.contextStatus[ctxid] = data.level.toLowerCase();
        }
        var tdtime = $(tr.insertCell(0));
        //tdtime.setAttribute('width', '20');
        tdtime.addClassName('info');
        tdtime.addClassName('time');
        var timespan = new Element('span');
        timespan.addClassName(data.level);
        setText(timespan,data.time);
        tdtime.appendChild(timespan);
        if(data.absolute_time){
            if(typeof(moment)=='function'){
                setText(timespan, MomentUtil.formatTime(data.absolute_time,'HH:mm:ss'));
            }
            tdtime.setAttribute('title', data.absolute_time);
        }
        var cellndx=1;
        var colspan="2";
        var tdnode=$(tr.insertCell(cellndx));
        cellndx++;
        tdnode.addClassName('node');
        var shownode=false;
        if (this.lastrow && typeof(this.lastrow['node'])!=undefined && data.node==this.lastrow['node']){
            tdnode.addClassName('repeat');
            tr.addClassName('node-repeat');
        }else if (!data.node) {
            tdnode.addClassName('empty');
            shownode = true;
            tr.addClassName('node-empty');
        } else{
            tdnode.setAttribute('title', data.node);
            setText(tdnode, data.node);
            shownode=true;
            tr.addClassName('node-new');
        }

        //add context column
        var tdctx = $(tr.insertCell(cellndx));
        cellndx++;
        tdctx.addClassName('stepnum');
        if (!shownode && this.lastrow && this.lastrow['stepctx'] == data['stepctx'] ) {
//                tdctx.addClassName('repeat');
        }else if(data['stepctx'] && this.workflow){

            var stepNumText = this.workflow.renderContextStepNumber(data['stepctx']);
            var cmdtext= stepNumText + " " + this.workflow.renderContextString(data['stepctx']);
            var icon= new Element('i');
            icon.addClassName('rdicon icon-small '+ this.workflow.contextType(data['stepctx']));
            tdctx.appendChild(icon);
            tdctx.appendChild(document.createTextNode(" "+cmdtext));
            tdctx.setAttribute('title', data['stepctx']);
            if(this.multiworkflow){
                var td = jQuery(tdctx);
                var stepinfo=this.multiworkflow.getStepInfoForStepctx(data['stepctx']);
                td.empty();
                td.attr('title',null);
                td.attr('data-bind',"template: {name: 'step-info-extended', data:$data, as: 'stepinfo'}");
                ko.applyBindings(stepinfo,td[0]);
            }
        }
        var tddata = $(tr.insertCell(cellndx));
        tddata.addClassName('data');
        tddata.setAttribute('colspan', colspan);
        if (null != data['loghtml']) {
            setHtml(tddata,data.loghtml);
            tddata.addClassName('datahtml log_'+ data.level.toLowerCase());
        } else {
            var txt = data.log;
            if(txt==''){
                txt="\n";
            }
            setText(tddata,txt);
            tddata.addClassName('log_'+data.level.toLowerCase());
        }
    },
    clearCmdOutput: function() {
        clearHtml($(this.parentElement));
        this.cmdoutputtbl = null;
        this.cmdoutspinner = null;
        this.runningcmd = null;

        var d2 = new Element("div");
        $(d2).addClassName("commandFlowError");
        $(d2).setAttribute("style", "display: none;");
        $(d2).setAttribute("id", "cmdoutputerror");
        $(d2).hide();

        $(this.parentElement).appendChild(d2);
    },
    beginExecution: function() {
        this.clearCmdOutput();
        $(this.parentElement).show();

//        this.setOutputAppendTop($F('outputappendtop') == "top");
//        this.setOutputAutoscroll($F('outputautoscrolltrue') == "true");
//        this.setGroupOutput($F('ctxshowgroup') == 'true');
//        this.setCollapseCtx($F('ctxcollapse') == "true");
//        this.setShowFinalLine($F('ctxshowlastline') == "true");
        this.isrunning = true;
    },

    finishedExecution: function(result,statusString) {
        if(!this.finishedExecutionAction){
            return;
        }
        if ($('cmdoutspinner')) {
            $('cmdoutspinner').remove();
        }
        this.cmdoutspinner = null;
        this.isrunning = false;
        if (this.fileloadId && $(this.fileloadId)) {
            $(this.fileloadId).hide();
        }

        this.jobFinishStatus(result,statusString);
        if (typeof(this.onComplete) == 'function') {
            this.onComplete();
        }
    },
    jobFinishStatus: function(result,statusString) {
        if (null != result) {
            if($('runstatus')){
                setHtml($('runstatus'), result == 'succeeded' ? '<span class="exec-status succeed">Succeeded</span>'
                    : (result == 'aborted' ? '<span class="exec-status warn">Killed</span>'
                    : '<span class="exec-status fail">Failed</span>'));
            }
            $$('.execstatus').each(function(e){
                setHtml(e, result == 'succeeded' ? '<span class="exec-status succeed">Succeeded</span>'
                : (result == 'aborted' ? '<span class="exec-status warn">Killed</span>'
                    : '<span class="exec-status fail">Failed</span>'));
            });
            if ($('jobInfo_' + this.executionId)) {
                var icon = $('jobInfo_' + this.executionId).down('.exec-status.icon');
                if (icon) {
                    var status = result == 'succeeded' ? 'succeed' :
                        result == 'aborted' ? 'warn' :
                        result == 'timedout' ? 'timedout' :
                        result == 'failed-with-retry' ? 'retry' :
                        result == 'failed' ? 'fail' :
                            'other';
                    ['succeed', 'fail', 'warn', 'running','retry','timedout','other'].each(function (s) {
                        $(icon).removeClassName(s);
                    });
                    $(icon).addClassName(status);
                }
            }
            if (this.updatepagetitle) {
                var prefix = (
                    result == 'succeeded' ?
                        '✅ [OK] ' :
                        result == 'aborted' ?
                            '✖︎ [KILLED] ' :
                            result == 'timedout' ?
                                '⏱︎ [TIMEOUT] ' :
                                result == 'failed' ?
                                    '⛔︎ [FAILED] ' :
                                    ('✴️ [' + (result) + '] ')//🔶
                );
                if (!document.title.startsWith(prefix)) {
                    document.title = prefix + document.title;
                }
            }
            if($('cancelresult')){
                $('cancelresult').hide();
            }
        }
    },
    beginFollowingOutput: function(id) {
        if (this.isrunning || this.runningcmd && this.runningcmd.completed) {
            return false;
        }
        this.beginExecution();
        this.starttime = new Date().getTime();
        this.lineCount=0;
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

    },

    docancel: function() {
        var obj=this;
        return jQuery.ajax({
            type: 'POST',
            url: this.appLinks.executionCancelExecution,
            dataType:'json',
            data: {id: this.executionId},
            beforeSend: _createAjaxSendTokensHandler('exec_cancel_token'),
            success: function (data,status,jqxhr) {
                obj.updatecancel(data);
            },
            error: function (jqxhr,status,err) {
                obj.updatecancel({error: "Failed to kill Job: " + (jqxhr.responseJSON && jqxhr.responseJSON.error? jqxhr.responseJSON.error: err)});
            }
        }).success(_createAjaxReceiveTokensHandler('exec_cancel_token'));
    },

    doincomplete: function() {
        var obj=this;
        return jQuery.ajax({
            type: 'POST',
            url: this.appLinks.executionMarkExecutionIncomplete,
            dataType:'json',
            data: {id: this.executionId},
            beforeSend: _createAjaxSendTokensHandler('exec_cancel_token'),
            success: function (data,status,jqxhr) {
                obj.updatecancel(data);
            },
            error: function (jqxhr,status,err) {
                obj.updatecancel({error: "Failed to mark Job as incomplete: " + (jqxhr.responseJSON && jqxhr.responseJSON.error? jqxhr.responseJSON.error: err)});
            }
        }).success(_createAjaxReceiveTokensHandler('exec_cancel_token'));
    },
});
