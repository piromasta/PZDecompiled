package zombie.characters.animals;

import gnu.trove.set.hash.TIntHashSet;
import java.util.ArrayList;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoMovingObject;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.util.Type;

public final class AnimalPopulationManager {
   private static AnimalPopulationManager instance;
   protected static final int SQUARES_PER_CHUNK = 8;
   protected static final int CHUNKS_PER_CELL;
   protected static final int SQUARES_PER_CELL;
   protected int minX;
   protected int minY;
   protected int width;
   protected int height;
   protected boolean bStopped;
   protected boolean bClient;
   private final TIntHashSet newChunks = new TIntHashSet();
   private final ArrayList<IsoAnimal> recentlyRemoved = new ArrayList();

   public static AnimalPopulationManager getInstance() {
      if (instance == null) {
         instance = new AnimalPopulationManager();
      }

      return instance;
   }

   AnimalPopulationManager() {
      this.newChunks.setAutoCompactionFactor(0.0F);
   }

   public void init(IsoMetaGrid var1) {
      this.bClient = GameClient.bClient;
      this.minX = var1.getMinX();
      this.minY = var1.getMinY();
      this.width = var1.getWidth();
      this.height = var1.getHeight();
      this.bStopped = false;
      this.n_init(this.bClient, GameServer.bServer, this.minX, this.minY, this.width, this.height);
   }

   public void addChunkToWorld(IsoChunk var1) {
      if (!this.bClient) {
         if (var1.isNewChunk()) {
            int var2 = var1.wy << 16 | var1.wx;
            this.newChunks.add(var2);
         }

         this.n_loadChunk(var1.wx, var1.wy);
      }
   }

   public void removeChunkFromWorld(IsoChunk var1) {
      if (!this.bClient) {
         if (!this.bStopped) {
            this.n_unloadChunk(var1.wx, var1.wy);

            int var2;
            for(var2 = var1.getMinLevel(); var2 <= var1.getMaxLevel(); ++var2) {
               for(int var3 = 0; var3 < 8; ++var3) {
                  for(int var4 = 0; var4 < 8; ++var4) {
                     IsoGridSquare var5 = var1.getGridSquare(var4, var3, var2);
                     if (var5 != null && !var5.getMovingObjects().isEmpty()) {
                        for(int var6 = 0; var6 < var5.getMovingObjects().size(); ++var6) {
                           IsoAnimal var7 = (IsoAnimal)Type.tryCastTo((IsoMovingObject)var5.getMovingObjects().get(var6), IsoAnimal.class);
                           if (var7 != null) {
                              var7.unloaded();
                              this.n_addAnimal(var7);
                              var7.setStateMachineLocked(false);
                              var7.setDefaultState();
                           }
                        }
                     }
                  }
               }
            }

            var2 = var1.wy << 16 | var1.wx;
            this.newChunks.remove(var2);
         }
      }
   }

   public void virtualizeAnimal(IsoAnimal var1) {
      var1.unloaded();
      this.n_addAnimal(var1);
      var1.delete();
      var1.setStateMachineLocked(false);
      var1.setDefaultState();
   }

   public void addToRecentlyRemoved(IsoAnimal var1) {
      if (!this.recentlyRemoved.contains(var1)) {
         this.recentlyRemoved.add(var1);
      }
   }

   public void update() {
      long var1 = System.currentTimeMillis();

      for(int var3 = this.recentlyRemoved.size() - 1; var3 >= 0; --var3) {
         IsoAnimal var4 = (IsoAnimal)this.recentlyRemoved.get(var3);
         if (!var4.getEmitter().isClear()) {
            var4.updateEmitter();
         }

         if (var1 - var4.removedFromWorldMS > 5000L) {
            var4.getEmitter().stopAll();
            this.recentlyRemoved.remove(var3);
         }
      }

   }

   public void save() {
      if (!this.bClient) {
         AnimalManagerMain.getInstance().saveRealAnimals();
         this.n_save();
      }
   }

   public void stop() {
      if (!this.bClient) {
         this.bStopped = true;
         this.n_stop();
         this.newChunks.clear();
         this.recentlyRemoved.clear();
      }
   }

   void n_init(boolean var1, boolean var2, int var3, int var4, int var5, int var6) {
      AnimalManagerWorker.getInstance().init(var1, var2, var3, var4, var5, var6);
   }

   void n_loadChunk(int var1, int var2) {
      AnimalManagerMain.getInstance().loadChunk(var1, var2);
   }

   void n_unloadChunk(int var1, int var2) {
      AnimalManagerMain.getInstance().unloadChunk(var1, var2);
   }

   void n_addAnimal(IsoAnimal var1) {
      VirtualAnimal var2 = new VirtualAnimal();
      var2.setX(var1.getX());
      var2.setY(var1.getY());
      var2.setZ(var1.getZ());
      var2.m_bMoveForwardOnZone = var1.isMoveForwardOnZone();
      var2.m_forwardDirection.set(var1.getForwardDirection().x, var1.getForwardDirection().y);
      var2.m_animals.add(var1);
      var2.id = var1.virtualID;
      var2.migrationGroup = var1.migrationGroup;
      MigrationGroupDefinitions.initValueFromDef(var2);
      AnimalManagerMain.getInstance().addAnimal(var2);
   }

   void n_saveRealAnimals(ArrayList<IsoAnimal> var1) {
      AnimalManagerWorker.getInstance().saveRealAnimals(var1);
   }

   void n_save() {
      AnimalManagerWorker.getInstance().save();
   }

   void n_stop() {
      AnimalManagerWorker.getInstance().stop();
   }

   static {
      CHUNKS_PER_CELL = IsoCell.CellSizeInChunks;
      SQUARES_PER_CELL = CHUNKS_PER_CELL * 8;
   }
}
