<template>
  <div id="tour-display" class="card" :class="{'tour-display--pad': pad}" v-if="tour">
    <div class="card-header">
      <div class="tour-card-indicator" :style="{'background-color':tour.titleBackgroundColor}">
        <span v-if="tour.title">{{tour.title}}</span>
        <span v-else>TOUR</span>
      </div>
      <span v-if="tour && tour.name" class="h4 card-title">{{tour.name}}</span>
      <span @click="stopTour" class="btn btn-simple card-close-button">
        <i class="fas fa-times-circle fa-2x"></i>
      </span>
    </div>

    <div class="card-content">
      <div>
        <div class="btn-group" style="margin-bottom:1em;">
          <span
            class="btn btn-default btn-sm"
            :disabled="stepIndex <= 0"
            @click="previousStep"
            style="width: auto !important;"
          >Previous</span>
          <span class="btn btn-default btn-sm" @click="resume"
                style="width: auto !important;">Resume</span>
          <span
            class="btn btn-default btn-sm"
            :disabled="stepIndex === tour.steps.length - 1"
            @click="nextStep"
            style="width: auto !important;"
          >Next</span>
        </div>
        <div class="step-content" v-html="tour.steps[stepIndex].content"></div>
        <progress-bar v-model="progress" label :labelText="progressText" />
      </div>
      <section>
        <modal v-model="modal.show" size="lg" :title="tour.steps[stepIndex].title" :footer="false" append-to-body>
          <img :src="modal.image" :alt="modal.alt" class="img-responsive" />
          <p class="modal-image-caption">{{modal.alt}}</p>
        </modal>
      </section>
    </div>
  </div>
</template>

<script lang='ts'>
  import Vue from 'vue';
  import _ from "lodash";
  import Trellis, { getRundeckContext } from "@/library/centralService";
  import TourServices from "@/app/components/tour/services";
  import {Tour, Step} from '../interfaces/Tours';

  const context = getRundeckContext();

  export default Vue.extend({
    name: "TourDisplay",
    props: ["eventBus", "pad"],
    data() {
      return {
        modal: {
          show: false,
          image: null,
          alt: null
        },
        tour: null as any,
        display: false,
        stepIndex: 0,
        progress: 0,
        progressText: "",
        initUrl: "",
        currentUrl: ""
      };
    },
    methods: {
      showIndicator() {
        const indicator = this.tour.steps[this.stepIndex].stepIndicator;
        if (indicator != null) {

          let indicatorTarget = document.getElementById(indicator);
          if( indicatorTarget == null ) {
             return;
          }
          let indicatorTargetViewportOffset = indicatorTarget.getBoundingClientRect();

          if (indicatorTargetViewportOffset) {
            let indicatorElement = document.createElement("span");
            indicatorElement.setAttribute("id", "tour-vue-indicator");
            indicatorElement.style.top = `${indicatorTargetViewportOffset.y + (indicatorTargetViewportOffset.height / 2 - 25)}px `;
            indicatorElement.style.left = `${indicatorTargetViewportOffset.x + indicatorTargetViewportOffset.width}px`;


            if (this.tour.steps[this.stepIndex].stepIndicatorPosition) {
              const stepIndicatorPosition = this.tour.steps[this.stepIndex].stepIndicatorPosition;

              if (stepIndicatorPosition === "bottom") {
                indicatorElement.classList.add("bottom");
                indicatorElement.style.top = `${indicatorTargetViewportOffset.y +
                (indicatorTargetViewportOffset.height + 10)}px `;
                indicatorElement.style.left = `${indicatorTargetViewportOffset.x +
                (indicatorTargetViewportOffset.width / 2 - 25)}px`;
              }
              if (stepIndicatorPosition === "top") {
                indicatorElement.classList.add("top");
                indicatorElement.style.top = `${indicatorTargetViewportOffset.y -
                60}px `;
                indicatorElement.style.left = `${indicatorTargetViewportOffset.x +
                (indicatorTargetViewportOffset.width / 2 - 25)}px`;
              }
              if (stepIndicatorPosition === "right") {
                indicatorElement.classList.add("right");
                indicatorElement.style.top = `${indicatorTargetViewportOffset.y + (indicatorTargetViewportOffset.height / 2 - 25)}px `;
                if(indicatorTargetViewportOffset.x>60){
                  indicatorElement.style.left = `${indicatorTargetViewportOffset.x - 60}px`;
                }else{
                  indicatorElement.style.left = `0px`;
                }
              }

            }
            setTimeout(() => {
              document.body.appendChild(indicatorElement);
            });
          }
        }
      },
      removeIndicator() {
        let element = document.getElementById("tour-vue-indicator");
        if (element) {
          element?.parentNode?.removeChild(element);
        }
      },
      initTour(tour: Tour, tourStep: any) {
        this.tour = tour;
        this.display = true;
        this.stepIndex = 0;
        if (tourStep) {
          this.stepIndex = tourStep;
        }
        this.setProgress();
        this.removeIndicator();
        document.body.classList.add("tour-open");
      },
      stopTour() {
        TourServices.unsetTour().then(() => {
          this.tour = null as any;
          this.display = false;
          document.body.classList.remove("tour-open");
          this.removeIndicator();
        });
      },
      previousStep() {
        if ( this.stepIndex > 0 ){
          this.stepIndex--;
          Trellis.FilterPrefs.setFilterPref(
            "activeTourStep",
            this.stepIndex
          ).then(() => {
            this.setProgress();
            this.removeIndicator();
          });
        }
      },
      nextStep() {
        let step = this.tour.steps[this.stepIndex];
        if (this.stepIndex !== this.tour.steps.length - 1) {
          this.stepIndex++;
          Trellis.FilterPrefs.setFilterPref(
            "activeTourStep",
            this.stepIndex
          ).then(() => {
            this.setProgress();
            this.removeIndicator();
            this.getNextUrl(this.stepIndex);
          });
        }
      },
      initNextUrl() {
        const step = this.tour.steps[0];
        if(step.currentUrl != null && this.currentUrl !== step.currentUrl){
          window.location.replace(
            `${window._rundeck.rdBase}${step.currentUrl}`
          );
        }
      },
      getNextUrl(stepIndex: any) {
        const nextStep = this.tour.steps[stepIndex];
        let nextUrl = null;

        if (nextStep != null) {
          if(nextStep.currentUrl != null){
            nextUrl = nextStep.currentUrl;
          }else{
            if(nextStep.nextStepUrl != null){
              nextUrl =  nextStep.nextStepUrl;
            }
          }

          if (nextUrl != null && this.currentUrl != nextUrl) {
            window.location.replace(
              `${window._rundeck.rdBase}${nextUrl}`
            );
          }
        }
      },
      setProgress() {
        // Updating the Progress Bar and Label
        let progressStep = 100 / this.tour.steps.length;
        this.progress = Math.ceil(progressStep * (this.stepIndex + 1));
        if (this.stepIndex === 0) {
          this.progressText = " ";
        } else {
          this.progressText = `${this.stepIndex + 1} of ${
            this.tour.steps.length
          }`;
        }
        // Handling the Step Indicator
        if (this.tour.steps[this.stepIndex].stepIndicator) {
          this.showIndicator();
        } else {
          this.removeIndicator();
        }
        setTimeout(() => {
          let images = document.getElementById("tour-display")?.querySelectorAll("img");
          _.map(images, element => {
            element.addEventListener("click", this.openImageModal);
          });
        });
      },
      openImageModal(event: any) {
        this.modal.image = event.target.src;
        this.modal.alt = event.target.alt;
        this.modal.show = true;
      },
      resume(){
        const step = this.tour.steps[this.stepIndex];
        if (step.currentUrl != null && step.currentUrl !== '') {
          if(this.currentUrl !== step.currentUrl){
            window.location.replace(
              `${window._rundeck.rdBase}${step.currentUrl}`
            );
          }
        }
      }
    },
    mounted() {
      const regEx = new RegExp(context.rdBase, "ig");
      this.currentUrl = window.location.href.replace(regEx, "");

      this.eventBus.$on("tourSelected", (tour: Tour) => {
        this.initTour(tour, 0);
        this.initNextUrl();
      });
      if (window._rundeck.activeTour && window._rundeck.activeTour !== "") {
        let tourParts = window._rundeck.activeTour.split(":");
        if(tourParts[2] != null){
          this.initUrl = tourParts[2];
        }
        TourServices.getTour(tourParts[0], tourParts[1]).then((tour: any) => {
          let tourStep = 0;
          if (window._rundeck.activeTourStep) {
            if (Number.isInteger(parseInt(window._rundeck.activeTourStep))) {
              tourStep = parseInt(window._rundeck.activeTourStep);
            }
          }
          this.initTour(tour, tourStep);
        });
      }
    },
  });
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
    .card-header {
      padding-top: 10px;
    }
  }

  .tour-display--pad {
    margin: 2em;
  }

  #tour-vue-indicator {
    position: absolute;
    z-index: 1000;
    width: 0;
    height: 0;
    border-style: solid;
    border-width: 25px 43.3px 25px 0;
    border-color: transparent #f0a810 transparent transparent;
    animation: shake 0.82s cubic-bezier(0.36, 0.07, 0.19, 0.97) both;
    transform: translate3d(0, 0, 0);
    backface-visibility: hidden;
    filter: drop-shadow(5px 5px 4px rgba(0, 0, 0, 0.5));
    &.top {
      border-width: 43.3px 25px 0 25px;
      border-color: #f0a810 transparent transparent transparent;
      filter: drop-shadow(0 5px 4px rgba(0, 0, 0, 0.5));
    }
    &.right {
      border-width: 25px 0 25px 43.3px;
      border-color: transparent transparent transparent #f0a810;
    }
    &.bottom {
      border-width: 0 25px 43.3px 25px;
      border-color: transparent transparent #f0a810 transparent;
      filter: drop-shadow(0px 5px 4px rgba(0, 0, 0, 0.5));
    }
  }
  @keyframes shake {
    10%,
    90% {
      transform: translate3d(-1px, 0, 0);
    }
    20%,
    80% {
      transform: translate3d(2px, 0, 0);
    }
    30%,
    50%,
    70% {
      transform: translate3d(-4px, 0, 0);
    }
    40%,
    60% {
      transform: translate3d(4px, 0, 0);
    }
  }
</style>
<style scoped lang="scss">
  .tour-card-indicator {
    // this is to simply show that the card in the right sidebar is the tour
    width: 100%;
    background-color: rgb(178, 178, 178);
    height: 14px;
    font-size: 8pt;
    text-align: center;
    color: white;
    margin-bottom: 1em;
    letter-spacing: 0.4em;
    border-radius: 3px;
  }
  .step-content {
    margin-bottom: 1em;
    word-break: break-word;
  }
  .step-title {
    margin-top: 0;
    margin-bottom: 1em;
    font-weight: bold;
  }
  .card-close-button {
    position: absolute;
    top: -1.5em;
    right: -2.2em;
  }
  .card-content .btn-group,
  .card-footer .btn-group {
    width: 100%;
    .btn {
      width: 50%;
    }
  }
  .progress div {
    padding-top: 3px;
  }
  .modal-image-caption {
    margin-top: 1em;
    font-size: 1.4em;
  }
</style>
