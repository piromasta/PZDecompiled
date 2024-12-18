package zombie.core.skinnedmodel.advancedanimation.events;

import zombie.characters.IsoGameCharacter;
import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;

public class AnimEventListenerWrapperFloat implements IAnimEventListener, IAnimEventListenerFloat {
   private final IAnimEventListenerFloat m_wrapped;

   private AnimEventListenerWrapperFloat(IAnimEventListenerFloat var1) {
      this.m_wrapped = var1;
   }

   public void animEvent(IsoGameCharacter var1, AnimEvent var2) {
      this.animEvent(var1, PZMath.tryParseFloat(var2.m_ParameterValue, 0.0F));
   }

   public void animEvent(IsoGameCharacter var1, float var2) {
      this.m_wrapped.animEvent(var1, var2);
   }

   public static IAnimEventListener wrapper(IAnimEventListenerFloat var0) {
      return (IAnimEventListener)(var0 instanceof IAnimEventListener ? (IAnimEventListener)var0 : new AnimEventListenerWrapperFloat(var0));
   }
}
