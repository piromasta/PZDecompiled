package zombie.pathfind.nativeCode;

import java.nio.ByteBuffer;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.popman.ObjectPool;

class ChunkUpdateTask implements IPathfindTask {
   protected static final int SQUARES_PER_CHUNK = 8;
   protected static final int LEVELS_PER_CHUNK = 64;
   int wx;
   int wy;
   short loadID;
   ByteBuffer bb;
   static ByteBuffer bbTemp;
   private static final int BLOCK_SIZE = 256;
   static final ObjectPool<ChunkUpdateTask> pool = new ObjectPool(ChunkUpdateTask::new);

   ChunkUpdateTask() {
   }

   private static int bufferSize(int var0) {
      return (var0 + 256 - 1) / 256 * 256;
   }

   private static ByteBuffer ensureCapacity(ByteBuffer var0, int var1) {
      if (var0 == null || var0.capacity() < var1) {
         var0 = ByteBuffer.allocateDirect(bufferSize(var1));
      }

      return var0;
   }

   private static ByteBuffer ensureCapacity(ByteBuffer var0) {
      if (var0 == null) {
         return ByteBuffer.allocateDirect(256);
      } else if (var0.capacity() - var0.position() < 256) {
         ByteBuffer var1 = ensureCapacity((ByteBuffer)null, var0.position() + 256);
         var1.put(0, var0, 0, var0.position());
         return var1.position(var0.position());
      } else {
         return var0;
      }
   }

   ChunkUpdateTask init(IsoChunk var1) {
      this.wx = var1.wx;
      this.wy = var1.wy;
      this.loadID = var1.getLoadID();
      this.bb = ensureCapacity(this.bb);
      this.bb.clear();
      this.bb.putInt(var1.minLevel + 32);
      this.bb.putInt(var1.maxLevel + 32);
      bbTemp = ensureCapacity(bbTemp);
      bbTemp.clear();
      int var2 = 0;

      int var3;
      for(var3 = var1.minLevel; var3 <= var1.maxLevel; ++var3) {
         for(int var4 = 0; var4 < 8; ++var4) {
            for(int var5 = 0; var5 < 8; ++var5) {
               this.bb = ensureCapacity(this.bb);
               IsoGridSquare var6 = var1.getGridSquare(var5, var4, var3);
               if (var6 == null) {
                  this.bb.putInt(0);
                  this.bb.putShort((short)0);
               } else {
                  int var7 = SquareUpdateTask.getBits(var6);
                  short var8 = SquareUpdateTask.getCost(var6);
                  this.bb.putInt(var7);
                  this.bb.putShort(var8);
                  IsoDirections var9 = var6.getSlopedSurfaceDirection();
                  if (var9 != null) {
                     bbTemp = ensureCapacity(bbTemp);
                     bbTemp.put((byte)var5);
                     bbTemp.put((byte)var4);
                     bbTemp.put((byte)(var3 + 32));
                     bbTemp.put((byte)var9.indexUnmodified());
                     bbTemp.putFloat(var6.getSlopedSurfaceHeightMin());
                     bbTemp.putFloat(var6.getSlopedSurfaceHeightMax());
                     ++var2;
                  }
               }
            }
         }
      }

      this.bb.putShort((short)var2);
      if (var2 > 0) {
         var3 = this.bb.position() + bbTemp.position();
         if (var3 > this.bb.capacity()) {
            ByteBuffer var10 = ByteBuffer.allocateDirect(bufferSize(var3));
            var10.put(0, this.bb, 0, this.bb.position());
            var10.position(this.bb.position());
            this.bb = var10;
         }

         this.bb.put(this.bb.position(), bbTemp, 0, bbTemp.position());
         this.bb.position(this.bb.position() + bbTemp.position());
      }

      this.bb.flip();
      return this;
   }

   public void execute() {
      PathfindNative.updateChunk(this.loadID, this.wx, this.wy, this.bb);
   }

   static ChunkUpdateTask alloc() {
      return (ChunkUpdateTask)pool.alloc();
   }

   public void release() {
      pool.release((Object)this);
   }
}
