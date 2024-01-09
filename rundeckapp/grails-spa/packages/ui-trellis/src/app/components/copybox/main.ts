import { createApp } from 'vue'
import copyBox from '../../../library/components/containers/copybox/CopyBox.vue'
// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.


window.addEventListener('DOMContentLoaded', init)
function init(){
    const els = document.body.getElementsByClassName('vue-copybox')
    if (!els)
        return

    for (let i = 0; i < els.length; i++) {
        const e = els[i]
        const app = createApp({
            components: { copyBox }
        })
        app.mount(e)
    }
}

