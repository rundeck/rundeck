 

 /**
 * Prevent buttons from gaining focus on mousedown event.
 */
 export function DisableButtonClickFocus() {
    const observer = new MutationObserver(disableButtonClickFocus)
    observer.observe(document.documentElement, {childList: true, subtree: true})
}

function preventDefault(e: MouseEvent) { e.preventDefault() }
function disableButtonClickFocus(e: MutationRecord[]) {
    for(const record of e) {
        if (record.type == 'childList')
            for(const node of record.addedNodes)
                if (node.nodeType == Node.ELEMENT_NODE) {
                    const el = node as HTMLElement
                    for (const child of [el, ...el.querySelectorAll('*')])
                        if (child.classList?.contains('btn'))
                            (<HTMLElement>child).addEventListener('mousedown', preventDefault)
                }
    }
}