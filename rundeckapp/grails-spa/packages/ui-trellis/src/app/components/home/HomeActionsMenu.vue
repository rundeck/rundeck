

<template>
  <dropdown menu-right append-to-body ref="dropdown">
    <a role="button" class="as-block link-hover link-quiet link-block-padded text-inverse dropdown-toggle">
      {{ $t('button.Action') }}
      <span class="caret"></span>
    </a>
    <template #dropdown>
      <li v-if="!project.loaded">
        <a href="#" class="text-muted">
          <b class="fas fa-spinner fa-spin loading-spinner text-muted"></b> Loading &hellip;
        </a>
      </li>
      <template v-else>
        <li v-if="project.auth.admin">
          <a :href="`/project/${project.name}/configure`">
            {{ $t('edit.configuration') }}
          </a>
        </li>

        <li class="divider" v-if="project.auth.admin"></li>

        <template v-if="project.auth.jobCreate">
          <li>
            <a :href="`/project/${project.name}/job/create`">
              <i class="glyphicon glyphicon-plus"></i>
              {{ $t('new.job.button.label') }}
            </a>
          </li>
          <li class="divider"></li>
          <li>
            <a :href="`/project/${project.name}/job/upload`">
              <i class="glyphicon glyphicon-upload"></i>
              {{ $t('upload.definition.button.label') }}
            </a>
          </li>
        </template>
      </template>
    </template>
  </dropdown>
</template>

<script lang="ts">
import {defineComponent} from 'vue'

export default defineComponent({
  name: "HomeActionsMenu",
  props: {
    project: {
      type: Object,
      required: true,
    },
    index: {
      type: Number,
      required: true,
    },
  },
  data() {
    return {
      show: false,
    }
  },
  mounted() {
    this.show = true;
  }
})
</script>