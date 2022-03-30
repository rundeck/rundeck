/// <reference types="../../../common/cypress" />

import { createPinia, setActivePinia } from 'pinia'

import { mount } from '@cypress/vue'

Cypress.Commands.add('mountWithPinia', (component, options = {}) => {
  const piniaStore = {
    global: {
      provide: {
        pinia: setActivePinia(createPinia()),
      },
    },
  }
  mount(component, {
    ...piniaStore,
    ...options,
  })
})
