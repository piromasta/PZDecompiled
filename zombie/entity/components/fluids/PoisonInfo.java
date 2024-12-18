package zombie.entity.components.fluids;

import zombie.core.math.PZMath;
import zombie.debug.objects.DebugClassFields;
import zombie.debug.objects.DebugNonRecursive;

@DebugClassFields
public class PoisonInfo {
   @DebugNonRecursive
   private final Fluid fluid;
   private final PoisonEffect maxEffect;
   private final float minAmount;
   private final float diluteRatio;

   protected PoisonInfo(Fluid var1, float var2, float var3, PoisonEffect var4) {
      this.fluid = var1;
      this.minAmount = var2;
      this.diluteRatio = PZMath.clamp(var3, 0.0F, 1.0F);
      this.maxEffect = var4;
   }

   public Fluid getFluid() {
      return this.fluid;
   }

   public PoisonEffect getPoisonEffect(float var1, float var2) {
      if (var1 > this.minAmount && (double)var2 == 1.0) {
         return this.maxEffect;
      } else {
         PoisonEffect var3 = PoisonEffect.None;
         if (this.minAmount > 0.0F) {
            float var4 = PZMath.clamp(var1 / this.minAmount, 0.0F, 1.0F);
            if (1.0F - this.diluteRatio > 0.0F) {
               var4 *= PZMath.clamp(var2 / (1.0F - this.diluteRatio), 0.0F, 1.0F);
            } else {
               var4 *= PZMath.clamp(var2, 0.0F, 1.0F);
            }

            int var5 = PZMath.roundToInt((float)this.maxEffect.getLevel() * var4);
            var5 = PZMath.clamp(var5, 0, this.maxEffect.getLevel());
            var3 = PoisonEffect.FromLevel(var5);
         }

         return var3;
      }
   }
}
