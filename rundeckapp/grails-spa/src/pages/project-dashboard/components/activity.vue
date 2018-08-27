<template>
  <div class="row">
    <div class="col-xs-12">
      <div class="card">
        <div class="card-content">
          <a class="h4" :href="`${rdBase}project/${project.name}/activity`">
            <span class="summary-count" :class="{ 'text-primary': project.execCount < 1, 'text-info': project.execCount > 0 }">
              {{project.execCount}}
            </span>
            <span>{{project.execCount | pluralize('Execution')}} In the last day</span>
          </a>
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
    'rdBase'
  ],
  data () {
    return {}
  }
}
</script>

<style scoped>
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
