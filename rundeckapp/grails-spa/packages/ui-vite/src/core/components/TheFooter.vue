<script setup lang="ts">
import { isDark, toggleDark } from '~/core/hooks';

const { t, availableLocales, locale } = useI18n()

const toggleLocales = () => {
  // change to some real logic
  const locales = availableLocales
  locale.value = locales[(locales.indexOf(locale.value) + 1) % locales.length]
}
</script>

<template>
  <nav class="flex items-center justify-end text-xl mt-6  fixed bottom-0 w-full border-t border-gray-300 dark:border-gray-700 h-8 bg-white dark:bg-black">



    <button
      class=" mx-2 !outline-none flex items-center"
      data-cy="theme-toggler"
      :title="t('button.toggle_dark')"
      @click="toggleDark()"
    >
      <carbon-moon v-if="isDark" />
      <carbon-sun v-else />
    </button>

    <a
      class="flex items-center mx-2"
      data-cy="language-selector"
      :title="t('button.toggle_langs')"
      @click="toggleLocales"
    >
      <carbon-language />
    </a>

    <router-link class="flex items-center mx-2" to="/about" :title="t('button.about')">
      <carbon-dicom-overlay />
    </router-link>

    <a
      class="flex items-center mx-2"
      rel="noreferrer"
      href="https://github.com/pagerduty"
      target="_blank"
      title="GitHub"
    >
      <carbon-logo-github />
    </a>
  </nav>
</template>
