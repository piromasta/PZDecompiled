package zombie.network.id;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import zombie.ZomboidFileSystem;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.iso.IsoWorld;
import zombie.network.GameClient;

public class ObjectIDManager {
   private static final ObjectIDManager instance = new ObjectIDManager();
   private static final int saveLastIDNumber = 100;
   private static int ObjectIDManagerCheckLimiter = 0;

   public static ObjectIDManager getInstance() {
      return instance;
   }

   private ObjectIDManager() {
   }

   public void clear() {
      ObjectIDType[] var1 = ObjectIDType.values();
      int var2 = var1.length;

      for(int var3 = 0; var3 < var2; ++var3) {
         ObjectIDType var4 = var1[var3];
         var4.lastID = 0L;
         var4.countNewID = 0L;
      }

   }

   public void load(DataInputStream var1, int var2) throws IOException {
      byte var3 = var1.readByte();

      for(byte var4 = 0; var4 < var3; ++var4) {
         byte var5 = var1.readByte();
         long var6 = var1.readLong();
         ObjectIDType.valueOf(var5).lastID = var6 + 100L;
         ObjectIDType.valueOf(var5).countNewID = 0L;
         DebugLog.General.println((Object)ObjectIDType.valueOf(var5));
      }

   }

   private void save(DataOutputStream var1) throws IOException {
      var1.write(ObjectIDType.permanentObjectIDTypes);
      ObjectIDType[] var2 = ObjectIDType.values();
      int var3 = var2.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         ObjectIDType var5 = var2[var4];
         if (var5.isPermanent) {
            var1.writeByte(var5.index);
            var1.writeLong(var5.lastID);
         }

         var5.countNewID = 0L;
         DebugLog.General.println((Object)var5);
      }

   }

   private boolean isNeedToSave() {
      ObjectIDType[] var1 = ObjectIDType.values();
      int var2 = var1.length;

      for(int var3 = 0; var3 < var2; ++var3) {
         ObjectIDType var4 = var1[var3];
         if (var4.countNewID >= 100L) {
            return true;
         }
      }

      return false;
   }

   public void checkForSaveDataFile(boolean var1) {
      if (!GameClient.bClient) {
         ++ObjectIDManagerCheckLimiter;
         if (var1 || ObjectIDManagerCheckLimiter > 300) {
            ObjectIDManagerCheckLimiter = 0;
            if (var1 || this.isNeedToSave()) {
               DebugLog.General.println("The id_manager_data.bin file is saved");
               File var2 = ZomboidFileSystem.instance.getFileInCurrentSave("id_manager_data.bin");

               try {
                  FileOutputStream var3 = new FileOutputStream(var2);

                  try {
                     DataOutputStream var4 = new DataOutputStream(var3);

                     try {
                        var4.writeInt(IsoWorld.getWorldVersion());
                        this.save(var4);
                     } catch (Throwable var9) {
                        try {
                           var4.close();
                        } catch (Throwable var8) {
                           var9.addSuppressed(var8);
                        }

                        throw var9;
                     }

                     var4.close();
                  } catch (Throwable var10) {
                     try {
                        var3.close();
                     } catch (Throwable var7) {
                        var10.addSuppressed(var7);
                     }

                     throw var10;
                  }

                  var3.close();
               } catch (IOException var11) {
                  DebugLog.General.printException(var11, "Save failed", LogSeverity.Error);
               }
            }
         }

      }
   }

   public static IIdentifiable get(ObjectID var0) {
      IIdentifiable var1 = (IIdentifiable)var0.getType().IDToObjectMap.get(var0.getObjectID());
      return var1;
   }

   public void remove(ObjectID var1) {
      IIdentifiable var2 = (IIdentifiable)var1.getType().IDToObjectMap.get(var1.getObjectID());
      boolean var3 = false;
      if (var1.getType().IDToObjectMap.contains(var1.getObjectID())) {
         var3 = var1.getType().IDToObjectMap.remove(var1.getObjectID(), var2);
      }

   }

   public void addObject(IIdentifiable var1) {
      if (var1 == null) {
         DebugLog.General.warn("%s ObjectID: is null");
      } else {
         long var2 = var1.getObjectID().getObjectID();
         ObjectIDType var4 = var1.getObjectID().getType();
         if (var2 == -1L) {
            if (GameClient.bClient) {
               return;
            }

            for(var2 = (long)((short)((int)var4.allocateID())); var4.IDToObjectMap.get(var2) != null; var2 = (long)((short)((int)var4.allocateID()))) {
            }
         }

         var4.IDToObjectMap.add(var2, var1);
         var1.getObjectID().set(var2, var4);
      }
   }

   public static ObjectID createObjectID(ObjectIDType var0) {
      try {
         Constructor var1 = var0.type.getDeclaredConstructor(ObjectIDType.class);
         return (ObjectID)var1.newInstance(var0);
      } catch (Exception var2) {
         DebugLog.General.printException(var2, "ObjectID creation failed", LogSeverity.Error);
         throw new RuntimeException();
      }
   }
}
