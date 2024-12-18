package zombie.inventory.recipemanager;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import zombie.debug.DebugLog;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.Food;
import zombie.scripting.objects.Recipe;
import zombie.util.Type;

public class SourceRecord {
   private static final ArrayDeque<SourceRecord> pool = new ArrayDeque();
   private ItemRecipe itemRecipe;
   private int sourceIndex;
   private Recipe.Source recipeSource;
   private final ArrayList<SourceType> sourceTypes = new ArrayList();
   public final ArrayList<ItemRecord> itemRecords = new ArrayList();
   private final ArrayList<ItemRecord> collectedItemRecords = new ArrayList();
   private final ArrayList<InventoryItem> collectedItems = new ArrayList();

   protected static SourceRecord alloc(ItemRecipe var0, int var1, Recipe.Source var2) {
      return pool.isEmpty() ? (new SourceRecord()).init(var0, var1, var2) : ((SourceRecord)pool.pop()).init(var0, var1, var2);
   }

   protected static void release(SourceRecord var0) {
      assert !pool.contains(var0);

      pool.push(var0.reset());
   }

   private SourceRecord() {
   }

   private SourceRecord init(ItemRecipe var1, int var2, Recipe.Source var3) {
      this.itemRecipe = var1;
      this.sourceIndex = var2;
      this.recipeSource = var3;

      for(int var4 = 0; var4 < var3.getItems().size(); ++var4) {
         String var5 = (String)var3.getItems().get(var4);
         SourceType var6 = SourceType.alloc(var5);
         this.sourceTypes.add(var6);
      }

      return this;
   }

   private SourceRecord reset() {
      this.sourceIndex = -1;
      this.recipeSource = null;

      for(int var1 = 0; var1 < this.itemRecords.size(); ++var1) {
         ItemRecord.release((ItemRecord)this.itemRecords.get(var1));
      }

      this.itemRecords.clear();
      this.sourceTypes.clear();
      this.collectedItemRecords.clear();
      this.collectedItems.clear();
      return this;
   }

   protected void applyUses(UsedItemProperties var1) {
      float var2 = this.getUsesRequired();
      RecipeMonitor.IncTab();
      if (RecipeMonitor.canLog()) {
         RecipeMonitor.Log("[" + this.sourceIndex + "] SourceRecord uses required: " + var2);
      }

      RecipeMonitor.IncTab();

      for(int var3 = 0; var3 < this.collectedItemRecords.size(); ++var3) {
         RecipeMonitor.IncTab();
         var2 = ((ItemRecord)this.collectedItemRecords.get(var3)).applyUses(var2, var1);
         RecipeMonitor.DecTab();
         if (RecipeMonitor.canLog()) {
            RecipeMonitor.Log("uses remaining: " + var2 + ", applied: " + this.collectedItemRecords.get(var3));
         }
      }

      RecipeMonitor.DecTab();
      if (var2 > 0.0F) {
         DebugLog.General.error("Uses required is '" + var2 + "', should be zero. Recipe = " + this.itemRecipe.getRecipeName());
         if (RecipeMonitor.canLog()) {
            RecipeMonitor.Log("Uses required is '" + var2 + "', should be zero. Recipe = " + this.itemRecipe.getRecipeName(), RecipeMonitor.colNeg);
         }
      }

      RecipeMonitor.DecTab();
   }

   protected ArrayList<InventoryItem> getCollectedItems() {
      return this.collectedItems;
   }

   protected boolean isDestroy() {
      return this.recipeSource.isDestroy();
   }

   protected boolean isKeep() {
      return this.recipeSource.isKeep();
   }

   protected boolean isUseIsItemCount() {
      return this.recipeSource.use <= 0.0F;
   }

   protected int getNumberOfTimesSourceCanBeDone() {
      float var1 = this.getUsesRequired();
      if (var1 <= 0.0F) {
         DebugLog.General.error("Uses required is zero?");
         return 0;
      } else {
         float var2 = this.getUsesTotalSourceItems();
         return (int)(var2 / var1);
      }
   }

   protected float getUsesTotalSourceItems() {
      float var1 = 0.0F;

      for(int var2 = 0; var2 < this.itemRecords.size(); ++var2) {
         var1 += ((ItemRecord)this.itemRecords.get(var2)).getUses();
      }

      return var1;
   }

   public float getUsesRequired() {
      float var1 = this.recipeSource.getCount();
      if (this.recipeSource.use > 0.0F) {
         var1 = this.recipeSource.getUse();
      }

      return var1;
   }

   protected boolean isValid() {
      return this.getUsesTotalSourceItems() >= this.getUsesRequired();
   }

   protected void clearCollectedItems() {
      this.collectedItemRecords.clear();
      this.collectedItems.clear();
   }

   protected void collectItems() {
      this.itemRecords.sort(Comparator.comparing(ItemRecord::getPriority));
      float var1 = this.getUsesRequired();
      RecipeMonitor.IncTab();
      if (RecipeMonitor.canLog()) {
         RecipeMonitor.Log("[" + this.sourceIndex + "] SourceRecord uses required: " + var1);
      }

      RecipeMonitor.IncTab();
      ItemRecord var2 = null;

      for(int var3 = 0; var3 < this.itemRecords.size(); ++var3) {
         var2 = (ItemRecord)this.itemRecords.get(var3);
         this.collectedItemRecords.add(var2);
         this.collectedItems.add(var2.getItem());
         var1 -= var2.getUses();
         if (RecipeMonitor.canLog()) {
            RecipeMonitor.Log("uses remaining: " + var1 + ", added: " + var2);
         }

         if (var1 <= 0.0F) {
            break;
         }
      }

      if (var1 > 0.0F) {
         if (RecipeMonitor.canLog()) {
            RecipeMonitor.Log("SourceRecord not satisfied, uses remaining: " + var1, RecipeMonitor.colNeg);
         }

         this.collectedItemRecords.clear();
         this.collectedItems.clear();
      }

      RecipeMonitor.DecTab();
      RecipeMonitor.DecTab();
   }

   protected boolean assignItemRecord(ItemRecord var1) {
      for(int var2 = 0; var2 < this.sourceTypes.size(); ++var2) {
         if (this.isValidSourceItem((SourceType)this.sourceTypes.get(var2), var1)) {
            var1.setSource(this, (SourceType)this.sourceTypes.get(var2));
            this.itemRecords.add(var1);
            return true;
         }
      }

      return false;
   }

   private boolean isValidSourceItem(SourceType var1, ItemRecord var2) {
      InventoryItem var3 = var2.getItem();
      boolean var4 = var3 instanceof DrainableComboItem;
      Food var5 = (Food)Type.tryCastTo(var3, Food.class);
      if (var1.isUsesFluid()) {
         if (var3.getFluidContainer() != null && var1.getSourceFluid() != null && !var3.getFluidContainer().isEmpty() && var3.getFluidContainer().isPerceivedFluidToPlayer(var1.getSourceFluid(), this.itemRecipe.getCharacter())) {
            if (RecipeMonitor.canLog()) {
               RecipeMonitor.Log("valid: fluid container, " + var1.getSourceFluid(), RecipeMonitor.colPos);
            }

            return true;
         } else {
            if (RecipeMonitor.canLog()) {
               RecipeMonitor.Log("failed: invalid or no fluid container, " + var1.getSourceFluid() + ", " + var1, RecipeMonitor.colNeg);
            }

            return false;
         }
      } else if ("Water".equals(var1.getItemType()) && var3 instanceof DrainableComboItem && var3.isWaterSource()) {
         return true;
      } else if (!var1.getItemType().equals(var3.getFullType())) {
         if (RecipeMonitor.canLog()) {
            RecipeMonitor.Log("failed: not equals [" + var1.getItemType() + "], " + var1, RecipeMonitor.colNeg);
         }

         return false;
      } else if (!this.itemRecipe.testItem(var3)) {
         if (RecipeMonitor.canLog()) {
            RecipeMonitor.Log("failed: testItem(), " + var1, RecipeMonitor.colNeg);
         }

         return false;
      } else if (this.itemRecipe.getRecipe().getHeat() > 0.0F && var4 && var3.isCookable() && var3.getInvHeat() + 1.0F < this.itemRecipe.getRecipe().getHeat()) {
         if (RecipeMonitor.canLog()) {
            RecipeMonitor.Log("failed: getHeat > 0, " + var1, RecipeMonitor.colNeg);
         }

         return false;
      } else if (this.itemRecipe.getRecipe().getHeat() < 0.0F && var4 && var3.isCookable() && var3.getInvHeat() > this.itemRecipe.getRecipe().getHeat()) {
         if (RecipeMonitor.canLog()) {
            RecipeMonitor.Log("failed: getHeat < 0, " + var1, RecipeMonitor.colNeg);
         }

         return false;
      } else if ("Clothing".equals(var3.getCategory()) && var3.isFavorite()) {
         if (RecipeMonitor.canLog()) {
            RecipeMonitor.Log("failed: is favorite clothing, " + var1, RecipeMonitor.colNeg);
         }

         return false;
      } else if (this.isDestroy()) {
         if (RecipeMonitor.canLog()) {
            RecipeMonitor.Log("valid: is destroy", RecipeMonitor.colPos);
         }

         return true;
      } else if (var4) {
         if (RecipeMonitor.canLog()) {
            RecipeMonitor.Log("valid: isDrainable", RecipeMonitor.colPos);
         }

         return true;
      } else if (this.recipeSource.use > 0.0F) {
         if (var5 != null) {
            if (RecipeMonitor.canLog()) {
               RecipeMonitor.Log("valid: use > 0 [food]", RecipeMonitor.colPos);
            }

            return true;
         } else {
            if (RecipeMonitor.canLog()) {
               RecipeMonitor.Log("invalid, " + var1, RecipeMonitor.colNeg);
            }

            return false;
         }
      } else {
         if (RecipeMonitor.canLog()) {
            RecipeMonitor.Log("valid: generic", RecipeMonitor.colPos);
         }

         return true;
      }
   }
}
