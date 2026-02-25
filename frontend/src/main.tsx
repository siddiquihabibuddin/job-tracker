import React from 'react'
import ReactDOM from 'react-dom/client'
import { createBrowserRouter, RouterProvider, Navigate } from 'react-router-dom'
import App from './App'
import './index.css'

import Dashboard from './routes/Dashboard'
import Applications from './routes/Applications'
import ApplicationDetail from './routes/ApplicationDetail'
import NewApplication from './routes/NewApplication'
import Profile from './routes/Profile'
import Login from './routes/Login'
import ErrorPage from './routes/ErrorPage'
import { AuthProvider } from './auth/AuthContext'
import RequireAuth from './auth/RequireAuth'
import { ErrorBoundary } from './components/ErrorBoundary'

const router = createBrowserRouter([
  {
    path: '/',
    element: <App />,
    errorElement: <ErrorPage />,
    children: [
      { index: true, element: <Navigate to="dashboard" replace /> }, // / -> /dashboard
      { path: 'login', element: <Login /> },                         // public
      {
        element: <RequireAuth />,                                    // guard this branch
        children: [
          { path: 'dashboard', element: <Dashboard /> },
          { path: 'applications', element: <Applications /> },
          { path: 'applications/new', element: <NewApplication /> },
          { path: 'applications/:id', element: <ApplicationDetail /> },
          { path: 'profile', element: <Profile /> },
        ],
      },
      { path: '*', element: <ErrorPage /> },
    ],
  },
])

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ErrorBoundary>
      <AuthProvider>
        <RouterProvider router={router} />
      </AuthProvider>
    </ErrorBoundary>
  </React.StrictMode>
)
