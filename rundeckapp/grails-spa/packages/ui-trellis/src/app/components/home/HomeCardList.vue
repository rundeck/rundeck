<template>
  <div class="card">
    <div id="project-list" class="card-content">
      <HomeSearchBar
          :placeholder="$t('page.home.search.projects.input.placeholder')"
          v-model="search"
          @on-focus="hideResults"
          @on-enter="handleSearch"
          @on-blur="handleSearch"
      />
      <p class="alert alert-info" v-if="showSearchResults && loadedProjectNames">
        <span :class="{ 'text-white': searchedProjectsCount>0, 'text-warning': searchedProjectsCount<1 }">
          {{ $tc('page.home.search.project.title', searchedProjectsCount) }}
        </span>
      </p>

      <div class="project_list_header">
        <div class="row row-border-top p-4">
          <div class="col-sm-6 col-md-8">
            <p class="text-lg">
              {{ $t('home.table.projects') }}
            </p>
          </div>
          <div class="col-sm-6 col-md-2">
            <p class="text-lg">
              {{ $t('home.table.activity') }}
            </p>
          </div>
          <div class="col-sm-12 col-md-2 col-last">
            <p class="text-lg">
              {{ $t('home.table.actions') }}
            </p>
          </div>
        </div>
      </div>

      <HomeBrowser :projects="filteredProjects" />

<!--      <div v-for="(project, index) in filteredProjects">-->
<!--        <div class="project_list_item" :key="`project${index}`">-->
<!--          <div class="row row-hover row-border-top">-->
<!--            <div class="col-sm-6 col-md-8">-->
<!--              <a-->
<!--                  :href="`/?project=${project.name}`"-->
<!--                  class="link-hover text-inverse project_list_item_link link-quiet"-->
<!--                  :id="`project${index}`"-->
<!--              >-->
<!--                <p class="h5">-->
<!--                    {{ project.label ? project.label : project.name }}-->
<!--                </p>-->

<!--                <span class="h5" v-if="typeof project.executionEnabled === 'boolean' && !project.executionEnabled">-->
<!--                  <span-->
<!--                      :title="$t('project.execution.disabled')"-->
<!--                      class="text-base text-warning has_tooltip"-->
<!--                      data-placement="right"-->
<!--                      data-toggle="tooltip"-->
<!--                      :data-container="`#project${index}`"-->
<!--                  >-->
<!--                    <i class="glyphicon glyphicon-pause"></i>-->
<!--                  </span>-->
<!--                </span>-->

<!--                <span class="h5" v-if="typeof project.scheduleEnabled === 'boolean' && !project.scheduleEnabled">-->
<!--                  <span-->
<!--                      class="text-base text-warning has_tooltip"-->
<!--                      data-placement="right"-->
<!--                      data-toggle="tooltip"-->
<!--                      :data-container="`#project${index}`"-->
<!--                      :title="$t('project.schedule.disabled')"-->
<!--                  >-->
<!--                    <i class="glyphicon glyphicon-ban-circle"></i>-->
<!--                  </span>-->
<!--                </span>-->

<!--                <span v-if="project.description.length > 0" class="text-secondary text-base">-->
<!--                  {{ project.description }}-->
<!--                </span>-->
<!--              </a>-->
<!--            </div>-->
<!--            <div class="col-sm-6 col-md-2 text-center">-->
<!--                <a-->
<!--                    :href="`project/${project.name}/activity`"-->
<!--                    class="as-block link-hover link-block-padded text-inverse"-->
<!--                    :class="{ 'text-secondary': project.execCount<1 }"-->
<!--                    data-toggle="popover"-->
<!--                    data-placement="bottom"-->
<!--                    data-trigger="hover"-->
<!--                    data-container="body"-->
<!--                    data-delay="{&quot;show&quot;:0,&quot;hide&quot;:200}"-->
<!--                    data-popover-template-class="popover-wide popover-primary"-->
<!--                    :bootstrapPopover="true"-->
<!--                    :bootstrapPopoverContentRef="`#exec_detail_${project}`"-->
<!--                >-->
<!--                  <span class="summary-count" :class="{ 'text-info':project.execCount>0 }">-->
<!--                    <span v-if="!project.loaded" >...</span>-->
<!--                    <span v-else>-->
<!--                      <span v-if="project.execCount>0">-->
<!--                        <span class="text-h3">-->
<!--                          {{ project.execCount }}-->
<!--                        </span>-->
<!--                      </span>-->
<!--                      <span v-if="project.execCount<1">None</span>-->
<!--                    </span>-->

<!--                  </span>-->
<!--                </a>-->

<!--                <div v-if="project.userCount>0" :id="`exec_detail_${project}`" style="display:none;" >-->
<!--                  <span v-if="project.execCount>0">-->
<!--                    {{project.execCount}}-->
<!--                  </span>-->
<!--                  <span>-->
<!--                    {{ $tc('Execution', project.execCount) }}-->
<!--                  </span>-->
<!--                  {{ $t('page.home.duration.in.the.last.day') }}-->
<!--                  {{ $t('by') }}-->
<!--                  <span class="text-info">-->
<!--                    {{ project.userCount }}-->
<!--                  </span>-->
<!--                  <span>-->
<!--                    {{ $tc('user', project.userCount) }}:-->
<!--                  </span>-->
<!--                  <span>-->
<!--                    {{ project.userSummary.join(', ') }}-->
<!--                  </span>-->
<!--                </div>-->
<!--                <span v-if="project.failedCount>0">-->
<!--                  <a-->
<!--                      class="text-warning"-->
<!--                      :href="`project/${project.name}/activity?statFilter=fail`"-->
<!--                  >-->
<!--                    <span>-->
<!--                      {{ project.failedCount }}{{ $t('page.home.project.executions.0.failed.parenthetical') }}-->
<!--                    </span>-->
<!--                  </a>-->
<!--                </span>-->
<!--            </div>-->
<!--            <div class="col-sm-12 col-md-2 col-last">-->
<!--              <div class="pull-right">-->
<!--                <div class="dropdown-toggle-hover">-->
<!--                  <a href="#" class="as-block link-hover link-quiet link-block-padded text-inverse dropdown-toggle" data-toggle="dropdown">-->
<!--                    {{ $t('button.Action') }}-->
<!--                    <span class="caret"></span>-->
<!--                  </a>-->
<!--                  <ul class="dropdown-menu pull-right" role="menu">-->
<!--                    <li v-if="!project.loaded">-->
<!--                      <a href="#" class="text-muted">-->
<!--                        <b class="fas fa-spinner fa-spin loading-spinner text-muted"></b> Loading &hellip;-->
<!--                      </a>-->
<!--                    </li>-->
<!--                    <template v-else>-->
<!--                      <li v-if="project.auth.admin">-->
<!--                        <a :href="`project/${project.name}/configure`">-->
<!--                          {{ $t('edit.configuration') }}-->
<!--                        </a>-->
<!--                      </li>-->

<!--                      <li class="divider" v-if="project.auth.admin"></li>-->

<!--                      <template v-if="project.auth.jobCreate">-->
<!--                        <li>-->
<!--                          <a :href="`project/${project.name}/job/create`">-->
<!--                            <i class="glyphicon glyphicon-plus"></i>-->
<!--                            {{ $t('new.job.button.label') }}-->
<!--                          </a>-->
<!--                        </li>-->
<!--                        <li class="divider"></li>-->
<!--                        <li>-->
<!--                          <a :href="`project/${project.name}/job/upload`">-->
<!--                            <i class="glyphicon glyphicon-upload"></i>-->
<!--                            {{ $t('upload.definition.button.label') }}-->
<!--                          </a>-->
<!--                        </li>-->
<!--                      </template>-->
<!--                    </template>-->
<!--                  </ul>-->
<!--                </div>-->
<!--              </div>-->
<!--            </div>-->
<!--          </div>-->
<!--        </div>-->

<!--        <div>-->
<!--          <div class="row" v-if="project.showMessage">-->
<!--            <div class="project_list_readme col-sm-10 col-sm-offset-1 col-xs-12">-->
<!--              <div v-if="project.showMotd">-->
<!--                <span v-if="project.readme.motdHTML" v-html="project.readme.motdHTML"></span>-->
<!--              </div>-->
<!--              <div v-if="project.showReadme">-->
<!--                <div>-->
<!--                  <span v-if="project.readme.readmeHTML" v-html="project.readme.readmeHTML"></span>-->
<!--                </div>-->
<!--              </div>-->
<!--            </div>-->
<!--          </div>-->
<!--          <template v-if="project.extra">-->
<!--            <div v-for="extra in project.extra" v-html="extra"></div>-->
<!--          </template>-->
<!--        </div>-->
<!--      </div>-->
    </div>
  </div>
</template>

<script lang="ts">
import {defineComponent} from 'vue'
import HomeSearchBar from "./HomeSearchBar.vue";
import HomeBrowser from "./HomeBrowser.vue";

export default defineComponent({
  name: "HomeCardList",
  components: {
    HomeSearchBar,
    HomeBrowser
  },
  props: {
    loadedProjectNames: {
      type: Boolean,
      default: false
    },
    projects: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      search: "",
      filteredProjects: [],
      showSearchResults: false
    }
  },
  computed: {
    searchedProjectsCount() {
      return this.filteredProjects?.length || 0
    },
  },
  methods: {
    handleSearch() {
      let regex;
      if (this.search.charAt(0) === '/' && this.search.charAt(this.search.length - 1) === '/') {
        regex = new RegExp(this.search.substring(1, this.search.length - 1),'i');
      }else{
        //simple match which is case-insensitive, escaping special regex chars
        regex = new RegExp(this.search.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'),'i');
      }
      this.filteredProjects = this.projects.filter((project) => {
        return project.name.match(regex) || project.label && project.label.match(regex);
      });

      this.showSearchResults = true;
    },
    hideResults() {
      this.showSearchResults = false;
    }
  },
  watch: {
    projects(newVal) {
      if(this.search === "") {
        this.filteredProjects = newVal
      }
    }
  }
})
</script>

<style scoped lang="scss">
.project_list_header {
  .text-lg {
    margin: 0;
  }
}

.project_list_item_link {
  p {
    margin: 0;
    + text-secondary {
      margin-top: var(--spacing-1);
    }
  }
}

.project_list_item_link:not(:has(> :nth-child(4))) > p {
  display: inline;
}

.has_tooltip {
  margin-right: var(--spacing-2);
}
</style>