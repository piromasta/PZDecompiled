package zombie.core.skinnedmodel.advancedanimation.events;

import zombie.characters.IsoGameCharacter;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;

public class AnimEventListenerWrapperNoParam implements IAnimEventListener, IAnimEventListenerNoParam {
   private final IAnimEventListenerNoParam m_wrapped;

   private AnimEventListenerWrapperNoParam(IAnimEventListenerNoParam var1) {
      this.m_wrapped = var1;
   }

   public void animEvent(IsoGameCharacter var1, AnimEvent var2) {
      this.animEvent(var1);
   }

   public void animEvent(IsoGameCharacter var1) {
      this.m_wrapped.animEvent(var1);
   }

   public static IAnimEventListener wrapper(IAnimEventListenerNoParam var0) {
      return (IAnimEventListener)(var0 instanceof IAnimEventListener ? (IAnimEventListener)var0 : new AnimEventListenerWrapperNoParam(var0));
   }
}
