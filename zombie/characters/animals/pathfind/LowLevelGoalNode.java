package zombie.characters.animals.pathfind;

import astar.IGoalNode;
import astar.ISearchNode;

public final class LowLevelGoalNode implements IGoalNode {
   LowLevelSearchNode searchNode;

   public LowLevelGoalNode() {
   }

   public boolean inGoal(ISearchNode var1) {
      return var1 == this.searchNode;
   }
}
