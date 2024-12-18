package zombie.core.skinnedmodel.advancedanimation.events;

import zombie.characters.IsoGameCharacter;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimEventSetVariable;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableReference;
import zombie.util.Type;

public class AnimEventListenerSetVariableWrapperString implements IAnimEventListener, IAnimEventListenerSetVariableString {
   private final IAnimEventListenerSetVariableString m_wrapped;

   private AnimEventListenerSetVariableWrapperString(IAnimEventListenerSetVariableString var1) {
      this.m_wrapped = var1;
   }

   public void animEvent(IsoGameCharacter var1, AnimEvent var2) {
      AnimEventSetVariable var3 = (AnimEventSetVariable)Type.tryCastTo(var2, AnimEventSetVariable.class);
      if (var3 != null) {
         this.animEvent(var1, var3.m_variableReference, var3.m_SetVariableValue);
      }

   }

   public void animEvent(IsoGameCharacter var1, AnimationVariableReference var2, String var3) {
      this.m_wrapped.animEvent(var1, var2, var3);
   }

   public static IAnimEventListener wrapper(IAnimEventListenerSetVariableString var0) {
      return (IAnimEventListener)(var0 instanceof IAnimEventListener ? (IAnimEventListener)var0 : new AnimEventListenerSetVariableWrapperString(var0));
   }
}
