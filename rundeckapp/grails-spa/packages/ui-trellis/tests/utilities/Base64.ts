export function BtoA(data: any) {
    if (typeof btoa == 'undefined')
        return Buffer.from(data).toString('base64')
    else
        return btoa(data)
}

export function AtoB(str: string) {
    if (typeof btoa == 'undefined')
        return Buffer.from(str, 'base64').toString()
    else
        return atob(str)
}