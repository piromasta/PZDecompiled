package zombie.inventory.recipemanager;

import java.util.ArrayDeque;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.entity.components.fluids.FluidConsume;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemUser;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.Food;
import zombie.network.GameClient;

public class ItemRecord {
   private static final ArrayDeque<ItemRecord> pool = new ArrayDeque();
   private ItemRecipe itemRecipe;
   private InventoryItem item;
   private SourceRecord sourceRecord;
   private SourceType sourceType;

   protected static ItemRecord alloc(ItemRecipe var0, InventoryItem var1) {
      return pool.isEmpty() ? (new ItemRecord()).init(var0, var1) : ((ItemRecord)pool.pop()).init(var0, var1);
   }

   protected static void release(ItemRecord var0) {
      assert !pool.contains(var0);

      pool.push(var0.reset());
   }

   private ItemRecord() {
   }

   private ItemRecord init(ItemRecipe var1, InventoryItem var2) {
      this.itemRecipe = var1;
      this.item = var2;
      return this;
   }

   private ItemRecord reset() {
      this.itemRecipe = null;
      this.item = null;
      this.sourceType = null;
      this.sourceRecord = null;
      return this;
   }

   public String toString() {
      return "item:" + this.item + ", sourceType=" + this.sourceType;
   }

   protected SourceType getSourceType() {
      return this.sourceType;
   }

   protected InventoryItem getItem() {
      return this.item;
   }

   protected void setSource(SourceRecord var1, SourceType var2) {
      this.sourceRecord = var1;
      this.sourceType = var2;
   }

   protected int getPriority() {
      if (this.itemRecipe.getSelectedItem() != null && this.itemRecipe.getSelectedItem() == this.item) {
         return 0;
      } else if (this.item.isEquipped()) {
         return 1;
      } else {
         return this.item.isInPlayerInventory() ? 2 : 3;
      }
   }

   protected float getUses() {
      if (this.sourceRecord.isKeep()) {
         return 1.0E8F;
      } else if (this.sourceType.isUsesFluid()) {
         return this.item.getFluidContainer() != null ? this.item.getFluidContainer().getAmount() : 0.0F;
      } else if (this.sourceRecord.isUseIsItemCount()) {
         return 1.0F;
      } else if (this.item instanceof DrainableComboItem) {
         return (float)this.item.getCurrentUses();
      } else if (this.item instanceof Food) {
         Food var1 = (Food)this.item;
         return (float)((int)(-var1.getHungerChange() * 100.0F));
      } else {
         return 1.0F;
      }
   }

   protected float applyUses(float var1, UsedItemProperties var2) {
      if (var1 <= 0.0F) {
         if (RecipeMonitor.canLog()) {
            RecipeMonitor.Log("uses required should not be <= 0", RecipeMonitor.colNeg);
         }

         DebugLog.General.error("uses required should not be <= 0");
         return 0.0F;
      } else {
         if (this.sourceRecord.isKeep()) {
            var2.addInventoryItem(this.item);
            --var1;
            if (RecipeMonitor.canLog()) {
               RecipeMonitor.Log("> source = keep, use = -1", RecipeMonitor.colGray);
            }
         } else {
            float var3;
            float var4;
            if (this.sourceType.isUsesFluid()) {
               var3 = this.getUses();
               var4 = PZMath.min(var3, var1);
               FluidConsume var5 = this.item.getFluidContainer().removeFluid(var4, true);
               var2.addFluidConsume(var5);
               var5.release();
               var1 -= var4;
               if (RecipeMonitor.canLog()) {
                  RecipeMonitor.Log("> source = fluid, use = -" + var4, RecipeMonitor.colGray);
               }
            } else if (this.sourceRecord.isDestroy()) {
               var2.addInventoryItem(this.item);
               ItemUser.RemoveItem(this.item);
               --var1;
            } else if (!(this.item instanceof DrainableComboItem) && !(this.item instanceof Food)) {
               var2.addInventoryItem(this.item);
               var1 -= (float)ItemUser.UseItem(this.item, true, false, (int)var1, false, false);
               if (RecipeMonitor.canLog()) {
                  RecipeMonitor.Log("> source = generic, use = -1", RecipeMonitor.colGray);
               }
            } else {
               var2.addInventoryItem(this.item);
               var3 = this.getUses();
               var4 = PZMath.min(var3, var1);
               this.applyUsesToItem(var4);
               var1 -= var4;
               if (RecipeMonitor.canLog()) {
                  RecipeMonitor.Log("> source = drainable, use = -" + var4, RecipeMonitor.colGray);
               }
            }
         }

         return var1;
      }
   }

   private void applyUsesToItem(float var1) {
      if (this.item instanceof DrainableComboItem) {
         DrainableComboItem var2 = (DrainableComboItem)this.item;
         if (RecipeMonitor.canLog()) {
            RecipeMonitor.Log("> applyUsesToItem: drainable, uses = " + var2.getCurrentUses(), RecipeMonitor.colGray);
         }

         var2.setCurrentUses(var2.getCurrentUses() - (int)var1);
         if (this.getUses() < 1.0F) {
            if (RecipeMonitor.canLog()) {
               RecipeMonitor.Log("> applyUsesToItem: drainable, new uses = " + var2.getCurrentUses() + " -> ItemUser.UseItem", RecipeMonitor.colGray);
            }

            ItemUser.UseItem(var2);
            return;
         }

         if (RecipeMonitor.canLog()) {
            RecipeMonitor.Log("> applyUsesToItem: drainable, new uses = " + var2.getCurrentUses(), RecipeMonitor.colGray);
         }

         if (GameClient.bClient && !this.item.isInPlayerInventory()) {
            GameClient.instance.sendItemStats(this.item);
         }
      }

      if (this.item instanceof Food) {
         Food var5 = (Food)this.item;
         if (var5.getHungerChange() < 0.0F) {
            if (RecipeMonitor.canLog()) {
               RecipeMonitor.Log("> applyUsesToItem: food, hungerChange = " + var5.getHungerChange(), RecipeMonitor.colGray);
            }

            float var3 = Math.min(-var5.getHungerChange() * 100.0F, var1);
            if (RecipeMonitor.canLog()) {
               RecipeMonitor.Log("> applyUsesToItem: food, use = " + var3, RecipeMonitor.colGray);
            }

            float var4 = var3 / (-var5.getHungerChange() * 100.0F);
            if (var4 < 0.0F) {
               var4 = 0.0F;
            }

            if (var4 > 1.0F) {
               var4 = 1.0F;
            }

            if (RecipeMonitor.canLog()) {
               RecipeMonitor.Log("> applyUsesToItem: food, percentage = " + var4, RecipeMonitor.colGray);
            }

            var5.setHungChange(var5.getHungChange() - var5.getHungChange() * var4);
            var5.setCalories(var5.getCalories() - var5.getCalories() * var4);
            var5.setCarbohydrates(var5.getCarbohydrates() - var5.getCarbohydrates() * var4);
            var5.setLipids(var5.getLipids() - var5.getLipids() * var4);
            var5.setProteins(var5.getProteins() - var5.getProteins() * var4);
            var5.setThirstChange(var5.getThirstChangeUnmodified() - var5.getThirstChangeUnmodified() * var4);
            var5.setFluReduction(var5.getFluReduction() - (int)((float)var5.getFluReduction() * var4));
            var5.setPainReduction(var5.getPainReduction() - var5.getPainReduction() * var4);
            var5.setEndChange(var5.getEnduranceChangeUnmodified() - var5.getEnduranceChangeUnmodified() * var4);
            var5.setReduceFoodSickness(var5.getReduceFoodSickness() - (int)((float)var5.getReduceFoodSickness() * var4));
            var5.setStressChange(var5.getStressChangeUnmodified() - var5.getStressChangeUnmodified() * var4);
            var5.setFatigueChange(var5.getFatigueChange() - var5.getFatigueChange() * var4);
            if ((double)var5.getHungerChange() > -0.01) {
               if (RecipeMonitor.canLog()) {
                  RecipeMonitor.Log("> applyUsesToItem: food, new hungerChange = " + var5.getHungerChange() + " -> ItemUser.UserItem", RecipeMonitor.colGray);
               }

               ItemUser.UseItem(var5);
               return;
            }

            if (RecipeMonitor.canLog()) {
               RecipeMonitor.Log("> applyUsesToItem: food, new hungerChange = " + var5.getHungerChange(), RecipeMonitor.colGray);
            }

            if (GameClient.bClient && !this.item.isInPlayerInventory()) {
               GameClient.instance.sendItemStats(this.item);
            }
         } else if (RecipeMonitor.canLog()) {
            RecipeMonitor.Log("> [WARN] applyUsesToItem: food, hungerChange = " + var5.getHungerChange(), RecipeMonitor.colNeg);
         }
      }

   }
}
