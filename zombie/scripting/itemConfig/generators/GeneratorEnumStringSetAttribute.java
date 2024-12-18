package zombie.scripting.itemConfig.generators;

import zombie.debug.DebugLog;
import zombie.entity.GameEntity;
import zombie.entity.components.attributes.AttributeInstance;
import zombie.entity.components.attributes.AttributeType;
import zombie.entity.components.attributes.AttributeValueType;
import zombie.scripting.itemConfig.RandomGenerator;

public class GeneratorEnumStringSetAttribute extends RandomGenerator<GeneratorEnumStringSetAttribute> {
   private final AttributeType.EnumStringSet attributeType;
   private final String[] enumsValues;
   private final String[] stringValues;
   private final Mode mode;

   public GeneratorEnumStringSetAttribute(AttributeType var1, Mode var2, String[] var3, String[] var4) {
      this(var1, var2, 1.0F, var3, var4);
   }

   public GeneratorEnumStringSetAttribute(AttributeType var1, Mode var2, float var3, String[] var4, String[] var5) {
      if (var3 < 0.0F) {
         throw new IllegalArgumentException("Chance may not be <= 0.");
      } else if (!(var1 instanceof AttributeType.EnumStringSet)) {
         throw new IllegalArgumentException("AttributeType valueType should be EnumStringSet.");
      } else {
         this.attributeType = (AttributeType.EnumStringSet)var1;
         this.setChance(var3);
         this.enumsValues = var4;
         this.stringValues = var5;
         this.mode = var2;
      }
   }

   public boolean execute(GameEntity var1) {
      if (var1.getAttributes() != null && this.attributeType.getValueType() == AttributeValueType.EnumSet) {
         if (var1.getAttributes().contains(this.attributeType)) {
            try {
               AttributeInstance.EnumStringSet var2 = (AttributeInstance.EnumStringSet)var1.getAttributes().getAttribute(this.attributeType);
               if (this.mode == GeneratorEnumStringSetAttribute.Mode.Set) {
                  var2.clear();
               }

               String[] var3;
               int var4;
               int var5;
               String var6;
               if (this.mode == GeneratorEnumStringSetAttribute.Mode.Remove) {
                  if (this.enumsValues != null) {
                     var3 = this.enumsValues;
                     var4 = var3.length;

                     for(var5 = 0; var5 < var4; ++var5) {
                        var6 = var3[var5];
                        if (!var2.removeEnumValueFromString(var6)) {
                           DebugLog.General.error("Unable to remove value '" + var6 + "'");
                        }
                     }
                  }

                  if (this.stringValues != null) {
                     var3 = this.stringValues;
                     var4 = var3.length;

                     for(var5 = 0; var5 < var4; ++var5) {
                        var6 = var3[var5];
                        if (!var2.removeStringValue(var6)) {
                           DebugLog.General.error("Unable to remove value '" + var6 + "'");
                        }
                     }
                  }
               } else {
                  if (this.enumsValues != null) {
                     var3 = this.enumsValues;
                     var4 = var3.length;

                     for(var5 = 0; var5 < var4; ++var5) {
                        var6 = var3[var5];
                        var2.addEnumValueFromString(var6);
                     }
                  }

                  if (this.stringValues != null) {
                     var3 = this.stringValues;
                     var4 = var3.length;

                     for(var5 = 0; var5 < var4; ++var5) {
                        var6 = var3[var5];
                        var2.addStringValue(var6);
                     }
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

   public GeneratorEnumStringSetAttribute copy() {
      return new GeneratorEnumStringSetAttribute(this.attributeType, this.mode, this.getChance(), this.enumsValues, this.stringValues);
   }

   public static enum Mode {
      Set,
      Add,
      Remove;

      private Mode() {
      }
   }
}
