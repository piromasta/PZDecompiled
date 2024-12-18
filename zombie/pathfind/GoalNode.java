package zombie.pathfind;

import astar.IGoalNode;
import astar.ISearchNode;

final class GoalNode implements IGoalNode {
   SearchNode searchNode;

   GoalNode() {
   }

   GoalNode init(SearchNode var1) {
      this.searchNode = var1;
      return this;
   }

   public boolean inGoal(ISearchNode var1) {
      return var1 == this.searchNode;
   }
}
