<template>
  <div id="app">
    <global-navbar />
    <main class="mt-10 mb-10">
      <RouterView />
    </main>
    <global-utilitybar />
  </div>
</template>

<style>
html, body{
  @apply bg-gray-100;
}
</style>