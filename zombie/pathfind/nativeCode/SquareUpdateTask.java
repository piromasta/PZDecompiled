package zombie.pathfind.nativeCode;

import zombie.characters.IsoGameCharacter;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWindowFrame;
import zombie.popman.ObjectPool;
import zombie.util.Type;

class SquareUpdateTask implements IPathfindTask {
   private static final int BIT_SOLID = 1;
   private static final int BIT_COLLIDE_W = 2;
   private static final int BIT_COLLIDE_N = 4;
   private static final int BIT_STAIR_TW = 8;
   private static final int BIT_STAIR_MW = 16;
   private static final int BIT_STAIR_BW = 32;
   private static final int BIT_STAIR_TN = 64;
   private static final int BIT_STAIR_MN = 128;
   private static final int BIT_STAIR_BN = 256;
   private static final int BIT_SOLID_FLOOR = 512;
   private static final int BIT_SOLID_TRANS = 1024;
   private static final int BIT_WINDOW_W = 2048;
   private static final int BIT_WINDOW_N = 4096;
   private static final int BIT_CAN_PATH_W = 8192;
   private static final int BIT_CAN_PATH_N = 16384;
   private static final int BIT_THUMP_W = 32768;
   private static final int BIT_THUMP_N = 65536;
   private static final int BIT_THUMPABLE = 131072;
   private static final int BIT_DOOR_E = 262144;
   private static final int BIT_DOOR_S = 524288;
   private static final int BIT_WINDOW_W_UNBLOCKED = 1048576;
   private static final int BIT_WINDOW_N_UNBLOCKED = 2097152;
   private static final int BIT_DOOR_W_UNBLOCKED = 4194304;
   private static final int BIT_DOOR_N_UNBLOCKED = 8388608;
   private static final int BIT_HOPPABLE_N = 16777216;
   private static final int BIT_HOPPABLE_W = 33554432;
   private static final int ALL_SOLID_BITS = 1025;
   private static final int ALL_STAIR_BITS = 504;
   int x;
   int y;
   int z;
   int bits;
   short cost;
   IsoDirections slopedSurfaceDirection;
   float slopedSurfaceHeightMin;
   float slopedSurfaceHeightMax;
   short loadID;
   static final ObjectPool<SquareUpdateTask> pool = new ObjectPool(SquareUpdateTask::new);

   SquareUpdateTask() {
   }

   SquareUpdateTask init(IsoGridSquare var1) {
      this.x = var1.x;
      this.y = var1.y;
      this.z = var1.z + 32;
      this.bits = getBits(var1);
      this.cost = getCost(var1);
      this.slopedSurfaceDirection = var1.getSlopedSurfaceDirection();
      if (this.slopedSurfaceDirection == null) {
         this.slopedSurfaceDirection = IsoDirections.Max;
      }

      this.slopedSurfaceHeightMin = var1.getSlopedSurfaceHeightMin();
      this.slopedSurfaceHeightMax = var1.getSlopedSurfaceHeightMax();
      this.loadID = var1.chunk.getLoadID();
      return this;
   }

   public void execute() {
      PathfindNative.updateSquare(this.loadID, this.x, this.y, this.z, this.bits, this.cost, this.slopedSurfaceDirection.indexUnmodified(), this.slopedSurfaceHeightMin, this.slopedSurfaceHeightMax);
   }

   static int getBits(IsoGridSquare var0) {
      int var1 = 0;
      if (var0.Is(IsoFlagType.solidfloor)) {
         var1 |= 512;
      }

      if (var0.isSolid()) {
         var1 |= 1;
      }

      if (var0.isSolidTrans()) {
         var1 |= 1024;
      }

      if (var0.Is(IsoFlagType.collideW)) {
         var1 |= 2;
      }

      if (var0.Is(IsoFlagType.collideN)) {
         var1 |= 4;
      }

      if (var0.Has(IsoObjectType.stairsTW)) {
         var1 |= 8;
      }

      if (var0.Has(IsoObjectType.stairsMW)) {
         var1 |= 16;
      }

      if (var0.Has(IsoObjectType.stairsBW)) {
         var1 |= 32;
      }

      if (var0.Has(IsoObjectType.stairsTN)) {
         var1 |= 64;
      }

      if (var0.Has(IsoObjectType.stairsMN)) {
         var1 |= 128;
      }

      if (var0.Has(IsoObjectType.stairsBN)) {
         var1 |= 256;
      }

      if (var0.Is(IsoFlagType.HoppableN)) {
         var1 |= 16777216;
      }

      if (var0.Is(IsoFlagType.HoppableW)) {
         var1 |= 33554432;
      }

      if (var0.Is(IsoFlagType.windowW) || var0.Is(IsoFlagType.WindowW)) {
         var1 |= 2050;
         if (isWindowUnblocked(var0, false)) {
            var1 |= 1048576;
         }
      }

      if (var0.Is(IsoFlagType.windowN) || var0.Is(IsoFlagType.WindowN)) {
         var1 |= 4100;
         if (isWindowUnblocked(var0, true)) {
            var1 |= 2097152;
         }
      }

      if (var0.Is(IsoFlagType.canPathW)) {
         var1 |= 8192;
      }

      if (var0.Is(IsoFlagType.canPathN)) {
         var1 |= 16384;
      }

      boolean var2 = false;
      boolean var3 = false;

      for(int var4 = 0; var4 < var0.getSpecialObjects().size(); ++var4) {
         IsoObject var5 = (IsoObject)var0.getSpecialObjects().get(var4);
         IsoDirections var6 = IsoDirections.Max;
         if (var5 instanceof IsoDoor) {
            var6 = ((IsoDoor)var5).getSpriteEdge(false);
            if (((IsoDoor)var5).IsOpen()) {
               var6 = ((IsoDoor)var5).getSpriteEdge(true);
               if (var6 == IsoDirections.N) {
                  var1 |= 8388608;
               } else if (var6 == IsoDirections.W) {
                  var1 |= 4194304;
               }

               var6 = IsoDirections.Max;
            }
         } else if (var5 instanceof IsoThumpable && ((IsoThumpable)var5).isDoor()) {
            var6 = ((IsoThumpable)var5).getSpriteEdge(false);
            if (((IsoThumpable)var5).IsOpen()) {
               var6 = ((IsoThumpable)var5).getSpriteEdge(true);
               if (var6 == IsoDirections.N) {
                  var1 |= 8388608;
               } else if (var6 == IsoDirections.W) {
                  var1 |= 4194304;
               }

               var6 = IsoDirections.Max;
            }
         }

         if (var6 == IsoDirections.W) {
            var1 |= 8192;
            var1 |= 2;
            var2 = true;
         } else if (var6 == IsoDirections.N) {
            var1 |= 16384;
            var1 |= 4;
            var3 = true;
         } else if (var6 == IsoDirections.S) {
            var1 |= 524288;
         } else if (var6 == IsoDirections.E) {
            var1 |= 262144;
         }
      }

      if (var0.Is(IsoFlagType.DoorWallW)) {
         var1 |= 8192;
         var1 |= 2;
         if (!var2) {
            var1 |= 4194304;
         }
      }

      if (var0.Is(IsoFlagType.DoorWallN)) {
         var1 |= 16384;
         var1 |= 4;
         if (!var3) {
            var1 |= 8388608;
         }
      }

      if (hasSquareThumpable(var0)) {
         var1 |= 8192;
         var1 |= 16384;
         var1 |= 131072;
      }

      if (hasWallThumpableN(var0)) {
         var1 |= 81920;
      }

      if (hasWallThumpableW(var0)) {
         var1 |= 40960;
      }

      return var1;
   }

   static short getCost(IsoGridSquare var0) {
      short var1 = 0;
      if (var0.HasTree() || var0.hasBush()) {
         var1 = (short)(var1 + 5);
      }

      return var1;
   }

   static boolean isWindowUnblocked(IsoGridSquare var0, boolean var1) {
      for(int var2 = 0; var2 < var0.getSpecialObjects().size(); ++var2) {
         IsoObject var3 = (IsoObject)var0.getSpecialObjects().get(var2);
         if (var3 instanceof IsoThumpable var4) {
            if (var4.isWindow() && var1 == var4.north) {
               if (var4.isBarricaded()) {
                  return false;
               }

               return true;
            }
         }

         if (var3 instanceof IsoWindow var6) {
            if (var1 == var6.north) {
               if (var6.isBarricaded()) {
                  return false;
               }

               if (var6.isInvincible()) {
                  return false;
               }

               if (var6.IsOpen()) {
                  return true;
               }

               if (var6.isDestroyed() && var6.isGlassRemoved()) {
                  return true;
               }

               return false;
            }
         }
      }

      IsoWindowFrame var5 = var0.getWindowFrame(var1);
      if (var5 != null && var5.canClimbThrough((IsoGameCharacter)null)) {
         return true;
      } else {
         return false;
      }
   }

   static boolean hasSquareThumpable(IsoGridSquare var0) {
      int var1;
      for(var1 = 0; var1 < var0.getSpecialObjects().size(); ++var1) {
         IsoThumpable var2 = (IsoThumpable)Type.tryCastTo((IsoObject)var0.getSpecialObjects().get(var1), IsoThumpable.class);
         if (var2 != null && var2.isThumpable() && var2.isBlockAllTheSquare()) {
            return true;
         }
      }

      for(var1 = 0; var1 < var0.getObjects().size(); ++var1) {
         IsoObject var3 = (IsoObject)var0.getObjects().get(var1);
         if (var3.isMovedThumpable()) {
            return true;
         }
      }

      return false;
   }

   static boolean hasWallThumpableN(IsoGridSquare var0) {
      IsoGridSquare var1 = var0.getAdjacentSquare(IsoDirections.N);
      if (var1 == null) {
         return false;
      } else {
         for(int var2 = 0; var2 < var0.getSpecialObjects().size(); ++var2) {
            IsoThumpable var3 = (IsoThumpable)Type.tryCastTo((IsoObject)var0.getSpecialObjects().get(var2), IsoThumpable.class);
            if (var3 != null && !var3.canClimbThrough((IsoGameCharacter)null) && !var3.canClimbOver((IsoGameCharacter)null) && var3.isThumpable() && !var3.isBlockAllTheSquare() && !var3.isDoor() && var3.TestCollide((IsoMovingObject)null, var0, var1)) {
               return true;
            }
         }

         return false;
      }
   }

   static boolean hasWallThumpableW(IsoGridSquare var0) {
      IsoGridSquare var1 = var0.getAdjacentSquare(IsoDirections.W);
      if (var1 == null) {
         return false;
      } else {
         for(int var2 = 0; var2 < var0.getSpecialObjects().size(); ++var2) {
            IsoThumpable var3 = (IsoThumpable)Type.tryCastTo((IsoObject)var0.getSpecialObjects().get(var2), IsoThumpable.class);
            if (var3 != null && !var3.canClimbThrough((IsoGameCharacter)null) && !var3.canClimbOver((IsoGameCharacter)null) && var3.isThumpable() && !var3.isBlockAllTheSquare() && !var3.isDoor() && var3.TestCollide((IsoMovingObject)null, var0, var1)) {
               return true;
            }
         }

         return false;
      }
   }

   static SquareUpdateTask alloc() {
      return (SquareUpdateTask)pool.alloc();
   }

   public void release() {
      pool.release((Object)this);
   }
}
