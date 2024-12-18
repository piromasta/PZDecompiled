package zombie.pathfind.highLevel;

import astar.IGoalNode;
import astar.ISearchNode;

public class HLGoalNode implements IGoalNode {
   HLSearchNode searchNode;

   public HLGoalNode() {
   }

   HLGoalNode init(HLSearchNode var1) {
      this.searchNode = var1;
      return this;
   }

   public boolean inGoal(ISearchNode var1) {
      return var1 == this.searchNode;
   }
}
