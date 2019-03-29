# Spaghetti Factory: 1D cloth
Implemented robust collision processing as described in the [2011 Bridson paper](https://www.cs.ubc.ca/~rbridson/docs/cloth2002.pdf). 
- Used continuous point-edge collision detection to determine when and where to apply impulses between point and edges. 
- Applied impulses to resolve collisions
- Iteratively solved for multiple collision interactions. 
- Applied penalty forces at a distance of 0.01 
- Implemented friction

![alt text](https://raw.githubusercontent.com/mjyip8/Snow/master/artifacts/long_spaghetti.gif)
![alt text](https://raw.githubusercontent.com/mjyip8/Snow/master/artifacts/spaghetti.gif)


## Additional Comments
My most successful attempt is far from realistic. The spaghetti is a little bouncy. The simulation also would be more efficient with some sort of acceleration structure (QuadTree?).
