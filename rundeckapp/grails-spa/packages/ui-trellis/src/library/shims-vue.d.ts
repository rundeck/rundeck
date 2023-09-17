declare module '*.vue' {
  import { defineComponent } from "vue";
  const Component: ReturnType<typeof defineComponent>;
  export default Component;
}

declare module 'vue2-ace-editor' {

}
declare module 'vue3-markdown';
