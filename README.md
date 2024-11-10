# Croniot

## What is Croniot?

Croniot is a framework that connects IoT devices to your smartphone via a local or remote server, allowing you to monitor sensors and run tasks seamlessly. 


This way you can easily monitor your IoT devices' sensors and run different tasks. All this data (sensors and tasks) is defined by you! 


The system is composed by 3 parts: an IoT device, a server and a mobile app.

## Advantages

The next time you want to make a new IoT project, forget about the repetitive code: 95% of the job is already done!


❌ No need to:

• Build a new server: Croniot provides a pre-configured server that runs locally or in Docker, handling data storage, transactions, and communication.


• Develop a mobile app: Croniot includes a ready-made app to monitor and control your IoT devices.


• Write repetitive IoT code: WiFi management, credential storage, and MQTT/HTTP communication are already built-in.


✅ Focus on your project-specific code:


• For each sensor, implement code to extract and format data.


• For each task, define the execution code, which you will trigger remotely from your smartphone.

<br>

Croniot saves time both in development and for end-users through the projects created with it.


What you see on the images is just a basic version of what Croniot can do right now. The functionalities are constantly being expanded with the aim of covering more use cases and making the framework even easier to use.

## This git repository
This is a Kotlin Multiplatform repository that contains the code of the Android app and the server. The IoT project is here: https://github.com/vladimir-ivanov-info/croniot-iot


## Quick start (beginner friendly)

### Server (Linux tested, also works on Windows)
Install docker-compose.<br><br>
Navigate to /server/ in your terminal.<br><br>
run `docker-compose up` to start the PostgreSQL database and server.<br><br>
Now you have a PostgreSQL database the server can use to store the data.<br><br>
Run the server.<br><br>


//TODO

### Android

The following images show how the app works. 

First, you register your account.

After that, your IoT uses your credentials to register itself, telling the server about the sensors that it has and tasks that it can perform (not shown on the images).

Then you log in and see a list with all your devices. In this case there is only one IoT device, which is my watering system.

When you click on the device, you can see a screen with 3 tabs that show this device's information.. 

The 1st tab "Sensors" shows all the sensors' information in a graph. The app knows how to graphically represent the values (minimum, maximum, whether it's a number or a string, etc.) because the IoT device told all the necesary information.


The 2nd tab "Task types" shows all the tasks that the IoT device can perform. When you click on a task, it lets you configure all the necessary values that the device expects in order to run the task. In this case, I choose that I want to water my plants for 374 seconds. After we press on the Add task button, the IoT device will run the task with the given parameters.


The 3rd tab "Tasks" shows the history of the tasks and the state in which they are (created, running, completed, error, etc.).




<img src="https://github.com/user-attachments/assets/0c0e8ac6-49d4-42e5-ac8a-05496354d38b" alt="Login screen" width="270" height="600">
<img src="https://github.com/user-attachments/assets/c3e16069-1856-4eff-8514-6d06949631f7" alt="Account register screen" width="270" height="600">
<img src="https://github.com/user-attachments/assets/ea69cf7f-9a79-46cc-9316-1b1cd8b978cb" alt="Account registered successfully" width="270" height="600">
<img src="https://github.com/user-attachments/assets/68d51fd7-8251-4c8e-b3a4-5b4c144a2f9c" alt="Devices screen" width="270" height="600">
<img src="https://github.com/user-attachments/assets/01653b76-c344-498c-9291-4cda0fed03d8" alt="Sensors screen" width="270" height="600">



<img src="https://github.com/user-attachments/assets/64010582-73c5-4911-84ec-f284d4c5251f" alt="Tasks' history" width="270" height="600">

<img src="https://github.com/user-attachments/assets/d3af0540-8f41-430c-bba6-07a03a7f9f7d" alt="Tasks' history" width="270" height="600">

<img src="https://github.com/user-attachments/assets/ccb5e7b1-d831-42dc-91e1-93ed50b5dd0c" alt="Tasks' history" width="270" height="600">

<img src="https://github.com/user-attachments/assets/f395cf9b-ccf4-433f-8e5d-610cf1cbc0dc" alt="Tasks' history" width="270" height="600">





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
