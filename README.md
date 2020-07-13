# bingo
[Java Swing] 

Simple random number generator for bingo, with large display and history tracker.

Built to specification for use in ITV office Bingo (but free to use or adapt for your own purposes).

Compatible with any OS, but only tested on Mac so far.

# Build

Compile simply with
```bash
  cd src
  javac RNG.java
```
and run with
```bash
  java -ea RNG
 ```

*Alternatively* use
```bash
  cd src
  chmod 711 make
  ./make
```
to create an executable .jar on Mac/Linux

# Use

1. Select the range of numbers. 
    1. For UK (90-ball) Bingo, leave the defaults of 1-90
    1. For US (75-ball) Bingo, change ```Biggest number``` to 75
1. Press ```Start```.
1. Click on the large top panel to draw the next ball.
1. The top panel will display the ball drawn.
    1. The middle panel displays all the balls drawn so far.
    1. The bottom panel displays the number of balls drawn and the number remaining in the bag.
1. Repeat step 3-5 until the game is over.
1. Use the middle panel to verify the winner.
