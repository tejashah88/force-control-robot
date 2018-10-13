# force-control-robot
A project for controlling a Raspberry Pi robot using the Leap Motion and Firebase. Works on windows only.

# How to use
You'll need to make a new firebase project and change the urls to point to the new database URL.

In the `bin/` folder, the `LMVServer/` folder should be transfered to the Raspberry Pi and the `LMVClient/` should be on the computer in which you are controlling the robot via the leap motion.

On the Raspberry Pi, run the following command:
```bash
java -Djava.library.path=LMVClient_lib/native/x64 -d64 -jar LMVClient.jar
```

On your computer, start the Leap motion service and double click `lmvclient.bat` to start the client.