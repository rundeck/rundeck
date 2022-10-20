// import _ from 'lodash'
import axios from 'axios'
import Tokens from './tokens'
import Generators from './generators'

import {RundeckToken} from '../interfaces/rundeckWindow'

export const setFilterPref = (key: any, value: any) => {
  return new Promise((resolve) => {
    Tokens.getUIAjaxTokens().then((uiToken) => {
      Generators.generateUrl(window.appLinks.userAddFilterPref, {
        filterpref: `${key}=${value}`
      }).then((url) => {
        axios.post(url, {}, {
          // config
          headers: {
            'X-RUNDECK-TOKEN-KEY': uiToken.TOKEN,
            'X-RUNDECK-TOKEN-URI': uiToken.URI
          }
        }).then((response) => {
          Tokens.setNewUIToken(response.headers).then(() => {
            resolve(response)
          })
        })
      })
    })
  })
}

export const unsetFilterPref = (key: string) => {
  return new Promise((resolve) => {
    Tokens.getUIAjaxTokens().then((uiToken) => {
      Generators.generateUrl(window.appLinks.userAddFilterPref, {
        filterpref: `${key}=!`
      }).then((url) => {
        axios.post(url, {}, {
          // config
          headers: {
            'X-RUNDECK-TOKEN-KEY': uiToken.TOKEN,
            'X-RUNDECK-TOKEN-URI': uiToken.URI
          }
        }).then((response) => {
          Tokens.setNewUIToken(response.headers).then(() => {
            // console.log(`unset ${key}`)
            resolve(true)
          })
        })
      })
    })
  })
}

export const getAvailableFilterPrefs = () => {
  return new Promise((resolve) => {
    Tokens.getUIAjaxTokens().then((uiToken) => {
      Generators.generateUrl(window.appLinks.userAddFilterPref, {
        filterpref: `dummy=!`
      }).then((url) => {
        axios.post(url, {}, {
          // config
          headers: {
            'X-RUNDECK-TOKEN-KEY': uiToken.TOKEN,
            'X-RUNDECK-TOKEN-URI': uiToken.URI
          }
        }).then((response) => {
          // console.log('getAvailableFilterPrefs', response)
          Tokens.setNewUIToken(response.headers).then(() => {
            // console.log(`unset ${key}`)
            resolve(response)
          })
        })
      })
    })
  })
}

export default {
  setFilterPref,
  getAvailableFilterPrefs,
  unsetFilterPref
}
