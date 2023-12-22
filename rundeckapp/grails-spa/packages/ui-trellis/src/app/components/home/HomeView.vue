<template>
  <div id="layoutBody">
    <div class="container-fluid" v-if="projectCount > 0 || !loadedProjectNames">
      <div class="row" v-if="isFirstRun">
<!--        TODO: add first run message-->
      </div>
      <HomeHeader :createProjectAllowed="createProjectAllowed" :projectCount="projectCount" />
    </div>
    <div class="container-fluid" v-if="projectCount === 0">
<!--      TODO: load first run component here-->
      <div v-if="createProjectAllowed"  id="firstRun" />
    </div>
    <div class="container-fluid">
      <div class="row">
        <div class="col-xs-12">
          <div class="card" v-if="projectCount < 1 && loadedProjectNames && !createProjectAllowed">

            <div class="card-content">
              <div class="well">
                <h2 class="text-warning">
                  {{ $t('no.authorized.access.to.projects') }}
                </h2>
                <p>
                  {{
                    $t('no.authorized.access.to.projects.contact.your.administrator.user.roles.0', [
                        roles.join(', ')
                    ])
                  }}
                </p>
              </div>
            </div>
          </div>
          <HomeCardList :loaded-project-names="loadedProjectNames" :projects="projects" />

        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import {defineComponent} from 'vue'
import HomeHeader from "./HomeHeader.vue";
import HomeCardList from "./HomeCardList.vue";
import {getProjects} from "@/app/components/home/services/homeServices";

export default defineComponent({
  name: "HomeView",
  components: {
    HomeHeader,
    HomeCardList
  },
  props: {
    isFirstRun: {
      type: Boolean,
      default: false
    },
    createProjectAllowed: {
      type: Boolean,
      default: false,
    },
    roles: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      projectCount: 0,
      loadedProjectNames: false,
      projects: null
    }
  },
  methods: {
    async getProjects() {
      try {
        this.projects = await getProjects()
        this.projectCount = this.projects.length
        // this.$root.setProjects(projects)
        this.loadedProjectNames = true
      } catch (e) {
        console.error(e)
      }
    }
  },
  mounted() {
    this.getProjects()
  }
})
</script>

<style scoped lang="scss">

</style>