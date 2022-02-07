
/**
 * Cleans the input values to remove blank strings, null and undefined, returns a new Object with only valid properties
 * @param inConfig Input config object, where key is fieldname, and value is entered value
 */
export const cleanConfigInput = (inConfig: any): { [index: string]: any } => {
  const newConfig: { [index: string]: any } = {}
  // set newConfig with values that are not blank
  for (const prop in inConfig) {
    if (inConfig.hasOwnProperty(prop)) {
      if (
        typeof inConfig[prop] !== 'undefined' &&
        inConfig[prop] !== null &&
        inConfig[prop] !== ''
      ) {
        newConfig[prop] = inConfig[prop]
      }
    }
  }
  return newConfig
}

/**
 * Return a new object, where any Array values are converted to comma-separated strings
 * @param inConfig input configuration values
 */
export const convertArrayInput = (inConfig: any): { [index: string]: string } => {
  const newConfig: { [index: string]: string } = {}

  for (const prop in inConfig) {
    if (inConfig[prop] instanceof Array) {
      newConfig[prop] = inConfig[prop].join(',')
    } else {
      newConfig[prop] = inConfig[prop]
    }
  }
  return newConfig
}
