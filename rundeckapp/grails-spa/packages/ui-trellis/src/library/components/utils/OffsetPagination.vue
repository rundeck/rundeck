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
  <Pagination v-model="currentPage" :total-pages="totalPages" @change="changePage" :disabled="disabled" v-if="pagination.total">
    <template v-if="showPrefix" v-slot:prefix>
    <span>
      <span class="text-info">{{pagination.offset + 1}}-{{pagination.offset + pagination.max}}</span>
      <span class="text-muted">of {{pagination.total}}</span>
    </span>
    </template>
  </Pagination>
</template>
<script lang="ts">
import { defineComponent } from 'vue'
import type { PropType } from 'vue'
import Pagination from './Pagination.vue'
import {Pageable} from "../../interfaces/UiTypes";

export default defineComponent({
  name: 'OffsetPagination',
  components: {
    Pagination,
  },
  props: {
    pagination: {
      type: Object as PropType<Pageable>,
      required: true,
    },
    showPrefix: {
      type: Boolean,
      default: true,
    },
    disabled: {
      type: Boolean,
      default: false,
    }
  },
  emits: ['change'],
  data() {
    return {
      currentPage: 0
    }
  },
  computed: {
    totalPages() {
      return Math.ceil(this.pagination.total / this.pagination.max)
    }
  },
  methods: {
    changePage(page: number) {
      this.$emit('change', this.pageOffset(page))
    },
    pageOffset(page: number) {
      return (page - 1) * this.pagination.max
    },
    pageNumberForOffset(offset: number) {
      return 1 + (offset / this.pagination.max)
    }
  },
  mounted() {
    this.currentPage = this.pageNumberForOffset(this.pagination.offset)
  }
})
</script>
