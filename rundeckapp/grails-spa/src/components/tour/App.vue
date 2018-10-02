<template>
  <li>
    <a @click="openTour">
     DUCK
    </a>
    <section>
      <modal v-model="tourSelectionModal" title="Available Tours" ref="modal" id="modal-demo">
        <ul>
          <li v-for="tour in tours">
            {{tour.name}}
          </li>
        </ul>
      </modal>
    </section>
  </li>
</template>

<script>
import _ from 'lodash'
import axios from 'axios'
export default {
  name: 'App',
  components: {
  },
  data () {
    return {
      tourSelectionModal: false,
      tours: []
    }
  },
  methods: {
    openTour: function () {
      let tourManifestUrl = `${window._rundeck.rdBase}user-assets/tour-manifest.json`

      axios.get(tourManifestUrl)
        .then((response) => {
          if (response && response.data && response.data.length) {
            let tourUrl = `${window._rundeck.rdBase}user-assets/tours/`
            _.each(response.data, (tour) => {
              axios.get(`${tourUrl}${tour}.json`)
                .then((response) => {
                  if (response && response.data) {
                    this.tours.push(response.data)
                  }
                })
                .catch(function (error) {
                  console.log(error)
                })
            })
            this.tourSelectionModal = true
          }
        })
        .catch(function (error) {
          console.log(error)
        })
    }
  },
  mounted () {
    console.log('hello world')
  }
}
</script>

<style>
</style>
