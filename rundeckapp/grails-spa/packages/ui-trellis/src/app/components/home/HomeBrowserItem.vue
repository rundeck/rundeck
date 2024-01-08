<template>
  <div class="project_list_item" :key="`project${index}`">
    <div class="row row-hover row-border-top">
      <div class="col-sm-6 col-md-8">
        <a
          :href="`/?project=${project.name}`"
          class="link-hover text-inverse project_list_item_link link-quiet"
          :id="`project${index}`"
        >
          <p class="h5">
            {{ project.label ? project.label : project.name }}
          </p>

          <tooltip :text="$t('project.execution.disabled')" data-placement="right" class="h5" v-if="!executionsEnabled">
            <span
                class="text-base text-warning"
                :data-container="`#project${index}`"
            >
              <i class="glyphicon glyphicon-pause"></i>
            </span>
          </tooltip>

          <tooltip :text="$t('project.schedule.disabled')" data-placement="right" class="h5" v-if="!scheduleEnabled">
            <span
                class="text-base text-warning"
                :data-container="`#project${index}`"
            >
              <i class="glyphicon glyphicon-ban-circle"></i>
            </span>
          </tooltip>

          <span
            v-if="project.description.length > 0"
            class="text-secondary text-base"
          >
            {{ project.description }}
          </span>
        </a>
      </div>
      <div class="col-sm-6 col-md-2 text-center">
        <a
          :href="`/project/${project.name}/activity`"
          class="as-block link-hover link-block-padded text-inverse"
          :class="{ 'text-secondary': project.execCount < 1 }"
          data-toggle="popover"
          data-placement="bottom"
          data-trigger="hover"
          data-container="body"
          data-delay='{"show":0,"hide":200}'
          data-popover-template-class="popover-wide popover-primary"
          :bootstrapPopover="true"
          :bootstrapPopoverContentRef="`#exec_detail_${project}`"
        >
          <span
            class="summary-count"
            :class="{ 'text-info': project.execCount > 0 }"
          >
            <span v-if="!project.loaded">...</span>
            <span v-else>
              <span v-if="project.execCount > 0">
                <span class="text-h3">
                  {{ project.execCount }}
                </span>
              </span>
              <span v-if="project.execCount < 1">None</span>
            </span>
          </span>
        </a>

        <div
          v-if="project.userCount > 0"
          :id="`exec_detail_${project}`"
          style="display: none"
        >
          <span v-if="project.execCount > 0">
            {{ project.execCount }}
          </span>
          <span>
            {{ $tc("Execution", project.execCount) }}
          </span>
          {{ $t("page.home.duration.in.the.last.day") }}
          {{ $t("by") }}
          <span class="text-info">
            {{ project.userCount }}
          </span>
          <span> {{ $tc("user", project.userCount) }}: </span>
          <span>
            {{ project.userSummary.join(", ") }}
          </span>
        </div>

        <a
          v-if="project.failedCount > 0"
          class="text-warning"
          :href="`/project/${project.name}/activity?statFilter=fail`"
        >
          <span>
            {{ project.failedCount }}{{ $t("page.home.project.executions.0.failed.parenthetical") }}
          </span>
        </a>
      </div>
      <div class="col-sm-12 col-md-2 col-last">
        <HomeActionsMenu :index="index" :project="project" />
      </div>
    </div>
  </div>

  <div>
    <div class="row" v-if="showMessage">
      <div class="project_list_readme col-sm-10 col-sm-offset-1 col-xs-12">
        <div v-if="showMotd">
          <span
            v-if="messageMeta.data.readme.motdHTML"
            v-html="messageMeta.data.readme.motdHTML"
          ></span>
        </div>
        <div v-if="showReadme">
          <div>
            <span
              v-if="messageMeta.data.readme.readmeHTML"
              v-html="messageMeta.data.readme.readmeHTML"
            ></span>
          </div>
        </div>
      </div>
    </div>
    <template v-if="project.extra">
      <div v-for="extra in project.extra" v-html="extra"></div>
    </template>
  </div>
</template>

<script lang="ts">
import {defineComponent, PropType} from "vue";
import UiSocket from "@/library/components/utils/UiSocket.vue";
import HomeActionsMenu from "@/app/components/home/HomeActionsMenu.vue";
import {ConfigMeta, MessageMeta, Project} from "@/app/components/home/types/projectTypes";

export default defineComponent({
  name: "HomeBrowserItem",
  components: {HomeActionsMenu, UiSocket },
  props: {
    project: {
      type: Object as PropType<Project>,
      required: true,
    },
    index: {
      type: Number,
      required: true,
    },
  },
  computed: {
    configSettings(): ConfigMeta {
      const emptyConfig = {name: 'config', data: {}};
      return this.project.meta.filter(metaObject => metaObject.name === 'config')[0] || emptyConfig;
    },
    messageMeta(): MessageMeta {
      const emptyMessageMeta = {name: 'message', data: {}};
      return this.project.meta.filter(metaObject => metaObject.name === 'message')[0] || emptyMessageMeta;
    },
    executionsEnabled(): boolean {
      return this.configSettings.data.executionsEnabled || false;
    },
    scheduleEnabled(): boolean {
      return this.configSettings.data.scheduleEnabled || false;
    },
    showMotd(): boolean {
      if(Object.keys(this.messageMeta.data.readme).length === 0) return false;

      const { readme, motdDisplay } = this.messageMeta.data;
      return readme.motdHTML && motdDisplay.includes('projectList');
    },
    showReadme(): boolean {
      if(Object.keys(this.messageMeta.data.readme).length === 0) return false;

      const { readmeDisplay, readme } = this.messageMeta.data;
      return readme.readmeHTML && readmeDisplay.includes('projectList')
    },
    showMessage(): boolean {
      if(Object.keys(this.messageMeta.data.readme).length === 0) return false;

      const { readme } = this.messageMeta.data;
      return readme && (this.showMotd || this.showReadme);
    }
  }
});
</script>