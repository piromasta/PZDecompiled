package zombie.ai;

import zombie.ai.permission.DefaultStatePermissions;
import zombie.ai.permission.IStatePermissions;
import zombie.characters.IsoGameCharacter;
import zombie.characters.MoveDeltaModifiers;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;

public abstract class State {
   public State() {
   }

   public void enter(IsoGameCharacter var1) {
   }

   public void execute(IsoGameCharacter var1) {
   }

   public void exit(IsoGameCharacter var1) {
   }

   public void animEvent(IsoGameCharacter var1, AnimEvent var2) {
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
