<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import PcLayout from '@/layouts/PcLayout.vue'
import MobileLayout from '@/layouts/MobileLayout.vue'
import { useBreakpoint } from '@/composables/useBreakpoint'

const route = useRoute()
const { isMobile } = useBreakpoint()

const isLoginPage = computed(() => route.name === 'login')
</script>

<template>
  <router-view v-if="isLoginPage" />
  <MobileLayout v-else-if="isMobile">
    <router-view />
  </MobileLayout>
  <PcLayout v-else>
    <router-view />
  </PcLayout>
</template>

<style lang="scss">
@use '@/styles/main.scss';
</style>
