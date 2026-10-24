# LifePilot — AI-Powered Task Assistant

**LifePilot** is a personal Android project exploring how generative AI can make everyday task management more useful. Users can manage tasks and use AI to turn natural-language requests into structured tasks or suggest an order for their day.

Built with **Kotlin**, **Jetpack Compose**, and a layered architecture, LifePilot is also a hands-on exploration of reliable AI integration, reactive UI state, and automated testing.

> **Project status:** Active development. This README distinguishes implemented functionality from ideas on the roadmap.

## Screen recording

![AI Task Creation](images/ai-task-creation.gif)

## Features Implemented

### Task management
- Create, view, edit, and delete tasks.
- Mark tasks as completed.
- Reorder tasks and maintain their positions.
- Set task details such as title, due date, priority, and category.
- Validate required task information and display UI feedback for operations and errors.

### AI-assisted task creation
- Parse natural-language input into structured task suggestions using **Google Gemini**.
- Review and work with parsed task data.
- Handle malformed or incomplete AI responses and API errors.

### Plan My Day
- Generate AI-assisted ordering suggestions for pending tasks.
- Display the suggested order and the AI's reasoning.
- Apply the suggested order to tasks while keeping completed tasks separate.
- Reject incomplete, duplicated, unknown, or invalid suggestion data.
- Detect task changes while a plan is being generated, or before it is applied, avoiding stale results.
- Prevent duplicate generation and application requests.
- Handle cancellation and failures without treating coroutine cancellation as an ordinary UI error.

## Tech Stack

| Area                        | Technologies                             |
|-----------------------------|------------------------------------------|
| Language                    | Kotlin                                   |
| UI                          | Jetpack Compose, Material 3              |
| Architecture                | MVVM, Clean Architecture principles      |
| Asynchronous work           | Kotlin Coroutines, Flow / StateFlow      |
| Dependency injection        | Hilt                                     |
| AI integration              | Google Gemini                            |
| Networking / serialization  | Retrofit, JSON / Gson                    |
| Testing                     | JUnit, Mockito, kotlinx-coroutines-test  |

> The codebase also explores Android persistence technologies. Check the current repository implementation before describing a particular database or offline behavior as fully shipped.

## Architecture

LifePilot separates UI concerns from business rules and data integrations:

```text
Jetpack Compose UI
       |
       v
ViewModels + UiState / UiEvent
       |
       v
Domain Use Cases + Models
       |
       v
Repositories / AI Planner
       |
       v
Local data sources / Gemini API
```

This structure keeps task validation and AI-planning rules testable independently of Android UI components.

## Testing & Reliability

The project includes unit tests for task operations, AI planning, and ViewModel state transitions. Examples of covered behavior include:

- Successful AI planning, loading states, and error states.
- Validation of task IDs and consecutive suggested orders.
- API retry behavior and handling of malformed responses.
- Discarding AI suggestions when tasks change during generation.
- Preventing duplicate requests, including calls made before a coroutine starts.
- Correct coroutine cancellation handling.
- Applying task order and emitting completion events.

Run local unit tests with:

```bash
./gradlew :app:testDebugUnitTest
```

## Getting Started

1. Clone the repository:
   ```bash
   git clone https://github.com/danielvilha/kotlin-life-pilot.git
   ```
2. Open the project in **Android Studio**.
3. Sync Gradle and select an Android emulator or connected device.
4. Configure the Gemini API credentials according to the project's local configuration. **Do not commit API keys or secrets.**
5. Build and run the app.

> The exact API-key setup steps should be documented after verifying the repository's current configuration.

## Roadmap

These are **planned or exploratory** improvements, not claims of completed features:

- [ ] Add real screenshots and a short demo of the current app.
- [ ] Improve offline persistence and synchronization behavior.
- [ ] Explore voice input for task creation.
- [ ] Explore image/document understanding for capturing tasks.
- [ ] Expand AI suggestions with contextual next actions.
- [ ] Improve accessibility, UX polish, and UI test coverage.
- [ ] Add observability and release-quality documentation.

## Motivation

LifePilot is a portfolio and learning project focused on combining production-style Android engineering practices with practical generative AI features. The goal is not simply to add a chatbot to a to-do list, but to build useful task workflows with predictable state, validation, and failure handling.

## Author

**Daniel Vilha**  
[GitHub](https://github.com/danielvilha)

## License

Copyright © 2026 Daniel Vilha

This project is licensed under the MIT License.

See the [LICENSE](LICENSE) file for details.