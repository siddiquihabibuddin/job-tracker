import axios from 'axios'

export const appsBase =
  import.meta.env.VITE_API_APPS_BASE?.toString() || '/v1'
export const statsBase =
  import.meta.env.VITE_API_STATS_BASE?.toString() || '/v1'

export const httpApps = axios.create({
  baseURL: appsBase,
  withCredentials: true,
})

export const httpStats = axios.create({
  baseURL: statsBase,
  withCredentials: true,
})

function getToken() {
  try { return sessionStorage.getItem('jt_token') } catch { return null }
}

// Called by AuthContext to wire up the 401 handler after React mounts
let onUnauthorized: (() => void) | null = null
export function registerUnauthorizedHandler(handler: () => void) {
  onUnauthorized = handler
}

function attachInterceptors(instance: typeof httpApps) {
  instance.interceptors.request.use((cfg) => {
    const t = getToken()
    if (t) (cfg.headers ||= {}).Authorization = `Bearer ${t}`
    return cfg
  })

  instance.interceptors.response.use(
    (res) => res,
    (err) => {
      if (err?.response?.status === 401 && onUnauthorized) {
        onUnauthorized()
      }
      return Promise.reject(err)
    }
  )
}

attachInterceptors(httpApps)
attachInterceptors(httpStats)
