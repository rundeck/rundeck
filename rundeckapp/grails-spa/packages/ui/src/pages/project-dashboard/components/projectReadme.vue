<template>
  <div class="row row-space" v-if="project.readme.readmeHTML">
    <div class="col-sm-12">
      <div class="card">
        <div class="card-header">
          <h4 class="card-title">
            Readme
          </h4>
        </div>
        <div class="card-content">
          <span v-if="project" v-html="project.readme.readmeHTML"></span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Readme',
  props: [
    'project'
  ],
  data () {
    return {}
  }
}
</script>
