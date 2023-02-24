
/**
 * Copies supplied text to clipboard. Falls back on text area
 * if navitagor clipboard or writeText is missing. This can
 * occur if the host is not local and/or is not secure(https).
 */
export async function CopyToClipboard(text: string) {
    if (navigator.clipboard && navigator.clipboard.writeText) {
        await navigator.clipboard.writeText(text)
    } else {
        let textArea = document.createElement("textarea")
        textArea.value = text
        // make the textarea out of viewport
        textArea.style.position = "fixed"
        textArea.style.left = "-999999px"
        textArea.style.top = "-999999px"
        document.body.appendChild(textArea)
        textArea.focus()
        textArea.select()
        return new Promise((res, rej) => {
            document.execCommand('copy') ? res(void(0)) : rej();
            textArea.remove();
        });
    }
}