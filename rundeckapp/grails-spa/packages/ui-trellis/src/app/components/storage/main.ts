import Vue from 'vue'
import KeyStorageView from "../../../library/components/storage/KeyStorageView.vue";

function initUtil() {
    const elm = document.getElementById('keyStorageView') as HTMLElement

    const vue = new Vue({
        el: elm,
        components: {KeyStorageView},
        template: `<KeyStorageView />`,
    })
}