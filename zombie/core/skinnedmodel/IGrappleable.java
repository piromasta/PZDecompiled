package zombie.core.skinnedmodel;

import org.lwjgl.util.vector.Vector3f;
import zombie.core.skinnedmodel.advancedanimation.GrappleOffsetBehaviour;
import zombie.core.skinnedmodel.advancedanimation.IAnimatable;
import zombie.inventory.types.HandWeapon;
import zombie.iso.Vector2;

public interface IGrappleable {
   void Grappled(IGrappleable var1, HandWeapon var2, float var3, String var4);

   void AcceptGrapple(IGrappleable var1, String var2);

   void RejectGrapple(IGrappleable var1);

   void LetGoOfGrappled(String var1);

   void GrapplerLetGo(IGrappleable var1, String var2);

   GrappleOffsetBehaviour getGrappleOffsetBehaviour();

   void setGrappleoffsetBehaviour(GrappleOffsetBehaviour var1);

   boolean isDoGrapple();

   void setDoGrapple(boolean var1);

   default void setDoGrappleLetGo() {
      this.setDoContinueGrapple(false);
   }

   IAnimatable getAnimatable();

   static IAnimatable getAnimatable(IGrappleable var0) {
      return var0 != null ? var0.getAnimatable() : null;
   }

   boolean isDoContinueGrapple();

   void setDoContinueGrapple(boolean var1);

   IGrappleable getGrappledBy();

   String getGrappledByString();

   String getGrappledByType();

   boolean isGrappling();

   boolean isBeingGrappled();

   boolean isBeingGrappledBy(IGrappleable var1);

   Vector2 getAnimForwardDirection(Vector2 var1);

   zombie.core.math.Vector3 getTargetGrapplePos(zombie.core.math.Vector3 var1);

   zombie.iso.Vector3 getTargetGrapplePos(zombie.iso.Vector3 var1);

   default void setTargetGrapplePos(zombie.core.math.Vector3 var1) {
      this.setTargetGrapplePos(var1.x, var1.y, var1.z);
   }

   default void setTargetGrapplePos(zombie.iso.Vector3 var1) {
      this.setTargetGrapplePos(var1.x, var1.y, var1.z);
   }

   void setTargetGrapplePos(float var1, float var2, float var3);

   Vector2 getTargetGrappleRotation(Vector2 var1);

   default void setTargetGrappleRotation(Vector2 var1) {
      this.setTargetGrappleRotation(var1.x, var1.y);
   }

   void setTargetGrappleRotation(float var1, float var2);

   default void setGrappleDeferredOffset(zombie.core.math.Vector3 var1) {
      this.setGrappleDeferredOffset(var1.x, var1.y, var1.z);
   }

   default void setGrappleDeferredOffset(zombie.iso.Vector3 var1) {
      this.setGrappleDeferredOffset(var1.x, var1.y, var1.z);
   }

   void setGrappleDeferredOffset(float var1, float var2, float var3);

   zombie.core.math.Vector3 getGrappleOffset(zombie.core.math.Vector3 var1);

   zombie.iso.Vector3 getGrappleOffset(zombie.iso.Vector3 var1);

   void setForwardDirection(Vector2 var1);

   void setTargetAndCurrentDirection(Vector2 var1);

   zombie.iso.Vector3 getPosition(zombie.iso.Vector3 var1);

   Vector3f getPosition(Vector3f var1);

   default void setPosition(zombie.iso.Vector3 var1) {
      this.setPosition(var1.x, var1.y, var1.z);
   }

   void setPosition(float var1, float var2, float var3);

   float getGrapplePosOffsetForward();

   void setGrapplePosOffsetForward(float var1);

   float getGrappleRotOffsetYaw();

   void setGrappleRotOffsetYaw(float var1);

   boolean isGrapplingTarget(IGrappleable var1);

   IGrappleable getGrapplingTarget();

   float getBearingToGrappledTarget();

   float getBearingFromGrappledTarget();

   String getSharedGrappleType();

   void setSharedGrappleType(String var1);

   String getSharedGrappleAnimNode();

   void setSharedGrappleAnimNode(String var1);

   float getSharedGrappleAnimTime();

   void setSharedGrappleAnimTime(float var1);

   String getGrappleResult();

   void setGrappleResult(String var1);

   default int getID() {
      return -1;
   }

   boolean canBeGrappled();

   boolean isPerformingAnyGrappleAnimation();

   boolean isPerformingGrappleGrabAnimation();

   void setPerformingGrappleGrabAnimation(boolean var1);

   boolean isPerformingGrappleAnimation();

   boolean isOnFloor();

   void setOnFloor(boolean var1);

   boolean isFallOnFront();

   void setFallOnFront(boolean var1);

   boolean isKilledByFall();

   void setKilledByFall(boolean var1);

   default boolean isMoving() {
      return false;
   }
}
