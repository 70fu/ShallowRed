# Shallowred

## Team
Sebastian Serwin (01427028)  
Simon Reiser (11777770)

## Setup
1. The file **EndgameDB_ShallowRed.bin** must be placed in the working directory, which is the app directory if the provided .exe file is used.
2. Import the agent by adding **shallowred_main.jar**, which is also provided in our submission, and enter **at.pwd.shallowred.ShallowRed** as class name.

**Important**: 
- The application needs to be **restarted after playing one game.**
- It's not possible to play two ShallowRed-Agents against each other using provided application, since only a single instance of the ShallowRed-Agent is created


## Approach
Our AI uses MCTS with the following modifications:
- The exploitation term of the UCB now consists of a linear interpolation of the average win count of the random MCTS-Simulations and a heuristical value **h**
    <!---\alpha\ h_i + (1-\alpha)\ \frac{w_i}{n_i} + C *  \sqrt{\frac{2\ lg(N_i)}{n_i}} --->
    ![Modified UCB](/readmeAssets/Modified_UCB.png)
    
    This heuristical value h is calculated the following way:
    - **For terminal nodes (of the Search Tree)**: Stones in depot of the starting player minus stones in the other depot, scaled to [0,1].
    - **For non-terminal nodes**: 
        - **If it's the starting player's turn**: Take the *maximum* **h** of all children.
        - **If it's the other player's turn**: Take the *minimum* **h** of all children.
    
    This technique can be seen in the paper *Monte Carlo Tree Search with Heuristic Evaluation using Implicit Minimax Backups* [1]

- The heuristical value **h** is set to ∞ on a proven win or -∞ if the node is a proven loss, since it only matters to win with any difference of stones.
- On every turn the relevant subtree of the search tree of the last turn is reused.
- The parameters **α** and **C** of our modified UCB formula are automatically tuned using CLOP [2] using the AlphaBeta-Agent of the framework with increased depth and other ShallowRed-Agents. We also performed several tournaments between differently configured ShallowRed-Agents.
- We have calculated an endgame database, which stores for all boards up to 25 stones the difference of stones of the resulting board, if all players play perfectly.

We also use our own optimized implementation of Kalaha.

## Failed Attempts
Our first idea was to improve the simulation games of MCTS using simple heuristics. We have written 6 simple heuristics, which rate each possible turn for a given board.  
Each of these heuristics have a weight, that tells how important this heuristic is.  

But even after playing over 12000 games against the AlphaBetaAgent of the framework with different combinations of weights, CLOP [2] was not able to find a combination of weights, that could consistently beat the AlphaBetaAgent.  
Less than 10 games have been won against the AlphaBetaAgent. 

## References
1. Marc Lanctot, Mark H. M. Winands, Tom Pepels & Nathan R. Sturtevant (2014). Monte Carlo Tree Search with Heuristic Evaluations using Implicit Minimax Backups. CoRR, abs/1406.0486, .
2. Coulom R. (2012) CLOP: Confident Local Optimization for Noisy Black-Box Parameter Tuning. In: van den Herik H.J., Plaat A. (eds) Advances in Computer Games. ACG 2011. Lecture Notes in Computer Science, vol 7168. Springer, Berlin, Heidelberg
