import { isRouteErrorResponse, useRouteError, Link } from 'react-router-dom'

export default function ErrorPage() {
  const err = useRouteError()
  const title = isRouteErrorResponse(err) ? `${err.status} ${err.statusText}` : 'Something went wrong'

  return (
    <main className="container">
      <article>
        <header>{title}</header>
        <p>{(err as any)?.data || (err as Error)?.message || 'Unexpected error'}</p>
        <p><Link to="/dashboard"><button>Back to Dashboard</button></Link></p>
      </article>
    </main>
  )
}