import { Navigate, Route, Routes } from 'react-router-dom'
import Layout from './components/Layout'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import Packages from './pages/Packages'
import Students from './pages/Students'
import Classes from './pages/Classes'
import Schedule from './pages/Schedule'
import './App.css'

function RequireAuth({ children }) {
  const user = localStorage.getItem('xgy_web_user')
  if (!user) return <Navigate to="/login" replace />
  return children
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route
        path="/"
        element={
          <RequireAuth>
            <Layout />
          </RequireAuth>
        }
      >
        <Route index element={<Dashboard />} />
        <Route path="packages" element={<Packages />} />
        <Route path="students" element={<Students />} />
        <Route path="classes" element={<Classes />} />
        <Route path="schedule" element={<Schedule />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
