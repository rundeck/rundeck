import {VNode} from 'vue'

/* Extracts default slot for rendering in a way that avoids a wrapper div if possible */
export function bareSlot(comp: Object) {
    let component: VNode
    //@ts-ignore
    if (! comp.$slots.default?.length)
        //@ts-ignore
        component = comp.$createElement('div')
    //@ts-ignore
    else if (comp.$slots.default.length > 1)
        //@ts-ignore
        component = comp.$createElement('div', comp.$slots.default)
    else
        //@ts-ignore
        component = comp.$slots.default[0]

    return component
}