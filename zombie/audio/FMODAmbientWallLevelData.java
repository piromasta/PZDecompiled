package zombie.audio;

import java.util.ArrayList;
import java.util.List;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkLevel;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.objects.IsoBarricade;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoWindow;
import zombie.util.IPooledObject;
import zombie.util.Pool;
import zombie.util.PooledObject;

public final class FMODAmbientWallLevelData extends PooledObject {
   private static final Pool<FMODAmbientWallLevelData> s_levelDataPool = new Pool(FMODAmbientWallLevelData::new);
   private static final Pool<FMODAmbientWall> s_wallPool = new Pool(FMODAmbientWall::new);
   IsoChunkLevel m_chunkLevel;
   final ArrayList<FMODAmbientWall> m_walls = new ArrayList();
   boolean bDirty = true;

   public FMODAmbientWallLevelData() {
   }

   public FMODAmbientWallLevelData init(IsoChunkLevel var1) {
      this.m_chunkLevel = var1;
      return this;
   }

   public void checkDirty() {
      if (this.bDirty) {
         this.bDirty = false;
         this.recreate();
      }

   }

   void recreate() {
      IPooledObject.release((List)this.m_walls);
      IsoChunk var1 = this.m_chunkLevel.getChunk();
      int var2 = this.m_chunkLevel.getLevel();
      IsoGridSquare[] var3 = var1.getSquaresForLevel(var2);
      byte var4 = 8;

      int var5;
      FMODAmbientWall var6;
      int var7;
      IsoGridSquare var8;
      for(var5 = 0; var5 < var4; ++var5) {
         var6 = null;

         for(var7 = 0; var7 < var4; ++var7) {
            var8 = var3[var7 + var5 * var4];
            if (this.shouldAddNorth(var8)) {
               if (var6 == null) {
                  var6 = (FMODAmbientWall)s_wallPool.alloc();
                  var6.owner = this;
                  var6.x1 = var8.x;
                  var6.y1 = var8.y;
               }
            } else if (var6 != null) {
               var6.x2 = var1.wx * var4 + var7;
               var6.y2 = var1.wy * var4 + var5;
               this.m_walls.add(var6);
               var6 = null;
            }
         }

         if (var6 != null) {
            var6.x2 = var1.wx * var4 + var4;
            var6.y2 = var1.wy * var4 + var5;
            this.m_walls.add(var6);
         }
      }

      for(var5 = 0; var5 < var4; ++var5) {
         var6 = null;

         for(var7 = 0; var7 < var4; ++var7) {
            var8 = var3[var5 + var7 * var4];
            if (this.shouldAddWest(var8)) {
               if (var6 == null) {
                  var6 = (FMODAmbientWall)s_wallPool.alloc();
                  var6.owner = this;
                  var6.x1 = var8.x;
                  var6.y1 = var8.y;
               }
            } else if (var6 != null) {
               var6.x2 = var1.wx * var4 + var5;
               var6.y2 = var1.wy * var4 + var7;
               this.m_walls.add(var6);
               var6 = null;
            }
         }

         if (var6 != null) {
            var6.x2 = var1.wx * var4 + var5;
            var6.y2 = var1.wy * var4 + var4;
            this.m_walls.add(var6);
         }
      }

   }

   boolean shouldAddNorth(IsoGridSquare var1) {
      if (var1 == null) {
         return false;
      } else {
         IsoGridSquare var2 = var1.getAdjacentSquare(IsoDirections.N);
         return var2 != null && isOutside(var1) != isOutside(var2) ? passesSoundNorth(var1, true) : false;
      }
   }

   public static boolean passesSoundNorth(IsoGridSquare var0, boolean var1) {
      if (var0 == null) {
         return false;
      } else {
         IsoObject var2;
         if (var0.getProperties().Is(IsoFlagType.WallN)) {
            var2 = var0.getWall(true);
            if (var2 != null) {
               return var2.getProperties().Is(IsoFlagType.HoppableN) || var2.getProperties().Is(IsoFlagType.SpearOnlyAttackThrough);
            }
         }

         if (var0.getProperties().Is(IsoFlagType.WallNW)) {
            return false;
         } else {
            if (!var1) {
               if (var0.Is(IsoFlagType.doorN)) {
                  var2 = var0.getDoor(true);
                  if (isDoorBlocked(var2)) {
                     return false;
                  }
               }

               if (var0.Is(IsoFlagType.WindowN) && isWindowBlocked(var0.getWindow(true))) {
                  return false;
               }
            }

            return true;
         }
      }
   }

   boolean shouldAddWest(IsoGridSquare var1) {
      if (var1 == null) {
         return false;
      } else {
         IsoGridSquare var2 = var1.getAdjacentSquare(IsoDirections.W);
         return var2 != null && isOutside(var1) != isOutside(var2) ? passesSoundWest(var1, true) : false;
      }
   }

   public static boolean passesSoundWest(IsoGridSquare var0, boolean var1) {
      if (var0 == null) {
         return false;
      } else {
         IsoObject var2;
         if (var0.getProperties().Is(IsoFlagType.WallW)) {
            var2 = var0.getWall(false);
            if (var2 != null) {
               return var2.getProperties().Is(IsoFlagType.HoppableW) || var2.getProperties().Is(IsoFlagType.SpearOnlyAttackThrough);
            }
         }

         if (var0.getProperties().Is(IsoFlagType.WallNW)) {
            return false;
         } else {
            if (!var1) {
               if (var0.Is(IsoFlagType.doorW)) {
                  var2 = var0.getDoor(false);
                  if (isDoorBlocked(var2)) {
                     return false;
                  }
               }

               if (var0.Is(IsoFlagType.WindowW) && isWindowBlocked(var0.getWindow(false))) {
                  return false;
               }
            }

            return true;
         }
      }
   }

   public static boolean isOutside(IsoGridSquare var0) {
      if (var0 == null) {
         return false;
      } else if (var0.getRoom() != null) {
         return false;
      } else if (var0.haveRoof && var0.associatedBuilding == null) {
         return false;
      } else {
         if (var0.haveRoof) {
            for(int var1 = var0.getZ() - 1; var1 >= 0; --var1) {
               IsoGridSquare var2 = IsoWorld.instance.CurrentCell.getGridSquare(var0.getX(), var0.getY(), var1);
               if (var2 != null && var2.getRoom() != null) {
                  return false;
               }
            }
         }

         return true;
      }
   }

   static boolean isDoorBlocked(IsoObject var0) {
      if (var0 instanceof IsoDoor var2) {
         return !var2.IsOpen();
      } else if (var0 instanceof IsoThumpable var1) {
         return !var1.IsOpen();
      } else {
         return false;
      }
   }

   static boolean isWindowBlocked(IsoWindow var0) {
      if (var0 == null) {
         return false;
      } else if (!var0.IsOpen() && !var0.isDestroyed()) {
         return true;
      } else {
         IsoBarricade var1 = var0.getBarricadeOnSameSquare();
         if (var1 != null && var1.isMetal()) {
            return true;
         } else {
            IsoBarricade var2 = var0.getBarricadeOnOppositeSquare();
            if (var2 != null && var2.isMetal()) {
               return true;
            } else {
               int var3 = var1 == null ? 0 : var1.getNumPlanks();
               int var4 = var2 == null ? 0 : var2.getNumPlanks();
               return var3 == 4 || var4 == 4;
            }
         }
      }
   }

   public static FMODAmbientWallLevelData alloc() {
      return (FMODAmbientWallLevelData)s_levelDataPool.alloc();
   }

   public void onReleased() {
      IPooledObject.release((List)this.m_walls);
      this.bDirty = true;
   }

   public static final class FMODAmbientWall extends PooledObject {
      FMODAmbientWallLevelData owner;
      public int x1;
      public int y1;
      public int x2;
      public int y2;

      public FMODAmbientWall() {
      }

      public boolean isHorizontal() {
         return this.y1 == this.y2;
      }
   }
}
