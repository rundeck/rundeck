import {
  RundeckBrowser
} from '@rundeck/client'

const elem=document.getElementById('web_ui_token')
const elemtext=elem && elem.textContent || null
const token = elemtext?JSON.parse(elemtext):null
export const client = new RundeckBrowser(token.TOKEN, token.URI, window._rundeck.rdBase)
