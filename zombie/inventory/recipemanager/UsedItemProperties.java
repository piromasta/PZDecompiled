package zombie.inventory.recipemanager;

import java.util.ArrayList;
import zombie.core.math.PZMath;
import zombie.entity.components.fluids.FluidConsume;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.Food;

public class UsedItemProperties {
   boolean tainted = false;
   boolean cooked = false;
   boolean burnt = false;
   int poisonLevel = -1;
   int poisonPower = 0;
   boolean rotten = false;
   boolean stale = false;
   float condition = 0.0F;
   float rottenness = 0.0F;
   int itemUsed = 0;
   int foodUsed = 0;

   public UsedItemProperties() {
   }

   protected void reset() {
      this.tainted = false;
      this.cooked = false;
      this.burnt = false;
      this.poisonLevel = -1;
      this.poisonPower = 0;
      this.rotten = false;
      this.stale = false;
      this.condition = 0.0F;
      this.rottenness = 0.0F;
      this.itemUsed = 0;
      this.foodUsed = 0;
   }

   protected void addFluidConsume(FluidConsume var1) {
   }

   protected void addInventoryItem(InventoryItem var1) {
      if (var1 instanceof Food) {
         if (((Food)var1).isTainted()) {
            this.tainted = true;
         }

         if (((Food)var1).isCooked()) {
            this.cooked = true;
         }

         if (((Food)var1).isBurnt()) {
            this.burnt = true;
         }

         if (((Food)var1).getPoisonDetectionLevel() >= 0) {
            if (this.poisonLevel == -1) {
               this.poisonLevel = ((Food)var1).getPoisonDetectionLevel();
            } else {
               this.poisonLevel = PZMath.min(this.poisonLevel, ((Food)var1).getPoisonDetectionLevel());
            }
         }

         this.poisonPower = PZMath.max(this.poisonPower, ((Food)var1).getPoisonPower());
         ++this.foodUsed;
         if (var1.getAge() > (float)var1.getOffAgeMax()) {
            this.rotten = true;
         } else if (!this.rotten && var1.getOffAgeMax() < 1000000000) {
            if (var1.getAge() < (float)var1.getOffAge()) {
               this.rottenness += 0.5F * var1.getAge() / (float)var1.getOffAge();
            } else {
               this.stale = true;
               this.rottenness += 0.5F + 0.5F * (var1.getAge() - (float)var1.getOffAge()) / (float)(var1.getOffAgeMax() - var1.getOffAge());
            }
         }
      }

      this.condition += (float)var1.getCondition() / (float)var1.getConditionMax();
      ++this.itemUsed;
   }

   protected void transferToResults(ArrayList<InventoryItem> var1, ArrayList<InventoryItem> var2) {
      this.rottenness /= (float)this.foodUsed;

      for(int var4 = 0; var4 < var1.size(); ++var4) {
         InventoryItem var3 = (InventoryItem)var1.get(var4);
         if (var3 instanceof Food var5) {
            if (var5.isCookable()) {
               var5.setCooked(this.cooked);
               var5.setBurnt(this.burnt);
               var5.setPoisonDetectionLevel(this.poisonLevel);
               var5.setPoisonPower(this.poisonPower);
               if (this.tainted) {
                  var5.setTainted(true);
               }
            }
         }

         if ((double)var3.getOffAgeMax() != 1.0E9) {
            if (this.rotten) {
               var3.setAge((float)var3.getOffAgeMax());
            } else {
               if (this.stale && this.rottenness < 0.5F) {
                  this.rottenness = 0.5F;
               }

               if (this.rottenness < 0.5F) {
                  var3.setAge(2.0F * this.rottenness * (float)var3.getOffAge());
               } else {
                  var3.setAge((float)var3.getOffAge() + 2.0F * (this.rottenness - 0.5F) * (float)(var3.getOffAgeMax() - var3.getOffAge()));
               }
            }
         }

         var3.setCondition(Math.round((float)var3.getConditionMax() * (this.condition / (float)this.itemUsed)));

         for(int var7 = 0; var7 < var2.size(); ++var7) {
            InventoryItem var6 = (InventoryItem)var2.get(var7);
            var3.setConditionFromModData(var6);
            if (var3.getScriptItem() == var6.getScriptItem() && var6.isFavorite()) {
               var3.setFavorite(true);
            }
         }
      }

   }
}
