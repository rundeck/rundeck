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
  <pagination v-model="currentPage" :total-pages="totalPages" @change="changePage($event)" :disabled="disabled" v-if="pagination.total">
    <span slot="prefix" v-if="showPrefix">
      <span class="text-info">{{pagination.offset + 1}}-{{pagination.offset + pagination.max}}</span>
      <span class="text-muted">of {{pagination.total}}</span>
    </span>
  </pagination>
</template>
<script lang="ts">
import Vue from 'vue'
import {Component, Prop} from 'vue-property-decorator'

import Pagination from './Pagination.vue'

Vue.component('pagination', Pagination)

@Component
export default class OffsetPagination extends Vue {
  currentPage: number=0

  @Prop({required: true})
  pagination: any

  @Prop({required:false,default:true})
  showPrefix!: boolean

  @Prop({default: false})
  disabled!: boolean

  // Computed properties are getters/setters
  get totalPages() {
    return Math.ceil(this.pagination.total / this.pagination.max)
  }

  mounted() {
    this.currentPage = this.pageNumberForOffset(this.pagination.offset)
  }

  changePage (page: number) {
      this.$emit('change', this.pageOffset(page))
  }
  pageOffset (page: number) {
    return (page - 1) * this.pagination.max
  }

  pageNumberForOffset(offset: number) {
    return 1 + (offset / this.pagination.max)
  }
}
</script>
