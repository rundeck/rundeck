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

function showError(message) {
  appendHtml($("editerror"), message);
  $("editerror").show();
}

function showRowSelected(elem, tbl, classname) {
  var i;
  var elems = document.getElementsByClassName(classname, tbl);
  for (i = 0; i < elems.length; i++) {
    var e = elems[i];
    $(e).removeClassName('selected');
  }
  if (elem && $(elem)) {
    $(elem).addClassName('selected');
  }
}


/** begin wf edit code */
var jobEdittedHandler;

function _onJobEdit(func) {
  jobEdittedHandler = func;
}

function jobWasEdited() {
  if (typeof (jobEdittedHandler) == 'function') {
    jobEdittedHandler();
  }
}
var _jobOptionData = [];

function _optionData(data) {
  _jobOptionData = data || [];
}

function _addOption(data) {
  "use strict";
  _jobOptionData.push(data);
}

function _removeOptionName(name) {
  "use strict";
  var findname = function (e) {
    return e.name === name;
  };
  var found = _jobOptionData.findIndex(findname);
  if (found >= 0) {
    _jobOptionData.splice(found, 1);
  }
}
var _VAR_DATA = {
  job: [],
  node: [],
  eh: [],
  globals: [],
  execution: []
};

function _jobGlobalVarData() {
  "use strict";
  if (_VAR_DATA['globals'].length < 1) {
    var globdata = loadJsonData('globalVarData');
    if (globdata) {
      globdata.each(function (e) {
        _VAR_DATA['globals'].push({
          key: 'globals.' + e,
          category: 'Global Vars'
        });
      });
    }
  }
  return _VAR_DATA['globals'];
}

function _jobVarData() {
  if (_VAR_DATA['job'].length < 1) {
    var jobdata = {
      'id': {
        title: 'Job ID'
      },
      'execid': {
        title: 'Execution ID'
      },
      'executionType': {
        title: 'Execution Type'
      },
      'name': {
        title: 'Job Name'
      },
      'group': {
        title: 'Job Group'
      },
      'username': {
        title: 'Name of user executing the job'
      },
      'project': {
        title: 'Project name'
      },
      'loglevel': {
        title: 'Execution log level',
        desc: 'Logging level, one of: INFO, DEBUG'
      },
      'user.email': {
        title: 'Email of user executing the job'
      },
      'retryAttempt': {
        title: 'Retry attempt number'
      },
      'retryInitialExecId': {
        title: 'Retry Original Execution ID'
      },
      'wasRetry': {
        title: 'True if execution is a retry'
      },
      'threadcount': {
        title: 'Job Threadcount'
      },
      'filter': {
        title: 'Job Node Filter Query'
      }
    };
    ['id', 'execid', 'executionType', 'name', 'group', 'username', 'project', 'loglevel', 'user.email', 'retryAttempt', 'wasRetry', 'threadcount', 'filter', 'retryInitialExecId'].each(function (e) {
      _VAR_DATA['job'].push({
        key: 'job.' + e,
        category: 'Job',
        title: jobdata[e].title,
        desc: jobdata[e].desc
      });
    });
  }
  return _VAR_DATA['job'];
}

function _executionVarData() {
  if (_VAR_DATA['execution'].length < 1) {
    var executiondata = {
      'id': {
        title: 'Execution ID'
      },
      'href': {
        title: 'URL to the execution output view'
      },
      'status': {
        title: 'Execution state (‘running’,‘failed’,‘aborted’,‘succeeded’)'
      },
      'user': {
        title: 'User who started the job'
      },
      'dateStarted': {
        title: 'Execution Start time '
      },
      'description': {
        title: 'Summary string for the execution'
      },
      'argstring': {
        title: 'Argument string for any job options'
      },
      'project': {
        title: 'Project name'
      },
      'loglevel': {
        title: 'Execution log level',
        desc: 'Logging level, one of: INFO, DEBUG'
      },
      'failedNodeListString': {
        title: 'Comma-separated list of any nodes that failed, if present'
      },
      'succeededNodeListString': {
        title: 'Comma-separated list of any nodes that succeeded, if present'
      },
      'abortedby': {
        title: 'User who aborted the execution'
      }
    };
    ['id', 'href', 'status', 'user', 'dateStarted', 'description', 'argstring', 'project', 'loglevel', 'failedNodeListString', 'succeededNodeListString', 'abortedby'].each(function (e) {
      _VAR_DATA['execution'].push({
        key: 'execution.' + e,
        category: 'Execution',
        title: executiondata[e].title,
        desc: executiondata[e].desc
      });
    });
  }
  return _VAR_DATA['execution'];
}

function _jobNodeData() {
  "use strict";
  if (_VAR_DATA['node'].length < 1) {
    var nodedata = {
      'name': {
        title: 'Node Name'
      },
      'hostname': {
        title: 'Node Hostname'
      },
      'username': {
        title: 'Node username'
      },
      'description': {
        title: 'Node description'
      },
      'tags': {
        title: 'Node tags'
      },
      'os-name': {
        title: 'OS Name'
      },
      'os-family': {
        title: 'OS Family'
      },
      'os-arch': {
        title: 'OS Architecture'
      },
      'os-version': {
        title: 'OS Version'
      }
    };
    ['name', 'hostname', 'username', 'description', 'tags', 'os-name', 'os-family', 'os-arch', 'os-version'].each(function (e) {
      _VAR_DATA['node'].push({
        key: 'node.' + e,
        category: 'Node',
        title: nodedata[e].title,
        desc: '(only available in node step context)'
      });
    });
  }
  return _VAR_DATA['node'];
}

function _jobEhData() {
  "use strict";
  if (_VAR_DATA['eh'].length < 1) {
    var ehdata = {
      'message': {
        title: 'Error Message'
      },
      'resultCode': {
        title: 'Result Code',
        desc: 'Exit code from an execution (if available)'
      },
      'failedNodes': {
        title: 'Failed Nodes List'
      },
      'reason': {
        title: 'Error Reason',
        desc: 'A code indicating the reason the step failed'
      }
    };
    ['message', 'resultCode', 'failedNodes', 'reason'].each(function (e) {
      _VAR_DATA['eh'].push({
        key: 'result.' + e,
        category: 'Error Handler',
        title: ehdata[e].title,
        desc: ehdata[e].desc
      });
    });
  }
  return _VAR_DATA['eh'];
}
/**
 * After loading WF item edit form in the list, update input and apply ACE editor
 * @param item li item
 * @param iseh true if error handler
 * @param isnodestep true if node step
 */
function postLoadItemEdit(item, iseh, isnodestep) {
  var liitem = jQuery(item);
  liitem.find('input[type=text]').each(function (ndx, elem) {
    elem.observe('keypress', noenter);
  });
  if (liitem.find('input[type=text]').length > 0) {
    liitem.find('input[type=text]')[0].focus();
  }
  isnodestep = isnodestep || liitem.data('wfitemnodestep');
  var calcnodestep = function () {
    //look for radio button for nodeStep
    var find = liitem.find('input[name=nodeStep][type=radio]:checked');
    if (find.length) {
      return find.val() == 'true';
    }
    return isnodestep;
  };
  addWfAutocomplete(liitem, iseh, calcnodestep, function (elem) {
    return jQuery(elem).hasClass('_wfscriptitem');
  }, function (elem, editor) {
    var isscriptStep = jQuery(elem).hasClass('_wfscriptitem');
    if (isscriptStep) {
      var key = liitem.find('._wfiedit').data('rkey');
      if (key) {
        workflowEditor.scriptSteps()[key].guessAceMode.subscribe(function (val) {
          setAceSyntaxMode(val, editor);
        });
      }
    }
  }, function (elem) {
    var obj = jQuery(elem);
    if (obj.hasClass('context_env_autocomplete')) {
      var key = liitem.find('._wfiedit').data('rkey');
      return (key && workflowEditor.scriptSteps()[key] && workflowEditor.scriptSteps()[key].guessAceMode() || 'sh');
    }
    return null;
  });
}

function addWfAutocomplete(liitem, iseh, isnodestepfunc, istextareatemplatemode, acetexteditorcallback, gettextfieldenvmode) {
  var baseVarData = [].concat(_jobVarData());
  baseVarData = baseVarData.concat(_jobGlobalVarData());

  autocompleteBase(baseVarData, liitem, iseh, isnodestepfunc, istextareatemplatemode, acetexteditorcallback, gettextfieldenvmode);

}

function addNotificationAutocomplete(liitem, iseh, isnodestepfunc, istextareatemplatemode, acetexteditorcallback, gettextfieldenvmode) {
  var baseVarData = [].concat(_jobVarData());
  baseVarData = baseVarData.concat(_jobGlobalVarData());
  baseVarData = baseVarData.concat(_executionVarData());

  autocompleteBase(baseVarData, liitem, iseh, isnodestepfunc, istextareatemplatemode, acetexteditorcallback, gettextfieldenvmode);
}

function autocompleteBase(baseVarData, liitem, iseh, isnodestepfunc, istextareatemplatemode, acetexteditorcallback, gettextfieldenvmode) {
  var baseNodeData = [];

  //special error-handler vars

  if (iseh) {
    baseVarData = baseVarData.concat(_jobEhData());
  }

  var mkenv = function (name) {
    return ('RD_' + name).toUpperCase().replace(/[^a-zA-Z0-9_]/g, '_').replace(/[{}$]/, '');
  };
  var varmodes = {
    sh: function (name) {
      return '$' + mkenv(name);
    },
    powershell: function (name) {
      return '$env:' + mkenv(name);
    },
    batchfile: function (name) {
      return '%' + mkenv(name) + '%';
    }
  };
  var autovarfunc = function (prefix, suffix, mode) {
    prefix = prefix || '${';
    suffix = suffix || '}';
    var mkvar = function (name) {
      return prefix + name + suffix;
    };
    var expvars = [];
    var data = [].concat(baseVarData);

    if (isnodestepfunc && isnodestepfunc()) {
      data = data.concat(_jobNodeData());
    }

    var mkmodevar = (varmodes[mode] || varmodes['sh']);
    for (var i = 0; i < data.length; i++) {
      expvars.push({
        value: mkvar(data[i].key),
        data: data[i]
      });
      if (mode) {
        expvars.push({
          value: mkmodevar(data[i].key),
          data: data[i]
        });
      }
    }
    for (var x = 0; x < _jobOptionData.length; x++) {
      expvars.push({
        value: mkvar('option.' + _jobOptionData[x].name),
        data: {
          category: 'Options',
          title: 'Option value',
          desc: 'For option: ' + _jobOptionData[x].name
        }
      });
      if (_jobOptionData[x].multivalued == true) {
        expvars.push({
          value: mkvar('option.' + _jobOptionData[x].name + '.delimiter'),
          data: {
            category: 'Options',
            title: 'Option Delimeter value',
            desc: 'For option: ' + _jobOptionData[x].name
          }
        });
      }
      if (mode) {
        expvars.push({
          value: mkmodevar('option.' + _jobOptionData[x].name),
          data: {
            category: 'Options',
            title: 'Option value',
            desc: 'For option: ' + _jobOptionData[x].name
          }
        });

        if (_jobOptionData[x].multivalued == true) {
          expvars.push({
            value: mkmodevar('option.' + _jobOptionData[x].name + '.delimiter'),
            data: {
              category: 'Options',
              title: 'Option value',
              desc: 'For option: ' + _jobOptionData[x].name
            }
          });
        }
      }
      if (_jobOptionData[x].type == 'file') {
        expvars.push({
          value: mkvar('file.' + _jobOptionData[x].name),
          data: {
            category: 'File Option',
            title: 'The local file path',
            desc: 'For file option: ' + _jobOptionData[x].name
          }
        });
        expvars.push({
          value: mkvar('file.' + _jobOptionData[x].name + '.fileName'),
          data: {
            category: 'File Option',
            title: 'The original File name',
            desc: 'For file option: ' + _jobOptionData[x].name
          }
        });
        expvars.push({
          value: mkvar('file.' + _jobOptionData[x].name + '.sha'),
          data: {
            category: 'File Option',
            title: 'The file contents SHA256 value',
            desc: 'For file option: ' + _jobOptionData[x].name
          }
        });

        if (mode) {
          expvars.push({
            value: mkmodevar('file.' + _jobOptionData[x].name),
            data: {
              category: 'File Option',
              title: 'The local file path',
              desc: 'For file option: ' + _jobOptionData[x].name
            }
          });
          expvars.push({
            value: mkmodevar('file.' + _jobOptionData[x].name + '.fileName'),
            data: {
              category: 'File Option',
              title: 'The original File name',
              desc: 'For file option:' + _jobOptionData[x].name
            }
          });
          expvars.push({
            value: mkmodevar('file.' + _jobOptionData[x].name + '.sha'),
            data: {
              category: 'File Option',
              title: 'The file contents SHA256 value',
              desc: 'For file option: ' + _jobOptionData[x].name
            }
          });
        }
      }
    }
    return expvars;
  };
  liitem.find('textarea.apply_ace').each(function (ndx, elem) {
    var isscriptStep = istextareatemplatemode && istextareatemplatemode(elem);
    var editor = _addAceTextarea(elem, null, function (editor, session, pos, prefix) {
      "use strict";
      var aceSyntaxMode = getAceSyntaxMode(editor);
      var vals = isscriptStep ? autovarfunc('@', '@', aceSyntaxMode) : autovarfunc(null, null, aceSyntaxMode);
      return vals.map(function (ea) {
        "use strict";
        //ace text editor lang tools completer format
        return {
          name: ea.value,
          value: ea.value,
          score: 1,
          meta: ea.data && ea.data.category || null,
          title: ea.data && ea.data.title || null,
          desc: ea.data && ea.data.desc || null,
          type: 'rdvar'
        };
      });
    });
    acetexteditorcallback && acetexteditorcallback(elem, editor);
  });
  liitem.find('.context_env_autocomplete,.context_var_autocomplete').each(function (i, elem) {
    var obj = jQuery(elem);
    var autoenvmode = gettextfieldenvmode && gettextfieldenvmode(elem) || null;
    obj.devbridgeAutocomplete({
      delimiter: /( |(?=\$))/,
      tabDisabled: true,
      lookup: function (q, callback) {
        var query = q.toLowerCase();
        var results = jQuery.grep(autovarfunc(null, null, autoenvmode), function (suggestion) {
          "use strict";
          return suggestion.value.toLowerCase().indexOf(query) !== -1
        });
        callback({
          suggestions: results
        });
      },
      groupBy: 'category',
      formatResult: function (suggestion, currentValue) {
        "use strict";
        if (suggestion.data.title) {
          return jQuery.Autocomplete.formatResult(suggestion, currentValue) + ' - ' + suggestion.data.title;
        }
        return jQuery.Autocomplete.formatResult(suggestion, currentValue)
      }
    })
  });
}

function _initJobPickerAutocomplete(uuid, nameid, groupid, projid) {
  "use strict";
  var currentProject = jQuery('#schedEditFrameworkProject').val();
  var isReadOnly = jQuery("#" + nameid).attr('readonly');
  var minChar = isReadOnly ? 500 : 0;
  jQuery("#" + nameid).devbridgeAutocomplete({
    minChars: minChar,
    deferRequestBy: 500,
    lookup: function (q, callback) {
      var project = projid && jQuery('#' + projid).val() || currentProject;
      var results = jQuery.ajax({
        url: _genUrl(appLinks.menuJobSearchJson, {
          jobFilter: q,
          project: project,
          runAuthRequired: true
        }),
        success: function (data, x) {
          callback({
            suggestions: jQuery.map(data, function (item) {
              return {
                value: item.name,
                data: item
              };
            })
          });
        }
      });
    },
    onSelect: function (selected) {
      //set group from selected job
      jQuery('#' + groupid).val(selected.data.group);
      //set uuid
      jQuery('#' + uuid).val(selected.data.id);
    }
  });
}

function _enableNameJobRefFields(enable, uuid, nameid, groupid, projid) {
  "use strict";
  var nameField = jQuery("#" + nameid);
  var groupField = jQuery("#" + groupid);
  var uuidField = jQuery("#" + uuid);

  nameField.attr('readonly', !enable);
  groupField.attr('readonly', !enable);
  uuidField.attr('readonly', enable);
  //_initJobPickerAutocomplete(uuid,nameid, groupid, projid);
  if (nameField.devbridgeAutocomplete()) {
    if (enable) {
      nameField.devbridgeAutocomplete().setOptions({
        minChars: 0
      });
    } else {
      nameField.devbridgeAutocomplete().setOptions({
        minChars: 500
      });
    }
  }
}
var _iseditting = null;

function _wfiedit(key, num, isErrorHandler) {
  if (_iseditting) {
    return;
  }
  jobWasEdited();
  var params = {
    num: num,
    isErrorHandler: isErrorHandler ? true : false,
    key: key
  };
  if (getCurSEID()) {
    params.scheduledExecutionId = getCurSEID();
  }
  jQuery('#wfli_' + key).load(_genUrl(appLinks.workflowEdit, params), function (resp, status, jqxhr) {
    _hideWFItemControls(key);
    postLoadItemEdit('#wfli_' + key, isErrorHandler);
  });
}

function _wficopy(key, num, isErrorHandler) {
  _ajaxWFAction(appLinks.workflowCopy, {
    num: num,
    edit: true
  });
}

function _wfiview(key, num, isErrorHandler) {
  var params = {
    num: num,
    edit: true,
    key: key,
    isErrorHandler: isErrorHandler ? true : false
  };
  if (getCurSEID()) {
    params['scheduledExecutionId'] = getCurSEID();
  }
  jQuery('#wfli_' + key).load(_genUrl(appLinks.workflowRender, params), _showWFItemControls);
}

function _wfisave(key, num, formelem, iseh) {
  jobWasEdited();
  var data = jQuery("#" + formelem + " :input").serialize();
  jQuery.ajax({
    type: 'POST',
    url: _genUrl(appLinks.workflowSave),
    data: data,
    beforeSend: _createAjaxSendTokensHandler('job_edit_tokens'),
    success: function (resp, status, jqxhr) {
      var litem = jQuery('#wfli_' + key);
      litem.html(resp);
      if (litem.find('div.wfitemEditForm').size() < 1) {
        _showWFItemControls();
        if (iseh) {
          _hideWFItemControlsAddEH(num);
          if (litem.parent().closest('li').find('.wfitem.jobtype').size() > 0) {
            //disable the config button
            _disableWFItemControlsConfigButton(num)
          }
        }
      } else {
        postLoadItemEdit('#wfli_' + key, iseh);
      }
    }
  }).success(_createAjaxReceiveTokensHandler('job_edit_tokens'));
}
var newitemElem;

function _wfiaddnew(type, nodestep) {
  jobWasEdited();
  var params = {
    newitemtype: type,
    newitemnodestep: nodestep ? true : false
  };
  if (getCurSEID()) {
    params.scheduledExecutionId = getCurSEID();
  }
  clearHtml('wfnewitem');
  jQuery('#wfnewtypes').hide();
  _hideWFItemControls('new');
  var wfcontent = jQuery('#workflowContent');
  var olist = wfcontent.find('ol').first();
  var num = wfcontent.find('ol > li').length;
  params['key'] = num;
  var parentli = jQuery('<li>' +
    '<span></span>' +
    '<div></div>' +
    '<ul class="wfhandleritem" style="display:none">' +
    '<li></li>' +
    '</ul>' +
    '</li>');
  if (num % 2 == 1) {
    parentli.addClass('alternate');
  }
  parentli.data('wfitemnum', num);


  parentli.find('div').attr('id', 'wfivis_' + num);
  parentli.find('ul').data('wfitemnum', num);
  parentli.find('ul li').attr('id', 'wfli_eh_' + num);
  olist.append(parentli);
  newitemElem = parentli.find('span').first()[0];
  jQuery(newitemElem).attr('id', 'wfli_' + num);
  jQuery(newitemElem).data('wfitemnodestep', nodestep);
  jQuery(newitemElem).load(_genUrl(appLinks.workflowEdit, params), function () {
    postLoadItemEdit('#wfli_' + num, false, nodestep ? true : false);
  });
}

function _addAceTextarea(textarea, callback, ext) {
  return _setupAceTextareaEditor(textarea, function (e) {
    jobWasEdited();
    if (callback) {
      callback(e);
    }
  }, ext);
}

function _wfisavenew(formelem) {
  jobWasEdited();
  var data = jQuery("#" + formelem + " :input").serialize();
  jQuery.ajax({
    type: 'POST',
    url: _genUrl(appLinks.workflowSave),
    data: data,
    beforeSend: _createAjaxSendTokensHandler('job_edit_tokens'),
    success: function (resp, status, jqxhr) {
      jQuery(newitemElem).html(resp);
      if (jQuery(newitemElem).find('.wfitemEditForm').length > 0) {
        //was error
        postLoadItemEdit(newitemElem);
      } else {
        var litem = jQuery(newitemElem.parentNode);
        litem.highlight();
        jQuery('#wfnewbutton').show();
        _showWFItemControls();
        jQuery('#workflowDropfinal').data('wfitemnum', parseInt(litem.data('wfitemnum')) + 1);
        newitemElem = null;
      }
    }
  }).success(_createAjaxReceiveTokensHandler('job_edit_tokens'));
}

function _wficancelnew() {
  jQuery(newitemElem.parentNode).remove();
  newitemElem = null;
  jQuery('#wfnewbutton').show();
  _showWFItemControls();
}

function _findParentAttr(e, attr) {
  return jQuery(e).closest('[' + attr + ']').attr(attr);
}
//events handlers for add/cancel new step
function _evtNewStepChooseType(evt) {
  var e = evt.element();
  var type = _findParentAttr(e, 'data-step-type');
  _wfiaddnew(type, false);
}

function _evtNewNodeStepChooseType(evt) {
  var e = evt.element();
  var type = _findParentAttr(e, 'data-node-step-type');
  _wfiaddnew(type, true);
}

function _evtNewStepCancel(evt) {
  jQuery('#wfnewtypes').hide();
  jQuery('#wfnewbutton').show();
}

function _hideWFItemControls(item) {
  jQuery('#workflowContent').find('span.wfitemcontrols').hide();
  jQuery('#wfundoredo,#wfnewbutton').hide();
  _iseditting = item;
  _disableWfDragdrop();
}

function _updateEmptyMessage() {
  var x = jQuery('#workflowContent').find('ol li');
  if (x.length > 0) {
    jQuery('#wfempty').hide();
  } else {
    jQuery('#wfempty').show();
  }
}

function _showWFItemControls() {
  jQuery('#workflowContent').find('span.wfitemcontrols').show();
  jQuery('#wfundoredo,#wfnewbutton').show();
  _updateWFUndoRedo();
  _enableWFDragdrop();
  _updateEmptyMessage();
  _iseditting = null;
}

function _hideWFItemControlsAddEH(num) {
  var lielem = jQuery('#wfli_' + num);
  lielem.find('.wfitem_add_errorhandler').parent().hide();
}

function _disableWFItemControlsConfigButton(num) {
  var lielem = jQuery('#wfli_' + num);
  lielem.find('.wfitemcontrols .btn-group button.dropdown-toggle').attr('disabled', 'disabled');
}

function _evtNewEHChooseType(evt) {
  var e = evt.element();
  var type = _findParentAttr(e, 'data-step-type');
  _wfiaddNewErrorHandler(e, type, null, false);
}

function _evtNewEHNodeStepType(evt) {
  var e = evt.element();
  var type = _findParentAttr(e, 'data-node-step-type');
  _wfiaddNewErrorHandler(e, type, null, true);
}

function _hideAddNewEHLinks() {
  jQuery('span.wfitem_add_errorhandler').hide();
}

function _showAddNewEHLinks() {
  jQuery('span.wfitem_add_errorhandler').show();
}

function _evtNewEHCancel(evt) {
  var d = jQuery(evt.element()).closest('ul.wfhandleritem').hide();
  _hideAddNewEH();

  _showWFItemControls();
}

function _wficancelnewEH(elem) {
  var ul = jQuery(elem).closest('ul.wfhandleritem').hide();
  var d = jQuery(elem).closest('ul.wfhandleritem li').first();
  clearHtml(d[0]);

  _showWFItemControls();
}

function _hideAddNewEH() {
  var wfstepnew = jQuery('#wfnewtypes');
  var newehdiv = jQuery('#wfnew_eh_types');
  newehdiv.hide();
  newehdiv.detach();
  wfstepnew.append(newehdiv);
}

function _wfishownewErrorHandler(key, num, nodeStep) {
  var newehdiv = jQuery('#wfnew_eh_types');
  var wfiehli = jQuery('#wfli_eh_' + key);
  clearHtml(wfiehli[0]);
  newehdiv.detach();
  wfiehli.append(newehdiv);

  var nodeFirstWfStrat = jQuery('#wf_strat_node_first').is(':checked');
  var allowedWfStepEh = !(nodeStep && nodeFirstWfStrat);
  //WF step error handler not allowed if strategy is "node-first" and the step is a node step
  var sections = newehdiv.find('.step_section');
  if (allowedWfStepEh) {
    sections.show()
  } else {
    sections.hide()
  }

  newehdiv.show();
  wfiehli.parent().show();
  _hideWFItemControls('eh');
}

function _wfiaddNewErrorHandler(elem, type, num, nodestep) {
  if (null == num) {
    //find num by looking for enclosing ul and getting wfitemNum attribute
    var d = jQuery(elem).closest('ul.wfhandleritem');
    if (d.length == 1) {
      num = d.data('wfitemnum');
    }
  }
  var key = 'eh_' + num;

  //add new error handler for the item num
  var params = {
    newitemtype: type,
    key: key,
    num: num,
    isErrorHandler: true,
    newitemnodestep: nodestep ? true : false
  };
  if (getCurSEID()) {
    params.scheduledExecutionId = getCurSEID();
  }
  var wfiehli = jQuery('#wfli_' + key);
  _hideAddNewEH();
  wfiehli.data('wfitemnodestep', nodestep);

  wfiehli.load(_genUrl(appLinks.workflowEdit, params), function () {
    postLoadItemEdit(wfiehli, true, nodestep);
  });
}

function _doMoveItem(from, to) {
  _ajaxWFAction(appLinks.workflowReorder, {
    fromnum: from,
    tonum: to,
    edit: true
  });
}

function _doRemoveItem(key, num, isErrorHandler) {
  var params = {
    delnum: num,
    edit: true,
    key: key,
    isErrorHandler: isErrorHandler
  };
  jQuery('#wfivis_' + key).fadeOut('fast',
    function (f) {
      _ajaxWFAction(appLinks.workflowRemove, params);
    });
}

function _doUndoWFAction() {
  _ajaxWFAction(appLinks.workflowUndo, {
    edit: true
  });
}

function _doRedoWFAction() {
  _ajaxWFAction(appLinks.workflowRedo, {
    edit: true
  });
}

function _doResetWFAction() {
  _ajaxWFAction(appLinks.workflowRevert, {
    edit: true
  });
}

function _ajaxWFAction(url, params) {
  jobWasEdited();
  var tokendataid = 'reqtoken_undo_workflow';
  if (getCurSEID()) {
    params['scheduledExecutionId'] = getCurSEID();
  }
  jQuery.ajax({
    type: 'POST',
    url: _genUrl(url),
    data: params,
    beforeSend: _createAjaxSendTokensHandler(tokendataid),
    success: function (data, status, jqxhr) {
      jQuery('#workflowContent').find('ol').html(data);
      newitemElem = null;
      jQuery('#wfnewbutton').show();
      _showWFItemControls();
    }
  });
}

function _updateWFUndoRedo() {
  var params = {};
  if (getCurSEID()) {
    params['scheduledExecutionId'] = getCurSEID();
  }
  jQuery('#wfundoredo').load(_genUrl(appLinks.workflowRenderUndo, params));
}


///Drag drop
var wfDragger;
var wfDragDropSelect = "#workflowContent ol>li";

function _disableWfDragdrop() {
  jQuery(wfDragDropSelect).attr('draggable', false);
  jQuery(wfDragDropSelect).off();
}

function _enableWFDragdrop() {
  "use strict";

  wfDragger = _enableDragdrop(
    wfDragDropSelect,
    null,
    function (elem) {
      return jQuery.extend({}, jQuery(elem).data());
    },
    function (datafrom, datato) {
      if (datafrom.wfitemnum < datato.wfitemnum) {
        return 'hoverActiveDown';
      } else if (datafrom.wfitemnum > datato.wfitemnum) {
        return 'hoverActiveUp';
      }
      return null;
    },
    ['hoverActiveUp', 'hoverActiveDown'],
    function (from, to) {
      _doMoveItem(from.wfitemnum, to.wfitemnum);
    }
  );
}

function _enableDragdrop(select, finalid, getdata, hovercss, allcss, callback) {
  var dragger = {
    //data from drag source
    dragged: null,
    //currently dragged source elem
    draggedElem: null,
    //array of drop elements
    allowed: []
  };

  var dragStart = function (evt) {
    var oEvt = evt.originalEvent;
    dragger.draggedElem = oEvt.target;
    dragger.dragged = getdata(oEvt.target);
    oEvt.dataTransfer.setData('text/x-rd-data', dragger.dragged);
    oEvt.dataTransfer.dropEffect = 'move';
    oEvt.dataTransfer.effectAllowed = 'move';

    if (finalid) {
      jQuery("#" + finalid).show();
    }
    return true;
  };
  var allowDrop = function (evt) {
    if (!dragger.draggedElem) {
      //not currently dragging one of my expected draggables
      return;
    }
    if (evt.originalEvent.currentTarget === dragger.draggedElem) {
      return;
    }
    evt.preventDefault();
    var css = hovercss(
      dragger.dragged,
      getdata(evt.originalEvent.currentTarget)
    );

    if (css) {
      jQuery(evt.originalEvent.currentTarget).addClass(css);
    }
    return false;
  };
  var dragend = function (evt) {
    jQuery(dragger.allowed).removeClass(allcss.join(' '));
    if (finalid) {
      jQuery("#" + finalid).hide();
    }
    dragger.draggedElem = null;
    return false;
  };
  var dragleave = function (evt) {
    evt.preventDefault();
    jQuery(evt.originalEvent.target).removeClass(allcss.join(' '));
    return false;
  };
  var drop = function (evt) {
    evt.preventDefault();
    var fromdata = dragger.dragged;
    var todata = getdata(evt.delegateTarget);
    jQuery(dragger.allowed).off();
    dragend();
    callback(jQuery.extend({}, fromdata), jQuery.extend({}, todata));
    return false;
  };


  dragger.allowed = jQuery(select).toArray();


  jQuery(select).attr('draggable', 'true')
    .on('dragstart', dragStart)
    .on('dragover', allowDrop)
    .on('dragleave', dragleave)
    .on('dragend', dragend)
    .on('drop', drop);
  if (finalid) {
    var finalElem = jQuery("#" + finalid);
    dragger.allowed.push(finalElem[0]);
    finalElem
      .on('dragover', allowDrop)
      .on('drop', drop)
      .on('dragleave', dragleave)
      .on('dragend', dragend)
      .addClass('ready');
  }
  return dragger;
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
  _enableOptDragDrop();
}

var optsDragger;

var optsDragDropSelect = "#optionContent ul>li";

function _disableOptDragdrop() {
  jQuery(optsDragDropSelect).attr('draggable', false).off();
  jQuery("#optionDropFinal").attr('draggable', false).off();
}

function _enableOptDragDrop() {
  "use strict";
  optsDragger = _enableDragdrop(
    optsDragDropSelect,
    'optionDropFinal',
    function (elem) {
      return jQuery.extend({}, jQuery(elem).data());
    },
    function (datafrom, datato) {
      if (datato.isFinal) {
        return 'hoverActiveAll';
      } else if (datafrom.optName !== datato.optName) {
        return 'hoverActiveUp';
      }
      return null;
    },

    ['hoverActiveUp', 'hoverActiveAll'],
    _dragReorderOption
  );
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
  _disableOptDragdrop();
}

function _updateOptsUndoRedo() {
  var params = {};
  if (getCurSEID()) {
    params['scheduledExecutionId'] = getCurSEID();
  }

  jQuery('#optundoredo')
    .load(_genUrl(appLinks.editOptsRenderUndo, params));
}

function _configureInputRestrictions(target) {
  $(target).select('input').each(function (elem) {
    if (elem.type == 'text') {
      elem.observe('keypress', noenter);
    }
  });
  $(target).select('input.restrictOptName').each(function (elem) {
    if (elem.type == 'text') {
      elem.observe('keypress', function (e) {
        return controlkeycode(e) || onlychars('[a-zA-Z_0-9.\\t-]', e);
      });
    }
  });
}

function _isjobScheduled() {
  return jQuery('#scheduledTrue:checked').val() == 'true';
}

function _optedit(name, elem) {
  jobWasEdited();
  var params = {
    name: name
  };
  if (getCurSEID()) {
    params['scheduledExecutionId'] = getCurSEID();
  }
  params['jobWasScheduled'] = _isjobScheduled();
  jQuery.ajax({
    type: 'GET',
    url: _genUrl(appLinks.editOptsEdit, params),
    success: function (data, status, jqxhr) {
      jQuery(elem).html(data);
      _hideOptControls();
      _configureInputRestrictions(elem);
    },
    failure: function (data, status, jqxhr) {
      alert("error: " + status);
    }
  });
}

function _optview(name, target) {
  var params = {
    name: name,
    edit: true
  };
  if (getCurSEID()) {
    params['scheduledExecutionId'] = getCurSEID();
  }
  params['jobWasScheduled'] = _isjobScheduled();
  jQuery(target).load(_genUrl(appLinks.editOptsRender, params), _showOptControls);
}

function _optsave(formelem, tokendataid, target) {
  jobWasEdited();
  var optname = jQuery('#' + formelem + ' :input[name=name]').val();
  var opttype = jQuery('#' + formelem + ' :input[name=type]').val();
  var multivalued = jQuery('#' + formelem + ' :input[name=multivalued]:checked').val() == "true" ? true : false
  jQuery.ajax({
    type: "POST",
    url: _genUrl(appLinks.editOptsSave, {
      jobWasScheduled: _isjobScheduled()
    }),
    data: jQuery('#' + formelem + " :input").serialize(),
    beforeSend: _createAjaxSendTokensHandler(tokendataid),
    success: function (data, status, xhr) {
      _removeOptionName(optname);
      _addOption({
        name: optname,
        type: opttype,
        multivalued: multivalued
      });
      jQuery(target).html(data);
      if (jQuery(target).find('div.optEditForm').length < 1) {
        _showOptControls();
        jQuery(target).highlight();
      } else {
        _configureInputRestrictions(target);
        _hideOptControls();
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
  jobWasEdited();
  var params = {
    newoption: true
  };
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
  createElement.setAttribute('id', 'optvis_' + num);
  newoptli.appendChild(createElement);
  olist.appendChild(newoptli);
  jQuery(createElement).load(_genUrl(appLinks.editOptsEdit, params), null, function (resp, status, jqxhr) {
    if (status == 'success') {
      _configureInputRestrictions(createElement);
      _hideOptControls();
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
  var params = {
    newoption: true,
    edit: true
  };
  if (getCurSEID()) {
    params['scheduledExecutionId'] = getCurSEID();
  }
  var optslist = $('optionsContent').down('ul.options');
  jQuery('#optionsContent').find('ul.options').load(_genUrl(appLinks.editOptsRenderAll, params), function (data, status, jqxhr) {
    _showOptControls();
  });
}


function _summarizeOpts() {
  var params = {
    newoption: true,
    edit: true
  };
  if (getCurSEID()) {
    params['scheduledExecutionId'] = getCurSEID();
  }
  var optssummary = $('optssummary');
  jQuery('#optssummary').load(_genUrl(appLinks.editOptsRenderSummary, params), _showOptControls);
}

function _optsavenew(formelem, tokendataid) {
  jobWasEdited();
  var params = jQuery('#' + formelem + ' :input').serialize();
  var optname = jQuery('#' + formelem + ' :input[name=name]').val();
  var opttype = jQuery('#' + formelem + ' :input[name=type]').val();
  var multivalued = jQuery('#' + formelem + ' :input[name=multivalued]:checked').val() == "true" ? true : false
  jQuery.ajax({
    type: "POST",
    url: _genUrl(appLinks.editOptsSave, {
      jobWasScheduled: _isjobScheduled()
    }),
    data: params,
    beforeSend: _createAjaxSendTokensHandler(tokendataid),
    success: function (data, status, xhr) {
      jQuery(newoptli).html(data);
      _addOption({
        name: optname,
        type: opttype,
        multivalued: multivalued
      });
      if (!newoptli.down('div.optEditForm')) {
        $(newoptli).highlight();
        newoptli = null;
        _showOptControls();
        _reloadOpts();
      } else if (newoptli.down('div.optEditForm')) {
        _configureInputRestrictions(newoptli);
        _hideOptControls();
      }
    }
  });

}

function _doRemoveOption(name, elem, tokendataid) {
  jobWasEdited();
  var params = {
    name: name,
    edit: true
  };
  if (getCurSEID()) {
    params['scheduledExecutionId'] = getCurSEID();
  }
  jQuery($(elem)).slideUp('fast',
    function (f) {
      jQuery.ajax({
        type: 'POST',
        url: _genUrl(appLinks.editOptsRemove),
        data: params,
        beforeSend: _createAjaxSendTokensHandler(tokendataid),
        success: function (data, status, jqxhr) {
          _removeOptionName(name);
          console.log(_genUrl(appLinks.editOptsRemove));
          jQuery('#optionsContent').find('ul').html(data);
          _showOptControls();
        }
      });
    }
  );
}

function _doRemoveScheduleDefinition(name, elem, tokendataid){
  jobWasEdited();
  var params = {
    name: name,
    edit: true
  };
  if (getCurSEID()) {
    params['scheduledExecutionId'] = getCurSEID();
  }

  console.log(name);
  console.log(elem);
  console.log(tokendataid);
}

function _dragReorderOption(fromData, toData) {
  "use strict";
  var data = {};
  if (!toData.optName && toData.isFinal) {
    data = {
      end: true
    };
  } else {
    data = {
      before: toData.optName
    };
  }


  _doReorderOption(fromData.optName, data);

}

function _doReorderOption(name, data) {
  jobWasEdited();
  var tokendataid = 'reqtoken_undo_opts';
  var params = {
    name: name,
    edit: true
  };
  if (data.pos) {
    params['relativePosition'] = data.pos;
  } else if (data.end) {
    params['last'] = true;
  } else if (data.before) {
    params['before'] = data.before;
  }
  if (getCurSEID()) {
    params['scheduledExecutionId'] = getCurSEID();
  }
  jQuery.ajax({
    type: "POST",
    url: _genUrl(appLinks.editOptsReorder, params),
    beforeSend: _createAjaxSendTokensHandler(tokendataid),
    success: function (data, status, xhr) {
      jQuery('#optionsContent').find('ul').html(data);
      _showOptControls();
    }
  });

}

function _doUndoOptsAction() {
  var params = {
    edit: true
  };
  if (getCurSEID()) {
    params['scheduledExecutionId'] = getCurSEID();
  }
  var tokendataid = 'reqtoken_undo_opts';
  jQuery.ajax({
    type: 'POST',
    url: _genUrl(appLinks.editOptsUndo),
    data: params,
    beforeSend: _createAjaxSendTokensHandler(tokendataid),
    success: function (data, status, jqxhr) {
      jQuery('#optionsContent').find('ul').html(data);
      _showOptControls();
    }
  });
}

function _doRedoOptsAction() {
  var params = {
    edit: true
  };
  if (getCurSEID()) {
    params['scheduledExecutionId'] = getCurSEID();
  }
  var tokendataid = 'reqtoken_undo_opts';
  jQuery.ajax({
    type: 'POST',
    url: _genUrl(appLinks.editOptsRedo),
    data: params,
    beforeSend: _createAjaxSendTokensHandler(tokendataid),
    success: function (data, status, jqxhr) {
      jQuery('#optionsContent').find('ul').html(data);
      _showOptControls();
    }
  });
}

function _doRevertOptsAction() {
  var params = {
    edit: true
  };
  if (getCurSEID()) {
    params['scheduledExecutionId'] = getCurSEID();
  }
  var tokendataid = 'reqtoken_undo_opts';
  jQuery.ajax({
    type: 'POST',
    url: _genUrl(appLinks.editOptsRevert),
    data: params,
    beforeSend: _createAjaxSendTokensHandler(tokendataid),
    success: function (data, status, jqxhr) {
      jQuery('#optionsContent').find('ul').html(data);
      _showOptControls();
    }
  });
}

//job chooser
var uuidField;
var jobNameFieldId;
var jobGroupFieldId;

function jobChosen(uuid, name, group, elem) {
  jobWasEdited();
  if (uuidField && jobNameFieldId && jobGroupFieldId) {
    jQuery('#' + uuidField).val(uuid);
    jQuery('#' + jobNameFieldId).val(name);
    jQuery('#' + jobGroupFieldId).val(group);
    doyftsuccess(uuidField);
    doyftsuccess(jobNameFieldId);
    doyftsuccess(jobGroupFieldId);
  }
  if (jQuery(elem).closest('.modal').length === 1) {
    jQuery(elem).closest('.modal').modal('hide');
  }
}

function loadJobChooserModal(elem, uuid, nameid, groupid, projectid, modalid, modalcontentid) {
  if (jQuery(elem).hasClass('active')) {
    jQuery('#' + modalid).modal('hide');
    return;
  }
  uuidField = uuid;
  jobNameFieldId = nameid;
  jobGroupFieldId = groupid;
  var project = selFrameworkProject;

  if (projectid) {
    project = jQuery('#' + projectid).val();
  }
  jQuery(elem).button('loading').addClass('active');
  jQuery.ajax({
    url: _genUrl(appLinks.menuJobsPicker, {
      jobsjscallback: 'true',
      runAuthRequired: true,
      projFilter: project
    }),
    success: function (resp, status, jqxhr) {
      jQuery(elem).button('reset').removeClass('active');
      jQuery('#' + modalcontentid).html(resp);
      jQuery('#' + modalid).modal('show');
    },
    error: function (resp, status, jqxhr) {
      showError("Error performing request: menuJobsPicker: " + transport);
      jQuery(elem).button('reset').removeClass('active');
    }
  });
}

//group chooser
function groupChosen(path) {
  jobWasEdited();
  jobeditor.groupPath(path)
  jQuery('#groupChooseModal').modal('hide')

}
jQuery(window).load(function () {
  jQuery('#groupChooseModal').on('shown.bs.modal', function (e) {
    jQuery.get(appLinks.scheduledExecutionGroupTreeFragment + '?jscallback=true', function (d) {
      jQuery('#groupChooseModalContent').html(d)
    });
  });
  jQuery('#groupChooseModal').on('hide.bs.modal', function (e) {

  });

  jQuery('.notifyFields').each(function (i, elem) {
    addNotificationAutocomplete(jQuery(elem));
  });
});
