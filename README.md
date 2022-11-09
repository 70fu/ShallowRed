# Shallowred

ShallowRed is an AI for the two-player strategy game [Kalah](https://en.wikipedia.org/wiki/Kalah). It was developed during the course *Strategy Game Programming* (although the course is only about programming AIs for Strategy Games) at *Vienna University of Technology*. It achieved the 2nd place in the tournament of all AIs submitted in the course (total of 8 AIs).

## Team
Sebastian Serwin (01427028)  
Simon Reiser (11777770)

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

We also use our own optimized implementation of Kalaha and not the implementation provided by the course.

See [SUBMISSION.md](SUBMISSION.md) for failed attempts and approaches.

## References
1. Marc Lanctot, Mark H. M. Winands, Tom Pepels & Nathan R. Sturtevant (2014). Monte Carlo Tree Search with Heuristic Evaluations using Implicit Minimax Backups. CoRR, abs/1406.0486, .
2. Coulom R. (2012) CLOP: Confident Local Optimization for Noisy Black-Box Parameter Tuning. In: van den Herik H.J., Plaat A. (eds) Advances in Computer Games. ACG 2011. Lecture Notes in Computer Science, vol 7168. Springer, Berlin, Heidelberg
