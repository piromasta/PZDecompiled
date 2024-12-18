package zombie.core.skinnedmodel.advancedanimation;

import zombie.core.math.PZMath;
import zombie.util.StringUtils;

public abstract class AnimationVariableSlotEnum<EnumType extends Enum<EnumType>> extends AnimationVariableSlot {
   private final EnumType m_defaultValue;
   private final Class<EnumType> m_enumTypeClass;

   public AnimationVariableSlotEnum(Class<EnumType> var1, String var2, EnumType var3) {
      super(var2);
      this.m_enumTypeClass = var1;
      this.m_defaultValue = var3;
   }

   public abstract EnumType getValue();

   public abstract void setValue(EnumType var1);

   public String getValueString() {
      Enum var1 = this.getValue();
      return var1 != null ? var1.name() : "";
   }

   public float getValueFloat() {
      return PZMath.tryParseFloat(this.getValueString(), 0.0F);
   }

   public boolean getValueBool() {
      return StringUtils.tryParseBoolean(this.getValueString());
   }

   public EnumType getDefaultValue() {
      return this.m_defaultValue;
   }

   public void setValue(String var1) {
      try {
         this.setValue(Enum.valueOf(this.m_enumTypeClass, var1));
      } catch (IllegalArgumentException var3) {
         this.setValue(this.m_defaultValue);
      }

   }

   public void setValue(float var1) {
      this.setValue(String.valueOf(var1));
   }

   public void setValue(boolean var1) {
      this.setValue(var1 ? "true" : "false");
   }

   public AnimationVariableType getType() {
      return AnimationVariableType.String;
   }

   public boolean canConvertFrom(String var1) {
      try {
         Enum var2 = Enum.valueOf(this.m_enumTypeClass, var1);
         return var2 != null;
      } catch (IllegalArgumentException var3) {
         return false;
      }
   }

   public void clear() {
      this.setValue(this.m_defaultValue);
   }
}
