import moment, {MomentInput} from "moment";

function formatDateFull(val: MomentInput) {
    return moment(val).format("MM/DD/YYYY hh:mm a")
}

function formatDate(val: MomentInput, format: string) {
    return moment(val).format(format)
}

function formatCalendar(val: MomentInput) {
    return moment(val).calendar()
}

function formatFromNow(val:MomentInput) {
    return moment(val).fromNow()
}

function formatReleaseDate(val: MomentInput) {
    return moment(val).format("M/D/YYYY")
}

function formatPublishDate(val: MomentInput) {
    return moment(val).format("MMMM Do YYYY hh:mm")
}

function formatKeyStorageDate(val: MomentInput) {
    return moment(val).format("dddd, MMMM Do YYYY, h:mm:ss a")
}

function formatHumanizedDuration(val: number) {
    return moment.duration(val).humanize()
}

export {
    formatDateFull,
    formatDate,
    formatCalendar,
    formatFromNow,
    formatReleaseDate,
    formatPublishDate,
    formatKeyStorageDate,
    formatHumanizedDuration,
}
