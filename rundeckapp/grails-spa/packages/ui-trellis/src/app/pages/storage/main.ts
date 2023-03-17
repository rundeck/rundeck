import Vue from 'vue'
import KeyStoragePage from "../../../library/components/storage/KeyStoragePage.vue";

const elm = document.getElementById('keyStoragePage')

const vue = new Vue({
    el: elm as Element,
    components: { KeyStoragePage }
})