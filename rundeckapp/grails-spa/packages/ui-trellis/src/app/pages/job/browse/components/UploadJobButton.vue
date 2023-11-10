<template>
  <a :href="jobPageStore.uploadJobHref()" class="btn" :class="`btn-${btnType}`" v-if="jobPageStore.authz['create']">

    <slot>{{ $t("job.upload.button.title") }}</slot>
  </a>
</template>

<script lang="ts">
import {JobPageStore, JobPageStoreInjectionKey} from '@/library/stores/JobPageStore'
import {defineComponent,inject} from 'vue'

export default defineComponent({
  name: 'UploadJobButton',
  props:{
    btnType: {
      type: String,
      default: 'default',
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
})
</script>

<style scoped lang="scss">

</style>