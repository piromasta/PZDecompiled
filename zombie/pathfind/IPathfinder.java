package zombie.pathfind;

import zombie.ai.astar.Mover;

public interface IPathfinder {
   void Succeeded(Path var1, Mover var2);

   void Failed(Mover var1);
}
