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

function loadTab (tabsid, anchor) {
    "use strict"
    var tabs = jQuery(tabsid).find('a[data-toggle="tab"]').map(function (i, e) {
        return jQuery(e).attr('href')
    }).get()
    if (tabs.indexOf(anchor) >= 0) {
        jQuery('a[href="' + anchor + '"]').tab('show')
    }
}

function tab_from_hash (prefix, hash) {
    if (prefix) {
        return '#' + prefix + hash.substring(1)
    } else {
        return hash
    }
}

function hash_from_tab (prefix, hash) {
    if (prefix && hash.startsWith('#' + prefix)) {
        return '#' + hash.substring(prefix.length + 1)
    } else {
        return hash
    }
}

function setupTabRouter (tabid, prefix) {
    loadTab(tabid, tab_from_hash(prefix, document.location.hash))
    jQuery(window).on('hashchange', function () {
        loadTab(tabid, tab_from_hash(prefix, document.location.hash))
    })
    jQuery(tabid).on('show.bs.tab', function (e) {
        var t = jQuery(e.target)
        if (t.attr('href').startsWith('#')) {
            document.location.hash = hash_from_tab(prefix, t.attr('href'))
        }
    })
}
