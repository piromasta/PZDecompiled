package zombie.pathfind.nativeCode;

import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.animals.IsoAnimal;
import zombie.popman.ObjectPool;

class PathRequestTask implements IPathfindTask {
   PathFindRequest request;
   int queueNumber;
   static final ObjectPool<PathRequestTask> pool = new ObjectPool(PathRequestTask::new);

   PathRequestTask() {
   }

   PathRequestTask init(PathFindRequest var1) {
      this.request = var1;
      if (var1.mover instanceof IsoPlayer && !(var1.mover instanceof IsoAnimal)) {
         this.queueNumber = 1;
      } else if (var1.mover instanceof IsoZombie && ((IsoZombie)var1.mover).target != null) {
         this.queueNumber = 2;
      } else {
         this.queueNumber = 3;
      }

      return this;
   }

   public void execute() {
      PathfindNativeThread.instance.addRequest(this.request, this.queueNumber);
   }

   static PathRequestTask alloc() {
      return (PathRequestTask)pool.alloc();
   }

   public void release() {
      pool.release((Object)this);
   }
}
