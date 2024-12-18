package zombie.entity.components.fluids;

import zombie.debug.objects.DebugClassFields;

@DebugClassFields
public class FluidProperties extends SealedFluidProperties {
   public FluidProperties() {
   }

   public SealedFluidProperties getSealedFluidProperties() {
      SealedFluidProperties var1 = new SealedFluidProperties();
      var1.setEffects(this.getFatigueChange(), this.getHungerChange(), this.getStressChange(), this.getThirstChange(), this.getUnhappyChange(), this.getAlcohol());
      var1.setNutrients(this.getCalories(), this.getCarbohydrates(), this.getLipids(), this.getProteins());
      var1.setReductions(this.getFluReduction(), this.getPainReduction(), this.getEnduranceChange(), this.getFoodSicknessReduction());
      var1.setAlcohol(this.getAlcohol());
      return var1;
   }

   public void setEffects(float var1, float var2, float var3, float var4, float var5, float var6) {
      super.setEffects(var1, var2, var3, var4, var5, var6);
   }

   public void setNutrients(float var1, float var2, float var3, float var4) {
      super.setNutrients(var1, var2, var3, var4);
   }

   public void setReductions(float var1, float var2, float var3, float var4) {
      super.setReductions(var1, var2, var3, var4);
   }

   public void setFatigueChange(float var1) {
      super.setFatigueChange(var1);
   }

   public void setHungerChange(float var1) {
      super.setHungerChange(var1);
   }

   public void setStressChange(float var1) {
      super.setStressChange(var1);
   }

   public void setThirstChange(float var1) {
      super.setThirstChange(var1);
   }

   public void setUnhappyChange(float var1) {
      super.setUnhappyChange(var1);
   }

   public void setCalories(float var1) {
      super.setCalories(var1);
   }

   public void setCarbohydrates(float var1) {
      super.setCarbohydrates(var1);
   }

   public void setLipids(float var1) {
      super.setLipids(var1);
   }

   public void setProteins(float var1) {
      super.setProteins(var1);
   }

   public void setAlcohol(float var1) {
      super.setAlcohol(var1);
   }

   public void setFluReduction(float var1) {
      super.setFluReduction(var1);
   }

   public void setPainReduction(float var1) {
      super.setPainReduction(var1);
   }

   public void setEnduranceChange(float var1) {
      super.setEnduranceChange(var1);
   }

   public void setFoodSicknessReduction(float var1) {
      super.setFoodSicknessReduction(var1);
   }
}
