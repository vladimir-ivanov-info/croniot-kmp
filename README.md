# Croniot

## What is Croniot?

Croniot is a framework that helps you connect your IoT devices with your smartphone via a local or remote server. 


This way you can easily monitor your IoT devices' sensors and run different tasks. All this data (sensors and tasks) is defined by you! 


The system is composed by 3 parts: an IoT device, a server and a mobile app.


The next time you want to make an IoT project, forget about the repetitive code: 95% of the job is already done:


❌ You don't need to make a new server for your IoT project. The server already exists! And you can easily run it either directly or in a docker container. This way you skip defining the data, creating a database structure, managing the transactions and the communication between the IoT and the mobile app.


❌ You don't need to create a new app to monitor your IoT device and run tasks on it. The app already exists!


❌ You don't need to create the repetitive IoT code: the WiFi connection management, the credentials' storage, the MQTT/HTTP connections management are already defined!


✅ Just focus on your unique project's code without creating all the repetitive software from scratch for each new project! All you have to do is:


  ⭐ For each sensor: make the code responsible of extracting the data from the sensor and transforming it to a human-readable value.


  ⭐ For each task: make the code that will be executed when you run the task from your smartphone.


Croniot is made with the idea of saving time in two ways: saving software development time and saving time to the final users through the resulting projects that are created with it.


What you see on the images is just a basic version of what Croniot can do right now. The functionalities are constantly expanding with the aim of covering more use cases and making the framework even easier to use.

## Project description
This is a Kotlin Multiplatform project that contains the code of the Android app and the server. The IoT project is here: https://github.com/vladimir-ivanov-info/croniot-iot


## Quick start (beginner friendly)

### Server (Linux tested, also works on Windows)
Install docker-compose.<br><br>
Go to `/server/` and open the terminal in this folder.<br><br>
run `docker-compose up`<br><br>
Now you have a PostgreSQL database the server can use to store the data.<br><br>
Run the server.<br><br>


//TODO

### Android

The following images show how the app works. 

First, you register your account.

After that, your IoT uses your credentials to register itself, telling the server about the sensors that it has and tasks that it can perform (not shown on the images).

Then you log in and see a list with all your devices. In this case there is only one IoT device, which is my watering system.

When you click on the device, you can see a screen with 3 tabs. 

The 1st tab "Sensors" shows all the sensors' information in a graph. The app knows how to graphically represent the values (minimum, maximum, whether it's a number or a string, etc.) because the IoT device told all the necesary information.


The 2nd tab "Task types" shows all the tasks that the IoT device can perform. When you click on a task, it lets you configure all the necessary values that the device expects in order to run the task. In this case, I choose that I want to water my plants for 374 seconds. After we press on the Add task button, the IoT device will run the task with the given parameters.


The 3rd tab "Tasks" shows the history of the tasks and the state in which they are (created, running, completed, error, etc.).




<img src="https://github.com/user-attachments/assets/0c0e8ac6-49d4-42e5-ac8a-05496354d38b" alt="Login screen" width="270" height="600">
<img src="https://github.com/user-attachments/assets/c3e16069-1856-4eff-8514-6d06949631f7" alt="Account register screen" width="270" height="600">
<img src="https://github.com/user-attachments/assets/ea69cf7f-9a79-46cc-9316-1b1cd8b978cb" alt="Account registered successfully" width="270" height="600">
<img src="https://github.com/user-attachments/assets/68d51fd7-8251-4c8e-b3a4-5b4c144a2f9c" alt="Devices screen" width="270" height="600">
<img src="https://github.com/user-attachments/assets/01653b76-c344-498c-9291-4cda0fed03d8" alt="Sensors screen" width="270" height="600">
<img src="https://github.com/user-attachments/assets/01653b76-c344-498c-9291-4cda0fed03d8" alt="Task types screen" width="270" height="600">
<img src="https://github.com/user-attachments/assets/b8859eaa-c8dd-42a8-bc4b-8a24b363238d" alt="Task configuration" width="270" height="600">
<img src="https://github.com/user-attachments/assets/6be13237-8a6f-4b20-8095-66020f803eb9" alt="Tasks' history" width="270" height="600">


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
