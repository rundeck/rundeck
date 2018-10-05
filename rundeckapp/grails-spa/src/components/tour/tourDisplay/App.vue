<template>
  <div id="tour-display" class="card" v-if="tour">
    <div class="card-header">
      <div class="row">
        <span v-if="tour && tour.name" class="h4 card-title col-xs-12 col-sm-9">{{tour.name}}</span>
        <span @click="stopTour" class="col-xs-12 col-sm-3"><i class="fas fa-times-circle"></i></span>
      </div>

    </div>
    <div class="card-content">
      <div v-if="currentStep">
        <h3 class="step-title">{{currentStep.title}}</h3>
        <div class="step-content" v-html="currentStep.content">
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
// import _ from 'lodash'
// import axios from 'axios'
export default {
  name: 'TourDisplay',
  props: ['eventBus'],
  data () {
    return {
      tour: null,
      display: false,
      stepIndex: null,
      currentStep: null
    }
  },
  methods: {
    makePointer () {
      // let template = `<div>Hello World</div>`
      // document.getElementsByTagName('body')[0].appendChild(template)
    },
    stopTour () {
      this.tour = null
      this.display = false
      document.body.classList.remove('tour-open')
    },
    previousStep () {
      if (!this.stepIndex <= 0) {
        this.stepIndex--
      }
    },
    nextStep () {
      if (!this.stepIndex === this.tour.steps.length - 1) {
        this.stepIndex++
      }
    }
  },
  computed: {},
  watch: {
    stepIndex: function(){
      this.currentStep = this.tour.steps[this.stepIndex]
      if(this.tour.steps[this.stepIndex].nextStepIndicator){
        this.makePointer()
        console.log('nextStepIndicator', this.tour.steps[this.stepIndex].nextStepIndicator)
      }
    }
  },
  mounted () {
    this.eventBus.$on('tourSelected', (tour) => {
      this.tour = tour
      this.display = true
      this.stepIndex = 0
      this.currentStep = this.tour.steps[0]
      document.body.classList.add('tour-open')
    })
  }
}
</script>

<style lang="scss">
body.tour-open {
  #layoutBody {
    width: calc(100% - 250px);
    display: inline-block;
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
