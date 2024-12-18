package zombie.inventory;

import zombie.core.Core;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.debug.DebugLog;
import zombie.entity.ComponentType;
import zombie.inventory.types.AlarmClockClothing;
import zombie.inventory.types.Clothing;
import zombie.inventory.types.Drainable;
import zombie.inventory.types.Food;
import zombie.inventory.types.InventoryContainer;
import zombie.inventory.types.Moveable;
import zombie.inventory.types.Radio;
import zombie.network.GameClient;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.Item;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.world.ItemInfo;
import zombie.world.WorldDictionary;

public final class InventoryItemFactory {
   public InventoryItemFactory() {
   }

   public static InventoryItem CreateItem(String var0) {
      return CreateItem(var0, 1.0F);
   }

   public static InventoryItem CreateItem(String var0, Food var1) {
      InventoryItem var2 = CreateItem(var0, 1.0F);
      Food var3 = (Food)Type.tryCastTo(var2, Food.class);
      if (var3 == null) {
         return null;
      } else {
         var3.setBaseHunger(var1.getBaseHunger());
         var3.setHungChange(var1.getHungChange());
         var3.setBoredomChange(var1.getBoredomChangeUnmodified());
         var3.setUnhappyChange(var1.getUnhappyChangeUnmodified());
         var3.setCarbohydrates(var1.getCarbohydrates());
         var3.setLipids(var1.getLipids());
         var3.setProteins(var1.getProteins());
         var3.setCalories(var1.getCalories());
         return var2;
      }
   }

   public static InventoryItem CreateItem(String var0, float var1) {
      return CreateItem(var0, var1, true);
   }

   public static Item getItem(String var0, boolean var1) {
      Item var2 = ScriptManager.instance.FindItem(var0, var1);
      if (var2 == null && var0.endsWith("Empty")) {
         var0 = var0.substring(0, var0.length() - 5);
         var2 = ScriptManager.instance.FindItem(var0, var1);
         if (var2 != null && !var2.containsComponent(ComponentType.FluidContainer)) {
            var2 = null;
         }
      }

      return var2;
   }

   public static InventoryItem CreateItem(String var0, float var1, boolean var2) {
      return createItemInternal(var0, var1, var2, true);
   }

   private static InventoryItem createItemInternal(String var0, float var1, boolean var2, boolean var3) {
      InventoryItem var4 = null;
      Item var5 = null;
      boolean var6 = false;
      String var7 = null;
      boolean var8 = false;

      try {
         if (var0.startsWith("Moveables.") && !var0.equalsIgnoreCase("Moveables.Moveable")) {
            String[] var9 = var0.split("\\.");
            var7 = var9[1];
            var6 = true;
            var0 = "Moveables.Moveable";
         }

         var5 = ScriptManager.instance.FindItem(var0, var2);
         if (var5 == null && var0.endsWith("Empty")) {
            var0 = var0.substring(0, var0.length() - 5);
            var5 = ScriptManager.instance.FindItem(var0, var2);
            if (var5 != null) {
               if (!var5.containsComponent(ComponentType.FluidContainer)) {
                  var5 = null;
               } else {
                  var8 = true;
               }
            }
         }
      } catch (Exception var10) {
         DebugLog.log("couldn't find item " + var0);
      }

      if (var5 == null) {
         DebugLog.log("Couldn't find item " + var0);
         return null;
      } else {
         var4 = var5.InstanceItem((String)null, var3);
         if (var8 && var4.hasComponent(ComponentType.FluidContainer)) {
            var4.getFluidContainer().Empty();
         }

         if (GameClient.bClient && (Core.getInstance().getPoisonousBerry() == null || Core.getInstance().getPoisonousBerry().isEmpty())) {
            Core.getInstance().setPoisonousBerry(GameClient.poisonousBerry);
         }

         if (GameClient.bClient && (Core.getInstance().getPoisonousMushroom() == null || Core.getInstance().getPoisonousMushroom().isEmpty())) {
            Core.getInstance().setPoisonousMushroom(GameClient.poisonousMushroom);
         }

         if (var0.equals(Core.getInstance().getPoisonousBerry())) {
            ((Food)var4).Poison = true;
            ((Food)var4).setPoisonLevelForRecipe(1);
            ((Food)var4).setPoisonDetectionLevel(1);
            ((Food)var4).setPoisonPower(5);
            ((Food)var4).setUseForPoison((int)(Math.abs(((Food)var4).getHungChange()) * 100.0F));
         }

         if (var0.equals(Core.getInstance().getPoisonousMushroom())) {
            ((Food)var4).Poison = true;
            ((Food)var4).setPoisonLevelForRecipe(2);
            ((Food)var4).setPoisonDetectionLevel(2);
            ((Food)var4).setPoisonPower(10);
            ((Food)var4).setUseForPoison((int)(Math.abs(((Food)var4).getHungChange()) * 100.0F));
         }

         var4.id = Rand.Next(2146250223) + 1233423;
         if (var4 instanceof Drainable) {
            var4.setCurrentUses((int)((float)var4.getMaxUses() * var1));
         }

         if (var6) {
            var4.type = var7;
            var4.fullType = var4.module + "." + var7;
            if (var4 instanceof Moveable && !((Moveable)var4).ReadFromWorldSprite(var7) && var4 instanceof Radio) {
               DebugLog.log("InventoryItemFactory -> Radio item = " + (var0 != null ? var0 : "unknown"));
            }
         }

         return var4;
      }
   }

   /** @deprecated */
   @Deprecated
   public static InventoryItem CreateItem(String var0, float var1, String var2) {
      InventoryItem var3 = null;
      Item var4 = ScriptManager.instance.getItem(var0);
      if (var4 == null) {
         DebugLog.log(var0 + " item not found.");
         return null;
      } else {
         var3 = var4.InstanceItem(var2, true);
         if (var3 == null) {
         }

         if (var3 instanceof Drainable) {
            var3.setCurrentUses((int)((float)var3.getMaxUses() * var1));
         }

         return var3;
      }
   }

   /** @deprecated */
   @Deprecated
   public static InventoryItem CreateItem(String var0, String var1, String var2, String var3) {
      InventoryItem var4 = new InventoryItem(var0, var1, var2, var3);
      var4.id = Rand.Next(2146250223) + 1233423;
      return var4;
   }

   public static InventoryItem CreateItem(short var0) {
      ItemInfo var1 = WorldDictionary.getItemInfoFromID(var0);
      if (var1 != null && var1.isValid()) {
         String var2 = var1.getFullType();
         if (var2 != null) {
            InventoryItem var3 = createItemInternal(var2, 1.0F, false, false);
            if (var3 != null) {
               return var3;
            }

            DebugLog.log("InventoryItemFactory.CreateItem() unknown item type \"" + (var2 != null ? var2 : "unknown") + "\", registry id = \"" + var0 + "\". Make sure all mods used in save are installed.");
         } else {
            DebugLog.log("InventoryItemFactory.CreateItem() unknown item (full type=null) with registry ID \"" + var0 + "\". Make sure all mods used in save are installed.");
         }
      } else if (var1 == null) {
         DebugLog.log("InventoryItemFactory.CreateItem() unknown item with registry ID \"" + var0 + "\". Make sure all mods used in save are installed.");
      } else {
         DebugLog.log("InventoryItemFactory.CreateItem() cannot create item: " + var1.ToString());
      }

      return null;
   }

   public static InventoryItem CreateItem(InventoryItem var0, String var1) {
      ItemVisual var2 = var0.getVisual();
      InventoryItem var3 = CreateItem(var1);
      ItemVisual var4 = var3.getVisual();
      var4.setTint(var2.getTint(var0.getClothingItem()));
      var4.setBaseTexture(var2.getBaseTexture());
      var4.setTextureChoice(var2.getTextureChoice());
      var4.setDecal(var2.getDecal(var0.getClothingItem()));
      if (var3 instanceof InventoryContainer && var0 instanceof InventoryContainer) {
         ((InventoryContainer)var3).getItemContainer().setItems(((InventoryContainer)var0).getItemContainer().getItems());
         if (!StringUtils.equals(var0.getName(), var0.getScriptItem().getDisplayName())) {
            var3.setName(var0.getName());
         }
      }

      var3.setColor(var0.getColor());
      var4.copyDirt(var2);
      var4.copyBlood(var2);
      var4.copyHoles(var2);
      var4.copyPatches(var2);
      if (var3 instanceof Clothing && var0 instanceof Clothing) {
         ((Clothing)var0).copyPatchesTo((Clothing)var3);
         ((Clothing)var3).setWetness(((Clothing)var0).getWetness());
      }

      if (var3 instanceof AlarmClockClothing && var0 instanceof AlarmClockClothing) {
         ((AlarmClockClothing)var3).setAlarmSet(((AlarmClockClothing)var0).isAlarmSet());
         ((AlarmClockClothing)var3).setHour(((AlarmClockClothing)var0).getHour());
         ((AlarmClockClothing)var3).setMinute(((AlarmClockClothing)var0).getMinute());
         ((AlarmClockClothing)var3).syncAlarmClock();
         ((AlarmClockClothing)var0).setAlarmSet(false);
         ((AlarmClockClothing)var0).syncAlarmClock();
      }

      var3.setConditionNoSound(var0.getCondition());
      var3.setFavorite(var0.isFavorite());
      if (var0.hasModData()) {
         var3.copyModData(var0.getModData());
      }

      var3.synchWithVisual();
      return var3;
   }
}
