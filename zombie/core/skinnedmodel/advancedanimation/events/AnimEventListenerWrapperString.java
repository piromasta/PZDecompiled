package zombie.core.skinnedmodel.advancedanimation.events;

import zombie.characters.IsoGameCharacter;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;

public class AnimEventListenerWrapperString implements IAnimEventListener, IAnimEventListenerString {
   private final IAnimEventListenerString m_wrapped;

   private AnimEventListenerWrapperString(IAnimEventListenerString var1) {
      this.m_wrapped = var1;
   }

   public void animEvent(IsoGameCharacter var1, AnimEvent var2) {
      this.animEvent(var1, var2.m_ParameterValue);
   }

   public void animEvent(IsoGameCharacter var1, String var2) {
      this.m_wrapped.animEvent(var1, var2);
   }

   public static IAnimEventListener wrapper(IAnimEventListenerString var0) {
      return (IAnimEventListener)(var0 instanceof IAnimEventListener ? (IAnimEventListener)var0 : new AnimEventListenerWrapperString(var0));
   }
}
