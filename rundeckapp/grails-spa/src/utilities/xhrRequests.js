import _ from 'lodash'
import axios from 'axios'

export const thing = function () {
  return new Promise((resolve, reject) => {

    resolve(response)
  })
}

export const getAjaxTokens(elementId) {

    return new Promise((resolve, reject) => {

      resolve(response)
    })

    let uiTokenElement = document.getElementById('ui_token')

    if (uiTokenElement) {
      let jsonText = uiTokenElement.textContent || uiTokenElement.innerText
      return jsonText && jsonText !== '' ? JSON.parse(jsonText) : null
    }

    export const ajaxSendTokens(elementId) {
        let uiToken = false
        let uiTokenElement = document.getElementById('ui_token')
        if (uiTokenElement) {
          let jsonText = uiTokenElement.textContent || uiTokenElement.innerText

          uiToken = jsonText && jsonText !== '' ? JSON.parse(jsonText) : null
        }





        export default {
          thing
        }
