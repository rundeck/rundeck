
export function splitAtCapitalLetter(val: string) {
    if (!val) return "";
    val = val.toString();
    if (val.match(/^[A-Z]+$/g)) return val;
    return val.match(/[A-Z][a-z]+|[0-9]+/g).join(" ");
}

export function limitString200ClickForMore(val: string) {
    if(!val) return ""
    if (val.length > 200) {
        return val.substring(0, 140) + "... click to read more";
    } else {
        return val;
    }
}

export function truncate(val: string, len: number) {
    if(!val) return ""
    if(val.length < len) return val
    return val.substring(0, len)
}