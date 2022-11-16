/*
 * Copyright 2019 Rundeck, Inc. (http://rundeck.com)
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


function JobFilter (data, filters) {
    var self = this
    self.name = ko.observable(data.name)
    self.jobFilter = ko.observable(data.jobFilter)
    self.projFilter = ko.observable(data.projFilter)
    self.groupPath = ko.observable(data.groupPath)
    self.descFilter = ko.observable(data.descFilter)
    self.loglevelFilter = ko.observable(data.loglevelFilter)
    self.idlist = ko.observable(data.idlist)
    self.scheduledFilter = ko.observable(data.scheduledFilter)
    self.serverNodeUUIDFilter = ko.observable(data.serverNodeUUIDFilter)
    self.filterKeyLabel = function (key) {
        return key ? message('jobquery.title.' + key) : ''
    }
    self.url = ko.pureComputed(function () {
        return filters.genFilterNameUrl(self.name())
    })
}

function JobFilters (data) {
    var self = this
    self.error = ko.observable()

    self.redirectUrl = ko.observable()
    self.currentFilter = ko.observable()
    self.filters = ko.observableArray()
    self.totalCount = ko.observable(0)
    self.filterToDelete = ko.observable()
    self.newFilterName = ko.observable('')
    self.newFilterError = ko.observable()

    self.findFilterByName = function (name) {
        var found = ko.utils.arrayFilter(self.filters(), function (e) {
            return e.name() == name
        })
        if (found && found.length == 1) {
            return found[0]
        } else {
            return null
        }
    }

    self.deleteFilterConfirm = function (filter) {
        if (typeof (filter) == 'string') {
            filter = self.findFilterByName(filter)
            if (!filter) {
                return
            }
        }
        self.filterToDelete(filter)
        jQuery('#deleteJobFilterKOModal').modal('show')
    }

    self.deleteCurrentFilterConfirm = function () {
        var filter
        if (typeof (self.currentFilter()) == 'string') {
            filter = self.findFilterByName(self.currentFilter())
            if (!filter) {
                return
            }
        }
        self.filterToDelete(filter)
        jQuery('#deleteJobFilterKOModal').modal('show')
    }
    self.deleteFilter = function (filter) {

        jQuery('#deleteJobFilterKOModal').modal('hide')
        jQuery.ajax({
            method: 'post',
            url: _genUrl(appLinks.menuDeleteJobFilterAjax, {filtername: filter.name()}),
            beforeSend: _createAjaxSendTokensHandler('ajaxFilterTokens')
        }).done(function (resp, status, jqxhr) {
            self.filterToDelete(null)
            self.filters.remove(filter)
            document.location = self.redirectUrl()
        }).done(_createAjaxReceiveTokensHandler('ajaxFilterTokens'))
    }
    self.genFilterNameUrl = function (filterName) {
        return _genUrl(appLinks.menuJobs, {filterName: filterName})
    }
    self.genFilterUrl = function (filter) {
        self.genFilterNameUrl(filter.name())
    }
    self.redirectFilterName = function (filterName) {
        document.location = self.genFilterNameUrl(filterName)
    }
    self.redirectFilter = function (filter) {
        self.redirectFilterName(filter.name())
    }
    self.saveFilter = function () {

        self.newFilterError(null)
        var queryParamsData = jQueryFormData(
            jQuery('#jobs_filters'),
            null,
            null,
            ['SYNCHRONIZER', '_', 'max', 'offset']
        )
        queryParamsData.newFilterName = self.newFilterName()
        console.log("query data", queryParamsData)
        jQuery.ajax({
            method: 'post',
            url: _genUrl(appLinks.menuSaveJobFilterAjax),
            beforeSend: _createAjaxSendTokensHandler('ajaxFilterTokens'),
            data: JSON.stringify(queryParamsData),
            contentType: 'application/json',
            dataType: 'json'
        }).fail(function (resp, status, error) {
            _ajaxReceiveTokens('ajaxFilterTokens', null, status, resp)
            if (resp.responseJSON && resp.responseJSON.message) {
                self.newFilterError(resp.responseJSON.message)
            } else {
                self.newFilterError("An error occurred: " + error)
            }
        }).done(function (resp, status, jqxhr) {
            jQuery('#saveJobFilterKOModal').modal('hide')
            self.redirectFilterName(self.newFilterName())
        }).done(_createAjaxReceiveTokensHandler('ajaxFilterTokens'))
    }
    if (data) {
        ko.mapping.fromJS(data, {
            filters: {
                key: function (data) {
                    return ko.utils.unwrapObservable(data.name)
                },
                create: function (options) {
                    return new JobFilter(options.data, self)
                }
            }
        }, self)
    }
}
