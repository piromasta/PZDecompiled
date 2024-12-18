package zombie.entity.components.crafting.recipe;

import java.util.ArrayList;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.inventory.InventoryItem;
import zombie.scripting.objects.Item;

public class ItemDataList {
   private ItemData[] dataElements;
   private int size = 0;
   private int peak = 0;
   private int unprocessed = 0;

   public ItemDataList(int var1) {
      this.dataElements = new ItemData[var1];

      for(int var2 = 0; var2 < var1; ++var2) {
         this.dataElements[var2] = new ItemData();
      }

   }

   private void checkCapacity(int var1) {
      if (var1 >= this.dataElements.length) {
         ItemData[] var2 = this.dataElements;
         this.dataElements = new ItemData[PZMath.max((int)((float)this.dataElements.length * 1.75F), var1 + 32)];
         System.arraycopy(var2, 0, this.dataElements, 0, var2.length);

         for(int var3 = var2.length; var3 < this.dataElements.length; ++var3) {
            this.dataElements[var3] = new ItemData();
         }
      }

   }

   public int size() {
      return this.size;
   }

   public Item getItem(int var1) {
      return this.get(var1).item;
   }

   public InventoryItem getInventoryItem(int var1) {
      return this.get(var1).inventoryItem;
   }

   public void setProcessed(int var1) {
      ItemData var2 = this.get(var1);
      if (!var2.processed) {
         var2.processed = true;
         --this.unprocessed;
      }

   }

   public boolean isProcessed(int var1) {
      return this.get(var1).processed;
   }

   public void getUnprocessed(ArrayList<InventoryItem> var1) {
      this.getUnprocessed(var1, false);
   }

   public void getUnprocessed(ArrayList<InventoryItem> var1, boolean var2) {
      for(int var3 = 0; var3 < this.size; ++var3) {
         ItemData var4 = this.dataElements[var3];
         if (!var4.processed && (!var4.existingItem || var2)) {
            if (var4.inventoryItem == null) {
               DebugLog.General.warn("Cannot collect unprocessed, inventory item is null!");
            } else {
               var1.add(var4.inventoryItem);
            }
         }
      }

   }

   public boolean hasUnprocessed() {
      return this.unprocessed > 0;
   }

   public void clear() {
      this.size = 0;
      this.unprocessed = 0;
   }

   public void reset() {
      for(int var1 = 0; var1 < this.peak; ++var1) {
         ItemData var2 = this.dataElements[var1];
         var2.item = null;
         var2.inventoryItem = null;
         var2.processed = false;
      }

      this.size = 0;
      this.peak = 0;
      this.unprocessed = 0;
   }

   private ItemData get(int var1) {
      if (var1 >= 0 && var1 < this.size) {
         return this.dataElements[var1];
      } else {
         throw new IndexOutOfBoundsException();
      }
   }

   private ItemData getNextElement() {
      this.checkCapacity(this.size + 1);
      ItemData var1 = this.dataElements[this.size++];
      if (this.size > this.peak) {
         this.peak = this.size;
      }

      var1.processed = false;
      ++this.unprocessed;
      return var1;
   }

   public void addItem(InventoryItem var1) {
      this.addItem(var1, false);
   }

   public void addItem(InventoryItem var1, boolean var2) {
      ItemData var3 = this.getNextElement();
      var3.inventoryItem = var1;
      var3.item = var1.getScriptItem();
      var3.existingItem = var2;
   }

   public void addItem(Item var1) {
      this.addItem(var1, false);
   }

   public void addItem(Item var1, boolean var2) {
      ItemData var3 = this.getNextElement();
      var3.inventoryItem = null;
      var3.item = var1;
      var3.existingItem = var2;
   }

   private static class ItemData {
      private Item item;
      private InventoryItem inventoryItem;
      private boolean processed = false;
      private boolean existingItem = false;

      private ItemData() {
      }
   }
}
