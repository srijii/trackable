# Changelog

All notable changes to Trackable will be documented in this file.

## [1.0.0] - 2026-11-08

### Added
- Onboarding flow (2-step: personal profile + nutritional targets)
- Personal profile: name, age, sex, weight, target weight, height, activity level
- Nutritional targets: calories, protein, carbs, fat, water goal
- Mifflin-St Jeor calculation engine (BMR, TDEE, goal-based adjustments)
- Water intake tracking with 2 configurable cup sizes
- Weight tracker
- Food inventory management
- Food logging with AI-powered parsing
- AI providers: Gemini 3.6-flash, NVIDIA NIM (Minimax M3.1 Flash), Local engine (offline)
- API key validation before saving
- Typo-tolerant food parsing (Levenshtein fuzzy matching)
- Dashboard warning banner for incomplete profile
- Two save buttons in Settings (Save API / Save General)
- Autofill button for nutritional targets
- Unsaved changes warning when navigating away
- No default foods on install (empty inventory, preserves existing foods)
- Start date tracking enabled by default (set to onboarding date)

### Technical
- Jetpack Compose (Material 3) UI
- Room database for local storage
- OkHttp for AI API communication
- Kotlin DSL Gradle build
- GitHub Actions CI workflow

## [Unreleased]

### Planned
- Food barcode scanning
- Meal planning
- Export data (CSV/JSON)
- Dark mode theme toggle
- More AI model options
