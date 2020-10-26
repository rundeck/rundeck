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

//= require moment-with-locales
//= require moment-timezone-with-data-10-year-range

 var MomentUtil =(function(){
     return {

    duration : function (start, end) {
        return (end?moment(end):moment()).diff(moment(start));
    },

    formatTime : function (text, format) {
        var time = moment.isMoment(text) ? text : moment(text);
        if (text && time.isValid()) {
            return time.format(format);
        } else {
            return '';
        }
    },
         formatTimeUTC: function (text, format) {
             "use strict";
             return MomentUtil.formatTime(moment.utc(text), format);
         },
    formatTimeSimple : function (text) {
        return MomentUtil.formatTime(text, 'h:mm:ss a');
    },
         formatTimeSimpleUTC: function (text) {
             return MomentUtil.formatTimeUTC(text, 'h:mm:ss a');
         },
    formatTimeAtDate : function (text) {
        var time = moment.isMoment(text) ? text : moment(text);
        if (!text || !time.isValid()) {
            return '';
        }
        var now = moment();
        var ms = now.diff(time);
        if(ms < 0 ){
            ms = -ms
        }
        var since = moment.duration(ms);
        if (since.asDays() < 1 && now.month() == time.month()) {
            //same date
            return time.format('h:mm a');
        }
        if (since.asDays() < 1 && now.month() != time.month()) {
            //within a day
            return time.format('h:mm a');
        } else if (since.asWeeks() < 1) {
            return time.format('ddd h:mm a');
        } else if (time.year() != now.year()) {
            return time.format('MMM Do YYYY h a');
        } else {
            return time.format('M/D ha');
        }
    },
         formatTimeAtDateUTC: function (text) {
             "use strict";

             return MomentUtil.formatTimeAtDate(moment.utc(text));
         },
    formatDurationSimple : function (ms) {
        if (ms < 0) {
            return '';
        }
        var duration = moment.duration(ms);
        var y = 0;
        if (duration.asYears() >= 1) {
            y = Math.floor(duration.asYears());
            duration = duration.subtract(y, 'years');
        }
        var d = 0 ;
        if(duration.asDays()>=1.0){
            d = Math.floor(duration.asDays());
            duration = duration.subtract(d,'days');
        }
        var m = duration.minutes();
        var s = duration.seconds();
        return (y>0?y+'y ':'') + (d>0?d+'d ':'') + duration.hours() + '.' + (m < 10 ? '0' + m : m) + ':' + (s < 10 ? '0' + s : s);
    },
    formatDurationHumanize : function (ms) {
        if (ms < 0) {
            return '';
        }
        var duration = moment.duration(ms);
        var valarr = [];
        if (duration.years() > 0) {
            valarr.push(duration.years() + 'y');
        }
        if (duration.months() > 0) {
            valarr.push(duration.months() + 'M');
        }
        if (duration.days() > 0) {
            valarr.push(duration.days() + 'd');
        }
        if (duration.hours() > 0) {
            valarr.push(duration.hours() + 'h');
        }
        if (duration.minutes() > 0) {
            valarr.push(duration.minutes() + 'm');
        }
        if (duration.seconds() > 0) {
            var s = duration.seconds();
            if (duration.milliseconds() > 0) {
                s++;
            }
            valarr.push(s + 's');
        } else if (ms < 1000) {
            valarr.push('0s');
        }
        return valarr.join(' ');
    },
    formatDurationMomentHumanize : function (ms) {
        if (ms < 0) {
            return '';
        }
        var duration = moment.duration(ms);
        return duration.humanize();
    },
    formatTimeInLocalTimeZone : function(text, format){
        var time = moment.isMoment(text) ? text : moment(text);
        if (text && time.isValid()) {
            return time.local().format(format);
        } else {
            return '';
        }
    }
};
})();
