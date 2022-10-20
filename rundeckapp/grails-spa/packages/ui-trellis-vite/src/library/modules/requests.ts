// export const request = (url, params) => {
//   return new Promise((resolve) => {
//     let urlparams = []
//     if (typeof (params) === 'string') {
//       urlparams = [params]
//     } else if (typeof (params) === 'object') {
//       for (var e in params) {
//         urlparams.push(`${encodeURIComponent(e)}=${encodeURIComponent(params[e])}`)
//       }
//     }
//     resolve(url + (urlparams.length ? ((url.indexOf('?') > 0 ? '&' : '?') + urlparams.join('&')) : ''))
//   })
// }

// export default {
//   request
// }
