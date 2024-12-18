package zombie.scripting.itemConfig.generators;

import zombie.entity.GameEntity;
import zombie.entity.components.attributes.AttributeType;
import zombie.entity.components.attributes.AttributeValueType;
import zombie.scripting.itemConfig.RandomGenerator;

public class GeneratorStringAttribute extends RandomGenerator<GeneratorStringAttribute> {
   private final AttributeType.String attributeType;
   private final String str;

   public GeneratorStringAttribute(AttributeType var1, String var2) {
      this(var1, 1.0F, var2);
   }

   public GeneratorStringAttribute(AttributeType var1, float var2, String var3) {
      if (var2 < 0.0F) {
         throw new IllegalArgumentException("Chance may not be <= 0.");
      } else if (!(var1 instanceof AttributeType.String)) {
         throw new IllegalArgumentException("AttributeType valueType should be string.");
      } else {
         this.attributeType = (AttributeType.String)var1;
         this.setChance(var2);
         this.str = var3;
      }
   }

   public boolean execute(GameEntity var1) {
      if (var1.getAttributes() != null && this.attributeType.getValueType() == AttributeValueType.String) {
         if (var1.getAttributes().contains(this.attributeType)) {
            var1.getAttributes().set(this.attributeType, this.str);
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public GeneratorStringAttribute copy() {
      return new GeneratorStringAttribute(this.attributeType, this.getChance(), this.str);
   }
}
