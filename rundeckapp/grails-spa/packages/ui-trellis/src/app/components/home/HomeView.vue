<template>
  <div id="layoutBody">
      <div class="container-fluid" v-if="dataLoaded && projectCount > 0 || !loadedProjectNames">
        <div class="row" v-if="isFirstRun">
          <HomeWelcome
              :appTitle="appTitle"
              :buildIdent="buildIdent"
              :logoImage="logoImage"
              :helpLinkUrl="helpLinkUrl"
          />
        </div>
        <HomeHeader
            :createProjectAllowed="createProjectAllowed"
            :projectCount="projectCount"
            :summaryRefresh="summaryRefresh"
            :refreshDelay="refreshDelay"
        />
      </div>
      <div class="container-fluid" v-if="dataLoaded && projectCount === 0">
        <FirstRun v-if="createProjectAllowed" />
      </div>
      <div class="container-fluid">
        <div class="row">
          <div class="col-xs-12">
            <div
                class="card"
                v-if="
              projectCount < 1 && loadedProjectNames && !createProjectAllowed
            "
            >
              <div class="card-content">
                <div class="well">
                  <h2 class="text-warning">
                    {{ $t("no.authorized.access.to.projects") }}
                  </h2>
                  <p>
                    {{
                      $t(
                          "no.authorized.access.to.projects.contact.your.administrator.user.roles.0",
                          [roles.join(", ")]
                      )
                    }}
                  </p>
                </div>
              </div>
            </div>
            <HomeCardList
                :loaded-project-names="loadedProjectNames"
                :projects="projects"
                :paging-max="pagingMax"
            />
          </div>
        </div>
      </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import HomeHeader from "./HomeHeader.vue";
import HomeCardList from "./HomeCardList.vue";
import {getProjectNames, getProjects} from "@/app/components/home/services/homeServices";
import FirstRun from "@/library/components/first-run/FirstRun.vue";
import HomeWelcome from "@/app/components/home/HomeWelcome.vue";


export default defineComponent({
  name: "HomeView",
  components: {
    HomeWelcome,
    FirstRun,
    HomeHeader,
    HomeCardList,
  },
  props: {
    isFirstRun: {
      type: Boolean,
      default: false,
    },
    createProjectAllowed: {
      type: Boolean,
      default: false,
    },
    roles: {
      type: Array as PropType<string[]>,
      default: () => [],
    },
    appTitle: {
      type: String,
      required: true,
    },
    buildIdent: {
      type: String,
      required: true,
    },
    logoImage: {
      type: String,
      required: true,
    },
    helpLinkUrl: {
      type: String,
      required: true,
    },
    summaryRefresh: {
      type: Boolean,
      default: false,
    },
    refreshDelay: {
      type: Number,
      default: 30000
    },
    pagingMax: {
      type: Number,
      default: 30,
    },
  },
  data() {
    return {
      projectCount: 0,
      loadedProjectNames: false,
      projects: null,
      dataLoaded: false,
      test: null
    };
  },
  methods: {
    async getPartialData() {
      try {
        const arrayOfProjectNames: string[] =  await getProjectNames();
        if(arrayOfProjectNames && arrayOfProjectNames.length > 0){
          this.projects = arrayOfProjectNames.map(projectName => ({name: projectName}))
        }

        this.projectCount = this.projects ? this.projects.length : 0;
      } catch (e) {
        console.error(e);
      }
    },
    async getProjects() {
      try {
        this.projects = await getProjects();
        if (this.projects && this.projectCount !== this.projects.length) {
          this.projectCount = this.projects.length;
        }
        this.loadedProjectNames = true;
      } catch (e) {
        console.error(e);
      } finally {
        this.dataLoaded= true;
      }
    },
  },
  mounted() {
    this.getPartialData();
    this.getProjects();
  },
});
</script>