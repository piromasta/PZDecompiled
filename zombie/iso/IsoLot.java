package zombie.iso;

import gnu.trove.list.array.TIntArrayList;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import zombie.ChunkMapFilenames;
import zombie.core.logger.ExceptionLogger;
import zombie.iso.enums.ChunkGenerationStatus;
import zombie.popman.ObjectPool;
import zombie.util.BufferedRandomAccessFile;

public class IsoLot {
   public static final HashMap<String, LotHeader> InfoHeaders = new HashMap();
   public static final ArrayList<String> InfoHeaderNames = new ArrayList();
   public static final HashMap<String, String> InfoFileNames = new HashMap();
   public static final HashMap<String, ChunkGenerationStatus> InfoFileModded = new HashMap();
   public static final ArrayList<MapFiles> MapFiles = new ArrayList();
   public static final ObjectPool<IsoLot> pool = new ObjectPool(IsoLot::new);
   public int maxLevel;
   public int minLevel;
   private String m_lastUsedPath = "";
   public int wx = 0;
   public int wy = 0;
   final int[] m_offsetInData = new int[4096];
   final TIntArrayList m_data = new TIntArrayList();
   private RandomAccessFile m_in = null;
   private int m_version;
   LotHeader info;

   public IsoLot() {
   }

   public static void Dispose() {
      Iterator var0 = MapFiles.iterator();

      while(var0.hasNext()) {
         MapFiles var1 = (MapFiles)var0.next();
         var1.Dispose();
      }

      MapFiles.clear();
      InfoHeaders.clear();
      InfoHeaderNames.clear();
      InfoFileNames.clear();
      InfoFileModded.clear();
      pool.forEach((var0x) -> {
         RandomAccessFile var1 = var0x.m_in;
         if (var1 != null) {
            var0x.m_in = null;

            try {
               var1.close();
            } catch (IOException var3) {
               ExceptionLogger.logException(var3);
            }
         }

      });
   }

   public static String readString(BufferedRandomAccessFile var0) throws EOFException, IOException {
      String var1 = var0.getNextLine();
      return var1;
   }

   public static int readInt(RandomAccessFile var0) throws EOFException, IOException {
      int var1 = var0.read();
      int var2 = var0.read();
      int var3 = var0.read();
      int var4 = var0.read();
      if ((var1 | var2 | var3 | var4) < 0) {
         throw new EOFException();
      } else {
         return (var1 << 0) + (var2 << 8) + (var3 << 16) + (var4 << 24);
      }
   }

   public static int readShort(RandomAccessFile var0) throws EOFException, IOException {
      int var1 = var0.read();
      int var2 = var0.read();
      if ((var1 | var2) < 0) {
         throw new EOFException();
      } else {
         return (var1 << 0) + (var2 << 8);
      }
   }

   public static synchronized void put(IsoLot var0) {
      var0.info = null;
      var0.m_data.resetQuick();
      pool.release((Object)var0);
   }

   public static synchronized IsoLot get(MapFiles var0, Integer var1, Integer var2, Integer var3, Integer var4, IsoChunk var5) {
      IsoLot var6 = (IsoLot)pool.alloc();
      var6.load(var0, var1, var2, var3, var4, var5);
      return var6;
   }

   public void loadNew(int var1, int var2, int var3, int var4, IsoChunk var5) {
   }

   public void load(MapFiles var1, Integer var2, Integer var3, Integer var4, Integer var5, IsoChunk var6) {
      String var7 = ChunkMapFilenames.instance.getHeader(var2, var3);
      this.info = (LotHeader)var1.InfoHeaders.get(var7);
      this.wx = var4;
      this.wy = var5;
      if (this.info == InfoHeaders.get(var7)) {
         var6.lotheader = this.info;
      }

      try {
         var7 = "world_" + var2 + "_" + var3 + ".lotpack";
         File var8 = new File((String)var1.InfoFileNames.get(var7));
         if (this.m_in == null || !this.m_lastUsedPath.equals(var8.getAbsolutePath())) {
            if (this.m_in != null) {
               this.m_in.close();
            }

            this.m_in = new BufferedRandomAccessFile(var8.getAbsolutePath(), "r", 4096);
            this.m_lastUsedPath = var8.getAbsolutePath();
            byte[] var9 = new byte[4];
            this.m_in.read(var9, 0, 4);
            boolean var10 = Arrays.equals(var9, LotHeader.LOTPACK_MAGIC);
            if (var10) {
               this.m_version = readInt(this.m_in);
               if (this.m_version < 0 || this.m_version > 1) {
                  throw new IOException("Unsupported version " + this.m_version);
               }
            } else {
               this.m_in.seek(0L);
               this.m_version = 0;
            }
         }

         int var26 = 0;
         int var27 = this.wx - var2 * IsoCell.CellSizeInChunks;
         int var11 = this.wy - var3 * IsoCell.CellSizeInChunks;
         int var12 = var27 * IsoCell.CellSizeInChunks + var11;
         this.m_in.seek((long)((this.m_version >= 1 ? 8 : 0) + 4) + (long)var12 * 8L);
         int var13 = readInt(this.m_in);
         this.m_in.seek((long)var13);
         this.m_data.resetQuick();
         int var14 = Math.max(this.info.minLevel, -32);
         int var15 = Math.min(this.info.maxLevel, 31);
         this.minLevel = 0;
         int var16 = 0;

         for(int var17 = var14; var17 <= var15; ++var17) {
            for(int var18 = 0; var18 < 8; ++var18) {
               for(int var19 = 0; var19 < 8; ++var19) {
                  int var20 = var18 + var19 * 8 + (var17 - this.info.minLevel) * 64;
                  this.m_offsetInData[var20] = -1;
                  if (var26 > 0) {
                     --var26;
                  } else {
                     int var21 = readInt(this.m_in);
                     if (var21 == -1) {
                        var26 = readInt(this.m_in);
                        if (var26 > 0) {
                           --var26;
                           continue;
                        }
                     }

                     if (var21 > 1) {
                        this.m_offsetInData[var20] = this.m_data.size();
                        this.m_data.add(var21 - 1);
                        this.minLevel = Math.min(var17, this.minLevel);
                        var16 = Math.max(var17, var16);
                        int var22 = readInt(this.m_in);

                        for(int var23 = 1; var23 < var21; ++var23) {
                           int var24 = readInt(this.m_in);
                           this.m_data.add(var24);
                        }
                     }
                  }
               }
            }
         }

         this.maxLevel = var16 + 1;
      } catch (Exception var25) {
         Arrays.fill(this.m_offsetInData, -1);
         this.m_data.resetQuick();
         ExceptionLogger.logException(var25);
      }

   }
}
