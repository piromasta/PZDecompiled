package zombie.scripting.itemConfig;

import zombie.entity.GameEntity;

public abstract class RandomGenerator<T extends RandomGenerator<T>> {
   private float chance = 1.0F;

   public RandomGenerator() {
   }

   protected void setChance(float var1) {
      this.chance = var1;
   }

   protected float getChance() {
      return this.chance;
   }

   public abstract boolean execute(GameEntity var1);

   public abstract T copy();
}
