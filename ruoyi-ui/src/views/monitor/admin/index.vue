<template>
  <i-frame :src="url" />
</template>
<script>
import iFrame from "@/components/iFrame/index";
export default {
  name: "Admin",
  components: { iFrame },
  data() {
    console.log(process.env)
    return {
      url: process.env.VUE_APP_MONITRO_ADMIN
    };
  },
};
</script>
