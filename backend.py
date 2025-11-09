from flask import Flask, jsonify, request
from flask_cors import CORS
from pynput import keyboard
import time
import threading
import psycopg2
from psycopg2 import sql
import google.generativeai as genai


genai.configure(api_key="apikey")

app = Flask(__name__)
CORS(app)

last_activity_time = time.time()
idle_start_time = None
is_idle = False
procrastination_log = []
tracking_active = False
listener = None
idle_threshold = 20  # seconds
start_time = None
stop_time = None

DB_CONFIG = {
    "dbname": "task_db",
    "user": "postgres",
    "password": "password",
    "host": "localhost",
    "port": 5432
}

# ✅ Create table if not exists
def init_db():
    try:
        conn = psycopg2.connect(**DB_CONFIG)
        cur = conn.cursor()
        cur.execute("""
            CREATE TABLE IF NOT EXISTS idle (
                id SERIAL PRIMARY KEY,
                start_time TIMESTAMP,
                stop_time TIMESTAMP,
                procrastination_score INTEGER,
                total_idle_time FLOAT,
                idle_events INTEGER
            );
        """)
        conn.commit()
        cur.close()
        conn.close()
        print("[DB] Table 'idle' is ready ✅")
    except Exception as e:
        print("[DB ERROR]", e)

def save_to_db(start_time, stop_time, procrastination_score, total_idle_time, idle_events):
    """Insert a new idle tracking record."""
    try:
        conn = psycopg2.connect(**DB_CONFIG)
        cur = conn.cursor()
        query = sql.SQL("""
            INSERT INTO idle (start_time, stop_time, procrastination_score, total_idle_time, idle_events)
            VALUES (%s, %s, %s, %s, %s);
        """)
        cur.execute(query, (start_time, stop_time, procrastination_score, total_idle_time, idle_events))
        conn.commit()
        cur.close()
        conn.close()
        print(f"[DB] Record inserted ✅ ({start_time} → {stop_time}, score={procrastination_score})")
    except Exception as e:
        print("[DB INSERT ERROR]", e)

def on_press(key):
    global last_activity_time, is_idle, idle_start_time
    if not tracking_active:
        return

    last_activity_time = time.time()

    if is_idle:
        idle_end = time.strftime("%Y-%m-%d %H:%M:%S", time.localtime(last_activity_time))
        idle_duration = last_activity_time - idle_start_time
        procrastination_log.append({
            "start": time.strftime("%Y-%m-%d %H:%M:%S", time.localtime(idle_start_time)),
            "end": idle_end,
            "duration_secs": round(idle_duration, 2)
        })
        print(f"[+] Activity resumed at {idle_end}, idle duration: {idle_duration:.2f} sec")
        is_idle = False
        idle_start_time = None

def check_idle():
    global last_activity_time, idle_start_time, is_idle, tracking_active
    while True:
        if not tracking_active:
            time.sleep(1)
            continue

        current_time = time.time()
        idle_time = current_time - last_activity_time

        if idle_time > idle_threshold and not is_idle:
            is_idle = True
            idle_start_time = last_activity_time + idle_threshold
            print(f"[!] Idle detected from {time.strftime('%Y-%m-%d %H:%M:%S', time.localtime(idle_start_time))}")

        time.sleep(1)

threading.Thread(target=check_idle, daemon=True).start()

@app.route('/api/start', methods=['POST'])
def start_tracking():
    global tracking_active, listener, last_activity_time, idle_threshold, start_time, procrastination_log

    data = request.json
    if data and 'threshold' in data:
        idle_threshold = int(data['threshold'])

    if not tracking_active:
        tracking_active = True
        last_activity_time = time.time()
        start_time = time.strftime("%Y-%m-%d %H:%M:%S", time.localtime(last_activity_time))
        procrastination_log = []

        listener = keyboard.Listener(on_press=on_press)
        listener.start()

        print(f"[TRACKING STARTED] at {start_time}")
        return jsonify({
            "status": "started",
            "message": "Tracking started successfully",
            "threshold": idle_threshold
        })

    return jsonify({"status": "already_running", "message": "Tracking is already active"})

@app.route('/api/stop', methods=['POST'])
def stop_tracking():
    global tracking_active, listener, is_idle, idle_start_time, stop_time

    if tracking_active:
        tracking_active = False
        stop_time_epoch = time.time()
        stop_time = time.strftime("%Y-%m-%d %H:%M:%S", time.localtime(stop_time_epoch))

        if listener:
            listener.stop()
            listener = None

        if is_idle:
            idle_end = time.time()
            idle_duration = idle_end - idle_start_time
            procrastination_log.append({
                "start": time.strftime("%Y-%m-%d %H:%M:%S", time.localtime(idle_start_time)),
                "end": time.strftime("%Y-%m-%d %H:%M:%S", time.localtime(idle_end)),
                "duration_secs": round(idle_duration, 2)
            })
            is_idle = False
            idle_start_time = None

        total_procrastination = sum(log['duration_secs'] for log in procrastination_log)
        procrastination_score = min(100, int((total_procrastination / 60) * 10))
        idle_events = len(procrastination_log)

        # 🧠 Save tracking session to DB
        save_to_db(start_time, stop_time, procrastination_score, total_procrastination, idle_events)

        print(f"[TRACKING STOPPED] at {stop_time}")
        return jsonify({
            "status": "stopped",
            "message": "Tracking stopped successfully"
        })

    return jsonify({"status": "not_running", "message": "Tracking is not active"})

@app.route('/api/status', methods=['GET'])
def get_status():
    global last_activity_time, is_idle, procrastination_log, tracking_active

    current_time = time.time()
    idle_seconds = int(current_time - last_activity_time) if tracking_active else 0
    total_procrastination = sum(log['duration_secs'] for log in procrastination_log)
    procrastination_score = min(100, int((total_procrastination / 60) * 10))

    return jsonify({
        "tracking_active": tracking_active,
        "is_idle": is_idle,
        "idle_seconds": idle_seconds,
        "last_activity": time.strftime("%Y-%m-%d %H:%M:%S", time.localtime(last_activity_time)),
        "procrastination_score": procrastination_score,
        "total_idle_time": round(total_procrastination, 2),
        "idle_events": len(procrastination_log)
    })

@app.route('/api/logs', methods=['GET'])
def get_logs():
    return jsonify({
        "logs": procrastination_log,
        "total_events": len(procrastination_log)
    })

@app.route('/api/clear', methods=['POST'])
def clear_logs():
    global procrastination_log
    procrastination_log = []
    return jsonify({
        "status": "cleared",
        "message": "Logs cleared successfully"
    })

def get_db_connection():
    return psycopg2.connect(
        host="localhost",
        database="task_db",
        user="postgres",
        password="password"
    )

@app.route("/api/generate-timetable")
def generate_timetable():
    try:
        conn = get_db_connection()
        cur = conn.cursor()

        cur.execute("""
            SELECT title, duration, completed
            FROM tasks
            WHERE completed = false
        """)
        tasks = cur.fetchall()

        # ✅ Fetch idle periods
        cur.execute("""
            SELECT start_time, stop_time, procrastination_score, total_idle_time
            FROM idle
        """)
        idle_periods = cur.fetchall()

        cur.close()
        conn.close()

        # ✅ Transform & Sort data
        tasks_sorted = sorted(tasks, key=lambda x: x[1])  # shortest tasks first
        idle_sorted = sorted(idle_periods, key=lambda x: x[2], reverse=True)  # highest procrastination first

        schedule_pairs = []
        i = 0


        # ✅ Send to AI for natural timetable formatting
        prompt = f"""
        You are generating a timetable to avoid procrastination periods.

        Here are the tasks (title, duration in minutes, completed status):
        {tasks}

        Here are known procrastination windows with scores:
        (start_time, stop_time, procrastination_score, idle_minutes)
        {idle_periods}

        Rules:
        - Day starts at 9:00 AM and ends before 8:00 PM
        - If procrastination score is high at a time, avoid placing long tasks there
        - Long tasks → low procrastination periods
        - Short tasks → can be placed during high procrastination scores
        - Do NOT fit tasks exactly inside idle windows; idle windows only indicate distraction level
        - Output timetable as points sequentially like task name/break - duration - time in proper format
        - Fill day sequentially with realistic breaks
        - do not start the day with a break
        - if tasks are completed do not add in the timetable
        - say free for the day if all tasks are done and finish it
        give only the timetable and not any explanation

        Now create the best productivity timetable.
        """

        model = genai.GenerativeModel("models/gemini-2.5-flash")
        response = model.generate_content(prompt)

        return jsonify({
            "ai_generated_timetable": response.text
        })

    except Exception as e:
        return jsonify({"error": str(e)})

@app.route("/")
def home():
    return jsonify({"message": "AI Procrastination-aware Scheduler ✅ Running"})

if __name__ == '__main__':
    init_db()  # ensure table exists before running
    app.run(debug=True, port=5000)





