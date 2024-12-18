package zombie.inventory;

import gnu.trove.map.hash.THashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTableIterator;
import se.krka.kahlua.vm.KahluaUtil;
import zombie.SandboxOptions;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.core.stash.StashSystem;
import zombie.debug.DebugLog;
import zombie.entity.ComponentType;
import zombie.inventory.types.Clothing;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.Food;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.InventoryContainer;
import zombie.inventory.types.Key;
import zombie.inventory.types.MapItem;
import zombie.inventory.types.WeaponPart;
import zombie.iso.BuildingDef;
import zombie.iso.ContainerOverlays;
import zombie.iso.InstanceTracker;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMetaChunk;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.areas.IsoBuilding;
import zombie.iso.areas.IsoRoom;
import zombie.iso.objects.IsoClothingDryer;
import zombie.iso.objects.IsoClothingWasher;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.zones.Zone;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.radio.ZomboidRadio;
import zombie.radio.media.MediaData;
import zombie.radio.media.RecordedMedia;
import zombie.randomizedWorld.randomizedBuilding.RandomizedBuildingBase;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.Item;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.util.list.PZArrayList;
import zombie.util.list.PZArrayUtil;
import zombie.vehicles.BaseVehicle;

public final class ItemPickerJava {
   private static IsoPlayer player;
   private static float OtherLootModifier;
   private static float FoodLootModifier;
   private static float CannedFoodLootModifier;
   private static float WeaponLootModifier;
   private static float RangedWeaponLootModifier;
   private static float AmmoLootModifier;
   private static float LiteratureLootModifier;
   private static float SurvivalGearsLootModifier;
   private static float MedicalLootModifier;
   private static float BagLootModifier;
   private static float MechanicsLootModifier;
   private static float ClothingLootModifier;
   private static float ContainerLootModifier;
   private static float KeyLootModifier;
   private static float KeyLootModifierD100;
   private static float MediaLootModifier;
   private static float MementoLootModifier;
   private static float CookwareLootModifier;
   private static float MaterialLootModifier;
   private static float FarmingLootModifier;
   private static float ToolLootModifier;
   private static String OtherLootType = "Other";
   private static String FoodLootType = "Food";
   private static String CannedFoodLootType = "CannedFood";
   private static String WeaponLootType = "Weapon";
   private static String RangedWeaponLootType = "RangedWeapon";
   private static String AmmoLootType = "Ammo";
   private static String LiteratureLootType = "Literature";
   private static String SurvivalGearsLootType = "SurvivalGears";
   private static String MedicalLootType = "Medical";
   private static String MechanicsLootType = "Mechanics";
   private static String ClothingLootType = "Clothing";
   private static String ContainerLootType = "Container";
   private static String KeyLootType = "Key";
   private static String MediaLootType = "Media";
   private static String MementoLootType = "Memento";
   private static String CookwareLootType = "Cookware";
   private static String MaterialLootType = "Material";
   private static String FarmingLootType = "Farming";
   private static String ToolLootType = "Tool";
   private static String GeneratorLootType = "Generator";
   public static float zombieDensityCap = 8.0F;
   public static final ArrayList<String> NoContainerFillRooms = new ArrayList();
   public static final ArrayList<ItemPickerUpgradeWeapons> WeaponUpgrades = new ArrayList();
   public static final HashMap<String, ItemPickerUpgradeWeapons> WeaponUpgradeMap = new HashMap();
   public static final THashMap<String, ItemPickerRoom> rooms = new THashMap();
   public static final THashMap<String, ItemPickerContainer> containers = new THashMap();
   public static final THashMap<String, ItemPickerContainer> ProceduralDistributions = new THashMap();
   public static final THashMap<String, VehicleDistribution> VehicleDistributions = new THashMap();
   private static ArrayList<String> addedInvalidAlready = new ArrayList();

   public ItemPickerJava() {
   }

   public static THashMap<String, ItemPickerContainer> getItemPickerContainers() {
      return containers;
   }

   public static void Parse() {
      rooms.clear();
      NoContainerFillRooms.clear();
      WeaponUpgradeMap.clear();
      WeaponUpgrades.clear();
      containers.clear();
      addedInvalidAlready.clear();
      KahluaTableImpl var0 = (KahluaTableImpl)LuaManager.env.rawget("NoContainerFillRooms");
      Iterator var1 = var0.delegate.entrySet().iterator();

      while(var1.hasNext()) {
         Map.Entry var2 = (Map.Entry)var1.next();
         String var3 = var2.getKey().toString();
         NoContainerFillRooms.add(var3);
      }

      KahluaTableImpl var10 = (KahluaTableImpl)LuaManager.env.rawget("WeaponUpgrades");
      Iterator var11 = var10.delegate.entrySet().iterator();

      while(var11.hasNext()) {
         Map.Entry var12 = (Map.Entry)var11.next();
         String var4 = var12.getKey().toString();
         ItemPickerUpgradeWeapons var5 = new ItemPickerUpgradeWeapons();
         var5.name = var4;
         WeaponUpgrades.add(var5);
         WeaponUpgradeMap.put(var4, var5);
         KahluaTableImpl var6 = (KahluaTableImpl)var12.getValue();
         Iterator var7 = var6.delegate.entrySet().iterator();

         while(var7.hasNext()) {
            Map.Entry var8 = (Map.Entry)var7.next();
            String var9 = var8.getValue().toString();
            var5.Upgrades.add(var9);
         }
      }

      ParseSuburbsDistributions();
      ParseVehicleDistributions();
      ParseProceduralDistributions();
   }

   private static void ParseSuburbsDistributions() {
      KahluaTableImpl var0 = (KahluaTableImpl)LuaManager.env.rawget("SuburbsDistributions");
      Iterator var1 = var0.delegate.entrySet().iterator();

      while(true) {
         label129:
         while(var1.hasNext()) {
            Map.Entry var2 = (Map.Entry)var1.next();
            String var3 = var2.getKey().toString();
            KahluaTableImpl var4 = (KahluaTableImpl)var2.getValue();
            if (var4.delegate.containsKey("rolls")) {
               ItemPickerContainer var12 = ExtractContainersFromLua(var4);
               containers.put(var3, var12);
            } else {
               ItemPickerRoom var5 = new ItemPickerRoom();
               rooms.put(var3, var5);
               Iterator var6 = var4.delegate.entrySet().iterator();

               while(true) {
                  while(true) {
                     if (!var6.hasNext()) {
                        continue label129;
                     }

                     Map.Entry var7 = (Map.Entry)var6.next();
                     String var8 = var7.getKey().toString();
                     if (var7.getValue() instanceof Double) {
                        var5.fillRand = ((Double)var7.getValue()).intValue();
                     } else if ("isShop".equals(var8)) {
                        var5.isShop = (Boolean)var7.getValue();
                     } else if ("professionChance".equals(var8)) {
                        var5.professionChance = ((Double)var7.getValue()).intValue();
                     } else if ("outfit".equals(var8)) {
                        var5.outfit = (String)var7.getValue();
                     } else if ("outfitFemale".equals(var8)) {
                        var5.outfitFemale = (String)var7.getValue();
                     } else if ("outfitMale".equals(var8)) {
                        var5.outfitMale = (String)var7.getValue();
                     } else if ("outfitChance".equals(var8)) {
                        var5.outfitChance = (String)var7.getValue();
                     } else if ("vehicle".equals(var8)) {
                        var5.vehicle = (String)var7.getValue();
                     } else if ("vehicles".equals(var8)) {
                        var5.vehicles = Arrays.asList(var8.split(";"));
                     } else if ("vehicleChance".equals(var8)) {
                        var5.vehicleChance = (String)var7.getValue();
                     } else if ("vehicleDistribution".equals(var8)) {
                        var5.vehicleDistribution = (String)var7.getValue();
                     } else if ("vehicleSkin".equals(var8)) {
                        var5.vehicleSkin = (Integer)var7.getValue();
                     } else if ("femaleChance".equals(var8)) {
                        var5.femaleChance = (String)var7.getValue();
                     } else if ("roomTypes".equals(var8)) {
                        var5.roomTypes = (String)var7.getValue();
                     } else if ("zoneRequires".equals(var8)) {
                        var5.zoneRequires = (String)var7.getValue();
                     } else if ("zoneDisallows".equals(var8)) {
                        var5.zoneDisallows = (String)var7.getValue();
                     } else if ("containerChance".equals(var8)) {
                        var5.containerChance = (String)var7.getValue();
                     } else if ("femaleOdds".equals(var8)) {
                        var5.femaleOdds = (String)var7.getValue();
                     } else if ("bagType".equals(var8)) {
                        var5.bagType = (String)var7.getValue();
                     } else if ("bagTable".equals(var8)) {
                        var5.bagTable = (String)var7.getValue();
                     } else {
                        KahluaTableImpl var9 = null;

                        try {
                           var9 = (KahluaTableImpl)var7.getValue();
                        } catch (Exception var11) {
                           var11.printStackTrace();
                        }

                        if (var9.delegate.containsKey("procedural") || !var8.isEmpty() && var9.delegate.containsKey("rolls") && var9.delegate.containsKey("items")) {
                           ItemPickerContainer var10 = ExtractContainersFromLua(var9);
                           var5.Containers.put(var8, var10);
                        } else {
                           DebugLog.ItemPicker.error("ERROR: SuburbsDistributions[\"" + var3 + "\"] is broken");
                        }
                     }
                  }
               }
            }
         }

         return;
      }
   }

   private static void ParseVehicleDistributions() {
      VehicleDistributions.clear();
      KahluaTableImpl var0 = (KahluaTableImpl)LuaManager.env.rawget("VehicleDistributions");
      if (var0 != null && var0.rawget(1) instanceof KahluaTableImpl) {
         var0 = (KahluaTableImpl)var0.rawget(1);
         Iterator var1 = var0.delegate.entrySet().iterator();

         while(true) {
            Map.Entry var2;
            do {
               do {
                  if (!var1.hasNext()) {
                     return;
                  }

                  var2 = (Map.Entry)var1.next();
               } while(!(var2.getKey() instanceof String));
            } while(!(var2.getValue() instanceof KahluaTableImpl));

            KahluaTableImpl var3 = (KahluaTableImpl)var2.getValue();
            VehicleDistribution var4 = new VehicleDistribution();
            KahluaTableImpl var5;
            if (var3.rawget("Normal") instanceof KahluaTableImpl) {
               var5 = (KahluaTableImpl)var3.rawget("Normal");
               ItemPickerRoom var6 = new ItemPickerRoom();
               Iterator var7 = var5.delegate.entrySet().iterator();

               while(var7.hasNext()) {
                  Map.Entry var8 = (Map.Entry)var7.next();
                  String var9 = var8.getKey().toString();
                  if (!var9.equals("specificId")) {
                     var6.Containers.put(var9, ExtractContainersFromLua((KahluaTableImpl)var8.getValue()));
                  }
               }

               var4.Normal = var6;
            }

            if (var3.rawget("Specific") instanceof KahluaTableImpl) {
               var5 = (KahluaTableImpl)var3.rawget("Specific");

               for(int var12 = 1; var12 <= var5.len(); ++var12) {
                  KahluaTableImpl var13 = (KahluaTableImpl)var5.rawget(var12);
                  ItemPickerRoom var14 = new ItemPickerRoom();
                  Iterator var15 = var13.delegate.entrySet().iterator();

                  while(var15.hasNext()) {
                     Map.Entry var10 = (Map.Entry)var15.next();
                     String var11 = var10.getKey().toString();
                     if (var11.equals("specificId")) {
                        var14.specificId = (String)var10.getValue();
                     } else {
                        var14.Containers.put(var11, ExtractContainersFromLua((KahluaTableImpl)var10.getValue()));
                     }
                  }

                  var4.Specific.add(var14);
               }
            }

            if (var4.Normal != null) {
               VehicleDistributions.put((String)var2.getKey(), var4);
            }
         }
      }
   }

   private static void ParseProceduralDistributions() {
      ProceduralDistributions.clear();
      KahluaTableImpl var0 = (KahluaTableImpl)Type.tryCastTo(LuaManager.env.rawget("ProceduralDistributions"), KahluaTableImpl.class);
      if (var0 != null) {
         KahluaTableImpl var1 = (KahluaTableImpl)Type.tryCastTo(var0.rawget("list"), KahluaTableImpl.class);
         if (var1 != null) {
            Iterator var2 = var1.delegate.entrySet().iterator();

            while(var2.hasNext()) {
               Map.Entry var3 = (Map.Entry)var2.next();
               String var4 = var3.getKey().toString();
               KahluaTableImpl var5 = (KahluaTableImpl)var3.getValue();
               ItemPickerContainer var6 = ExtractContainersFromLua(var5);
               ProceduralDistributions.put(var4, var6);
            }

         }
      }
   }

   private static ItemPickerContainer ExtractContainersFromLua(KahluaTableImpl var0) {
      ItemPickerContainer var1 = new ItemPickerContainer();
      if (var0.delegate.containsKey("procedural")) {
         var1.procedural = var0.rawgetBool("procedural");
         var1.proceduralItems = ExtractProcList(var0);
         return var1;
      } else {
         if (var0.delegate.containsKey("noAutoAge")) {
            var1.noAutoAge = var0.rawgetBool("noAutoAge");
         }

         if (var0.delegate.containsKey("fillRand")) {
            var1.fillRand = var0.rawgetInt("fillRand");
         }

         if (var0.delegate.containsKey("maxMap")) {
            var1.maxMap = var0.rawgetInt("maxMap");
         }

         if (var0.delegate.containsKey("stashChance")) {
            var1.stashChance = var0.rawgetInt("stashChance");
         }

         if (var0.delegate.containsKey("dontSpawnAmmo")) {
            var1.dontSpawnAmmo = var0.rawgetBool("dontSpawnAmmo");
         }

         if (var0.delegate.containsKey("ignoreZombieDensity")) {
            var1.ignoreZombieDensity = var0.rawgetBool("ignoreZombieDensity");
         }

         if (var0.delegate.containsKey("cookFood")) {
            var1.cookFood = var0.rawgetBool("cookFood");
         }

         if (var0.delegate.containsKey("canBurn")) {
            var1.canBurn = var0.rawgetBool("canBurn");
         }

         if (var0.delegate.containsKey("isTrash")) {
            var1.isTrash = var0.rawgetBool("isTrash");
         }

         if (var0.delegate.containsKey("isWorn")) {
            var1.isWorn = var0.rawgetBool("isWorn");
         }

         if (var0.delegate.containsKey("isRotten")) {
            var1.isRotten = var0.rawgetBool("isRotten");
         }

         if (var0.delegate.containsKey("onlyOne")) {
            var1.onlyOne = var0.rawgetBool("onlyOne");
         }

         double var2 = (Double)var0.delegate.get("rolls");
         if (var0.delegate.containsKey("junk")) {
            var1.junk = ExtractContainersFromLua((KahluaTableImpl)var0.rawget("junk"));
         }

         if (var0.delegate.containsKey("bags")) {
            var1.bags = ExtractContainersFromLua((KahluaTableImpl)var0.rawget("bags"));
         }

         if (var0.delegate.containsKey("defaultInventoryLoot")) {
            var1.defaultInventoryLoot = var0.rawgetBool("defaultInventoryLoot");
         }

         var1.rolls = (float)((int)var2);
         KahluaTableImpl var4 = (KahluaTableImpl)var0.delegate.get("items");
         ArrayList var5 = new ArrayList();
         int var6 = var4.len();

         for(int var7 = 0; var7 < var6; var7 += 2) {
            String var8 = (String)Type.tryCastTo(var4.delegate.get(KahluaUtil.toDouble((long)(var7 + 1))), String.class);
            Double var9 = (Double)Type.tryCastTo(var4.delegate.get(KahluaUtil.toDouble((long)(var7 + 2))), Double.class);
            if (var8 != null && var9 != null) {
               Item var10 = ScriptManager.instance.FindItem(var8);
               boolean var11 = var10 != null || InventoryItemFactory.getItem(var8, true) != null;
               if (!var11 || var10 != null && var10.OBSOLETE) {
                  if (Core.bDebug && !addedInvalidAlready.contains(var8)) {
                     addedInvalidAlready.add(var8);
                     DebugLog.ItemPicker.println("ignoring invalid ItemPicker item type \"%s\", obsolete = \"%s\"", var8, var10 != null);
                  }
               } else {
                  ItemPickerItem var12 = new ItemPickerItem();
                  var12.itemName = var8;
                  var12.chance = var9.floatValue();
                  var5.add(var12);
               }
            }
         }

         var1.Items = (ItemPickerItem[])var5.toArray(var1.Items);
         return var1;
      }
   }

   private static ArrayList<ProceduralItem> ExtractProcList(KahluaTableImpl var0) {
      ArrayList var1 = new ArrayList();
      KahluaTableImpl var2 = (KahluaTableImpl)var0.rawget("procList");

      ProceduralItem var5;
      for(KahluaTableIterator var3 = var2.iterator(); var3.advance(); var1.add(var5)) {
         KahluaTableImpl var4 = (KahluaTableImpl)var3.getValue();
         var5 = new ProceduralItem();
         var5.name = var4.rawgetStr("name");
         var5.min = var4.rawgetInt("min");
         var5.max = var4.rawgetInt("max");
         var5.weightChance = var4.rawgetInt("weightChance");
         String var6 = var4.rawgetStr("forceForItems");
         String var7 = var4.rawgetStr("forceForZones");
         String var8 = var4.rawgetStr("forceForTiles");
         String var9 = var4.rawgetStr("forceForRooms");
         if (!StringUtils.isNullOrWhitespace(var6)) {
            var5.forceForItems = Arrays.asList(var6.split(";"));
         }

         if (!StringUtils.isNullOrWhitespace(var7)) {
            var5.forceForZones = Arrays.asList(var7.split(";"));
         }

         if (!StringUtils.isNullOrWhitespace(var8)) {
            var5.forceForTiles = Arrays.asList(var8.split(";"));
         }

         if (!StringUtils.isNullOrWhitespace(var9)) {
            var5.forceForRooms = Arrays.asList(var9.split(";"));
         }
      }

      return var1;
   }

   public static void InitSandboxLootSettings() {
      OtherLootModifier = (float)SandboxOptions.getInstance().OtherLootNew.getValue();
      FoodLootModifier = (float)SandboxOptions.getInstance().FoodLootNew.getValue();
      WeaponLootModifier = (float)SandboxOptions.getInstance().WeaponLootNew.getValue();
      RangedWeaponLootModifier = (float)SandboxOptions.getInstance().RangedWeaponLootNew.getValue();
      AmmoLootModifier = (float)SandboxOptions.getInstance().AmmoLootNew.getValue();
      CannedFoodLootModifier = (float)SandboxOptions.getInstance().CannedFoodLootNew.getValue();
      LiteratureLootModifier = (float)SandboxOptions.getInstance().LiteratureLootNew.getValue();
      SurvivalGearsLootModifier = (float)SandboxOptions.getInstance().SurvivalGearsLootNew.getValue();
      MedicalLootModifier = (float)SandboxOptions.getInstance().MedicalLootNew.getValue();
      MechanicsLootModifier = (float)SandboxOptions.getInstance().MechanicsLootNew.getValue();
      ClothingLootModifier = (float)SandboxOptions.getInstance().ClothingLootNew.getValue();
      ContainerLootModifier = (float)SandboxOptions.getInstance().ContainerLootNew.getValue();
      KeyLootModifier = (float)SandboxOptions.getInstance().KeyLootNew.getValue();
      KeyLootModifierD100 = (float)SandboxOptions.getInstance().KeyLootNew.getValue();
      MediaLootModifier = (float)SandboxOptions.getInstance().MediaLootNew.getValue();
      MementoLootModifier = (float)SandboxOptions.getInstance().MementoLootNew.getValue();
      CookwareLootModifier = (float)SandboxOptions.getInstance().CookwareLootNew.getValue();
      MaterialLootModifier = (float)SandboxOptions.getInstance().MaterialLootNew.getValue();
      FarmingLootModifier = (float)SandboxOptions.getInstance().FarmingLootNew.getValue();
      ToolLootModifier = (float)SandboxOptions.getInstance().ToolLootNew.getValue();
   }

   private static float doSandboxSettings(int var0) {
      switch (var0) {
         case 1:
            return 0.0F;
         case 2:
            return (float)SandboxOptions.instance.InsaneLootFactor.getValue();
         case 3:
            return (float)SandboxOptions.instance.ExtremeLootFactor.getValue();
         case 4:
            return (float)SandboxOptions.instance.RareLootFactor.getValue();
         case 5:
            return (float)SandboxOptions.instance.NormalLootFactor.getValue();
         case 6:
            return (float)SandboxOptions.instance.CommonLootFactor.getValue();
         case 7:
            return (float)SandboxOptions.instance.AbundantLootFactor.getValue();
         default:
            return 0.6F;
      }
   }

   public static void fillContainer(ItemContainer var0, IsoPlayer var1) {
      ItemPickInfo var2 = ItemPickInfo.GetPickInfo(var0, ItemPickInfo.Caller.FillContainer);
      fillContainerInternal(var2, var0, var1);
   }

   private static void fillContainerInternal(ItemPickInfo var0, ItemContainer var1, IsoPlayer var2) {
      if (!GameClient.bClient && !"Tutorial".equals(Core.GameMode)) {
         if (var1 != null) {
            IsoGridSquare var3 = var1.getSourceGrid();
            IsoRoom var4 = null;
            if (var3 != null) {
               var4 = var3.getRoom();
               ItemPickerContainer var6;
               if (!var1.getType().equals("inventorymale") && !var1.getType().equals("inventoryfemale")) {
                  ItemPickerRoom var10 = null;
                  if (rooms.containsKey("all")) {
                     var10 = (ItemPickerRoom)rooms.get("all");
                  }

                  String var11;
                  if (var4 != null && rooms.containsKey(var4.getName())) {
                     var11 = var4.getName();
                     ItemPickerRoom var14 = (ItemPickerRoom)rooms.get(var11);
                     ItemPickerContainer var13 = null;
                     if (var14.Containers.containsKey(var1.getType())) {
                        var13 = (ItemPickerContainer)var14.Containers.get(var1.getType());
                     }

                     if (var13 == null && var14.Containers.containsKey("other")) {
                        var13 = (ItemPickerContainer)var14.Containers.get("other");
                     }

                     if (var13 == null && var14.Containers.containsKey("all")) {
                        var13 = (ItemPickerContainer)var14.Containers.get("all");
                        var11 = "all";
                     }

                     if (var13 == null) {
                        fillContainerTypeInternal(var0, var10, var1, var11, var2);
                        LuaEventManager.triggerEvent("OnFillContainer", var11, var1.getType(), var1);
                     } else {
                        if (rooms.containsKey(var4.getName())) {
                           var10 = (ItemPickerRoom)rooms.get(var4.getName());
                        }

                        if (var10 != null) {
                           fillContainerTypeInternal(var0, var10, var1, var4.getName(), var2);
                           LuaEventManager.triggerEvent("OnFillContainer", var4.getName(), var1.getType(), var1);
                        }

                     }
                  } else {
                     var6 = null;
                     if (var4 != null) {
                        var11 = var4.getName();
                     } else {
                        var11 = "all";
                     }

                     fillContainerTypeInternal(var0, var10, var1, var11, var2);
                     LuaEventManager.triggerEvent("OnFillContainer", var11, var1.getType(), var1);
                  }
               } else if (var1.getParent() == null || !(var1.getParent() instanceof IsoDeadBody) || !((IsoDeadBody)var1.getParent()).isSkeleton()) {
                  String var5 = var1.getType();
                  if (var1.getParent() != null && var1.getParent() instanceof IsoDeadBody) {
                     var5 = ((IsoDeadBody)var1.getParent()).getOutfitName();
                  }

                  var6 = (ItemPickerContainer)((ItemPickerRoom)rooms.get("all")).Containers.get("Outfit_" + var5);

                  for(int var7 = 0; var7 < var1.getItems().size(); ++var7) {
                     InventoryItem var8 = (InventoryItem)var1.getItems().get(var7);
                     if (var8 instanceof InventoryContainer) {
                        ItemPickerContainer var9 = (ItemPickerContainer)containers.get(var8.getType());
                        if (var6 != null && var6.bags != null && !var8.hasTag("BagsFillException")) {
                           rollContainerItemInternal(var0, (InventoryContainer)var8, (IsoGameCharacter)null, var6.bags);
                           LuaEventManager.triggerEvent("OnFillContainer", "Zombie Bag", var8.getType(), var6.bags);
                        } else if (var9 != null && Rand.Next(var9.fillRand) == 0) {
                           rollContainerItemInternal(var0, (InventoryContainer)var8, (IsoGameCharacter)null, (ItemPickerContainer)containers.get(var8.getType()));
                           LuaEventManager.triggerEvent("OnFillContainer", "Zombie Bag", var8.getType(), ((InventoryContainer)var8).getItemContainer());
                        }
                     }
                  }

                  boolean var12 = true;
                  if (var6 != null) {
                     var12 = var6.defaultInventoryLoot;
                  }

                  if (var6 != null) {
                     rollItemInternal(var0, var6, var1, true, var2, (ItemPickerRoom)null);
                  }

                  if (var12) {
                     var6 = (ItemPickerContainer)((ItemPickerRoom)rooms.get("all")).Containers.get(var1.getType());
                     rollItemInternal(var0, var6, var1, true, var2, (ItemPickerRoom)null);
                  }

                  InstanceTracker.inc("Container Rolls", "Zombie/" + var5);
                  LuaEventManager.triggerEvent("OnFillContainer", "Zombie", var5, var1);
               }
            }
         }
      }
   }

   public static void fillContainerType(ItemPickerRoom var0, ItemContainer var1, String var2, IsoGameCharacter var3) {
      ItemPickInfo var4 = ItemPickInfo.GetPickInfo(var1, ItemPickInfo.Caller.FillContainerType);
      if (var4 != null) {
         var4.updateRoomDist(var0);
      }

      fillContainerTypeInternal(var4, var0, var1, var2, var3);
   }

   private static void fillContainerTypeInternal(ItemPickInfo var0, ItemPickerRoom var1, ItemContainer var2, String var3, IsoGameCharacter var4) {
      boolean var5 = true;
      if (NoContainerFillRooms.contains(var3)) {
         var5 = false;
      }

      ItemPickerContainer var6 = null;
      if (var1 == null) {
         var6 = (ItemPickerContainer)var1.Containers.get("all");
         rollItemInternal(var0, var6, var2, var5, var4, var1);
      } else if (var1.Containers.containsKey("all")) {
         var6 = (ItemPickerContainer)var1.Containers.get("all");
         rollItemInternal(var0, var6, var2, var5, var4, var1);
      }

      String var10001 = StringUtils.isNullOrEmpty(var3) ? "unknown" : var3;
      InstanceTracker.inc("Container Rolls", var10001 + "/" + var2.getType());
      var6 = (ItemPickerContainer)var1.Containers.get(var2.getType());
      if (var6 == null) {
         var6 = (ItemPickerContainer)var1.Containers.get("other");
      }

      if (var6 != null) {
         rollItemInternal(var0, var6, var2, var5, var4, var1);
      }

   }

   public static InventoryItem tryAddItemToContainer(ItemContainer var0, String var1, ItemPickerContainer var2) {
      Item var3 = ScriptManager.instance.FindItem(var1);
      if (var3 == null) {
         return null;
      } else if (var3.OBSOLETE) {
         return null;
      } else {
         float var4 = var3.getActualWeight() * (float)var3.getCount();
         if (!var0.hasRoomFor((IsoGameCharacter)null, var4)) {
            return null;
         } else {
            boolean var5 = var0.getContainingItem() instanceof InventoryContainer && var0.getContainingItem().getContainer() != null && var0.getContainingItem().getContainer().getParent() != null && var0.getContainingItem().getContainer().getParent() instanceof IsoDeadBody;
            if (!var5 && var0.getContainingItem() instanceof InventoryContainer) {
               ItemContainer var6 = var0.getContainingItem().getContainer();
               if (var6 != null && !var6.hasRoomFor((IsoGameCharacter)null, var4)) {
                  return null;
               }
            }

            return ItemSpawner.spawnItem(var1, var0);
         }
      }
   }

   private static void rollProceduralItem(ArrayList<ProceduralItem> var0, ItemContainer var1, float var2, IsoGameCharacter var3, ItemPickerRoom var4) {
      ItemPickInfo var5 = ItemPickInfo.GetPickInfo(var1, ItemPickInfo.Caller.RollProceduralItem);
      if (var5 != null) {
         var5.updateRoomDist(var4);
      }

      rollProceduralItemInternal(var5, var0, var1, var2, var3, var4);
   }

   private static void rollProceduralItemInternal(ItemPickInfo var0, ArrayList<ProceduralItem> var1, ItemContainer var2, float var3, IsoGameCharacter var4, ItemPickerRoom var5) {
      if (var2.getSourceGrid() != null) {
         boolean var6 = var2.getSourceGrid().getRoom() != null;
         HashMap var7 = null;
         if (var6) {
            var7 = var2.getSourceGrid().getRoom().getRoomDef().getProceduralSpawnedContainer();
         }

         HashMap var8 = new HashMap();
         HashMap var9 = new HashMap();

         for(int var10 = 0; var10 < var1.size(); ++var10) {
            ProceduralItem var11 = (ProceduralItem)var1.get(var10);
            String var12 = var11.name;
            int var13 = var11.min;
            int var14 = var11.max;
            int var15 = var11.weightChance;
            List var16 = var11.forceForItems;
            List var17 = var11.forceForZones;
            List var18 = var11.forceForTiles;
            List var19 = var11.forceForRooms;
            if (var7 != null && var7.get(var12) == null) {
               var7.put(var12, 0);
            }

            int var28;
            if (var16 != null && var6 && var2.getSourceGrid() != null && var2.getSourceGrid().getRoom() != null && var2.getSourceGrid().getRoom().getBuilding() != null && var2.getSourceGrid().getRoom().getBuilding().getRoomsNumber() <= RandomizedBuildingBase.maximumRoomCount) {
               for(int var27 = var2.getSourceGrid().getRoom().getRoomDef().x; var27 < var2.getSourceGrid().getRoom().getRoomDef().x2; ++var27) {
                  for(var28 = var2.getSourceGrid().getRoom().getRoomDef().y; var28 < var2.getSourceGrid().getRoom().getRoomDef().y2; ++var28) {
                     IsoGridSquare var30 = var2.getSourceGrid().getCell().getGridSquare(var27, var28, var2.getSourceGrid().z);
                     if (var30 != null) {
                        for(int var23 = 0; var23 < var30.getObjects().size(); ++var23) {
                           IsoObject var24 = (IsoObject)var30.getObjects().get(var23);
                           if (var16.contains(var24.getSprite().name)) {
                              var8.clear();
                              var8.put(var12, -1);
                              break;
                           }
                        }
                     }
                  }
               }
            } else {
               IsoGridSquare var20;
               if (var18 == null) {
                  if (var19 != null && var6) {
                     var20 = var2.getSourceGrid();
                     if (var20 != null) {
                        for(var28 = 0; var28 < var19.size(); ++var28) {
                           if (var20.getBuilding().getRandomRoom((String)var19.get(var28)) != null) {
                              var8.clear();
                              var8.put(var12, -1);
                              break;
                           }
                        }
                     }
                  }
               } else {
                  var20 = var2.getSourceGrid();
                  if (var20 != null) {
                     for(var28 = 0; var28 < var20.getObjects().size(); ++var28) {
                        IsoObject var29 = (IsoObject)var20.getObjects().get(var28);
                        if (var29.getSprite() != null && var18.contains(var29.getSprite().getName())) {
                           var8.clear();
                           var8.put(var12, -1);
                           break;
                        }
                     }
                  } else if (var17 != null) {
                     ArrayList var21 = IsoWorld.instance.MetaGrid.getZonesAt(var2.getSourceGrid().x, var2.getSourceGrid().y, 0);

                     for(int var22 = 0; var22 < var21.size(); ++var22) {
                        if ((var7 == null || (Integer)var7.get(var12) < var14) && (var17.contains(((Zone)var21.get(var22)).type) || var17.contains(((Zone)var21.get(var22)).name))) {
                           var8.clear();
                           var8.put(var12, -1);
                           break;
                        }
                     }
                  }
               }
            }

            if (var16 == null && var17 == null && var18 == null && var19 == null) {
               if (var6 && var13 == 1 && (Integer)var7.get(var12) == 0) {
                  var8.put(var12, var15);
               } else if (!var6 || (Integer)var7.get(var12) < var14) {
                  var9.put(var12, var15);
               }
            }
         }

         String var25 = null;
         if (!var8.isEmpty()) {
            var25 = getDistribInHashMap(var8);
         } else if (!var9.isEmpty()) {
            var25 = getDistribInHashMap(var9);
         }

         if (var25 != null) {
            ItemPickerContainer var26 = (ItemPickerContainer)ProceduralDistributions.get(var25);
            if (var26 != null) {
               if (var26.junk != null) {
                  doRollItemInternal(var0, var26.junk, var2, var3, var4, true, var5, true);
               }

               doRollItemInternal(var0, var26, var2, var3, var4, true, var5);
               if (var7 != null) {
                  var7.put(var25, (Integer)var7.get(var25) + 1);
               }

            }
         }
      }
   }

   private static String getDistribInHashMap(HashMap<String, Integer> var0) {
      int var1 = 0;
      int var3 = 0;

      Iterator var4;
      String var5;
      for(var4 = var0.keySet().iterator(); var4.hasNext(); var1 += (Integer)var0.get(var5)) {
         var5 = (String)var4.next();
      }

      int var2;
      if (var1 == -1) {
         var2 = Rand.Next(var0.size());
         var4 = var0.keySet().iterator();

         for(int var7 = 0; var4.hasNext(); ++var7) {
            if (var7 == var2) {
               return (String)var4.next();
            }
         }
      }

      var2 = Rand.Next(var1);
      var4 = var0.keySet().iterator();

      do {
         if (!var4.hasNext()) {
            return null;
         }

         var5 = (String)var4.next();
         int var6 = (Integer)var0.get(var5);
         var3 += var6;
      } while(var3 < var2);

      return var5;
   }

   public static void rollItem(ItemPickerContainer var0, ItemContainer var1, boolean var2, IsoGameCharacter var3, ItemPickerRoom var4) {
      ItemPickInfo var5 = ItemPickInfo.GetPickInfo(var1, ItemPickInfo.Caller.RollItem);
      if (var5 != null) {
         var5.updateRoomDist(var4);
      }

      rollItemInternal(var5, var0, var1, var2, var3, var4);
   }

   private static void rollItemInternal(ItemPickInfo var0, ItemPickerContainer var1, ItemContainer var2, boolean var3, IsoGameCharacter var4, ItemPickerRoom var5) {
      if (!GameClient.bClient && !GameServer.bServer) {
         player = IsoPlayer.getInstance();
      }

      if (var1 != null && var2 != null) {
         float var6 = getZombieDensityFactor(var1, var2);
         if (var1.procedural) {
            rollProceduralItemInternal(var0, var1.proceduralItems, var2, var6, var4, var5);
         } else {
            if (var1.junk != null) {
               doRollItemInternal(var0, var1.junk, var2, var6, var4, var3, var5, true);
            }

            doRollItemInternal(var0, var1, var2, var6, var4, var3, var5);
         }
      }

   }

   public static void doRollItem(ItemPickerContainer var0, ItemContainer var1, float var2, IsoGameCharacter var3, boolean var4, ItemPickerRoom var5) {
      ItemPickInfo var6 = ItemPickInfo.GetPickInfo(var1, ItemPickInfo.Caller.DoRollItem);
      if (var6 != null) {
         var6.updateRoomDist(var5);
      }

      doRollItemInternal(var6, var0, var1, var2, var3, var4, var5);
   }

   private static void doRollItemInternal(ItemPickInfo var0, ItemPickerContainer var1, ItemContainer var2, float var3, IsoGameCharacter var4, boolean var5, ItemPickerRoom var6) {
      doRollItemInternal(var0, var1, var2, var3, var4, var5, var6, false);
   }

   private static void doRollItemInternal(ItemPickInfo var0, ItemPickerContainer var1, ItemContainer var2, float var3, IsoGameCharacter var4, boolean var5, ItemPickerRoom var6, boolean var7) {
      IsoObject var8 = null;
      if (var2.getParent() != null) {
         var8 = var2.getParent();
      }

      String var9 = "";
      boolean var10 = false;
      boolean var11 = false;
      boolean var12 = false;
      if ((var8 instanceof IsoClothingDryer || Objects.equals(var2.getType(), "clothingdryer")) && Rand.NextBool(5)) {
         var11 = true;
      }

      if (var8 instanceof IsoClothingWasher || Objects.equals(var2.getType(), "clothingwasher")) {
         if (Rand.NextBool(2)) {
            var11 = true;
         } else {
            var10 = true;
         }
      }

      int var13 = (int)((double)var1.rolls * SandboxOptions.instance.RollsMultiplier.getValue());
      var13 = Math.max(var13, 1);

      for(int var14 = 0; var14 < var13; ++var14) {
         ItemPickerItem[] var15 = var1.Items;

         for(int var16 = 0; var16 < var15.length; ++var16) {
            ItemPickerItem var17 = var15[var16];
            var9 = var17.itemName;
            Item var18 = ScriptManager.instance.FindItem(var9);
            if (var18 == null && var9.endsWith("Empty")) {
               var9 = var9.substring(0, var9.length() - 5);
               var18 = ScriptManager.instance.FindItem(var9, true);
               if (var18 != null) {
                  if (!var18.containsComponent(ComponentType.FluidContainer)) {
                     var18 = null;
                  } else {
                     var12 = true;
                  }
               }
            }

            if ((float)Rand.Next(10000) <= getActualSpawnChance(var17, var4, var2, var3, var7)) {
               InventoryItem var19 = tryAddItemToContainer(var2, var9, var1);
               if (var19 == null) {
                  return;
               }

               boolean var20 = false;
               boolean var21 = var1.isTrash;
               if (var6 != null) {
                  var20 = var6.isShop;
               }

               float var22 = getAdjustedZombieDensity(var3, var18, var7);
               ItemConfigurator.ConfigureItem(var19, var0, var7, var22);
               if (!var20) {
                  checkStashItem(var19, var1);
               }

               if (var2.getType().equals("freezer") && var19 instanceof Food && ((Food)var19).isFreezing()) {
                  ((Food)var19).freeze();
               }

               int var25;
               label407: {
                  if (var19 instanceof Key) {
                     Key var23 = (Key)var19;
                     if (var19.hasTag("BuildingKey") && !var2.getType().equals("inventoryfemale") && !var2.getType().equals("inventorymale")) {
                        var23.takeKeyId();
                        if (var2.getSourceGrid() != null && var2.getSourceGrid().getBuilding() != null && var2.getSourceGrid().getBuilding().getDef() != null) {
                           BuildingDef var24 = var2.getSourceGrid().getBuilding().getDef();
                           var25 = var24.getKeySpawned();
                           int var26 = var2.getSourceGrid().getBuilding().getRoomsNumber() / 10 + 1;
                           if (var26 < 2) {
                              var26 = 2;
                           }

                           if (var25 <= var26 && var2.getCountTagRecurse("BuildingKey") <= 1) {
                              var24.setKeySpawned(var25 + 1);
                              ItemPickerJava.KeyNamer.nameKey(var19, var2.getSourceGrid());
                           } else {
                              var2.Remove(var19);
                           }
                        } else {
                           var2.Remove(var19);
                        }
                        break label407;
                     }
                  }

                  if (var19 instanceof Key && var19.hasTag("BuildingKey") && (var2.getType().equals("inventoryfemale") || var2.getType().equals("inventorymale"))) {
                     var2.Remove(var19);
                  }
               }

               if (var19 instanceof Key && (var19.getFullType().equals("Base.CarKey") || var19.hasTag(".CarKey"))) {
                  addVehicleKeyAsLoot(var19, var2);
               }

               String var27 = var19.getScriptItem().getRecordedMediaCat();
               if (var27 != null) {
                  RecordedMedia var28 = ZomboidRadio.getInstance().getRecordedMedia();
                  MediaData var31 = var28.getRandomFromCategory(var27);
                  if (var31 == null) {
                     var2.Remove(var19);
                     if ("Home-VHS".equalsIgnoreCase(var27)) {
                        var31 = var28.getRandomFromCategory("Retail-VHS");
                        if (var31 == null) {
                           return;
                        }

                        var19 = ItemSpawner.spawnItem("Base.VHS_Retail", var2);
                        if (var19 == null) {
                           return;
                        }

                        var19.setRecordedMediaData(var31);
                     }

                     return;
                  }

                  var19.setRecordedMediaData(var31);
               }

               if (!var1.noAutoAge) {
                  var19.setAutoAge();
               }

               if (!var21 && WeaponUpgradeMap.containsKey(var19.getType())) {
                  DoWeaponUpgrade(var19);
               }

               if (var19 instanceof DrainableComboItem && var19.hasTag("LessFull") && !var20 && !var21 && Rand.Next(100) < 80) {
                  ((DrainableComboItem)var19).randomizeUses();
               } else if (var19 instanceof DrainableComboItem && !var20 && !var21 && Rand.Next(100) < 40) {
                  ((DrainableComboItem)var19).randomizeUses();
               }

               if (var12 && var19.hasComponent(ComponentType.FluidContainer)) {
                  var19.getFluidContainer().Empty();
               }

               if (!var20 && !var21 && var19 instanceof HandWeapon && Rand.Next(100) < 40 && ((HandWeapon)var19).getPhysicsObject() == null) {
                  var19.setConditionNoSound(Rand.Next(1, var19.getConditionMax()));
               }

               if (!var20 && !var21 && var19.hasHeadCondition() && Rand.Next(100) < 40) {
                  var19.setHeadCondition(Rand.Next(1, var19.getHeadConditionMax()));
               }

               if (!var20 && !var21 && var19.hasSharpness() && Rand.Next(100) < 40) {
                  var19.setSharpness(Rand.Next(0.0F, var19.getMaxSharpness()));
               }

               if (!var21 && var19 instanceof HandWeapon) {
                  HandWeapon var29 = (HandWeapon)var19;
                  if (!var1.dontSpawnAmmo && Rand.Next(100) < 90) {
                     var25 = 30;
                     if (Core.getInstance().getOptionReloadDifficulty() > 1 && !StringUtils.isNullOrEmpty(var29.getMagazineType()) && Rand.Next(100) < 90) {
                        if (Rand.NextBool(3)) {
                           InventoryItem var37 = ItemSpawner.spawnItem(var29.getMagazineType(), var2);
                           if (Rand.NextBool(5)) {
                              var37.setCurrentAmmoCount(Rand.Next(1, var37.getMaxAmmo()));
                           }

                           if (!Rand.NextBool(5)) {
                              var37.setCurrentAmmoCount(var37.getMaxAmmo());
                           }
                        } else {
                           if (!StringUtils.isNullOrWhitespace(var29.getMagazineType())) {
                              var29.setContainsClip(true);
                           }

                           if (Rand.NextBool(6)) {
                              var29.setCurrentAmmoCount(Rand.Next(1, var29.getMaxAmmo()));
                           } else {
                              var25 = Rand.Next(60, 100);
                           }
                        }

                        if (var29.haveChamber() & Rand.NextBool(5)) {
                           var29.setRoundChambered(true);
                        }
                     }

                     if (Core.getInstance().getOptionReloadDifficulty() == 1 || StringUtils.isNullOrEmpty(var29.getMagazineType()) && Rand.Next(100) < 30) {
                        var29.setCurrentAmmoCount(Rand.Next(1, var29.getMaxAmmo()));
                        if (var29.haveChamber()) {
                           var29.setRoundChambered(true);
                        }
                     }

                     if (!StringUtils.isNullOrEmpty(var29.getAmmoBox()) && Rand.Next(100) < var25) {
                        ItemSpawner.spawnItem(var29.getAmmoBox(), var2);
                     } else if (!StringUtils.isNullOrEmpty(var29.getAmmoType()) && Rand.Next(100) < 50) {
                        ItemSpawner.spawnItems(var29.getAmmoType(), Rand.Next(1, 5), var2);
                     }
                  }
               }

               if (!var20 && var19 instanceof InventoryContainer) {
                  if (var1.bags != null && !var19.hasTag("BagsFillException")) {
                     rollContainerItemInternal(var0, (InventoryContainer)var19, var4, var1.bags);
                     LuaEventManager.triggerEvent("OnFillContainer", "Container", var19.getType(), var1.bags);
                  } else if (containers.containsKey(var19.getType())) {
                     ItemPickerContainer var30 = (ItemPickerContainer)containers.get(var19.getType());
                     if (var5 && Rand.Next(var30.fillRand) == 0) {
                        rollContainerItemInternal(var0, (InventoryContainer)var19, var4, (ItemPickerContainer)containers.get(var19.getType()));
                        if (((ItemPickerContainer)containers.get(var19.getType())).junk != null) {
                           rollContainerItemInternal(var0, (InventoryContainer)var19, var4, ((ItemPickerContainer)containers.get(var19.getType())).junk, true);
                        }

                        if (var19.hasTag("NeverEmpty") && ((InventoryContainer)var19).getItemContainer().isEmpty()) {
                           var2.Remove(var19);
                        } else {
                           LuaEventManager.triggerEvent("OnFillContainer", "Container", var19.getType(), ((InventoryContainer)var19).getItemContainer());
                        }
                     }
                  }
               }

               if (var19 instanceof Food) {
                  Food var32 = (Food)var19;
                  if (var19.isCookable() && (var1.cookFood || var1.canBurn) && var32.getReplaceOnCooked() == null) {
                     if (var1.canBurn && var32.getMinutesToBurn() > 0.0F && Rand.Next(100) < 25) {
                        var32.setBurnt(true);
                     } else {
                        var32.setCooked(true);
                        var32.setAutoAge();
                     }
                  }
               }

               if (var1.isTrash) {
                  trashItem(var19);
               }

               if (var1.isWorn) {
                  wearDownItem(var19);
               }

               if (var1.isRotten) {
                  rotItem(var19);
               }

               if (var19 instanceof Food && var19.hasTag("SpawnCooked")) {
                  var19.setCooked(true);
               }

               if (var19.hasTag("Regional")) {
                  IsoGridSquare var33 = var2.getSquare();
                  if (var33 != null) {
                     String var36 = getSquareRegion(var33);
                     Object var38 = LuaManager.getFunctionObject("SpecialLootSpawns.OnCreateRegion." + var19.getType());
                     if (var38 != null) {
                        LuaManager.caller.pcallvoid(LuaManager.thread, var38, var19, var36);
                     }
                  }
               }

               if (var19 instanceof Clothing) {
                  Clothing var34 = (Clothing)var19;
                  if (var10 || var11) {
                     if (var10) {
                        var34.randomizeCondition(0, 75, 1, 0);
                     }

                     if (var11) {
                        var34.randomizeCondition(100, 0, 0, 0);
                     }
                  }
               }

               if (!StringUtils.isNullOrEmpty(var19.getScriptItem().getSpawnWith()) && !var21 && !var1.isWorn) {
                  InventoryItem var35 = ItemSpawner.spawnItem(var19.getScriptItem().getSpawnWith(), var2);
                  if (var35 != null) {
                     var35.copyClothing(var19);
                  }
               }

               if (var8 != null && var19.hasTag("ApplyOwnerName") && var8 instanceof IsoDeadBody && ((IsoDeadBody)var8).getDescriptor() != null) {
                  var19.nameAfterDescriptor(((IsoDeadBody)var8).getDescriptor());
               } else if (var8 != null && var19.hasTag("MonogramOwnerName") && var8 instanceof IsoDeadBody && ((IsoDeadBody)var8).getDescriptor() != null) {
                  var19.monogramAfterDescriptor(((IsoDeadBody)var8).getDescriptor());
               }

               if (var19 instanceof DrainableComboItem && !var19.isKeepOnDeplete() && var19.getCurrentUses() <= 0) {
                  var19.setCurrentUses(1);
               }

               if (var1.onlyOne) {
                  return;
               }
            }
         }
      }

   }

   private static void checkStashItem(InventoryItem var0, ItemPickerContainer var1) {
      if (var1.stashChance > 0 && var0 instanceof MapItem && !StringUtils.isNullOrEmpty(((MapItem)var0).getMapID())) {
         var0.setStashChance(var1.stashChance);
      }

      StashSystem.checkStashItem(var0);
   }

   public static void rollContainerItem(InventoryContainer var0, IsoGameCharacter var1, ItemPickerContainer var2) {
      ItemPickInfo var3 = ItemPickInfo.GetPickInfo(var0.getItemContainer(), ItemPickInfo.Caller.RollContainerItem);
      rollContainerItemInternal(var3, var0, var1, var2);
      if (var2.junk != null) {
         rollContainerItemInternal(var3, var0, var1, var2.junk, true);
      }

   }

   private static void rollContainerItemInternal(ItemPickInfo var0, InventoryContainer var1, IsoGameCharacter var2, ItemPickerContainer var3) {
      rollContainerItemInternal(var0, var1, var2, var3, false);
   }

   private static void rollContainerItemInternal(ItemPickInfo var0, InventoryContainer var1, IsoGameCharacter var2, ItemPickerContainer var3, boolean var4) {
      if (var3 != null) {
         IsoObject var5 = null;
         if (var1.getOutermostContainer() != null && var1.getOutermostContainer().getParent() != null) {
            var5 = var1.getOutermostContainer().getParent();
         }

         ItemContainer var6 = var1.getInventory();
         float var7 = getZombieDensityFactor(var3, var6);
         String var8 = "";
         int var9 = (int)((double)var3.rolls * SandboxOptions.instance.RollsMultiplier.getValue());
         var9 = Math.max(var9, 1);

         for(int var10 = 0; var10 < var9; ++var10) {
            ItemPickerItem[] var11 = var3.Items;

            for(int var12 = 0; var12 < var11.length; ++var12) {
               ItemPickerItem var13 = var11[var12];
               var8 = var13.itemName;
               Item var14 = ScriptManager.instance.FindItem(var8);
               boolean var15 = false;
               if (var14 == null && var8.endsWith("Empty")) {
                  var8 = var8.substring(0, var8.length() - 5);
                  var14 = ScriptManager.instance.FindItem(var8, true);
                  if (var14 != null) {
                     if (!var14.containsComponent(ComponentType.FluidContainer)) {
                        continue;
                     }

                     var15 = true;
                  }
               }

               if ((float)Rand.Next(10000) <= getActualSpawnChance(var13, var2, var6, var7, var4)) {
                  InventoryItem var16 = tryAddItemToContainer(var6, var8, var3);
                  if (var16 == null) {
                     return;
                  }

                  ItemConfigurator.ConfigureItem(var16, var0, false, 0.0F);
                  MapItem var17 = (MapItem)Type.tryCastTo(var16, MapItem.class);
                  int var19;
                  if (var17 != null && !StringUtils.isNullOrEmpty(var17.getMapID()) && var3.maxMap > 0) {
                     int var18 = 0;

                     for(var19 = 0; var19 < var6.getItems().size(); ++var19) {
                        MapItem var20 = (MapItem)Type.tryCastTo((InventoryItem)var6.getItems().get(var19), MapItem.class);
                        if (var20 != null && !StringUtils.isNullOrEmpty(var20.getMapID())) {
                           ++var18;
                        }
                     }

                     if (var18 > var3.maxMap) {
                        var6.Remove(var16);
                     }
                  }

                  checkStashItem(var16, var3);
                  if (!var3.isTrash && var16 instanceof HandWeapon) {
                     HandWeapon var23 = (HandWeapon)var16;
                     if (!var3.dontSpawnAmmo && Rand.Next(100) < 90) {
                        var19 = 30;
                        boolean var25 = var1.getType().contains("HollowBook");
                        if (Core.getInstance().getOptionReloadDifficulty() > 1 && !StringUtils.isNullOrEmpty(var23.getMagazineType()) && (var25 || Rand.Next(100) < 90)) {
                           if (Rand.NextBool(3)) {
                              InventoryItem var21 = ItemSpawner.spawnItem(var23.getMagazineType(), var6);
                              if (var25 || Rand.NextBool(5)) {
                                 var21.setCurrentAmmoCount(Rand.Next(1, var21.getMaxAmmo()));
                              }

                              if (!Rand.NextBool(5)) {
                                 var21.setCurrentAmmoCount(var21.getMaxAmmo());
                              }
                           } else {
                              if (!StringUtils.isNullOrWhitespace(var23.getMagazineType())) {
                                 var23.setContainsClip(true);
                              }

                              if (!var25 && !Rand.NextBool(6)) {
                                 var19 = Rand.Next(60, 100);
                              } else {
                                 var23.setCurrentAmmoCount(Rand.Next(1, var23.getMaxAmmo()));
                              }
                           }

                           if (var23.haveChamber() & Rand.NextBool(5)) {
                              var23.setRoundChambered(true);
                           }
                        }

                        if (Core.getInstance().getOptionReloadDifficulty() == 1 || StringUtils.isNullOrEmpty(var23.getMagazineType()) && (var25 || Rand.Next(100) < 30)) {
                           var23.setCurrentAmmoCount(Rand.Next(1, var23.getMaxAmmo()));
                           if (var23.haveChamber() & Rand.NextBool(5)) {
                              var23.setRoundChambered(true);
                           }
                        }

                        if (!StringUtils.isNullOrEmpty(var23.getAmmoBox()) && Rand.Next(100) < var19) {
                           ItemSpawner.spawnItem(var23.getAmmoBox(), var6);
                        } else if (!StringUtils.isNullOrEmpty(var23.getAmmoType()) && Rand.Next(100) < 50) {
                           ItemSpawner.spawnItems(var23.getAmmoType(), Rand.Next(1, 5), var6);
                        }
                     }
                  }

                  if (!var3.isTrash && var16 instanceof HandWeapon && Rand.Next(100) < 40 && ((HandWeapon)var16).getPhysicsObject() == null) {
                     var16.setConditionNoSound(Rand.Next(1, var16.getConditionMax()));
                  }

                  if (!var3.isTrash && var16.hasHeadCondition() && Rand.Next(100) < 40) {
                     var16.setHeadCondition(Rand.Next(1, var16.getHeadConditionMax()));
                  }

                  if (!var3.isTrash && var16.hasSharpness() && Rand.Next(100) < 40) {
                     var16.setSharpness(Rand.Next(0.0F, var16.getMaxSharpness()));
                  }

                  if (var6.getType().equals("freezer") && var16 instanceof Food && ((Food)var16).isFreezing()) {
                     ((Food)var16).freeze();
                  }

                  if (var16 instanceof DrainableComboItem && var16.hasTag("LessFull") && Rand.Next(100) < 80) {
                     ((DrainableComboItem)var16).randomizeUses();
                  } else if (var16 instanceof DrainableComboItem && Rand.Next(100) < 40) {
                     ((DrainableComboItem)var16).randomizeUses();
                  }

                  if (var15 && var16.hasComponent(ComponentType.FluidContainer)) {
                     var16.getFluidContainer().Empty();
                  }

                  ItemContainer var24 = var1.getOutermostContainer();
                  if (var16 instanceof Key) {
                     Key var26 = (Key)var16;
                     if (var16.hasTag("BuildingKey") && var24 != null && var24.getType() != null) {
                        var26.takeKeyId();
                        BuildingDef var27 = null;
                        if (var24.getSquare() != null && var24.getSquare().getBuilding() != null && var24.getSquare().getBuilding().getDef() != null) {
                           var27 = var24.getSquare().getBuilding().getDef();
                        }

                        if (var27 != null) {
                           int var29 = var27.getKeySpawned();
                           int var22 = var24.getSquare().getBuilding().getRoomsNumber() / 5 + 1;
                           if (var22 < 2) {
                              var22 = 2;
                           }

                           if (var29 <= var22 && var24.getCountTagRecurse("BuildingKey") <= 1) {
                              var27.setKeySpawned(var29 + 1);
                              ItemPickerJava.KeyNamer.nameKey(var16, var24.getSquare());
                           } else {
                              var6.Remove(var16);
                           }
                        } else {
                           var6.Remove(var16);
                        }
                     }
                  }

                  String var28 = var16.getScriptItem().getRecordedMediaCat();
                  if (var28 != null) {
                     RecordedMedia var30 = ZomboidRadio.getInstance().getRecordedMedia();
                     MediaData var31 = var30.getRandomFromCategory(var28);
                     if (var31 == null) {
                        var6.Remove(var16);
                        if ("Home-VHS".equalsIgnoreCase(var28)) {
                           var31 = var30.getRandomFromCategory("Retail-VHS");
                           if (var31 == null) {
                              return;
                           }

                           var16 = ItemSpawner.spawnItem("Base.VHS_Retail", var6);
                           if (var16 == null) {
                              return;
                           }

                           var16.setRecordedMediaData(var31);
                        }

                        return;
                     }

                     var16.setRecordedMediaData(var31);
                  }

                  if (var16 instanceof InventoryContainer) {
                     if (var3.bags != null && !var16.hasTag("BagsFillException")) {
                        rollContainerItemInternal(var0, (InventoryContainer)var16, var2, var3.bags);
                        LuaEventManager.triggerEvent("OnFillContainer", "Container", var16.getType(), var3.bags);
                     } else if (containers.containsKey(var16.getType())) {
                        ItemPickerContainer var10000 = (ItemPickerContainer)containers.get(var16.getType());
                        rollContainerItemInternal(var0, (InventoryContainer)var16, var2, (ItemPickerContainer)containers.get(var16.getType()));
                        if (((ItemPickerContainer)containers.get(var16.getType())).junk != null) {
                           rollContainerItemInternal(var0, (InventoryContainer)var16, var2, ((ItemPickerContainer)containers.get(var16.getType())).junk, true);
                        }

                        if (var16.hasTag("NeverEmpty") && ((InventoryContainer)var16).getItemContainer().isEmpty()) {
                           var6.Remove(var16);
                        } else {
                           LuaEventManager.triggerEvent("OnFillContainer", "Container", var16.getType(), ((InventoryContainer)var16).getItemContainer());
                        }
                     }
                  }

                  if (var16 instanceof Key && (var16.getFullType().equals("Base.CarKey") || var16.hasTag(".CarKey"))) {
                     addVehicleKeyAsLoot(var16, var6);
                  }

                  if (!var6.getType().equals("freezer")) {
                     var16.setAutoAge();
                  }

                  if (var3.isTrash) {
                     trashItem(var16);
                  }

                  if (var3.isWorn) {
                     wearDownItem(var16);
                  }

                  if (var16 instanceof Food && var16.hasTag("SpawnCooked")) {
                     var16.setCooked(true);
                  }

                  if (var16.hasTag("Regional") && var24 != null && var24.getSquare() != null) {
                     String var32 = getSquareRegion(var24.getSquare());
                     Object var33 = LuaManager.getFunctionObject("SpecialLootSpawns.OnCreateRegion." + var16.getType());
                     if (var33 != null) {
                        LuaManager.caller.pcallvoid(LuaManager.thread, var33, var16, var32);
                     }
                  }

                  if (!StringUtils.isNullOrEmpty(var16.getScriptItem().getSpawnWith()) && !var3.isWorn && !var3.isTrash) {
                     InventoryItem var34 = ItemSpawner.spawnItem(var16.getScriptItem().getSpawnWith(), var6);
                     if (var34 != null) {
                        var34.copyClothing(var16);
                     }
                  }

                  if (var5 != null && var16.hasTag("ApplyOwnerName") && var5 instanceof IsoDeadBody && ((IsoDeadBody)var5).getDescriptor() != null) {
                     var16.nameAfterDescriptor(((IsoDeadBody)var5).getDescriptor());
                  } else if (var5 != null && var16.hasTag("MonogramOwnerName") && var5 instanceof IsoDeadBody && ((IsoDeadBody)var5).getDescriptor() != null) {
                     var16.monogramAfterDescriptor(((IsoDeadBody)var5).getDescriptor());
                  }

                  if (var16 instanceof DrainableComboItem && !var16.isKeepOnDeplete() && var16.getCurrentUses() <= 0) {
                     var16.setCurrentUses(1);
                  }

                  if (var3.onlyOne) {
                     return;
                  }
               }
            }
         }
      }

   }

   private static void DoWeaponUpgrade(InventoryItem var0) {
      ItemPickerUpgradeWeapons var1 = (ItemPickerUpgradeWeapons)WeaponUpgradeMap.get(var0.getType());
      if (var1 != null) {
         if (var1.Upgrades.size() != 0) {
            int var2 = Rand.Next(var1.Upgrades.size());

            for(int var3 = 0; var3 < var2; ++var3) {
               String var4 = (String)PZArrayUtil.pickRandom((List)var1.Upgrades);
               InventoryItem var5 = InventoryItemFactory.CreateItem(var4);
               if (var5 != null) {
                  ((HandWeapon)var0).attachWeaponPart((WeaponPart)var5);
               }
            }

         }
      }
   }

   public static float getLootModifier(String var0) {
      Item var1 = ScriptManager.instance.FindItem(var0);
      if (var1 == null) {
         return 0.6F;
      } else if (!SandboxOptions.instance.LootItemRemovalList.getSplitCSVList().contains(var0) && !SandboxOptions.instance.LootItemRemovalList.getSplitCSVList().contains(ScriptManager.getItemName(var0))) {
         String var2 = getLootType(var1);
         return getLootModifierFromType(var2);
      } else {
         return 0.0F;
      }
   }

   public static float getLootModifierFromType(String var0) {
      float var1 = OtherLootModifier;
      if (Objects.equals(var0, GeneratorLootType)) {
         return doSandboxSettings(SandboxOptions.instance.GeneratorSpawning.getValue());
      } else if (Objects.equals(var0, MementoLootType)) {
         return MementoLootModifier;
      } else if (Objects.equals(var0, MedicalLootType)) {
         return MedicalLootModifier;
      } else if (Objects.equals(var0, MechanicsLootType)) {
         return MechanicsLootModifier;
      } else if (Objects.equals(var0, MaterialLootType)) {
         return MaterialLootModifier;
      } else if (Objects.equals(var0, FarmingLootType)) {
         return FarmingLootModifier;
      } else if (Objects.equals(var0, ToolLootType)) {
         return ToolLootModifier;
      } else if (Objects.equals(var0, CookwareLootType)) {
         return CookwareLootModifier;
      } else if (Objects.equals(var0, SurvivalGearsLootType)) {
         return SurvivalGearsLootModifier;
      } else if (Objects.equals(var0, CannedFoodLootType)) {
         return CannedFoodLootModifier;
      } else if (Objects.equals(var0, FoodLootType)) {
         return FoodLootModifier;
      } else if (Objects.equals(var0, AmmoLootType)) {
         return AmmoLootModifier;
      } else if (Objects.equals(var0, WeaponLootType)) {
         return WeaponLootModifier;
      } else if (Objects.equals(var0, RangedWeaponLootType)) {
         return RangedWeaponLootModifier;
      } else if (Objects.equals(var0, KeyLootType)) {
         return KeyLootModifier;
      } else if (Objects.equals(var0, ContainerLootType)) {
         return ContainerLootModifier;
      } else if (Objects.equals(var0, LiteratureLootType)) {
         return LiteratureLootModifier;
      } else if (Objects.equals(var0, ClothingLootType)) {
         return ClothingLootModifier;
      } else {
         return Objects.equals(var0, MediaLootType) ? MediaLootModifier : var1;
      }
   }

   public static String getLootType(Item var0) {
      if (!Objects.equals(var0.getName(), "Generator") && !Objects.equals(var0.getFullName(), "Base.Generator") && !var0.hasTag("Generator")) {
         if (var0.isMementoLoot()) {
            return MementoLootType;
         } else if (var0.isMedicalLoot()) {
            return MedicalLootType;
         } else if (var0.isMechanicsLoot()) {
            return MechanicsLootType;
         } else if (var0.isMaterialLoot()) {
            return MaterialLootType;
         } else if (var0.isFarmingLoot()) {
            return FarmingLootType;
         } else if (var0.isToolLoot()) {
            return ToolLootType;
         } else if (var0.isCookwareLoot()) {
            return CookwareLootType;
         } else if (var0.isSurvivalGearLoot()) {
            return SurvivalGearsLootType;
         } else if (var0.getType() != Item.Type.Food && !"Food".equals(var0.getDisplayCategory())) {
            if (!"Ammo".equals(var0.getDisplayCategory()) && !var0.hasTag("AmmoCase") && (var0.getType() != Item.Type.Normal || StringUtils.isNullOrEmpty(var0.getAmmoType()))) {
               if (var0.getType() == Item.Type.Weapon && !var0.isRanged()) {
                  return WeaponLootType;
               } else if (var0.getType() != Item.Type.WeaponPart && (var0.getType() != Item.Type.Weapon || !var0.isRanged()) && !var0.hasTag("FirearmLoot")) {
                  if (var0.getType() != Item.Type.Key && var0.getType() != Item.Type.KeyRing && !var0.hasTag("KeyRing")) {
                     if (var0.Capacity <= 0 && var0.getType() != Item.Type.Container && !"Bag".equals(var0.getDisplayCategory())) {
                        if (var0.getType() == Item.Type.Literature) {
                           return LiteratureLootType;
                        } else if (var0.getType() == Item.Type.Clothing) {
                           return ClothingLootType;
                        } else {
                           return var0.getRecordedMediaCat() != null ? MediaLootType : OtherLootType;
                        }
                     } else {
                        return ContainerLootType;
                     }
                  } else {
                     return KeyLootType;
                  }
               } else {
                  return RangedWeaponLootType;
               }
            } else {
               return AmmoLootType;
            }
         } else {
            return !var0.CannedFood && var0.getDaysFresh() != 1000000000 && var0.getType() == Item.Type.Food ? FoodLootType : CannedFoodLootType;
         }
      } else {
         return GeneratorLootType;
      }
   }

   public static void updateOverlaySprite(IsoObject var0) {
      ContainerOverlays.instance.updateContainerOverlaySprite(var0);
   }

   public static void doOverlaySprite(IsoGridSquare var0) {
      if (!GameClient.bClient) {
         if (var0 != null && var0.getRoom() != null && !var0.isOverlayDone()) {
            PZArrayList var1 = var0.getObjects();

            for(int var2 = 0; var2 < var1.size(); ++var2) {
               IsoObject var3 = (IsoObject)var1.get(var2);
               if (var3 != null && var3.getContainer() != null && !var3.getContainer().isExplored()) {
                  fillContainer(var3.getContainer(), IsoPlayer.getInstance());
                  var3.getContainer().setExplored(true);
                  if (GameServer.bServer) {
                     LuaManager.GlobalObject.sendItemsInContainer(var3, var3.getContainer());
                  }
               }

               updateOverlaySprite(var3);
            }

            var0.setOverlayDone(true);
         }
      }
   }

   public static ItemPickerContainer getItemContainer(String var0, String var1, String var2, boolean var3) {
      ItemPickerRoom var4 = (ItemPickerRoom)rooms.get(var0);
      if (var4 == null) {
         return null;
      } else {
         ItemPickerContainer var5 = (ItemPickerContainer)var4.Containers.get(var1);
         if (var5 != null && var5.procedural) {
            ArrayList var6 = var5.proceduralItems;

            for(int var7 = 0; var7 < var6.size(); ++var7) {
               ProceduralItem var8 = (ProceduralItem)var6.get(var7);
               if (var2.equals(var8.name)) {
                  ItemPickerContainer var9 = (ItemPickerContainer)ProceduralDistributions.get(var2);
                  if (var9.junk != null && var3) {
                     return var9.junk;
                  }

                  if (!var3) {
                     return var9;
                  }
               }
            }
         }

         return var3 && var5 != null ? var5.junk : var5;
      }
   }

   public static void keyNamerBuilding(InventoryItem var0, IsoGridSquare var1) {
      ItemPickerJava.KeyNamer.nameKey(var0, var1);
   }

   public static void trashItem(InventoryItem var0) {
      if (var0 instanceof HandWeapon && ((HandWeapon)var0).getPhysicsObject() == null) {
         if (Rand.Next(100) < 95) {
            var0.setConditionNoSound(1);
         } else {
            var0.setConditionNoSound(1);
         }
      }

      if (var0.hasHeadCondition()) {
         if (Rand.NextBool(2)) {
            var0.setHeadCondition(Rand.Next(1, var0.getHeadConditionMax()));
         } else {
            var0.setHeadCondition(1);
         }
      }

      if (var0.hasSharpness()) {
         if (Rand.NextBool(2)) {
            var0.setSharpness(Rand.Next(0.0F, var0.getMaxSharpness()));
         } else {
            var0.setSharpness(0.0F);
         }
      }

      if (var0 instanceof DrainableComboItem) {
         if (Rand.Next(100) < 90) {
            var0.setCurrentUses(1);
         } else {
            ((DrainableComboItem)var0).randomizeUses();
         }
      }

      Item var1 = var0.getScriptItem();
      if (var0 instanceof Food var2) {
         if (!var1.CannedFood || !var1.CantEat) {
            boolean var3 = false;
            boolean var4 = false;
            if (!var2.hasTag("Vermin") && var2.isCookable() && !var1.CannedFood && var2.getReplaceOnCooked() == null && Rand.Next(100) < 75) {
               if (Rand.Next(100) < 50) {
                  var2.setCooked(true);
               } else {
                  var3 = true;
                  var2.setBurnt(true);
               }
            }

            if (!var2.isRotten() && var2.getOffAgeMax() < 1000000000 && Rand.Next(100) < 95) {
               var2.setRotten(true);
               var2.setAge((float)var2.getOffAgeMax());
               var4 = true;
            } else if (var2.isFresh() && var2.getOffAge() < 1000000000 && Rand.Next(100) < 95) {
               var2.setAge((float)var2.getOffAge());
               var4 = true;
            }

            if (var4 && Rand.Next(2) == 0) {
               var4 = false;
            }

            if (var2.isbDangerousUncooked() && !var2.isCooked()) {
               var4 = true;
            }

            if (var2.hasTag("Vermin")) {
               var4 = true;
            }

            double var5 = (double)(var2.getBaseHunger() * 100.0F * -1.0F) + 0.1;
            double var7 = (double)(var2.getHungerChange() * 100.0F * -1.0F) + 0.1;
            if (var7 < var5) {
               var5 = var7;
            }

            if (!var3 && !var4 && Rand.Next(100) != 0) {
               if (var5 >= 4.0) {
                  int var9 = Rand.Next(8);
                  if (var9 == 0) {
                     var2.multiplyFoodValues(0.75F);
                  } else if (var9 <= 2) {
                     var2.multiplyFoodValues(0.5F);
                  } else {
                     var2.multiplyFoodValues(0.25F);
                  }
               } else if (var5 >= 2.0) {
                  var2.multiplyFoodValues(0.5F);
               }
            }
         }
      }

      if (var0 instanceof Clothing var10) {
         var10.randomizeCondition(25, 95, 10, 75);
      }

   }

   public static void trashItemLooted(InventoryItem var0) {
      if (var0 instanceof HandWeapon && ((HandWeapon)var0).getPhysicsObject() == null && Rand.Next(100) < 50) {
         var0.setCondition(1, false);
      }

      if (var0.hasHeadCondition()) {
         var0.setHeadCondition(Rand.Next(1, var0.getHeadConditionMax()));
      }

      if (var0.hasSharpness()) {
         var0.setSharpness(Rand.Next(0.0F, var0.getMaxSharpness()));
      }

      if (var0 instanceof DrainableComboItem) {
         if (Rand.Next(100) < 75) {
            var0.setCurrentUses(1);
         } else {
            ((DrainableComboItem)var0).randomizeUses();
         }
      }

      Item var1 = var0.getScriptItem();
      if (var0 instanceof Food var2) {
         if (!var1.CannedFood || !var1.CantEat) {
            boolean var3 = var2.isbDangerousUncooked() && !var2.isCooked();
            if (var2.hasTag("Vermin")) {
               var3 = true;
            }

            double var4 = (double)(var2.getBaseHunger() * 100.0F * -1.0F) + 0.1;
            double var6 = (double)(var2.getHungerChange() * 100.0F * -1.0F) + 0.1;
            if (var6 < var4) {
               var4 = var6;
            }

            if (!var3 && Rand.Next(100) < 75) {
               if (var4 >= 4.0) {
                  int var8 = Rand.Next(8);
                  if (var8 == 0) {
                     var2.multiplyFoodValues(0.75F);
                  } else if (var8 <= 2) {
                     var2.multiplyFoodValues(0.5F);
                  } else {
                     var2.multiplyFoodValues(0.25F);
                  }
               } else if (var4 >= 2.0) {
                  var2.multiplyFoodValues(0.5F);
               }
            }
         }
      }

      if (var0 instanceof Clothing var9) {
         var9.randomizeCondition(10, 50, 10, 50);
      }

   }

   public static void trashItemRats(InventoryItem var0) {
      if (var0 instanceof HandWeapon && Rand.Next(100) < 75) {
         wearDownItem(var0);
      }

      Item var1 = var0.getScriptItem();
      if (var0 instanceof Food var2) {
         if (!var1.CannedFood || !var1.CantEat) {
            double var3 = (double)(var2.getBaseHunger() * 100.0F * -1.0F) + 0.1;
            double var5 = (double)(var2.getHungerChange() * 100.0F * -1.0F) + 0.1;
            if (var5 < var3) {
               var3 = var5;
            }

            if (var3 >= 4.0) {
               int var7 = Rand.Next(8);
               if (var7 == 0) {
                  var2.multiplyFoodValues(0.75F);
               } else if (var7 <= 2) {
                  var2.multiplyFoodValues(0.5F);
               } else {
                  var2.multiplyFoodValues(0.25F);
               }
            } else if (var3 >= 2.0) {
               var2.multiplyFoodValues(0.5F);
            }
         }
      }

      if (var0 instanceof Clothing var8) {
         var8.randomizeCondition(25, 95, 10, 95);
      }

   }

   public static void wearDownItem(InventoryItem var0) {
      int var1;
      if (var0 instanceof HandWeapon) {
         if (Rand.Next(100) < 25) {
            var0.setCondition(1, false);
         } else {
            var1 = Rand.Next(var0.getConditionMax());
            int var2 = Rand.Next(var0.getConditionMax());
            int var3 = Rand.Next(var0.getConditionMax());
            if (var2 < var1) {
               var1 = var2;
            }

            if (var3 < var1) {
               var1 = var3;
            }

            var0.setCondition(var1 + 1, false);
         }
      }

      if (var0.hasHeadCondition()) {
         var0.setHeadCondition(Rand.Next(1, var0.getHeadConditionMax()));
      }

      if (var0.hasSharpness()) {
         var0.setSharpness(Rand.Next(0.0F, var0.getMaxSharpness()));
      }

      if (var0 instanceof DrainableComboItem) {
         var1 = var0.getMaxUses();
         --var1;
         if (Rand.Next(100) < 75) {
            var0.setCurrentUses(1);
         } else {
            ((DrainableComboItem)var0).randomizeUses();
         }
      }

      Item var12 = var0.getScriptItem();
      if (var0 instanceof Food var10) {
         if (!var12.CannedFood || !var12.CantEat) {
            boolean var13 = false;
            boolean var4 = false;
            if (!var10.hasTag("Vermin") && var10.isCookable() && var10.getReplaceOnCooked() == null && Rand.Next(100) < 50) {
               if (Rand.Next(100) < 50) {
                  var10.setCooked(true);
               } else {
                  var13 = true;
                  var10.setBurnt(true);
               }
            }

            if (!var10.isRotten() && var10.getOffAgeMax() < 1000000000 && Rand.Next(100) < 75) {
               var10.setRotten(true);
               var10.setAge((float)var10.getOffAgeMax());
               var4 = true;
            } else if (var10.isFresh() && var10.getOffAge() < 1000000000 && Rand.Next(100) < 75) {
               var10.setAge((float)var10.getOffAge());
               var4 = true;
            }

            if (var4 && Rand.Next(2) == 0) {
               var4 = false;
            }

            if (var10.isbDangerousUncooked() && !var10.isCooked()) {
               var4 = true;
            }

            if (var10.hasTag("Vermin")) {
               var4 = true;
            }

            double var5 = (double)(var10.getBaseHunger() * 100.0F * -1.0F) + 0.1;
            double var7 = (double)(var10.getHungerChange() * 100.0F * -1.0F) + 0.1;
            if (var7 < var5) {
               var5 = var7;
            }

            if (!var13 && !var4 && Rand.Next(100) < 75) {
               if (var5 >= 4.0) {
                  int var9 = Rand.Next(8);
                  if (var9 <= 2) {
                     var10.multiplyFoodValues(0.75F);
                  } else if (var9 <= 4) {
                     var10.multiplyFoodValues(0.5F);
                  } else {
                     var10.multiplyFoodValues(0.25F);
                  }
               } else if (var5 >= 2.0) {
                  var10.multiplyFoodValues(0.5F);
               }
            }
         }
      }

      if (var0 instanceof Clothing var11) {
         var11.randomizeCondition(0, 25, 1, 25);
      }

   }

   public static void rotItem(InventoryItem var0) {
      Item var1 = var0.getScriptItem();
      if (var0 instanceof Food var2) {
         if ((!var1.CannedFood || !var1.CantEat) && var2.getOffAgeMax() < 1000000000) {
            if (var2.isRotten()) {
               return;
            }

            if (Rand.Next(100) < 75) {
               var2.setRotten(true);
               var2.setAge((float)var2.getOffAgeMax());
            } else if (var2.isFresh() && Rand.Next(100) < 95) {
               var2.setAge((float)var2.getOffAge());
            }
         }
      }

      if (var0 instanceof Clothing var3) {
         var3.randomizeCondition(0, 75, 1, 25);
      }

   }

   public static void spawnLootCarKey(InventoryItem var0, ItemContainer var1) {
      spawnLootCarKey(var0, var1, var1);
   }

   public static void spawnLootCarKey(InventoryItem var0, ItemContainer var1, ItemContainer var2) {
      ArrayList var3 = IsoWorld.instance.CurrentCell.getVehicles();
      if (var3.size() < 1) {
         var1.Remove(var0);
      } else {
         BaseVehicle var4 = (BaseVehicle)var3.get(Rand.Next(var3.size()));
         if (var4 != null && !var4.isPreviouslyMoved() && isGoodKey(var4.getScriptName())) {
            Key var5 = (Key)var0;
            var5.setKeyId(var4.getKeyId());
            var4.setPreviouslyMoved(true);
            var4.keySpawned = 1;
            BaseVehicle.keyNamerVehicle(var5, var4);
            Color var6 = Color.HSBtoRGB(var4.colorHue, var4.colorSaturation * 0.5F, var4.colorValue);
            var5.setColor(var6);
            var5.setCustomColor(true);
            if ((float)Rand.Next(100) < 1.0F * KeyLootModifierD100 && var2.getSourceGrid() != null && var2.getSourceGrid().getBuilding() != null && var2.getSourceGrid().getBuilding().getDef() != null) {
               var4.addBuildingKeyToGloveBox(var2.getSourceGrid());
            }
         } else {
            var1.Remove(var0);
         }
      }

   }

   public static boolean isGoodKey(String var0) {
      return !var0.contains("Burnt") && !var0.contains("Smashed") && !var0.equals("TrailerAdvert") && !var0.equals("TrailerCover") && !var0.equals("Trailer");
   }

   public static boolean addVehicleKeyAsLoot(InventoryItem var0, ItemContainer var1) {
      if (var1.getCountTagRecurse("CarKey") < 2) {
         spawnLootCarKey(var0, var1);
         return true;
      } else {
         var1.Remove(var0);
         return false;
      }
   }

   public static boolean containerHasZone(ItemContainer var0, String var1) {
      return squareHasZone(var0.getSourceGrid(), var1);
   }

   public static boolean squareHasZone(IsoGridSquare var0, String var1) {
      ArrayList var2 = IsoWorld.instance.MetaGrid.getZonesAt(var0.x, var0.y, 0);

      for(int var3 = 0; var3 < var2.size(); ++var3) {
         if (Objects.equals(((Zone)var2.get(var3)).name, var1) || Objects.equals(((Zone)var2.get(var3)).type, var1)) {
            return true;
         }
      }

      return false;
   }

   public static String getContainerZombiesType(ItemContainer var0) {
      return var0 != null && var0.getSourceGrid() != null ? getSquareZombiesType(var0.getSourceGrid()) : null;
   }

   public static String getSquareZombiesType(IsoGridSquare var0) {
      return var0.getSquareZombiesType();
   }

   public static String getSquareBuildingName(IsoGridSquare var0) {
      ArrayList var1 = LuaManager.GlobalObject.getZones(var0.x, var0.y, 0);

      for(int var2 = 0; var2 < var1.size(); ++var2) {
         if (Objects.equals(((Zone)var1.get(var2)).type, "BuildingName")) {
            return ((Zone)var1.get(var2)).name;
         }
      }

      return null;
   }

   public static String getSquareRegion(IsoGridSquare var0) {
      return var0.getSquareRegion();
   }

   public static float getBaseChance(ItemPickerItem var0, IsoGameCharacter var1, boolean var2) {
      String var3 = var0.itemName;
      Item var4 = ScriptManager.instance.FindItem(var3);
      return var0.chance * getBaseChanceMultiplier(var1, var2, var4);
   }

   public static float getBaseChanceMultiplier(IsoGameCharacter var0, boolean var1, Item var2) {
      float var3 = 1.0F;
      if (player != null && var0 != null) {
         if (var0.Traits.Lucky.isSet()) {
            var3 = (float)((double)var3 * 1.1);
         } else if (var0.Traits.Unlucky.isSet()) {
            var3 = (float)((double)var3 * 0.9);
         }
      }

      if (var1) {
         var3 = (float)((double)var3 * 1.4);
      }

      if (var2 != null && var2.hasTag("MoreWhenNoZombies") && (SandboxOptions.instance.Zombies.getValue() == 6 || SandboxOptions.instance.zombieConfig.PopulationMultiplier.getValue() == 0.0)) {
         var3 = (float)((double)var3 * 2.0);
      }

      return var3;
   }

   public static float getLootModifier(String var0, boolean var1) {
      float var2 = getLootModifier(var0);
      if (var1 && var2 > 0.0F) {
         var2 = 1.0F;
      }

      return var2;
   }

   public static float getAdjustedZombieDensity(float var0, Item var1, boolean var2) {
      return !var2 && (var1 == null || !var1.ignoreZombieDensity()) ? var0 : 0.0F;
   }

   public static float getActualSpawnChance(ItemPickerItem var0, IsoGameCharacter var1, ItemContainer var2, float var3, boolean var4) {
      String var5 = var0.itemName;
      Item var6 = ScriptManager.instance.FindItem(var5);
      float var7 = getLootModifier(var5, var4);
      float var8 = getBaseChance(var0, var1, var4);
      var3 = getAdjustedZombieDensity(var3, var6, var4);
      float var9;
      if (var2.getSourceGrid() != null) {
         var9 = SandboxOptions.instance.getCurrentLootMultiplier(var2.getSourceGrid());
      } else {
         var9 = SandboxOptions.instance.getCurrentLootMultiplier();
      }

      return (var8 * 100.0F * var7 + var3) * var9;
   }

   public static float getZombieDensityFactor(ItemPickerContainer var0, ItemContainer var1) {
      float var2 = 0.0F;
      if (!var0.ignoreZombieDensity && IsoWorld.instance != null && SandboxOptions.instance.ZombiePopLootEffect.getValue() != 0) {
         IsoMetaChunk var3 = null;
         if (player != null) {
            var3 = IsoWorld.instance.getMetaChunk(PZMath.fastfloor(player.getX() / 8.0F), PZMath.fastfloor(player.getY() / 8.0F));
         } else if (var1.getSourceGrid() != null) {
            var3 = IsoWorld.instance.getMetaChunk(PZMath.fastfloor((float)var1.getSourceGrid().getX() / 8.0F), PZMath.fastfloor((float)var1.getSourceGrid().getY() / 8.0F));
         }

         if (var3 != null) {
            var2 = var3.getLootZombieIntensity();
         }

         var2 = Math.min(var2, zombieDensityCap);
         return var2 * (float)SandboxOptions.instance.ZombiePopLootEffect.getValue();
      } else {
         return var2;
      }
   }

   public static final class ItemPickerUpgradeWeapons {
      public String name;
      public ArrayList<String> Upgrades = new ArrayList();

      public ItemPickerUpgradeWeapons() {
      }
   }

   public static final class ItemPickerContainer {
      public ItemPickerItem[] Items = new ItemPickerItem[0];
      public float rolls;
      public boolean noAutoAge;
      public int fillRand;
      public int maxMap;
      public int stashChance;
      public ItemPickerContainer junk;
      public ItemPickerContainer bags;
      public boolean procedural;
      public boolean dontSpawnAmmo = false;
      public boolean ignoreZombieDensity = false;
      public boolean cookFood = false;
      public boolean canBurn = false;
      public boolean isTrash = false;
      public boolean isWorn = false;
      public boolean isRotten = false;
      public boolean onlyOne = false;
      public boolean defaultInventoryLoot = true;
      public ArrayList<ProceduralItem> proceduralItems;

      public ItemPickerContainer() {
      }
   }

   public static final class ItemPickerRoom {
      public THashMap<String, ItemPickerContainer> Containers = new THashMap();
      public int fillRand;
      public boolean isShop;
      public String specificId = null;
      public int professionChance;
      public String outfit = null;
      public String outfitFemale = null;
      public String outfitMale = null;
      public String outfitChance = null;
      public String vehicle = null;
      public List<String> vehicles = null;
      public String vehicleChance = null;
      public String vehicleDistribution = null;
      public Integer vehicleSkin = null;
      public String femaleChance = null;
      public String roomTypes = null;
      public String zoneRequires = null;
      public String zoneDisallows = null;
      public String containerChance = null;
      public String femaleOdds = null;
      public String bagType = null;
      public String bagTable = null;
      public int professionChanceInt;

      public ItemPickerRoom() {
      }
   }

   public static final class VehicleDistribution {
      public ItemPickerRoom Normal;
      public final ArrayList<ItemPickerRoom> Specific = new ArrayList();

      public VehicleDistribution() {
      }
   }

   public static final class ItemPickerItem {
      public String itemName;
      public float chance;

      public ItemPickerItem() {
      }
   }

   public static final class ProceduralItem {
      public String name;
      public int min;
      public int max;
      public List<String> forceForItems;
      public List<String> forceForZones;
      public List<String> forceForTiles;
      public List<String> forceForRooms;
      public int weightChance;

      public ProceduralItem() {
      }
   }

   public static final class KeyNamer {
      public static ArrayList<String> badZones = new ArrayList();
      public static ArrayList<String> BigBuildingRooms = new ArrayList();
      public static ArrayList<String> RestaurantSubstrings = new ArrayList();
      public static ArrayList<String> Restaurants = new ArrayList();
      public static ArrayList<String> RoomSubstrings = new ArrayList();
      public static ArrayList<String> Rooms = new ArrayList();

      public KeyNamer() {
      }

      public static void clear() {
         badZones.clear();
         BigBuildingRooms.clear();
         RestaurantSubstrings.clear();
         Restaurants.clear();
         RoomSubstrings.clear();
         Rooms.clear();
      }

      public static void nameKey(InventoryItem var0, IsoGridSquare var1) {
         String var2 = getName(var1);
         if (var2 != null) {
            var2 = var2 + "Key";
            var2 = Translator.getText("IGUI_" + var2);
            String var10001 = Translator.getText(var0.getDisplayName());
            var0.setName(var10001 + " - " + var2);
         }

      }

      public static String getName(IsoGridSquare var0) {
         if (var0 != null && var0.getBuilding() != null) {
            IsoBuilding var1 = var0.getBuilding();
            String var2 = ItemPickerJava.getSquareZombiesType(var0);
            String var3 = null;
            if (ItemPickerJava.getSquareBuildingName(var0) != null) {
               var3 = ItemPickerJava.getSquareBuildingName(var0);
               return var3;
            } else if (var1.containsRoom("bedroom") && var1.containsRoom("livingroom") && var1.containsRoom("kitchen")) {
               return "Residential";
            } else {
               if (var2 != null) {
                  switch (var2) {
                     case "Prison":
                        var3 = "Prison";
                        break;
                     case "Police":
                        var3 = "Police";
                        break;
                     case "Army":
                        var3 = "Army";
                  }
               }

               if (badZones.contains(var3)) {
                  var3 = null;
               }

               if (var3 != null) {
                  return var3;
               } else {
                  Iterator var4 = BigBuildingRooms.iterator();

                  while(var4.hasNext()) {
                     String var8 = (String)var4.next();
                     if (var8.equals("storageunit") && var1.containsRoom("bedroom")) {
                        break;
                     }

                     if (var1.containsRoom(var8)) {
                        return var8;
                     }
                  }

                  if (ItemPickerJava.getSquareZombiesType(var0) != null) {
                     return ItemPickerJava.getSquareZombiesType(var0);
                  } else {
                     if (var0.getRoom() != null && var0.getRoom().getRoomDef() != null) {
                        String var7 = var0.getRoom().getRoomDef().getName();
                        if (Rooms.contains(var7)) {
                           return var7;
                        }

                        Iterator var9 = RoomSubstrings.iterator();

                        String var6;
                        while(var9.hasNext()) {
                           var6 = (String)var9.next();
                           if (var7.contains(var6)) {
                              return var6;
                           }
                        }

                        if (Restaurants.contains(var7)) {
                           return var7;
                        }

                        var9 = RestaurantSubstrings.iterator();

                        while(var9.hasNext()) {
                           var6 = (String)var9.next();
                           if (var7.contains(var6)) {
                              return var6;
                           }
                        }
                     }

                     if (ItemPickerJava.squareHasZone(var0, "TrailerPark") && var1.containsRoom("bedroom")) {
                        return "TrailerPark";
                     } else if (ItemPickerJava.squareHasZone(var0, "Ranch") && var1.containsRoom("bedroom")) {
                        return "Ranch";
                     } else if (ItemPickerJava.squareHasZone(var0, "Forest")) {
                        return "Forest";
                     } else {
                        return ItemPickerJava.squareHasZone(var0, "DeepForest") ? "DeepForest" : null;
                     }
                  }
               }
            }
         } else {
            return null;
         }
      }
   }
}
