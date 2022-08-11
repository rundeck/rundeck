// import _ from 'lodash'
import axios from 'axios'

export const getUIAjaxTokens = () => {
  return new Promise((resolve, reject) => {
    if (window._rundeck.token) {
      resolve(window._rundeck.token)
    } else {
      let uiTokenElement = document.getElementById('ui_token')

      if (uiTokenElement) {
        let jsonText = uiTokenElement.textContent || uiTokenElement.innerText
        let response = jsonText && jsonText !== '' ? JSON.parse(jsonText) : null
        window._rundeck.token = response
        resolve(response)
      } else {
        reject(new Error('No token exists'))
      }
    }
  })
}

export const setNewUIToken = (responseHeaders) => {
  return new Promise((resolve, reject) => {
    window._rundeck.token = {
      'TOKEN': responseHeaders['x-rundeck-token-key'],
      'URI': responseHeaders['x-rundeck-token-uri']
    }
    resolve()
  })
}

export const setFilterPref = (key, value) => {
  return new Promise((resolve) => {
    getUIAjaxTokens().then((uiToken) => {
      generateUrl(window.appLinks.userAddFilterPref, {
        filterpref: `${key}=${value}`
      }).then((url) => {
        axios.post(url, {}, {
          // config
          headers: {
            'X-RUNDECK-TOKEN-KEY': uiToken.TOKEN,
            'X-RUNDECK-TOKEN-URI': uiToken.URI
          }
        }).then((response) => {
          setNewUIToken(response.headers).then(() => {
            resolve(response)
          })
        })
      })
    })
  })
}

export const unsetFilterPref = (key) => {
  return new Promise((resolve) => {
    getUIAjaxTokens().then((uiToken) => {
      generateUrl(window.appLinks.userAddFilterPref, {
        filterpref: `${key}=!`
      }).then((url) => {
        axios.post(url, {}, {
          // config
          headers: {
            'X-RUNDECK-TOKEN-KEY': uiToken.TOKEN,
            'X-RUNDECK-TOKEN-URI': uiToken.URI
          }
        }).then((response) => {
          setNewUIToken(response.headers).then(() => {
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
    getUIAjaxTokens().then((uiToken) => {
      generateUrl(window.appLinks.userAddFilterPref, {
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
          setNewUIToken(response.headers).then(() => {
            // console.log(`unset ${key}`)
            resolve(response)
          })
        })
      })
    })
  })
}

export const generateUrl = (url, params) => {
  return new Promise((resolve) => {
    let urlparams = []
    if (typeof (params) === 'string') {
      urlparams = [params]
    } else if (typeof (params) === 'object') {
      for (var e in params) {
        urlparams.push(`${encodeURIComponent(e)}=${encodeURIComponent(params[e])}`)
      }
    }
    resolve(url + (urlparams.length ? ((url.indexOf('?') > 0 ? '&' : '?') + urlparams.join('&')) : ''))
  })
}

export default {
  getUIAjaxTokens,
  setFilterPref,
  getAvailableFilterPrefs,
  unsetFilterPref,
  generateUrl
}
