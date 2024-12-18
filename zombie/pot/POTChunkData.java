package zombie.pot;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

public final class POTChunkData {
   static final int FILE_VERSION = 1;
   static final int BIT_SOLID = 1;
   static final int BIT_WALLN = 2;
   static final int BIT_WALLW = 4;
   static final int BIT_WATER = 8;
   static final int BIT_ROOM = 16;
   static final int EMPTY_CHUNK = 0;
   static final int SOLID_CHUNK = 1;
   static final int REGULAR_CHUNK = 2;
   static final int WATER_CHUNK = 3;
   static final int ROOM_CHUNK = 4;
   static final int NUM_CHUNK_TYPES = 5;
   public final boolean pot;
   public final int CHUNK_DIM;
   public final int CHUNKS_PER_CELL;
   public final int CELL_DIM;
   public final int x;
   public final int y;
   public final Chunk[] chunks;

   POTChunkData(int var1, int var2, boolean var3) {
      this.CHUNK_DIM = var3 ? 8 : 10;
      this.CHUNKS_PER_CELL = var3 ? 32 : 30;
      this.CELL_DIM = var3 ? 256 : 300;
      this.pot = var3;
      this.x = var1;
      this.y = var2;
      this.chunks = new Chunk[this.CHUNKS_PER_CELL * this.CHUNKS_PER_CELL];

      for(int var4 = 0; var4 < this.chunks.length; ++var4) {
         this.chunks[var4] = new Chunk();
      }

   }

   void load(File var1) throws IOException {
      FileInputStream var2 = new FileInputStream(var1);

      try {
         DataInputStream var3 = new DataInputStream(var2);

         try {
            short var4 = var3.readShort();

            assert var4 == 1;

            for(int var5 = 0; var5 < this.CHUNKS_PER_CELL; ++var5) {
               for(int var6 = 0; var6 < this.CHUNKS_PER_CELL; ++var6) {
                  this.chunks[var6 + var5 * this.CHUNKS_PER_CELL].load(var3);
               }
            }
         } catch (Throwable var9) {
            try {
               var3.close();
            } catch (Throwable var8) {
               var9.addSuppressed(var8);
            }

            throw var9;
         }

         var3.close();
      } catch (Throwable var10) {
         try {
            var2.close();
         } catch (Throwable var7) {
            var10.addSuppressed(var7);
         }

         throw var10;
      }

      var2.close();
   }

   void save(String var1) throws IOException {
      FileOutputStream var2 = new FileOutputStream(var1);

      try {
         DataOutputStream var3 = new DataOutputStream(var2);

         try {
            var3.writeShort(1);

            for(int var4 = 0; var4 < this.CHUNKS_PER_CELL; ++var4) {
               for(int var5 = 0; var5 < this.CHUNKS_PER_CELL; ++var5) {
                  this.chunks[var5 + var4 * this.CHUNKS_PER_CELL].save(var3);
               }
            }
         } catch (Throwable var8) {
            try {
               var3.close();
            } catch (Throwable var7) {
               var8.addSuppressed(var7);
            }

            throw var8;
         }

         var3.close();
      } catch (Throwable var9) {
         try {
            var2.close();
         } catch (Throwable var6) {
            var9.addSuppressed(var6);
         }

         throw var9;
      }

      var2.close();
   }

   int getMinSquareX() {
      return this.x * this.CELL_DIM;
   }

   int getMinSquareY() {
      return this.y * this.CELL_DIM;
   }

   int getMaxSquareX() {
      return (this.x + 1) * this.CELL_DIM - 1;
   }

   int getMaxSquareY() {
      return (this.y + 1) * this.CELL_DIM - 1;
   }

   boolean containsSquare(int var1, int var2) {
      return var1 >= this.getMinSquareX() && var1 <= this.getMaxSquareX() && var2 >= this.getMinSquareY() && var2 <= this.getMaxSquareY();
   }

   byte getSquareBits(int var1, int var2) {
      if (!this.containsSquare(var1, var2)) {
         return 0;
      } else {
         int var3 = (var1 - this.getMinSquareX()) / this.CHUNK_DIM;
         int var4 = (var2 - this.getMinSquareY()) / this.CHUNK_DIM;
         Chunk var5 = this.chunks[var3 + var4 * this.CHUNKS_PER_CELL];
         return var5.getBits((var1 - this.getMinSquareX()) % this.CHUNK_DIM, (var2 - this.getMinSquareY()) % this.CHUNK_DIM);
      }
   }

   void setSquareBits(int var1, int var2, byte var3) {
      int var4 = (var1 - this.getMinSquareX()) / this.CHUNK_DIM;
      int var5 = (var2 - this.getMinSquareY()) / this.CHUNK_DIM;
      Chunk var6 = this.chunks[var4 + var5 * this.CHUNKS_PER_CELL];
      var6.setBits((var1 - this.getMinSquareX()) % this.CHUNK_DIM, (var2 - this.getMinSquareY()) % this.CHUNK_DIM, var3);
   }

   final class Chunk {
      public final int[] counts;
      public byte[] bits;
      final int NSQRS;

      Chunk() {
         this.NSQRS = POTChunkData.this.CHUNK_DIM * POTChunkData.this.CHUNK_DIM;
         this.counts = new int[5];
         this.counts[0] = this.NSQRS;
      }

      void load(DataInputStream var1) throws IOException {
         Arrays.fill(this.counts, 0);
         byte var2 = var1.readByte();
         if (var2 != 0 && var2 != 1 && var2 != 3 && var2 != 4) {
            assert var2 == 2;

            this.bits = new byte[this.NSQRS];

            for(int var3 = 0; var3 < this.NSQRS; ++var3) {
               this.bits[var3] = var1.readByte();
               int var10002 = this.counts[this.getTypeOf(this.bits[var3])]++;
            }
         } else {
            this.counts[var2] = this.NSQRS;
         }

      }

      void save(DataOutputStream var1) throws IOException {
         int var2 = this.getType();
         var1.writeByte(var2);
         if (var2 == 2) {
            var1.write(this.bits);
         }

      }

      byte getBits(int var1, int var2) {
         if (this.counts[0] == this.NSQRS) {
            return 0;
         } else if (this.counts[1] == this.NSQRS) {
            return 1;
         } else if (this.counts[3] == this.NSQRS) {
            return 8;
         } else {
            return this.counts[4] == this.NSQRS ? 16 : this.bits[var1 + var2 * POTChunkData.this.CHUNK_DIM];
         }
      }

      byte setBits(int var1, int var2, byte var3) {
         byte var4 = this.getBits(var1, var2);
         int var5 = this.getTypeOf(var4);
         int var6 = this.getTypeOf(var3);
         if (var5 == var6 && var5 != 2) {
            return var3;
         } else {
            assert this.counts[var5] > 0;

            assert this.counts[var6] < this.NSQRS;

            int var10002 = this.counts[var5]--;
            var10002 = this.counts[var6]++;
            if (this.getType() == 2) {
               if (this.bits == null) {
                  this.bits = new byte[this.NSQRS];
                  Arrays.fill(this.bits, var4);
               }

               this.bits[var1 + var2 * POTChunkData.this.CHUNK_DIM] = var3;
            } else {
               this.bits = null;
            }

            return var4;
         }
      }

      int getType() {
         if (this.counts[0] == this.NSQRS) {
            return 0;
         } else if (this.counts[1] == this.NSQRS) {
            return 1;
         } else if (this.counts[3] == this.NSQRS) {
            return 3;
         } else {
            return this.counts[4] == this.NSQRS ? 4 : 2;
         }
      }

      int getTypeOf(byte var1) {
         if (var1 == 0) {
            return 0;
         } else if (var1 == 1) {
            return 1;
         } else if (var1 == 8) {
            return 3;
         } else {
            return var1 == 16 ? 4 : 2;
         }
      }
   }
}
