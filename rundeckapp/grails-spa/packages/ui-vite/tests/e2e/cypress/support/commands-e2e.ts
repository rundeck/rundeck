/// <reference types="../../../common/cypress" />

Cypress.Commands.add('login', (url, email, password) => {
  cy.visit(url)
  cy.get('input[name=email]').type(email)
  cy.get('input[name=password]').type(password)
  cy.get('input[type=submit]').click()
})
