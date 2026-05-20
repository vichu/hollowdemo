<script setup>
import { useNav } from '@slidev/client'
const { currentPage } = useNav()
</script>

<template>
  <div class="slide-logo" v-if="currentPage !== 1">
    <img src="/images/osslf.png" alt="Open Source Summit NA 2026" />
  </div>
</template>

<style scoped>
.slide-logo {
  position: fixed;
  bottom: 12px;
  right: 16px;
  z-index: 100;
  pointer-events: none;
}

.slide-logo img {
  height: 36px;
  opacity: 0.85;
  object-fit: contain;
}
</style>
