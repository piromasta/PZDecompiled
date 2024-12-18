package zombie.scripting.itemConfig.generators;

import zombie.core.random.Rand;
import zombie.entity.GameEntity;
import zombie.entity.components.attributes.AttributeType;
import zombie.entity.components.attributes.AttributeValueType;
import zombie.scripting.itemConfig.RandomGenerator;

public class GeneratorNumericAttribute extends RandomGenerator<GeneratorNumericAttribute> {
   private final AttributeType.Numeric<?, ?> attributeType;
   private final float min;
   private final float max;

   public GeneratorNumericAttribute(AttributeType var1, float var2) {
      this(var1, 1.0F, 0.0F, var2);
   }

   public GeneratorNumericAttribute(AttributeType var1, float var2, float var3) {
      this(var1, 1.0F, var2, var3);
   }

   public GeneratorNumericAttribute(AttributeType var1, float var2, float var3, float var4) {
      if (var3 > var4) {
         var4 = var3;
         var3 = var3;
      }

      if (var2 < 0.0F) {
         throw new IllegalArgumentException("Chance may not be <= 0.");
      } else if (!(var1 instanceof AttributeType.Numeric)) {
         throw new IllegalArgumentException("AttributeType valueType should be numeric.");
      } else {
         this.attributeType = (AttributeType.Numeric)var1;
         this.setChance(var2);
         this.min = var3;
         this.max = var4;
      }
   }

   public boolean execute(GameEntity var1) {
      if (var1.getAttributes() != null && AttributeValueType.IsNumeric(this.attributeType.getValueType())) {
         if (var1.getAttributes().contains(this.attributeType)) {
            if (this.min == this.max) {
               var1.getAttributes().setFloatValue(this.attributeType, this.min);
            } else {
               var1.getAttributes().setFloatValue(this.attributeType, Rand.Next(this.min, this.max));
            }

            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public GeneratorNumericAttribute copy() {
      return new GeneratorNumericAttribute(this.attributeType, this.getChance(), this.min, this.max);
   }
}
