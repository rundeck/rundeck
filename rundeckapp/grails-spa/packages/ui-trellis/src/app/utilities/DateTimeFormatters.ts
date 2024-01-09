import moment, {MomentInput} from "moment";

export function formatDateFull(val: MomentInput) {
    return moment(val).format("MM/DD/YYYY hh:mm a")
}

export function formatDate(val: MomentInput, format: string) {
    return moment(val).format(format)
}

export function formatCalendar(val: MomentInput) {
    return moment(val).calendar()
}

export function formatFromNow(val:MomentInput) {
    return moment(val).fromNow()
}

export function formatReleaseDate(val: MomentInput) {
    return moment(val).format("M/D/YYYY")
}

export function formatPublishDate(val: MomentInput) {
    return moment(val).format("MMMM Do YYYY hh:mm")
}

export function formatTimeSimple(val: MomentInput) {
    return moment(val).format('h:mm:ss a')
}

export function formatTimeAtDate (text:MomentInput){
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
}
export function formatFromNowSimple (time:MomentInput) {
    return formatDurationSimple(moment(time).diff(moment.now()))
}
export function formatDurationSimple (ms:number) {
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
}
export function formatDurationFromNowHumanize (time:MomentInput) {
    return formatDurationHumanize(moment(time).diff(moment.now()))
}
export function formatDurationHumanize  (ms:number) {
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
}

export function formatDurationFromNowMomentHumanize (time:MomentInput) {
    return formatHumanizedDuration(moment(time).diff(moment.now()))
}
export function formatKeyStorageDate(val: MomentInput) {
    return moment(val).format("dddd, MMMM Do YYYY, h:mm:ss a")
}

export function formatHumanizedDuration(val: number) {
    return moment.duration(val).humanize()
}