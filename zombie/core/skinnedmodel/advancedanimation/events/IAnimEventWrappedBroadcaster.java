package zombie.core.skinnedmodel.advancedanimation.events;

import zombie.characters.IsoGameCharacter;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;

public interface IAnimEventWrappedBroadcaster extends IAnimEventListener {
   AnimEventBroadcaster getAnimEventBroadcaster();

   default void addAnimEventListener(String var1, IAnimEventListener var2) {
      this.getAnimEventBroadcaster().addListener(var1, var2);
   }

   default void addAnimEventListener(String var1, IAnimEventListenerBoolean var2) {
      this.getAnimEventBroadcaster().addListener(var1, var2);
   }

   default void addAnimEventListener(String var1, IAnimEventListenerString var2) {
      this.getAnimEventBroadcaster().addListener(var1, var2);
   }

   default void addAnimEventListener(String var1, IAnimEventListenerNoParam var2) {
      this.getAnimEventBroadcaster().addListener(var1, var2);
   }

   default void addAnimEventListener(String var1, IAnimEventListenerFloat var2) {
      this.getAnimEventBroadcaster().addListener(var1, var2);
   }

   default void addAnimEventListener(IAnimEventListenerSetVariableString var1) {
      this.getAnimEventBroadcaster().addListener(var1);
   }

   default void animEvent(IsoGameCharacter var1, AnimEvent var2) {
      this.getAnimEventBroadcaster().animEvent(var1, var2);
   }
}
