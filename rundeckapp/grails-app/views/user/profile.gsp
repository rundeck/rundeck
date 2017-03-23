<%@ page import="rundeck.AuthToken; com.dtolabs.rundeck.server.authorization.AuthConstants" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <title>%{--
  - Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -     http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  --}%

    <g:set var="tokenAdmin" value="${auth.resourceAllowedTest(
            kind: 'user',
            action: [AuthConstants.ACTION_ADMIN],
            context: 'application'
    )}"/>
    <g:set var="selfToken" value="${auth.resourceAllowedTest(
            kind: 'apitoken',
            action: [AuthConstants.GENERATE_USER_TOKEN],
            context: 'application'
    )}"/>
    <g:set var="serviceToken" value="${auth.resourceAllowedTest(
            kind: 'apitoken',
            action: [AuthConstants.GENERATE_SERVICE_TOKEN],
            context: 'application'
    )}"/>
    <g:appTitle/> - <g:message code="userController.page.profile.title"/>: ${user.login}</title>
    <g:javascript library="prototype/effects"/>
    <asset:javascript src="knockout.debug.js"/>
    <asset:javascript src="knockout-mapping.js"/>
    <asset:javascript src="knockout-foreachprop.js"/>
    <script type="text/javascript">
        function addTokenRow(elem, login, tokenid) {
            var table = $(elem).down('.apitokentable');
            var row = new Element('tbody');
            table.insert(row);
            $(row).addClassName('apitokenform');
            $(row).style.opacity = 0;
            jQuery(row).load(
                _genUrl(appLinks.userRenderApiToken, {login: login, tokenid: tokenid}),
                function (resp, status, jqxhr) {
                    addRowBehavior($(row));
                    jQuery($(row)).fadeTo("slow",1);
                }
            );
        }
        function tokenAjaxError(msg) {
            jQuery('.gentokenerror-text').text("Error: " + msg);
            jQuery('.gentokenerror').show();
        }
        function revealUserToken(login, elem) {
            var dom = jQuery(elem);
            var tokenid = dom.data('tokenId');
            var holder = jQuery('.token-data-holder[data-token-id=' + tokenid + ']');
            jQuery.ajax({
                type: 'POST',
                dataType: 'json',
                url: _genUrl(appLinks.userRevealTokenData, {login: login, tokenid: tokenid}),
                beforeSend: _ajaxSendTokens.curry('api_req_tokens'),
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
            }).success(_ajaxReceiveTokens.curry('api_req_tokens'));
        }
        function generateUserToken(login, elem, data) {
            var dom = jQuery('#' + elem);
            jQuery.ajax({
                type: 'POST',
                dataType: 'json',
                url: _genUrl(appLinks.userGenerateUserToken),
                data: data,
                beforeSend: _ajaxSendTokens.curry('api_req_tokens'),
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
                .success(_ajaxReceiveTokens.curry('api_req_tokens'))
                .then(function () {
                    jQuery('#gentokenmodal').modal('hide');
                });
        }
        function clearToken(elem) {
            var dom = jQuery(elem);
            var login = dom.find('input[name="login"]').val();
            var nelem = $(elem).up('.userapitoken');
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
                beforeSend: _ajaxSendTokens.curry('api_req_tokens'),
                success: function (data, status, jqxhr) {
                    if (data.error) {
                        tokenAjaxError(data.error);
                    } else if (data.result) {
                        //remove row element
                        jQuery($(elem)).fadeOut("slow");
                    }
                },
                error: function (jqxhr, status, error) {
                    tokenAjaxError(jqxhr.responseJSON && jqxhr.responseJSON.error ? jqxhr.responseJSON.error : error);
                }, complete: function () {
                    jQuery('#' + elem.identify() + ' .modal').modal('hide');
                }
            }).success(_ajaxReceiveTokens.curry('api_req_tokens'));
        }
        function mkhndlr(func) {
            return function (e) {
                e.stop();
                func();
                return false;
            };
        }
        function addRowBehavior(e) {
//        Event.observe(e.down('.clearconfirm input.yes'),'click',mkhndlr(clearToken.curry(e)));
        }
        function highlightNew(elem) {
            jQuery(' .apitokenform.newtoken').fadeTo('slow', 1);
        }
        function changeLanguage() {
            var url = '${g.createLink(controller: 'user', action: 'profile')}';
            window.location.href = url + "?lang=" + jQuery("#language").val();
        }
        function setLanguage() {
            //grails stores current locale in http session under below key
            var selectedLanguage = '${session[org.springframework.web.servlet.i18n.SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME]}';
            jQuery("#language").val(selectedLanguage);
        }
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
        jQuery(function () {
            jQuery(document).on('click', '.obs_reveal_token', function (e) {
                revealUserToken('${enc(js:user.login)}', jQuery(e.target));
            });
            jQuery(document).on('click', '.clearconfirm input.yes', function (e) {
                e.preventDefault();
                clearToken(jQuery(e.target).closest('.apitokenform')[0]);
                return false;
            });
            var dom = jQuery('#gentokensection');
            if (dom.length == 1) {
                var data = loadJsonData('genPageData');
                var roleset = new RoleSet(data.roles);
                window.tokencreator = new TokenCreator({roleset: roleset, user: data.user});
                ko.applyBindings(tokencreator, dom[0]);
            }
        });
    </script>
    <g:embedJSON
            data="${[user: user.login, roles: authRoles, adminAuth: tokenAdmin, userTokenAuth: selfToken, svcTokenAuth: serviceToken]}"
            id="genPageData"></g:embedJSON>
</head>
<body>

<div class="row">
    <div class="col-sm-12">
        <h3>
            <g:link action="profile" params="[login: user.login]">
                <g:icon name="user"/>
                ${user.login}
            </g:link>

            <g:link action="edit"
                    params="[login: user.login]"
                    class="small btn btn-link btn-sm"
                    title="${message(code: 'userController.action.edit.description', args: [user.login])}">
                <g:icon name="edit"/>
                <g:message code="button.Edit.label" />
            </g:link>
        </h3>
    </div>

    <div class="col-sm-12">
        <div class="help-block">
            <g:message code="userController.page.profile.description" />
        </div>
    </div>

    <div class="col-sm-12">
        <span class="pull-right">
            <label for="language"><g:message code="user.profile.language.label"/></label>
            <select name="language" id="language" onchange="changeLanguage();">
                <option value="">English</option>
                <option value="es_419">Español</option>
            </select>
        </span>
    </div>
</div>

<div class="pageBody" id="userProfilePage">
    <g:render template="/common/messages"/>
    <g:jsonToken id='api_req_tokens' url="${request.forwardURI}"/>
    <tmpl:user user="${user}" edit="${true}"/>
</div>
</body>
</html>


