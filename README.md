<div align="center">

# CVForge

**Build your CV. Find your fit. Land the internship.**

CVForge is an Android app where students build a CV, discover internships and track their applications.
Companies, teachers and alumni post internships, endorse skills and manage applicants.

Android app (Java) · Course project for CSE-2200 Advanced Programming Laboratory, KUET

</div>

## Table of contents

1. [Overview](#overview)
2. [Features](#features)
3. [Screenshots](#screenshots)
4. [Tech stack](#tech-stack)
5. [Architecture](#architecture)
6. [Android components](#android-components)
7. [Networking](#networking)
8. [Firebase setup](#firebase-setup)
9. [How to run](#how-to-run)
10. [Project structure](#project-structure)
11. [Limitations and future work](#limitations-and-future-work)
12. [Author](#author)

## Overview

Finding a first internship is hard. Students send the same CV everywhere, never hear back, and lose track of what they applied for.
Posters have the opposite problem: many applicants, fake posts, and no easy way to see who fits.

CVForge puts both sides in one app. A student builds a CV with a live preview, sees how well it matches each internship,
swipes through jobs, and follows every application on a Kanban board. Companies, teachers and alumni post internships, review
applicants, plan interviews and talk to students in chat. An admin verifies Company and Alumni accounts and reviews suspicious posts.

## Features

### Student
- CV builder with three templates (Modern, Classic, Minimal), live preview, several CV versions, QR code, PDF export and sharing
- Import projects from a GitHub account
- Profile strength ring that tells what to improve
- Discover internships from CVForge posts and from outside job feeds, with search, filters, swipe cards and bookmarks
- Match Radar and match ring: how well the CV fits the skills an internship asks for
- Apply with one tap, choose which CV version to send
- Kanban tracker: drag a card between Applied, Shortlisted, Interview and Offer, private notes, status timeline, live updates
- Reminders for deadlines and interviews, a morning digest, and an interview that can be added to the phone calendar
- AI helpers (Gemini): bullet writer, "Tailor my CV" for one job, skill gap roadmap, interview coach (voice or typing), follow-up email draft
- Verified endorsements from teachers and alumni, mentor slots with alumni, badges and a day streak

### Company
- Dashboard with numbers and the latest applicants, a list of own postings with applicant counts, and a profile tab
- Post, edit, close and delete internships (with scam rule checks)
- Applicants list with match ring and verified skills: shortlist, plan an interview, make an offer or reject

### Teacher
- Post internships and manage applicants
- Dashboard with numbers on own posts, and endorsement requests from students (approve a skill with a comment)

### Alumni
- Post opportunities and manage applicants
- Endorse skills and add 30-minute mentor slots that students can book

### Admin
- Approve Company and Alumni accounts (Verified badge)
- Review flagged posts: rule reasons, an optional AI second opinion (SAFE, SUSPICIOUS or SCAM), then Keep or Remove

### For all roles
- Profile photos (tap the avatar to change it)
- Chat between student and company/teacher (per application) and between student and alumni (per booked slot), with typing dots and unread badges
- Chat message notifications while the app is running
- Scam post alert: rule checks when a post is created, flagged posts go to the admin
- Ghosting detector: an application with no reply for 7 days shows a nudge and a follow-up email draft
- Offline banner and cached data (saved jobs and CVs open without internet)

## Screenshots

<table>
  <tr>
    <td align="center"><img src="screenshots/app/home.jpg" width="200" alt="Home"><br><sub>Home: Today's Focus</sub></td>
    <td align="center"><img src="screenshots/app/cv-builder.jpg" width="200" alt="CV Builder"><br><sub>CV Builder with live preview</sub></td>
    <td align="center"><img src="screenshots/app/tracker.jpg" width="200" alt="Tracker"><br><sub>Kanban application tracker</sub></td>
  </tr>
  <tr>
    <td align="center"><img src="screenshots/app/profile.jpg" width="200" alt="Profile"><br><sub>Student profile</sub></td>
    <td align="center"><img src="screenshots/app/request-endorsement.jpg" width="200" alt="Request endorsement"><br><sub>Request an endorsement</sub></td>
    <td align="center"><img src="screenshots/app/interview-coach.jpg" width="200" alt="Interview coach"><br><sub>AI interview coach</sub></td>
  </tr>
  <tr>
    <td align="center"><img src="screenshots/app/company-dashboard.jpg" width="200" alt="Company dashboard"><br><sub>Company dashboard</sub></td>
    <td align="center"><img src="screenshots/app/company-applicants.jpg" width="200" alt="Company applicants"><br><sub>Company applicants</sub></td>
    <td align="center"><img src="screenshots/app/chat.jpg" width="200" alt="Chat"><br><sub>Application chat</sub></td>
  </tr>
</table>

## Tech stack

| Area | Used |
|------|------|
| Language | Java (no Kotlin) |
| UI | XML layouts, View Binding, reusable `<include>` layouts, Material Components |
| Lists | RecyclerView with adapters and view holders (two or three view types in the chat and the coach) |
| Custom views | `SwipeCardView`, `MatchRadarView`, `ProfileStrengthRing`, `CvPreviewView`, `BottomNavView`, `ConfettiView`, `CircleDecorationView` |
| Local data | Room (CVs, bookmarks, cached jobs), SharedPreferences |
| Networking | Retrofit + Gson, OkHttp logging, `HttpURLConnection` + `XmlPullParser` for the RSS feed |
| Background work | Started Service, WorkManager (reminders, digest, ghosting), AlarmManager (exact reminders) |
| Firebase | Authentication (email/password and Google), Cloud Firestore, Cloud Messaging, Firebase AI Logic, App Check |
| AI | Gemini through Firebase AI Logic, model `gemini-3.5-flash-lite` (`AiRepository.MODEL_NAME`) |
| Other | ZXing (QR code), Android `PdfDocument`, `SpeechRecognizer`, `CalendarContract`, Credential Manager (Google sign-in) |
| Android versions | `minSdk 23`, `targetSdk 32`, `compileSdk 34` |
| Tests | JUnit unit tests (46) |

## Architecture

![CVForge architecture](screenshots/architecture.png)

The app has three simple layers:

- **UI layer** (`ui`): Activities, Fragments, adapters and custom views. Screens only show data and react to taps.
- **Repository layer** (`data`): one repository per topic (users, CVs, internships, applications, chat, endorsements, mentor slots, jobs).
  Screens talk to repositories and never to Firestore or Room directly.
- **Data sources**: remote (Cloud Firestore, Firebase Auth, Retrofit APIs, the RSS feed, Gemini) and local (Room and SharedPreferences).
  Jobs and CVs are cached in Room so the app still shows data offline.

Services, receivers and workers (`service`, `receiver`) run in the background and use the same repositories.

## Android components

| Type | Component | What it does |
|------|-----------|--------------|
| Service (started) | `JobSyncService` | Loads jobs from the outside feeds into Room, logs its lifecycle, sends a broadcast when it is done |
| Service (Firebase) | `CvForgeMessagingService` | Receives push messages and saves the FCM token |
| Receiver (manifest) | `BootReceiver` | After a restart or an app update, sets the reminder alarms again |
| Receiver (manifest) | `ReminderReceiver` | Shows the interview or deadline notification when an alarm fires |
| Receiver (in code) | `NetworkReceiver` | Shows the offline banner on the student screen |
| Receiver (in code) | jobs-synced receiver in `DiscoverFragment` | Refreshes the job list when `JobSyncService` finishes |
| Workers | `ReminderWorker`, `DigestWorker`, `GhostingWorker` | Reminders every 6 hours, a daily morning digest, a daily ghosting check |
| ContentProvider (own) | `CvProvider` | Gives access to bookmarks and CV drafts (`bookmarks` and `cv_drafts` paths) |
| FileProvider | `androidx.core.content.FileProvider` | Shares the exported CV PDF with other apps |
| Other providers used | `CalendarContract`, `MediaStore` | Add an interview to the calendar, read the photo orientation |

Permissions:

| Permission | Why |
|------------|-----|
| `INTERNET` | Firebase, job APIs, GitHub, RSS feed |
| `ACCESS_NETWORK_STATE` | Offline banner |
| `POST_NOTIFICATIONS` | Notifications (asked at runtime on Android 13+) |
| `SCHEDULE_EXACT_ALARM` | Interview and deadline reminders at the exact time |
| `RECEIVE_BOOT_COMPLETED` | Set reminders again after a restart |
| `READ_CALENDAR`, `WRITE_CALENDAR` | Add an interview to the phone calendar (asked at runtime) |
| `RECORD_AUDIO` | Speak the answer in the interview coach (asked at runtime, typing also works) |

## Networking

| API or feed | Method and URL | Used in |
|-------------|----------------|---------|
| Remotive | `GET https://remotive.com/api/remote-jobs` (search `intern`) | Discover |
| Arbeitnow | `GET https://www.arbeitnow.com/api/job-board-api` | Discover |
| We Work Remotely (RSS, parsed with `XmlPullParser`) | `GET https://weworkremotely.com/categories/remote-programming-jobs.rss` | Discover |
| GitHub | `GET https://api.github.com/users/{user}/repos` | Import projects in the CV editor |

None of these four needs an API key in the code. The GitHub API is limited to 60 requests per hour without a key.
Gemini is not called with a key from the app: it goes through Firebase AI Logic.

## Firebase setup

**Authentication:** email/password and Google sign-in.

![Authentication providers](screenshots/firebase/auth-providers.jpg)

**Cloud Firestore collections**

| Collection | Purpose |
|------------|---------|
| `users` | Name, role, verified flag, FCM token |
| `profilePhotos` | Profile photo of each user |
| `internships` | Posts, with scam flag and reasons |
| `applications` | One per student and internship: status, status history, CV copy, interview time |
| `applicationNotes` | Private notes of the student |
| `cvs` | CV versions of each student |
| `endorsements` | Skill endorsement requests and approvals |
| `mentorSlots` | Alumni slots and bookings |
| `chats` (with `messages` sub-collection) | Conversations and messages |

![Firestore data](screenshots/firebase/firestore-data.jpg)

Profile photos are saved as Base64 text in Firestore. Firebase Storage is not used. The Realtime Database is not used either
(there is no `FirebaseDatabase` in the code).

![Firestore rules](screenshots/firebase/firestore-rules.jpg)

**Firebase AI Logic** is used for the bullet writer, Tailor my CV, the roadmap, the interview coach, follow-up emails and the
scam second opinion. **App Check** protects these calls (Play Integrity in release builds, a debug provider in debug builds).

![AI Logic monitoring](screenshots/firebase/ai-logic-monitoring.jpg)

![App Check metrics](screenshots/firebase/app-check-metrics.jpg)

## How to run

1. Clone the repository and open it in Android Studio.
2. Create your own Firebase project and register the Android app with the id `com.example.cvforge`.
3. Download `google-services.json` and put it in the `app/` folder. It is not in the repository, because it belongs to one Firebase project.
4. In the Firebase console turn on:
   - **Authentication**: Email/Password and Google
   - **Cloud Firestore** (create the database, then publish rules that allow the collections above)
   - **Firebase AI Logic** (Gemini Developer API)
   - **App Check**: register the app. For debug builds run the app once, copy the debug token shown in Logcat and add it under *Manage debug tokens*.
5. For Google sign-in add the **SHA-1 and SHA-256** of your debug keystore to the Android app in Firebase, then download `google-services.json` again.
6. Build and run on a device or emulator (Android 6.0 or newer).
7. To try the admin screen, set `role` to `admin` by hand on one document of the `users` collection.

Push notifications from the server need Cloud Functions, which need the paid Firebase plan. The app does not rely on them:
the `functions/` folder has two written functions (status change and chat message), but they are not deployed.
Without them, reminders, the morning digest and in-app chat notifications still work, only server push does not.

## Project structure

```
app/src/main/java/com/example/cvforge/
├── ai/         Gemini calls and prompts (bullets, tailor CV, roadmap, coach, scam review)
├── data/       Repositories, models, Room database (local) and models (model)
├── network/    Retrofit APIs, RSS parser and loader, job mapper
├── provider/   CvProvider (own ContentProvider) and its contract
├── receiver/   BootReceiver, ReminderReceiver, NetworkReceiver
├── service/    JobSyncService, FCM service, workers, chat notifier
├── ui/         Screens, one package per feature
│   ├── admin, alumni, auth, company, teacher   (one per role and sign-in)
│   ├── student, cv, discover, tracker          (student tabs and CV editor)
│   ├── post, profile, chat, coach              (postings, endorsements, mentors, chat, interview coach)
│   └── view/   Custom views
└── util/       Helpers: PDF, QR, match score, scam rules, reminders, profile photo
```

## Limitations and future work

- The QR code points to a public profile page (`cvforge-93934.web.app/u/<uid>`) that does not exist yet. The QR code is created, but the page is not.
- Server push notifications need Cloud Functions and the paid plan, so they are written but not deployed.
- Profile photos are small Base64 images in Firestore. Firebase Storage would be better for large images.
- There are unit tests for the rules, parsers and planners, but no automatic UI tests yet.
- The app is not published on Google Play, and the Play Integrity check is only active in release builds.
- Future ideas: a public profile web page, resume parsing, more feed sources, and a tablet layout.

## Author

**Nishat Tasin Shreya**
Roll 2307035 · Batch 2K22
Department of Computer Science and Engineering
Khulna University of Engineering & Technology (KUET)

Course teachers:
- Safin Ahmmed, Assistant Professor
- Ehsanul Karim Talha, Lecturer
