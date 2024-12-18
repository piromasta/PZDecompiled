package zombie.core.skinnedmodel;

import zombie.core.skinnedmodel.advancedanimation.GrappleOffsetBehaviour;
import zombie.core.skinnedmodel.advancedanimation.IAnimatable;
import zombie.inventory.types.HandWeapon;
import zombie.iso.Vector2;

public interface IGrappleableWrapper extends IGrappleable {
   IGrappleable getWrappedGrappleable();

   default boolean isDoGrapple() {
      return this.getWrappedGrappleable().isDoGrapple();
   }

   default void setDoGrapple(boolean var1) {
      this.getWrappedGrappleable().setDoGrapple(var1);
   }

   default boolean isDoContinueGrapple() {
      return this.getWrappedGrappleable().isDoContinueGrapple();
   }

   default void setDoContinueGrapple(boolean var1) {
      this.getWrappedGrappleable().setDoContinueGrapple(var1);
   }

   default void Grappled(IGrappleable var1, HandWeapon var2, float var3, String var4) {
      this.getWrappedGrappleable().Grappled(var1, var2, var3, var4);
   }

   default void RejectGrapple(IGrappleable var1) {
      this.getWrappedGrappleable().RejectGrapple(var1);
   }

   default void AcceptGrapple(IGrappleable var1, String var2) {
      this.getWrappedGrappleable().AcceptGrapple(var1, var2);
   }

   default void LetGoOfGrappled(String var1) {
      this.getWrappedGrappleable().LetGoOfGrappled(var1);
   }

   default void GrapplerLetGo(IGrappleable var1, String var2) {
      this.getWrappedGrappleable().GrapplerLetGo(var1, var2);
   }

   default GrappleOffsetBehaviour getGrappleOffsetBehaviour() {
      return this.getWrappedGrappleable().getGrappleOffsetBehaviour();
   }

   default void setGrappleoffsetBehaviour(GrappleOffsetBehaviour var1) {
      this.getWrappedGrappleable().setGrappleoffsetBehaviour(var1);
   }

   default boolean isBeingGrappled() {
      return this.getWrappedGrappleable().isBeingGrappled();
   }

   default boolean isBeingGrappledBy(IGrappleable var1) {
      return this.getWrappedGrappleable().isBeingGrappledBy(var1);
   }

   default IGrappleable getGrappledBy() {
      return this.getWrappedGrappleable().getGrappledBy();
   }

   default String getGrappledByString() {
      return this.getWrappedGrappleable().getGrappledByString();
   }

   default String getGrappledByType() {
      return this.getWrappedGrappleable().getGrappledByType();
   }

   default boolean isGrappling() {
      return this.getWrappedGrappleable().isGrappling();
   }

   default boolean isGrapplingTarget(IGrappleable var1) {
      return this.getWrappedGrappleable().isGrapplingTarget(var1);
   }

   default IGrappleable getGrapplingTarget() {
      return this.getWrappedGrappleable().getGrapplingTarget();
   }

   default float getBearingToGrappledTarget() {
      return this.getWrappedGrappleable().getBearingToGrappledTarget();
   }

   default float getBearingFromGrappledTarget() {
      return this.getWrappedGrappleable().getBearingFromGrappledTarget();
   }

   default String getSharedGrappleType() {
      return this.getWrappedGrappleable().getSharedGrappleType();
   }

   default void setSharedGrappleType(String var1) {
      this.getWrappedGrappleable().setSharedGrappleType(var1);
   }

   default String getSharedGrappleAnimNode() {
      return this.getWrappedGrappleable().getSharedGrappleAnimNode();
   }

   default void setSharedGrappleAnimNode(String var1) {
      this.getWrappedGrappleable().setSharedGrappleAnimNode(var1);
   }

   default float getSharedGrappleAnimTime() {
      return this.getWrappedGrappleable().getSharedGrappleAnimTime();
   }

   default void setSharedGrappleAnimTime(float var1) {
      this.getWrappedGrappleable().setSharedGrappleAnimTime(var1);
   }

   default String getGrappleResult() {
      return this.getWrappedGrappleable().getGrappleResult();
   }

   default void setGrappleResult(String var1) {
      this.getWrappedGrappleable().setGrappleResult(var1);
   }

   default void setGrapplePosOffsetForward(float var1) {
      this.getWrappedGrappleable().setGrapplePosOffsetForward(var1);
   }

   default float getGrappleRotOffsetYaw() {
      return this.getWrappedGrappleable().getGrappleRotOffsetYaw();
   }

   default void setGrappleRotOffsetYaw(float var1) {
      this.getWrappedGrappleable().setGrappleRotOffsetYaw(var1);
   }

   default float getGrapplePosOffsetForward() {
      return this.getWrappedGrappleable().getGrapplePosOffsetForward();
   }

   default void setTargetAndCurrentDirection(Vector2 var1) {
      this.setForwardDirection(var1);
      IAnimatable var2 = this.getAnimatable();
      if (var2.hasAnimationPlayer()) {
         var2.getAnimationPlayer().setTargetAndCurrentDirection(var1);
      }

   }

   default zombie.core.math.Vector3 getTargetGrapplePos(zombie.core.math.Vector3 var1) {
      var1.set(0.0F, 0.0F, 0.0F);
      IAnimatable var2 = this.getAnimatable();
      if (var2 != null && var2.hasAnimationPlayer()) {
         var2.getAnimationPlayer().getTargetGrapplePos(var1);
      }

      return var1;
   }

   default zombie.iso.Vector3 getTargetGrapplePos(zombie.iso.Vector3 var1) {
      var1.set(0.0F, 0.0F, 0.0F);
      IAnimatable var2 = this.getAnimatable();
      if (var2 != null && var2.hasAnimationPlayer()) {
         var2.getAnimationPlayer().getTargetGrapplePos(var1);
      }

      return var1;
   }

   default void setTargetGrapplePos(float var1, float var2, float var3) {
      IAnimatable var4 = this.getAnimatable();
      if (var4 != null && var4.hasAnimationPlayer()) {
         var4.getAnimationPlayer().setTargetGrapplePos(var1, var2, var3);
      }

   }

   default void setTargetGrappleRotation(float var1, float var2) {
      IAnimatable var3 = this.getAnimatable();
      if (var3 != null && var3.hasAnimationPlayer()) {
         var3.getAnimationPlayer().setTargetGrappleRotation(var1, var2);
      }

   }

   default Vector2 getTargetGrappleRotation(Vector2 var1) {
      var1.set(1.0F, 0.0F);
      IAnimatable var2 = this.getAnimatable();
      if (var2 != null && var2.hasAnimationPlayer()) {
         var2.getAnimationPlayer().getTargetGrappleRotation(var1);
      }

      return var1;
   }

   default zombie.core.math.Vector3 getGrappleOffset(zombie.core.math.Vector3 var1) {
      var1.set(0.0F, 0.0F, 0.0F);
      IAnimatable var2 = this.getAnimatable();
      if (var2 != null && var2.hasAnimationPlayer()) {
         var2.getAnimationPlayer().getGrappleOffset(var1);
      }

      return var1;
   }

   default zombie.iso.Vector3 getGrappleOffset(zombie.iso.Vector3 var1) {
      var1.set(0.0F, 0.0F, 0.0F);
      IAnimatable var2 = this.getAnimatable();
      if (var2 != null && var2.hasAnimationPlayer()) {
         var2.getAnimationPlayer().getGrappleOffset(var1);
      }

      return var1;
   }

   default void setGrappleDeferredOffset(float var1, float var2, float var3) {
      IAnimatable var4 = this.getAnimatable();
      if (var4 != null && var4.hasAnimationPlayer()) {
         var4.getAnimationPlayer().setGrappleOffset(var1, var2, var3);
      }

   }

   default boolean canBeGrappled() {
      return !this.isBeingGrappled();
   }

   default boolean isPerformingAnyGrappleAnimation() {
      return this.getWrappedGrappleable().isPerformingAnyGrappleAnimation();
   }

   default boolean isPerformingGrappleGrabAnimation() {
      return this.getWrappedGrappleable().isPerformingGrappleGrabAnimation();
   }

   default void setPerformingGrappleGrabAnimation(boolean var1) {
      this.getWrappedGrappleable().setPerformingGrappleGrabAnimation(var1);
   }

   default boolean isOnFloor() {
      return this.getWrappedGrappleable().isOnFloor();
   }

   default void setOnFloor(boolean var1) {
      this.getWrappedGrappleable().setOnFloor(var1);
   }
}
