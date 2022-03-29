# PagerDuty Process Automation

<div align='center'>
  <img
    src='https://user-images.githubusercontent.com/11247099/111864893-a457fd00-899e-11eb-9f05-f4b88987541d.png'
    alt='Vitesse - Opinionated Vite starter template with rigorous linting'
    width='600'
  />
</div>

<br>

<div align='center'>
  <h3>Demonstrations</h3>
  <a href="https://vitesse-enterprise.netlify.app/">Live on Netlify</a>
  <br>
  <a href="https://vitesse-enterprise.vercel.app/">Live on Vercel</a>
  <br>
  <a href="https://stackblitz.com/fork/github/FranciscoKloganB/vitesse-enterprise?file=.stackblitzrc">
    Playground on Stackblitz
  </a>
</div>

<br>

## Features

- ⚡️ [Vue 3](https://github.com/vuejs/vue-next)

  - 🏃 [Vite 2](https://github.com/vitejs/vite), [pnpm](https://pnpm.js.org/), [ESBuild](https://github.com/evanw/esbuild)

- 🗂 [File based routing](./src/core/pages)

- 📦 [Components auto importing](./src/core/components)

- 🍍 [State Management via Pinia](https://pinia.esm.dev/)

- 📑 [Layout system](./src/core/layouts)

- 📲 [PWA](https://github.com/antfu/vite-plugin-pwa)

- 🎨 [Windi CSS](https://github.com/windicss/windicss)

- 😃 [Use icons from any icon sets, with no compromise](https://github.com/antfu/unplugin-icons)

- 🌍 [I18n ready](./locales)

- 🗒 [Markdown Support](https://github.com/antfu/vite-plugin-md)

- 🔥 Use the [new `<script setup>` syntax](https://github.com/vuejs/rfcs/pull/227)

- 📥 [APIs auto importing](https://github.com/antfu/unplugin-auto-import)

- 🖨 Static-site generation (SSG) via [vite-ssg](https://github.com/antfu/vite-ssg)

- 🦾 [TypeScript](https://www.typescriptlang.org/), of course

- 🦔 Critical CSS via [critters](https://github.com/GoogleChromeLabs/critters)

- 🖌️ [SCSS](https://sass-lang.com/) support

- 👮🏻 Format and Lint with VSCode and CLI
  - 💾 [Eslint](https://eslint.org/), [Import Sort*](https://github.com/renke/import-sort),
  [Markdownlint](https://github.com/DavidAnson/markdownlint), [Prettier](https://prettier.io/)
  and, [Stylelint](https://stylelint.io/).

- 🤖 Standards checking with pre-commit hooks. Test validation on pre-push with [Husky](https://github.com/typicode/husky)

- ⚙️ Component and end-to-end testing with [Cypress](https://cypress.io/)

- 📤 [GitHub Actions](https://github.com/features/actions)

- ☁️ Deploy on [Netlify](https://www.netlify.com/), zero-config - See [Netlify Deployment](#deploy-on-netlify)
- ☁️ Deploy on [Vercel](https://vercel.com/) - See [Vercel Deployment](#deploy-on-vercel)

**CLI only. VSCode plugin does not support configuration files.*

## Pre-packed

### UI Frameworks

- [`windicss`](https://github.com/windicss/windicss)
  - [`typography`](https://windicss.org/plugins/official/typography.html)

### Icons

- [`iconify`](https://iconify.design) - use icons from any icon sets [🔍Icônes](https://icones.netlify.app/)
- [`unplugin-icons`](https://github.com/antfu/unplugin-icons) - icons as Vue components

### Plugins

- [`pinia`](https://pinia.esm.dev)
- [`vue-router`](https://github.com/vuejs/vue-router)
  - [`vite-plugin-pages`](https://github.com/hannoeru/vite-plugin-pages)
  - [`vite-plugin-vue-layouts`](https://github.com/JohnCampionJr/vite-plugin-vue-layouts)
- [`vite-plugin-pwa`](https://github.com/antfu/vite-plugin-pwa)
- [`vite-plugin-windicss`](https://github.com/antfu/vite-plugin-windicss)
- [`vite-plugin-md`](https://github.com/antfu/vite-plugin-md)
  - [`markdown-it-prism`](https://github.com/jGleitz/markdown-it-prism)
  - [`prism-theme-vars`](https://github.com/antfu/prism-theme-vars)
- [`vue-i18n-next`](https://github.com/intlify/vue-i18n-next)
  - [`vite-plugin-vue-i18n`](https://github.com/intlify/vite-plugin-vue-i18n)
- [`vueuse`](https://github.com/antfu/vueuse)
  - [`@vueuse/head`](https://github.com/vueuse/head)
- [`vite-ssg-sitemap`](https://github.com/jbaubree/vite-ssg-sitemap)
- [`unplugin-vue-components`](https://github.com/antfu/unplugin-vue-components)
- [`unplugin-auto-import`](https://github.com/antfu/unplugin-auto-import)

### Nice to haves

- Alias `@` to `<rootDir>`
- Alias `~` to `<rootDir>/src` a.k.a. `<srcDir>`
- Predefined and fully typed global variables:
  - `VITE_APP_VERSION` is read from `package.json` version at build time
  - `VITE_APP_BUILD_EPOCH` is populated as `new Date().getTime()` at build time

### Dev tools

- [`typescript`](https://www.typescriptlang.org/)
- [`cypress`](https://cypress.io/) - E2E Testing
- [`pnpm`](https://pnpm.js.org/) - fast, disk space efficient package manager
- [`vite-ssg`](https://github.com/antfu/vite-ssg) - Static-site generation
  - [`critters`](https://github.com/GoogleChromeLabs/critters) - Critical CSS
- [`recommended vscode extensions`](./.vscode/extensions.json) - Near IDE experience on VSCode

## Variations

This template is strongly opinionated with my personal preferences and feature sets. It is a fork of
the [original template](https://github.com/antfu/vitesse) created by [@antfu](https://github.com/antfu)
and also takes inspiration from [vitesse-stackter](https://github.com/shamscorner/vitesse-stackter-clean-architect)
made by [@shamscorner](https://github.com/shamscorner).

For an up-to-date list of official and community contributions to the `vitesse ecosystem`, we recommend
checking the official repository's [variations section](https://github.com/antfu/vitesse#variations).

## Try it now

### GitHub Template

[Create a repo from this template on GitHub](https://github.com/FranciscoKloganB/vitesse-enterprise/generate).

### Clone to local

If you prefer to do it manually with the cleaner git history

```bash
npx degit FranciscoKloganB/vitesse-enterprise my-vitesse-enterprise-app
cd my-vitesse-enterprise-app
pnpm i # If you don't have pnpm installed, run: npm install -g pnpm
```

## Checklist

When you use this template, try follow the checklist to update your info properly

- [ ] Remove `.github/funding.yml` file which contains the funding info
- Remove `vercel.json` or `netlify.yml` depending on your deployment provider.
- [ ] Rename `name` field in `package.json`
- [ ] Rename `projectId` field in `cypress.json`
- [ ] Change the author name in `LICENSE`
- [ ] Change the title in `App.vue`
- [ ] Change the hostname in `vite.config.ts`
- [ ] Change the favicon in `public`
- [ ] Clean up the READMEs and remove routes
- [ ] Bump project dependencies (e.g.: dependabot) - We update them on a
best-effort-basis, every now and then.

## Usage

### Development

Run and visit <http://localhost:4000>

```bash
pnpm dev
```

### Code formatting

```bash
pnpm lint
```

### Testing

```bash
# Interactive test run
pnpm test:components
pnpm test:e2e
pnpm test:unit
```

### Build for production

```bash
# Ouputs files to dist folder. Ready to be served in SPA mode.
pnpm build:prod
# Outputs files to dist folder which passed through the Vite SSG pipeline. Ready to be served.
pnpm build:prod:ssg
```

### Previewing

```bash
# Runs project locally with files from dist folder
pnpm preview
# Can also run locally with HTTPS (may require sudo)
pnpm preview:https
```

### Separation of Concerns

- Create separate folder for each module and place them under `/src` folder
- The following folders are auto imported within each module
  - `components/`
  - `modules/`
  - `pages/`
  - `stores/`
- Place all the layouts in the `core/layouts` folder
- Place all the custom styles in the `core/assets/styles` folder and import them into `main.{css,scss}`

### Deploy on Netlify

Go to [Netlify](https://app.netlify.com/start) and select your clone, `OK` along the way,
and your App will be live in a minute.

### Deploy on Vercel

Vercel has some short-commings regarding `pnpm` based projects. After creating
and importing the project on [Vercel dashboard](https://vercel.com/dashboard) you will
need to navigate to the project settings and override the `Build Command` and `Install Command`
respectively, with the following bash commands:

  ```bash
    # Build Command
    npx pnpm i --store=node_modules/.pnpm-store && npx pnpm run build:prod:ssg
    # Install Command
    npm i -g pnpm && pnpm -i
  ```

Alternatevely, for zero configuration on import you need to alter `package.json` scripts to have
a `build` and `install` entries with the commands above.

## Why

On top of what was mentioned by [Anthony Fu](https://github.com/antfu/) in the original
[post](https://github.com/antfu/vitesse#why), working in conjunction with others often
requires strict styling rules. Creating a seamless developer experience across VSCode,
CLI and CI/CD consumes time. Vue, Vite and, Vitesse, are amazing tools for web developmen
and are now my go to choices for development. To avoid repeating myself over and over
again for to get this configurations going, I decided to fork Vitesse repository and
create a baseline project for my own (or other people) use, enabling us to be *vite*.
Pun intended.

## Feedback

If you notice any bug, inconsistency or change for improvement, feel free to either create
an issue so that I may try to fix it later or to propose a pull request with the changes.
