package zombie.scripting.itemConfig;

import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.entity.GameEntity;

public class Randomizer {
   private final RandomGenerator[] generators;

   public Randomizer(RandomGenerator[] var1) throws ItemConfig.ItemConfigException {
      this.generators = var1;
      if (this.generators.length == 0) {
         throw new ItemConfig.ItemConfigException("Attempting to construct a Randomizer with no entries.");
      } else {
         PZMath.normalize((Object[])this.generators, RandomGenerator::getChance, RandomGenerator::setChance);
      }
   }

   public Randomizer(Randomizer var1) {
      this.generators = new RandomGenerator[var1.generators.length];

      for(int var2 = 0; var2 < this.generators.length; ++var2) {
         this.generators[var2] = var1.generators[var2].copy();
      }

   }

   public boolean execute(GameEntity var1) {
      RandomGenerator var2;
      if (this.generators.length > 1) {
         float var3 = Rand.Next(0.0F, 1.0F);
         float var4 = 1.0F;

         for(int var5 = this.generators.length - 1; var5 >= 1; --var5) {
            var2 = this.generators[var5];
            if (var3 > var4 - var2.getChance() && var3 <= var4) {
               return var2.execute(var1);
            }

            var4 -= var2.getChance();
         }
      }

      var2 = this.generators[0];
      return var2.execute(var1);
   }

   public Randomizer copy() {
      return new Randomizer(this);
   }
}
