import { useCallback, useEffect, useState } from 'react'

/**
 * Hook genérico de GET. Estado: { data, loading, error, refresh }.
 * `fn` deve retornar a resposta ApiResponse já desempacotada (o axios interceptor faz isso).
 */
export default function useApi(fn, deps = []) {
  const [data,    setData]    = useState(null)
  const [loading, setLoading] = useState(true)
  const [error,   setError]   = useState(null)

  const refresh = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await fn()
      setData(res?.data ?? res)
    } catch (e) {
      setError(e)
    } finally {
      setLoading(false)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps)

  useEffect(() => { refresh() }, [refresh])

  return { data, loading, error, refresh }
}
