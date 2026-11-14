<template>
  <inner-link
    v-for="(item, index) in tagsViewStore.iframeViews"
    v-show="route.path === item.path"
    :key="item.path"
    :iframe-id="'iframe' + index"
    :src="iframeUrl(item.meta ? item.meta.link : '', item.query)"
  ></inner-link>
</template>

<script setup lang="ts">
import { useTagsViewStore } from '@/store/modules/tagsView';
import InnerLink from '../InnerLink/index.vue';

const route = useRoute();
const tagsViewStore = useTagsViewStore();

function iframeUrl(url: string | undefined, query: any) {
  if (Object.keys(query).length > 0) {
    const params = Object.keys(query)
      .map(key => key + '=' + query[key])
      .join('&');
    return url + '?' + params;
  }
  return url;
}
</script>
