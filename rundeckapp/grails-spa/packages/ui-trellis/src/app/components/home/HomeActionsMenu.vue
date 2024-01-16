<template>
  <dropdown menu-right append-to-body ref="dropdown">
    <a role="button" class="as-block link-hover link-quiet link-block-padded text-inverse dropdown-toggle">
      {{ $t('button.Action') }}
      <span class="caret"></span>
    </a>
    <template #dropdown>
      <template v-for="(option, index) in availableOptions">
        <li v-if="option.show" :key="`item${index}`">
          <a :href="option.link">
            <i v-if="option.icon" :class="`glyphicon ${option.icon}`"></i>
            {{ $t(option.text) }}
          </a>
        </li>
        <li class="divider" v-if="option.show && index !== availableOptions.length -1 && visibleOptions > 1"></li>
      </template>
    </template>
  </dropdown>
</template>

<script lang="ts">
import {defineComponent} from 'vue'
import {AuthzMeta, ProjectActionsItemDropdown} from "@/app/components/home/types/projectTypes";

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
  computed: {
    authConfig(): AuthzMeta {
      const emptyConfig = {name: 'authz', data: {}};
      return this.project.meta.filter(meta => meta.name === 'authz')[0] || emptyConfig;
    },
    isAdmin(): boolean {
      const values: boolean[] = Object.values(this.authConfig.data.project);
      return values.some(val => val === true);
    },
    availableOptions(): ProjectActionsItemDropdown[] {
      const createPermissions = !!this.authConfig.data.types.job.create;

      return [{
          show: this.isAdmin,
          link: `/project/${this.project.name}/configure`,
          text: 'edit.configuration',
        },
        {
          show: createPermissions,
          link: `/project/${this.project.name}/job/create`,
          icon: 'glyphicon-plus',
          text: 'new.job.button.label',
        },
        {
          show: createPermissions,
          link: `/project/${this.project.name}/job/upload`,
          icon: 'glyphicon-upload',
          text: 'upload.definition.button.label',
        },
      ]
    },
    visibleOptions(): number {
      return this.availableOptions.filter(option => option.show === true).length;
    }
  }
})
</script>