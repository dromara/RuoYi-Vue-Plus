<template>
  <div>
    <i-frame v-model:src="url"></i-frame>
  </div>
</template>

<script setup>
import iFrame from '@/components/iFrame'

import { ref } from 'vue';

const url = ref(import.meta.env.VITE_APP_MONITRO_ADMIN);
</script>
