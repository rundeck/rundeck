import {RundeckToken} from '../interfaces/rundeckWindow'

export async function getUIAjaxTokens (): Promise<RundeckToken> {
  if (window._rundeck.token) {
    return (window._rundeck.token)
  } else {
    let uiTokenElement = document.getElementById('ui_token')

    if (uiTokenElement) {
      let jsonText = uiTokenElement.textContent || uiTokenElement.innerText
      let response = jsonText && jsonText !== '' ? JSON.parse(jsonText) : null
      window._rundeck.token = response
      return response
    } else {
      throw new Error('No token exists')
    }
  }
}


export async function setNewUIToken  (responseHeaders: any){
    window._rundeck.token = {
      'TOKEN': responseHeaders['x-rundeck-token-key'],
      'URI': responseHeaders['x-rundeck-token-uri']
    }
}

export async function getToken  (token_name: string){
    if (window._rundeck.tokens[token_name]) {
      return (window._rundeck.tokens[token_name])
    } else {
      let uiTokenElement = document.getElementById(token_name)

      if (uiTokenElement) {
        let jsonText = uiTokenElement.textContent || uiTokenElement.innerText
        let response = jsonText && jsonText !== '' ? JSON.parse(jsonText) : null
        window._rundeck.tokens[token_name] = response
        return(response)
      } else {
        throw new Error('No token exists')
      }
    }
}

export async function setToken  (responseHeaders: any, token_name: string) {
    window._rundeck.tokens[token_name] = {
      'TOKEN': responseHeaders['x-rundeck-token-key'],
      'URI': responseHeaders['x-rundeck-token-uri']
    }
}

export default {
  getUIAjaxTokens,
  setNewUIToken,
  getToken,
  setToken
}
