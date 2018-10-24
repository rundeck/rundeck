<template>
  <li>
    <a class="btn btn-simple" @click="openTourSelectorModal">
     <img src="../duck.png" alt="" height="32px" style="margin-top:12px; margin-right:15px; opacity:.6;">
    </a>
    <section>
      <modal v-model="tourSelectionModal" title="Available Tours" ref="modal">
        <div class="list-group">
          <a class="list-group-item" href="#" v-for="tour in tours" v-bind:key="tour.$index" @click="startTour(tour)">
            {{tour.name}}
            <span v-if="tour.author">by {{tour.author}}</span>
          </a>
        </div>
        <div slot="footer">
          <btn @click="tourSelectionModal=false">Close</btn>
        </div>
      </modal>
    </section>
  </li>
</template>

<script>
import Trellis from '@rundeck/ui-trellis'
import TourServices from '@/components/tour/services'

import {RundeckBrowser} from 'ts-rundeck'

const token = JSON.parse(document.getElementById('web_ui_token').textContent)
const client = new RundeckBrowser(token.TOKEN, token.URI, 'http://ubuntu:4440')

export default {
  name: 'TourPicker',
  props: ['eventBus'],
  data () {
    return {
      hasActiveTour: false,
      tourSelectionModal: false,
      tours: []
    }
  },
  methods: {
    startTour: function (tour) {
      Trellis.FilterPrefs.setFilterPref('activeTour', tour.key).then(() => {
        this.eventBus.$emit('tourSelected', tour)
        this.tourSelectionModal = false
      })
    },
    openTourSelectorModal: function () {
      client.projectList().then( r => console.log(r._response))

      client.projectList().then( r => {
        r.forEach(p => {
          console.log(p.name)
        })
      })

      if (this.tours.length) {
        this.tourSelectionModal = true
      } else {
        TourServices.getTours().then((tours) => {
          this.tourSelectionModal = true
          this.tours = tours
        })
      }
    }
  }
}
</script>

<style scoped lang="scss">
// This is a hack because we're limiting the normal padding/margin for list-group-items in the dropdown menus in mainbar.scss for the theme itself
a.list-group-item {
  padding: 10px 15px !important;
  margin-bottom: -1px !important;
}
</style>
