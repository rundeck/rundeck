<template>
  <div class="row">
    <div class="col-xs-12">
      <div class="card">
        <div class="card-content">
          <a class="h4" :href="`${rdBase}project/${project.name}/activity`">
            <span class="summary-count" :class="{ 'text-primary': count < 1, 'text-info': count > 0 }">
              {{count}}
            </span>
            {{$tc('execution',count)}}
          </a>

            In the last

            <dropdown menu-right>
            <btn class="dropdown-toggle">
              {{period}}
              <span class="caret"></span>
            </btn>
            <template slot="dropdown">

              <li v-for="(val,period) in periods" :key="period"><a role="button" @click="changePeriod(period)">{{period}}</a></li>
            </template>
          </dropdown>

          <span :if="project.failedCount > 0">
            <a class="text-warning" :href="`${rdBase}project/${project.name}/activity?statFilter=fail`">
              ({{project.failedCount}} Failed)
            </a>
          </span>
          <div :if="project.userCount > 0">
            by
            <span class="text-info">{{project.userCount}}</span>
            <span>{{project.userCount | pluralize('User')}}</span>
            <ul class="users">
              <li v-for="user in project.userSummary" :key="user">{{user}}</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>

export default {
  name: 'Activity',
  props: [
    'project',
    'eventBus'
  ],
  data () {
    return {
      count:0,
      period:'Day',
      periods: {
        Hour:'1h',
        Day:'1d',
        Week:'1w',
        Month:'1m'
      }
    }
  },
  methods:{
    changePeriod(val){
      if(this.periods[val]){
        this.period=val
        this.eventBus&&this.eventBus.$emit('change-query-period',this.periods[val])
      }
    }
  },
  mounted(){
    this.count=this.project.execCount
     this.eventBus&&this.eventBus.$on('activity-query-result', (data) => {
      this.count=data.total
    })
  }
}
</script>

<style scoped lang="scss">
  ul.users {
    display: inline;
    margin: 0;
    padding: 0;
  }

  ul.users li {
    display: inline;
    list-style: none;
    margin: 0;
    padding: 0;
  }

  ul.users li:after {
    content: ", ";
    color: #aaa;
  }

  ul.users li:last-child:after {
    content: "";
  }
</style>
