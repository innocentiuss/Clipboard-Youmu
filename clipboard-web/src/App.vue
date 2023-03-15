<template>
 <ClipboardView></ClipboardView>
</template>

<script lang="ts">
import { defineComponent } from 'vue';
import ClipboardView from './components/ClipboardView.vue';

export default defineComponent({
  name: 'App',
  components: {
    ClipboardView
  }
});
</script>
