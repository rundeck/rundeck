import Vue from 'vue';
declare const _default: import("vue").VueConstructor<{
    hasActiveTour: boolean;
    tourSelectionModal: boolean;
    tours: any[];
} & {
    startTour: (tourLoader: string, tourEntry: any) => void;
    openTourSelectorModal: () => void;
} & Record<"eventBus", any> & Vue>;
export default _default;
