package zombie.core.skinnedmodel.advancedanimation;

import zombie.characters.action.ActionContext;
import zombie.core.skinnedmodel.IGrappleable;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.animation.debug.AnimationPlayerRecorder;
import zombie.core.skinnedmodel.model.ModelInstance;

public interface IAnimatable extends IAnimationVariableSource {
   ActionContext getActionContext();

   default boolean canTransitionToState(String var1) {
      ActionContext var2 = this.getActionContext();
      return var2 != null && var2.canTransitionToState(var1);
   }

   AnimationPlayer getAnimationPlayer();

   AnimationPlayerRecorder getAnimationPlayerRecorder();

   boolean isAnimationRecorderActive();

   AdvancedAnimator getAdvancedAnimator();

   ModelInstance getModelInstance();

   String GetAnimSetName();

   String getUID();

   default short getOnlineID() {
      return -1;
   }

   boolean hasAnimationPlayer();

   IGrappleable getGrappleable();
}
