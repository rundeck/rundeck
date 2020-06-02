export interface Tour {
  key: string
  name: string
  steps: Array<Step>
}

export interface Step {
  title: string
  content: string
  nextStepUrl?: string
  currentUrl?: string
  stepIndicator?: string
  stepIndicatorPosition?: string
}
