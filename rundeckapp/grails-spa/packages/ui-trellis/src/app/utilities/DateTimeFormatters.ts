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

export function formatKeyStorageDate(val: MomentInput) {
    return moment(val).format("dddd, MMMM Do YYYY, h:mm:ss a")
}

export function formatHumanizedDuration(val: number) {
    return moment.duration(val).humanize()
}