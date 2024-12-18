package zombie.inventory;

import java.util.ArrayList;
import java.util.List;
import zombie.Lua.LuaEventManager;
import zombie.characters.IsoGameCharacter;
import zombie.inventory.types.InventoryContainer;
import zombie.iso.InstanceTracker;
import zombie.iso.IsoGridSquare;

public abstract class ItemSpawner {
   public ItemSpawner() {
   }

   private static void inc(InventoryItem var0, int var1) {
      if (var0 != null) {
         InstanceTracker.adj("Item Spawns", var0.getFullType(), var1);
      }
   }

   public static List<InventoryItem> spawnItems(InventoryItem var0, int var1, ItemContainer var2) {
      ArrayList var3 = var2.AddItems(var0, var1);
      inc((InventoryItem)var3.get(0), var3.size());
      return var3;
   }

   public static List<InventoryItem> spawnItems(String var0, int var1, ItemContainer var2) {
      ArrayList var3 = var2.AddItems(var0, var1);
      inc((InventoryItem)var3.get(0), var3.size());
      return var3;
   }

   public static InventoryItem spawnItem(InventoryItem var0, IsoGridSquare var1, float var2, float var3, float var4, boolean var5) {
      if (var0 == null) {
         return null;
      } else {
         var1.AddWorldInventoryItem(var0, var2, var3, var4);
         inc(var0, 1);
         if (var5 && var0 instanceof InventoryContainer && ItemPickerJava.containers.containsKey(var0.getType())) {
            ItemPickerJava.rollContainerItem((InventoryContainer)var0, (IsoGameCharacter)null, (ItemPickerJava.ItemPickerContainer)ItemPickerJava.getItemPickerContainers().get(var0.getType()));
            LuaEventManager.triggerEvent("OnFillContainer", "Container", var0.getType(), ((InventoryContainer)var0).getItemContainer());
         }

         return var0;
      }
   }

   public static InventoryItem spawnItem(InventoryItem var0, IsoGridSquare var1, float var2, float var3, float var4) {
      return spawnItem(var0, var1, var2, var3, var4, true);
   }

   public static InventoryItem spawnItem(InventoryItem var0, IsoGridSquare var1) {
      return spawnItem(var0, var1, 0.0F, 0.0F, 0.0F, true);
   }

   public static InventoryItem spawnItem(InventoryItem var0, IsoGridSquare var1, boolean var2) {
      return spawnItem(var0, var1, 0.0F, 0.0F, 0.0F, var2);
   }

   public static InventoryItem spawnItem(String var0, IsoGridSquare var1, float var2, float var3, float var4, boolean var5) {
      return spawnItem(InventoryItemFactory.CreateItem(var0), var1, var2, var3, var4, var5);
   }

   public static InventoryItem spawnItem(String var0, IsoGridSquare var1, float var2, float var3, float var4) {
      return spawnItem(InventoryItemFactory.CreateItem(var0), var1, var2, var3, var4, true);
   }

   public static InventoryItem spawnItem(InventoryItem var0, ItemContainer var1, boolean var2) {
      var1.AddItem(var0);
      inc(var0, 1);
      if (var2 && var0 instanceof InventoryContainer && ItemPickerJava.containers.containsKey(var0.getType())) {
         ItemPickerJava.rollContainerItem((InventoryContainer)var0, (IsoGameCharacter)null, (ItemPickerJava.ItemPickerContainer)ItemPickerJava.getItemPickerContainers().get(var0.getType()));
         LuaEventManager.triggerEvent("OnFillContainer", "Container", var0.getType(), ((InventoryContainer)var0).getItemContainer());
      }

      return var0;
   }

   public static InventoryItem spawnItem(InventoryItem var0, ItemContainer var1) {
      return spawnItem(var0, var1, true);
   }

   public static InventoryItem spawnItem(String var0, ItemContainer var1, boolean var2) {
      return spawnItem(InventoryItemFactory.CreateItem(var0), var1, var2);
   }

   public static InventoryItem spawnItem(String var0, ItemContainer var1) {
      return spawnItem(InventoryItemFactory.CreateItem(var0), var1, true);
   }
}
