package zombie.characters.animals;

import java.util.ArrayList;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.iso.IsoCell;

public final class AnimalManagerWorker {
   private static AnimalManagerWorker instance = new AnimalManagerWorker();
   protected static final int SQUARES_PER_CHUNK = 8;
   protected static final int CHUNKS_PER_CELL;
   protected static final int SQUARES_PER_CELL;
   int m_minX;
   int m_minY;
   int m_width;
   int m_height;
   boolean m_bServer;
   boolean m_bClient;
   AnimalCell[] m_cells;
   final ArrayList<AnimalCell> m_loadedCells = new ArrayList();

   public AnimalManagerWorker() {
   }

   public static AnimalManagerWorker getInstance() {
      if (instance == null) {
         instance = new AnimalManagerWorker();
      }

      return instance;
   }

   void init(boolean var1, boolean var2, int var3, int var4, int var5, int var6) {
      this.m_bServer = var2;
      this.m_bClient = var1;
      this.m_minX = var3;
      this.m_minY = var4;
      this.m_width = var5;
      this.m_height = var6;
      this.m_cells = new AnimalCell[this.m_width * this.m_height];
      if (!var1) {
         for(int var7 = 0; var7 < this.m_cells.length; ++var7) {
            this.allocCell(var7);
         }

      }
   }

   public void allocCell(int var1) {
      this.m_cells[var1] = AnimalCell.alloc().init(this.m_minX + var1 % this.m_width, this.m_minY + var1 / this.m_width);
      AnimalZones.createJunctions(this.m_cells[var1]);
   }

   public void allocCell(int var1, int var2) {
      AnimalCell var3 = this.getCellFromCellPos(var1, var2);
      if (var3 != null) {
         var3.m_bAddedJunctions = false;
         AnimalZones.createJunctions(var3);
      } else {
         this.allocCell(var1 - this.m_minX + (var2 - this.m_minY) * this.m_width);
      }
   }

   void saveRealAnimals(ArrayList<IsoAnimal> var1) {
      for(int var2 = 0; var2 < var1.size(); ++var2) {
         IsoAnimal var3 = (IsoAnimal)var1.get(var2);
         AnimalCell var4 = this.getCellFromSquarePos(PZMath.fastfloor(var3.getX()), PZMath.fastfloor(var3.getY()));
         if (var4 != null) {
            this.loadIfNeeded(var4);
            VirtualAnimal var5 = new VirtualAnimal();
            var5.setX(var3.getX());
            var5.setY(var3.getY());
            var5.setZ(var3.getZ());
            var5.m_bMoveForwardOnZone = var3.isMoveForwardOnZone();
            var5.m_forwardDirection.set(var3.getForwardDirection().x, var3.getForwardDirection().y);
            var5.id = var3.virtualID;
            var5.migrationGroup = var3.migrationGroup;
            var5.m_animals.add(var3);
            var4.m_saveRealAnimalHack.add(var5);
            var4.m_bDataChanged = true;
         }
      }

   }

   void save() {
      for(int var1 = 0; var1 < this.m_loadedCells.size(); ++var1) {
         AnimalCell var2 = (AnimalCell)this.m_loadedCells.get(var1);
         if (var2.m_bDataChanged) {
            var2.m_bDataChanged = false;
            var2.save();
         }
      }

   }

   void stop() {
      int var1;
      for(var1 = 0; var1 < this.m_loadedCells.size(); ++var1) {
         AnimalCell var2 = (AnimalCell)this.m_loadedCells.get(var1);
         if (var2.m_bDataChanged) {
            var2.m_bDataChanged = false;
            var2.unload();
         }
      }

      this.m_loadedCells.clear();
      if (this.m_cells != null) {
         for(var1 = 0; var1 < this.m_cells.length; ++var1) {
            this.m_cells[var1].release();
            this.m_cells[var1] = null;
         }

         this.m_cells = null;
      }
   }

   AnimalCell getCellFromCellPos(int var1, int var2) {
      var1 -= this.m_minX;
      var2 -= this.m_minY;
      return var1 >= 0 && var1 < this.m_width && var2 >= 0 && var2 < this.m_height && this.m_cells != null ? this.m_cells[var1 + var2 * this.m_width] : null;
   }

   AnimalCell getCellFromSquarePos(int var1, int var2) {
      var1 -= this.m_minX * SQUARES_PER_CELL;
      var2 -= this.m_minY * SQUARES_PER_CELL;
      if (var1 >= 0 && var2 >= 0) {
         int var3 = var1 / SQUARES_PER_CELL;
         int var4 = var2 / SQUARES_PER_CELL;
         return var3 < this.m_width && var4 < this.m_height ? this.m_cells[var3 + var4 * this.m_width] : null;
      } else {
         return null;
      }
   }

   AnimalCell getCellFromChunkPos(int var1, int var2) {
      return this.getCellFromSquarePos(var1 * 8, var2 * 8);
   }

   void loadIfNeeded(AnimalCell var1) {
      var1.m_loadedTime = System.currentTimeMillis();
      if (!var1.isLoaded()) {
         var1.load();

         assert !this.m_loadedCells.contains(var1);

         this.m_loadedCells.add(var1);
      }
   }

   void loadChunk(int var1, int var2) {
      AnimalCell var3 = this.getCellFromChunkPos(var1, var2);
      if (var3 != null) {
         var3.setChunkLoaded(var1, var2, true);
         this.loadIfNeeded(var3);
         AnimalChunk var4 = var3.getChunkFromChunkPos(var1, var2);
         if (var4 != null) {
            this.passToMain(var4.m_animals);
            var4.m_animals.clear();
            var3.m_bDataChanged = true;
         }
      }
   }

   void unloadChunk(int var1, int var2) {
      AnimalCell var3 = this.getCellFromChunkPos(var1, var2);
      if (var3 != null) {
         var3.setChunkLoaded(var1, var2, false);
      }
   }

   void addAnimal(VirtualAnimal var1) {
      AnimalCell var2 = this.getCellFromSquarePos(PZMath.fastfloor(var1.getX()), PZMath.fastfloor(var1.getY()));
      if (var2 != null) {
         this.loadIfNeeded(var2);
         AnimalChunk var3 = var2.getChunkFromSquarePos(PZMath.fastfloor(var1.getX()), PZMath.fastfloor(var1.getY()));
         if (var3 != null) {
            VirtualAnimal var4 = var3.findAnimalByID(var1.id);
            if (var4 != var1) {
               if (var4 != null) {
                  for(int var5 = 0; var5 < var1.m_animals.size(); ++var5) {
                     IsoAnimal var6 = (IsoAnimal)var1.m_animals.get(var5);
                     if (var4.m_animals.contains(var6)) {
                        DebugLog.Animal.error("Trying to add an existing animal.");
                        var1.m_animals.remove(var5--);
                     } else {
                        IsoAnimal var7 = var4.findAnimalById(var6.getAnimalID());
                        if (var7 != null) {
                           DebugLog.Animal.error("Trying to add an animal with an existing IsoAnimal.animalID.");
                           var1.m_animals.remove(var5--);
                        }
                     }
                  }

                  var4.m_animals.addAll(var1.m_animals);
                  var2.m_bDataChanged = true;
               } else {
                  var3.m_animals.add(var1);
                  var2.m_bDataChanged = true;
                  if (var3.m_animals.size() + var3.m_animalTracks.size() == 1) {
                     AnimalZones.addAnimalChunk(var3);
                  }

               }
            }
         }
      }
   }

   public AnimalChunk getAnimalChunk(float var1, float var2) {
      AnimalCell var3 = this.getCellFromSquarePos(PZMath.fastfloor(var1), PZMath.fastfloor(var2));
      if (var3 == null) {
         return null;
      } else {
         this.loadIfNeeded(var3);
         AnimalChunk var4 = var3.getChunkFromSquarePos(PZMath.fastfloor(var1), PZMath.fastfloor(var2));
         return var4;
      }
   }

   void moveAnimal(VirtualAnimal var1, float var2, float var3) {
      this.removeFromChunk(var1);
      var1.setX(var2);
      var1.setY(var3);
      if (this.isChunkLoadedWorldPos(PZMath.fastfloor(var2), PZMath.fastfloor(var3)) && !this.onEdgeOfLoadedArea(PZMath.fastfloor(var2), PZMath.fastfloor(var3))) {
         ArrayList var4 = new ArrayList();
         var4.add(var1);
         var1.setRemoved(true);
         this.passToMain(var4);
      } else {
         this.addAnimal(var1);
      }
   }

   void removeFromChunk(VirtualAnimal var1) {
      AnimalCell var2 = this.getCellFromSquarePos(PZMath.fastfloor(var1.getX()), PZMath.fastfloor(var1.getY()));
      if (var2 != null) {
         this.loadIfNeeded(var2);
         AnimalChunk var3 = var2.getChunkFromSquarePos(PZMath.fastfloor(var1.getX()), PZMath.fastfloor(var1.getY()));
         if (var3 != null) {
            if (var3.m_animals.size() + var3.m_animalTracks.size() == 1) {
               AnimalZones.removeAnimalChunk(var3);
            }

            var3.m_animals.remove(var1);
         }
      }
   }

   void passToMain(ArrayList<VirtualAnimal> var1) {
      AnimalManagerMain.getInstance().fromWorker(var1);
   }

   boolean isChunkLoadedWorldPos(int var1, int var2) {
      AnimalCell var3 = this.getCellFromSquarePos(var1, var2);
      return var3 == null ? false : var3.isChunkLoadedWorldPos(var1, var2);
   }

   boolean onEdgeOfLoadedArea(int var1, int var2) {
      return false;
   }

   static {
      CHUNKS_PER_CELL = IsoCell.CellSizeInChunks;
      SQUARES_PER_CELL = CHUNKS_PER_CELL * 8;
   }
}
