/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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

//= require knockout.min
//= require knockout-mapping
//= require knockout-onenter

function TokenCreator(data) {
    var self = this;
    self.roleset = ko.observable(data.roleset);
    self.user = ko.observable(data.user);
    self.adminAuth = ko.observable(data.adminAuth);
    self.userTokenAuth = ko.observable(data.userTokenAuth);
    self.svcTokenAuth = ko.observable(data.svcTokenAuth);
    self.tokenTime = ko.observable(data.tokenTime);
    self.tokenTimeUnit = ko.observable(data.tokenTimeUnit || 'm');
    self.tokenUser = ko.observable(data.tokenUser || data.user);
    self.tokenRolesStr = ko.observable(data.tokenRoles);
    self.generateData = ko.computed(function () {
        return {
            login: self.user(),
            tokenTime: self.tokenTime(),
            tokenTimeUnit: self.tokenTimeUnit(),
            tokenUser: self.tokenUser(),
            tokenRoles: (self.adminAuth() || self.svcTokenAuth()) ? self.tokenRolesStr() : self.roleset().selectedRoles().join(',')
        }
    });
    self.actionGenerate = function () {
        generateUserToken(self.user(), 'gentokensection', self.generateData());
    };
}
function Role(name, checked) {
    var self = this;
    self.name = ko.observable(name);
    self.checked = ko.observable(checked ? true : false);
}
function RoleSet(list) {
    var self = this;
    self.roles = ko.observableArray([]);
    self.checked = ko.observableArray([]);
    self.checkAll = function () {
        ko.utils.arrayForEach(self.roles(), function (r) {
            r.checked(true);
        });
    };
    self.uncheckAll = function () {
        ko.utils.arrayForEach(self.roles(), function (r) {
            r.checked(false);
        });
    };
    self.selectedRoles = ko.computed(function () {
        var arr = [];
        var roles = self.roles();
        for (var i = 0; i < roles.length; i++) {
            if (roles[i].checked()) {
                arr.push(roles[i].name());
            }
        }
        return arr;
    });

    if (list) {
        var arr = [];
        for (var i = 0; i < list.length; i++) {
            arr.push(new Role(list[i], true));
        }
        self.roles(arr);
    }
}

function highlightNew() {
    jQuery(' .apitokenform.newtoken').fadeTo('slow', 1);
}
function tokenAjaxError(msg) {
    jQuery('.gentokenerror-text').text("Error: " + msg);
    jQuery('.gentokenerror').show();
}

function addTokenRow(elem, login, tokenid) {
    var table = $(elem).down('.apitokentable');
    var row = new Element('tbody');
    table.insert(row);
    $(row).addClassName('apitokenform');
    $(row).style.opacity = 0;
    jQuery(row).load(
        _genUrl(appLinks.userRenderApiToken, {login: login, tokenid: tokenid}),
        function (resp, status, jqxhr) {
            jQuery($(row)).fadeTo("slow",1);
        }
    );
}

function clearToken(elem) {
    var dom = jQuery(elem);
    var login = dom.find('input[name="login"]').val();
    var params = {login: login};
    if (dom.find('input[name="tokenid"]').length > 0) {
        params.tokenid = dom.find('input[name="tokenid"]').val();
    } else {
        params.token = dom.find('input[name="token"]').val();
    }
    jQuery.ajax({
        type: 'POST',
        dataType: 'json',
        url: _genUrl(appLinks.userClearApiToken, params),
        beforeSend: _createAjaxSendTokensHandler('api_req_tokens'),
        success: function (data, status, jqxhr) {
            if (data.error) {
                tokenAjaxError(data.error);
            } else if (data.result) {
                //remove row element
                jQuery(elem).fadeOut("slow");
            }
        },
        error: function (jqxhr, status, error) {
            tokenAjaxError(jqxhr.responseJSON && jqxhr.responseJSON.error ? jqxhr.responseJSON.error : error);
        }, complete: function () {
            jQuery('#' + elem.identify() + ' .modal').modal('hide');
        }
    }).success(_createAjaxReceiveTokensHandler('api_req_tokens'));
}
function generateUserToken(login, elem, data) {
    var dom = jQuery('#' + elem);
    jQuery.ajax({
        type: 'POST',
        dataType: 'json',
        url: _genUrl(appLinks.userGenerateUserToken),
        data: data,
        beforeSend: _createAjaxSendTokensHandler('api_req_tokens'),
        success: function (data, status, jqxhr) {
            if (data.result) {
                addTokenRow(elem, login, data.tokenid);
            } else {
                tokenAjaxError(data.error);
            }
        },
        error: function (jqxhr, status, error) {
            tokenAjaxError(jqxhr.responseJSON && jqxhr.responseJSON.error ? jqxhr.responseJSON.error : error);
        }
    })
        .success(_createAjaxReceiveTokensHandler('api_req_tokens'))
        .then(function () {
            jQuery('#gentokenmodal').modal('hide');
        });
}

function revealUserToken(login, elem) {
    var dom = jQuery(elem);
    var tokenid = dom.data('tokenId');
    var holder = jQuery('.token-data-holder[data-token-id=' + tokenid + ']');
    jQuery.ajax({
        type: 'POST',
        dataType: 'json',
        url: _genUrl(appLinks.userRevealTokenData, {login: login, tokenid: tokenid}),
        beforeSend: _createAjaxSendTokensHandler('api_req_tokens'),
        success: function (data, status, jqxhr) {
            if (data.result) {
                dom.hide();
                holder.collapse();
                holder.html('<code>' + data.apitoken + '</code>');
            } else {
                tokenAjaxError(data.error);
            }
        },
        error: function (jqxhr, status, error) {
            tokenAjaxError(jqxhr.responseJSON && jqxhr.responseJSON.error ? jqxhr.responseJSON.error : error);
        }
    }).success(_createAjaxReceiveTokensHandler('api_req_tokens'));
}


jQuery(function () {
    var data = loadJsonData('genPageData');
    jQuery("#language").val(data.language);
    jQuery(' .apitokenform.newtoken').fadeTo('slow', 1);
    jQuery(document).on('click', '.obs_reveal_token', function (e) {
        revealUserToken(data.user, jQuery(e.target));
    });
    jQuery(document).on('click', '.clearconfirm input.yes', function (e) {
        e.preventDefault();
        clearToken(jQuery(e.target).closest('.apitokenform')[0]);
        return false;
    });
    var dom = jQuery('#gentokensection');
    if (dom.length == 1) {
        var roleset = new RoleSet(data.roles);
        window.tokencreator = new TokenCreator({roleset: roleset, user: data.user, adminAuth: data.adminAuth,
            userTokenAuth: data.userTokenAuth, svcTokenAuth: data.svcTokenAuth});
        ko.applyBindings(tokencreator, dom[0]);
    }
});
