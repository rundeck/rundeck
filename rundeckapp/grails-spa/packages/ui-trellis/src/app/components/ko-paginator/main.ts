import { createApp, defineComponent, PropType } from "vue";
import Pagination from "../../../library/components/utils/Pagination.vue";

interface KoPagerPage {
  current: boolean;
  [key: string]: unknown;
}

interface KoPager {
  pageList: {
    (): KoPagerPage[];
    subscribe(callback: (pages: KoPagerPage[]) => void): void;
  };
  setPage(page: number): void;
}

interface KoPaginationEvent {
  name: string;
  pager: KoPager;
}

/** Adapt KO Pager with Paginator wrapper */
const KoPaginator = defineComponent({
  name: "KoPaginator",
  components: { Pagination },
  props: {
    pager: {
      type: Object as PropType<KoPager>,
      required: true,
    },
  },
  data() {
    return {
      activePage: 0,
      totalPages: 0,
    };
  },

  created() {
    this.activePage =
      this.pager.pageList().findIndex((p: KoPagerPage) => p.current) + 1;
    this.totalPages = this.pager.pageList().length;
  },

  mounted() {
    this.pager.pageList.subscribe((pages: KoPagerPage[]) => {
      this.activePage = pages.findIndex((p: KoPagerPage) => p.current) + 1;
      this.totalPages = pages.length;
    });
  },

  methods: {
    handlePageChange(page: number) {
      this.pager.setPage(page - 1);
    },
  },
  template: `
    <Pagination 
        v-model="activePage"
        :totalPages="totalPages"
        @change="handlePageChange"
    />
    `,
});

const mounted = new Map<string, boolean>();

/** Listen to events advertising pagination and mount to elements */
window._rundeck.eventBus.on("ko-pagination", (event: KoPaginationEvent) => {
  const { name, pager } = event;

  if (!mounted.has(name)) {
    const elements = document.querySelectorAll(
      `[data-ko-pagination='${name}']`,
    );

    for (const elm of elements) {
      const app = createApp(KoPaginator, {
        pager,
      });
      app.mount(elm);
    }

    mounted.set(name, true);
  }
});
