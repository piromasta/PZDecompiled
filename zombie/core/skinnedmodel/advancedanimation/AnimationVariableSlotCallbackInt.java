package zombie.core.skinnedmodel.advancedanimation;

import java.util.function.Consumer;
import java.util.function.Supplier;
import zombie.core.math.PZMath;

public final class AnimationVariableSlotCallbackInt extends AnimationVariableSlotCallback<Integer> {
   private int m_defaultValue = 0;

   public AnimationVariableSlotCallbackInt(String var1, CallbackGetStrongTyped var2) {
      super(var1, var2);
   }

   public AnimationVariableSlotCallbackInt(String var1, CallbackGetStrongTyped var2, CallbackSetStrongTyped var3) {
      super(var1, var2, var3);
   }

   public AnimationVariableSlotCallbackInt(String var1, int var2, CallbackGetStrongTyped var3) {
      super(var1, var3);
      this.m_defaultValue = var2;
   }

   public AnimationVariableSlotCallbackInt(String var1, int var2, CallbackGetStrongTyped var3, CallbackSetStrongTyped var4) {
      super(var1, var3, var4);
      this.m_defaultValue = var2;
   }

   public Integer getDefaultValue() {
      return this.m_defaultValue;
   }

   public String getValueString() {
      return ((Integer)this.getValue()).toString();
   }

   public float getValueFloat() {
      return (float)(Integer)this.getValue();
   }

   public boolean getValueBool() {
      return this.getValueFloat() != 0.0F;
   }

   public void setValue(String var1) {
      this.trySetValue(PZMath.tryParseInt(var1, 0));
   }

   public void setValue(float var1) {
      this.trySetValue((int)var1);
   }

   public void setValue(boolean var1) {
      this.trySetValue(var1 ? 1 : 0);
   }

   public AnimationVariableType getType() {
      return AnimationVariableType.Float;
   }

   public boolean canConvertFrom(String var1) {
      return true;
   }

   public interface CallbackSetStrongTyped extends Consumer<Integer> {
   }

   public interface CallbackGetStrongTyped extends Supplier<Integer> {
   }
}
