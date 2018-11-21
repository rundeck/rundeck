export function getRundeckContext() {
    return window._rundeck;
}
export function getSynchronizerToken() {
    const tokenString = document.getElementById('web_ui_token').innerText;
    return JSON.parse(tokenString);
}
//# sourceMappingURL=rundeckService.js.map