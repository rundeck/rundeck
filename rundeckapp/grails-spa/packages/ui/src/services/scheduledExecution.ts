import Trellis from '@rundeck/ui-trellis'
import axios from 'axios'
import _ from 'lodash'
import {seRoot} from '@/components/job/workflow/constants'

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

export default {
  getSchedEx
}
