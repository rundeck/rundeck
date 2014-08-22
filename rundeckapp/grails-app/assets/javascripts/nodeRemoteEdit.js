/*
 Copyright 2014 SimplifyOps Inc, <http://simplifyops.com>

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

/*********
 *  remote editor
 *********/

var remoteSite;
var remoteEditStarted = false;
var remoteEditExpect = false;
var projectname;

/**
 * Start remote editor for node with url
 * @param name node name
 * @param url url
 */
function doRemoteEdit(name, project, url) {
    _remoteEditClear();

    projectname = project;
    setText('editNodeIdent',name);

    //create iframe for url
    var ifm = document.createElement('iframe');
    ifm.width = "640px";
    ifm.height = "480px";
    _remoteEditExpect(url);
    ifm.src = url;

    $('remoteEditTarget').appendChild(ifm);

    _remoteEditShow();
}

//setup functions

/**
 * Begin listener for message protocol from origin url
 * @param originUrl
 */
function _remoteEditExpect(originUrl) {
    remoteEditExpect = true;
    remoteSite = originUrl;
    remoteEditStarted = false;

    Event.observe(window, 'message', _rdeckNodeEditOnmessage);
}

/**
 * Stop listener for message protocol
 */
function _remoteEditStop() {
    remoteEditExpect = false;
    remoteSite = null;
    remoteEditStarted = false;
    projectname = null;

    Event.stopObserving(window, 'message', _rdeckNodeEditOnmessage);
}

/**
 * Clear/reset remote editor dom content
 */
function _remoteEditClear() {
    _remoteEditStop();

    _clearTarget();

    shouldrefresh = false;
    clearHtml('editNodeIdent');
    var errhold = $('remoteEditError');
    clearHtml(errhold);
    errhold.hide();
}

/**
 * Show remote editor dom content
 */
function _remoteEditShow() {
    $('remoteEditTarget').show();
    $('remoteEditholder').show();

    $('remoteEditResultHolder').hide();
    $('nodesTable').hide();
}

/**
 * Hide remote editor dom content
 */
function _remoteEditHide() {
    $('remoteEditholder').hide();
    $('remoteEditTarget').hide();

    $('remoteEditToolbar').show();
    $('nodesTable').show();
}

/**
 * Clear iframe holder
 */
function _clearTarget() {
    clearHtml('remoteEditTarget');
    $('remoteEditTarget').hide();

}
/**
 * Finish all node editor stuff, and hide it
 */
function _remoteEditCompleted() {
    _remoteEditStop();
    _remoteEditHide();
    _remoteEditClear();
}

var shouldrefresh = false;
/**
 * If necessary, reload the nodes page
 */
function _remoteEditContinue() {
    if (shouldrefresh) {
        document.location = appLinks.frameworkNodes;
    } else {
        _remoteEditCompleted();
    }
}

/**
 * Perform Ajax request to tell server to re-fetch the nodes data for the project
 */
function _remoteEditDidSave() {
    //no-op now
}

//protocol handler functions//


/**
 * handler for :finished message
 * @param changed true if changes were saved
 */
function _rdeckNodeEditFinished(changed) {

    if (changed) {
        setText($('remoteEditResultText'), "Node changes were saved successfully.");
        _remoteEditDidSave();
    } else {
        setText($('remoteEditResultText'), "Node changes were not saved.");
    }
    _remoteEditStop();
    _clearTarget();

    $('remoteEditToolbar').hide();
    $('remoteEditResultHolder').show();
}

/**
 * handler for error message
 * @param origin
 * @param msg
 */
function _rdeckNodeEditError(origin, msg) {
    _remoteEditStop();
    _clearTarget();

    var errhold = $('remoteEditError');
    setText(errhold, (origin ? origin + " reported an error: " : "") + msg);
    errhold.show();
}

/**
 * handler for :started message
 */
function _rdeckNodeEditStarted() {
    remoteEditStarted = true;
}

var PROTOCOL = 'rundeck:node:edit';
/**
 * onmessage handler
 * @param msg
 */
function _rdeckNodeEditOnmessage(msg) {
    if (!remoteEditExpect || !remoteSite || !remoteSite.startsWith(msg.origin + "/")) {
        return;
    }
    var data = msg.data;
    if (!remoteEditStarted && PROTOCOL + ':started' == data) {
        _rdeckNodeEditStarted();
    } else if (PROTOCOL + ':error' == data || data.startsWith(PROTOCOL + ':error:')) {
        var err = data.substring((PROTOCOL + ':error').length);
        if (err.startsWith(":")) {
            err = err.substring(1);
        }
        _rdeckNodeEditError(msg.origin, err ? err : "(No message)");
    } else if (remoteEditStarted) {
        if (PROTOCOL + ':finished:true' == data) {
            _rdeckNodeEditFinished(true);
        } else if (PROTOCOL + ':finished:false' == data) {
            _rdeckNodeEditFinished(false);
        } else {
            _rdeckNodeEditError(null, "Unexpected message received from [" + msg.origin + "]: " + data);
        }
    }
}
/**
 * END remote edit code
 */
