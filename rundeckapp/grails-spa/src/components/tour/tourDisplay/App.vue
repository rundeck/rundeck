<template>
  <div id="tour-display">
    <button class="btn btn-default" @click="stopTour">Stop Tour</button>
    <pre>{{tour}}</pre>
  </div>
</template>

<script>
// import _ from 'lodash'
// import axios from 'axios'
export default {
  name: 'TourDisplay',
  props:['eventBus'],
  components: {
  },
  data () {
    return {
      tour: null,
      display: false
    }
  },
  methods: {
    stopTour () {
      this.tour = null
      this.display = false
    }
  },
  mounted () {
    console.log('Mounted: Tour Display')
    console.log('eventbus', this.eventBus)
    // Listen for the i-got-clicked event and its payload.
    this.eventBus.$on('tourSelected', (tour) => {
      console.log(`Oh, that's nice. It's gotten clicks! :)`, tour)
      this.tour = tour
      this.display = true
      document.body.classList.add('tour-open')
    });
  }
}
</script>

<style lang="scss">
body.tour-open #layoutBody {
  max-width: calc(100% - 250px);
  display: inline-block;
}
</style>
