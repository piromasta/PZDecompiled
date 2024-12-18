package zombie.pathfind.nativeCode;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import zombie.ai.astar.Mover;
import zombie.core.logger.ExceptionLogger;
import zombie.pathfind.Path;
import zombie.pathfind.PathNode;

public class PathfindNativeThread extends Thread {
   public static PathfindNativeThread instance;
   public boolean bStop;
   public final Object notifier = new Object();
   public final Object renderLock = new Object();
   protected final ConcurrentLinkedQueue<IPathfindTask> chunkTaskQueue = new ConcurrentLinkedQueue();
   protected final ConcurrentLinkedQueue<SquareUpdateTask> squareTaskQueue = new ConcurrentLinkedQueue();
   protected final ConcurrentLinkedQueue<IPathfindTask> vehicleTaskQueue = new ConcurrentLinkedQueue();
   protected final ConcurrentLinkedQueue<PathRequestTask> requestTaskQueue = new ConcurrentLinkedQueue();
   protected final ConcurrentLinkedQueue<IPathfindTask> taskReturnQueue = new ConcurrentLinkedQueue();
   private final RequestQueue requests = new RequestQueue();
   protected final HashMap<Mover, PathFindRequest> requestMap = new HashMap();
   protected final ConcurrentLinkedQueue<PathFindRequest> requestToMain = new ConcurrentLinkedQueue();
   protected final Path shortestPath = new Path();
   protected final ByteBuffer pathBB = ByteBuffer.allocateDirect(3072);
   private final Sync sync = new Sync();

   PathfindNativeThread() {
      this.pathBB.order(ByteOrder.LITTLE_ENDIAN);
   }

   public void run() {
      while(!this.bStop) {
         try {
            this.runInner();
         } catch (Throwable var2) {
            ExceptionLogger.logException(var2);
         }
      }

   }

   private void runInner() {
      this.sync.startFrame();
      synchronized(this.renderLock) {
         this.updateThread();
      }

      this.sync.endFrame();

      while(this.shouldWait()) {
         synchronized(this.notifier) {
            try {
               this.notifier.wait();
            } catch (InterruptedException var4) {
            }
         }
      }

   }

   private void updateThread() {
      int var1 = 10;

      IPathfindTask var2;
      for(var2 = (IPathfindTask)this.chunkTaskQueue.poll(); var2 != null; var2 = (IPathfindTask)this.chunkTaskQueue.poll()) {
         var2.execute();
         this.taskReturnQueue.add(var2);
         if (var2 instanceof ChunkUpdateTask) {
            --var1;
            if (var1 <= 0) {
               break;
            }
         }
      }

      for(SquareUpdateTask var6 = (SquareUpdateTask)this.squareTaskQueue.poll(); var6 != null; var6 = (SquareUpdateTask)this.squareTaskQueue.poll()) {
         var6.execute();
         this.taskReturnQueue.add(var6);
      }

      for(var2 = (IPathfindTask)this.vehicleTaskQueue.poll(); var2 != null; var2 = (IPathfindTask)this.vehicleTaskQueue.poll()) {
         var2.execute();
         this.taskReturnQueue.add(var2);
      }

      for(PathRequestTask var7 = (PathRequestTask)this.requestTaskQueue.poll(); var7 != null; var7 = (PathRequestTask)this.requestTaskQueue.poll()) {
         var7.execute();
         this.taskReturnQueue.add(var7);
      }

      PathfindNative.update();
      int var8 = 2;

      while(!this.requests.isEmpty()) {
         PathFindRequest var3 = this.requests.removeFirst();
         if (var3.cancel) {
            this.requestToMain.add(var3);
         } else {
            try {
               this.findPath(var3);
               if (!var3.targetXYZ.isEmpty()) {
                  this.findShortestPathOfMultiple(var3);
               }
            } catch (Throwable var5) {
               ExceptionLogger.logException(var5);
               var3.path.clear();
            }

            this.requestToMain.add(var3);
            --var8;
            if (var8 == 0) {
               break;
            }
         }
      }

   }

   private boolean shouldWait() {
      if (this.bStop) {
         return false;
      } else if (!this.chunkTaskQueue.isEmpty()) {
         return false;
      } else if (!this.squareTaskQueue.isEmpty()) {
         return false;
      } else if (!this.vehicleTaskQueue.isEmpty()) {
         return false;
      } else if (!this.requestTaskQueue.isEmpty()) {
         return false;
      } else {
         return this.requests.isEmpty();
      }
   }

   void wake() {
      synchronized(this.notifier) {
         this.notifier.notify();
      }
   }

   public void addRequest(PathFindRequest var1, int var2) {
      if (var2 == 1) {
         this.requests.playerQ.add(var1);
      } else if (var2 == 2) {
         this.requests.aggroZombieQ.add(var1);
      } else {
         this.requests.otherQ.add(var1);
      }

   }

   private void findPath(PathFindRequest var1) {
      this.pathBB.clear();
      int var2 = PathfindNative.instance.findPath(var1, this.pathBB, var1.doNotRelease);
      short var3 = 0;
      if (var2 == 1) {
         var3 = this.pathBB.getShort();
         this.pathBB.limit(2 + var3 * 3 * 4);
      }

      var1.path.clear();

      for(int var4 = 0; var4 < var3; ++var4) {
         float var5 = this.pathBB.getFloat();
         float var6 = this.pathBB.getFloat();
         float var7 = this.pathBB.getFloat() - 32.0F;
         var1.path.addNode(var5, var6, var7);
      }

      if (var3 == 1) {
         PathNode var8 = var1.path.getNode(0);
         var1.path.addNode(var8.x, var8.y, var8.z);
      }

      if (var1.path.size() < 2) {
         var1.path.clear();
      }
   }

   private void findShortestPathOfMultiple(PathFindRequest var1) {
      this.shortestPath.copyFrom(var1.path);
      float var2 = var1.targetX;
      float var3 = var1.targetY;
      float var4 = var1.targetZ;
      float var5 = this.shortestPath.isEmpty() ? 3.4028235E38F : this.shortestPath.length();

      for(int var6 = 0; var6 < var1.targetXYZ.size(); var6 += 3) {
         var1.targetX = var1.targetXYZ.get(var6);
         var1.targetY = var1.targetXYZ.get(var6 + 1);
         var1.targetZ = var1.targetXYZ.get(var6 + 2);
         var1.path.clear();
         this.findPath(var1);
         if (!var1.path.isEmpty()) {
            float var7 = var1.path.length();
            if (var7 < var5) {
               var5 = var7;
               this.shortestPath.copyFrom(var1.path);
               var2 = var1.targetX;
               var3 = var1.targetY;
               var4 = var1.targetZ;
            }
         }
      }

      var1.path.copyFrom(this.shortestPath);
      var1.targetX = var2;
      var1.targetY = var3;
      var1.targetZ = var4;
   }

   public void stopThread() {
      this.bStop = true;
      this.wake();

      while(this.isAlive()) {
         try {
            Thread.sleep(5L);
         } catch (InterruptedException var2) {
         }
      }

   }

   public void cleanup() {
      IPathfindTask var1;
      for(var1 = (IPathfindTask)this.chunkTaskQueue.poll(); var1 != null; var1 = (IPathfindTask)this.chunkTaskQueue.poll()) {
         var1.release();
      }

      for(SquareUpdateTask var2 = (SquareUpdateTask)this.squareTaskQueue.poll(); var2 != null; var2 = (SquareUpdateTask)this.squareTaskQueue.poll()) {
         var2.release();
      }

      for(var1 = (IPathfindTask)this.vehicleTaskQueue.poll(); var1 != null; var1 = (IPathfindTask)this.vehicleTaskQueue.poll()) {
         var1.release();
      }

      for(PathRequestTask var3 = (PathRequestTask)this.requestTaskQueue.poll(); var3 != null; var3 = (PathRequestTask)this.requestTaskQueue.poll()) {
         var3.release();
      }

      for(var1 = (IPathfindTask)this.taskReturnQueue.poll(); var1 != null; var1 = (IPathfindTask)this.taskReturnQueue.poll()) {
         var1.release();
      }

      PathFindRequest var4;
      while(!this.requests.isEmpty()) {
         var4 = this.requests.removeLast();
         if (!var4.doNotRelease) {
            var4.release();
         }
      }

      while(!this.requestToMain.isEmpty()) {
         var4 = (PathFindRequest)this.requestToMain.remove();
         if (!var4.doNotRelease) {
            var4.release();
         }
      }

      this.requestMap.clear();
   }

   private static class Sync {
      private int fps = 20;
      private long period;
      private long excess;
      private long beforeTime;
      private long overSleepTime;

      private Sync() {
         this.period = 1000000000L / (long)this.fps;
         this.beforeTime = System.nanoTime();
         this.overSleepTime = 0L;
      }

      void begin() {
         this.beforeTime = System.nanoTime();
         this.overSleepTime = 0L;
      }

      void startFrame() {
         this.excess = 0L;
      }

      void endFrame() {
         long var1 = System.nanoTime();
         long var3 = var1 - this.beforeTime;
         long var5 = this.period - var3 - this.overSleepTime;
         if (var5 > 0L) {
            try {
               Thread.sleep(var5 / 1000000L);
            } catch (InterruptedException var8) {
            }

            this.overSleepTime = System.nanoTime() - var1 - var5;
         } else {
            this.excess -= var5;
            this.overSleepTime = 0L;
         }

         this.beforeTime = System.nanoTime();
      }
   }
}
