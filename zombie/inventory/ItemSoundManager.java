package zombie.inventory;

import java.util.ArrayList;
import zombie.audio.BaseSoundEmitter;
import zombie.iso.IsoWorld;

public final class ItemSoundManager {
   private static final ArrayList<InventoryItem> items = new ArrayList();
   private static final ArrayList<BaseSoundEmitter> emitters = new ArrayList();
   private static final ArrayList<InventoryItem> toAdd = new ArrayList();
   private static final ArrayList<InventoryItem> toRemove = new ArrayList();
   private static final ArrayList<InventoryItem> toStopItems = new ArrayList();
   private static final ArrayList<BaseSoundEmitter> toStopEmitters = new ArrayList();

   public ItemSoundManager() {
   }

   public static void addItem(InventoryItem var0) {
      if (var0 != null && !items.contains(var0)) {
         toRemove.remove(var0);
         int var1 = toStopItems.indexOf(var0);
         if (var1 != -1) {
            toStopItems.remove(var1);
            BaseSoundEmitter var2 = (BaseSoundEmitter)toStopEmitters.remove(var1);
            items.add(var0);
            emitters.add(var2);
         } else if (!toAdd.contains(var0)) {
            toAdd.add(var0);
         }
      }
   }

   public static void removeItem(InventoryItem var0) {
      toAdd.remove(var0);
      int var1 = items.indexOf(var0);
      if (var0 != null && var1 != -1) {
         if (!toRemove.contains(var0)) {
            toRemove.add(var0);
         }
      }
   }

   public static void removeItems(ArrayList<InventoryItem> var0) {
      for(int var1 = 0; var1 < var0.size(); ++var1) {
         removeItem((InventoryItem)var0.get(var1));
      }

   }

   public static void update() {
      int var0;
      if (!toStopItems.isEmpty()) {
         for(var0 = 0; var0 < toStopItems.size(); ++var0) {
            BaseSoundEmitter var1 = (BaseSoundEmitter)toStopEmitters.get(var0);
            var1.stopAll();
            IsoWorld.instance.returnOwnershipOfEmitter(var1);
         }

         toStopItems.clear();
         toStopEmitters.clear();
      }

      BaseSoundEmitter var2;
      InventoryItem var4;
      if (!toAdd.isEmpty()) {
         for(var0 = 0; var0 < toAdd.size(); ++var0) {
            var4 = (InventoryItem)toAdd.get(var0);

            assert !items.contains(var4);

            items.add(var4);
            var2 = IsoWorld.instance.getFreeEmitter();
            IsoWorld.instance.takeOwnershipOfEmitter(var2);
            emitters.add(var2);
         }

         toAdd.clear();
      }

      if (!toRemove.isEmpty()) {
         for(var0 = 0; var0 < toRemove.size(); ++var0) {
            var4 = (InventoryItem)toRemove.get(var0);

            assert items.contains(var4);

            int var5 = items.indexOf(var4);
            items.remove(var5);
            BaseSoundEmitter var3 = (BaseSoundEmitter)emitters.get(var5);
            emitters.remove(var5);
            toStopItems.add(var4);
            toStopEmitters.add(var3);
         }

         toRemove.clear();
      }

      for(var0 = 0; var0 < items.size(); ++var0) {
         var4 = (InventoryItem)items.get(var0);
         var2 = (BaseSoundEmitter)emitters.get(var0);
         ItemContainer var6 = getExistingContainer(var4);
         if (var6 != null || var4.getWorldItem() != null && var4.getWorldItem().getWorldObjectIndex() != -1) {
            var4.updateSound(var2);
            var2.tick();
         } else {
            removeItem(var4);
         }
      }

   }

   private static ItemContainer getExistingContainer(InventoryItem var0) {
      ItemContainer var1 = var0.getOutermostContainer();
      if (var1 != null) {
         if (var1.containingItem != null && var1.containingItem.getWorldItem() != null) {
            if (var1.containingItem.getWorldItem().getWorldObjectIndex() == -1) {
               var1 = null;
            }
         } else if (var1.parent != null) {
            if (var1.parent.getObjectIndex() == -1 && var1.parent.getMovingObjectIndex() == -1 && var1.parent.getStaticMovingObjectIndex() == -1) {
               var1 = null;
            }
         } else {
            var1 = null;
         }
      }

      return var1;
   }

   public static void Reset() {
      items.clear();
      emitters.clear();
      toAdd.clear();
      toRemove.clear();
      toStopItems.clear();
      toStopEmitters.clear();
   }
}
