import { RundeckContext, AppLinks } from "../library";
import { defineComponent } from "vue";

declare global {
  const Component: ReturnType<typeof defineComponent>;
  interface Window {
    _rundeck: RundeckContext;
    appLinks: AppLinks;
    ProWebhookComponents: Component[];
    _workflowEditState: {
      isEditing: boolean;
      hasUnsavedChanges: boolean;
    };
    SVGInject: typeof import("@iconfu/svg-inject").default;
    markdeep: {
      format(content: string, inline?: boolean): string;
      format(
        content: string,
        inline: boolean,
        callback: (formatted: string, error?: string) => void,
      ): void;
    };
  }
}
