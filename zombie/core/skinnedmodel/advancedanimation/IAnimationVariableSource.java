package zombie.core.skinnedmodel.advancedanimation;

public interface IAnimationVariableSource {
   IAnimationVariableSlot getVariable(AnimationVariableHandle var1);

   IAnimationVariableSlot getVariable(String var1);

   String getVariableString(String var1);

   float getVariableFloat(String var1, float var2);

   boolean getVariableBoolean(String var1);

   boolean getVariableBoolean(String var1, boolean var2);

   Iterable<IAnimationVariableSlot> getGameVariables();

   boolean isVariable(String var1, String var2);

   boolean containsVariable(String var1);

   default IAnimationVariableSource getSubVariableSource(String var1) {
      return null;
   }
}
