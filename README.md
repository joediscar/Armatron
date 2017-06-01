# Armatron
An android application that communicates to an IOIO board to control Lynxmotion's AL5D robotic arm.

This project is an Android Studio project.  Just import this into Android Studio as a new project, build it, and
put it on your phone.

To use this app with your Lynxmotion AL5D Robotic arm, you must interface your AL5D's SSC-32 board to an
IOIO board (https://www.sparkfun.com/products/13613).   The application is configured to use I/O pin #1 of the IOIO
board for Tx which should be connected to the Rx pin of the SSC-32 board.

Once done, you can connect your phone to the IOIO via bluetooth or USB cable and launch this application.  This app
gives you the ability to use inverse kinematics to move the arm into position, and open/close the gripper.  The 
gestures that are recognized are:

(1)  Single touch - move the arm to a new effector target
(2)  Two Finger touch scroll up - move the wrist up
(3)  Two Finger touch scroll down - move the wrist down
(4)  Two fingers scroll side-to-side - rotate the base
(5)  Two fingers pinch - close the gripper
(6)  Two fingers spread - open the gripper

This source includes the full source of the Caliko project's Fabrik IK library.  This differs from the original
Caliko project in that the included version will build in a Java 1.7 Android environment.  I intend to fork a new branch
on Caliko so that I don't have to include their source here... 
