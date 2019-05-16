<template>
  <span>
    <btn
      @click="filterOpen=true"
      size="xs"
      :class="hasQuery?'btn-queried btn-info':'btn-secondary'"
      v-tooltip="hasQuery?$t('Click to edit Search Query'):''"
    >
      <span v-if="hasQuery" class="query-params-summary">
        <ul class="list-inline">
          <li v-for="qname in queryParamsList" :key="qname">
            <i18n :path="'jobquery.title.'+qname"/>:
            <code class="queryval">{{query[qname]}}</code>
          </li>
        </ul>
      </span>
      <span v-else>{{$t('search.ellipsis')}}</span>
    </btn>

    <modal id="activityFilter" v-model="filterOpen" :title="$t('Search Activity')" size="lg" @hide="closing">
      <div>
        <div class="base-filters">
          <div class="row">
            <div class="col-xs-12 col-sm-4">
              <div class="form-group">
                <label for="jobIdFilter" class="sr-only">
                  <i18n path="jobquery.title.jobFilter"/>
                </label>
                <input
                  type="text"
                  name="jobFilter"
                  v-model="query.jobFilter"
                  autofocus="true"
                  class="form-control"
                  :placeholder="$t('jobquery.title.jobFilter')"
                >
              </div>

              <div class="form-group" v-if="query.jobIdFilter">
                <label for="jobIdFilter" class="sr-only">
                  <i18n path="jobquery.title.jobIdFilter"/>
                </label>
                <input
                  type="text"
                  name="jobIdFilter"
                  v-model="query.jobIdFilter"
                  class="form-control"
                  :placeholder="$t( 'jobquery.title.jobIdFilter')"
                >
              </div>
            </div>
            <div class="col-xs-12 col-sm-4">
              <div class="form-group">
                <label for="userFilter" class="sr-only">
                  <i18n path="jobquery.title.userFilter"/>
                </label>
                <input
                  type="text"
                  name="userFilter"
                  v-model="query.userFilter"
                  class="form-control"
                  :placeholder="$t( 'jobquery.title.userFilter')"
                >
              </div>
            </div>
            <div class="col-xs-12 col-sm-4">
              <div class="form-group">
                <label for="execnodeFilter" class="sr-only">
                  <i18n path="jobquery.title.filter"/>
                </label>
                <input
                  type="text"
                  name="execnodeFilter"
                  v-model="query.execnodeFilter"
                  class="form-control"
                  :placeholder="$t( 'jobquery.title.filter')"
                >
              </div>
            </div>
          </div>
          <div class="row">
            <div class="col-xs-12 col-sm-4">
              <div class="form-group">
                <label for="titleFilter" class="sr-only">
                  <i18n path="jobquery.title.titleFilter"/>
                </label>
                <input
                  type="text"
                  name="titleFilter"
                  v-model="query.titleFilter"
                  class="form-control"
                  :placeholder="$t( 'jobquery.title.titleFilter')"
                >
              </div>
            </div>
            <div class="col-xs-12 col-sm-4">
              <div class="form-group">
                <label for="statFilter" class="sr-only">
                  <i18n path="jobquery.title.statFilter"/>
                </label>
                <select
                  name="statFilter"
                  v-model="query.statFilter"
                  noSelection="['': 'Any']"
                  valueMessagePrefix="status.label"
                  class="form-control"
                >
                  <option value>Any</option>
                  <option>succeed</option>
                  <option>fail</option>
                  <option>cancel</option>
                </select>
              </div>
            </div>
            <div class="col-xs-12 col-sm-4">
              <div class="form-group">
                <label for="recentFilter" class="sr-only">
                  <i18n path="jobquery.title.recentFilter"/>
                </label>
                <span class="radiolist">
                  <select name="recentFilter" v-model="query.recentFilter" class="form-control">
                    <option value>Any Time</option>
                    <option :value="val" v-for="(key,val) in recentDateFilters" :key="key">{{key}}</option>
                    <option value="-">Other...</option>
                  </select>
                </span>
              </div>
            </div>
          </div>
        </div>
        <div class="date-filters panel panel-default" v-if="query.recentFilter==='-'">
          <div class="panel-body  form-horizontal">
            <div v-for="df in DateFilters" :key=df.name class="container-fluid">
                <date-filter v-model="df.filter">
                  {{$t('jobquery.title.'+df.name)}}
                </date-filter>
            </div>
          </div>
        </div>
      </div>
      <template slot="footer">
        <btn @click="filterOpen=false">Cancel</btn>
        <btn @click="search">Search</btn>
        <btn @click="saveFilter">Save as a Filter</btn>
      </template>
    </modal>
  </span>
</template>
<script>
import DateTimePicker from './dateTimePicker.vue'
import DateFilter from './dateFilter.vue'

export default {
  components:{
    DateTimePicker,
    DateFilter
  },
  props: ["eventBus", "value"],
  data() {
    return {
      filterOpen: false,
      QueryNames: [
        "jobFilter",
        "jobIdFilter",
        "userFilter",
        "execNodeFilter",
        "titleFilter",
        "statFilter",
        "startafterFilter",
        "startbeforeFilter",
        "endafterFilter",
        "endbeforeFilter"
      ],
      DateFilters:[
        {
          name:'startafterFilter',
          filter:{
            enabled:false,
            datetime:''
          }
        },

        {
          name:'startbeforeFilter',
          filter:{
            enabled:false,
            datetime:''
          }
        },
        {
          name:'endafterFilter',
          filter:{
            enabled:false,
            datetime:''
          }
        },
        {
          name:'endbeforeFilter',
          filter:{
            enabled:false,
            datetime:''
          }
        },
      ],
      query: {
        jobFilter: "",
        jobIdFilter: "",
        userFilter: "",
        execNodeFilter: "",
        titleFilter: "",
        statFilter: "",
        recentFilter: "",
        startafterFilter:"",
        startbeforeFilter:"",
        endafterFilter:"",
        endbeforeFilter:"",
      },
      hasQuery: false,
      showDateFilters: false,
      recentDateFilters: {
        "1d": "1 Day",
        "1w": "1 Week",
        "1m": "1 Month"
      },
      didSearch: false
    }
  },
  methods: {
    search() {
      let isquery = false
      for (let filt in this.QueryNames) {
        if (this.query[filt] !== "") {
          isquery = true
          break
        }
      }
      this.hasQuery = isquery
      this.$emit("input", this.query)
      this.didSearch=true
      this.filterOpen = false
    },
    cancel() {
      this.reset()
      this.filterOpen = false
    },
    reset(){
      this.query = Object.assign({}, this.value)
      this.DateFilters.forEach(element => {
        element.filter.datetime=this.query[element.name]
        element.filter.enabled=!!element.filter.datetime
      });
    },
    closing(){
      if(this.didSearch){
        this.didSearch=false
      }else{
        this.reset()
      }
    },
    saveFilter() {}
  },
  watch: {
    value: {
      handler(newValue, oldValue) {
        this.reset()
      },
      deep: true
    },
    DateFilters:{
      handler(newValue,oldVale){
        newValue.forEach(element => {
          if(element.filter.enabled){
            this.query[element.name]=element.filter.datetime
          }else{
            this.query[element.name]=''
          }
        });
      },
      deep:true
    }
  },
  computed: {
    queryParamsList() {
      return this.QueryNames.filter(s => !!this.query[s])
    }
  },
  mounted() {
    this.reset()
  }
}
</script>
<style lang="scss" scoped>
.query-params-summary {
  ul.list-inline {
    display: inline-block;
    margin: 0;
  }
}
.btn-queried {
  border-style: dotted;
}
</style>
