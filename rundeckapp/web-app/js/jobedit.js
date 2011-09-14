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

function showError(message) {
    $("editerror").innerHTML += message;
    $("editerror").show();
}
function showRowSelected(elem, tbl, classname) {
    var i;
    var elems = document.getElementsByClassName(classname, tbl);
    for (i = 0 ; i < elems.length ; i++) {
        var e = elems[i];
        $(e).removeClassName('selected');
    }
    if (elem && $(elem)) {
        $(elem).addClassName('selected');
    }
}


function _editFormSelectProject(value){

}

function prepJobType(data) {
    if (data.authorized) {
        $('scriptAuthWarn').hide();
    } else {
        $('scriptAuthWarn').show();
    }
}
function testScriptAuth(project) {
    if (!project) {
        selFrameworkProject = '';
        return;
    }
    selFrameworkProject = project;
    new Ajax.Request(
        applinks.frameworkTestScriptAuth, {
        parameters: "project=" + project,
        onSuccess: function(transport) {
            var data = transport.responseText;
            var orig = data;
            if (typeof(data) === "string") {
                eval("data=" + data);
                prepJobType(data);
            }
        },
        onFailure: function() {
            showError("Error performing request: testScriptAuth for [" + project + "]");
        }
    });
}

/** Nodeset matching */
var mnodetimer = null;
function _formUpdateMatchedNodes() {
    var i;
    var project = $('schedEditFrameworkProject').value;
    if (!project) {
        $('mnodeswait').hide();
        return;
    }
    var params = {project:project,view:'embed',declarenone:true,defaultLocalNode:true,fullresults:true,formInput:true,requireRunAuth:true};
    for (i in node_filter_keys) {
        var key = node_filter_keys[i];
        if ($('schedJobNodeInclude' + key) && $F('schedJobNodeInclude' + key)) {
            params['nodeInclude' + key] = $F('schedJobNodeInclude' + key);
        }
        if ($('schedJobNodeExclude' + key) && $F('schedJobNodeExclude' + key)) {
            params['nodeExclude' + key] = $F('schedJobNodeExclude' + key);
        }
    }
    if ($('nodeExcludePrecedenceTrue').checked) {
        params.nodeExcludePrecedence = "true";
    } else {
        params.nodeExcludePrecedence = "false";
    }
    if ($('mnodeswait')) {
        $('mnodeswait').show();
        $('mnodeswait').loading("");
    }
    new Ajax.Updater('matchednodes', applinks.frameworkNodesFragment, {parameters:params,evalScripts:true,onSuccess:function(
        e) {
        $('mnodeswait').hide();
    }});
}
function _matchNodes() {
    if (mnodetimer) {
        clearTimeout(mnodetimer);
    }
    if ($('mnodeswait')) {
        $('mnodeswait').show();
        $('mnodeswait').loading("");
    }
    mnodetimer = setTimeout(_formUpdateMatchedNodes, 500);
}
function _matchNodesKeyPress(e) {
    if (!noenter(e)) {
        return false;
    }
    _matchNodes();
    return true;
}

/** end Nodeset matching */

function addFilter(name, isinclude, label) {
    var prefix = (isinclude ? 'Include' : 'Exclude');
    if ($('nodeFilter' + prefix + name)) {
        $('nodeFilter' + prefix + name).show();
        if ($('filterAdd' + prefix + name)) {
            $('filterAdd' + prefix + name).hide();
        }
        return;
    }
}

function setFilter(name, isinclude, value) {
    var prefix = (isinclude ? 'Include' : 'Exclude');
    if ($('schedJobNode' + prefix + name)) {
        $('schedJobNode' + prefix + name).setValue(value);
        _matchNodes();
    }
}
function removeFilter(name, isinclude) {
    var prefix = (isinclude ? 'Include' : 'Exclude');
    if ($('nodeFilter' + prefix + name)) {
        $('nodeFilter' + prefix + name).hide();
        if ($('schedJobNode' + prefix + name)) {
            $('schedJobNode' + prefix + name).setValue('');
            if ($('filterAdd' + prefix + name)) {
                $('filterAdd' + prefix + name).show();
            }
            _matchNodes();
        }
    }
}

/** begin wf edit code */


function _wfiedit(num) {
    var params = {num:num};
    if (getCurSEID()) {
        params.scheduledExecutionId = getCurSEID();
    }
    new Ajax.Updater($('wfli_' + num),
        applinks.workflowEdit, {
        parameters: params,
        evalScripts:true,
        onSuccess: function(transport) {
            _hideWFItemControls();
        },
        onComplete: function(transport) {
            if (transport.request.success()) {

                $('wfli_' + num).select('input').each(function(elem) {
                    if (elem.type === 'text') {
                        elem.observe('keypress', noenter);
                    }
                });
            }
        }
    });
}

function _wfiview(num) {
    var params = {num:num,edit:true};
    if (getCurSEID()) {
        params['scheduledExecutionId'] = getCurSEID();
    }
    new Ajax.Updater($('wfli_' + num),
        applinks.workflowRender, {
        parameters: params,
        evalScripts:true,
        onSuccess: function(transport) {
            _showWFItemControls();
        }
    });
}
function _wfisave(num, formelem) {
    var params = {num:num};
    if (getCurSEID()) {
        params['scheduledExecutionId'] = getCurSEID();
    }
    new Ajax.Updater($('wfli_' + num),
        applinks.workflowSave, {
        parameters: Form.serialize(formelem),
        evalScripts: true,
        onComplete: function(transport) {
            if (transport.request.success()) {
                if (!$('wfli_' + num).down('div.wfitemEditForm')) {
                    _showWFItemControls();
                }
            }
        }
    });
}
var newitemli;
function _wfiaddnew(type) {
    var params = {newitemtype:type};
    if (getCurSEID()) {
        params.scheduledExecutionId = getCurSEID();
    }
    $('wfnewitem').innerHTML = '';
    $('wfnewtypes').hide();
    _hideWFItemControls();
    var olist = $('workflowContent').down('ol');
    var litems = $$('#workflowContent ol li');
    var num = litems.length;
    newitemli = new Element('li');
    if (num % 2 == 1) {
        newitemli.addClassName('alternate');
    }
    newitemli.setAttribute('id', 'wfli_' + num);
    newitemli.setAttribute('wfitemNum', num);
    var createElement = new Element('div');
    createElement.setAttribute('id','wfivis_' + num);
    newitemli.appendChild(createElement);
    olist.appendChild(newitemli);
    new Ajax.Updater(createElement,
        applinks.workflowEdit, {
        parameters: params,
        evalScripts:true,
        onComplete: function(transport) {
            if (transport.request.success()) {
                $(createElement).select('input').each(function(elem) {
                    if (elem.type == 'text') {
                        elem.observe('keypress', noenter);
                    }
                });
            }
        }
    });
}
function _wfisavenew(formelem) {
    var params = Form.serialize(formelem);
    new Ajax.Updater(newitemli,
        applinks.workflowSave, {
        parameters: params,
        evalScripts: true,
        onComplete: function(transport) {
            if (transport.request.success() && !newitemli.down('div.wfitemEditForm')) {
                $(newitemli).highlight();
                $('wfnewbutton').show();
                _showWFItemControls();
                $('workflowDropfinal').setAttribute('wfitemNum', parseInt(newitemli.getAttribute('wfitemNum')) + 1);
                newitemli = null;
            }
        }
    });
}
function _wficancelnew() {
    var olist = $('workflowContent').down('ol');
    $(olist).removeChild(newitemli);
    newitemli = null;
    $('wfnewbutton').show();
    _showWFItemControls();
}
function _hideWFItemControls() {
    $$('#workflowContent span.wfitemcontrols').each(Element.hide);
    $('wfundoredo').hide();
    $('wfnewbutton').hide();
}
function _updateEmptyMessage() {
    var x = $('workflowContent').down('ol li');
    if (x) {
        $('wfempty').hide();
    } else {
        $('wfempty').show();
    }
}
function _showWFItemControls() {
    $$('#workflowContent span.wfitemcontrols').each(Element.show);
    $('wfundoredo').show();
    $('wfnewbutton').show();
    _updateWFUndoRedo();
    _enableDragdrop();
    _updateEmptyMessage();
}

function _doMoveItem(from, to) {
    var params = {fromnum:from,tonum:to,edit:true};
    if (getCurSEID()) {
        params['scheduledExecutionId'] = getCurSEID();
    }

    new Ajax.Updater($('workflowContent').down('ol'),
        applinks.workflowReorder, {
        parameters: params,
        evalScripts:true,
        onComplete: function(transport) {
            if (transport.request.success()) {
                newitemli = null;
                $('wfnewbutton').show();
                _showWFItemControls();
            }
        }
    });
}
function _doRemoveItem(num) {
    var params = {delnum:num,edit:true};
    if (getCurSEID()) {
        params['scheduledExecutionId'] = getCurSEID();
    }
    Effect.Fade($('wfivis_' + num), {duration:0.2,afterFinish:
        function(f) {
            new Ajax.Updater($('workflowContent').down('ol'),
                applinks.workflowRemove, {
                parameters: params,
                evalScripts:true,
                onComplete: function(transport) {
                    if (transport.request.success()) {
                        newitemli = null;
                        $('wfnewbutton').show();
                        _showWFItemControls();
                    }
                }
            });
        }
    });
}
function _doUndoWFAction() {
    var params = {edit:true};
    if (getCurSEID()) {
        params['scheduledExecutionId'] = getCurSEID();
    }

    new Ajax.Updater($('workflowContent').down('ol'),
        applinks.workflowUndo, {
        parameters: params,
        evalScripts:true,
        onComplete: function(transport) {
            if (transport.request.success()) {
                newitemli = null;
                $('wfnewbutton').show();
                _showWFItemControls();
            }
        }
    });
}
function _doRedoWFAction() {
    var params = {edit:true};
    if (getCurSEID()) {
        params['scheduledExecutionId'] = getCurSEID();
    }

    new Ajax.Updater($('workflowContent').down('ol'),
        applinks.workflowRedo, {
        parameters: params,
        evalScripts:true,
        onComplete: function(transport) {
            if (transport.request.success()) {
                newitemli = null;
                $('wfnewbutton').show();
                _showWFItemControls();
            }
        }
    });
}
function _doResetWFAction() {
    var params = {edit:true};
    if (getCurSEID()) {
        params['scheduledExecutionId'] = getCurSEID();
    }

    new Ajax.Updater($('workflowContent').down('ol'),
        applinks.workflowRevert, {
        parameters: params,
        evalScripts:true,
        onComplete: function(transport) {
            if (transport.request.success()) {
                newitemli = null;
                $('wfnewbutton').show();
                _showWFItemControls();
            }
        }
    });
}
function _updateWFUndoRedo() {
    var params = {};
    if (getCurSEID()) {
        params['scheduledExecutionId'] = getCurSEID();
    }
    new Ajax.Updater($('wfundoredo'), applinks.workflowRenderUndo, {parameters:params});
}


///Drag drop
function moveDragItem(dragged, droparea) {
    var num = $(dragged).getAttribute('wfitemNum');
    var to = $(droparea).getAttribute('wfitemNum');

    if (to > num) {
        to = to - 1;
    }

    _doMoveItem(num, to);
}
function _enableDragdrop() {
    $$('#workflowContent ol li').each(function(item) {
        new Draggable(
            item,
        {
            revert: 'failure',
            ghosting: false,
            constraint:'vertical',
            handle:'dragHandle',
            scroll: window,
            onStart:function(d) {
                $('workflowDropfinal').show();
            },
            onEnd:function(d) {
                $('workflowDropfinal').hide();
            }
        }
            );
    });

    $$('#workflowContent ol li').each(function(item) {
        Droppables.add(item, {
            hoverclass: 'hoverActive',
            onDrop: moveDragItem
        }
            );
        $(item).addClassName("ready");
    });
    $$('#workflowDropfinal').each(function(item) {
        Droppables.add(item, {
            hoverclass: 'hoverActive',
            onDrop: moveDragItem
        }
            );
        $(item).addClassName("ready");
    });
}
/** end wf edit code */

/** common Undo/Redo code */
function _doRevertAction(key) {
    if ('workflow' == key) {
        return _doResetWFAction();
    } else if ('opts' == key) {
        return _doRevertOptsAction();
    }
    return null;
}
function _doRedoAction(key) {
    if ('workflow' == key) {
        return _doRedoWFAction();
    } else if ('opts' == key) {
        return _doRedoOptsAction();
    }
    return null;
}
function _doUndoAction(key) {
    if ('workflow' == key) {
        return _doUndoWFAction();
    } else if ('opts' == key) {
        return _doUndoOptsAction();
    }
    return null;
}

/** end Common Undo/Redo code*/


/** begin Option edit code */


var newoptli;
function _showOptControls() {
    $$('#optionsContent .opteditcontrols').each(Element.show);
    $('optnewbutton').show();
    _updateOptsUndoRedo();
    _showOptEmptyMessage();
    $('optsload').innerHTML = '';
}
function _showOptEmptyMessage() {
    var x = $('optionsContent').down('ul li');
    if (x) {
        $('optempty').hide();
        $('optheader').show();
    } else {
        $('optempty').show();
        $('optheader').hide();
    }
}
function _hideOptControls() {
    $$('#optionsContent .opteditcontrols').each(Element.hide);
    $('optnewbutton').hide();
    $('optsload').innerHTML = '';
}
function _updateOptsUndoRedo() {
    var params = {};
    if (getCurSEID()) {
        params['scheduledExecutionId'] = getCurSEID();
    }
    new Ajax.Updater($('optundoredo'), applinks.editOptsRenderUndo, {parameters:params});
}

function _configureInputRestrictions(target) {
    $(target).select('input').each(function(elem) {
        if (elem.type == 'text') {
            elem.observe('keypress', noenter);
        }
    });
    $(target).select('input.restrictOptName').each(function(elem) {
        if (elem.type == 'text') {
            elem.observe('keypress', onlychars.curry('[a-zA-Z_0-9.\\t]'));
        }
    });
}

function _optedit(name, elem) {
    var params = {name:name};
    if (getCurSEID()) {
        params['scheduledExecutionId'] = getCurSEID();
    }
    $('optsload').loading();
    new Ajax.Updater(elem,
        applinks.editOptsEdit, {
        parameters: params,
        evalScripts:true,
        onSuccess: function(transport) {
            _hideOptControls();
        },
        onComplete: function(transport) {
            if (transport && transport.request.success()) {
                _configureInputRestrictions(elem);
            }
        },
        onFailure: function(transport) {
            alert("error: " + transport);
        }
    });
}

function _optview(name, target) {
    var params = {name:name,edit:true};
    if (getCurSEID()) {
        params['scheduledExecutionId'] = getCurSEID();
    }
    new Ajax.Updater(target,
        applinks.editOptsRender, {
        parameters: params,
        evalScripts:true,
        onComplete: function(transport) {
            if (transport.request.success()) {
                _showOptControls();
            }
        }
    });
}
function _optsave(formelem, target) {
    $('optsload').loading();
    new Ajax.Updater(target,
        applinks.editOptsSave, {
        parameters: Form.serialize(formelem),
        evalScripts: true,
        onComplete: function(transport) {
            if (transport.request.success()) {
                if (!$(target).down('div.optEditForm')) {
                    _showOptControls();
                    $(target).highlight();
                } else {
                    _configureInputRestrictions(target);
                    _hideOptControls();
                }
            }
        }
    });
}
function _optaddnewIfNone() {
    //if no options in the list, load new edit form:
    var litems = $$('#optionsContent ul li');
    if (!litems || 0 == litems.length) {
        _optaddnew();
    }
}
function _optaddnew() {
    var params = {newoption:true};
    if (getCurSEID()) {
        params['scheduledExecutionId'] = getCurSEID();
    }
    _hideOptControls();
    var olist = $('optionsContent').down('ul');
    var litems = $$('#optionsContent ul li');
    var num = litems.length;
    newoptli = new Element('li');
    if (num % 2 == 1) {
        newoptli.addClassName('alternate');
    }
    newoptli.addClassName('optEntry');
    var createElement = new Element('div');
    createElement.setAttribute('id','optvis_' + num);
    newoptli.appendChild(createElement);
    olist.appendChild(newoptli);
    $('optsload').loading();
    new Ajax.Updater(createElement,
        applinks.editOptsEdit, {
        parameters: params,
        evalScripts:true,
        onComplete: function(transport) {
            if (transport && transport.request.success()) {
                _configureInputRestrictions(createElement);
                _hideOptControls();
            }
            $('optsload').innerHTML = '';
        }
    });
}

function _optcancelnew() {
    var olist = $('optionsContent').down('ul');
    $(olist).removeChild(newoptli);
    newoptli = null;
    _showOptControls();
}

function _reloadOpts() {
    var params = {newoption:true,edit:true};
    if (getCurSEID()) {
        params['scheduledExecutionId'] = getCurSEID();
    }
    var optslist = $('optionsContent').down('ul.options');
    $('optsload').loading();
    new Ajax.Updater(optslist,
        applinks.editOptsRenderAll, {
        parameters: params,
        evalScripts: true,
        onComplete: function(transport) {
            if (transport.request.success()) {
                _showOptControls();
            }
        }
    });
}


function _summarizeOpts() {
    var params = {newoption:true,edit:true};
    if (getCurSEID()) {
        params['scheduledExecutionId'] = getCurSEID();
    }
    var optssummary = $('optssummary');
    $('optsload').loading();
    new Ajax.Updater(optssummary,
        applinks.editOptsRenderSummary, {
        parameters: params,
        evalScripts: true,
        onComplete: function(transport) {
            if (transport.request.success()) {
                _showOptControls();
            }
        }
    });
}

function _optsavenew(formelem) {
    var params = Form.serialize(formelem);
    $('optsload').loading();
    new Ajax.Updater(newoptli,
        applinks.editOptsSave, {
        parameters: params,
        evalScripts: true,
        onComplete: function(transport) {
            if (transport.request.success() && !newoptli.down('div.optEditForm')) {
                $(newoptli).highlight();
                newoptli = null;
                _showOptControls();
                _reloadOpts();
            } else if (transport.request.success() && newoptli.down('div.optEditForm')) {
                _configureInputRestrictions(newoptli);
                _hideOptControls();
            }
        }
    });
}

function _doRemoveOption(name, elem) {
    var params = {name:name,edit:true};
    if (getCurSEID()) {
        params['scheduledExecutionId'] = getCurSEID();
    }
    $('optsload').loading();
    Effect.Fade($(elem), {duration:0.2,afterFinish:
        function(f) {
            new Ajax.Updater($('optionsContent').down('ul'),
                applinks.editOptsRemove, {
                parameters: params,
                evalScripts:true,
                onComplete: function(transport) {
                    if (transport.request.success()) {
                        _showOptControls();
                    }
                }
            });
        }
    });
}

function _doUndoOptsAction() {
    var params = {edit:true};
    if (getCurSEID()) {
        params['scheduledExecutionId'] = getCurSEID();
    }

    new Ajax.Updater($('optionsContent').down('ul'),
        applinks.editOptsUndo, {
        parameters: params,
        evalScripts:true,
        onComplete: function(transport) {
            if (transport.request.success()) {
                _showOptControls();
            }
        }
    });
}
function _doRedoOptsAction() {
    var params = {edit:true};
    if (getCurSEID()) {
        params['scheduledExecutionId'] = getCurSEID();
    }

    new Ajax.Updater($('optionsContent').down('ul'),
        applinks.editOptsRedo, {
        parameters: params,
        evalScripts:true,
        onComplete: function(transport) {
            if (transport.request.success()) {
                _showOptControls();
            }
        }
    });
}
function _doRevertOptsAction() {
    var params = {edit:true};
    if (getCurSEID()) {
        params['scheduledExecutionId'] = getCurSEID();
    }

    new Ajax.Updater($('optionsContent').down('ul'),
        applinks.editOptsRevert, {
        parameters: params,
        evalScripts:true,
        onComplete: function(transport) {
            if (transport.request.success()) {
                _showOptControls();
            }
        }
    });
}

