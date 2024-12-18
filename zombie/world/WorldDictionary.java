package zombie.world;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import zombie.GameWindow;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.entity.GameEntity;
import zombie.erosion.ErosionRegions;
import zombie.erosion.categories.ErosionCategory;
import zombie.gameStates.ChooseGameInfo;
import zombie.inventory.InventoryItem;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.scripting.entity.GameEntityScript;
import zombie.scripting.objects.Item;
import zombie.scripting.ui.XuiManager;
import zombie.scripting.ui.XuiScript;
import zombie.world.logger.Log;
import zombie.world.logger.WorldDictionaryLogger;

public class WorldDictionary {
   public static final int VERSION = 1;
   public static final String SAVE_FILE_READABLE = "WorldDictionaryReadable.lua";
   public static final String SAVE_FILE_LOG = "WorldDictionaryLog.lua";
   public static final String SAVE_FILE = "WorldDictionary";
   public static final String SAVE_EXT = ".bin";
   public static final boolean logUnset = false;
   public static final boolean logMissingObjectID = false;
   private static final Map<String, ItemInfo> itemLoadList = new HashMap();
   private static final Map<String, EntityInfo> entityLoadList = new HashMap();
   private static final List<String> objNameLoadList = new ArrayList();
   private static DictionaryData data;
   private static boolean isNewGame = true;
   private static boolean allowScriptItemLoading = false;
   private static final String netValidator = "DICTIONARY_PACKET_END";
   private static byte[] clientRemoteData;

   public WorldDictionary() {
   }

   protected static void log(String var0) {
      log(var0, true);
   }

   protected static void log(String var0, boolean var1) {
      if (var1) {
         DebugLog.log("WorldDictionary: " + var0);
      }

   }

   public static void setIsNewGame(boolean var0) {
      isNewGame = var0;
   }

   public static boolean isIsNewGame() {
      return isNewGame;
   }

   public static void StartScriptLoading() {
      allowScriptItemLoading = true;
      itemLoadList.clear();
      entityLoadList.clear();
      ScriptsDictionary.StartScriptLoading();
      StringDictionary.StartScriptLoading();
   }

   public static void ScriptsLoaded() {
      allowScriptItemLoading = false;
   }

   public static boolean isAllowScriptItemLoading() {
      return allowScriptItemLoading;
   }

   private static void onLoadItem(Item var0) {
      if (!GameClient.bClient) {
         if (!allowScriptItemLoading) {
            log("Warning script item loaded after WorldDictionary is initialised");
            if (Core.bDebug) {
               throw new RuntimeException("This shouldn't be happening.");
            }
         } else {
            ItemInfo var1 = (ItemInfo)itemLoadList.get(var0.getFullName());
            if (var1 == null) {
               var1 = new ItemInfo();
               var1.name = var0.getName();
               var1.moduleName = var0.getModuleName();
               var1.fullType = var0.getFullName();
               itemLoadList.put(var0.getFullName(), var1);
            }

            if (var1.modID != null && !var0.getModID().equals(var1.modID)) {
               if (var1.modOverrides == null) {
                  var1.modOverrides = new ArrayList();
               }

               if (!var1.modOverrides.contains(var1.modID)) {
                  var1.modOverrides.add(var1.modID);
               } else {
                  log("modOverrides for item '" + var1.fullType + "' already contains mod id: " + var1.modID);
               }
            }

            var1.modID = var0.getModID();
            if (var1.modID.equals("pz-vanilla")) {
               var1.existsAsVanilla = true;
            }

            var1.isModded = !var1.modID.equals("pz-vanilla");
            var1.obsolete = var0.getObsolete();
            var1.scriptItem = var0;
            var1.entityScript = var0;
         }
      }
   }

   public static void onLoadEntity(GameEntityScript var0) {
      if (!GameClient.bClient) {
         if (!allowScriptItemLoading) {
            log("Warning script entityScript loaded after WorldDictionary is initialised");
            if (Core.bDebug) {
               throw new RuntimeException("This shouldn't be happening.");
            }
         } else if (var0 instanceof Item) {
            onLoadItem((Item)var0);
         } else {
            EntityInfo var1 = (EntityInfo)entityLoadList.get(var0.getFullName());
            if (var1 == null) {
               var1 = new EntityInfo();
               var1.name = var0.getName();
               var1.moduleName = var0.getModuleName();
               var1.fullType = var0.getFullName();
               entityLoadList.put(var0.getFullName(), var1);
            }

            if (var1.modID != null && !var0.getModID().equals(var1.modID)) {
               if (var1.modOverrides == null) {
                  var1.modOverrides = new ArrayList();
               }

               if (!var1.modOverrides.contains(var1.modID)) {
                  var1.modOverrides.add(var1.modID);
               } else {
                  log("modOverrides for entityScript '" + var1.fullType + "' already contains mod id: " + var1.modID);
               }
            }

            var1.modID = var0.getModID();
            if (var1.modID.equals("pz-vanilla")) {
               var1.existsAsVanilla = true;
            }

            var1.isModded = !var1.modID.equals("pz-vanilla");
            var1.obsolete = var0.getObsolete();
            var1.entityScript = var0;
         }
      }
   }

   private static void collectObjectNames() {
      objNameLoadList.clear();
      if (!GameClient.bClient) {
         ArrayList var0 = new ArrayList();

         for(int var1 = 0; var1 < ErosionRegions.regions.size(); ++var1) {
            for(int var2 = 0; var2 < ((ErosionRegions.Region)ErosionRegions.regions.get(var1)).categories.size(); ++var2) {
               ErosionCategory var3 = (ErosionCategory)((ErosionRegions.Region)ErosionRegions.regions.get(var1)).categories.get(var2);
               var0.clear();
               var3.getObjectNames(var0);
               Iterator var4 = var0.iterator();

               while(var4.hasNext()) {
                  String var5 = (String)var4.next();
                  if (!objNameLoadList.contains(var5)) {
                     objNameLoadList.add(var5);
                  }
               }
            }
         }

      }
   }

   private static void collectStrings() throws WorldDictionaryException {
      boolean var0 = allowScriptItemLoading;
      allowScriptItemLoading = true;
      ArrayList var1 = XuiManager.GetAllLayouts();
      Iterator var2 = var1.iterator();

      while(var2.hasNext()) {
         XuiScript var3 = (XuiScript)var2.next();
         if (var3.getXuiLayoutName() != null) {
            StringDictionary.Generic.register(var3.getXuiLayoutName());
         }
      }

      allowScriptItemLoading = var0;
   }

   public static void loadDataFromServer(ByteBuffer var0) throws IOException {
      if (GameClient.bClient) {
         int var1 = var0.getInt();
         clientRemoteData = new byte[var1];
         var0.get(clientRemoteData, 0, clientRemoteData.length);
      }

   }

   public static void saveDataForClient(ByteBuffer var0) throws IOException {
      if (GameServer.bServer) {
         int var1 = var0.position();
         var0.putInt(0);
         int var2 = var0.position();
         if (data.serverDataCache != null) {
            var0.put(data.serverDataCache);
         } else {
            if (Core.bDebug) {
               throw new RuntimeException("Should be sending data from the serverDataCache here.");
            }

            data.saveToByteBuffer(var0);
         }

         GameWindow.WriteString(var0, "DICTIONARY_PACKET_END");
         int var3 = var0.position();
         var0.position(var1);
         var0.putInt(var3 - var2);
         var0.position(var3);
      }

   }

   public static void init() throws WorldDictionaryException {
      boolean var0 = true;
      collectObjectNames();
      WorldDictionaryLogger.startLogging();
      WorldDictionaryLogger.log("-------------------------------------------------------", false);
      SimpleDateFormat var1 = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
      WorldDictionaryLogger.log("Time: " + var1.format(new Date()), false);
      log("Checking dictionary...");
      Log.Info var2 = null;

      try {
         if (!GameClient.bClient) {
            if (data == null || data.isClient()) {
               data = new DictionaryData();
            }
         } else if (data == null || !data.isClient()) {
            data = new DictionaryDataClient();
         }

         data.reset();
         ScriptsDictionary.reset();
         StringDictionary.reset();
         collectStrings();
         if (GameClient.bClient) {
            if (clientRemoteData == null) {
               throw new WorldDictionaryException("WorldDictionary data not received from server.");
            }

            ByteBuffer var3 = ByteBuffer.wrap(clientRemoteData);
            data.loadFromByteBuffer(var3);
            String var4 = GameWindow.ReadString(var3);
            if (!var4.equals("DICTIONARY_PACKET_END")) {
               throw new WorldDictionaryException("WorldDictionary data received from server is corrupt.");
            }

            clientRemoteData = null;
         }

         data.backupCurrentDataSet();
         data.load();
         ArrayList var7 = new ArrayList();
         var2 = new Log.Info(var1.format(new Date()), Core.GameSaveWorld, 219, var7);
         WorldDictionaryLogger.log((Log.BaseLog)var2);
         data.parseInfoLoadList(itemLoadList);
         data.parseInfoLoadList(entityLoadList);
         data.parseCurrentInfoSet();
         itemLoadList.clear();
         entityLoadList.clear();
         data.parseObjectNameLoadList(objNameLoadList);
         objNameLoadList.clear();
         StringDictionary.parseRegisters();
         ScriptsDictionary.parseRegisters();
         data.getDictionaryMods(var7);
         data.saveAsText("WorldDictionaryReadable.lua");
         data.save();
         data.deleteBackupCurrentDataSet();
      } catch (Exception var6) {
         var0 = false;
         var6.printStackTrace();
         log("Warning: error occurred loading dictionary!");
         if (var2 != null) {
            var2.HasErrored = true;
         }

         if (data != null) {
            data.createErrorBackups();
         }
      }

      try {
         WorldDictionaryLogger.saveLog("WorldDictionaryLog.lua");
         WorldDictionaryLogger.reset();
      } catch (Exception var5) {
         var5.printStackTrace();
      }

      if (!var0) {
         throw new WorldDictionaryException("WorldDictionary: Cannot load world due to WorldDictionary error.");
      }
   }

   public static void onWorldLoaded() {
   }

   public static ItemInfo getItemInfoFromType(String var0) {
      return data.getItemInfoFromType(var0);
   }

   public static ItemInfo getItemInfoFromID(short var0) {
      return data.getItemInfoFromID(var0);
   }

   public static short getItemRegistryID(String var0) {
      return data.getItemRegistryID(var0);
   }

   public static String getItemTypeFromID(short var0) {
      return data.getItemTypeFromID(var0);
   }

   public static String getItemTypeDebugString(short var0) {
      return data.getItemTypeDebugString(var0);
   }

   public static EntityInfo getEntityInfoFromType(String var0) {
      return data.getEntityInfoFromType(var0);
   }

   public static EntityInfo getEntityInfoFromID(short var0) {
      return data.getEntityInfoFromID(var0);
   }

   public static short getEntityRegistryID(String var0) {
      return data.getEntityRegistryID(var0);
   }

   public static String getEntityTypeFromID(short var0) {
      return data.getEntityTypeFromID(var0);
   }

   public static String getEntityTypeDebugString(short var0) {
      return data.getEntityTypeDebugString(var0);
   }

   public static String getSpriteNameFromID(int var0) {
      return data.getSpriteNameFromID(var0);
   }

   public static int getIdForSpriteName(String var0) {
      return data.getIdForSpriteName(var0);
   }

   public static String getObjectNameFromID(byte var0) {
      return data.getObjectNameFromID(var0);
   }

   public static byte getIdForObjectName(String var0) {
      return data.getIdForObjectName(var0);
   }

   public static String getItemModID(short var0) {
      ItemInfo var1 = getItemInfoFromID(var0);
      return var1 != null ? var1.modID : null;
   }

   public static String getItemModID(String var0) {
      ItemInfo var1 = getItemInfoFromType(var0);
      return var1 != null ? var1.modID : null;
   }

   public static String getModNameFromID(String var0) {
      if (var0 != null) {
         if (var0.equals("pz-vanilla")) {
            return "Project Zomboid";
         }

         ChooseGameInfo.Mod var1 = ChooseGameInfo.getModDetails(var0);
         if (var1 != null && var1.getName() != null) {
            return var1.getName();
         }
      }

      return "Unknown mod";
   }

   public static void DebugPrintItem(InventoryItem var0) {
      Item var1 = var0.getScriptItem();
      if (var1 != null) {
         DebugPrintItem(var1);
      } else {
         String var2 = var0.getFullType();
         ItemInfo var3 = null;
         if (var2 != null) {
            var3 = getItemInfoFromType(var2);
         }

         if (var3 == null && var0.getRegistry_id() >= 0) {
            var3 = getItemInfoFromID(var0.getRegistry_id());
         }

         if (var3 != null) {
            var3.DebugPrint();
         } else {
            DebugLog.log("WorldDictionary: Cannot debug print item: " + (var2 != null ? var2 : "unknown"));
         }
      }

   }

   public static void DebugPrintItem(Item var0) {
      String var1 = var0.getFullName();
      ItemInfo var2 = null;
      if (var1 != null) {
         var2 = getItemInfoFromType(var1);
      }

      if (var2 == null && var0.getRegistry_id() >= 0) {
         var2 = getItemInfoFromID(var0.getRegistry_id());
      }

      if (var2 != null) {
         var2.DebugPrint();
      } else {
         DebugLog.log("WorldDictionary: Cannot debug print item: " + (var1 != null ? var1 : "unknown"));
      }

   }

   public static void DebugPrintItem(String var0) {
      ItemInfo var1 = getItemInfoFromType(var0);
      if (var1 != null) {
         var1.DebugPrint();
      } else {
         DebugLog.log("WorldDictionary: Cannot debug print item: " + var0);
      }

   }

   public static void DebugPrintItem(short var0) {
      ItemInfo var1 = getItemInfoFromID(var0);
      if (var1 != null) {
         var1.DebugPrint();
      } else {
         DebugLog.log("WorldDictionary: Cannot debug print item id: " + var0);
      }

   }

   public static String getEntityModID(short var0) {
      EntityInfo var1 = getEntityInfoFromID(var0);
      return var1 != null ? var1.modID : null;
   }

   public static String getEntityModID(String var0) {
      EntityInfo var1 = getEntityInfoFromType(var0);
      return var1 != null ? var1.modID : null;
   }

   public static void DebugPrintEntity(GameEntity var0) {
      if (var0 instanceof InventoryItem) {
         DebugPrintItem((InventoryItem)var0);
      } else if (Core.bDebug) {
         throw new RuntimeException("Not implemented yet.");
      }
   }

   public static void DebugPrintEntity(GameEntityScript var0) {
      String var1 = var0.getFullName();
      EntityInfo var2 = null;
      if (var1 != null) {
         var2 = getEntityInfoFromType(var1);
      }

      if (var2 == null && var0.getRegistry_id() >= 0) {
         var2 = getEntityInfoFromID(var0.getRegistry_id());
      }

      if (var2 != null) {
         var2.DebugPrint();
      } else {
         DebugLog.log("WorldDictionary: Cannot debug print entity: " + (var1 != null ? var1 : "unknown"));
      }

   }

   public static void DebugPrintEntity(String var0) {
      EntityInfo var1 = getEntityInfoFromType(var0);
      if (var1 != null) {
         var1.DebugPrint();
      } else {
         DebugLog.log("WorldDictionary: Cannot debug print entity: " + var0);
      }

   }

   public static void DebugPrintEntity(short var0) {
      EntityInfo var1 = getEntityInfoFromID(var0);
      if (var1 != null) {
         var1.DebugPrint();
      } else {
         DebugLog.log("WorldDictionary: Cannot debug print entity id: " + var0);
      }

   }
}
