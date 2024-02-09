<template>
  <div class="row homeHeader" id="homeHeader">
    <div class="flex justify-between">
      <div class="col-sm-12 col-md-5 card">
        <div class="card-content flex flex--justify-between flex--items-center h-full">
          <h3 id="projectCount" class="text-h3" v-if="loadedProjectNames">
            <span id="projectCountNumber">{{ projectCount }}</span> {{ projectCount === 1 ? $t('page.home.section.project.title') : $t('page.home.section.project.title.plural') }}
          </h3>
          <span class="text-h3 text-muted loading" v-if="!loadedProjectNames">
                <b class="fas fa-spinner fa-spin loading-spinner"></b>
                {{ $t('page.home.loading.projects') }}
          </span>
          <a v-if="createProjectAllowed" :href="createProjectLink" id="createProject" class="btn btn-primary pull-right">
            {{ $t('page.home.new.project.button.label') }}
            <b class="glyphicon glyphicon-plus"></b>
          </a>
        </div>
      </div>
      <div class="col-sm-12 col-md-7 card">
        <div class="card-content flex flex--direction-col flex--justify-center h-full">
          <div v-if="projectCount && loaded">
            <h4 class="h4">
              <span class="summary-count" :class="[execCount ? 'text-info': 'text-strong']">
                {{ execCount }}
              </span>
              <span>
                {{ execCount === 1 ? $t('Execution') : $t('Executions') }}
              </span>
              {{ $t('page.home.duration.in.the.last.day') }}
              <span class="summary-count" :class="[totalFailedCount? 'text-warning': 'text-strong']">
                {{ $t('page.home.project.executions.0.failed.parenthetical', [totalFailedCount]) }}
              </span>
            </h4>

            <p class="margin-0" v-if="recentProjectsCount > 1">
              {{ $t('in') }}
              <span class="text-info">
                {{ recentProjectsCount }}
              </span>
              {{ $t('projects') }}:
              <a class="project-link" v-for="project in recentProjects" :href="projectLink(project)">
                {{ project }}
              </a>
            </p>
            <p class="margin-0" v-if="recentUsersCount">
              {{ $t('by') }}
              <span class="text-info">
                {{ recentUsersCount }}
              </span>
              {{ recentUsersCount === 1 ? $t('user') : $t('users') }}: {{ recentUsers.join(', ') }}
            </p>
          </div>
          <p v-else class="text-muted">
              ...
          </p>
        </div>
      </div>
    </div>
  </div>

</template>
<script lang="ts">
import {defineComponent} from 'vue'
import {getAppLinks, getRundeckContext} from "@/library";
import {getSummary} from "./services/homeServices";

export default defineComponent({
  name: "HomeHeader",
  props: {
    createProjectAllowed: {
      type: Boolean,
      default: false
    },
    projectCount: {
      type: Number,
      default: 0
    },
    summaryRefresh: {
      type: Boolean,
      default: false,
    },
    refreshDelay: {
      type: Number,
      default: 30000
    }
  },
  data() {
    return {
      recentUsers: [],
      recentProjects: [],
      loaded: false,
      execCount: 0,
      totalFailedCount: 0,
    }
  },
  computed: {
    createProjectLink(): string {
      return getAppLinks().frameworkCreateProject
    },
    recentProjectsCount(): number {
      return this.recentProjects?.length || 0
    },
    recentUsersCount(): number {
      return this.recentUsers?.length || 0
    },
    loadedProjectNames(): boolean {
      return !!this.projectCount
    }
  },
  methods: {
    projectLink(project: string): string {
      return `${getRundeckContext().rdBase}/project/${project}/`
    },
    async fetchSummary() {
      try {
        const response = await getSummary();
        if(response){
          this.execCount = response.execCount;
          this.totalFailedCount = response.totalFailedCount;
          this.recentProjects = response.recentProjects;
          this.recentUsers = response.recentUsers;

          if(this.summaryRefresh){
            setTimeout(() => {
              this.fetchSummary();
            }, this.refreshDelay);
          }
        }
      } catch (e) {
        console.error(e);
      } finally {
        this.loaded = true;
      }
    }
  },
  mounted() {
    this.fetchSummary();
  }
})
</script>

<style lang="scss">
.homeHeader {
  .card {
    margin: 0 15px 20px;
  }

  .text-h3 {
    margin: 0;
    display: inline;
  }

  .h4 {
    margin: 0;

    .summary-count {
      margin-right: 6px;
    }
  }

  .margin-0 {
    margin: 0;
  }

  .project-link {
    display: inline;
    margin-right: 6px;
  }
}
</style>