package zombie.world;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import zombie.debug.DebugLog;
import zombie.scripting.ScriptManager;

public class DictionaryDataClient extends DictionaryData {
   public DictionaryDataClient() {
   }

   protected boolean isClient() {
      return true;
   }

   protected <T extends DictionaryInfo<?>> void parseInfoLoadList(Map<String, T> var1) throws WorldDictionaryException {
   }

   protected <T extends DictionaryInfo<?>> void parseCurrentInfoSet(Map<String, T> var1) throws WorldDictionaryException {
      Iterator var2 = var1.entrySet().iterator();

      while(var2.hasNext()) {
         Map.Entry var3 = (Map.Entry)var2.next();
         DictionaryInfo var4 = (DictionaryInfo)var3.getValue();
         boolean var5 = false;
         if (var4 instanceof ItemInfo var6) {
            if (!var6.removed && var6.scriptItem == null) {
               var6.scriptItem = ScriptManager.instance.getSpecificItem(var6.fullType);
            }

            if (var6.scriptItem != null) {
               var6.scriptItem.setRegistry_id(var6.registryID);
               var6.scriptItem.setModID(var6.modID);
               var6.isLoaded = true;
               var5 = true;
            }
         } else {
            DebugLog.General.warn("--------------------------------------------------------");
            DebugLog.General.warn("-----   Reminder purge entity script storing   ---------");
            DebugLog.General.warn("--------------------------------------------------------");
            DebugLog.General.printStackTrace();
            EntityInfo var7 = (EntityInfo)var4;
            if (!var7.removed && var7.entityScript == null) {
               var7.entityScript = ScriptManager.instance.getSpecificEntity(var7.fullType);
            }

            if (var7.entityScript != null) {
               var7.entityScript.setRegistry_id(var7.registryID);
               var7.entityScript.setModID(var7.modID);
               var7.isLoaded = true;
               var5 = true;
            }
         }

         if (!var5 && !var4.removed) {
            DebugLog.General.error("Warning client has no script for dictionary info: " + var4.fullType);
         }
      }

   }

   protected void parseObjectNameLoadList(List<String> var1) throws WorldDictionaryException {
   }

   protected void backupCurrentDataSet() throws IOException {
   }

   protected void deleteBackupCurrentDataSet() throws IOException {
   }

   protected void createErrorBackups() {
   }

   protected void load() throws IOException, WorldDictionaryException {
   }

   protected void save() throws IOException, WorldDictionaryException {
   }

   protected void saveToByteBuffer(ByteBuffer var1) throws IOException {
   }
}
