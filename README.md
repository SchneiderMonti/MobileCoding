# 🔐 AccessGate

**AccessGate** is a university project that explores alternative and playful ways of authenticating users beyond traditional login methods.  
Instead of focusing on real-world security, the app experiments with interaction-based authentication such as tap rhythms, device movements, PIN input, and biometric prompts.

The goal of the project is to showcase **UX design, state management, and Android architecture** using **Jetpack Compose, ViewModel, and Room**, while also investigating how users remember and reproduce non-standard authentication patterns.

---

## ✨ Features

- **Multiple Authentication Methods**
  - Tap rhythm (“Tap Jingle”)
  - PIN code
  - Fingerprint (Android biometric prompt)
  - Device movement patterns (“Flip Pattern”)

- **Wizard-Based Enrollment**
  - Step-by-step flow for creating authentication methods
  - Naming and hint system to help users remember their patterns

- **Authentication Gate**
  - Users must successfully authenticate before accessing details
  - Automatic navigation on successful authentication

- **Local Persistence**
  - Authentication data stored locally using Room
  - Simple CRUD operations for entries

---

## ⚠ Disclaimer

This application is **not intended to be a real-world security solution**.  
It is a **proof-of-concept and UI/UX showcase**, designed to explore fun and unconventional authentication ideas rather than replace established security practices.  
Certain implementations (e.g., storing PINs without hashing) were deliberately kept simple for demonstration purposes.

---

## 🛠 Tech Stack

- **Kotlin**
- **Jetpack Compose**
- **ViewModel & StateFlow**
- **Room Database**
- **AndroidX Biometric API**

---

## 📚 Project Context

This project was developed as part of a university course to explore:
- Interaction-driven authentication concepts
- User experience design in security-related interfaces
- Clean Android architecture using modern libraries

The focus is on **conceptual design, usability, and technical structure**, rather than production-level security.

---

## 🚀 How to Run

1. Clone the repository  
2. Open the project in **Android Studio**
3. Sync Gradle and run on an emulator or physical device (Android 8+ recommended)
(Running on the emalutar restricts you of trying the sensor authentications: Tap Jingle, Flip Pattern, Biometric Scans)

---

