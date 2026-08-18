<p align="center">
  <img src="docs/logo.png" width="120" alt="NextClass logo">
</p>

<h1 align="center">NextClass</h1>

**Your college timetable and next-class room, right on your home screen. Fully offline.**

NextClass is a tiny, private Android app for university students. It shows the
class you're in now (or the one coming up) — course, room, and time — on the app
and in a home-screen **widget** that flips to the next class the moment the
current one ends. Add CA/assignment/exam deadlines, mark a class cancelled when a
teacher is absent, and share your whole week with a classmate as a single code.

> It runs entirely on your phone. It does **not** connect to any university
> server, needs no login, and asks for no account. Your timetable never leaves
> your device.

---

## ✨ Features

- **Now / Next hero** — the current or upcoming class, with the room called out big.
- **Home-screen widget** — flips to the next class exactly when the current one ends; shows your nearest deadline as a chip.
- **Editable timetable** — add, edit, or delete classes; timings, room, group, type.
- **Cancel a class for today** — teacher absent? Toggle it off just for today; the widget skips it.
- **Deadlines calendar** — track CAs, assignments, quizzes, and exams with a countdown.
- **Share codes** — one person builds the section's timetable once; everyone else imports it in seconds.

## 📲 Install

NextClass isn't on the Play Store — you sideload the APK.

1. Download the latest **`NextClass.apk`** from the [Releases](../../releases) page.
2. Open it. Android will ask to allow installing from this source — allow it.
3. Open the app and set up your timetable (below).

**On Honor / Xiaomi / Oppo / Vivo phones**, the aggressive battery manager can
freeze background apps and stop the widget updating on time. For a reliable
widget:

- **Recent apps → long-press NextClass → Lock** (so it isn't auto-cleaned).
- **Settings → Apps → NextClass → Battery → No restrictions / allow auto-launch.**

## 🗓️ Setting up your timetable

On first launch you'll be asked how to start:

**Option A — Paste a share code (fastest).**
Everyone in the same section has the same classes, so one person builds it and
shares a code. Get the code from a classmate, then **Paste a share code** → paste → done.

**Option B — Build it yourself.**
Choose **Start with an empty timetable**, then tap **+ Add class** for each slot
(day, start/end time, course code, room like `28-506`, type, group).

To share yours later: **≡ (top-right) → Share my timetable → Copy / Share…**

> Rooms read *building-room*: `28-506` = building 28, 5th floor, room 06 — the way
> everyone on campus says it.

<details>
<summary>📋 Sample timetable (try the import flow)</summary>

Copy everything on the line below, open NextClass → **Paste a share code**, and paste it:

```
NCT1-eyJ2IjoxLCJzbG90cyI6W3siZCI6MSwicyI6NTYwLCJlIjo2MTAsInQiOiJMZWN0dXJlIiwiZyI6Ikc6QWxsIiwiYyI6IkNTRTEwMSIsInIiOiIxMC0yMDEifSx7ImQiOjEsInMiOjYxMCwiZSI6NjYwLCJ0IjoiTGVjdHVyZSIsImciOiJHOkFsbCIsImMiOiJNQVQyMDEiLCJyIjoiMTAtMjAxIn0seyJkIjoxLCJzIjo3NjAsImUiOjgxMCwidCI6IlByYWN0aWNhbCIsImciOiJHOjEiLCJjIjoiQ1NFMTAxIiwiciI6IjEyLTEwNSJ9LHsiZCI6MSwicyI6ODEwLCJlIjo4NjAsInQiOiJQcmFjdGljYWwiLCJnIjoiRzoxIiwiYyI6IkNTRTEwMSIsInIiOiIxMi0xMDUifSx7ImQiOjIsInMiOjU2MCwiZSI6NjEwLCJ0IjoiTGVjdHVyZSIsImciOiJHOkFsbCIsImMiOiJQSFkxMDEiLCJyIjoiMTEtMzAxIn0seyJkIjoyLCJzIjo2NjAsImUiOjcxMCwidCI6IkxlY3R1cmUiLCJnIjoiRzpBbGwiLCJjIjoiTUFUMjAxIiwiciI6IjExLTMwMSJ9LHsiZCI6MiwicyI6NzYwLCJlIjo4MTAsInQiOiJMZWN0dXJlIiwiZyI6Ikc6QWxsIiwiYyI6IkNTRTIwMiIsInIiOiIxMC0yMDQifSx7ImQiOjMsInMiOjYxMCwiZSI6NjYwLCJ0IjoiTGVjdHVyZSIsImciOiJHOkFsbCIsImMiOiJFTkcxMDEiLCJyIjoiMDktMTEwIn0seyJkIjozLCJzIjo3NjAsImUiOjgxMCwidCI6IlByYWN0aWNhbCIsImciOiJHOjAiLCJjIjoiUEhZMTAxIiwiciI6IjEyLTEwOCJ9LHsiZCI6MywicyI6ODEwLCJlIjo4NjAsInQiOiJQcmFjdGljYWwiLCJnIjoiRzowIiwiYyI6IlBIWTEwMSIsInIiOiIxMi0xMDgifSx7ImQiOjQsInMiOjU2MCwiZSI6NjEwLCJ0IjoiTGVjdHVyZSIsImciOiJHOkFsbCIsImMiOiJDU0UyMDIiLCJyIjoiMTAtMjA0In0seyJkIjo0LCJzIjo2NjAsImUiOjcxMCwidCI6IkxlY3R1cmUiLCJnIjoiRzpBbGwiLCJjIjoiTUFUMjAxIiwiciI6IjExLTMwMSJ9LHsiZCI6NCwicyI6ODYwLCJlIjo5MTAsInQiOiJMZWN0dXJlIiwiZyI6Ikc6QWxsIiwiYyI6IkNTRTEwMSIsInIiOiIxMC0yMDEifSx7ImQiOjUsInMiOjcxMCwiZSI6NzYwLCJ0IjoiTGVjdHVyZSIsImciOiJHOkFsbCIsImMiOiJFTkcxMDEiLCJyIjoiMDktMTEwIn0seyJkIjo1LCJzIjo4MTAsImUiOjg2MCwidCI6IlByYWN0aWNhbCIsImciOiJHOjEiLCJjIjoiQ1NFMjAyIiwiciI6IjEyLTEwNSJ9XX0=
```

</details>

## 🧩 Adding the widget

Long-press an empty spot on your home screen → **Widgets** → find **NextClass** →
drag **Next Class** onto the screen. Tap it any time to open the app.

## 🔒 Privacy

- Your data stays on your phone. The **only** network use is a once-a-day check to GitHub for a new app version — never your timetable.
- No university servers, no login, no analytics, no accounts.
- Everything is stored in a single JSON file in the app's private storage.

## 🛠️ Build from source

```bash
git clone https://github.com/bijo-ai/nextclass.git
cd nextclass
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Requires the Android SDK (compileSdk 36) and a JDK 17+. No third-party
libraries — framework only.

## 🚧 Roadmap

- [ ] Fast grid editor (tap a cell instead of a dialog per class)
- [ ] Screenshot import (on-device OCR, experimental)
- [ ] Course-code → friendly-name labels
- [ ] More widget sizes / themes

## 🙋 Made by Bijo

Built by **Bijo K Varghese**, a B.Tech AI/ML student.

- GitHub: [github.com/bijo-ai](https://github.com/bijo-ai)
- LinkedIn: [Bijo K Varghese](https://www.linkedin.com/in/bijo-k-varghese-9bba32319)

Found it useful, or want a feature? Open an [issue](../../issues) or say hi.

## 📄 License

[MIT](LICENSE) — free to use, modify, and share.
