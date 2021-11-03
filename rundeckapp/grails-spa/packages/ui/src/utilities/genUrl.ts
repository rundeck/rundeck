export function _genUrlQuery(params: any) {
  let urlparams = []
  if (typeof (params) === 'string') {
    urlparams = [params]
  } else if (typeof (params) === 'object') {
    for (var e in params) {
      urlparams.push(encodeURIComponent(e) + '=' + encodeURIComponent(params[e]))
    }
  }
  return urlparams.join('&')
}

/**
 * Generate a URL
 * @param url
 * @param params
 * @returns {string}
 * @private
 */
export function _genUrl(url: string, params: any) {
  let paramString = _genUrlQuery(params)
  return url + (paramString.length ? ((url.indexOf('?') > 0 ? '&' : '?') + paramString) : '')
}