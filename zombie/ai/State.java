package zombie.ai;

import zombie.ai.permission.DefaultStatePermissions;
import zombie.ai.permission.IStatePermissions;
import zombie.characters.IsoGameCharacter;
import zombie.characters.MoveDeltaModifiers;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.events.AnimEventBroadcaster;
import zombie.core.skinnedmodel.advancedanimation.events.IAnimEventListener;
import zombie.core.skinnedmodel.advancedanimation.events.IAnimEventWrappedBroadcaster;
import zombie.debug.DebugLog;

public abstract class State implements IAnimEventListener, IAnimEventWrappedBroadcaster {
   private final AnimEventBroadcaster m_animEventBroadcaster = new AnimEventBroadcaster();

   public State() {
   }

   public void enter(IsoGameCharacter var1) {
   }

   public void execute(IsoGameCharacter var1) {
   }

   public void exit(IsoGameCharacter var1) {
   }

   public AnimEventBroadcaster getAnimEventBroadcaster() {
      return this.m_animEventBroadcaster;
   }

   public void animEvent(IsoGameCharacter var1, AnimEvent var2) {
      DebugLog.ActionSystem.trace("%s.animEvent: %s(%s) time=%f", this.getClass().getName(), var2.m_EventName, var2.m_ParameterValue, var2.m_TimePc);
      this.getAnimEventBroadcaster().animEvent(var1, var2);
   }

   public boolean isAttacking(IsoGameCharacter var1) {
      return false;
   }

   public boolean isMoving(IsoGameCharacter var1) {
      return false;
   }

   public boolean isDoingActionThatCanBeCancelled() {
      return false;
   }

   public void getDeltaModifiers(IsoGameCharacter var1, MoveDeltaModifiers var2) {
   }

   public boolean isIgnoreCollide(IsoGameCharacter var1, int var2, int var3, int var4, int var5, int var6, int var7) {
      return false;
   }

   public String getName() {
      return this.getClass().getSimpleName();
   }

   public IStatePermissions getStatePermissions() {
      return DefaultStatePermissions.Instance;
   }
}
