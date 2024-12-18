package zombie.scripting.itemConfig.generators;

import zombie.debug.DebugLog;
import zombie.entity.GameEntity;
import zombie.entity.components.attributes.AttributeInstance;
import zombie.entity.components.attributes.AttributeType;
import zombie.entity.components.attributes.AttributeValueType;
import zombie.scripting.itemConfig.RandomGenerator;

public class GeneratorEnumSetAttribute extends RandomGenerator<GeneratorEnumSetAttribute> {
   private final AttributeType.EnumSet attributeType;
   private final String[] values;
   private final Mode mode;

   public GeneratorEnumSetAttribute(AttributeType var1, Mode var2, String[] var3) {
      this(var1, var2, 1.0F, var3);
   }

   public GeneratorEnumSetAttribute(AttributeType var1, Mode var2, float var3, String[] var4) {
      if (var3 < 0.0F) {
         throw new IllegalArgumentException("Chance may not be <= 0.");
      } else if (!(var1 instanceof AttributeType.EnumSet)) {
         throw new IllegalArgumentException("AttributeType valueType should be EnumSet.");
      } else {
         this.attributeType = (AttributeType.EnumSet)var1;
         this.setChance(var3);
         this.values = var4;
         this.mode = var2;
      }
   }

   public boolean execute(GameEntity var1) {
      if (var1.getAttributes() != null && this.attributeType.getValueType() == AttributeValueType.EnumSet) {
         if (var1.getAttributes().contains(this.attributeType)) {
            try {
               AttributeInstance.EnumSet var2 = (AttributeInstance.EnumSet)var1.getAttributes().getAttribute(this.attributeType);
               if (this.mode == GeneratorEnumSetAttribute.Mode.Set) {
                  var2.clear();
               }

               String[] var3;
               int var4;
               int var5;
               String var6;
               if (this.mode == GeneratorEnumSetAttribute.Mode.Remove) {
                  var3 = this.values;
                  var4 = var3.length;

                  for(var5 = 0; var5 < var4; ++var5) {
                     var6 = var3[var5];
                     if (!var2.removeValueFromString(var6)) {
                        DebugLog.General.error("Unable to remove value '" + var6 + "'");
                     }
                  }
               } else {
                  var3 = this.values;
                  var4 = var3.length;

                  for(var5 = 0; var5 < var4; ++var5) {
                     var6 = var3[var5];
                     var2.addValueFromString(var6);
                  }
               }
            } catch (Exception var7) {
               var7.printStackTrace();
            }

            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public GeneratorEnumSetAttribute copy() {
      return new GeneratorEnumSetAttribute(this.attributeType, this.mode, this.getChance(), this.values);
   }

   public static enum Mode {
      Set,
      Add,
      Remove;

      private Mode() {
      }
   }
}
