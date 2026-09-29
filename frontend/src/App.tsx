import { Navigate, Route, Routes } from "react-router-dom";
import { NavBar } from "./components/NavBar";
import { JobDetailPage } from "./pages/JobDetailPage";
import { JobImageDetailPage } from "./pages/JobImageDetailPage";
import { JobImageListPage } from "./pages/JobImageListPage";
import { JobListPage } from "./pages/JobListPage";
import { TaskDetailPage } from "./pages/TaskDetailPage";
import { TaskListPage } from "./pages/TaskListPage";

export function App() {
  return (
    <div className="app">
      <NavBar />
      <main className="page">
        <Routes>
          <Route path="/" element={<Navigate to="/tasks" replace />} />
          <Route path="/tasks" element={<TaskListPage />} />
          <Route path="/tasks/:id" element={<TaskDetailPage />} />
          <Route path="/jobs" element={<JobListPage />} />
          <Route path="/jobs/:name" element={<JobDetailPage />} />
          <Route path="/job-images" element={<JobImageListPage />} />
          <Route path="/job-images/:id" element={<JobImageDetailPage />} />
        </Routes>
      </main>
    </div>
  );
}
