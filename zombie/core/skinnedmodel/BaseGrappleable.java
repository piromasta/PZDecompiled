package zombie.core.skinnedmodel;

import java.util.Objects;
import java.util.function.Supplier;
import org.lwjgl.util.vector.Vector3f;
import zombie.characters.IsoGameCharacter;
import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableSlotCallbackBool;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableSlotCallbackFloat;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableSlotCallbackString;
import zombie.core.skinnedmodel.advancedanimation.GrappleOffsetBehaviour;
import zombie.core.skinnedmodel.advancedanimation.IAnimatable;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableCallbackMap;
import zombie.debug.DebugLog;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IsoMovingObject;
import zombie.iso.Vector2;
import zombie.iso.objects.IsoDeadBody;
import zombie.util.StringUtils;
import zombie.util.lambda.Invokers;

public class BaseGrappleable implements IGrappleable {
   private IsoGameCharacter m_character;
   private IsoDeadBody m_deadBody;
   private IsoMovingObject m_isoMovingObject;
   private IGrappleable m_parentGrappleable;
   private boolean m_bDoGrapple = false;
   private boolean m_bDoContinueGrapple = false;
   private boolean m_bBeingGrappled = false;
   private IGrappleable m_grappledBy = null;
   private boolean m_bIsGrappling = false;
   private IGrappleable m_grapplingTarget = null;
   private String m_sharedGrappleType = "";
   private String m_sharedGrappleAnimNode = "";
   private float m_sharedGrappleTime = 0.0F;
   private String m_grappleResult = "";
   private float m_grappleOffsetForward = 0.0F;
   private float m_grappleOffsetYaw = 0.0F;
   private GrappleOffsetBehaviour m_grappleOffsetBehaviour;
   private boolean m_isPerformingGrappleGrabAnim;
   private Invokers.Params0.ICallback m_onGrappleBeginCallback;
   private Invokers.Params0.ICallback m_onGrappleEndCallback;

   public BaseGrappleable() {
      this.m_grappleOffsetBehaviour = GrappleOffsetBehaviour.None;
      this.m_isPerformingGrappleGrabAnim = false;
   }

   public BaseGrappleable(IsoGameCharacter var1) {
      this.m_grappleOffsetBehaviour = GrappleOffsetBehaviour.None;
      this.m_isPerformingGrappleGrabAnim = false;
      this.m_character = var1;
      this.m_isoMovingObject = this.m_character;
      this.m_parentGrappleable = this.m_character;
   }

   public BaseGrappleable(IsoDeadBody var1) {
      this.m_grappleOffsetBehaviour = GrappleOffsetBehaviour.None;
      this.m_isPerformingGrappleGrabAnim = false;
      this.m_deadBody = var1;
      this.m_isoMovingObject = this.m_deadBody;
      this.m_parentGrappleable = this.m_deadBody;
   }

   public IAnimatable getAnimatable() {
      return this.m_parentGrappleable.getAnimatable();
   }

   public void Grappled(IGrappleable var1, HandWeapon var2, float var3, String var4) {
      if (var1 == null) {
         DebugLog.Grapple.warn("Grappler is null. Nothing to grapple us.");
      } else if (var3 < 0.5F) {
         DebugLog.Grapple.debugln("Effectiveness insufficient. %f. Rejecting grapple.", var3);
         var1.RejectGrapple(this.getParentGrappleable());
      } else if (!this.canBeGrappled()) {
         DebugLog.Grapple.debugln("No transition available to grappled state.");
         var1.RejectGrapple(this.getParentGrappleable());
      } else {
         this.m_bBeingGrappled = true;
         this.m_grappledBy = var1;
         this.m_sharedGrappleType = var4;
         this.m_sharedGrappleAnimNode = "";
         this.m_sharedGrappleTime = 0.0F;
         DebugLog.Grapple.debugln("Accepting grapple by: %s", this.getGrappledByString(), this.getGrappledBy().getClass().getName());
         var1.AcceptGrapple(this.getParentGrappleable(), var4);
         this.invokeOnGrappleBeginEvent();
      }
   }

   public void RejectGrapple(IGrappleable var1) {
      if (this.isGrappling() && !this.isGrapplingTarget(var1)) {
         DebugLog.Grapple.warn("Target is not being grappled.");
      } else {
         DebugLog.Grapple.debugln("Grapple rejected.");
         this.resetGrappleStateToDefault("Rejected");
      }
   }

   public void AcceptGrapple(IGrappleable var1, String var2) {
      this.setGrapplingTarget(var1, var2);
      DebugLog.Grapple.debugln("Grapple accepted. Grappled target: %s", this.getGrapplingTarget().getClass().getName());
      this.invokeOnGrappleBeginEvent();
   }

   public void LetGoOfGrappled(String var1) {
      if (!this.isGrappling()) {
         DebugLog.Grapple.warn("Not currently grappling.");
      } else {
         IGrappleable var2 = this.getGrapplingTarget();
         this.resetGrappleStateToDefault(var1);
         if (var2 == null) {
            DebugLog.Grapple.warn("Nothing is being grappled. Nothing to let go of.");
         } else {
            DebugLog.Grapple.debugln("Letting go of grappled. Result: %s", var1);
            var2.GrapplerLetGo(this.getParentGrappleable(), var1);
            this.invokeOnGrappleEndEvent();
         }
      }
   }

   public void GrapplerLetGo(IGrappleable var1, String var2) {
      if (!this.isBeingGrappled()) {
         DebugLog.Grapple.warn("GrapplerLetGo> Not currently being grappled,.");
      } else if (!this.isBeingGrappledBy(var1)) {
         DebugLog.Grapple.warn("GrapplerLetGo> Not being grappled by this character.");
      } else {
         DebugLog.Grapple.debugln("Grappler has let us go. Result: %s.", var2);
         this.resetGrappleStateToDefault(var2);
         this.invokeOnGrappleEndEvent();
      }
   }

   private void resetGrappleStateToDefault() {
      this.resetGrappleStateToDefault("");
   }

   private void resetGrappleStateToDefault(String var1) {
      this.m_bDoGrapple = false;
      this.m_bDoContinueGrapple = false;
      this.m_bIsGrappling = false;
      this.m_bBeingGrappled = false;
      this.m_grapplingTarget = null;
      this.m_grappleResult = var1;
      this.m_sharedGrappleType = "";
      this.m_sharedGrappleAnimNode = "";
      this.m_sharedGrappleTime = 0.0F;
      this.m_grappleOffsetForward = 0.0F;
      this.m_grappleOffsetBehaviour = GrappleOffsetBehaviour.None;
      this.setGrappleDeferredOffset(0.0F, 0.0F, 0.0F);
   }

   public boolean isBeingGrappled() {
      return this.m_bBeingGrappled;
   }

   public boolean isBeingGrappledBy(IGrappleable var1) {
      return this.isBeingGrappled() && this.getGrappledBy() == var1;
   }

   public Vector2 getAnimForwardDirection(Vector2 var1) {
      var1.set(1.0F, 0.0F);
      return var1;
   }

   public zombie.core.math.Vector3 getTargetGrapplePos(zombie.core.math.Vector3 var1) {
      var1.set(0.0F, 0.0F, 0.0F);
      return var1;
   }

   public zombie.iso.Vector3 getTargetGrapplePos(zombie.iso.Vector3 var1) {
      var1.set(0.0F, 0.0F, 0.0F);
      return var1;
   }

   public void setTargetGrapplePos(zombie.core.math.Vector3 var1) {
      this.getParentGrappleable().setTargetGrapplePos(var1);
   }

   public void setTargetGrapplePos(zombie.iso.Vector3 var1) {
      this.getParentGrappleable().setTargetGrapplePos(var1);
   }

   public Vector2 getTargetGrappleRotation(Vector2 var1) {
      return this.getParentGrappleable().getTargetGrappleRotation(var1);
   }

   public void setTargetGrappleRotation(float var1, float var2) {
      this.getParentGrappleable().setTargetGrappleRotation(var1, var2);
   }

   public void setTargetGrapplePos(float var1, float var2, float var3) {
      this.getParentGrappleable().setTargetGrapplePos(var1, var2, var3);
   }

   public void setGrappleDeferredOffset(float var1, float var2, float var3) {
      this.getParentGrappleable().setGrappleDeferredOffset(var1, var2, var3);
   }

   public zombie.core.math.Vector3 getGrappleOffset(zombie.core.math.Vector3 var1) {
      return this.getParentGrappleable().getGrappleOffset(var1);
   }

   public zombie.iso.Vector3 getGrappleOffset(zombie.iso.Vector3 var1) {
      return this.getParentGrappleable().getGrappleOffset(var1);
   }

   public void setForwardDirection(Vector2 var1) {
      this.getParentGrappleable().setForwardDirection(var1);
   }

   public void setTargetAndCurrentDirection(Vector2 var1) {
      this.getParentGrappleable().setTargetAndCurrentDirection(var1);
   }

   public zombie.iso.Vector3 getPosition(zombie.iso.Vector3 var1) {
      return this.getParentGrappleable().getPosition(var1);
   }

   public Vector3f getPosition(Vector3f var1) {
      return this.getParentGrappleable().getPosition(var1);
   }

   public void setPosition(float var1, float var2, float var3) {
      this.getParentGrappleable().setPosition(var1, var2, var3);
   }

   public IGrappleable getGrappledBy() {
      return this.isBeingGrappled() ? this.m_grappledBy : null;
   }

   public String getGrappledByString() {
      if (this.isBeingGrappled()) {
         return this.m_grappledBy != null ? this.m_grappledBy.getClass().getName() + "_" + this.m_grappledBy.getID() : "null";
      } else {
         return "";
      }
   }

   public String getGrappledByType() {
      if (this.isBeingGrappled()) {
         return this.m_grappledBy != null ? this.m_grappledBy.getClass().getName() : "null";
      } else {
         return "None";
      }
   }

   public boolean isGrappling() {
      return this.m_bIsGrappling;
   }

   public boolean isGrapplingTarget(IGrappleable var1) {
      return this.getGrapplingTarget() == var1;
   }

   public IGrappleable getGrapplingTarget() {
      return !this.isGrappling() ? null : this.m_grapplingTarget;
   }

   private void setGrapplingTarget(IGrappleable var1, String var2) {
      this.resetGrappleStateToDefault();
      this.m_bIsGrappling = true;
      this.m_bDoContinueGrapple = true;
      this.m_grapplingTarget = var1;
      this.m_sharedGrappleType = var2;
   }

   public float getBearingToGrappledTarget() {
      IGrappleable var1 = this.getGrapplingTarget();
      if (var1 == null) {
         return 0.0F;
      } else {
         float var2 = PZMath.calculateBearing(this.getPosition(new zombie.iso.Vector3()), this.getAnimForwardDirection(new Vector2()), var1.getPosition(new zombie.iso.Vector3()));
         return var2;
      }
   }

   public float getBearingFromGrappledTarget() {
      IGrappleable var1 = this.getGrapplingTarget();
      if (var1 == null) {
         return 0.0F;
      } else {
         float var2 = PZMath.calculateBearing(var1.getPosition(new zombie.iso.Vector3()), var1.getAnimForwardDirection(new Vector2()), this.getPosition(new zombie.iso.Vector3()));
         return var2;
      }
   }

   public String getSharedGrappleType() {
      return this.m_sharedGrappleType;
   }

   public void setSharedGrappleType(String var1) {
      if (!StringUtils.equals(this.m_sharedGrappleType, var1)) {
         this.m_sharedGrappleType = var1;
         IGrappleable var2 = this.getGrapplingTarget();
         if (var2 != null) {
            var2.setSharedGrappleType(this.m_sharedGrappleType);
         }

         IGrappleable var3 = this.getGrappledBy();
         if (var3 != null) {
            var3.setSharedGrappleType(this.m_sharedGrappleType);
         }

      }
   }

   public String getSharedGrappleAnimNode() {
      return this.m_sharedGrappleAnimNode;
   }

   public void setSharedGrappleAnimNode(String var1) {
      this.m_sharedGrappleAnimNode = var1;
   }

   public float getSharedGrappleAnimTime() {
      return this.m_sharedGrappleTime;
   }

   public void setSharedGrappleAnimTime(float var1) {
      this.m_sharedGrappleTime = var1;
   }

   public String getGrappleResult() {
      return this.m_grappleResult;
   }

   public void setGrappleResult(String var1) {
      this.m_grappleResult = var1;
   }

   public IGrappleable getParentGrappleable() {
      return this.m_parentGrappleable;
   }

   public boolean canBeGrappled() {
      IGrappleable var1 = this.getParentGrappleable();
      return var1 != null && var1.canBeGrappled();
   }

   public void setGrapplePosOffsetForward(float var1) {
      this.m_grappleOffsetForward = var1;
   }

   public float getGrapplePosOffsetForward() {
      if (this.isBeingGrappled()) {
         return this.getGrappledBy().getGrapplePosOffsetForward();
      } else {
         return this.isGrappling() ? this.m_grappleOffsetForward : 0.0F;
      }
   }

   public void setGrappleRotOffsetYaw(float var1) {
      this.m_grappleOffsetYaw = var1;
   }

   public float getGrappleRotOffsetYaw() {
      if (this.isBeingGrappled()) {
         return this.getGrappledBy().getGrappleRotOffsetYaw();
      } else {
         return this.isGrappling() ? this.m_grappleOffsetYaw : 0.0F;
      }
   }

   public GrappleOffsetBehaviour getGrappleOffsetBehaviour() {
      if (this.isBeingGrappled()) {
         return this.getGrappledBy().getGrappleOffsetBehaviour();
      } else {
         return this.isGrappling() ? this.m_grappleOffsetBehaviour : GrappleOffsetBehaviour.None;
      }
   }

   public void setGrappleoffsetBehaviour(GrappleOffsetBehaviour var1) {
      this.m_grappleOffsetBehaviour = var1;
   }

   public boolean isDoGrapple() {
      return this.m_bDoGrapple || this.isPerformingGrappleGrabAnimation();
   }

   public void setDoGrapple(boolean var1) {
      this.m_bDoGrapple = var1;
   }

   public boolean isDoContinueGrapple() {
      return this.m_bDoContinueGrapple;
   }

   public void setDoContinueGrapple(boolean var1) {
      this.m_bDoContinueGrapple = var1;
   }

   public boolean isPerformingAnyGrappleAnimation() {
      return this.isPerformingGrappleGrabAnimation() || this.isPerformingGrappleAnimation();
   }

   public boolean isPerformingGrappleGrabAnimation() {
      return this.m_isPerformingGrappleGrabAnim;
   }

   public void setPerformingGrappleGrabAnimation(boolean var1) {
      this.m_isPerformingGrappleGrabAnim = var1;
   }

   public boolean isPerformingGrappleAnimation() {
      return this.getParentGrappleable().isPerformingGrappleAnimation();
   }

   public boolean isOnFloor() {
      return this.m_isoMovingObject != null && this.m_isoMovingObject.isOnFloor();
   }

   public void setOnFloor(boolean var1) {
      if (this.m_isoMovingObject != null) {
         this.m_isoMovingObject.setOnFloor(var1);
      }

   }

   public boolean isFallOnFront() {
      return this.m_character != null && this.m_character.isFallOnFront() || this.m_deadBody != null && this.m_deadBody.isFallOnFront();
   }

   public void setFallOnFront(boolean var1) {
      if (this.m_character != null) {
         this.m_character.setFallOnFront(var1);
      }

      if (this.m_deadBody != null) {
         this.m_deadBody.setFallOnFront(var1);
      }

   }

   public boolean isKilledByFall() {
      return this.m_character != null && this.m_character.isKilledByFall() || this.m_deadBody != null && this.m_deadBody.isKilledByFall();
   }

   public void setKilledByFall(boolean var1) {
      if (this.m_character != null) {
         this.m_character.setKilledByFall(var1);
      }

      if (this.m_deadBody != null) {
         this.m_deadBody.setKilledByFall(var1);
      }

   }

   public void setOnGrappledBeginCallback(Invokers.Params0.ICallback var1) {
      this.m_onGrappleBeginCallback = var1;
   }

   private void invokeOnGrappleBeginEvent() {
      if (this.m_onGrappleBeginCallback != null) {
         this.m_onGrappleBeginCallback.accept();
      }

   }

   public void setOnGrappledEndCallback(Invokers.Params0.ICallback var1) {
      this.m_onGrappleEndCallback = var1;
   }

   private void invokeOnGrappleEndEvent() {
      if (this.m_onGrappleEndCallback != null) {
         this.m_onGrappleEndCallback.accept();
      }

   }

   public static void RegisterGrappleVariables(IAnimationVariableCallbackMap var0, IGrappleable var1) {
      Objects.requireNonNull(var1);
      var0.setVariable("bDoGrapple", var1::isDoGrapple);
      Objects.requireNonNull(var1);
      var0.setVariable("bDoContinueGrapple", var1::isDoContinueGrapple);
      Objects.requireNonNull(var1);
      var0.setVariable("bIsGrappling", var1::isGrappling);
      Objects.requireNonNull(var1);
      AnimationVariableSlotCallbackString.CallbackGetStrongTyped var10002 = var1::getGrappleResult;
      Objects.requireNonNull(var1);
      var0.setVariable("grappleResult", var10002, var1::setGrappleResult);
      Objects.requireNonNull(var1);
      var0.setVariable("sharedGrappleType", var1::getSharedGrappleType);
      Objects.requireNonNull(var1);
      var10002 = var1::getSharedGrappleAnimNode;
      Objects.requireNonNull(var1);
      var0.setVariable("sharedGrappleAnimNode", var10002, var1::setSharedGrappleAnimNode);
      Objects.requireNonNull(var1);
      var0.setVariable("sharedGrappleTime", var1::getSharedGrappleAnimTime);
      Objects.requireNonNull(var1);
      AnimationVariableSlotCallbackFloat.CallbackGetStrongTyped var2 = var1::getGrapplePosOffsetForward;
      Objects.requireNonNull(var1);
      var0.setVariable("grappleOffsetForward", var2, var1::setGrapplePosOffsetForward);
      Objects.requireNonNull(var1);
      Supplier var10003 = var1::getGrappleOffsetBehaviour;
      Objects.requireNonNull(var1);
      var0.setVariable("grappleOffsetBehaviour", GrappleOffsetBehaviour.class, var10003, var1::setGrappleoffsetBehaviour);
      Objects.requireNonNull(var1);
      var0.setVariable("bearingToGrappledTarget", var1::getBearingToGrappledTarget);
      Objects.requireNonNull(var1);
      var0.setVariable("bearingFromGrappledTarget", var1::getBearingFromGrappledTarget);
      Objects.requireNonNull(var1);
      var0.setVariable("bBeingGrappled", var1::isBeingGrappled);
      Objects.requireNonNull(var1);
      var0.setVariable("grappledBy", var1::getGrappledByString);
      Objects.requireNonNull(var1);
      var0.setVariable("grappledByType", var1::getGrappledByType);
      Objects.requireNonNull(var1);
      AnimationVariableSlotCallbackBool.CallbackGetStrongTyped var3 = var1::isPerformingGrappleGrabAnimation;
      Objects.requireNonNull(var1);
      var0.setVariable("GrappleGrabAnim", var3, var1::setPerformingGrappleGrabAnimation);
      Objects.requireNonNull(var1);
      var0.setVariable("GrappleAnim", var1::isPerformingGrappleAnimation);
      Objects.requireNonNull(var1);
      var0.setVariable("AnyGrappleAnim", var1::isPerformingAnyGrappleAnimation);
   }
}
