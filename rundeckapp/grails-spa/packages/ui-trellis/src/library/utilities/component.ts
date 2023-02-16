import Vue, {VNode} from 'vue'

/* Extracts default slot for rendering in a way that avoids a wrapper div if possible */
export function bareSlot(comp: Vue) {
    let component: VNode
    if (! comp.$slots.default?.length)
        component = comp.$createElement('div')
    else if (comp.$slots.default.length > 1)
        component = comp.$createElement('div', comp.$slots.default)
    else
        component = comp.$slots.default[0]

    return component
}