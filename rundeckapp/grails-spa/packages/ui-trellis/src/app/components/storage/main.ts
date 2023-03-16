import Vue from 'vue'
import KeyStorageView from "../../../library/components/storage/KeyStorageView.vue";
window.addEventListener('DOMContentLoaded', initUtil)
function initUtil() {
    const elm = document.getElementsByClassName('keyStorageView')
    console.log("storage initUtil")
    console.log(elm)
    const vue = new Vue({
        el: elm[0],
        components: {KeyStorageView},
        template: `<KeyStorageView />`,
    })
}