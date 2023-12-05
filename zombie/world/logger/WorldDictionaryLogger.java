package zombie.world.logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import zombie.ZomboidFileSystem;
import zombie.debug.DebugLog;
import zombie.network.GameClient;

public class WorldDictionaryLogger {
   private static final ArrayList<Log.BaseLog> _logItems = new ArrayList();

   public WorldDictionaryLogger() {
   }

   public static void reset() {
      _logItems.clear();
   }

   public static void startLogging() {
      reset();
   }

   public static void log(Log.BaseLog var0) {
      if (!GameClient.bClient) {
         _logItems.add(var0);
      }
   }

   public static void log(String var0) {
      log(var0, true);
   }

   public static void log(String var0, boolean var1) {
      if (!GameClient.bClient) {
         if (var1) {
            DebugLog.log("WorldDictionary: " + var0);
         }

         _logItems.add(new Log.Comment(var0));
      }
   }

   public static void saveLog(String var0) throws IOException {
      if (!GameClient.bClient) {
         boolean var1 = false;

         Log.BaseLog var2;
         for(int var3 = 0; var3 < _logItems.size(); ++var3) {
            var2 = (Log.BaseLog)_logItems.get(var3);
            if (!var2.isIgnoreSaveCheck()) {
               var1 = true;
               break;
            }
         }

         if (var1) {
            String var10002 = ZomboidFileSystem.instance.getCurrentSaveDir();
            File var12 = new File(var10002 + File.separator);
            if (var12.exists() && var12.isDirectory()) {
               String var4 = ZomboidFileSystem.instance.getFileNameInCurrentSave(var0);
               File var5 = new File(var4);

               try {
                  FileWriter var6 = new FileWriter(var5, true);

                  try {
                     var6.write("log = log or {};" + System.lineSeparator());
                     var6.write("table.insert(log, {" + System.lineSeparator());
                     int var7 = 0;

                     while(true) {
                        if (var7 >= _logItems.size()) {
                           var6.write("};" + System.lineSeparator());
                           break;
                        }

                        var2 = (Log.BaseLog)_logItems.get(var7);
                        var2.saveAsText(var6, "\t");
                        ++var7;
                     }
                  } catch (Throwable var10) {
                     try {
                        var6.close();
                     } catch (Throwable var9) {
                        var10.addSuppressed(var9);
                     }

                     throw var10;
                  }

                  var6.close();
               } catch (Exception var11) {
                  var11.printStackTrace();
                  throw new IOException("Error saving WorldDictionary log.");
               }
            }

         }
      }
   }
}
