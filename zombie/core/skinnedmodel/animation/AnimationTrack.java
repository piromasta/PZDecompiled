package zombie.core.skinnedmodel.animation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;
import zombie.core.PerformanceSettings;
import zombie.core.math.PZMath;
import zombie.core.profiling.PerformanceProfileProbe;
import zombie.core.skinnedmodel.HelperFunctions;
import zombie.core.skinnedmodel.advancedanimation.AnimBoneWeight;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.advancedanimation.PooledAnimBoneWeightArray;
import zombie.core.skinnedmodel.model.SkinningBone;
import zombie.core.skinnedmodel.model.SkinningData;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.iso.Vector2;
import zombie.network.GameServer;
import zombie.network.ServerGUI;
import zombie.util.IPooledObject;
import zombie.util.Lambda;
import zombie.util.Pool;
import zombie.util.PooledArrayObject;
import zombie.util.PooledFloatArrayObject;
import zombie.util.PooledObject;
import zombie.util.StringUtils;
import zombie.util.lambda.Consumers;
import zombie.util.list.PZArrayUtil;

public final class AnimationTrack extends PooledObject {
   public boolean IsPlaying;
   public boolean IsPrimary;
   public AnimationClip CurrentClip;
   public int priority;
   private boolean m_isRagdollFirstFrame;
   public float m_ragdollStartTime = 0.0F;
   public float m_ragdollMaxTime = 5.0F;
   private float m_currentTimeValue;
   private float m_previousTimeValue;
   public boolean SyncTrackingEnabled;
   private boolean m_IKAimingLeftArm = false;
   private boolean m_IKAimingRightArm = false;
   public boolean reverse;
   public boolean bLooping;
   private final KeyframeSpan[] m_pose = new KeyframeSpan[60];
   private final KeyframeSpan m_deferredPoseSpan = new KeyframeSpan();
   public float SpeedDelta;
   public float BlendDelta;
   public float blendFieldWeight;
   public String name;
   private String matchingGrappledAnimNode;
   private boolean isGrappler = false;
   public float earlyBlendOutTime;
   public boolean triggerOnNonLoopedAnimFadeOutEvent;
   private int m_layerIdx;
   public AnimLayer m_animLayer;
   private PooledArrayObject<AnimBoneWeight> m_boneWeightBindings;
   private PooledFloatArrayObject m_boneWeights;
   private final ArrayList<IAnimListener> listeners = new ArrayList();
   private final ArrayList<IAnimListener> listenersInvoking = new ArrayList();
   private SkinningBone m_deferredBone;
   private BoneAxis m_deferredBoneAxis;
   private boolean m_useDeferredMovement = true;
   private boolean m_useDeferredRotation;
   private float m_deferredRotationScale = 1.0F;
   private final DeferredMotionData m_deferredMotion = new DeferredMotionData();
   public boolean m_isInitialAdjustmentCalculated = false;
   public final Vector3f m_initialAdjustment = new Vector3f();
   private static final Pool<AnimationTrack> s_pool = new Pool(AnimationTrack::new);

   public static AnimationTrack alloc() {
      return (AnimationTrack)s_pool.alloc();
   }

   protected AnimationTrack() {
      PZArrayUtil.arrayPopulate(this.m_pose, KeyframeSpan::new);
      this.resetInternal();
   }

   private AnimationTrack resetInternal() {
      this.IsPlaying = false;
      this.IsPrimary = false;
      this.CurrentClip = null;
      this.priority = 0;
      this.m_isRagdollFirstFrame = false;
      this.m_ragdollStartTime = 0.0F;
      this.m_ragdollMaxTime = 5.0F;
      this.m_currentTimeValue = 0.0F;
      this.m_previousTimeValue = 0.0F;
      this.SyncTrackingEnabled = true;
      this.reverse = false;
      this.bLooping = false;
      PZArrayUtil.forEach((Object[])this.m_pose, KeyframeSpan::clear);
      this.m_deferredPoseSpan.clear();
      this.SpeedDelta = 1.0F;
      this.BlendDelta = 0.0F;
      this.blendFieldWeight = 0.0F;
      this.name = "!Empty!";
      this.earlyBlendOutTime = 0.0F;
      this.triggerOnNonLoopedAnimFadeOutEvent = false;
      this.m_animLayer = null;
      this.m_layerIdx = -1;
      Pool.tryRelease((IPooledObject)this.m_boneWeightBindings);
      this.m_boneWeightBindings = null;
      Pool.tryRelease((IPooledObject)this.m_boneWeights);
      this.m_boneWeights = null;
      this.listeners.clear();
      this.listenersInvoking.clear();
      this.m_deferredBone = null;
      this.m_deferredBoneAxis = BoneAxis.Y;
      this.m_useDeferredMovement = true;
      this.m_useDeferredRotation = false;
      this.m_deferredRotationScale = 1.0F;
      this.m_deferredMotion.reset();
      this.m_isInitialAdjustmentCalculated = false;
      return this;
   }

   public void get(int var1, Vector3f var2, Quaternion var3, Vector3f var4) {
      this.m_pose[var1].lerp(this.getCurrentAnimationTime(), var2, var3, var4);
   }

   private Keyframe getDeferredMovementFrameAt(int var1, float var2, Keyframe var3) {
      KeyframeSpan var4 = this.getKeyframeSpan(var1, var2, this.m_deferredPoseSpan);
      return var4.lerp(var2, var3);
   }

   private KeyframeSpan getKeyframeSpan(int var1, float var2, KeyframeSpan var3) {
      if (!var3.isBone(var1)) {
         var3.clear();
      }

      Keyframe[] var4 = this.CurrentClip.getBoneFramesAt(var1);
      int var5 = var4.length;
      if (var5 == 0) {
         var3.clear();
         return var3;
      } else if (var3.containsTime(var2)) {
         return var3;
      } else {
         Keyframe var6 = var4[var5 - 1];
         if (var2 >= var6.Time) {
            var3.fromIdx = var5 > 1 ? var5 - 2 : 0;
            var3.toIdx = var5 - 1;
            var3.from = var4[var3.fromIdx];
            var3.to = var4[var3.toIdx];
            return var3;
         } else {
            Keyframe var7 = var4[0];
            if (var2 <= var7.Time) {
               var3.clear();
               var3.toIdx = 0;
               var3.to = var7;
               return var3;
            } else {
               int var8 = 0;
               if (var3.isSpan() && var3.to.Time <= var2) {
                  var8 = var3.toIdx;
               }

               var3.clear();

               for(int var9 = var8; var9 < var5 - 1; ++var9) {
                  Keyframe var10 = var4[var9];
                  Keyframe var11 = var4[var9 + 1];
                  if (var10.Time <= var2 && var2 <= var11.Time) {
                     var3.fromIdx = var9;
                     var3.toIdx = var9 + 1;
                     var3.from = var10;
                     var3.to = var11;
                     break;
                  }
               }

               return var3;
            }
         }
      }
   }

   public void removeListener(IAnimListener var1) {
      this.listeners.remove(var1);
   }

   public void Update(float var1) {
      try {
         this.UpdateKeyframes(var1);
      } catch (Exception var3) {
         var3.printStackTrace();
      }

   }

   public void UpdateKeyframes(float var1) {
      AnimationTrack.s_performance.updateKeyframes.invokeAndMeasure(this, var1, AnimationTrack::updateKeyframesInternal);
   }

   private void updateKeyframesInternal(float var1) {
      if (this.CurrentClip == null) {
         throw new RuntimeException("AnimationPlayer.Update was called before startClip");
      } else {
         if (var1 > 0.0F) {
            this.TickCurrentTime(var1);
         }

         if (!GameServer.bServer || ServerGUI.isCreated()) {
            this.updatePose();
         }

         this.updateDeferredValues();
      }
   }

   private void updatePose() {
      AnimationTrack.s_performance.updatePose.invokeAndMeasure(this, AnimationTrack::updatePoseInternal);
   }

   private void updatePoseInternal() {
      float var1 = this.getCurrentAnimationTime();

      for(int var2 = 0; var2 < 60; ++var2) {
         this.getKeyframeSpan(var2, var1, this.m_pose[var2]);
      }

   }

   private void updateDeferredValues() {
      AnimationTrack.s_performance.updateDeferredValues.invokeAndMeasure(this, AnimationTrack::updateDeferredValuesInternal);
   }

   private void updateDeferredValuesInternal() {
      if (this.m_deferredBone != null) {
         DeferredMotionData var1 = this.m_deferredMotion;
         var1.m_deferredRotationDiff = 0.0F;
         var1.m_deferredMovementDiff.set(0.0F, 0.0F);
         var1.m_counterRotatedMovementDiff.set(0.0F, 0.0F);
         float var2 = this.getPreviousAnimationTime();
         float var3 = this.getCurrentAnimationTime();
         if (this.isLooping() && var2 > var3) {
            float var4 = this.getDuration();
            this.appendDeferredValues(var1, var2, var4);
            var2 = 0.0F;
         }

         this.appendDeferredValues(var1, var2, var3);
      }
   }

   private void appendDeferredValues(DeferredMotionData var1, float var2, float var3) {
      int var4 = this.getDeferredMovementBoneIdx();
      Keyframe var5 = this.getDeferredMovementFrameAt(var4, var2, AnimationTrack.L_updateDeferredValues.prevKeyFrame);
      Keyframe var6 = this.getDeferredMovementFrameAt(var4, var3, AnimationTrack.L_updateDeferredValues.keyFrame);
      if (!GameServer.bServer) {
         var1.m_prevDeferredRotation = this.getDeferredTwistRotation(var5.Rotation);
         var1.m_targetDeferredRotationQ.set(var6.Rotation);
         var1.m_targetDeferredRotation = this.getDeferredTwistRotation(var6.Rotation);
         float var7 = PZMath.getClosestAngle(var1.m_prevDeferredRotation, var1.m_targetDeferredRotation);
         var1.m_deferredRotationDiff += var7 * this.getDeferredRotationScale();
      }

      this.getDeferredMovement(var5.Position, var1.m_prevDeferredMovement);
      var1.m_targetDeferredPosition.set(var6.Position);
      this.getDeferredMovement(var6.Position, var1.m_targetDeferredMovement);
      Vector2 var10 = AnimationTrack.L_updateDeferredValues.diff.set(var1.m_targetDeferredMovement.x - var1.m_prevDeferredMovement.x, var1.m_targetDeferredMovement.y - var1.m_prevDeferredMovement.y);
      Vector2 var8 = AnimationTrack.L_updateDeferredValues.crDiff.set(var10);
      if (this.getUseDeferredRotation() && !this.isRagdoll()) {
         float var9 = var8.normalize();
         var8.rotate(-(var1.m_targetDeferredRotation + 1.5707964F));
         var8.scale(-var9);
      }

      Vector2 var10000 = var1.m_deferredMovementDiff;
      var10000.x += var10.x;
      var10000 = var1.m_deferredMovementDiff;
      var10000.y += var10.y;
      var10000 = var1.m_counterRotatedMovementDiff;
      var10000.x += var8.x;
      var10000 = var1.m_counterRotatedMovementDiff;
      var10000.y += var8.y;
   }

   private float getDeferredTwistRotation(Quaternion var1) {
      if (this.m_deferredBoneAxis == BoneAxis.Z) {
         return HelperFunctions.getRotationZ(var1);
      } else if (this.m_deferredBoneAxis == BoneAxis.Y) {
         return HelperFunctions.getRotationY(var1);
      } else {
         DebugLog.Animation.error("BoneAxis unhandled: %s", String.valueOf(this.m_deferredBoneAxis));
         return 0.0F;
      }
   }

   private Vector2 getDeferredMovement(Vector3f var1, Vector2 var2) {
      if (this.m_deferredBoneAxis == BoneAxis.Y) {
         var2.set(var1.x, -var1.z);
      } else {
         var2.set(var1.x, var1.y);
      }

      return var2;
   }

   public Vector3f getCurrentDeferredCounterPosition(Vector3f var1) {
      this.getCurrentDeferredPosition(var1);
      if (this.m_deferredBoneAxis == BoneAxis.Y) {
         var1.set(-var1.x, 0.0F, var1.z);
      } else {
         var1.set(-var1.x, -var1.y, 0.0F);
      }

      return var1;
   }

   public float getCurrentDeferredRotation() {
      return this.m_deferredMotion.m_targetDeferredRotation;
   }

   public Vector3f getCurrentDeferredPosition(Vector3f var1) {
      var1.set(this.m_deferredMotion.m_targetDeferredPosition);
      return var1;
   }

   public int getDeferredMovementBoneIdx() {
      return this.m_deferredBone != null ? this.m_deferredBone.Index : -1;
   }

   public float getCurrentTrackTime() {
      return this.getReversibleTimeValue(this.m_currentTimeValue);
   }

   public float getPreviousTrackTime() {
      return this.getReversibleTimeValue(this.m_previousTimeValue);
   }

   public float getCurrentAnimationTime() {
      return this.isRagdoll() ? this.CurrentClip.getDuration() : this.getCurrentTrackTime();
   }

   public float getPreviousAnimationTime() {
      return this.isRagdoll() ? 0.0F : this.getPreviousTrackTime();
   }

   private float getReversibleTimeValue(float var1) {
      return this.reverse ? this.getDuration() - var1 : var1;
   }

   protected void TickCurrentTime(float var1) {
      AnimationTrack.s_performance.tickCurrentTime.invokeAndMeasure(this, var1, AnimationTrack::tickCurrentTimeInternal);
   }

   private void tickCurrentTimeInternal(float var1) {
      var1 *= this.SpeedDelta;
      if (!this.IsPlaying) {
         var1 = 0.0F;
      }

      float var2 = this.getDuration();
      this.m_previousTimeValue = this.m_currentTimeValue;
      this.m_currentTimeValue += var1;
      if (this.bLooping) {
         if (this.m_previousTimeValue == 0.0F && this.m_currentTimeValue > 0.0F) {
            this.invokeOnAnimStartedEvent();
         }

         if (this.m_currentTimeValue >= var2) {
            this.invokeOnLoopedAnimEvent();
            this.m_currentTimeValue %= var2;
            this.invokeOnAnimStartedEvent();
         }

      } else {
         if (this.m_currentTimeValue < 0.0F) {
            this.m_currentTimeValue = 0.0F;
         }

         if (this.m_previousTimeValue == 0.0F && this.m_currentTimeValue > 0.0F) {
            this.invokeOnAnimStartedEvent();
         }

         if (this.triggerOnNonLoopedAnimFadeOutEvent) {
            float var3 = var2 - this.earlyBlendOutTime;
            if (this.m_previousTimeValue < var3 && var3 <= this.m_currentTimeValue) {
               this.invokeOnNonLoopedAnimFadeOutEvent();
            }
         }

         if (this.m_currentTimeValue > var2) {
            this.m_currentTimeValue = var2;
         }

         boolean var4 = this.isRagdoll();
         if (!var4 && this.m_previousTimeValue < var2 && this.m_currentTimeValue >= var2) {
            this.invokeOnLoopedAnimEvent();
            this.invokeOnNonLoopedAnimFinishedEvent();
         }

      }
   }

   public float getDuration() {
      if (this.isRagdoll()) {
         return this.m_ragdollMaxTime;
      } else {
         return this.hasClip() ? this.CurrentClip.getDuration() : 0.0F;
      }
   }

   private void invokeListeners(Consumer<IAnimListener> var1) {
      if (!this.listeners.isEmpty()) {
         this.listenersInvoking.clear();
         this.listenersInvoking.addAll(this.listeners);

         for(int var2 = 0; var2 < this.listenersInvoking.size(); ++var2) {
            IAnimListener var3 = (IAnimListener)this.listenersInvoking.get(var2);
            var1.accept(var3);
         }

      }
   }

   private <T1> void invokeListeners(T1 var1, Consumers.Params1.ICallback<IAnimListener, T1> var2) {
      Lambda.capture(this, var1, var2, (var0, var1x, var2x, var3) -> {
         var1x.invokeListeners(var0.consumer(var2x, var3));
      });
   }

   protected void invokeOnAnimStartedEvent() {
      this.invokeListeners(this, IAnimListener::onAnimStarted);
   }

   protected void invokeOnLoopedAnimEvent() {
      this.invokeListeners(this, IAnimListener::onLoopedAnim);
   }

   protected void invokeOnNonLoopedAnimFadeOutEvent() {
      this.invokeListeners(this, IAnimListener::onNonLoopedAnimFadeOut);
   }

   protected void invokeOnNonLoopedAnimFinishedEvent() {
      this.invokeListeners(this, IAnimListener::onNonLoopedAnimFinished);
   }

   public void onReleased() {
      if (!this.listeners.isEmpty()) {
         this.listenersInvoking.clear();
         this.listenersInvoking.addAll(this.listeners);

         for(int var1 = 0; var1 < this.listenersInvoking.size(); ++var1) {
            IAnimListener var2 = (IAnimListener)this.listenersInvoking.get(var1);
            var2.onTrackDestroyed(this);
         }

         this.listeners.clear();
         this.listenersInvoking.clear();
      }

      this.reset();
   }

   public Vector2 getDeferredMovementDiff(Vector2 var1) {
      var1.set(this.m_deferredMotion.m_counterRotatedMovementDiff);
      return var1;
   }

   public float getDeferredRotationDiff() {
      return this.m_deferredMotion.m_deferredRotationDiff;
   }

   public float getClampedBlendDelta() {
      return PZMath.clamp(this.BlendDelta, 0.0F, 1.0F);
   }

   public void addListener(IAnimListener var1) {
      this.listeners.add(var1);
   }

   public void startClip(AnimationClip var1, boolean var2, float var3) {
      if (var1 == null) {
         throw new NullPointerException("Supplied clip is null.");
      } else {
         this.reset();
         this.IsPlaying = true;
         this.bLooping = var2;
         this.CurrentClip = var1;
         this.m_isRagdollFirstFrame = this.isRagdoll();
         this.m_ragdollMaxTime = var3;
      }
   }

   public AnimationTrack reset() {
      return this.resetInternal();
   }

   public void setBoneWeights(List<AnimBoneWeight> var1) {
      this.m_boneWeightBindings = PooledAnimBoneWeightArray.toArray(var1);
      this.m_boneWeights = null;
   }

   public void initBoneWeights(SkinningData var1) {
      if (!this.hasBoneMask()) {
         if (this.m_boneWeightBindings != null) {
            if (this.m_boneWeightBindings.isEmpty()) {
               this.m_boneWeights = PooledFloatArrayObject.alloc(0);
            } else {
               this.m_boneWeights = PooledFloatArrayObject.alloc(var1.numBones());
               PZArrayUtil.arraySet(this.m_boneWeights.array(), 0.0F);

               for(int var2 = 0; var2 < this.m_boneWeightBindings.length(); ++var2) {
                  AnimBoneWeight var3 = (AnimBoneWeight)this.m_boneWeightBindings.get(var2);
                  this.initWeightBinding(var1, var3);
               }

            }
         }
      }
   }

   protected void initWeightBinding(SkinningData var1, AnimBoneWeight var2) {
      if (var2 != null && !StringUtils.isNullOrEmpty(var2.boneName)) {
         String var3 = var2.boneName;
         SkinningBone var4 = var1.getBone(var3);
         if (var4 == null) {
            DebugLog.Animation.error("Bone not found: %s", var3);
         } else {
            float var5 = var2.weight;
            this.assignBoneWeight(var5, var4.Index);
            if (var2.includeDescendants) {
               Objects.requireNonNull(var4);
               Lambda.forEach(var4::forEachDescendant, this, var5, (var0, var1x, var2x) -> {
                  var1x.assignBoneWeight(var2x, var0.Index);
               });
            }

         }
      }
   }

   private void assignBoneWeight(float var1, int var2) {
      if (!this.hasBoneMask()) {
         throw new NullPointerException("Bone weights array not initialized.");
      } else {
         float var3 = this.m_boneWeights.get(var2);
         this.m_boneWeights.set(var2, Math.max(var1, var3));
      }
   }

   public float getBoneWeight(int var1) {
      if (!this.hasBoneMask()) {
         return 1.0F;
      } else {
         return DebugOptions.instance.Character.Debug.Animate.NoBoneMasks.getValue() ? 1.0F : PZArrayUtil.getOrDefault(this.m_boneWeights.array(), var1, 0.0F);
      }
   }

   public float getDeferredBoneWeight() {
      return this.m_deferredBone == null ? 0.0F : this.getBoneWeight(this.m_deferredBone.Index);
   }

   public void setLayerIdx(int var1) {
      this.m_layerIdx = var1;
   }

   public int getLayerIdx() {
      return this.m_animLayer != null ? this.m_animLayer.getDepth() : 0;
   }

   public boolean hasBoneMask() {
      return this.m_boneWeights != null;
   }

   public boolean isLooping() {
      return this.bLooping;
   }

   public void setDeferredBone(SkinningBone var1, BoneAxis var2) {
      this.m_deferredBone = var1;
      this.m_deferredBoneAxis = var2;
   }

   public void setUseDeferredMovement(boolean var1) {
      this.m_useDeferredMovement = var1;
   }

   public boolean getUseDeferredMovement() {
      return this.m_useDeferredMovement;
   }

   public void setUseDeferredRotation(boolean var1) {
      this.m_useDeferredRotation = var1;
   }

   public boolean getUseDeferredRotation() {
      return this.m_useDeferredRotation;
   }

   public void setDeferredRotationScale(float var1) {
      this.m_deferredRotationScale = var1;
   }

   public float getDeferredRotationScale() {
      return this.m_deferredRotationScale;
   }

   public boolean isFinished() {
      return !this.bLooping && this.getDuration() > 0.0F && this.m_currentTimeValue >= this.getDuration();
   }

   public float getCurrentTimeValue() {
      return this.m_currentTimeValue;
   }

   public void setCurrentTimeValue(float var1) {
      this.m_currentTimeValue = var1;
   }

   public float getPreviousTimeValue() {
      return this.m_previousTimeValue;
   }

   public void setPreviousTimeValue(float var1) {
      this.m_previousTimeValue = var1;
   }

   public void rewind(float var1) {
      this.advance(-var1);
   }

   public void scaledRewind(float var1) {
      this.scaledAdvance(-var1);
   }

   public void scaledAdvance(float var1) {
      this.advance(var1 * this.SpeedDelta);
   }

   public void advance(float var1) {
      this.m_currentTimeValue = PZMath.wrap(this.m_currentTimeValue + var1, 0.0F, this.getDuration());
      this.m_previousTimeValue = PZMath.wrap(this.m_previousTimeValue + var1, 0.0F, this.getDuration());
   }

   public void advanceFraction(float var1) {
      this.advance(this.getDuration() * var1);
   }

   public void moveCurrentTimeValueTo(float var1) {
      float var2 = var1 - this.m_currentTimeValue;
      this.advance(var2);
   }

   public void moveCurrentTimeValueToFraction(float var1) {
      float var2 = this.getDuration() * var1;
      this.moveCurrentTimeValueTo(var2);
   }

   public float getCurrentTimeFraction() {
      return this.hasClip() ? this.m_currentTimeValue / this.getDuration() : 0.0F;
   }

   public boolean hasClip() {
      return this.CurrentClip != null;
   }

   public AnimationClip getClip() {
      return this.CurrentClip;
   }

   public int getPriority() {
      return this.priority;
   }

   public boolean isGrappler() {
      return this.isGrappler;
   }

   public static AnimationTrack createClone(AnimationTrack var0, Supplier<AnimationTrack> var1) {
      AnimationTrack var2 = (AnimationTrack)var1.get();
      var2.IsPlaying = var0.IsPlaying;
      var2.CurrentClip = var0.CurrentClip;
      var2.priority = var0.priority;
      var2.m_isRagdollFirstFrame = var0.m_isRagdollFirstFrame;
      var2.m_currentTimeValue = var0.m_currentTimeValue;
      var2.m_previousTimeValue = var0.m_previousTimeValue;
      var2.SyncTrackingEnabled = var0.SyncTrackingEnabled;
      var2.m_IKAimingLeftArm = var0.m_IKAimingLeftArm;
      var2.m_IKAimingRightArm = var0.m_IKAimingRightArm;
      var2.reverse = var0.reverse;
      var2.bLooping = var0.bLooping;
      var2.SpeedDelta = var0.SpeedDelta;
      var2.BlendDelta = var0.BlendDelta;
      var2.blendFieldWeight = var0.blendFieldWeight;
      var2.name = var0.name;
      var2.earlyBlendOutTime = var0.earlyBlendOutTime;
      var2.triggerOnNonLoopedAnimFadeOutEvent = var0.triggerOnNonLoopedAnimFadeOutEvent;
      var2.setAnimLayer(var0.m_animLayer);
      var2.m_boneWeightBindings = PooledAnimBoneWeightArray.toArray(var0.m_boneWeightBindings);
      var2.m_boneWeights = PooledFloatArrayObject.toArray(var0.m_boneWeights);
      var2.m_deferredBone = var0.m_deferredBone;
      var2.m_deferredBoneAxis = var0.m_deferredBoneAxis;
      var2.m_useDeferredMovement = var0.m_useDeferredMovement;
      var2.m_useDeferredRotation = var0.m_useDeferredRotation;
      var2.m_deferredRotationScale = var0.m_deferredRotationScale;
      var2.matchingGrappledAnimNode = var0.matchingGrappledAnimNode;
      var2.isGrappler = var0.isGrappler();
      return var2;
   }

   public String getMatchingGrappledAnimNode() {
      return this.matchingGrappledAnimNode;
   }

   public void setMatchingGrappledAnimNode(String var1) {
      this.matchingGrappledAnimNode = var1;
      this.isGrappler = !StringUtils.isNullOrWhitespace(this.matchingGrappledAnimNode);
   }

   public void setAnimLayer(AnimLayer var1) {
      if (this.m_animLayer != var1) {
         if (this.m_animLayer != null) {
            this.removeListener(this.m_animLayer);
         }

         this.m_animLayer = var1;
         if (this.m_animLayer != null) {
            this.addListener(var1);
         }

      }
   }

   public boolean isRagdollFirstFrame() {
      return this.m_isRagdollFirstFrame;
   }

   public void initRagdollTransform(int var1, Vector3f var2, Quaternion var3, Vector3f var4) {
      if (!this.isRagdoll()) {
         DebugLog.Animation.warn("This track is not a ragdoll track: %s", this.getName());
      } else {
         Keyframe[] var5 = this.CurrentClip.getBoneFramesAt(var1);
         Keyframe[] var6 = var5;
         int var7 = var5.length;

         for(int var8 = 0; var8 < var7; ++var8) {
            Keyframe var9 = var6[var8];
            var9.set(var2, var3, var4);
         }

      }
   }

   public boolean isRagdoll() {
      return this.CurrentClip != null && this.CurrentClip.IsRagdoll;
   }

   private void initRagdollTransform(int var1, TwistableBoneTransform var2) {
      Vector3f var3 = new Vector3f();
      Quaternion var4 = new Quaternion();
      Vector3f var5 = new Vector3f();
      var2.getPRS(var3, var4, var5);
      this.initRagdollTransform(var1, var3, var4, var5);
   }

   private void initRagdollTransform(int var1, Matrix4f var2) {
      Vector3f var3 = new Vector3f();
      Quaternion var4 = new Quaternion();
      Vector3f var5 = new Vector3f();
      HelperFunctions.getPosition(var2, var3);
      HelperFunctions.getRotation(var2, var4);
      var5.set(1.0F, 1.0F, 1.0F);
      this.initRagdollTransform(var1, var3, var4, var5);
   }

   public void initRagdollTransforms(List<Matrix4f> var1) {
      for(int var2 = 0; var2 < var1.size(); ++var2) {
         this.initRagdollTransform(var2, (Matrix4f)var1.get(var2));
      }

      this.m_isRagdollFirstFrame = false;
      this.updatePose();
   }

   public void initRagdollTransforms(TwistableBoneTransform[] var1) {
      for(int var2 = 0; var2 < var1.length; ++var2) {
         this.initRagdollTransform(var2, var1[var2]);
      }

      this.m_isRagdollFirstFrame = false;
      this.updatePose();
   }

   public String getName() {
      return this.CurrentClip != null ? this.CurrentClip.Name : "!Empty!";
   }

   public boolean isIKAimingLeftArm() {
      return this.m_IKAimingLeftArm;
   }

   public void setIKAimingLeftArm(boolean var1) {
      this.m_IKAimingLeftArm = var1;
   }

   public boolean isIKAimingRightArm() {
      return this.m_IKAimingRightArm;
   }

   public void setIKAimingRightArm(boolean var1) {
      this.m_IKAimingRightArm = var1;
   }

   private static class KeyframeSpan {
      Keyframe from;
      Keyframe to;
      int fromIdx = -1;
      int toIdx = -1;

      private KeyframeSpan() {
      }

      void clear() {
         this.from = null;
         this.to = null;
         this.fromIdx = -1;
         this.toIdx = -1;
      }

      Keyframe lerp(float var1, Keyframe var2) {
         var2.setIdentity();
         if (this.from == null && this.to == null) {
            return var2;
         } else if (this.to == null) {
            var2.set(this.from);
            return var2;
         } else if (this.from == null) {
            var2.set(this.to);
            return var2;
         } else if (this.from == this.to) {
            var2.set(this.to);
            return var2;
         } else {
            return Keyframe.lerp(this.from, this.to, var1, var2);
         }
      }

      void lerp(float var1, Vector3f var2, Quaternion var3, Vector3f var4) {
         if (this.from == null && this.to == null) {
            Keyframe.setIdentity(var2, var3, var4);
         } else if (this.to == null) {
            this.from.get(var2, var3, var4);
         } else if (this.from == null) {
            this.to.get(var2, var3, var4);
         } else if (this.from == this.to) {
            this.to.get(var2, var3, var4);
         } else if (!PerformanceSettings.InterpolateAnims) {
            this.to.get(var2, var3, var4);
         } else {
            Keyframe.lerp(this.from, this.to, var1, var2, var3, var4);
         }
      }

      boolean isSpan() {
         return this.from != null && this.to != null;
      }

      boolean isPost() {
         return (this.from == null || this.to == null) && this.from != this.to;
      }

      boolean isEmpty() {
         return this.from == null && this.to == null;
      }

      boolean containsTime(float var1) {
         return this.isSpan() && this.from.Time <= var1 && var1 <= this.to.Time;
      }

      public boolean isBone(int var1) {
         return this.from != null && this.from.Bone == var1 || this.to != null && this.to.Bone == var1;
      }
   }

   private static class DeferredMotionData {
      float m_targetDeferredRotation;
      float m_prevDeferredRotation;
      final Quaternion m_targetDeferredRotationQ = new Quaternion();
      final Vector3f m_targetDeferredPosition = new Vector3f();
      final Vector2 m_prevDeferredMovement = new Vector2();
      final Vector2 m_targetDeferredMovement = new Vector2();
      float m_deferredRotationDiff;
      final Vector2 m_deferredMovementDiff = new Vector2();
      final Vector2 m_counterRotatedMovementDiff = new Vector2();

      private DeferredMotionData() {
      }

      public void reset() {
         this.m_deferredRotationDiff = 0.0F;
         this.m_targetDeferredRotation = 0.0F;
         this.m_prevDeferredRotation = 0.0F;
         this.m_targetDeferredRotationQ.setIdentity();
         this.m_targetDeferredMovement.set(0.0F, 0.0F);
         this.m_targetDeferredPosition.set(0.0F, 0.0F, 0.0F);
         this.m_prevDeferredMovement.set(0.0F, 0.0F);
         this.m_deferredMovementDiff.set(0.0F, 0.0F);
         this.m_counterRotatedMovementDiff.set(0.0F, 0.0F);
      }
   }

   private static class s_performance {
      static final PerformanceProfileProbe tickCurrentTime = new PerformanceProfileProbe("AnimationTrack.tickCurrentTime");
      static final PerformanceProfileProbe updateKeyframes = new PerformanceProfileProbe("AnimationTrack.updateKeyframes");
      static final PerformanceProfileProbe updateDeferredValues = new PerformanceProfileProbe("AnimationTrack.updateDeferredValues");
      static final PerformanceProfileProbe updatePose = new PerformanceProfileProbe("AnimationTrack.updatePose");

      private s_performance() {
      }
   }

   private static class L_updateDeferredValues {
      static final Keyframe keyFrame = new Keyframe(new Vector3f(), new Quaternion(), new Vector3f(1.0F, 1.0F, 1.0F));
      static final Keyframe prevKeyFrame = new Keyframe(new Vector3f(), new Quaternion(), new Vector3f(1.0F, 1.0F, 1.0F));
      static final Vector2 crDiff = new Vector2();
      static final Vector2 diff = new Vector2();

      private L_updateDeferredValues() {
      }
   }
}
