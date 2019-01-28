<template>
  <div class="row">
    <div class="col-xs-12">
      <div class="card">
        <div class="card-content">
            <span class="text-primary">{{project.description}}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Description',
  props: [
    'project'
  ],
  data () {
    return {}
  }
}
</script>
