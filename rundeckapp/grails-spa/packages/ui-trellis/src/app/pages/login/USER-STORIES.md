# Login Page — User Stories

Each story maps to a discrete UI behavior in `login.gsp`. Chrome DevTools verification steps are included per story.

---

## US-01: Username field receives autofocus on page load

**As a** user arriving at the login page,  
**I want** the username field to be focused automatically,  
**So that** I can start typing immediately without clicking.

**Acceptance criteria:**
- On DOM-ready, `document.activeElement` is the username input (`#login`)

**Chrome DevTools verification:**
```js
// Console on login page:
document.activeElement.id === 'login'   // must be true
```

---

## US-02: Instance name label shown when configured

**As a** Rundeck admin who configured `gui.instanceName`,  
**I want** the instance name to appear at the top of the login card,  
**So that** users know which Rundeck instance they're logging into.

**Acceptance criteria:**
- When `gui.instanceName` has a value, a `.instance-label` element is visible with that value
- When `gui.instanceName` is empty/unset, no `.instance-label` element exists

**Chrome DevTools verification:**
```js
// When configured:
document.querySelector('.instance-label').textContent.trim()  // must match config value

// When not configured:
document.querySelector('.instance-label')  // must be null
```

---

## US-03: App logo is rendered via SVG injection

**As a** user,  
**I want** the Rundeck logo to render as an inline SVG,  
**So that** it scales crisply and respects the theme colors.

**Acceptance criteria:**
- An `<img>` tag with `src` pointing to the app logo asset is replaced by an `<svg>` element by SVGInject
- The SVG is a child of `.logo`

**Chrome DevTools verification:**
```js
// After page load (SVGInject runs on img onload):
document.querySelector('.logo svg')  // must exist (not null)
document.querySelector('.logo img[src*="logo"]')  // may be null (replaced) or may remain — check SVGInject behavior
```

---

## US-04: Custom user logo shown when configured

**As a** Rundeck admin who configured `gui.logo`,  
**I want** a custom logo image to appear below the main logo,  
**So that** our brand identity is visible on the login page.

**Acceptance criteria:**
- When `gui.logo` is configured, an `<img>` tag sourced from `/user-assets/<logo>` is visible inside `.logo`
- When not configured, no such `<img>` is present

**Chrome DevTools verification:**
```js
// When configured:
document.querySelector('.logo img[src*="user-assets"]')  // must exist

// When not configured:
document.querySelector('.logo img[src*="user-assets"]')  // must be null
```

---

## US-05: Welcome HTML content shown when configured

**As a** Rundeck admin who configured `gui.login.welcomeHtml`,  
**I want** custom HTML content to appear above the login fields,  
**So that** I can give users context or branding information.

**Acceptance criteria:**
- When `gui.login.welcomeHtml` has a value, a div with the sanitized HTML appears before the SSO/local login sections
- When not configured, no such div exists

**Chrome DevTools verification:**
```js
// When configured:
document.querySelector('.card-content > div:first-child span').innerHTML  // must contain welcome HTML content
```

---

## US-06: SSO login button appears when SSO is enabled

**As a** user on an instance with SSO configured,  
**I want** an SSO login button to appear,  
**So that** I can authenticate through my identity provider.

**Acceptance criteria:**
- When both `request.showSSOButton` attribute and `sso.loginButton.enabled=true`, the `.sso-login` div with `.sso-login-link` anchor is visible
- The anchor `href` matches `sso.loginButton.url`
- The anchor text matches `sso.loginButton.title`
- When SSO is not configured, no `.sso-login` element exists

**Chrome DevTools verification:**
```js
// When SSO enabled:
document.querySelector('.sso-login-link').href    // must match configured SSO URL
document.querySelector('.sso-login-link').textContent.trim()  // must match configured title

// When SSO disabled:
document.querySelector('.sso-login')  // must be null
```

---

## US-07: SSO button icon shown when image is enabled

**As a** user on an instance with SSO configured with image enabled,  
**I want** an icon to appear alongside the SSO button text,  
**So that** the button is more visually distinct.

**Acceptance criteria:**
- When `sso.loginButton.image.enabled=true`, an `<img class="sso-login-img">` is visible inside `.sso-login-container`
- When the image is disabled, no `.sso-login-img` is present

**Chrome DevTools verification:**
```js
document.querySelector('.sso-login-img')  // must exist when image enabled, null when disabled
```

---

## US-08: Local login form shown when local login is enabled

**As a** user,  
**I want** to see username and password fields,  
**So that** I can log in with my local Rundeck credentials.

**Acceptance criteria:**
- When `login.localLogin.enabled=true` (default), `#login` and `#password` inputs are visible
- When local login is disabled, these fields do not appear

**Chrome DevTools verification:**
```js
document.querySelector('#login')     // must exist and be visible when local login enabled
document.querySelector('#password')  // must exist and be visible
```

---

## US-09: Welcome message text shown when configured

**As a** Rundeck admin who configured `gui.login.welcome`,  
**I want** a welcome message to appear above the username field,  
**So that** users are greeted with a relevant message.

**Acceptance criteria:**
- When `gui.login.welcome` is set (or the i18n key has a value), an `<h4 class="text-default">` with the message text appears above the form fields
- When neither source has a value, no such element is present

**Chrome DevTools verification:**
```js
document.querySelector('h4.text-default')?.textContent.trim()  // must match configured message when set
```

---

## US-10: Empty username blocks form submission with inline error

**As a** user who clicks Login without entering a username,  
**I want** to see an error message,  
**So that** I understand the username field is required.

**Acceptance criteria:**
- Clicking the Login button with an empty username shows `#empty-username-msg`
- The form is NOT submitted (network request to `/j_security_check` is NOT initiated)
- Entering any character and re-submitting hides the error

**Chrome DevTools verification:**
```js
// 1. Clear username field if anything typed:
document.querySelector('#login').value = ''

// 2. Click submit:
document.querySelector('#btn-login').click()

// 3. Error must be visible:
getComputedStyle(document.querySelector('#empty-username-msg')).display !== 'none'  // true

// 4. Confirm no network request fired — check Network tab, no POST to /j_security_check
```

---

## US-11: Login spinner appears and button hides on valid submit

**As a** user who submits valid credentials,  
**I want** to see a loading indicator,  
**So that** I know the system is processing my login.

**Acceptance criteria:**
- After submitting with a non-empty username, `#btn-login` becomes hidden (`display: none`)
- `#login-spinner` becomes visible
- These state changes happen synchronously before the POST request completes

**Chrome DevTools verification:**
```js
// 1. Enter a username:
document.querySelector('#login').value = 'testuser'
document.querySelector('#password').value = 'testpass'

// 2. Click login (may redirect — watch quickly):
document.querySelector('#btn-login').click()

// 3. Immediately check:
getComputedStyle(document.querySelector('#btn-login')).display    // must be 'none'
getComputedStyle(document.querySelector('#login-spinner')).display // must not be 'none'
```

---

## US-12: Server login error message displayed after failed login

**As a** user who enters incorrect credentials,  
**I want** to see a clear error message,  
**So that** I know my login attempt failed.

**Acceptance criteria:**
- When the server returns `flash.loginErrorCode`, an `.alert-danger` div with the translated error message is visible in `.card-footer`
- When no error, no `.alert-danger` is shown (or it is hidden)

**Chrome DevTools verification:**
```js
// After a failed login attempt:
document.querySelector('.card-footer .alert-danger').textContent.trim()  // must be non-empty error message

// On fresh page load (no error):
document.querySelector('.card-footer .alert-danger')  // must be null or hidden
```

---

## US-13: Footer message HTML shown when configured

**As a** Rundeck admin who configured `gui.login.footerMessageHtml`,  
**I want** custom HTML to appear in the card footer,  
**So that** I can add help links or compliance text.

**Acceptance criteria:**
- When `gui.login.footerMessageHtml` is set, sanitized HTML appears in `.card-footer` after the error area
- When not configured, no such content exists

**Chrome DevTools verification:**
```js
// When configured:
document.querySelector('.card-footer > div:last-child span').innerHTML  // must contain footer HTML
```

---

## US-14: Disclaimer section shown when configured

**As a** Rundeck admin who configured `gui.login.disclaimer`,  
**I want** a disclaimer to appear below the login card,  
**So that** users see required legal or policy notices.

**Acceptance criteria:**
- When `gui.login.disclaimer` is set, a second `.row` with `.card-content` and the sanitized disclaimer appears below the main card row
- When not configured, no second row exists

**Chrome DevTools verification:**
```js
// When configured (second .row after the card):
document.querySelectorAll('.content .row').length  // must be 2

// When not configured:
document.querySelectorAll('.content .row').length  // must be 1
```

---

## US-15: Form submits via POST to /j_security_check

**As a** developer,  
**I want** the login form to POST credentials to the correct Spring Security endpoint,  
**So that** authentication is handled by the framework.

**Acceptance criteria:**
- The `<form>` element has `action` ending in `/j_security_check`
- The `<form>` element has `method="post"` (case-insensitive)
- The username field has `name="j_username"`
- The password field has `name="j_password"`

**Chrome DevTools verification:**
```js
const form = document.querySelector('form')
form.action.endsWith('/j_security_check')  // true
form.method.toLowerCase()                  // 'post'
document.querySelector('[name="j_username"]')  // must exist
document.querySelector('[name="j_password"]')  // must exist
```

---

## US-16: Title link navigates to configured URL or home

**As a** user,  
**I want** clicking the logo to navigate to the configured title link or home,  
**So that** the logo is a home link.

**Acceptance criteria:**
- The logo `<a>` tag has `href` equal to `gui.titleLink` if configured, otherwise `/`

**Chrome DevTools verification:**
```js
document.querySelector('.logo > a').href  // must match configured titleLink or end in '/'
```
