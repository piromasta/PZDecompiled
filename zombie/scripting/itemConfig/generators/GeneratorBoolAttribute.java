package zombie.scripting.itemConfig.generators;

import zombie.entity.GameEntity;
import zombie.entity.components.attributes.AttributeType;
import zombie.entity.components.attributes.AttributeValueType;
import zombie.scripting.itemConfig.RandomGenerator;

public class GeneratorBoolAttribute extends RandomGenerator<GeneratorBoolAttribute> {
   private final AttributeType.Bool attributeType;
   private final boolean value;

   public GeneratorBoolAttribute(AttributeType var1, boolean var2) {
      this(var1, 1.0F, var2);
   }

   public GeneratorBoolAttribute(AttributeType var1, float var2, boolean var3) {
      if (var2 < 0.0F) {
         throw new IllegalArgumentException("Chance may not be <= 0.");
      } else if (!(var1 instanceof AttributeType.Bool)) {
         throw new IllegalArgumentException("AttributeType valueType should be boolean.");
      } else {
         this.attributeType = (AttributeType.Bool)var1;
         this.setChance(var2);
         this.value = var3;
      }
   }

   public boolean execute(GameEntity var1) {
      if (var1.getAttributes() != null && this.attributeType.getValueType() == AttributeValueType.Boolean) {
         if (var1.getAttributes().contains(this.attributeType)) {
            var1.getAttributes().set(this.attributeType, this.value);
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public GeneratorBoolAttribute copy() {
      return new GeneratorBoolAttribute(this.attributeType, this.getChance(), this.value);
   }
}
