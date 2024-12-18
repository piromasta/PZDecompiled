package zombie.pathfind;

import java.util.ArrayDeque;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;

final class PathRequestTask {
   PolygonalMap2 map;
   PathFindRequest request;
   static final ArrayDeque<PathRequestTask> pool = new ArrayDeque();

   PathRequestTask() {
   }

   PathRequestTask init(PolygonalMap2 var1, PathFindRequest var2) {
      this.map = var1;
      this.request = var2;
      return this;
   }

   void execute() {
      if (this.request.mover instanceof IsoPlayer) {
         this.map.requests.playerQ.add(this.request);
      } else if (this.request.mover instanceof IsoZombie && ((IsoZombie)this.request.mover).target != null) {
         this.map.requests.aggroZombieQ.add(this.request);
      } else {
         this.map.requests.otherQ.add(this.request);
      }

   }

   static PathRequestTask alloc() {
      synchronized(pool) {
         return pool.isEmpty() ? new PathRequestTask() : (PathRequestTask)pool.pop();
      }
   }

   public void release() {
      synchronized(pool) {
         assert !pool.contains(this);

         pool.push(this);
      }
   }
}
