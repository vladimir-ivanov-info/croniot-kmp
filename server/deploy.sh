#!/bin/bash
echo "$(pwd)"

# Variables
JAR_FILE="./server/build/libs/server-all.jar"  # Ensure this matches the actual JAR file name
REMOTE_JAR_PATH="/home/ubuntu/croniot/server-all.jar"
VPS_USER="ubuntu"
VPS_HOST="vps-52e8a9fb.vps.ovh.net"
JAVA_OPTIONS="-Xms256m -Xmx512m"  # Adjust Java options as needed

PROCESS_NAME="server-all.jar"

# Build the JAR file
echo "Building the JAR file..."
#cd "./server/"
#./gradlew clean build
#gradle :server:build

./gradlew :server:build -x test --info

echo "$(ls -l ./server/build/libs/)"

# Check if JAR file is created
if [ ! -f "$JAR_FILE" ]; then
  echo "Error: JAR file $JAR_FILE not found!"
  exit 1
fi

# Copy the JAR file to the VPS
echo "Copying the JAR file to the VPS..."
#scp -i /home/vladimir/vps_keys/vps_ssh_key $JAR_FILE $VPS_USER@$VPS_HOST:$REMOTE_JAR_PATH
scp $JAR_FILE $VPS_USER@$VPS_HOST:$REMOTE_JAR_PATH
echo "### ### ### File uploaded..."





# Connect to the VPS, stop server process if exists and run the new server
echo "### ### ### Connecting to VPS server..."
ssh -t $VPS_USER@$VPS_HOST << 'EOF'
echo "Running server"

# Find the process ID of the running server
PID=$(ps aux | grep 'server-all.jar' | grep -v grep | awk '{print $2}')
if [ -z "$PID" ]; then
    echo "No matching process found, starting server..."
    cd "/home/ubuntu/croniot"
    java -jar server-all.jar
else
    echo "Server is already running, PID: $PID"
    sudo kill $PID
    cd "/home/ubuntu/croniot"
    java -jar $PROCESS_NAME
fi
EOF

