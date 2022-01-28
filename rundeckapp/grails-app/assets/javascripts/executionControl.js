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
var FollowControl = function (eid, elem, params) {


    this.executionId = eid
    this.targetElement = elem
    //utils to obviate prototype
    const Element_hide = function (e, e2) {
        jQuery(e2 || e).hide()
    }
    const Element_show = function (e, e2) {
        jQuery(e2 || e).show()
    }

    Object.assign(this, {
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
        ctxGroupNodes: {},
        contextStatus: {},
        extraParams: {},
        execData: {},
        appLinks: {},
        parentElement: null,
        fileloadId: null,
        fileloadPctId: null,
        fileloadProgressId: null,
        cmdOutputErrorId: null,
        outfileSizeId: null,
        autoscroll: true,
        cmdoutputtbl: null,
        cmdoutspinner: null,
        runningcmd: null,
        finishedExecutionAction: true,
        lineCount: 0,
        lastrow: null,
        contextIdCounter: 0,

        lastTBody: null,

        //node mode

        taildelay: 1,
        isrunning: false,
        starttime: null,
        updatepagetitle: false,

        //instance vars
        totalCount: 0,
        totalDuration: 0,
        killjobhtml: '',
        killjobauth: false,
        nodemode: false,
        browsemode: false,
        tailmode: false,
        cancelload: false,
        partialload: false,
        truncateToTail: false,
        lastlines: 20,
        maxLastLines: 500,
        iconUrl: '/images/icon',
        smallIconUrl: '/images/icon-small',
        workflow: null,
        multiworkflow: null,
        clusterExec: null,
        showClusterExecWarning: true,
        onLoadComplete: null,
        onLoadingFile: null,
        onFileloadMessage: null,
        onFileloadError: null,
        onFileloadPercentage: null
    })
    Object.assign(this, params)
    Object.assign(this, {
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
    setMode: function(mode){
        this.tailmode=mode=="tail";
        this.browsemode=mode=="browse";
        this.nodemode=mode=="node";
        this.tailmode=this.tailmode||!(this.browsemode||this.nodemode);
        this.refresh=this.tailmode;
        this.readyMode();
    },
    resetMode: function (mode) {
        this.setMode(mode)
        this.reload()
    },
    setLogWrap: function (wrapped) {
        "use strict";
        if (this.cmdoutputtbl) {
            if (wrapped) {
                jQuery(this.cmdoutputtbl).removeClass('no-wrap')
            } else {
                jQuery(this.cmdoutputtbl).addClass('no-wrap')
            }
        }
    },
    readyMode: function(){
        var obj=this;
        this.setGroupOutput(this.browsemode||this.nodemode);
        if (this.targetElement && jQuery('#' + this.targetElement).length) {

            if(this.tailmode){

            }else if(this.browsemode){


            }else if(this.nodemode){

            }
        }
    },
    appendCmdOutputError: function (message) {
        if (jQuery('#' + this.cmdOutputErrorId).length) {
            appendText('#' + this.cmdOutputErrorId, message)
            jQuery('#' + this.cmdOutputErrorId).show()
        }
        if (typeof (this.onFileloadError) === 'function') {
            this.onFileloadError(message)
        }
    },
    _log: function(message) {
        if (jQuery('#log').length) {
            appendText("#log", message)
            appendHtml("#log", "<br>")
        }
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
        jQuery('#lastlinesvalue').value = this.lastlines
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
        var oldval = parseInt(jQuery('#lastlinesvalue').value)
        val = parseInt(val);
        oldval = oldval + val;
        this.updateLastlines(oldval);
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
            this.ctxBodySet._each(Element_hide)
            this.ctxBodyFinalSet._each(this.showFinalLine.value ? Element_show : Element_hide)
            jQuery('.expandicon,tr.contextRow').each(function (i, e) {
                jQuery(e).addClass('closed').removeClass('opened')
            });
        } else {
            this.ctxBodySet._each(Element_show)
            this.ctxBodyFinalSet._each(Element_show)
            jQuery('.expandicon,tr.contextRow').each(function (i, e) {
                jQuery(e).removeClass('closed').addClass('opened')
            });
        }
        this.setCtxCollapseDisplay(val);
    },
    setCtxCollapseDisplay:function(val) {
        if (jQuery('#ctxcollapseLabel').length) {
            if (val) {
                jQuery('#ctxcollapseLabel').addClass('active')
            } else {
                jQuery('#ctxcollapseLabel').removeClass('active')
            }
        }
        if (jQuery('#ctxshowlastlineoption').length) {
            if (val) {
                jQuery('#ctxshowlastlineoption').show()
            } else {
                jQuery('#ctxshowlastlineoption').hide()
            }
        }
    },

    setGroupOutput:function (val) {
        if (this.groupOutput.value != val) {
            this.groupOutput.value = val;
        }
        this.ctxGroupSet.forEach(this.groupOutput.value ? Element_show : Element_hide)
        if (this.groupOutput.value && this.collapseCtx.value) {
            this.ctxBodySet.forEach(Element_hide)
            this.ctxBodyFinalSet.forEach(this.showFinalLine.value ? Element_show : Element_hide)
        } else {
            this.ctxBodySet.forEach(Element_show)
            this.ctxBodyFinalSet.forEach(Element_show)
        }

        if (!this.groupOutput.value) {
            if (this.cmdoutputtbl) {
                this.setColTime(this.colTime.value);
                this.setColNode(this.colNode.value);
                this.setColStep(this.colStep.value);
            }
            if (jQuery('#ctxcollapseLabel').length) {
                jQuery('#ctxcollapseLabel').hide()
            }
            if (jQuery('#ctxshowlastlineoption').length) {
                jQuery('#ctxshowlastlineoption').hide()
            }

        } else {
            if (this.cmdoutputtbl) {
                jQuery(this.cmdoutputtbl).removeClass('collapse_time')
                jQuery(this.cmdoutputtbl).addClass('collapse_node')
                jQuery(this.cmdoutputtbl).addClass('collapse_stepnum')
            }
            if (jQuery('#ctxcollapseLabel').length) {
                jQuery('#ctxcollapseLabel').show()
            }
            this.setCtxCollapseDisplay(this.collapseCtx.value);
        }
        jQuery('.obs_grouped_true').each(val ? Element_show : Element_hide)
        jQuery('.obs_grouped_false').each(!val ? Element_show : Element_hide)
        if (jQuery('#ctxshowgroupoption').length) {
            if (val) {
                jQuery('#ctxshowgroupoption').addClass('active')
            } else {
                jQuery('#ctxshowgroupoption').removeClass('active')
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
            if (!obj.showFinalLine.value &&
                obj.collapseCtx.value &&
                obj.ctxBodySet[ndx] &&
                !jQuery(obj.ctxBodySet[ndx]).is(':visible')) {
                Element_hide(elem)
            } else {
                Element_show(elem)
            }
        });

        if (jQuery('#ctxshowlastlineoption').length) {
            if (val) {
                jQuery('#ctxshowlastlineoption').addClass('active')
            } else {
                jQuery('#ctxshowlastlineoption').removeClass('active')
            }
        }
    },
        togCls: function (val, obj, cls) {
            if (obj) {

                if (val) {
                    jQuery(obj).removeClass(cls)
                } else {
                    jQuery(obj).addClass(cls)
                }
            }
        },
    setColTime: function (show) {
        this.togCls(show, this.cmdoutputtbl, 'collapse_time')

        this.colTime.value = show;
    },
    setColNode: function (show) {
        this.togCls(show, this.cmdoutputtbl, 'collapse_node')

        this.colNode.value = show;
    },
    setColStep: function (show) {
        this.togCls(show, this.cmdoutputtbl, 'collapse_stepnum')


        this.colStep.value = show;
    },
    setOutputAppendTop: function(istop) {
        if (this.appendtop.value != istop) {
            this.appendtop.changed = !this.appendtop.changed;
        }

        if (jQuery('#appendTopLabel').length) {

            if (istop) {
                jQuery('#appendTopLabel').addClass('active')
            } else {
                jQuery('#appendTopLabel').removeClass('active')
            }
        }
        if (jQuery('#appendBottomLabel').length) {
            if (istop) {
                jQuery('#appendBottomLabel').removeClass('active')
            } else {
                jQuery('#appendBottomLabel').addClass('active')
            }
        }
        this.appendtop.value = istop;

        if (!this.isrunning) {
            this.reverseOutputTable(this.cmdoutputtbl);
        }
    },
    clearTable: function(tbl) {

        if (tbl) {
            jQuery(tbl).remove()
            this.cmdoutputtbl = null;
        }
        this._init();
    },

    createTable: function(id) {
        var tbl = jQuery("<table>")
        tbl.attr("border", "0")
        tbl.attr("width", "100%")
        tbl.attr("height", "auto")
        tbl.attr("cellSpacing", "0")
        tbl.attr("cellPadding", "0")
        tbl.addClass('execoutput')
        if(id){
            tbl.attr('id', id)
        }
        if(!this.tailmode){
            jQuery(tbl).addClass('collapse_node')
            jQuery(tbl).addClass('collapse_stepnum')
        }

        var tbod = jQuery("<tbody>")
        tbl.append(tbod)

        let parent = jQuery(typeof(this.parentElement) === 'string' ? '#' + this.parentElement : this.parentElement)
        parent.append(tbl)
        parent.show()
        return tbl[0]
    },
    pauseLoading: function (callback) {
        this._onStopCallback = callback
        this.cancelload = true
    },
    resumeLoading: function () {
        this.cancelload = false
        this.loadMoreOutput(this.runningcmd.id, this.runningcmd.offset)
    },
    showLoading:function(message,percent){
        if (typeof (this.onLoadingFile) === 'function') {
            this.onLoadingFile(true)
        }
        if (typeof (this.onFileloadMessage) === 'function') {
            this.onFileloadMessage(message)
        }
        if (percent != null && typeof (this.onFileloadPercentage) === 'function') {
            this.onFileloadPercentage(percent)
        }

    },
    hideLoading:function(){
        if (typeof (this.onLoadingFile) === 'function') {
            this.onLoadingFile(false)
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
                console.log("error",e)
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
                jQuery(this.cmdoutputtbl).hide()
            }
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

        var entries = [].concat(data.entries)
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
                jQuery('#' + generateId(this.parentElement) + '_clusterinfo').show()
            } else {
                jQuery('#' + generateId(this.parentElement) + '_clusterinfo').hide()
            }
        }

        if (this.runningcmd.completed && this.runningcmd.jobcompleted) {
            //halt timer

            if (null != data.totalSize) {
                if (jQuery('#' + this.outfileSizeId)) {
                    setText('#' + this.outfileSizeId, data.totalSize + " bytes")
                }
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
            if (!this.cancelload) {
                setTimeout(function () {
                    obj.loadMoreOutput(obj.runningcmd.id, obj.runningcmd.offset)
                }, time)

            }
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

            if (null != data.totalSize) {
                if (jQuery('#' + this.outfileSizeId)) {
                    setText('#' + this.outfileSizeId, data.totalSize + " bytes")
                }
            }
        }
        if (this.cancelload) {
            if (typeof (this._onStopCallback) == 'function') {
                var cb = this._onStopCallback
                this._onStopCallback = null
                cb()
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
                var temptbod = jQuery("<tbody>")
                temptbod.attr('id', 'final' + this.lastTBody.getAttribute('id'))
                if (this.isAppendTop()) {
                    this.cmdoutputtbl.insertBefore(temptbod[0], this.lastTBody);
                } else {
                    this.cmdoutputtbl.appendChild(temptbod[0]);
                }

                temptbod.append(lastcell)
                this.ctxBodyFinalSet.push(temptbod);
                if (0 == this.lastTBody.rows.length) {
                    // var expicon = jQuery('#ctxExp' + this.contextIdCounter);
//                    if (expicon) {
//                        expicon.removeClassName('expandicon');
//                    }
                    var ctxgrp = jQuery('#ctxgroup' + this.contextIdCounter)

                    if (ctxgrp.length && ctxgrp[0].rows.length > 0) {
                        jQuery(ctxgrp[0].rows[0]).addClass('expandable')
                        jQuery(ctxgrp[0].rows[0]).addClass('action')
                    }
                } else {

                    var ctxgrp = jQuery('#ctxgroup' + this.contextIdCounter)

                    if (ctxgrp.length && ctxgrp[0].rows.length > 0) {
                        jQuery(ctxgrp[0].rows[0]).addClass('expandable')
                        jQuery(ctxgrp[0].rows[0]).addClass('action')
                    }
                }
            } catch(e) {
                console.log("error",e)
                this.appendCmdOutputError("finishDataOutput: "+e);
            }
        }
        if(this.lineCount == 0) {
            //show empty message
            jQuery('#' + generateId(this.parentElement) + '_empty').show()
        }
    },
        toggleDataBody: function (elem, ctxid) {
            if (jQuery(elem).is(':visible')) {
                jQuery(elem).hide()
            jQuery('#ctxExp' + ctxid).removeClass('opened')
            jQuery('#ctxExp' + ctxid).addClass('closed')
            jQuery('#ctxExp' + ctxid).closest('tr.contextRow').removeClass('opened')
            jQuery('#ctxExp' + ctxid).closest('tr.contextRow').addClass('closed')
            if (jQuery('#finaldatabody' + ctxid).length) {
                if (this.collapseCtx.value && this.showFinalLine.value) {
                    jQuery('#finaldatabody' + ctxid).show()
                } else {
                    jQuery('#finaldatabody' + ctxid).hide()
                }
            }
        } else {
                jQuery(elem).show()
            jQuery('#ctxExp' + ctxid).removeClass('closed')
            jQuery('#ctxExp' + ctxid).addClass('opened')
            jQuery('#ctxExp' + ctxid).closest('tr.contextRow').removeClass('closed')
            jQuery('#ctxExp' + ctxid).closest('tr.contextRow').addClass('opened')
            if (jQuery('#finaldatabody' + ctxid).length) {
                jQuery('#finaldatabody' + ctxid).show()
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
                if (!jQuery(tbl.tBodies[j].rows[0]).hasClass('contextRow')) {
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
                if (!jQuery(row).hasClass('contextRow')) {
                    tbl.tBodies[j].removeChild(row);
                    count--;
                    k--;
                }
            }
            console.log("tbody " + j + ", new length: " + tbl.tBodies[j].rows.length);

            if (tbl.tBodies[j].rows.length == 1 && jQuery(tbl.tBodies[j].rows[0]).hasClass('contextRow')) {
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

                    var rows = new Array(parent.rows)
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
                        if (jQuery(row).hasClass('contextRow')) {
                            jQuery(row).addClass(this.isAppendTop() ? "up" : "down")
                            jQuery(row).removeClass(this.isAppendTop() ? "down" : "up")
                        }
                    }
                }


                this.appendtop.changed = false;
            }
        } catch(e) {
            console.log("error",e)
            this.appendCmdOutputError("reverseOutputTable "+e);
        }
    },
    isAtBottom: function()
    {
        var elem = document.querySelector( '#section-main' );
        var a = elem.scrollHeight;
        var b = elem.scrollTop;
        var c = elem.clientHeight;
        return ((a - b) <= (c*1.1));
    },
    scrollToBottom: function()
    {
        var elem = document.querySelector( '#section-main' );
        elem.scrollTop = elem.scrollHeight;
    },
    genDataRowNodes: function(data, tbl) {
        this.reverseOutputTable(tbl);
        var node = data.node;
        if (!node) {
            node = this.execData.node;
        }
        if (!this.ctxGroupNodes[node]) {
            this.ctxGroupNodes[node] = 'node_' + this.contextIdCounter++
        }
        var ctxid = this.ctxGroupNodes[node]
        var tbody;
        if (!this.ctxGroupTbodies[ctxid]) {
            tbody = this.createNewNodeTbody(data, tbl, ctxid);
            this.ctxGroupTbodies[ctxid] = tbody;
        } else {
            tbody = this.ctxGroupTbodies[ctxid];
        }

        var tr = tbody.insertRow(-1)
        this.configureDataRow(tr, data, ctxid);
        if (jQuery('#ctxCount' + ctxid).length) {
            setText('#ctxCount' + ctxid, '' + tbody.rows.length + " lines")
            if (data.level == 'ERROR' || data.level == 'SEVERE') {
                jQuery('#ctxCount' + ctxid).addClass(data.level)
            }
        }
        this.runningcmd.count++;
        this.lastrow = data;
        return tr;
    },

    createNewNodeTbody: function(data, tbl, ctxid) {
        //create new Table body
        var newtbod = jQuery("<tbody>")

        newtbod.attr('id', 'ctxgroup' + ctxid)
        if (this.isAppendTop()) {
            tbl.insertBefore(newtbod[0], tbl.tBodies[0]);
        } else {
            jQuery(tbl).append(newtbod);
        }
        this.ctxGroupSet.push(newtbod[0])

        var tr = newtbod[0].insertRow(this.isAppendTop() ? 0 : -1)
        var iconcell = tr.insertCell(0)
        iconcell.setAttribute('id', 'ctxIcon' + ctxid);
        jQuery(tr).addClass('contextRow')
        if (this.isAppendTop()) {
            jQuery(tr).addClass("up")
        } else {
            jQuery(tr).addClass("down")
        }
        jQuery(tr).addClass('expandable')
        jQuery(tr).addClass('action')
        jQuery(iconcell).addClass("icon")
        var cell = tr.insertCell(1)
        cell.setAttribute('colSpan', '4');


        if (null != data['node'] && '' != data['node']) {
            var sp = jQuery('<span>')
            sp.addClass('node')
            setText(sp,data['node']);
            cell.appendChild(sp[0]);
        }

        if ( data['stepctx'] && this.workflow) {
            var contextstr= this.workflow.renderContextString(data['stepctx']);
        } else {
            jQuery(tr).addClass('console');
            jQuery(cell).append(jQuery(" <span class='console'>[console]</span>"))
        }
        var countspan = jQuery('<span>')
        countspan.attr('id', 'ctxCount' + ctxid)
        countspan.attr('count', '0')
        countspan.addClass('ctxcounter')
        setText(countspan, " -");
        cell.appendChild(countspan[0]);
        var cell2 = tr.insertCell(2)
        cell2.setAttribute('id', 'ctxExp' + ctxid);
        jQuery(cell2).addClass('rowexpicon')
        jQuery(cell2).addClass('expandicon')
        var obj=this;
        //create new tablebody for data rows
        var datatbod = jQuery("<tbody>")
        datatbod.attr('id', 'databody' + ctxid)
        jQuery(tbl).append(datatbod);

        tr.onclick = function () {
            obj.toggleDataBody(datatbod[0], ctxid)
        }


        //start all data tbody as closed
        Element_hide(datatbod)
        jQuery(cell2).addClass('closed')
        jQuery(tr).addClass('closed')

        return datatbod[0]
    },

    createFinalContextTbody: function(data, tbl, ctxid) {
        //remove last row and place in new table body
        try {
            var lastcell = this.lastTBody.rows[this.isAppendTop() ? 0 : this.lastTBody.rows.length - 1];
            this.lastTBody.removeChild(lastcell);
            var temptbod = jQuery("<tbody>")
            temptbod.attr('id', 'final' + this.lastTBody.getAttribute('id'))
            if (this.isAppendTop()) {
                tbl.insertBefore(temptbod[0], this.lastTBody);
            } else {
                jQuery(tbl).append(temptbod);
            }
            temptbod.append(lastcell)
            this.ctxBodyFinalSet.push(temptbod);
            if (this.showFinalLine.value) {
                Element_show((temptbod))
            } else if (this.groupOutput.value && this.collapseCtx.value) {
                Element_hide((temptbod))
            }
            if (0 == this.lastTBody.rows.length) {
                // var expicon = jQuery('#ctxExp' + this.contextIdCounter);
//                if (expicon) {
//                    expicon.removeClassName('expandicon');
//                }
                var ctxgrp = jQuery('#ctxgroup' + this.contextIdCounter)

                if (ctxgrp.length && ctxgrp[0].rows.length > 0) {
                    jQuery(ctxgrp[0].rows[0]).addClass('expandable')
                    jQuery(ctxgrp[0].rows[0]).addClass('action')
                }
            } else {

                var ctxgrp = jQuery('#ctxgroup' + this.contextIdCounter)

                if (ctxgrp.length && ctxgrp[0].rows.length > 0) {
                    jQuery(ctxgrp[0].rows[0]).addClass('expandable')
                    jQuery(ctxgrp[0].rows[0]).addClass('action')
                }
            }
        } catch(e) {
            console.log("error",e)
            this.appendCmdOutputError("createFinalContextTbody "+e);
        }


        this.contextIdCounter++;
    },
    createNewContextTbody: function(data, tbl, ctxid) {
        //create new Table body
        var newtbod = jQuery("<tbody>")

        newtbod.attr('id', 'ctxgroup' + ctxid)
        if (this.isAppendTop()) {
            tbl.insertBefore(newtbod[0], tbl.tBodies[0])
        } else {
            jQuery(tbl).append(newtbod)
        }
        this.ctxGroupSet.push(newtbod[0])
        if (!this.groupOutput.value) {
            newtbod.hide();
        }


        var tr = (newtbod[0].insertRow(this.isAppendTop() ? 0 : -1))

        var iconcell = (tr.insertCell(0))
        iconcell.setAttribute('id', 'ctxIcon' + ctxid);
        jQuery(tr).addClass('contextRow')
        if (this.isAppendTop()) {
            jQuery(tr).addClass("up")
        } else {
            jQuery(tr).addClass("down")
        }
        jQuery(iconcell).addClass("icon")
        var cell = (tr.insertCell(1))
        cell.setAttribute('colSpan', '2');


        if (null != data['node'] && '' != data['node']) {
            var sp = jQuery('<span>')
            jQuery(sp).addClass('node')
            setText(sp, data['node']);
            cell.appendChild(sp[0]);
        }

        if (data['stepctx'] && this.workflow) {
            var contextstr = this.workflow.renderContextString(data['stepctx']);
            var stepnum = this.workflow.renderContextStepNumber(data['stepctx']);

            var sp = jQuery('<span>')
            sp.addClass('stepnum')
            sp.title=contextstr;
            setText(sp,contextstr);
            cell.appendChild(sp[0]);
            var sp2 = jQuery('<span>')
            sp2.addClass('stepident')
            setText(sp, contextstr);
            cell.appendChild(sp2[0]);
            //if dynamic step info available load dynamically
            if(this.multiworkflow){
                this.multiworkflow.getStepInfoForStepctx(data['stepctx'],function(info){
                    "use strict";
                    setText(sp2,info.stepident());
                });
            }
        } else {
            jQuery(tr).addClass('console')
            jQuery(cell).append(jQuery(" <span class='console'>[console]</span>"))
        }
        var cell2 = (tr.insertCell(2))
        cell2.setAttribute('id', 'ctxExp' + ctxid);
        jQuery(cell2).addClass('rowexpicon')
        jQuery(cell2).addClass('expandicon')
        var obj=this;

        //create new tablebody for data rows
        var datatbod = jQuery("<tbody>")
        if (this.isAppendTop()) {
            tbl.insertBefore(datatbod[0], newtbod);
        } else {
            jQuery(tbl).append(datatbod);
        }
        this.lastTBody = datatbod[0];
        this.lastTBody.setAttribute('id', 'databody' + ctxid);
        tr.onclick = function () {
            obj.toggleDataBody(datatbod[0], ctxid)
        }
        this.ctxBodySet.push(this.lastTBody);
        if (this.groupOutput.value && this.collapseCtx.value) {
            Element_hide((this.lastTBody))
            jQuery(cell2).addClass('closed')
            jQuery(tr).addClass('closed')
        } else {
            jQuery(cell2).addClass('opened')
            jQuery(tr).addClass('opened')
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
        var tr = (this.lastTBody.insertRow(this.isAppendTop() ? 0 : -1))
        this.configureDataRow(tr, data, ctxid);

        this.runningcmd.count++;
        this.lastrow = data;
        return tr;
    },

    configureDataRow: function(tr, data, ctxid) {

        if (data.level == 'ERROR' || data.level == 'SEVERE') {
            this.contextStatus[ctxid] = data.level.toLowerCase();
        }
        var tdtime = jQuery(tr.insertCell(0))
        //tdtime.setAttribute('width', '20');
        tdtime.addClass('info')
        tdtime.addClass('time')
        var timespan = jQuery('<span>')
        timespan.addClass(data.level)
        setText(timespan,data.time);
        tdtime.append(timespan)
        if(data.absolute_time){
            if(typeof(moment)=='function'){
                setText(timespan, MomentUtil.formatTime(data.absolute_time,'HH:mm:ss'));
            }
            tdtime.attr('title', data.absolute_time)
        }
        var cellndx=1;
        var colspan="2";
        var tdnode = jQuery(tr.insertCell(cellndx))
        cellndx++;
        tdnode.addClass('node')
        var shownode=false;
        if (this.lastrow && typeof(this.lastrow['node'])!=undefined && data.node==this.lastrow['node']){
            tdnode.addClass('repeat')
            jQuery(tr).addClass('node-repeat')
        }else if (!data.node) {
            tdnode.addClass('empty')
            shownode = true;
            jQuery(tr).addClass('node-empty')
        } else{
            tdnode.attr('title', data.node)
            setText(tdnode, data.node);
            shownode=true;
            jQuery(tr).addClass('node-new')
        }

        //add context column
        var tdctx = jQuery(tr.insertCell(cellndx))
        cellndx++;
        tdctx.addClass('stepnum')
        if (!shownode && this.lastrow && this.lastrow['stepctx'] == data['stepctx'] ) {
//                tdctx.addClass('repeat');
        }else if(data['stepctx'] && this.workflow){

            var stepNumText = this.workflow.renderContextStepNumber(data['stepctx']);
            var cmdtext= stepNumText + " " + this.workflow.renderContextString(data['stepctx']);
            var icon = jQuery('<i>')
            icon.addClass('rdicon icon-small ' + this.workflow.contextType(data['stepctx']))
            tdctx.append(icon)
            tdctx.append(document.createTextNode(" " + cmdtext))
            tdctx.attr('title', data['stepctx'])
            if(this.multiworkflow){
                var td = jQuery(tdctx);
                var stepinfo=this.multiworkflow.getStepInfoForStepctx(data['stepctx']);
                td.empty();
                td.attr('title',null);
                td.attr('data-bind',"template: {name: 'step-info-extended', data:$data, as: 'stepinfo'}");
                ko.applyBindings(stepinfo,td[0]);
            }
        }
        var tddata = jQuery(tr.insertCell(cellndx))
        tddata.addClass('data')
        tddata.attr('colspan', colspan);
        if (null != data['loghtml']) {
            tddata.html(data.loghtml);
            tddata.addClass('datahtml log_' + data.level.toLowerCase())
        } else {
            var txt = data.log;
            if(txt==''){
                txt="\n";
            }
            setText(tddata,txt);
            tddata.addClass('log_' + data.level.toLowerCase())
        }
        //append node
        if (shownode && data.node) {
            let nodeCss = '';
            if(!data['stepctx']){
                nodeCss = 'console';
            }
            jQuery(tddata).prepend(jQuery('<span class="inset-node '+nodeCss+'"></span>').text(' '+data.node).prepend(jQuery('<i class="fas fa-hdd"></i>')))
        }
    },
    clearCmdOutput: function() {
        clearHtml((this.parentElement))
        this.cmdoutputtbl = null;
        this.cmdoutspinner = null;
        this.runningcmd = null;
    },
    beginExecution: function() {
        this.clearCmdOutput();
        jQuery(this.parentElement).show()

        this.isrunning = true;
        this.cancelload = false
    },

    finishedExecution: function(result,statusString) {
        if(!this.finishedExecutionAction){
            return;
        }
        if (jQuery('#cmdoutspinner').length) {
            jQuery('#cmdoutspinner').remove()
        }
        this.cmdoutspinner = null;
        this.isrunning = false;
        this.hideLoading()

        this.jobFinishStatus(result,statusString);
        if (typeof(this.onComplete) == 'function') {
            this.onComplete(result, statusString)
        }
    },
    jobFinishStatus: function(result,statusString) {

    },
    isCompleted: function (id) {
        return this.runningcmd && this.runningcmd.completed && this.runningcmd.id === id
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
    })


    this._init()

    this.readyMode()
}
