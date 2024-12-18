package zombie.core.backup;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.compress.archivers.zip.ParallelScatterZipCreator;
import org.apache.commons.compress.archivers.zip.Zip64Mode;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.parallel.InputStreamSupplier;
import zombie.ZomboidFileSystem;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.iso.IsoChunk;
import zombie.network.CoopSlave;
import zombie.network.GameServer;
import zombie.network.ServerOptions;

public class ZipBackup {
   private static final int compressionMethod = 0;
   static ParallelScatterZipCreator scatterZipCreator = null;
   private static long lastBackupTime = 0L;

   public ZipBackup() {
   }

   public static void onStartup() {
      lastBackupTime = System.currentTimeMillis();
      if (ServerOptions.getInstance().BackupsOnStart.getValue()) {
         makeBackupFile(GameServer.ServerName, ZipBackup.BackupTypes.startup);
      }

   }

   public static void onVersion() {
      if (ServerOptions.getInstance().BackupsOnVersionChange.getValue()) {
         String var10000 = ZomboidFileSystem.instance.getCacheDir();
         String var0 = var10000 + File.separator + "backups" + File.separator + "last_server_version.txt";
         String var1 = getStringFromZip(var0);
         String var2 = Core.getInstance().getGameVersion().toString();
         if (!var2.equals(var1)) {
            putTextFile(var0, var2);
            makeBackupFile(GameServer.ServerName, ZipBackup.BackupTypes.version);
         }

      }
   }

   public static void onPeriod() {
      int var0 = ServerOptions.getInstance().BackupsPeriod.getValue();
      if (var0 > 0) {
         if (System.currentTimeMillis() - lastBackupTime > (long)(var0 * '\uea60')) {
            lastBackupTime = System.currentTimeMillis();
            makeBackupFile(GameServer.ServerName, ZipBackup.BackupTypes.period);
         }

      }
   }

   public static void makeBackupFile(String var0, BackupTypes var1) {
      String var10000 = ZomboidFileSystem.instance.getCacheDir();
      String var2 = var10000 + File.separator + "backups" + File.separator + var1.name();
      long var3 = System.currentTimeMillis();
      DebugLog.DetailedInfo.trace("Start making backup to: " + var2);
      scatterZipCreator = new ParallelScatterZipCreator();
      CoopSlave.status("UI_ServerStatus_CreateBackup");
      FileOutputStream var5 = null;
      ZipArchiveOutputStream var6 = null;

      try {
         File var7 = new File(var2);
         if (!var7.exists()) {
            var7.mkdirs();
         }

         rotateBackupFile(var1);
         String var8 = var2 + File.separator + "backup_1.zip";

         try {
            Files.deleteIfExists(Paths.get(var8));
         } catch (IOException var16) {
            var16.printStackTrace();
         }

         File var9 = new File(var8);
         var9.delete();
         var5 = new FileOutputStream(var9);
         var6 = new ZipArchiveOutputStream(var5);
         var6.setUseZip64(Zip64Mode.AsNeeded);
         var6.setMethod(0);
         var6.setLevel(0);
         zipTextFile("readme.txt", getBackupReadme(var0));
         var6.setComment(getBackupReadme(var0));
         zipFile("options.ini", "options.ini");
         zipFile("popman-options.ini", "popman-options.ini");
         zipFile("latestSave.ini", "latestSave.ini");
         zipFile("debug-options.ini", "debug-options.ini");
         zipFile("sounds.ini", "sounds.ini");
         zipFile("gamepadBinding.config", "gamepadBinding.config");
         zipDir("mods", "mods");
         zipDir("Lua", "Lua");
         zipDir("db", "db");
         zipDir("Server", "Server");
         synchronized(IsoChunk.WriteLock) {
            zipDir("Saves" + File.separator + "Multiplayer" + File.separator + var0, "Saves" + File.separator + "Multiplayer" + File.separator + var0);

            try {
               scatterZipCreator.writeTo(var6);
               DebugLog.log(scatterZipCreator.getStatisticsMessage().toString());
               var6.close();
               var5.close();
            } catch (IOException var14) {
               var14.printStackTrace();
            }
         }
      } catch (Exception var17) {
         var17.printStackTrace();
         if (var5 != null) {
            try {
               var5.close();
            } catch (IOException var13) {
               var13.printStackTrace();
            }
         }
      }

      DebugLog.log("Backup made in " + (System.currentTimeMillis() - var3) + " ms");
   }

   private static void rotateBackupFile(BackupTypes var0) {
      int var1 = ServerOptions.getInstance().BackupsCount.getValue() - 1;
      if (var1 > 0) {
         Path var2 = Paths.get(ZomboidFileSystem.instance.getCacheDir() + File.separator + "backups" + File.separator + var0 + File.separator + "backup_" + (var1 + 1) + ".zip");

         try {
            Files.deleteIfExists(var2);
         } catch (IOException var8) {
            var8.printStackTrace();
         }

         for(int var3 = var1; var3 > 0; --var3) {
            Path var4 = Paths.get(ZomboidFileSystem.instance.getCacheDir() + File.separator + "backups" + File.separator + var0 + File.separator + "backup_" + var3 + ".zip");
            Path var5 = Paths.get(ZomboidFileSystem.instance.getCacheDir() + File.separator + "backups" + File.separator + var0 + File.separator + "backup_" + (var3 + 1) + ".zip");

            try {
               Files.move(var4, var5);
            } catch (Exception var7) {
            }
         }

      }
   }

   private static String getBackupReadme(String var0) {
      SimpleDateFormat var1 = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
      Date var2 = new Date(System.currentTimeMillis());
      var1.format(var2);
      int var3 = getWorldVersion(var0);
      String var4 = "";
      if (var3 == -2) {
         var4 = "World isn't exist";
      } else if (var3 == -1) {
         var4 = "World version cannot be determined";
      } else {
         var4 = String.valueOf(var3);
      }

      String var10000 = var1.format(var2);
      return "Backup time: " + var10000 + "\nServerName: " + var0 + "\nCurrent server version:" + Core.getInstance().getGameVersion() + "\nCurrent world version:219\nWorld version in this backup is:" + var4;
   }

   private static int getWorldVersion(String var0) {
      String var10002 = ZomboidFileSystem.instance.getSaveDir();
      File var1 = new File(var10002 + File.separator + "Multiplayer" + File.separator + var0 + File.separator + "map_t.bin");
      if (var1.exists()) {
         try {
            FileInputStream var2 = new FileInputStream(var1);

            int var9;
            label64: {
               byte var15;
               try {
                  DataInputStream var3 = new DataInputStream(var2);

                  label60: {
                     try {
                        byte var4 = var3.readByte();
                        byte var5 = var3.readByte();
                        byte var6 = var3.readByte();
                        byte var7 = var3.readByte();
                        if (var4 != 71 || var5 != 77 || var6 != 84 || var7 != 77) {
                           var15 = -1;
                           break label60;
                        }

                        int var8 = var3.readInt();
                        var9 = var8;
                     } catch (Throwable var12) {
                        try {
                           var3.close();
                        } catch (Throwable var11) {
                           var12.addSuppressed(var11);
                        }

                        throw var12;
                     }

                     var3.close();
                     break label64;
                  }

                  var3.close();
               } catch (Throwable var13) {
                  try {
                     var2.close();
                  } catch (Throwable var10) {
                     var13.addSuppressed(var10);
                  }

                  throw var13;
               }

               var2.close();
               return var15;
            }

            var2.close();
            return var9;
         } catch (Exception var14) {
            var14.printStackTrace();
         }
      }

      return -2;
   }

   private static void putTextFile(String var0, String var1) {
      try {
         Path var2 = Paths.get(var0);
         Files.createDirectories(var2.getParent());

         try {
            Files.delete(var2);
         } catch (Exception var4) {
         }

         Files.write(var2, var1.getBytes(), new OpenOption[0]);
      } catch (Exception var5) {
         var5.printStackTrace();
      }

   }

   private static String getStringFromZip(String var0) {
      String var1 = null;

      try {
         Path var2 = Paths.get(var0);
         if (Files.exists(var2, new LinkOption[0])) {
            List var3 = Files.readAllLines(var2);
            var1 = (String)var3.get(0);
         }
      } catch (Exception var4) {
         var4.printStackTrace();
      }

      return var1;
   }

   private static void zipTextFile(String var0, String var1) {
      InputStreamSupplier var2 = () -> {
         ByteArrayInputStream var1x = new ByteArrayInputStream(var1.getBytes(StandardCharsets.UTF_8));
         return var1x;
      };
      ZipArchiveEntry var3 = new ZipArchiveEntry(var0);
      var3.setMethod(0);
      scatterZipCreator.addArchiveEntry(var3, var2);
   }

   private static void zipFile(String var0, String var1) {
      Path var2 = Paths.get(ZomboidFileSystem.instance.getCacheDir() + File.separator + var1);
      if (Files.exists(var2, new LinkOption[0])) {
         InputStreamSupplier var3 = () -> {
            InputStream var1 = null;

            try {
               var1 = Files.newInputStream(var2);
            } catch (IOException var3) {
               var3.printStackTrace();
            }

            return var1;
         };
         ZipArchiveEntry var4 = new ZipArchiveEntry(var0);
         var4.setMethod(0);
         scatterZipCreator.addArchiveEntry(var4, var3);
      }
   }

   private static void zipDir(String var0, String var1) {
      Path var2 = Paths.get(ZomboidFileSystem.instance.getCacheDir() + File.separator + var1);
      if (Files.exists(var2, new LinkOption[0])) {
         try {
            String var10002 = ZomboidFileSystem.instance.getCacheDir();
            File var3 = new File(var10002 + File.separator + var1);
            if (var3.isDirectory()) {
               Iterator var4 = Arrays.asList(var3.listFiles()).iterator();
               int var5 = var3.getAbsolutePath().length() + 1;

               while(var4.hasNext()) {
                  File var6 = (File)var4.next();
                  if (!var6.isDirectory()) {
                     String var7 = var6.getAbsolutePath().substring(var5);
                     InputStreamSupplier var8 = () -> {
                        InputStream var1 = null;

                        try {
                           var1 = Files.newInputStream(var6.toPath());
                        } catch (IOException var3) {
                           var3.printStackTrace();
                        }

                        return var1;
                     };
                     ZipArchiveEntry var9 = new ZipArchiveEntry(var0 + File.separator + var7);
                     var9.setMethod(0);
                     scatterZipCreator.addArchiveEntry(var9, var8);
                  }
               }
            }
         } catch (Exception var10) {
            var10.printStackTrace();
         }

      }
   }

   private static enum BackupTypes {
      period,
      startup,
      version;

      private BackupTypes() {
      }
   }
}
