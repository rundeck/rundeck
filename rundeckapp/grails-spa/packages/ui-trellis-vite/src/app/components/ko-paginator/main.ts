import Vue from 'vue'

import Pagination from '@/library/components/utils/Pagination.vue'

const template = `
    <Pagination 
        :value="activePage"
        :totalPages="totalPages"
        @change="handlePageChange"
    />
    `

/** Adapt KO Pager with Paginator wrapper */
const koPaginator = Vue.extend({
    template,
    components: {Pagination},
    props: ['pager'],
    data() { return {
        activePage: 0,
        totalPages: 0,
    }},

    created() {
        this.activePage = this.pager.pageList().findIndex((p: any) => p.current) + 1
        this.totalPages = this.pager.pageList().length
    },

    mounted() {
        this.pager.pageList.subscribe((pages: any) => {
            this.activePage = pages.findIndex((p: any) => p.current) + 1
            this.totalPages = pages.length
        })
    },

    methods: {
        handlePageChange(page: number) {
            this.pager.setPage(page-1)
        }
    }
})

const mounted = new Map<String, boolean>()

/** Listen to events advertising pagination and mount to elements */
window._rundeck.eventBus.$on('ko-pagination', (event: any) => {
    const {name, pager} = event

    if (!mounted.has(name)) {
        const elements = document.querySelectorAll(`[data-ko-pagination='${name}']`)

        for (const elm of elements) {
            new koPaginator({
                el: elm,
                propsData: {
                    pager
                }
            })
        }

        mounted.set(name, true)
    }
})
