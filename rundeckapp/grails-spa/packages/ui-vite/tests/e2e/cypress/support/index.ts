/// <reference types="cypress" />

import './commands-e2e'

Cypress.on('window:before:load', (win: any) => {
  win.handleFromCypress = function (request: {
    url: RequestInfo
    method: any
    requestHeaders: any
    requestBody: any
  }) {
    return fetch(request.url, {
      method: request.method,
      headers: request.requestHeaders,
      body: request.requestBody,
    }).then((res: any) => {
      const content =
        res.headers.map['content-type'] === 'application/json' ? res.json() : res.text()
      return new Promise((resolve) => {
        content.then((body: any) => resolve([res.status, res.headers, body]))
      })
    })
  }
})
