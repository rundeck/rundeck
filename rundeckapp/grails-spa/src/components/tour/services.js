import axios from 'axios'
import _ from 'lodash'
import Trellis from '@rundeck/ui-trellis'
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
            })
        })
        resolve(tours)
      }
    }).catch(function (error) {
      reject(new Error(error))
      console.log('Tour manifest not found')
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
  return new Promise((resolve) => {
    Trellis.FilterPrefs.unsetFilterPref('activeTour').then(() => {
      Trellis.FilterPrefs.unsetFilterPref('activeTourStep').then(() => {
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
