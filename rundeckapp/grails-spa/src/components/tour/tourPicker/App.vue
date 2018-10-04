<template>
  <li>
    <a @click="openTourSelectorModal">
      DUCK
     <!-- <img src="../duck.png" alt=""> -->
    </a>
    <section>
      <modal v-model="tourSelectionModal" title="Available Tours" ref="modal" id="modal-demo">
        <ul>
          <li v-for="tour in tours" v-bind:key="tour.$index" @click="startTour(tour)">
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
  name: 'TourPicker',
  components: {
  },
  props:['eventBus'],
  data () {
    return {
      tourSelectionModal: false,
      tours: []
    }
  },
  methods: {
    startTour: function (tour) {
      // alert('starting tour')
      console.log('tour', tour)
      this.eventBus.$emit('tourSelected', tour)
      this.tourSelectionModal = false

    },
    openTourSelectorModal: function () {
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
    console.log('Mounted: Tour Picker')
    console.log('this.eventBus', this.eventBus)
    // console.log('this', this)
    // let uiTokenElement = document.getElementById('ui_token')

    // if (uiTokenElement) {

    //   let jsonText = dataElement.textContent || dataElement.innerText;

    //   return jsonText && jsonText!=''?JSON.parse(jsonText):null;


    // }
    // var data = {};
    // if(elem && elem.data('rundeck-token-key') && elem.data('rundeck-token-uri')){
    //     data={TOKEN: elem.data('rundeck-token-key'), URI: elem.data('rundeck-token-uri')};
    // }else{
    //     data=loadJsonData(id);
    //     clearHtml(document.getElementById(id));
    // }
    // if(data && data.TOKEN && data.URI){
    //     jqxhr.setRequestHeader('X-RUNDECK-TOKEN-KEY',data.TOKEN);
    //     jqxhr.setRequestHeader('X-RUNDECK-TOKEN-URI',data.URI);
    // }

    // axios.interceptors.request.use((config) => {
    //   fileObject.xhr = config;
    //   return config;
    // })

    // axios.get(window.appLinks.userAddFilterPref, {
    //   params: {
    //     filterpre: 'something'
    //   }
    // })
    // jQuery.ajax({
    //         url: _genUrl(appLinks.userAddFilterPref, {filterpref: key + "=" + sidebarClosed}),
    //         method: 'POST',
    //         beforeSend: _ajaxSendTokens.curry('ui_token'),
    //         success: function () {
    //             console.log("saved sidebar position" );
    //         },
    //         error: function () {
    //             console.log("saving sidebar position failed" );
    //         }
    //     })
    //     .success(_ajaxReceiveTokens.curry('ui_token'));
    // })
  }
}
</script>

<style>
</style>
