<template>
  <div class="row">
    <div class="col-xs-12">
      <div class="card">
        <div class="card-content">
          <a class="h4" :href="`${rdBase}project/${project.name}/activity`">
            <span class="summary-count" :class="{ 'text-strong': count < 1, 'text-info': count > 0 }">
              {{count}}
            </span>
            {{$tc('execution',count)}}
          </a>

          {{$t('In the last Day')}}

          <span :if="project.failedCount > 0">
            <a :class="{'text-warning':project.failedCount>0,'text-muted':project.failedCount<1}" :href="`${rdBase}project/${project.name}/activity?statFilter=fail`">
              ({{project.failedCount}} Failed)
            </a>
          </span>
          <div :if="project.userCount > 0">
            by
            <span class="text-info">{{project.userCount}}</span> &nbsp;
            <span>{{project.userCount | pluralize('User')}}</span>: &nbsp;
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
    'rdBase'
  ],
  data () {
    return {
      count:0
    }
  },

  mounted(){
    this.count=this.project.execCount
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
