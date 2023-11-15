<template>
  <a :href="jobPageStore.createJobHref()" class="btn" :class="`btn-${btnType}`" v-if="jobPageStore.jobAuthz['create']">
    <i class="glyphicon glyphicon-plus"></i>
    <slot>{{ $t("new.job.button.label") }}</slot>
  </a>
</template>
<script lang="ts">
import {JobPageStore, JobPageStoreInjectionKey} from '@/library/stores/JobPageStore'
import {inject} from 'vue'
export default {
  name: 'create-new-job-button',
  props:{
    btnType: {
      type: String,
      default: 'primary',
    }
  },
  setup(props) {
    const jobPageStore: JobPageStore = inject(
      JobPageStoreInjectionKey
    ) as JobPageStore;
    return {
      jobPageStore,
    };
  }
}
</script>