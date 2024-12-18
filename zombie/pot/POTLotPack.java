package zombie.pot;

import gnu.trove.list.array.TIntArrayList;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import zombie.iso.IsoLot;
import zombie.iso.LotHeader;
import zombie.iso.SliceY;
import zombie.util.BufferedRandomAccessFile;

public final class POTLotPack {
   static File m_lastFile = null;
   static RandomAccessFile m_in = null;
   public final POTLotHeader lotHeader;
   public final boolean pot;
   public final int CHUNK_DIM;
   public final int CHUNKS_PER_CELL;
   public final int CELL_DIM;
   public final int x;
   public final int y;
   int m_version;
   final boolean[] m_loadedChunks;
   final int[] m_offsetInData;
   final TIntArrayList m_data = new TIntArrayList();

   POTLotPack(POTLotHeader var1) {
      this.lotHeader = var1;
      this.pot = var1.pot;
      this.x = var1.x;
      this.y = var1.y;
      this.CHUNK_DIM = this.pot ? 8 : 10;
      this.CHUNKS_PER_CELL = this.pot ? 32 : 30;
      this.CELL_DIM = this.pot ? 256 : 300;
      this.m_loadedChunks = new boolean[this.CHUNKS_PER_CELL * this.CHUNKS_PER_CELL];
      this.m_offsetInData = new int[this.CELL_DIM * this.CELL_DIM * (var1.maxLevel - var1.minLevel + 1)];
      Arrays.fill(this.m_offsetInData, -1);
   }

   void clear() {
      this.m_data.clear();
   }

   void load(File var1) throws IOException {
      if (m_in == null || m_lastFile != var1) {
         if (m_in != null) {
            m_in.close();
         }

         System.out.println(var1.getPath());
         m_in = new BufferedRandomAccessFile(var1, "r", 4096);
         m_lastFile = var1;
      }

      m_in.seek(0L);
      byte[] var2 = new byte[4];
      m_in.read(var2, 0, 4);
      boolean var3 = Arrays.equals(var2, LotHeader.LOTPACK_MAGIC);
      if (var3) {
         this.m_version = IsoLot.readInt(m_in);
         if (this.m_version < 0 || this.m_version > 1) {
            throw new IOException("Unsupported version " + this.m_version);
         }
      } else {
         m_in.seek(0L);
         this.m_version = 0;
      }

      for(int var4 = 0; var4 < this.CHUNKS_PER_CELL; ++var4) {
         for(int var5 = 0; var5 < this.CHUNKS_PER_CELL; ++var5) {
            this.loadChunk(this.x * this.CHUNKS_PER_CELL + var4, this.y * this.CHUNKS_PER_CELL + var5);
         }
      }

   }

   void loadChunk(int var1, int var2) throws IOException {
      int var3 = 0;
      int var4 = var1 - this.x * this.CHUNKS_PER_CELL;
      int var5 = var2 - this.y * this.CHUNKS_PER_CELL;
      int var6 = var4 * this.CHUNKS_PER_CELL + var5;
      m_in.seek((long)((this.m_version >= 1 ? 8 : 0) + 4) + (long)var6 * 8L);
      int var7 = IsoLot.readInt(m_in);
      m_in.seek((long)var7);
      int var8 = Math.max(this.lotHeader.minLevel, -32);
      int var9 = Math.min(this.lotHeader.maxLevel, 31);
      if (this.m_version == 0) {
         --var9;
      }

      for(int var10 = var8; var10 <= var9; ++var10) {
         for(int var11 = 0; var11 < this.CHUNK_DIM; ++var11) {
            for(int var12 = 0; var12 < this.CHUNK_DIM; ++var12) {
               int var13 = var11 + var12 * this.CELL_DIM;
               var13 += var4 * this.CHUNK_DIM + var5 * this.CHUNK_DIM * this.CELL_DIM + (var10 - this.lotHeader.minLevel) * this.CELL_DIM * this.CELL_DIM;
               this.m_offsetInData[var13] = -1;
               if (var3 > 0) {
                  --var3;
               } else {
                  int var14 = IsoLot.readInt(m_in);
                  if (var14 == -1) {
                     var3 = IsoLot.readInt(m_in);
                     if (var3 > 0) {
                        --var3;
                        continue;
                     }
                  }

                  if (var14 > 1) {
                     this.m_offsetInData[var13] = this.m_data.size();
                     this.m_data.add(var14 - 1);
                     int var15 = IsoLot.readInt(m_in);

                     for(int var16 = 1; var16 < var14; ++var16) {
                        int var17 = IsoLot.readInt(m_in);
                        this.m_data.add(var17);
                     }
                  }
               }
            }
         }
      }

   }

   void save(String var1) throws IOException {
      int var2 = this.CHUNKS_PER_CELL * this.CHUNKS_PER_CELL;
      ByteBuffer var3 = SliceY.SliceBuffer;
      var3.order(ByteOrder.LITTLE_ENDIAN);
      var3.clear();
      var3.put(LotHeader.LOTPACK_MAGIC);
      var3.putInt(1);
      var3.putInt(this.CHUNK_DIM);
      int var4 = var3.position();
      var3.position(var4 + var2 * 8);

      for(int var5 = 0; var5 < this.CHUNKS_PER_CELL; ++var5) {
         for(int var6 = 0; var6 < this.CHUNKS_PER_CELL; ++var6) {
            this.saveChunk(var3, var4, var5, var6);
         }
      }

      FileOutputStream var10 = new FileOutputStream(var1);

      try {
         var10.write(var3.array(), 0, var3.position());
      } catch (Throwable var9) {
         try {
            var10.close();
         } catch (Throwable var8) {
            var9.addSuppressed(var8);
         }

         throw var9;
      }

      var10.close();
   }

   void saveChunk(ByteBuffer var1, int var2, int var3, int var4) {
      var1.putLong(var2 + (var3 * this.CHUNKS_PER_CELL + var4) * 8, (long)var1.position());
      int var5 = 0;

      for(int var6 = this.lotHeader.minLevelNotEmpty; var6 <= this.lotHeader.maxLevelNotEmpty; ++var6) {
         for(int var7 = 0; var7 < this.CHUNK_DIM; ++var7) {
            for(int var8 = 0; var8 < this.CHUNK_DIM; ++var8) {
               int var9 = var7 + var8 * this.CELL_DIM + (var6 - this.lotHeader.minLevel) * this.CELL_DIM * this.CELL_DIM;
               var9 += var3 * this.CHUNK_DIM + var4 * this.CHUNK_DIM * this.CELL_DIM;
               int var10 = this.m_offsetInData[var9];
               if (var10 == -1) {
                  ++var5;
               } else {
                  if (var5 > 0) {
                     var1.putInt(-1);
                     var1.putInt(var5);
                     var5 = 0;
                  }

                  int var11 = this.m_data.getQuick(var10);
                  var1.putInt(var11 + 1);
                  byte var12 = -1;
                  var1.putInt(var12);

                  for(int var13 = 0; var13 < var11; ++var13) {
                     var1.putInt(this.m_data.getQuick(var10 + 1 + var13));
                  }
               }
            }
         }
      }

      if (var5 > 0) {
         var1.putInt(-1);
         var1.putInt(var5);
      }

   }

   String[] getSquareData(int var1, int var2, int var3) {
      var1 -= this.lotHeader.getMinSquareX();
      var2 -= this.lotHeader.getMinSquareY();
      int var4 = var1 + var2 * this.CELL_DIM + (var3 - this.lotHeader.minLevel) * this.CELL_DIM * this.CELL_DIM;
      int var5 = this.m_offsetInData[var4];
      if (var5 == -1) {
         return null;
      } else {
         int var6 = this.m_data.getQuick(var5);
         String[] var7 = new String[var6];

         for(int var8 = 0; var8 < var6; ++var8) {
            var7[var8] = (String)this.lotHeader.tilesUsed.get(this.m_data.getQuick(var5 + 1 + var8));
         }

         return var7;
      }
   }

   void setSquareData(int var1, int var2, int var3, String[] var4) {
      if (var3 >= this.lotHeader.minLevel && var3 <= this.lotHeader.maxLevel) {
         var1 -= this.lotHeader.getMinSquareX();
         var2 -= this.lotHeader.getMinSquareY();
         int var5 = var1 + var2 * this.CELL_DIM + (var3 - this.lotHeader.minLevel) * this.CELL_DIM * this.CELL_DIM;
         if (var4 != null && var4.length != 0) {
            this.m_offsetInData[var5] = this.m_data.size();
            this.m_data.add(var4.length);
            String[] var6 = var4;
            int var7 = var4.length;

            for(int var8 = 0; var8 < var7; ++var8) {
               String var9 = var6[var8];
               this.m_data.add(this.lotHeader.getTileIndex(var9));
            }

            this.lotHeader.minLevelNotEmpty = Math.min(this.lotHeader.minLevelNotEmpty, var3);
            this.lotHeader.maxLevelNotEmpty = Math.max(this.lotHeader.maxLevelNotEmpty, var3);
         } else {
            this.m_offsetInData[var5] = -1;
         }
      }
   }
}
