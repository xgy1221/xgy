import { Navigate, Route, Routes } from 'react-router-dom'
import { getUser } from './auth/roles'
import Layout from './components/Layout'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import Packages from './pages/Packages'
import Students from './pages/Students'
import Teachers from './pages/Teachers'
import Courses from './pages/Courses'
import Finance from './pages/Finance'
import Users from './pages/Users'
import './App.css'

function RequireAuth({ children }) {
  const user = getUser()
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
        <Route path="students" element={<Students />} />
        <Route path="teachers" element={<Teachers />} />
        <Route path="courses" element={<Courses />} />
        <Route path="packages" element={<Packages />} />
        <Route path="finance" element={<Finance />} />
        <Route path="users" element={<Users />} />
        <Route path="classes" element={<Navigate to="/courses" replace />} />
        <Route path="schedule" element={<Navigate to="/courses" replace />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
