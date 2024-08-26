# Croniot

## Description
This is a Kotlin Multiplatform project of the Croniot system. This project targets Android and Server.

## What is croniot useful for?

Croniot is a system that connects your IoT devices with your smartphone via a VPS server. This way you can easily monitor your IoT devices' sensors and run different tasks defined by you. Forget about the repetitive code: 95% of the job is already done! Just focus on your unique code without creating all the repetitive software from scratch for each new project. The IoT device tells the server the format of its sensors' data and tasks' expected parameters and your Android phone presents to you all this data in a user-friendly manner.

Croniot is made with the idea of saving time in two ways: saving software development time and saving time to the final users through the resulting projects that are created with it.

## Quick start (beginner friendly)

### Server (Linux tested, also works on Windows)
Install docker-compose.<br><br>
Go to `/server/` and open the terminal in this folder.<br><br>
run `docker-compose up`<br><br>
Now you have a PostgreSQL database the server can use to store the data.<br><br>
Run the server.<br><br>


//TODO

### Android

Once

### IoT

Go to https://github.com/vladimir-ivanov-info/croniot-iot and read the corresponding section. 

## Usage

croniot is used in different projects: watering system, anti-theft systems for e-scooters and bikes. Github links will be attached in this section after being uploaded.

## Contributing



* Croniot

* `/composeApp` is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - `commonMain` is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    `iosMain` would be the right folder for such calls.

* `/server` is for the Ktor server application.

* `/shared` is for the code that will be shared between all targets in the project.
  The most important subfolder is `commonMain`. If preferred, you can add code to the platform-specific folders here too.


Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
