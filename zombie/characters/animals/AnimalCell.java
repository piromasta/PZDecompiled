package zombie.characters.animals;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.ZomboidFileSystem;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.utils.BooleanGrid;
import zombie.iso.IsoCell;
import zombie.iso.IsoMetaCell;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoWorld;
import zombie.iso.SliceY;
import zombie.popman.ObjectPool;

public final class AnimalCell {
   protected static final int SQUARES_PER_CHUNK = 8;
   protected static final int CHUNKS_PER_CELL;
   protected static final int SQUARES_PER_CELL;
   int m_x;
   int m_y;
   AnimalChunk[] m_chunks;
   boolean m_bLoaded = false;
   boolean m_bFileLoaded = false;
   boolean m_bDataChanged = false;
   long m_loadedTime = 0L;
   final BooleanGrid m_loadedChunks;
   final ArrayList<VirtualAnimal> m_saveRealAnimalHack;
   final ArrayList<IsoAnimal> m_animalList;
   final ArrayList<IsoAnimal> m_animalListToReattach;
   boolean m_bAddedJunctions;
   static final ObjectPool<AnimalCell> pool;

   public AnimalCell() {
      this.m_loadedChunks = new BooleanGrid(CHUNKS_PER_CELL, CHUNKS_PER_CELL);
      this.m_saveRealAnimalHack = new ArrayList();
      this.m_animalList = new ArrayList();
      this.m_animalListToReattach = new ArrayList();
      this.m_bAddedJunctions = false;
   }

   AnimalCell init(int var1, int var2) {
      this.m_x = var1;
      this.m_y = var2;
      return this;
   }

   void save() {
      if (this.isLoaded() && !Core.getInstance().isNoSave()) {
         String var1 = ZomboidFileSystem.instance.getFileNameInCurrentSave("apop", "apop_" + this.m_x + "_" + this.m_y + ".bin");

         try {
            FileOutputStream var2 = new FileOutputStream(var1);

            try {
               BufferedOutputStream var3 = new BufferedOutputStream(var2);

               try {
                  ByteBuffer var4 = SliceY.SliceBuffer;
                  var4.clear();
                  var4.putInt(219);
                  ArrayList var5 = new ArrayList();
                  int var6 = 0;

                  while(true) {
                     if (var6 >= this.m_chunks.length) {
                        this.m_saveRealAnimalHack.clear();
                        var3.write(var4.array(), 0, var4.position());
                        break;
                     }

                     if (this.m_saveRealAnimalHack.isEmpty()) {
                        this.m_chunks[var6].save(var4);
                     } else {
                        var5.clear();

                        for(int var7 = 0; var7 < this.m_saveRealAnimalHack.size(); ++var7) {
                           VirtualAnimal var8 = (VirtualAnimal)this.m_saveRealAnimalHack.get(var7);
                           AnimalChunk var9 = this.getChunkFromSquarePos(PZMath.fastfloor(var8.getX()), PZMath.fastfloor(var8.getY()));
                           if (var9 == this.m_chunks[var6]) {
                              var5.add(var8);
                           }
                        }

                        this.m_chunks[var6].save(var4, var5);
                     }

                     ++var6;
                  }
               } catch (Throwable var12) {
                  try {
                     var3.close();
                  } catch (Throwable var11) {
                     var12.addSuppressed(var11);
                  }

                  throw var12;
               }

               var3.close();
            } catch (Throwable var13) {
               try {
                  var2.close();
               } catch (Throwable var10) {
                  var13.addSuppressed(var10);
               }

               throw var13;
            }

            var2.close();
         } catch (IOException var14) {
            ExceptionLogger.logException(var14);
         }

      }
   }

   void load() {
      assert !this.isLoaded();

      this.m_bLoaded = true;
      this.m_chunks = new AnimalChunk[CHUNKS_PER_CELL * CHUNKS_PER_CELL];

      for(int var1 = 0; var1 < this.m_chunks.length; ++var1) {
         this.m_chunks[var1] = AnimalChunk.alloc().init(this.m_x * CHUNKS_PER_CELL + var1 % CHUNKS_PER_CELL, this.m_y * CHUNKS_PER_CELL + var1 / CHUNKS_PER_CELL);
         this.m_chunks[var1].cell = this;
      }

      this.checkAnimalZonesGenerated();
      String var2 = ZomboidFileSystem.instance.getFileNameInCurrentSave("apop", "apop_" + this.m_x + "_" + this.m_y + ".bin");
      this.m_bFileLoaded = this.load(var2);
      if (!this.m_bFileLoaded) {
         AnimalZones.getInstance().spawnAnimalsInCell(this);
      }

   }

   private void checkAnimalZonesGenerated() {
      IsoMetaGrid var1 = IsoWorld.instance.getMetaGrid();
      IsoMetaCell var2 = IsoWorld.instance.getMetaGrid().getCellData(this.m_x, this.m_y);
      if (var2 == null) {
         var2 = new IsoMetaCell(this.m_x, this.m_y);
         var1.setCellData(this.m_x, this.m_y, var2);
      }

      int var3 = this.m_x * IsoCell.CellSizeInChunks;
      int var4 = this.m_y * IsoCell.CellSizeInChunks;
      var2.checkAnimalZonesGenerated(var3, var4);
   }

   boolean load(String var1) {
      try {
         FileInputStream var2 = new FileInputStream(var1);

         boolean var15;
         try {
            BufferedInputStream var3 = new BufferedInputStream(var2);

            try {
               ByteBuffer var4 = SliceY.SliceBuffer;
               var4.clear();
               int var5 = var3.read(var4.array());
               var4.limit(var5);
               this.load(var4);
               int var6 = 0;

               while(true) {
                  if (var6 >= this.m_animalListToReattach.size()) {
                     var15 = true;
                     break;
                  }

                  for(int var7 = 0; var7 < this.m_animalList.size(); ++var7) {
                     IsoAnimal var8 = (IsoAnimal)this.m_animalList.get(var7);
                     if (var8.animalID == ((IsoAnimal)this.m_animalListToReattach.get(var6)).attachBackToMother) {
                        ((IsoAnimal)this.m_animalListToReattach.get(var6)).setMother(var8);
                        break;
                     }
                  }

                  ++var6;
               }
            } catch (Throwable var11) {
               try {
                  var3.close();
               } catch (Throwable var10) {
                  var11.addSuppressed(var10);
               }

               throw var11;
            }

            var3.close();
         } catch (Throwable var12) {
            try {
               var2.close();
            } catch (Throwable var9) {
               var12.addSuppressed(var9);
            }

            throw var12;
         }

         var2.close();
         return var15;
      } catch (FileNotFoundException var13) {
         return false;
      } catch (Exception var14) {
         ExceptionLogger.logException(var14);
         return false;
      }
   }

   void load(ByteBuffer var1) throws IOException {
      int var2 = var1.getInt();

      for(int var3 = 0; var3 < this.m_chunks.length; ++var3) {
         this.m_chunks[var3].cell = this;
         this.m_chunks[var3].load(var1, var2);
      }

   }

   void unload() {
      assert this.isLoaded();

      if (this.m_bDataChanged) {
         this.m_bDataChanged = false;
         this.save();
      }

      for(int var1 = 0; var1 < this.m_chunks.length; ++var1) {
         this.m_chunks[var1].release();
         this.m_chunks[var1] = null;
      }

      this.m_bLoaded = false;
   }

   boolean isLoaded() {
      return this.m_bLoaded;
   }

   AnimalChunk getChunkFromCellPos(int var1, int var2) {
      if (!this.isLoaded()) {
         return null;
      } else if (var1 >= 0 && var1 < SQUARES_PER_CELL && var2 >= 0 && var2 < SQUARES_PER_CELL) {
         var1 /= 8;
         var2 /= 8;
         return this.m_chunks[var1 + var2 * CHUNKS_PER_CELL];
      } else {
         return null;
      }
   }

   AnimalChunk getChunkFromChunkPos(int var1, int var2) {
      if (!this.isLoaded()) {
         return null;
      } else {
         var1 -= this.m_x * CHUNKS_PER_CELL;
         var2 -= this.m_y * CHUNKS_PER_CELL;
         return var1 >= 0 && var1 < CHUNKS_PER_CELL && var2 >= 0 && var2 < CHUNKS_PER_CELL ? this.m_chunks[var1 + var2 * CHUNKS_PER_CELL] : null;
      }
   }

   AnimalChunk getChunkFromSquarePos(int var1, int var2) {
      var1 -= this.m_x * SQUARES_PER_CELL;
      var2 -= this.m_y * SQUARES_PER_CELL;
      return this.getChunkFromCellPos(var1, var2);
   }

   void setChunkLoaded(int var1, int var2, boolean var3) {
      var1 -= this.m_x * CHUNKS_PER_CELL;
      var2 -= this.m_y * CHUNKS_PER_CELL;
      this.m_loadedChunks.setValue(var1, var2, var3);
   }

   boolean isChunkLoadedChunkPos(int var1, int var2) {
      var1 -= this.m_x * CHUNKS_PER_CELL;
      var2 -= this.m_y * CHUNKS_PER_CELL;
      return this.m_loadedChunks.getValue(var1, var2);
   }

   boolean isChunkLoadedWorldPos(int var1, int var2) {
      var1 -= this.m_x * SQUARES_PER_CELL;
      var2 -= this.m_y * SQUARES_PER_CELL;
      var1 /= 8;
      var2 /= 8;
      return this.m_loadedChunks.getValue(var1, var2);
   }

   static AnimalCell alloc() {
      return (AnimalCell)pool.alloc();
   }

   void release() {
      if (this.isLoaded()) {
         this.unload();
      }

      this.m_chunks = null;
      this.m_bLoaded = false;
      this.m_bFileLoaded = false;
      this.m_bDataChanged = false;
      this.m_loadedTime = 0L;
      this.m_loadedChunks.clear();
      this.m_saveRealAnimalHack.clear();
      this.m_animalList.clear();
      this.m_animalListToReattach.clear();
      this.m_bAddedJunctions = false;
      pool.release((Object)this);
   }

   static {
      CHUNKS_PER_CELL = IsoCell.CellSizeInChunks;
      SQUARES_PER_CELL = CHUNKS_PER_CELL * 8;
      pool = new ObjectPool(AnimalCell::new);
   }
}
