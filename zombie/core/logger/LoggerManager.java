package zombie.core.logger;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import zombie.ZomboidFileSystem;
import zombie.characters.IsoGameCharacter;
import zombie.debug.DebugLog;

public final class LoggerManager {
   private static boolean s_isInitialized = false;
   private static final HashMap<String, ZLogger> s_loggers = new HashMap();

   public LoggerManager() {
   }

   public static synchronized ZLogger getLogger(String var0) {
      if (!s_loggers.containsKey(var0)) {
         createLogger(var0, false);
      }

      return (ZLogger)s_loggers.get(var0);
   }

   public static synchronized void init() {
      if (!s_isInitialized) {
         DebugLog.General.debugln("Initializing...");
         s_isInitialized = true;
         backupOldLogFiles();
      }
   }

   private static void backupOldLogFiles() {
      try {
         File var0 = new File(getLogsDir());
         File[] var1 = ZomboidFileSystem.listAllFiles(var0);
         if (var1.length == 0) {
            return;
         }

         Date var3 = getLogFileLastModifiedTime(var1[0]);
         String var4 = "logs_" + ZomboidFileSystem.getDateStampString(var3);
         String var10002 = getLogsDir();
         File var2 = new File(var10002 + File.separator + var4);
         ZomboidFileSystem.ensureFolderExists(var2);

         for(int var6 = 0; var6 < var1.length; ++var6) {
            File var7 = var1[var6];
            if (var7.isFile()) {
               String var10003 = var2.getAbsolutePath();
               var7.renameTo(new File(var10003 + File.separator + var7.getName()));
               var7.delete();
            }
         }
      } catch (Exception var5) {
         DebugLog.General.error("Exception thrown trying to initialize LoggerManager, trying to copy old log files.");
         DebugLog.General.error("Exception: ");
         DebugLog.General.error(var5);
         var5.printStackTrace();
      }

   }

   private static Date getLogFileLastModifiedTime(File var0) {
      Calendar var1 = Calendar.getInstance();
      var1.setTimeInMillis(var0.lastModified());
      return var1.getTime();
   }

   public static synchronized void createLogger(String var0, boolean var1) {
      init();
      s_loggers.put(var0, new ZLogger(var0, var1));
   }

   public static String getLogsDir() {
      String var0 = ZomboidFileSystem.instance.getCacheDirSub("Logs");
      ZomboidFileSystem.ensureFolderExists(var0);
      File var1 = new File(var0);
      return var1.getAbsolutePath();
   }

   public static String getPlayerCoords(IsoGameCharacter var0) {
      int var10000 = (int)var0.getX();
      return "(" + var10000 + "," + (int)var0.getY() + "," + (int)var0.getZ() + ")";
   }
}
