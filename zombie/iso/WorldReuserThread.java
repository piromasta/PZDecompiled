package zombie.iso;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import zombie.GameWindow;
import zombie.core.Core;
import zombie.core.ThreadGroups;
import zombie.core.logger.ExceptionLogger;
import zombie.debug.DebugLog;
import zombie.iso.objects.IsoTree;
import zombie.network.MPStatistic;

public final class WorldReuserThread {
   public static final WorldReuserThread instance = new WorldReuserThread();
   private final ArrayList<IsoObject> objectsToReuse = new ArrayList();
   private final ArrayList<IsoTree> treesToReuse = new ArrayList();
   public boolean finished;
   private Thread worldReuser;
   private final ConcurrentLinkedQueue<IsoChunk> reuseGridSquares = new ConcurrentLinkedQueue();

   public WorldReuserThread() {
   }

   public void run() {
      this.worldReuser = new Thread(ThreadGroups.Workers, () -> {
         while(!this.finished) {
            MPStatistic.getInstance().WorldReuser.Start();
            this.testReuseChunk();
            this.reconcileReuseObjects();
            MPStatistic.getInstance().WorldReuser.End();

            try {
               Thread.sleep(1000L);
            } catch (InterruptedException var2) {
               ExceptionLogger.logException(var2);
            }
         }

      });
      this.worldReuser.setName("WorldReuser");
      this.worldReuser.setDaemon(true);
      this.worldReuser.setUncaughtExceptionHandler(GameWindow::uncaughtException);
      this.worldReuser.start();
   }

   public void reconcileReuseObjects() {
      synchronized(this.objectsToReuse) {
         if (!this.objectsToReuse.isEmpty()) {
            synchronized(CellLoader.isoObjectCache) {
               if (CellLoader.isoObjectCache.size() < 320000) {
                  CellLoader.isoObjectCache.addAll(this.objectsToReuse);
               }
            }

            this.objectsToReuse.clear();
         }
      }

      synchronized(this.treesToReuse) {
         if (!this.treesToReuse.isEmpty()) {
            synchronized(CellLoader.isoTreeCache) {
               if (CellLoader.isoTreeCache.size() < 40000) {
                  CellLoader.isoTreeCache.addAll(this.treesToReuse);
               }
            }

            this.treesToReuse.clear();
         }

      }
   }

   public void testReuseChunk() {
      try {
         for(IsoChunk var1 = (IsoChunk)this.reuseGridSquares.poll(); var1 != null; var1 = (IsoChunk)this.reuseGridSquares.poll()) {
            if (Core.bDebug) {
               if (ChunkSaveWorker.instance.toSaveQueue.contains(var1)) {
                  DebugLog.log("ERROR: reusing chunk that needs to be saved");
               }

               if (IsoChunkMap.chunkStore.contains(var1)) {
                  DebugLog.log("ERROR: reusing chunk in chunkStore");
               }

               if (!var1.refs.isEmpty()) {
                  DebugLog.log("ERROR: reusing chunk with refs");
               }
            }

            if (Core.bDebug) {
            }

            this.reuseGridSquares(var1);
            if (this.treesToReuse.size() > 1000 || this.objectsToReuse.size() > 5000) {
               this.reconcileReuseObjects();
            }
         }
      } catch (Throwable var2) {
         ExceptionLogger.logException(var2);
      }

   }

   public void addReuseChunk(IsoChunk var1) {
      this.reuseGridSquares.add(var1);
   }

   public void reuseGridSquares(IsoChunk var1) {
      byte var2 = 64;

      for(int var3 = var1.minLevel; var3 <= var1.maxLevel; ++var3) {
         for(int var4 = 0; var4 < var2; ++var4) {
            int var5 = var1.squaresIndexOfLevel(var3);
            IsoGridSquare var6 = var1.squares[var5][var4];
            if (var6 != null) {
               for(int var7 = 0; var7 < var6.getObjects().size(); ++var7) {
                  IsoObject var8 = (IsoObject)var6.getObjects().get(var7);
                  if (var8 instanceof IsoTree) {
                     var8.reset();
                     synchronized(this.treesToReuse) {
                        this.treesToReuse.add((IsoTree)var8);
                     }
                  } else if (var8.getClass() == IsoObject.class) {
                     var8.reset();
                     synchronized(this.objectsToReuse) {
                        this.objectsToReuse.add(var8);
                     }
                  } else {
                     var8.reuseGridSquare();
                  }
               }

               var6.discard();
               var1.squares[var5][var4] = null;
            }
         }
      }

      var1.resetForStore();
      IsoChunkMap.chunkStore.add(var1);
   }
}
