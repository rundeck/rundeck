context('Basic', () => {
  beforeEach(() => {
    cy.visit('/')
  })

  it('basic nav', () => {
    cy.url().should('eq', 'http://localhost:4000/')

    cy.contains('[Home Layout]').should('exist')

    cy.get('#input')
      .should('be.visible')
      .type('Vitesse{Enter}')
      .url()
      .should('eq', 'http://localhost:4000/users/Vitesse')

    cy.contains('[Default Layout]').should('exist')

    cy.get('.btn').should('be.visible').click().url().should('eq', 'http://localhost:4000/')
  })

  it('markdown', () => {
    cy.get('[title="About"]')
      .should('be.visible')
      .click()
      .url()
      .should('eq', 'http://localhost:4000/about')

    cy.get('pre.language-js').should('exist')
  })
})
