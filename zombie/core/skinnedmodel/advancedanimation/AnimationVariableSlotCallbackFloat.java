package zombie.core.skinnedmodel.advancedanimation;

import java.util.function.Consumer;
import java.util.function.Supplier;
import zombie.core.math.PZMath;

public final class AnimationVariableSlotCallbackFloat extends AnimationVariableSlotCallback<Float> {
   private float m_defaultValue = 0.0F;

   public AnimationVariableSlotCallbackFloat(String var1, CallbackGetStrongTyped var2) {
      super(var1, var2);
   }

   public AnimationVariableSlotCallbackFloat(String var1, CallbackGetStrongTyped var2, CallbackSetStrongTyped var3) {
      super(var1, var2, var3);
   }

   public AnimationVariableSlotCallbackFloat(String var1, float var2, CallbackGetStrongTyped var3) {
      super(var1, var3);
      this.m_defaultValue = var2;
   }

   public AnimationVariableSlotCallbackFloat(String var1, float var2, CallbackGetStrongTyped var3, CallbackSetStrongTyped var4) {
      super(var1, var3, var4);
      this.m_defaultValue = var2;
   }

   public Float getDefaultValue() {
      return this.m_defaultValue;
   }

   public String getValueString() {
      return ((Float)this.getValue()).toString();
   }

   public float getValueFloat() {
      return (Float)this.getValue();
   }

   public boolean getValueBool() {
      return this.getValueFloat() != 0.0F;
   }

   public void setValue(String var1) {
      this.trySetValue(PZMath.tryParseFloat(var1, 0.0F));
   }

   public void setValue(float var1) {
      this.trySetValue(var1);
   }

   public void setValue(boolean var1) {
      this.trySetValue(var1 ? 1.0F : 0.0F);
   }

   public AnimationVariableType getType() {
      return AnimationVariableType.Float;
   }

   public boolean canConvertFrom(String var1) {
      return true;
   }

   public interface CallbackSetStrongTyped extends Consumer<Float> {
   }

   public interface CallbackGetStrongTyped extends Supplier<Float> {
   }
}
