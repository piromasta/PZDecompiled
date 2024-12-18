package zombie.entity.components.fluids;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.debug.objects.DebugClassFields;
import zombie.util.io.BitHeader;
import zombie.util.io.BitHeaderRead;
import zombie.util.io.BitHeaderWrite;

@DebugClassFields
public class SealedFluidProperties {
   public static final String Str_Fatigue = "Fatigue";
   public static final String Str_Hunger = "Hunger";
   public static final String Str_Stress = "Stress";
   public static final String Str_Thirst = "Thirst";
   public static final String Str_Unhappy = "Unhappy";
   public static final String Str_Calories = "Calories";
   public static final String Str_Carbohydrates = "Carbohydrates";
   public static final String Str_Lipids = "Lipids";
   public static final String Str_Proteins = "Proteins";
   public static final String Str_Alcohol = "Alcohol";
   public static final String Str_Flu = "FluReduction";
   public static final String Str_Pain = "PainReduction";
   public static final String Str_Endurance = "EnduranceChange";
   public static final String Str_FoodSickness = "FoodSicknessReduction";
   private float fatigueChange = 0.0F;
   private float hungerChange = 0.0F;
   private float stressChange = 0.0F;
   private float thirstChange = 0.0F;
   private float unhappyChange = 0.0F;
   private float calories = 0.0F;
   private float carbohydrates = 0.0F;
   private float lipids = 0.0F;
   private float proteins = 0.0F;
   private float alcohol = 0.0F;
   private float fluReduction = 0.0F;
   private float painReduction = 0.0F;
   private float enduranceChange = 0.0F;
   private float foodSicknessReduction = 0.0F;

   public SealedFluidProperties() {
   }

   public void save(ByteBuffer var1) throws IOException {
      BitHeaderWrite var2 = BitHeader.allocWrite(BitHeader.HeaderSize.Integer, var1);
      if (this.fatigueChange > 0.0F) {
         var2.addFlags(1);
         var1.putFloat(this.fatigueChange);
      }

      if (this.hungerChange > 0.0F) {
         var2.addFlags(2);
         var1.putFloat(this.hungerChange);
      }

      if (this.stressChange > 0.0F) {
         var2.addFlags(4);
         var1.putFloat(this.stressChange);
      }

      if (this.thirstChange > 0.0F) {
         var2.addFlags(8);
         var1.putFloat(this.thirstChange);
      }

      if (this.unhappyChange > 0.0F) {
         var2.addFlags(16);
         var1.putFloat(this.unhappyChange);
      }

      if (this.calories > 0.0F) {
         var2.addFlags(32);
         var1.putFloat(this.calories);
      }

      if (this.carbohydrates > 0.0F) {
         var2.addFlags(64);
         var1.putFloat(this.carbohydrates);
      }

      if (this.lipids > 0.0F) {
         var2.addFlags(128);
         var1.putFloat(this.lipids);
      }

      if (this.proteins > 0.0F) {
         var2.addFlags(256);
         var1.putFloat(this.proteins);
      }

      if (this.alcohol > 0.0F) {
         var2.addFlags(512);
         var1.putFloat(this.alcohol);
      }

      if (this.fluReduction > 0.0F) {
         var2.addFlags(1024);
         var1.putFloat(this.fluReduction);
      }

      if (this.painReduction > 0.0F) {
         var2.addFlags(2048);
         var1.putFloat(this.painReduction);
      }

      if (this.enduranceChange > 0.0F) {
         var2.addFlags(4096);
         var1.putFloat(this.enduranceChange);
      }

      if (this.foodSicknessReduction > 0.0F) {
         var2.addFlags(8192);
         var1.putFloat(this.foodSicknessReduction);
      }

      var2.write();
      var2.release();
   }

   public void load(ByteBuffer var1, int var2) throws IOException {
      this.clear();
      BitHeaderRead var3 = BitHeader.allocRead(BitHeader.HeaderSize.Integer, var1);
      if (var3.hasFlags(1)) {
         this.fatigueChange = var1.getFloat();
      }

      if (var3.hasFlags(2)) {
         this.hungerChange = var1.getFloat();
      }

      if (var3.hasFlags(4)) {
         this.stressChange = var1.getFloat();
      }

      if (var3.hasFlags(8)) {
         this.thirstChange = var1.getFloat();
      }

      if (var3.hasFlags(16)) {
         this.unhappyChange = var1.getFloat();
      }

      if (var3.hasFlags(32)) {
         this.calories = var1.getFloat();
      }

      if (var3.hasFlags(64)) {
         this.carbohydrates = var1.getFloat();
      }

      if (var3.hasFlags(128)) {
         this.lipids = var1.getFloat();
      }

      if (var3.hasFlags(256)) {
         this.proteins = var1.getFloat();
      }

      if (var3.hasFlags(512)) {
         this.alcohol = var1.getFloat();
      }

      if (var3.hasFlags(1024)) {
         this.fluReduction = var1.getFloat();
      }

      if (var3.hasFlags(2048)) {
         this.painReduction = var1.getFloat();
      }

      if (var3.hasFlags(4096)) {
         this.enduranceChange = var1.getFloat();
      }

      if (var3.hasFlags(8192)) {
         this.foodSicknessReduction = var1.getFloat();
      }

      var3.release();
   }

   public boolean hasProperties() {
      if (this.fatigueChange != 0.0F) {
         return true;
      } else if (this.hungerChange != 0.0F) {
         return true;
      } else if (this.stressChange != 0.0F) {
         return true;
      } else if (this.thirstChange != 0.0F) {
         return true;
      } else if (this.unhappyChange != 0.0F) {
         return true;
      } else if (this.calories != 0.0F) {
         return true;
      } else if (this.carbohydrates != 0.0F) {
         return true;
      } else if (this.lipids != 0.0F) {
         return true;
      } else if (this.proteins != 0.0F) {
         return true;
      } else if (this.alcohol != 0.0F) {
         return true;
      } else if (this.fluReduction != 0.0F) {
         return true;
      } else if (this.painReduction != 0.0F) {
         return true;
      } else if (this.enduranceChange != 0.0F) {
         return true;
      } else {
         return this.foodSicknessReduction != 0.0F;
      }
   }

   protected void clear() {
      this.fatigueChange = 0.0F;
      this.hungerChange = 0.0F;
      this.stressChange = 0.0F;
      this.thirstChange = 0.0F;
      this.unhappyChange = 0.0F;
      this.calories = 0.0F;
      this.carbohydrates = 0.0F;
      this.lipids = 0.0F;
      this.proteins = 0.0F;
      this.alcohol = 0.0F;
      this.fluReduction = 0.0F;
      this.painReduction = 0.0F;
      this.enduranceChange = 0.0F;
      this.foodSicknessReduction = 0.0F;
   }

   protected void addFromMultiplied(SealedFluidProperties var1, float var2) {
      this.fatigueChange += var1.fatigueChange * var2;
      this.hungerChange += var1.hungerChange * var2;
      this.stressChange += var1.stressChange * var2;
      this.thirstChange += var1.thirstChange * var2;
      this.unhappyChange += var1.unhappyChange * var2;
      this.calories += var1.calories * var2;
      this.carbohydrates += var1.carbohydrates * var2;
      this.lipids += var1.lipids * var2;
      this.proteins += var1.proteins * var2;
      this.alcohol += var1.alcohol * var2;
      this.fluReduction += var1.fluReduction * var2;
      this.painReduction += var1.painReduction * var2;
      this.enduranceChange += var1.enduranceChange * var2;
      this.foodSicknessReduction += var1.foodSicknessReduction * var2;
   }

   protected void setEffects(float var1, float var2, float var3, float var4, float var5, float var6) {
      this.setFatigueChange(var1);
      this.setHungerChange(var2);
      this.setStressChange(var3);
      this.setThirstChange(var4);
      this.setUnhappyChange(var5);
      this.setAlcohol(var6);
   }

   protected void setNutrients(float var1, float var2, float var3, float var4) {
      this.setCalories(var1);
      this.setCarbohydrates(var2);
      this.setLipids(var3);
      this.setProteins(var4);
   }

   protected void setReductions(float var1, float var2, float var3, float var4) {
      this.setFluReduction(var1);
      this.setPainReduction(var2);
      this.setEnduranceChange(var3);
      this.setFoodSicknessReduction(var4);
   }

   public float getFatigueChange() {
      return this.fatigueChange;
   }

   protected void setFatigueChange(float var1) {
      this.fatigueChange = var1;
   }

   public float getHungerChange() {
      return this.hungerChange;
   }

   protected void setHungerChange(float var1) {
      this.hungerChange = var1;
   }

   public float getStressChange() {
      return this.stressChange;
   }

   protected void setStressChange(float var1) {
      this.stressChange = var1;
   }

   public float getThirstChange() {
      return this.thirstChange;
   }

   protected void setThirstChange(float var1) {
      this.thirstChange = var1;
   }

   public float getUnhappyChange() {
      return this.unhappyChange;
   }

   protected void setUnhappyChange(float var1) {
      this.unhappyChange = var1;
   }

   public float getCalories() {
      return this.calories;
   }

   protected void setCalories(float var1) {
      this.calories = var1;
   }

   public float getCarbohydrates() {
      return this.carbohydrates;
   }

   protected void setCarbohydrates(float var1) {
      this.carbohydrates = var1;
   }

   public float getLipids() {
      return this.lipids;
   }

   protected void setLipids(float var1) {
      this.lipids = var1;
   }

   public float getProteins() {
      return this.proteins;
   }

   protected void setProteins(float var1) {
      this.proteins = var1;
   }

   public float getAlcohol() {
      return this.alcohol;
   }

   protected void setAlcohol(float var1) {
      this.alcohol = var1;
   }

   public float getFluReduction() {
      return this.fluReduction;
   }

   protected void setFluReduction(float var1) {
      this.fluReduction = var1;
   }

   public float getPainReduction() {
      return this.painReduction;
   }

   protected void setPainReduction(float var1) {
      this.painReduction = var1;
   }

   public float getEnduranceChange() {
      return this.enduranceChange;
   }

   protected void setEnduranceChange(float var1) {
      this.enduranceChange = var1;
   }

   public float getFoodSicknessReduction() {
      return this.foodSicknessReduction;
   }

   protected void setFoodSicknessReduction(float var1) {
      this.foodSicknessReduction = var1;
   }
}
