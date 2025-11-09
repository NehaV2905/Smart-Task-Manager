// App.js
import React, { useState, useEffect } from "react";
import "./App.css";

const PYTHON_API_URL = "http://localhost:5000/api";
const SPRING_API_URL = "http://localhost:8080/api/tasks";

function App() {
  const [tasks, setTasks] = useState([]);
  const [taskTitle, setTaskTitle] = useState("");
  const [duration, setDuration] = useState(30);

  const [tracking, setTracking] = useState(false);
  const [threshold, setThreshold] = useState(20);
  const [lastKeyPress, setLastKeyPress] = useState(new Date());
  const [procrastinationScore, setProcrastinationScore] = useState(0);
  const [idleSeconds, setIdleSeconds] = useState(0);
  const [isIdle, setIsIdle] = useState(false);
  const [totalIdleTime, setTotalIdleTime] = useState(0);
  const [idleEvents, setIdleEvents] = useState(0);
  const [startTime, setStartTime] = useState(null);
  const [stopTime, setStopTime] = useState(null);

  // Modal state
  const [showModal, setShowModal] = useState(false);
  const [timetableContent, setTimetableContent] = useState("");

  // ✅ Load tasks from Spring Boot on mount
  useEffect(() => {
    fetchTasks();
  }, []);

  const fetchTasks = async () => {
    try {
      const res = await fetch(SPRING_API_URL);
      const data = await res.json();
      setTasks(data);
    } catch (err) {
      console.error("Error fetching tasks:", err);
    }
  };

  const addTask = async () => {
    if (taskTitle.trim() === "") return;
    const newTask = { title: taskTitle, duration: parseInt(duration), completed: false };

    try {
      const res = await fetch(SPRING_API_URL, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(newTask)
      });

      if (res.ok) {
        const createdTask = await res.json();
        setTasks([createdTask, ...tasks]);
        setTaskTitle("");
      }
    } catch {
      alert("Spring Boot not running on port 8080");
    }
  };

  const toggleComplete = async (id) => {
    try {
      const res = await fetch(`${SPRING_API_URL}/${id}/toggle`, { method: "PATCH" });
      if (res.ok) {
        const updated = await res.json();
        setTasks(tasks.map((t) => (t.id === id ? updated : t)));
      }
    } catch (err) {
      console.error(err);
    }
  };

  const deleteTask = async (id) => {
    try {
      const res = await fetch(`${SPRING_API_URL}/${id}`, { method: "DELETE" });
      if (res.ok) setTasks(tasks.filter((t) => t.id !== id));
    } catch (err) {
      console.error(err);
    }
  };

  // ✅ Poll Python tracker status
  useEffect(() => {
    let interval;
    if (tracking) {
      interval = setInterval(async () => {
        try {
          const res = await fetch(`${PYTHON_API_URL}/status`);
          const data = await res.json();
          setProcrastinationScore(data.procrastination_score);
          setIdleSeconds(data.idle_seconds);
          setIsIdle(data.is_idle);
          setTotalIdleTime(data.total_idle_time);
          setIdleEvents(data.idle_events);
          setLastKeyPress(new Date(data.last_activity));
        } catch {}
      }, 1000);
    }
    return () => clearInterval(interval);
  }, [tracking]);

  const toggleTracking = async () => {
    try {
      if (!tracking) {
        await fetch(`${PYTHON_API_URL}/start`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ threshold })
        });
        const now = new Date();
        setTracking(true);
        setStartTime(now);
        setStopTime(null);
        setLastKeyPress(now);
        setProcrastinationScore(0);
        setIdleSeconds(0);
      } else {
        await fetch(`${PYTHON_API_URL}/stop`, { method: "POST" });
        setTracking(false);
        setStopTime(new Date());
      }
    } catch {
      alert("Python backend not running on port 5000");
    }
  };

  const generateTimetable = async () => {
    try {
      const res = await fetch(`${PYTHON_API_URL}/generate-timetable`);
      const data = await res.json();
      setTimetableContent(data.ai_generated_timetable);
      setShowModal(true);
    } catch {
      alert("⚠️ Python AI not running");
    }
  };

  const clearLogs = async () => {
    await fetch(`${PYTHON_API_URL}/clear`, { method: "POST" });
    setProcrastinationScore(0);
    setTotalIdleTime(0);
    setIdleEvents(0);
  };

  const totalTasks = tasks.length;
  const completedTasks = tasks.filter((t) => t.completed).length;

  return (
    <div className="app-container">
      {/* ✅ UI SAME AS SECOND FILE */}
      {/* ------- HEADER ------- */}
      <div className="header-container">
        <div className="header-content">
          <h1 className="main-title">ZenFlow</h1>
          <p className="subtitle">AI-Powered Productivity & Procrastination Tracker</p>
        </div>

        <div className="stats-grid">
          <div className="stat-card"><div className="stat-content">
            <div className="stat-icon blue"><span className="icon">✓</span></div>
            <div><div className="stat-number">{completedTasks}/{totalTasks}</div><div className="stat-label">Tasks Completed</div></div>
          </div></div>

          <div className="stat-card"><div className="stat-content">
            <div className="stat-icon purple"><span className="icon">📊</span></div>
            <div><div className="stat-number">{procrastinationScore}%</div><div className="stat-label">Procrastination</div></div>
          </div></div>

          <div className="stat-card"><div className="stat-content">
            <div className="stat-icon pink"><span className="icon">⏱️</span></div>
            <div><div className="stat-number">{idleSeconds}s</div><div className="stat-label">Idle Time</div></div>
          </div></div>
        </div>
      </div>

      {/* ------- MAIN CONTENT ------- */}
      <div className="main-grid">

        {/* ✅ Task Manager (Spring Boot) */}
        <div className="card">
          <div className="card-header"><div className="card-icon blue"><span className="icon">📅</span></div><h2>Task Manager</h2></div>

          <div className="input-section">
            <input value={taskTitle} onChange={(e) => setTaskTitle(e.target.value)}
              placeholder="What do you need to focus on?" className="text-input"
              onKeyPress={(e) => e.key === "Enter" && addTask()} />

            <div className="duration-row">
              <div className="duration-input">
                <label>Duration (minutes)</label>
                <input type="number" value={duration} onChange={(e) => setDuration(e.target.value)} className="number-input" />
              </div>
              <button onClick={addTask} className="add-button">+ Add</button>
            </div>
          </div>

          <div className="tasks-section">
            {tasks.length === 0 ? <p>No tasks yet</p> :
              tasks.map((task) => (
                <div key={task.id} className={`task-item ${task.completed ? "completed" : ""}`}>
                  <div className="task-content">
                    <button className={`checkbox ${task.completed ? "checked" : ""}`} onClick={() => toggleComplete(task.id)}>
                      {task.completed && "✓"}
                    </button>
                    <div>
                      <div className={task.completed ? "line-through" : ""}>{task.title}</div>
                      <div>{task.duration} mins</div>
                    </div>
                  </div>
                  <button onClick={() => deleteTask(task.id)} className="delete-button">🗑️</button>
                </div>
              ))}
          </div>
        </div>

        {/* ✅ Procrastination Tracker — unchanged */}
        <div className="card">
          <div className="card-header"><div className="card-icon purple"><span>📈</span></div><h2>Activity Tracker</h2></div>

          <div className="tracker-inputs">


            <button onClick={toggleTracking} className={`tracking-button ${tracking ? "stop" : "start"}`}>
              {tracking ? "Stop" : "Start"}
            </button>
          </div>

          <div className="status-box">
            <div>Status: {tracking ? (isIdle ? "😴 Idle" : "✅ Active") : "❌ Not Tracking"}</div>
            <div>Last Activity: {lastKeyPress.toLocaleTimeString()}</div>
            {startTime && <div>Started: {startTime.toLocaleTimeString()}</div>}
            {stopTime && <div>Stopped: {stopTime.toLocaleTimeString()}</div>}
            <div>Focus Score: {100 - procrastinationScore}%</div>
            <div>Idle Events: {idleEvents}</div>
            <div className="progress-container"><div className="progress-bar" style={{ width: `${100 - procrastinationScore}%` }} /></div>
          </div>

          <div className="button-group">
            <button onClick={generateTimetable} disabled={tasks.length === 0} className="generate-button">⚡ Generate Timetable</button>
            <button onClick={clearLogs} className="clear-button">🔄 Clear Stats</button>
          </div>
        </div>
      </div>

      {/* Timetable Modal */}
      {showModal && (
        <div className="modal-overlay" onClick={() => setShowModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2 className="modal-title">✅ AI Generated Timetable</h2>
              <button className="modal-close-button" onClick={() => setShowModal(false)}>✕</button>
            </div>
            <div className="modal-body">
              <pre className="timetable-text">{timetableContent}</pre>
            </div>
            <div className="modal-footer">
              <button className="modal-close-btn" onClick={() => setShowModal(false)}>Close</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default App;