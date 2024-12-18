package zombie.scripting.itemConfig.generators;

import zombie.entity.GameEntity;
import zombie.entity.components.attributes.AttributeType;
import zombie.entity.components.attributes.AttributeValueType;
import zombie.scripting.itemConfig.RandomGenerator;

public class GeneratorEnumAttribute extends RandomGenerator<GeneratorEnumAttribute> {
   private final AttributeType.Enum attributeType;
   private final String str;

   public GeneratorEnumAttribute(AttributeType var1, String var2) {
      this(var1, 1.0F, var2);
   }

   public GeneratorEnumAttribute(AttributeType var1, float var2, String var3) {
      if (var2 < 0.0F) {
         throw new IllegalArgumentException("Chance may not be <= 0.");
      } else if (!(var1 instanceof AttributeType.Enum)) {
         throw new IllegalArgumentException("AttributeType valueType should be Enum.");
      } else {
         this.attributeType = (AttributeType.Enum)var1;
         this.setChance(var2);
         this.str = var3;
      }
   }

   public boolean execute(GameEntity var1) {
      if (var1.getAttributes() != null && this.attributeType.getValueType() == AttributeValueType.Enum) {
         if (var1.getAttributes().contains(this.attributeType)) {
            var1.getAttributes().putFromScript(this.attributeType, this.str);
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public GeneratorEnumAttribute copy() {
      return new GeneratorEnumAttribute(this.attributeType, this.getChance(), this.str);
   }
}
