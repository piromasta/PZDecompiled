package zombie.characters.animals.pathfind;

import astar.IGoalNode;
import astar.ISearchNode;

public final class HighLevelGoalNode implements IGoalNode {
   HighLevelSearchNode searchNode;

   public HighLevelGoalNode() {
   }

   HighLevelGoalNode init(HighLevelSearchNode var1) {
      this.searchNode = var1;
      return this;
   }

   public boolean inGoal(ISearchNode var1) {
      return var1 == this.searchNode;
   }
}
