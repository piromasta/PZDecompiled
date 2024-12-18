package zombie.scripting.itemConfig.generators;

import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.entity.GameEntity;
import zombie.entity.components.fluids.Fluid;
import zombie.entity.components.fluids.FluidContainer;
import zombie.scripting.itemConfig.RandomGenerator;
import zombie.util.StringUtils;

public class GeneratorFluidContainer extends RandomGenerator<GeneratorFluidContainer> {
   private final String containerID;
   private final Fluid[] fluids;
   private final float[] ratios;
   private final float min;
   private final float max;

   public GeneratorFluidContainer(String var1, Fluid[] var2, float[] var3, float var4) {
      this(var1, var2, var3, 1.0F, 0.0F, var4);
   }

   public GeneratorFluidContainer(String var1, Fluid[] var2, float[] var3, float var4, float var5) {
      this(var1, var2, var3, 1.0F, var4, var5);
   }

   public GeneratorFluidContainer(String var1, Fluid[] var2, float[] var3, float var4, float var5, float var6) {
      if (var5 > var6) {
         var6 = var5;
         var5 = var5;
      }

      if (var4 <= 0.0F) {
         throw new IllegalArgumentException("Chance may not be <= 0.");
      } else if (StringUtils.isNullOrWhitespace(var1)) {
         throw new IllegalArgumentException("ContainerID cannot be null or whitespace.");
      } else if (var2 != null && var3 == null) {
         throw new IllegalArgumentException("Ratios can not be null if fluids are added.");
      } else if (var2 != null && var2.length != var3.length) {
         throw new IllegalArgumentException("Fluids and ratios size must be equal.");
      } else {
         if (var2 != null) {
            for(int var7 = 0; var7 < var2.length; ++var7) {
               if (var2[var7] == null) {
                  throw new IllegalArgumentException("Fluid can not be null.");
               }

               if (var3[var7] <= 0.0F) {
                  throw new IllegalArgumentException("Ratio can not be <= 0.");
               }
            }

            this.fluids = var2;
            this.ratios = PZMath.normalize(var3);
         } else {
            this.fluids = null;
            this.ratios = null;
         }

         this.containerID = var1;
         this.setChance(var4);
         this.min = var5;
         this.max = var6;
      }
   }

   public boolean execute(GameEntity var1) {
      FluidContainer var2 = null;
      if (var1.getFluidContainer() != null && var1.getFluidContainer().getContainerName().equalsIgnoreCase(this.containerID)) {
         var2 = var1.getFluidContainer();
      }

      if (var2 != null) {
         float var3 = 1.0F;
         if (this.min == this.max) {
            var3 = this.min;
         } else {
            var3 = Rand.Next(this.min, this.max);
         }

         float var4 = var2.getCapacity() * PZMath.clamp_01(var3);
         if (this.fluids != null) {
            var2.Empty();

            for(int var5 = 0; var5 < this.fluids.length; ++var5) {
               var2.addFluid(this.fluids[var5], var4 * this.ratios[var5]);
            }
         } else {
            if (var2.isEmpty()) {
               return true;
            }

            var2.adjustAmount(var4);
         }

         return true;
      } else {
         return false;
      }
   }

   public GeneratorFluidContainer copy() {
      return new GeneratorFluidContainer(this.containerID, this.fluids, this.ratios, this.getChance(), this.min, this.max);
   }
}
