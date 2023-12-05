package zombie.ai.astar;

public class AStarPathFinder {
   public AStarPathFinder() {
   }

   public static enum PathFindProgress {
      notrunning,
      failed,
      found,
      notyetfound;

      private PathFindProgress() {
      }
   }
}
