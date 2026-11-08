# Trackable — Personal Nutrition & Health Tracker

<div align="center">
<img width="1200" height="400" alt="Trackable Banner" src="https://via.placeholder.com/1200x400?text=Trackable+Nutrition+Tracker" />
</div>

> **Trackable** is a privacy-first, offline-capable nutrition tracking app for Android. Log what you eat, track water intake, monitor weight, and get AI-powered food parsing — all with your data staying on your device.

## ✨ Features

| Feature | Description |
|---------|-------------|
| 🍎 **Food Logging** | Log meals with AI-powered parsing. Supports typos, broken input, and partial matches (e.g. `"50g paner"`, `"milkk 50ml"`) |
| 💧 **Water Intake** | Track water with 2 configurable cup sizes (set in Settings) |
| ⚖️ **Weight Tracker** | Log and monitor your weight over time |
| 🎯 **Nutritional Targets** | Calorie, protein, carbs, and fat goals auto-calculated using Mifflin-St Jeor equation |
| 👤 **Personal Profile** | Name, age, sex, weight, target weight, height, activity level |
| 🤖 **AI Providers** | Gemini 3.6-flash, NVIDIA NIM (Minimax M3.1 Flash), or 100% Local engine (offline) |
| 🔐 **API Key Validation** | Keys are tested against the provider before saving; falls back to Local if offline |
| 📱 **Onboarding** | 2-step setup: personal profile → nutritional targets (optional "Set Later") |

## 📱 Screenshots

<div align="center">
<table>
<tr>
<td><b>Dashboard</b></td>
<td><b>Onboarding</b></td>
<td><b>Food Logging</b></td>
</tr>
<tr>
<td>[IMAGE: dashboard screenshot]</td>
<td>[IMAGE: onboarding screen]</td>
<td>[IMAGE: food log screen]</td>
</tr>
<tr>
<td><b>Water Intake</b></td>
<td><b>Settings</b></td>
<td><b>Weight Tracker</b></td>
</tr>
<tr>
<td>[IMAGE: water intake screen]</td>
<td>[IMAGE: settings screen]</td>
<td>[IMAGE: weight tracker screen]</td>
</tr>
</table>
</div>

> Replace the `[IMAGE: ...]` placeholders above with actual screenshots from your device/emulator before publishing.

## 🚀 Getting Started

### Prerequisites

- Android Studio (latest stable)
- Android SDK 24+ (minSdk 24, targetSdk 36)
- An Android device or emulator

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/YOUR_USERNAME/trackable.git
   cd trackable
   ```

2. **Open in Android Studio**
   - Open Android Studio → **Open** → select this project directory
   - Wait for Gradle sync to complete

3. **Configure API Keys (optional)**
   - Create a `.env` file in the project root (see `.env.example`)
   - Add your API key:
     ```
     GEMINI_API_KEY=your_gemini_api_key_here
     NVIDIA_API_KEY=your_nvidia_api_key_here
     ```
   - Or enter keys directly in the app's **Settings → AI Model & API Key**

4. **Build & Run**
   - Select your device/emulator
   - Click **Run** (▶️)

## 🧠 How It Works

### Onboarding (First Launch)
1. **Personal Profile** — Enter name, age, sex, weight, target weight, height, activity level (all required)
2. **Nutritional Targets** — Auto-calculated using your profile:
   - **BMR** via Mifflin-St Jeor equation (sex-specific)
   - **TDEE** via activity multiplier
   - **Goal**: Gain / Loss / Maintenance (1% threshold)
   - **Protein / Fat / Carbs** based on target calories

   You can tap **"Set Later"** to use default values and adjust in Settings later.

### Food Logging
- Type what you ate (e.g. `"50g paneer"`, `"milkk 50ml"`, `"paneer 50 g"`, `"milk, paneer 50,60"`)
- The app is typo-tolerant — it fixes spelling, joins split units, and parses comma-separated items
- Works offline with the Local engine, or online with Gemini/NVIDIA

### Water Intake
- Set your two preferred cup sizes in **Settings → Water Cup Sizes**
- Quick-add those amounts from the dashboard

## ⚙️ Settings

| Section | What you can configure |
|---------|----------------------|
| **AI Model & API Key** | Choose provider (Gemini / NVIDIA / Local), enter key, test before saving |
| **Daily Nutritional Targets** | Calories, protein, carbs, fat, water goal — with autofill from profile |
| **Water Cup Sizes** | Two quick-add cup sizes for Water Intake |
| **Tracking Start Date** | Enable/disable date-based tracking, block dates before/after |
| **Personal Profile** | Weight, target weight, height, age, activity level |

## 🔐 Privacy

- All data stays **on your device** (Room database)
- No cloud sync or user accounts
- API keys are stored locally and only used for AI parsing
- **Local engine** requires no internet at all

## 🛠️ Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose (Material 3)
- **Database**: Room (SQLite)
- **Dependency Injection**: Manual (ViewModels + Repository)
- **Networking**: OkHttp
- **JSON**: Moshi
- **Build**: Gradle (Kotlin DSL)

## 📄 License

MIT License — see [LICENSE](LICENSE) for details.

## 🤝 Contributing

1. Fork the repo
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

<div align="center">
Made with ❤️ for personal nutrition tracking
</div>
