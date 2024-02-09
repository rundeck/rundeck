<template>
  <li id="appTour">
    <!-- <a class="btn btn-xs" @click="openTourSelectorModal">
      Tours
    </a> -->
    <section>
      <modal
        ref="modal"
        v-model="tourSelectionModal"
        title="Available Tours"
        append-to-body
      >
        <div v-for="(tourLoader, tIndex) in tours" :key="tIndex">
          <div class="panel panel-default" style="padding-bottom: 1px">
            <div class="panel-heading">
              <strong>{{ tourLoader.loader }}</strong>
            </div>
            <div class="list-group">
              <a
                v-for="(tour, index) in tourLoader.tours"
                :key="index"
                class="list-group-item"
                href="#"
                @click="
                  startTour(
                    tour.provider ? tour.provider : tourLoader.provider,
                    tour,
                  )
                "
              >
                {{ tour.name }}
                <span v-if="tour.author">by {{ tour.author }}</span>
              </a>
            </div>
          </div>
        </div>
        <template #footer>
          <div>
            <btn @click="tourSelectionModal = false">Close</btn>
          </div>
        </template>
      </modal>
    </section>
  </li>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import Trellis, { getRundeckContext } from "../../../../library";
import TourServices from "../services";
import { RootStore } from "../../../../library/stores/RootStore";

const context = getRundeckContext();

interface Tour {
  name: string;
  author: string;
  provider: string;
}
interface TourLoader {
  tours: Tour[];
  provider: string;
  loader: string;
}

export default defineComponent({
  name: "TourPicker",
  inject: ["rootStore"],
  props: ["eventBus"],
  data() {
    return {
      hasActiveTour: false,
      tourSelectionModal: false,
      tours: [] as TourLoader[],
    };
  },
  mounted() {
    // @ts-ignore
    this.rootStore.utilityBar.addItems([
      {
        type: "action",
        id: "utility-tours",
        container: "root",
        group: "right",
        class: "fas fa-lightbulb",
        label: "Tours",
        visible: true,
        action: this.openTourSelectorModal,
      },
    ]);
  },
  beforeMount() {
    if (window._rundeck) {
      window._rundeck.eventBus.on("refresh-tours", () => {
        this.loadTours();
      });
    }
  },
  beforeUnmount() {
    window._rundeck.eventBus.off("refresh-tours");
  },
  methods: {
    startTour: function (tourLoader: string, tourEntry: any) {
      TourServices.getTour(tourLoader, tourEntry.key).then((tour: any) => {
        Trellis.FilterPrefs.setFilterPref(
          "activeTour",
          tourLoader + ":" + tourEntry.key,
        ).then(() => {
          if (tour.project) {
            window.location.replace(
              `${context.rdBase}project/${tour.project}/home`,
            );
          } else {
            this.eventBus.emit("tourSelected", tour);

            this.tourSelectionModal = false;
          }
        });
      });
    },
    openTourSelectorModal: function () {
      if (this.tours.length) {
        this.tourSelectionModal = true;
      } else {
        this.loadTours();
        this.tourSelectionModal = true;
      }
    },
    loadTours() {
      TourServices.getTours().then((tours) => {
        this.tours = tours;
      });
    },
  },
});
</script>

<style scoped lang="scss">
// This is a hack because we're limiting the normal padding/margin for list-group-items in the dropdown menus in mainbar.scss for the theme itself
// a.list-group-item {
//   padding: 10px 15px !important;
//   margin-bottom: -1px !important;
// }
</style>
