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


(function () {
    window._rundeckui = {
        addHeaderLink (opts) {
            if (opts.href && opts.text) {
                var link = jQuery(
                    '<a href="" class=""></a>'
                )
                link.text(opts.text)
                link.attr('href', opts.href)

                var container = jQuery('<li class="user-button">' + '</li>').append(link)
                jQuery('#navbar-menu').prepend(container)
                return link
            }
        },
        scheduledExecution: {
            show: {

                addMainCard (opts) {

                    var target = jQuery('#_job_main_placeholder')
                    var card = jQuery(
                        '<div class="row _job_main_element"><div class="col-xs-12"><div class="card"><div class="card-content"></div></div></div></div>')
                    var holder = card.find('.card-content')
                    if (opts.before) {
                        target.before(card)
                    } else {
                        target.after(card)
                    }

                    let content = opts.content
                    if (typeof (content) === 'string') {
                        content = jQuery(content)
                    }

                    holder.append(content)
                    return content
                },
                addJobStatsContent (opts) {

                    var target = jQuery('#_job_stats_main')
                    let content = opts.content
                    if (typeof (content) === 'string') {
                        content = jQuery(content)
                    }
                    if (opts.before) {
                        target.before(content)
                    } else {
                        target.after(content)
                    }
                    return content
                },
                addJobStatsItem (opts) {
                    var div = jQuery('#_job_stats_main .jobstats')
                    if (div.length > 0) {

                        //prepend a new item into the columns listing the job stats
                        let tablerows = div.find('.job-stats-item')
                        if (tablerows.length < 1) {
                            return
                        }
                        let target = tablerows[0]
                        tablerows.removeClass('col-sm-4').addClass('col-sm-3')

                        let content = opts.content
                        if (typeof (content) === 'string') {
                            content = jQuery(content)
                        }
                        if (opts.before) {
                            jQuery(target).before(content)
                        } else {
                            jQuery(target).after(content)
                        }
                        return content
                    }
                    return null
                }
            }
        }
    }
})()
