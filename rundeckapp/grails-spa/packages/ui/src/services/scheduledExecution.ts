import Trellis from '@rundeck/ui-trellis'
import axios from 'axios'
import _ from 'lodash'
import {seRoot} from '@/components/job/workflow/constants'



export const createSchedEx = () => {
  let tours = [] as any[]
  return new Promise<any[]>((resolve, reject) => {
    axios.get(TourConstants.tourManifestUrl, getHeaderObject()).then((response) => {
      if (response && response.data && response.data.length) {
        _.each(response.data, (tourLoader) => {
          tours.push(tourLoader)
        })
        resolve(tours)
      }
    }).catch(function (error) {
      reject(new Error(error))
      console.log('Tour manifest not found')
    })
  })
}

export const getSchedEx = (projectSchedExId: string) => {
  return new Promise((resolve, reject) => {
    axios.get(`${seRoot}/${projectSchedExId}/edit`)
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

export const updateSchedEx = () => {
  return new Promise<void>((resolve) => {
    Trellis.FilterPrefs.unsetFilterPref('activeTour').then(() => {
      Trellis.FilterPrefs.unsetFilterPref('activeTourStep').then(() => {
        resolve()
      })
    })
  })
}

export const deleteSchedEx = () => {
  return new Promise<void>((resolve) => {
    Trellis.FilterPrefs.unsetFilterPref('activeTour').then(() => {
      Trellis.FilterPrefs.unsetFilterPref('activeTourStep').then(() => {
        resolve()
      })
    })
  })
}

export default {
  createSchedEx,
  updateSchedEx,
  getSchedEx,
  deleteSchedEx
}
