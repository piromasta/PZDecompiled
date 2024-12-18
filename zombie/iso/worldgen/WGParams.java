package zombie.iso.worldgen;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;
import zombie.GameWindow;
import zombie.ZomboidFileSystem;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.debug.DebugLog;
import zombie.network.GameClient;

public class WGParams {
   private static final byte[] FILE_MAGIC = new byte[]{87, 71, 69, 78};
   public static WGParams instance = new WGParams();
   private String seedString = "";
   private int seed;
   private int minXCell = 0;
   private int minYCell = 0;
   private int maxXCell = 100;
   private int maxYCell = 100;

   private WGParams() {
   }

   public String getSeedString() {
      return this.seedString;
   }

   public void setSeedString(String var1) {
      this.seedString = var1;
      this.seed = var1.hashCode();
   }

   public long getSeed() {
      return (long)this.seed;
   }

   public Random getRandom(int var1, int var2) {
      return this.getRandom(var1, var2, 0L);
   }

   public Random getRandom(int var1, int var2, long var3) {
      Random var5 = new Random((long)this.seed + var3);
      long var6 = var5.nextLong();
      long var8 = var5.nextLong();
      var5.setSeed((long)var1 * var6 ^ (long)var2 * var8 ^ (long)this.seed);
      return var5;
   }

   public int getMinXCell() {
      return this.minXCell;
   }

   public void setMinXCell(int var1) {
      this.minXCell = var1;
   }

   public int getMinYCell() {
      return this.minYCell;
   }

   public void setMinYCell(int var1) {
      this.minYCell = var1;
   }

   public int getMaxXCell() {
      return this.maxXCell;
   }

   public void setMaxXCell(int var1) {
      this.maxXCell = var1;
   }

   public int getMaxYCell() {
      return this.maxYCell;
   }

   public void setMaxYCell(int var1) {
      this.maxYCell = var1;
   }

   public void save() {
      if (!GameClient.bClient && !Core.getInstance().isNoSave()) {
         DebugLog.log("Saving worldgen params");

         try {
            ByteBuffer var1 = ByteBuffer.allocate(10000);
            var1.put(FILE_MAGIC);
            var1.putInt(219);
            GameWindow.WriteStringUTF(var1, this.getSeedString());
            var1.putInt(this.getMinXCell());
            var1.putInt(this.getMinYCell());
            var1.putInt(this.getMaxXCell());
            var1.putInt(this.getMaxYCell());
            var1.flip();
            File var2 = new File(ZomboidFileSystem.instance.getFileNameInCurrentSave("map_worldgen.bin"));
            FileOutputStream var3 = new FileOutputStream(var2);
            var3.getChannel().truncate(0L);
            var3.write(var1.array(), 0, var1.limit());
            var3.flush();
            var3.close();
         } catch (Exception var4) {
            ExceptionLogger.logException(var4);
         }

      }
   }

   public void load() {
      if (!GameClient.bClient) {
         DebugLog.log("Loading worldgen params");
         File var1 = new File(ZomboidFileSystem.instance.getFileNameInCurrentSave("map_worldgen.bin"));
         if (var1.exists()) {
            try {
               FileInputStream var2 = new FileInputStream(var1);

               try {
                  ByteBuffer var3 = ByteBuffer.allocate((int)var1.length());
                  var3.clear();
                  int var4 = var2.read(var3.array());
                  var3.limit(var4);
                  byte[] var5 = new byte[4];
                  var3.get(var5);
                  if (!Arrays.equals(var5, FILE_MAGIC)) {
                     throw new IOException(var1.getAbsolutePath() + " does not appear to be map_worldgen.bin");
                  }

                  int var6 = var3.getInt();
                  this.setSeedString(GameWindow.ReadStringUTF(var3));
                  this.setMinXCell(var3.getInt());
                  this.setMinYCell(var3.getInt());
                  this.setMaxXCell(var3.getInt());
                  this.setMaxYCell(var3.getInt());
               } catch (Throwable var8) {
                  try {
                     var2.close();
                  } catch (Throwable var7) {
                     var8.addSuppressed(var7);
                  }

                  throw var8;
               }

               var2.close();
            } catch (Exception var9) {
               ExceptionLogger.logException(var9);
            }

         }
      }
   }
}
