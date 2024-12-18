package zombie.core.skinnedmodel.advancedanimation.events;

import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;

public interface IAnimEventCallback {
   void OnAnimEvent(AnimLayer var1, AnimEvent var2);
}
