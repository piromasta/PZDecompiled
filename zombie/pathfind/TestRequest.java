package zombie.pathfind;

import zombie.ai.astar.Mover;

public final class TestRequest implements IPathfinder {
   public final Path path = new Path();
   public boolean done;

   public TestRequest() {
   }

   public void Succeeded(Path var1, Mover var2) {
      this.path.copyFrom(var1);
      this.done = true;
   }

   public void Failed(Mover var1) {
      this.path.clear();
      this.done = true;
   }
}
