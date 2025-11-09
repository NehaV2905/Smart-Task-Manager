const API = "http://localhost:8080/api/tasks";

export async function getTasks() {
  const res = await fetch(API);
  if (!res.ok) throw new Error("Failed to load tasks");
  return await res.json();
}

export async function addTask(task) {
  const res = await fetch(API, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(task),
  });
  if (!res.ok) throw new Error("Failed to add task");
  return await res.json();
}

export async function deleteTask(id) {
  const res = await fetch(`${API}/${id}`, { method: "DELETE" });
  if (!res.ok) throw new Error("Failed to delete task");
}
