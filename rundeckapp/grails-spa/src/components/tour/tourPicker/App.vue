<template>
  <li>
    <a class="btn btn-simple" @click="openTourSelectorModal">
     <img src="../duck.png" alt="" height="32px" style="margin-top:12px; margin-right:15px; opacity:.6;">
    </a>
    <section>
      <modal v-model="tourSelectionModal" title="Available Tours" ref="modal">
        <div v-for="tourLoader in tours" v-bind:key="tourLoader.$index">
          <div class="loader-header header">{{tourLoader.loader}}</div>
          <div class="list-group indent">
            <a class="list-group-item" href="#" v-for="tour in tourLoader.tours" v-bind:key="tour.$index" @click="startTour(tour.provider ? tour.provider : tourLoader.provider,tour)">
              {{tour.name}}
              <span v-if="tour.author">by {{tour.author}}</span>
            </a>
          </div>
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
    startTour: function (tourLoader, tourEntry) {
      TourServices.getTour(tourLoader, tourEntry.key).then((tour) => {
        Trellis.FilterPrefs.setFilterPref('activeTour', tourLoader + ':' + tourEntry.key).then(() => {
          this.eventBus.$emit('tourSelected', tour)
          this.tourSelectionModal = false
        })
      })
    },
    openTourSelectorModal: function () {
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
.loader-header {
  font-weight: bold;
  text-decoration: underline;
  margin-bottom: 3px;
}
.indent { padding-left: 3px; }
</style>
