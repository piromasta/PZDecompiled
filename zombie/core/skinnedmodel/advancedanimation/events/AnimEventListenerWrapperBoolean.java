package zombie.core.skinnedmodel.advancedanimation.events;

import zombie.characters.IsoGameCharacter;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;

public class AnimEventListenerWrapperBoolean implements IAnimEventListener, IAnimEventListenerBoolean {
   private final IAnimEventListenerBoolean m_wrapped;

   private AnimEventListenerWrapperBoolean(IAnimEventListenerBoolean var1) {
      this.m_wrapped = var1;
   }

   public void animEvent(IsoGameCharacter var1, AnimEvent var2) {
      this.animEvent(var1, Boolean.parseBoolean(var2.m_ParameterValue));
   }

   public void animEvent(IsoGameCharacter var1, boolean var2) {
      this.m_wrapped.animEvent(var1, var2);
   }

   public static IAnimEventListener wrapper(IAnimEventListenerBoolean var0) {
      return (IAnimEventListener)(var0 instanceof IAnimEventListener ? (IAnimEventListener)var0 : new AnimEventListenerWrapperBoolean(var0));
   }
}
