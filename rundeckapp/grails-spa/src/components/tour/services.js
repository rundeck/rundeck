import axios from 'axios'
import _ from 'lodash'
import xhrRequestsHelper from '@/utilities/xhrRequests'

export const getTours = () => {
  const tourManifestUrl = `${window._rundeck.rdBase}user-assets/tour-manifest.json`
  const tourUrl = `${window._rundeck.rdBase}user-assets/tours/`
  let tours = []

  return new Promise((resolve) => {
    axios.get(tourManifestUrl).then((response) => {
      if (response && response.data && response.data.length) {
        _.each(response.data, (tour) => {
          axios.get(`${tourUrl}${tour}.json`)
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
  const tourUrl = `${window._rundeck.rdBase}user-assets/tours/`

  return new Promise((resolve, reject) => {
    axios.get(`${tourUrl}${tourKey}.json`)
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
