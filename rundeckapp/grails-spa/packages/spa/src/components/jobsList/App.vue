<template>
  <div class="card">
    <ul>
      <li v-for="group in uberJobs" v-bind:key="group.name">
        <h3>{{group.name}}</h3>
        <ul class="list-group">
          <li class="list-group-item" v-for="job in group.jobs" v-bind:key="job.id">
            <div class="row">
              <div class="col-xs-12 col-sm-1">
                PLAY
              </div>
              <div class="col-xs-12 col-sm-4">
                <strong><a :href="job.permalink">{{job.name}}</a></strong>
              </div>
              <div class="col-xs-12 col-sm-4">

              </div>
              <div class="col-xs-12 col-sm-3">
                <dropdown menu-right class=" pull-right">
                  <btn class="dropdown-toggle" @click="jobActionMenu(job.id)"><span class="caret"></span></btn>
                  <ul class="dropdown-menu dropdown-menu-right" slot="dropdown" v-html="actionMenuHtml"></ul>
                </dropdown>
              </div>
            </div>
            <!-- <vue-simple-markdown :source="job.description"></vue-simple-markdown> -->
          </li>
        </ul>
      </li>
    </ul>
    <div class="panel-group">
      <div class="panel panel-default">
        <div class="panel-heading" role="button" @click="toggleAccordion(0)">
          <h4 class="panel-title">Collapsible Group Item #1</h4>
        </div>
        <collapse v-model="showAccordion[0]">
          <div class="panel-body">
            Anim pariatur cliche reprehenderit, enim eiusmod high life accusamus terry richardson ad squid. 3 wolf
            moon officia aute, non cupidatat skateboard dolor brunch. Food truck quinoa nesciunt laborum eiusmod.
          </div>
        </collapse>
      </div>
      <div class="panel panel-default">
        <div class="panel-heading" role="button" @click="toggleAccordion(1)">
          <h4 class="panel-title">Collapsible Group Item #2</h4>
        </div>
        <collapse v-model="showAccordion[1]">
          <div class="panel-body">
            Anim pariatur cliche reprehenderit, enim eiusmod high life accusamus terry richardson ad squid. 3 wolf
            moon officia aute, non cupidatat skateboard dolor brunch. Food truck quinoa nesciunt laborum eiusmod.
          </div>
        </collapse>
      </div>
      <div class="panel panel-info">
        <div class="panel-heading" role="button" @click="toggleAccordion(2)">
          <h4 class="panel-title">Collapsible Group Item #3</h4>
        </div>
        <collapse v-model="showAccordion[2]">
          <div class="panel-body">
            Anim pariatur cliche reprehenderit, enim eiusmod high life accusamus terry richardson ad squid. 3 wolf
            moon officia aute, non cupidatat skateboard dolor brunch. Food truck quinoa nesciunt laborum eiusmod.
          </div>
        </collapse>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios'
import _ from 'lodash'

export default {
  name: 'App',
  components: {
    // VueSimpleMarkdown
  },
  data () {
    return {
      groups: null,
      tabIndex: 0,
      jobs: null,
      actionMenuHtml: null,
      uberJobs: [],
      showAccordion: [true, false, false]
    }
  },
  methods: {
    toggleAccordion (index) {
      if (this.showAccordion[index]) {
        this.$set(this.showAccordion, index, false)
      } else {
        this.showAccordion = this.showAccordion.map((v, i) => i === index)
      }
    },
    jobActionMenu (jobId) {
      console.log('jobId', jobId)
      // "/project/anvilsonline/job/actionMenuFragment"
      axios({
        method: 'get',
        headers: {'x-rundeck-ajax': true},
        url: `${window._rundeck.rdBase}project/${window._rundeck.projectName}/job/actionMenuFragment`,
        params: {
          id: jobId
        },
        withCredentials: true
      }).then((response) => {
        // console.log('Job Action Menu Fragment', response.data)
        this.actionMenuHtml = response.data
        // this.actionMenuHtml = _.sortBy(response.data.group)

        // if (response.data) {
        //   this.jobs = response.data
        //   // this.project = response.data.projects[0]
        // }
      })
    }
  },
  computed: {
    jobGroupFilter (group) {
      return _.filter(this.jobs, (job) => {
        return job.group === group
      })
    }
  },
  created () {
  },
  mounted () {
    if (window._rundeck && window._rundeck.rdBase && window._rundeck.projectName) {
      axios({
        method: 'get',
        headers: {'x-rundeck-ajax': true},
        url: `${window._rundeck.rdBase}menu/jobsAjax`,
        params: {
          project: `${window._rundeck.projectName}`
        },
        withCredentials: true
      }).then((response) => {
        // console.log('Rundeck Project Jobs', response)//
        if (response.data) {
          this.jobs = response.data
          this.groups = _.uniq(_.map(response.data, (job) => {
            // console.log('job', job)
            return job.group
          }))
          _.each(this.groups, (group) => {
            let obj = {
              name: group,
              jobs: _.filter(this.jobs, (job) => {
                return job.group === group
              })
            }
            this.uberJobs.push(obj)
          })
          console.log('uberJobs', this.uberJobs)
          // console.log('group', this.groups)
        }
      })
    }
  }
}
</script>

<style scoped lang="scss">
.display-as-table{
  display: table;
  .table-cell{
    display: table-cell;
  }
}
</style>
