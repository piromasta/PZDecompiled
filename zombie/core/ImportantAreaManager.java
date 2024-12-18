package zombie.core;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedList;
import zombie.ZomboidFileSystem;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.iso.SliceY;
import zombie.network.GameClient;
import zombie.network.ServerMap;

public class ImportantAreaManager {
   private static ImportantAreaManager instance = new ImportantAreaManager();
   public static final int importantAreasMaximum = 100;
   public static final int importantAreasTimeout = 10000;
   public static final LinkedList<ImportantArea> ImportantAreas = new LinkedList();
   public static final LinkedList<ImportantArea> ImportantAreasForDelete = new LinkedList();

   public ImportantAreaManager() {
   }

   public static ImportantAreaManager getInstance() {
      return instance;
   }

   public final void load(ByteBuffer var1, int var2) throws IOException {
      ImportantAreas.clear();
      int var3 = var1.getInt();

      for(int var4 = 0; var4 < var3; ++var4) {
         ImportantArea var5 = new ImportantArea(0, 0);
         var5.load(var1, var2);
         ImportantAreas.add(var5);
      }

   }

   public final void save(ByteBuffer var1) throws IOException {
      var1.putInt(ImportantAreas.size());
      Iterator var2 = ImportantAreas.iterator();

      while(var2.hasNext()) {
         ImportantArea var3 = (ImportantArea)var2.next();
         var3.save(var1);
      }

   }

   public void saveDataFile() {
      if (!GameClient.bClient) {
         File var1 = ZomboidFileSystem.instance.getFileInCurrentSave("important_area_data.bin");

         try {
            FileOutputStream var2 = new FileOutputStream(var1);

            try {
               BufferedOutputStream var3 = new BufferedOutputStream(var2);

               try {
                  synchronized(SliceY.SliceBufferLock) {
                     SliceY.SliceBuffer.clear();
                     SliceY.SliceBuffer.putInt(219);
                     this.save(SliceY.SliceBuffer);
                     var3.write(SliceY.SliceBuffer.array(), 0, SliceY.SliceBuffer.position());
                  }
               } catch (Throwable var9) {
                  try {
                     var3.close();
                  } catch (Throwable var7) {
                     var9.addSuppressed(var7);
                  }

                  throw var9;
               }

               var3.close();
            } catch (Throwable var10) {
               try {
                  var2.close();
               } catch (Throwable var6) {
                  var10.addSuppressed(var6);
               }

               throw var10;
            }

            var2.close();
         } catch (FileNotFoundException var11) {
            throw new RuntimeException(var11);
         } catch (IOException var12) {
            throw new RuntimeException(var12);
         }
      }
   }

   public ImportantArea updateOrAdd(int var1, int var2) {
      int var3 = var1 / 64;
      int var4 = var2 / 64;
      Iterator var5 = ImportantAreas.iterator();

      ImportantArea var6;
      do {
         if (!var5.hasNext()) {
            if (ImportantAreas.size() >= 100) {
               DebugLog.Multiplayer.warn("ImportantAreas size is too big. Random map area will unload.");
               ImportantAreas.remove(Rand.Next(0, ImportantAreas.size()));
               return null;
            }

            ImportantArea var7 = new ImportantArea(var3, var4);
            ImportantAreas.add(var7);
            return var7;
         }

         var6 = (ImportantArea)var5.next();
      } while(var6.sx != var3 || var6.sy != var4);

      var6.lastUpdate = System.currentTimeMillis();
      return var6;
   }

   public void process() {
      Iterator var1 = ImportantAreas.iterator();

      while(var1.hasNext()) {
         ImportantArea var2 = (ImportantArea)var1.next();
         if (System.currentTimeMillis() - var2.lastUpdate > 10000L) {
            ImportantAreasForDelete.add(var2);
         } else {
            ServerMap.instance.importantAreaIn(var2.sx, var2.sy);
         }
      }

      ImportantAreas.removeAll(ImportantAreasForDelete);
   }
}
