/* eslint-disable no-unused-vars */
/* eslint-disable @typescript-eslint/no-unused-vars */
/// <reference types="cypress" />

import type { MountingOptions } from '@vue/test-utils'

// https://github.com/cypress-io/cypress-example-todomvc
declare global {
  namespace Cypress {
    interface Chainable<Subject> {
      /**
       * Perfoms a login at a given URL with the provided email and password.
       *
       * @example cy.login('https://myauth.com/login', 'root', 'secret')
       */
      login(url: string, email: string, password: string): Chainable<any>
      /**
       * Mounts a component with access to a Pinia Store
       *
       * @example cy.mountWithPinia(TheNavbar)
       * @example cy.mountWithPinia(TheNavbar, { propsData: { msg: 'hello world' } })
       */
      mountWithPinia(component, options?: MountingOptions<any, any>): void
    }
  }
}
