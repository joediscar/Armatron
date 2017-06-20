# Armatron
An android application that communicates to an IOIO board to control Lynxmotion's AL5D robotic arm.

# INTRODUCTION

This project is actually a proof-of-concept I developed on my way to making an autonomous hexapod demonstration
bot using Lynxmotion's T-HEX platform.  Lynxmotion T-HEX uses an SSC-32 servo controller board to control the
24 servos that make up its legs.  The SSC-32 is driven by (in my case) a Bot Board II (a Basic Micro controller).
The T-HEX objective was complete autonomy meaning its requirements  are that it knows its location, can plot
its own courses, can use various sensors, can communicate to a network, etc.

It occurred to me that the hardware requirements could easily use an Android mobile device rather than me
purchasing a lot of discrete components.  The main advantage of an Android approach is that the processor on
a typical smartphone is measured in gigahertz instead of the paltry 16 MHZ in the Bot Board II.

So the architecture of an Android-controlled T-HEX would retain the SSC-32 as a servo controller, but all the
computations would occur on the Android.   Having the Android communicate to the SSC-32 is actually really
easy if you use Sparkfun's IOIO-OTG board.

To test out whether the approach was feasible, I did a proof-of-concept using an AL5D Robotic arm.  Mechanically
the AL5D is very similar to one leg of a T-HEX.  The result of this research works pretty well--far better than
the AL5D was using the Flowstone software.

So to spur on development of this approach, I'm making my work available here. 



# HOW TO USE

This project is an Android Studio project.  Just import this into Android Studio as a new project, build it, and
put it on your phone or tablet.

To use this app with your Lynxmotion AL5D Robotic arm, you must interface your AL5D's SSC-32 board to an
IOIO board (https://www.sparkfun.com/products/13613).   The application is configured to use I/O pin #1 of the IOIO
board for Tx which should be connected to the Rx pin of the SSC-32 board.  The IOIO Pin #2 can connect to the 
Rx pin of the SSC-32... and of course you need to connect the ground.   Supply power to the IOIO via its 
Vin pins, and that pretty much completes the soldering.


Once done, you can connect your phone to the IOIO via bluetooth or USB cable and launch this application.  This app
gives you the ability to use inverse kinematics to move the arm into position, and open/close the gripper.  The 
gestures that are recognized are:

1.  Single touch - move the arm to a new effector target
2.  Two Finger touch scroll up - move the wrist up
3.  Two Finger touch scroll down - move the wrist down
4.  Two fingers scroll side-to-side - rotate the base
5.  Two fingers pinch - close the gripper
6.  Two fingers spread - open the gripper

![Cheatsheet](http://discar.tv/share/cheatsheet.png)



This source includes the full source of the Caliko project's Fabrik IK library.  This differs from the original
Caliko project in that the included version will build in a Java 1.7 Android environment.  I've been communicating
with the developer of Caliko to see if a Java 1.7 jar artifact could be made available. 

You can see a short video demo of the result, an android-controlled robot arm, here: http://discar.tv/armatron.html . 

