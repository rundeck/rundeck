<template>
  <div id="tour-display" class="card" v-if="tour">
    <div class="card-header">
      <div class="row">
        <span v-if="tour && tour.name" class="h3 card-title col-xs-12 col-sm-9">{{tour.name}}</span>
        <span @click="stopTour" class="col-xs-12 col-sm-3 btn btn-simple" style="margin-top:10px;">
          <i class="fas fa-times-circle"></i>
        </span>
      </div>
    </div>
    <div class="card-content">
      <div>
        <h4 class="step-title" style="margin-top:0;">{{tour.steps[stepIndex].title}}</h4>
        <div class="step-content" v-html="tour.steps[stepIndex].content">
        </div>
      </div>
    </div>
    <div class="card-footer">
      <div class="btn-group">
        <span class="btn btn-default" :disabled="stepIndex <= 0" @click="previousStep">Previous</span>
        <span class="btn btn-default" :disabled="stepIndex === tour.steps.length - 1" @click="nextStep">Next</span>
      </div>
    </div>
  </div>
</template>

<script>
import xhrRequestsHelper from '@/utilities/xhrRequests'
import TourServices from '@/components/tour/services'

export default {
  name: 'TourDisplay',
  props: ['eventBus'],
  data () {
    return {
      tour: null,
      display: false,
      stepIndex: 0,
      currentStep: null
    }
  },
  methods: {
    initTour (tour, tourStep) {
      this.tour = tour
      this.display = true
      this.stepIndex = 0
      if (tourStep) {
        this.stepIndex = tourStep
      }
      document.body.classList.add('tour-open')
    },
    stopTour () {
      TourServices.unsetTour().then(() => {
        this.tour = null
        this.display = false
        document.body.classList.remove('tour-open')
      })
    },
    previousStep () {
      if (!this.stepIndex <= 0) {
        this.stepIndex--
        xhrRequestsHelper.setFilterPref('activeTourStep', this.stepIndex)
      }
    },
    nextStep () {
      console.log('next step')
      if (this.stepIndex !== this.tour.steps.length - 1) {
        this.stepIndex++
        xhrRequestsHelper.setFilterPref('activeTourStep', this.stepIndex)
      }
    }
  },
  mounted () {
    this.eventBus.$on('tourSelected', (tour) => {
      this.initTour(tour)
    })

    if (window._rundeck.activeTour && window._rundeck.activeTour !== '') {
      TourServices.getTour(window._rundeck.activeTour).then((tour) => {
        let tourStep = 0
        if (window._rundeck.activeTourStep) {
          if (Number.isInteger(parseInt(window._rundeck.activeTourStep))) {
            tourStep = parseInt(window._rundeck.activeTourStep)
          }
        }
        this.initTour(tour, tourStep)
      })
    }
  }
}
</script>

<style lang="scss">
body.tour-open {
  #layoutBody {
    width: calc(100% - 265px);
    display: inline-block;
    margin-right: 10px;
  }
  #tour-display {
    display: inline-block;
    width: 240px;
    vertical-align: top;
  }
}

#tour-display {
  display: none;
}
</style>
<style scoped lang="scss">
.card-footer .btn-group {
  width: 100%;
  .btn {
    width: 50%;
  }
}
</style>
