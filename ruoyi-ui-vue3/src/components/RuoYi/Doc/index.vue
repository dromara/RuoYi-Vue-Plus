<template>
  <div>
    <svg-icon icon-class="question" @click="goto" />
  </div>
</template>

<script setup>
const url = ref('https://gitee.com/JavaLionLi/RuoYi-Vue-Plus/wikis/pages');

function goto() {
  window.open(url.value)
}
</script>