# PSXInfo

PSXInfo is an addon to PSX that generates files with the history of the position of PSX as well as calculates a score for each landing.

# Landing Score

  - Each pilot starts with a 100% score (perfect landing)
  - Points are then deducted via various parameters via the touchdown variable given by PSX and seconds between landing the main gear and landing the nose (nose transition time)
  - If a pilot bounces or makes the aircraft UNSERVICABLE, the get 0%
  - Unservicable means either: tailstrike (even on takeoff), wing strike, pod strike, gear collapse
  - To reset the serviceability of an aircraft. Click on the red UNSERVICABLE label which will make the main score label turn from red to black
  - If the runway of your scenery does not align with PSX, turn off "Expert Mode" in the options. This will make the landing off and threshold distance not be taken into account when calculating the score. Turning it on will make the score slighly harsher as you need to land on the right spot in order to get a good score


# Future Development...
- Add a section that you can input values to discover what score the parameters will yield
- Implement a server that will keep a collection of all scores
- Potentially look at changing the range of scores similar to how Flight Simulator has it. i.e. the score keeps on going up and not within a 0% - 100% range. The benefits of this is that it will allow for expansion.
