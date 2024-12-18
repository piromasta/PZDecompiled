package zombie.inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import zombie.iso.BuildingDef;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.iso.zones.Zone;
import zombie.scripting.ScriptManager;
import zombie.scripting.itemConfig.ItemConfig;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.VehicleScript;
import zombie.util.StringUtils;

public class ItemConfigurator {
   private static final String[] vehicle_containers = new String[]{"TruckBed", "TruckBedOpen", "SeatFrontLeft", "SeatFrontRight", "SeatMiddleLeft", "SeatMiddleRight", "SeatRearLeft", "SeatRearRight", "GloveBox"};
   private static final boolean verbose = false;
   private static final boolean verbose_tiles = false;
   private static int NEXT_ID = 0;
   private static final HashMap<String, IntegerStore> STRING_INTEGER_HASH_MAP = new HashMap();
   private static final HashMap<String, IntegerStore> TILE_INTEGER_HASH_MAP = new HashMap();

   public ItemConfigurator() {
   }

   private static boolean registerString(String var0) {
      if (var0 != null && !STRING_INTEGER_HASH_MAP.containsKey(var0)) {
         STRING_INTEGER_HASH_MAP.put(var0, new IntegerStore(NEXT_ID++));
         return true;
      } else {
         return false;
      }
   }

   public static boolean registerZone(String var0) {
      return registerString(var0);
   }

   public static int GetIdForString(String var0) {
      IntegerStore var1 = (IntegerStore)STRING_INTEGER_HASH_MAP.get(var0);
      return var1 != null ? var1.get() : -1;
   }

   public static int GetIdForSprite(String var0) {
      IntegerStore var1 = (IntegerStore)TILE_INTEGER_HASH_MAP.get(var0);
      return var1 != null ? var1.get() : -1;
   }

   public static void Preprocess() {
      STRING_INTEGER_HASH_MAP.clear();
      TILE_INTEGER_HASH_MAP.clear();
      ArrayList var0 = IsoWorld.instance.MetaGrid.Zones;
      Iterator var1 = var0.iterator();

      while(var1.hasNext()) {
         Zone var2 = (Zone)var1.next();
         if (!StringUtils.isNullOrWhitespace(var2.name) && registerString(var2.name)) {
         }

         if (!StringUtils.isNullOrWhitespace(var2.type) && registerString(var2.type)) {
         }
      }

      ArrayList var7 = IsoWorld.instance.MetaGrid.Buildings;
      Iterator var8 = var7.iterator();

      Iterator var4;
      while(var8.hasNext()) {
         BuildingDef var3 = (BuildingDef)var8.next();
         var4 = var3.rooms.iterator();

         while(var4.hasNext()) {
            RoomDef var5 = (RoomDef)var4.next();
            if (registerString(var5.getName())) {
            }
         }
      }

      ArrayList var9 = ScriptManager.instance.getAllItems();
      Iterator var10 = var9.iterator();

      while(var10.hasNext()) {
         Item var13 = (Item)var10.next();
         if (var13.getType() == Item.Type.Container && registerString(var13.getName())) {
         }
      }

      String[] var11 = vehicle_containers;
      int var14 = var11.length;

      for(int var15 = 0; var15 < var14; ++var15) {
         String var6 = var11[var15];
         if (registerString(var6)) {
         }
      }

      ArrayList var12 = ScriptManager.instance.getAllVehicleScripts();
      var4 = var12.iterator();

      while(var4.hasNext()) {
         VehicleScript var16 = (VehicleScript)var4.next();
         if (var16.getName() != null && registerString(var16.getName())) {
         }
      }

      registerString("freezer");
      var4 = IsoSpriteManager.instance.NamedMap.entrySet().iterator();

      while(var4.hasNext()) {
         Map.Entry var18 = (Map.Entry)var4.next();
         if (((IsoSprite)var18.getValue()).getProperties() != null && ((IsoSprite)var18.getValue()).getProperties().Is(IsoFlagType.container) && registerString(((IsoSprite)var18.getValue()).getProperties().Val("container"))) {
         }
      }

      var4 = IsoSpriteManager.instance.IntMap.valueCollection().iterator();

      while(var4.hasNext()) {
         IsoSprite var20 = (IsoSprite)var4.next();
         if (var20 != null && var20.getID() >= 0 && var20.getName() != null) {
            TILE_INTEGER_HASH_MAP.put(var20.getName(), new IntegerStore(var20.getID()));
         }
      }

      ArrayList var17 = ScriptManager.instance.getAllItemConfigs();
      Iterator var21 = var17.iterator();

      while(var21.hasNext()) {
         ItemConfig var19 = (ItemConfig)var21.next();
         var19.BuildBuckets();
      }

   }

   public static void ConfigureItem(InventoryItem var0, ItemPickInfo var1, boolean var2, float var3) {
      if (var0 != null && var1 != null && var0.getScriptItem() != null && var0.getScriptItem().getItemConfig() != null) {
         var1.setJunk(var2);
         Item var4 = var0.getScriptItem();
         ItemConfig var5 = var4.getItemConfig();
         if (var5 != null) {
            try {
               var5.ConfigureEntitySpawned(var0, var1);
            } catch (Exception var7) {
               var7.printStackTrace();
            }

         }
      }
   }

   public static void ConfigureItemOnCreate(InventoryItem var0) {
      if (var0 != null && var0.getScriptItem() != null && var0.getScriptItem().getItemConfig() != null) {
         Item var1 = var0.getScriptItem();
         ItemConfig var2 = var1.getItemConfig();
         if (var2 != null) {
            try {
               var2.ConfigureEntityOnCreate(var0);
            } catch (Exception var4) {
               var4.printStackTrace();
            }

         }
      }
   }

   public static class IntegerStore {
      private final int id;

      public IntegerStore(int var1) {
         this.id = var1;
      }

      public int get() {
         return this.id;
      }
   }
}
