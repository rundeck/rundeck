import Vue from 'vue'
import copyBox from '@rundeck/ui-trellis/lib/components/containers/copybox/CopyBox.vue'
// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.


window.addEventListener('DOMContentLoaded', init)
function init(){
    const els = document.body.getElementsByClassName('vue-copybox')
    if (!els)
        return

    for (var i = 0; i < els.length; i++) {
        const e = els[i]
        /* eslint-disable no-new */
        new Vue({
            el: e,
            components: { copyBox }
        })
    }
}

