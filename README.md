# Croniot

**An IoT framework that connects devices to your smartphone — monitor sensors and trigger tasks — instantly or on a schedule, without rebuilding the stack for every project. Built with Kotlin Multiplatform (server + Android) and C++ (ESP32).**

Croniot eliminates the repetitive infrastructure of IoT projects. You define your sensors and tasks on the device; the server and the Android app adapt automatically.

---

## Architecture

```
┌──────────────┐       MQTT / HTTP       ┌──────────────┐     HTTP / MQTT   ┌──────────────┐
│   IoT Device │ ◄─────────────────────► │    Server    │ ◄───────────────► │  Android App │
│   (ESP32)    │                         │   (Ktor)     │                   │  (Compose)   │
└──────────────┘                         └──────┬───────┘                   └──────────────┘
                                                │
                                         ┌──────┴────────┐
                                         │  PostgreSQL   │
                                         │  + MQTT Broker│
                                         └───────────────┘
```

| Component | Tech |
|-----------|------|
| **Android app** | Kotlin · Jetpack Compose · MVI · Coroutines & Flow · Koin · Room |
| **Server** | Ktor · Coroutines · Dagger · jOOQ · Docker Compose · PostgreSQL · MQTT |
| **Shared (KMP)** | Domain models · DTOs · Validation logic — single source of truth across client & server |
| **IoT device** | C++ · ESP32 · MQTT · HTTP · [croniot-iot repo →](https://github.com/vladimir-ivanov-info/croniot-iot) |
| **Infrastructure** | Docker Compose · GitHub Actions CI/CD (automated AAB signing & Play Store release) |

## Key technical decisions

- **Kotlin Multiplatform (KMP):** Domain models and DTOs live in `shared/commonMain`, ensuring end-to-end type consistency between client and server. A change to a DTO breaks at compile time, not at runtime.
- **Self-describing devices:** Each IoT device registers its own sensors and tasks (with metadata: min/max, data type, units). The app renders UI dynamically — no hardcoded screens per device.
- **Clean Architecture + MVI:** Layered separation (data → domain → presentation) with unidirectional data flow.
- **Docker Compose orchestration:** One command (`docker compose up`) starts PostgreSQL.

## What Croniot gives you out of the box

The next time you start an IoT project, **99 % of the work is already done:**

| You skip | You focus on | Or just reuse |
|----------|-------------|---------------|
| Building a server (data storage, auth, MQTT routing) | Sensor logic: extract and format your hardware data | Built-in sensors like `WiFiSensor` — a few lines of code |
| Developing a mobile app (UI, networking, state) | Task logic: define what happens when you trigger a task remotely | Built-in tasks like `Water plants` — configure and go |
| Writing WiFi/MQTT/HTTP boilerplate on the device | Your project-specific business logic | Pre-made device templates for common setups |

## App walkthrough

<!-- Recommended: 4–5 screenshots max. Show the core flow. -->

| Login | Your devices | Device's sensors
|-------|-------------|---------------|
| <img src="https://github.com/user-attachments/assets/ea270666-e5f2-4916-bacb-aed2ad865535" width="320"> | <img src="https://github.com/user-attachments/assets/cd27be2e-1d4f-46ed-a95f-bbec3fe6605d" width="320"> | <img src="https://github.com/user-attachments/assets/d358e151-9356-4e53-bb6c-8f6e5ca79828" width="320"> |

| Device's task types | Scheduled task | Stateful task
|-------|-------------|---------------|
| <img src="https://github.com/user-attachments/assets/c3ffa9cd-32e9-45f0-9d6f-8a593c10ac07" width="320"> | <img src="https://github.com/user-attachments/assets/84161598-574b-4171-a8ba-ec5840636a6f" width="320"> | <img src="https://github.com/user-attachments/assets/44c3d697-619d-4d16-856c-4f63870e46c7" width="320"> |

1. **Register / Log in** — standard auth flow.
2. **Device list** — shows all IoT devices linked to your account.
3. **Sensors tab** — live graphs rendered from device-reported metadata (type, range, units).
4. **Task types tab** — select a task the device supports, configure parameters, and send it.

## Quick start

### Prerequisites
- Ubuntu (or you can adapt the commands to your OS)
- Android Studio (Panda)
- An ESP32 board, PlatformIO and ESP IDF (for the IoT side)

### Run the server

#### 1. Configure global variables
##### Warning: these passwords are for testing. Generate your own keystore and use a different database password and name.
```bash
nano ~/.basrc

Add this to the end of the file:

export CRONIOT_MQTT_BROKER_URL="tcp://localhost:1883"<br>
export CRONIOT_MQTT_CLIENT_ID="croniot-server"<br>  
export CRONIOT_DB_URL="jdbc:postgresql://localhost:5433/iot_testdb"<br>
export CRONIOT_DB_USER="testuser"<br>
export CRONIOT_DB_PASSWORD="testpass"<br>
export CRONIOT_KEYSTORE_PASSWORD="croslslp1Nng"

source ~/.bashrc
```

#### 2. Install, configure and run Mosquitto.

```bash
sudo apt update
sudo apt install -y mosquitto mosquitto-clients
sudo systemctl enable mosquitto
sudo systemctl start mosquitto
sudo nano /etc/mosquitto/mosquitto.conf

Add these 2 lines to the end:
  listener 1883
  allow_anonymous true

sudo systemctl restart mosquitto

sudo ufw allow 1883
sudo ufw reload
```

#### 3. Run PostgreSQL via Docker Compose
```bash
cd server/
docker compose up        # starts PostgreSQL
```
#### 4. Run the server

Install JDK<br>
```bash
sudo apt update && sudo apt install openjdk-21-jdk
```

Open firewall ports
```bash
sudo ufw allow 8090
sudo ufw allow 8443
sudo ufw reload
```

Open the `croniot-kmp` project in Android Studio or IntelliJ Idea.<br>
Go to the server's `build.gradle` and run the `shadorJar` task.<br>
Go to `croniot-kmp/server/build/libs` and you should see the server: `server-1.0.0-all.jar`<br>
Move this jar to the `croniot-kmp/server` folder.

Run `java -jar server-1.0.0-all.jar`

### Run the Android app

Open the project in Android Studio, select the `composeApp` run configuration, and deploy to a device or emulator.

### Set up an IoT device

See the [croniot-watering-system repository](https://github.com/vladimir-ivanov-info/croniot-iot) as an example for wiring, flashing, and configuration instructions.

## Project structure

```
croniot-kmp/
├── composeApp/              # Android app entry point (Jetpack Compose)
│   └── src/
│       ├── commonMain/      # Shared Compose UI logic
│       └── androidMain/     # Android-specific code
├── client/                  # Client-side modules (Clean Architecture)
│   ├── core/                # Core utilities and shared UI components
│   ├── data/                # Data layer (repositories, network, local)
│   ├── domain/              # Domain layer (use cases, models)
│   ├── presentation/        # Presentation layer (ViewModels, state)
│   └── features/            # Feature modules
│       ├── home/
│       ├── login/
│       ├── sensors/
│       └── tasktypes/
├── server/                  # Ktor server application
│   ├── src/main/kotlin/
│   │   ├── application/     # App entry point, Dagger modules
│   │   ├── config/          # Server configuration
│   │   ├── controllers/     # Request handlers
│   │   ├── data/
│   │   │   ├── db/          # jOOQ entities, DAOs, utilities
│   │   │   ├── mappers/     # Entity ↔ domain mappers
│   │   │   └── repositories/
│   │   ├── di/              # Dagger components
│   │   ├── http/            # HTTP route definitions
│   │   ├── mqtt/            # MQTT client and handlers
│   │   ├── services/        # Business logic services
│   │   └── usecases/        # Use cases
│   └── docker-compose.yml
├── shared/                  # KMP shared module
│   └── src/commonMain/kotlin/
│       ├── messages/        # Message definitions
│       ├── models/          # Domain models and DTOs
│       └── serialization/   # Serialization utilities
├── build-logic/             # Gradle convention plugins
├── baselineprofile/         # Baseline profile generation (Macrobenchmark)
├── .claude/                 # Claude Code configuration
│   ├── rules/               # Project instructions (CLAUDE.md)
│   └── skills/              # Custom agent skills
└── .github/workflows/       # CI/CD pipeline
    ├── android-ci.yml       # Build & test on push/PR
    ├── baseline-profile.yml # Baseline profile generation
    ├── code-format.yml      # Code formatting checks
    └── play-store-upload.yml # AAB signing & Play Store release
```

## Real-world usage

Croniot is already used in personal projects including an [automated watering system](https://github.com/vladimir-ivanov-info/croniot-watering-system) and anti-theft 4G+GNSS tracking for e-scooters and bikes.

