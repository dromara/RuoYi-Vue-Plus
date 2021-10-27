<template>
  <i-frame :src="url" />
</template>
<script>
import iFrame from "@/components/iFrame/index";
export default {
  name: "XxlJob",
  components: { iFrame },
  data() {
    return {
      url: process.env.VUE_APP_XXL_JOB_ADMIN
    };
  },
};
</script>
