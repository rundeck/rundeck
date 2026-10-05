/** A footer button of a PtModal, rendered with PtButton. */
type PtModalButton = {
  /** Identifier sent with the `button-click` event. */
  id: string;
  label: string;
  /** PtButton severity: success, secondary, info, warning or danger. */
  severity?: string;
  outlined?: boolean;
  text?: boolean;
  icon?: string;
  loading?: boolean;
  disabled?: boolean;
};

export { PtModalButton };
