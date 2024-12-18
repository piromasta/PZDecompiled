package zombie.core.skinnedmodel.advancedanimation;

import java.util.function.Consumer;
import java.util.function.Supplier;

public interface IAnimationVariableCallbackMap extends IAnimationVariableMap {
   void setVariable(String var1, AnimationVariableSlotCallbackBool.CallbackGetStrongTyped var2);

   void setVariable(String var1, AnimationVariableSlotCallbackBool.CallbackGetStrongTyped var2, AnimationVariableSlotCallbackBool.CallbackSetStrongTyped var3);

   void setVariable(String var1, AnimationVariableSlotCallbackString.CallbackGetStrongTyped var2);

   void setVariable(String var1, AnimationVariableSlotCallbackString.CallbackGetStrongTyped var2, AnimationVariableSlotCallbackString.CallbackSetStrongTyped var3);

   void setVariable(String var1, AnimationVariableSlotCallbackFloat.CallbackGetStrongTyped var2);

   void setVariable(String var1, AnimationVariableSlotCallbackFloat.CallbackGetStrongTyped var2, AnimationVariableSlotCallbackFloat.CallbackSetStrongTyped var3);

   void setVariable(String var1, AnimationVariableSlotCallbackInt.CallbackGetStrongTyped var2);

   void setVariable(String var1, AnimationVariableSlotCallbackInt.CallbackGetStrongTyped var2, AnimationVariableSlotCallbackInt.CallbackSetStrongTyped var3);

   void setVariable(String var1, boolean var2, AnimationVariableSlotCallbackBool.CallbackGetStrongTyped var3);

   void setVariable(String var1, boolean var2, AnimationVariableSlotCallbackBool.CallbackGetStrongTyped var3, AnimationVariableSlotCallbackBool.CallbackSetStrongTyped var4);

   void setVariable(String var1, String var2, AnimationVariableSlotCallbackString.CallbackGetStrongTyped var3);

   void setVariable(String var1, String var2, AnimationVariableSlotCallbackString.CallbackGetStrongTyped var3, AnimationVariableSlotCallbackString.CallbackSetStrongTyped var4);

   void setVariable(String var1, float var2, AnimationVariableSlotCallbackFloat.CallbackGetStrongTyped var3);

   void setVariable(String var1, float var2, AnimationVariableSlotCallbackFloat.CallbackGetStrongTyped var3, AnimationVariableSlotCallbackFloat.CallbackSetStrongTyped var4);

   void setVariable(String var1, int var2, AnimationVariableSlotCallbackInt.CallbackGetStrongTyped var3);

   void setVariable(String var1, int var2, AnimationVariableSlotCallbackInt.CallbackGetStrongTyped var3, AnimationVariableSlotCallbackInt.CallbackSetStrongTyped var4);

   <EnumType extends Enum<EnumType>> void setVariable(String var1, Class<EnumType> var2, Supplier<EnumType> var3);

   <EnumType extends Enum<EnumType>> void setVariable(String var1, Class<EnumType> var2, Supplier<EnumType> var3, Consumer<EnumType> var4);
}
