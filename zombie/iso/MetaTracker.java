package zombie.iso;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import zombie.AmbientStreamManager;
import zombie.ZomboidFileSystem;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.network.GameClient;
import zombie.network.GameServer;

public class MetaTracker {
   private MetaTracker() {
   }

   public static void save() {
      if (!GameClient.bClient && !GameServer.bServer && !Core.getInstance().isNoSave()) {
         try {
            ByteBuffer var0 = ByteBuffer.allocate(10000);
            var0.putInt(219);
            IsoWorld.instance.helicopter.save(var0);
            AmbientStreamManager.instance.save(var0);
            var0.flip();
            File var1 = new File(ZomboidFileSystem.instance.getFileNameInCurrentSave("metadata.bin"));
            FileOutputStream var2 = new FileOutputStream(var1);
            var2.getChannel().truncate(0L);
            var2.write(var0.array(), 0, var0.limit());
            var2.flush();
            var2.close();
         } catch (Exception var3) {
            ExceptionLogger.logException(var3);
         }

      }
   }

   public static void load() {
      if (!Core.getInstance().isNoSave()) {
         String var0 = ZomboidFileSystem.instance.getFileNameInCurrentSave("metadata.bin");
         File var1 = new File(var0);
         if (var1.exists()) {
            try {
               FileInputStream var2 = new FileInputStream(var1);

               try {
                  ByteBuffer var3 = ByteBuffer.allocate((int)var1.length());
                  var3.clear();
                  int var4 = var2.read(var3.array());
                  var3.limit(var4);
                  int var5 = var3.getInt();
                  IsoWorld.instance.helicopter.load(var3, var5);
                  AmbientStreamManager.instance.load(var3, var5);
               } catch (Throwable var7) {
                  try {
                     var2.close();
                  } catch (Throwable var6) {
                     var7.addSuppressed(var6);
                  }

                  throw var7;
               }

               var2.close();
            } catch (Exception var8) {
               ExceptionLogger.logException(var8);
            }

         }
      }
   }
}
