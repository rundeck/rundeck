<!--
  - Copyright 2018 Rundeck, Inc. (http://rundeck.com)
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -     http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  -->

<template>
  <div v-if="totalPages>1" class="paging_links">
    <nav aria-label="Paging">
      <ul class="pagination pagination-sm">
        <li class="pagination-text" v-if="$slots['prefix']">
          <slot name="prefix"></slot>
        </li>
        <li :class="{disabled:!hasPreviousButton||disabled}">
          <a href="#"
             @click.prevent="changePage(modelValue-1)"
             title="Previous Page"
             :class="navigationClass">
            <slot name="prevPage"><i class="glyphicon glyphicon-arrow-left"></i></slot>
          </a>
        </li>
        <li v-for="(page, index) in pageList" :key="page.page+'/'+index" :class="{[skipClass]:page.skip,active:page.page===modelValue,disabled:disabled}">
          <span v-if="page.skip"><slot name="skip">&hellip;</slot></span>
          <a v-else-if="page.page!==modelValue"
             href="#"
             @click.prevent="changePage(page.page)"
             :class="navigationClass"
             :title="'Page '+page.page">{{page.page}}</a>
          <span v-else
                :class="navigationClass"
                :title="'Page '+page.page">{{page.page}}</span>
        </li>
        <li :class="{disabled:!hasNextButton||disabled}">
          <a href="#"
             @click.prevent="changePage(modelValue+1)"
             :class="navigationClass"
             title="Next Page">
            <slot name="nextPage"><i class="glyphicon glyphicon-arrow-right"></i></slot>
          </a>
        </li>
      </ul>
    </nav>
  </div>
</template>
<script lang="ts">
import {defineComponent} from 'vue'

  export default defineComponent({
    name: 'Pagination',
    props: {
      modelValue: {
        type: Number,
        required: true,
      },
      totalPages: {
        type: Number,
        required: true,
      },
      disabled: {
        type: Boolean,
        required: false,
        default: false,
      },
      navigationClass: {
        type: String,
        required: false,
        default:'page_nav_btn',
      },
      navigationDisabledClass: {
        type: String,
        required: false,
        default:'page_nav_btn_disabled',
      },
      currentPageClass: {
        type: String,
        required: false,
        default: 'page_current',
      },
      skipClass: {
        type: String,
        required: false,
        default: 'text-muted',
      },
      pagingWindowSize: {
        type: Number,
        required: false,
        default: 7,
      },
    },
    emits: ['update:modelValue', 'change'],
    computed: {
      maxPagesDisplay() {
        return Math.min(this.totalPages, this.pagingWindowSize)
      },
      windowLeftPage() {
        const leftNum = Math.floor(this.maxPagesDisplay / 2)
        const windowLeft = this.modelValue - leftNum

        const adjustL = windowLeft < 1 ? 1 - windowLeft : 0

        return windowLeft + adjustL
      },
      windowRightPage() {
        return this.windowLeftPage + (this.maxPagesDisplay - 1)
      },
      /**
       * Create list of page links to display
       */
      pageList() {
        const pages: any[] = []
        let skipped = false
        const curPage = this.modelValue

        // creates sliding window of size pagingWindowSize


        const totalPages1 = this.totalPages

        // assume total pages >1 because we do not show paging otherwise
        // and always show last page

        const minPage = this.windowLeftPage
        const maxPage = this.windowRightPage

        // always add first page
        pages.push({page: 1})

        for (let i = 2; i < totalPages1; i++) {
          let shouldSkip = false
          if (i < minPage || i > maxPage) {
            shouldSkip = true
          }
          if (!skipped && shouldSkip) {
            skipped = true
            pages.push({skip: true})
            continue
          } else if (skipped && !shouldSkip) {
            skipped = false
          }
          if (!skipped) {
            pages.push({page: i})
          }
        }

        // always add last page
        pages.push({page: totalPages1})

        return pages
      },
      hasPreviousButton(): boolean {
        return this.modelValue > 1
      },
      hasNextButton(): boolean {
        return this.modelValue < this.totalPages
      },
    },
    methods: {
      changePage(page: number) {
        if (!this.disabled && page > 0 && page <= this.totalPages && page !== this.modelValue) {
          this.$emit('update:modelValue', page)
          this.$emit('change', page)
        }
      },
    },
  })

</script>
<style scoped lang="scss">
.pagination > li > a,
.pagination > li > span {
  height: 2em;
  min-width: 3em;
  padding: 0;
}
.pagination > li.pagination-text > span {
  border: none;
  margin-top: 1px;
  margin-right: 1em;
}
</style>
