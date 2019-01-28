export const generateUrl = (url: string, params: any) => {
  return new Promise<string>((resolve) => {
    let urlparams = []
    if (typeof (params) === 'string') {
      urlparams = [params]
    } else if (typeof (params) === 'object') {
      for (var e in params) {
        urlparams.push(`${encodeURIComponent(e)}=${encodeURIComponent(params[e])}`)
      }
    }
    resolve(url + (urlparams.length ? ((url.indexOf('?') > 0 ? '&' : '?') + urlparams.join('&')) : ''))
  })
}

export default {
  generateUrl
}
