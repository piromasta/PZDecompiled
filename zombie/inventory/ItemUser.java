package zombie.inventory;

import java.util.ArrayList;
import zombie.characters.IsoGameCharacter;
import zombie.debug.DebugLog;
import zombie.inventory.types.Clothing;
import zombie.inventory.types.DrainableComboItem;
import zombie.iso.IsoObject;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoMannequin;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.network.GameServer;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.Item;
import zombie.util.StringUtils;
import zombie.vehicles.VehiclePart;

public final class ItemUser {
   private static final ArrayList<InventoryItem> tempItems = new ArrayList();

   public ItemUser() {
   }

   public static void UseItem(InventoryItem var0) {
      UseItem(var0, false, false, 1, false, false);
   }

   public static int UseItem(InventoryItem var0, boolean var1, boolean var2, int var3, boolean var4, boolean var5) {
      if (!var0.isDisappearOnUse() && !var1 && !var5) {
         return 0;
      } else {
         int var6 = Math.min(var0.uses, var3);
         if (!var4) {
            var0.uses -= var6;
         }

         String var7;
         int var8;
         InventoryItem var9;
         if (var0.replaceOnUse != null && !var2 && !var1 && !var5) {
            var7 = var0.replaceOnUse;
            if (!var7.contains(".")) {
               var7 = var0.module + "." + var7;
            }

            CreateItem(var7, tempItems);

            for(var8 = 0; var8 < tempItems.size(); ++var8) {
               var9 = (InventoryItem)tempItems.get(var8);
               var9.setConditionFromModData(var0);
               AddItem(var0, var9);
               var9.setFavorite(var0.isFavorite());
            }
         }

         if (var0 instanceof DrainableComboItem && !StringUtils.isNullOrEmpty(((DrainableComboItem)var0).getReplaceOnDeplete()) && var0.uses <= 0) {
            var7 = ((DrainableComboItem)var0).getReplaceOnDeplete();
            if (!var7.contains(".")) {
               var7 = var0.module + "." + var7;
            }

            CreateItem(var7, tempItems);

            for(var8 = 0; var8 < tempItems.size(); ++var8) {
               var9 = (InventoryItem)tempItems.get(var8);
               var9.setConditionFromModData(var0);
               AddItem(var0, var9);
               var9.setFavorite(var0.isFavorite());
            }
         }

         if (var5) {
            RemoveItem(var0);
         } else if (var0.uses <= 0) {
            if (!var0.isKeepOnDeplete()) {
               RemoveItem(var0);
            }
         } else if (GameServer.bServer) {
            GameServer.sendItemStats(var0);
         }

         return var6;
      }
   }

   public static void CreateItem(String var0, ArrayList<InventoryItem> var1) {
      var1.clear();
      Item var2 = ScriptManager.instance.FindItem(var0);
      if (var2 == null) {
         DebugLog.General.warn("ERROR: ItemUses.CreateItem: can't find " + var0);
      } else {
         int var3 = var2.getCount();

         for(int var4 = 0; var4 < var3; ++var4) {
            InventoryItem var5 = InventoryItemFactory.CreateItem(var0);
            if (var5 == null) {
               return;
            }

            var1.add(var5);
         }

      }
   }

   public static void AddItem(InventoryItem var0, InventoryItem var1) {
      IsoWorldInventoryObject var2 = var0.getWorldItem();
      if (var2 != null && var2.getWorldObjectIndex() == -1) {
         var2 = null;
      }

      if (var2 != null) {
         var2.getSquare().AddWorldInventoryItem(var1, 0.0F, 0.0F, 0.0F, true);
      } else {
         if (var0.container != null) {
            VehiclePart var3 = var0.container.vehiclePart;
            if (GameServer.bServer) {
               GameServer.sendAddItemToContainer(var0.container, var1);
            }

            var0.container.AddItem(var1);
            if (var3 != null) {
               var3.setContainerContentAmount(var3.getItemContainer().getCapacityWeight());
            }
         }

      }
   }

   public static void RemoveItem(InventoryItem var0) {
      IsoWorldInventoryObject var1 = var0.getWorldItem();
      if (var1 != null && var1.getWorldObjectIndex() == -1) {
         var1 = null;
      }

      if (var1 != null) {
         var1.getSquare().transmitRemoveItemFromSquare(var1);
         if (var0.container != null) {
            var0.container.Items.remove(var0);
            var0.container.setDirty(true);
            var0.container.setDrawDirty(true);
            var0.container = null;
         }

      } else {
         if (var0.container != null) {
            IsoObject var2 = var0.container.parent;
            VehiclePart var3 = var0.container.vehiclePart;
            if (var2 instanceof IsoGameCharacter) {
               IsoGameCharacter var4 = (IsoGameCharacter)var2;
               if (var0 instanceof Clothing && var0.isWorn()) {
                  ((Clothing)var0).Unwear();
               }

               var4.removeFromHands(var0);
               if (var4.getClothingItem_Back() == var0) {
                  var4.setClothingItem_Back((InventoryItem)null);
               }
            }

            if (GameServer.bServer) {
               GameServer.sendRemoveItemFromContainer(var0.container, var0);
            }

            if (var0.container != null) {
               var0.container.Items.remove(var0);
               var0.container.setDirty(true);
               var0.container.setDrawDirty(true);
               var0.container = null;
            }

            if (var2 instanceof IsoDeadBody) {
               ((IsoDeadBody)var2).checkClothing(var0);
            }

            if (var2 instanceof IsoMannequin) {
               ((IsoMannequin)var2).checkClothing(var0);
            }

            if (var3 != null) {
               var3.setContainerContentAmount(var3.getItemContainer().getCapacityWeight());
            }
         }

      }
   }
}
