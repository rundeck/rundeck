export interface EditorOptions {
  animatedScroll: boolean;
  autoScrollEditorIntoView: boolean;
  behavioursEnabled: boolean;
  copyWithEmptySelection: boolean;
  cursorStyle: "ace" | "slim" | "smooth" | "wide";
  customScrollbar: boolean;
  displayIndentGuides: boolean;
  dragDelay: number;
  dragEnabled: boolean;
  enableAutoIndent: boolean;
  enableBasicAutocompletion: boolean | AceAutoCompleter[];
  enableCodeLens: boolean;
  enableKeyboardAccessibility: boolean;
  enableLiveAutocompletion: boolean | AceAutoCompleter[];
  enableMobileMenu: boolean;
  enableMultiselect: boolean;
  enableSnippets: boolean;
  fadeFoldWidgets: boolean;
  firstLineNumber: number;
  fixedWidthGutter: boolean;
  focusTimeout: number;
  foldStyle: "markbegin" | "markbeginend" | "manual";
  fontFamily: string;
  fontSize: string | number;
  hScrollBarAlwaysVisible: boolean;
  hasCssTransforms: boolean;
  highlightActiveLine: boolean;
  highlightGutterLine: boolean;
  highlightIndentGuides: boolean;
  highlightSelectedWord: boolean;
  indentedSoftWrap: boolean;
  keyboardHandler: string;
  liveAutocompletionDelay: number;
  liveAutocompletionThreshold: number;
  maxLines: number;
  maxPixelHeight: number;
  mergeUndoDeltas: boolean | "always";
  minLines: number;
  mode: string;
  navigateWithinSoftTabs: boolean;
  newLineMode: "auto" | "unix" | "windows";
  overwrite: boolean;
  placeholder: string;
  printMargin: number | boolean;
  printMarginColumn: number;
  readOnly: boolean;
  relativeLineNumbers: boolean;
  scrollPastEnd: number;
  scrollSpeed: number;
  selectionStyle: "line" | "text" | "fullLine" | "screenLine";
  showFoldWidgets: boolean;
  showFoldedAnnotations: boolean;
  showGutter: boolean;
  showInvisibles: boolean;
  showLineNumbers: boolean;
  showPrintMargin: boolean;
  tabSize: number;
  textInputAriaLabel: string;
  theme: string;
  tooltipFollowsMouse: boolean;
  useResizeObserver: boolean;
  useSoftTabs: boolean;
  useSvgGutterIcons: boolean;
  useWorker: boolean;
  vScrollBarAlwaysVisible: boolean;
  value: string;
  wrap: number | boolean | "off" | "free" | "printmargin";
  wrapBehavioursEnabled: boolean;
  wrapMethod: "code" | "text" | "auto";
}

// The editor object is freakin massive and I
// didn't want to import the whole thing so
// I'm just ignoring the type errors in the
// function below.
export interface AceAutoCompleter {
  hideInlinePreview?: boolean;
  id?: string;
  identifierRegexps?: RegExp[];
  // @ts-ignore
  insertMatch?: (editor, data) => void;
  // @ts-ignore
  onInsert?: (editor, completion) => void;
  // @ts-ignore
  onSeen?: (editor, completion) => void;
  triggerCharacters?: string[];
  cancel?(): void;
  // @ts-ignore
  getCompletions(editor, session, position, prefix, callback): void;
  getDocTooltip?(item: Completion): string | void | Completion;
}

export interface Completion {
  value: string;
  meta: string;
  type?: string | undefined;
  caption?: string | undefined;
  snippet?: any;
  score?: number | undefined;
  exactMatch?: number | undefined;
  docHTML?: string | undefined;
  title?: string;
  desc?: string;
}
