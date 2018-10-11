import axios from 'axios'
import _ from 'lodash'
import xhrRequestsHelper from '@/utilities/xhrRequests'
import TourConstants from '@/components/tour/constants'

export const getTours = () => {
  let tours = []

  return new Promise((resolve) => {
    axios.get(TourConstants.tourManifestUrl).then((response) => {
      if (response && response.data && response.data.length) {
        _.each(response.data, (tour) => {
          axios.get(`${TourConstants.tourUrl}${tour}.json`)
            .then((tourResponse) => {
              if (tourResponse && tourResponse.data) {
                tours.push(tourResponse.data)
              }
            })
            .catch(function (error) {
              console.log(error)
              // reject(error)
            })
        })
        resolve(tours)
      }
    })
  })
}

export const getTour = (tourKey) => {
  return new Promise((resolve, reject) => {
    axios.get(`${TourConstants.tourUrl}${tourKey}.json`)
      .then((response) => {
        if (response && response.data) {
          resolve(response.data)
        }
      })
      .catch(function (error) {
        console.log(error)
        reject(error)
      })
  })
}

export const unsetTour = () => {
  console.log('unset tour')
  return new Promise((resolve) => {
    xhrRequestsHelper.unsetFilterPref('activeTour').then(() => {
      xhrRequestsHelper.unsetFilterPref('activeTourStep').then(() => {
        resolve()
      })
    })
  })
}

export default {
  getTours,
  getTour,
  unsetTour
}
