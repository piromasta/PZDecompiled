package zombie.core.skinnedmodel.advancedanimation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import zombie.characters.IsoGameCharacter;
import zombie.core.math.PZMath;
import zombie.core.profiling.PerformanceProbes;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.IGrappleable;
import zombie.core.skinnedmodel.animation.AnimationMultiTrack;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.core.skinnedmodel.animation.IAnimListener;
import zombie.core.skinnedmodel.animation.StartAnimTrackParameters;
import zombie.core.skinnedmodel.animation.debug.AnimationPlayerRecorder;
import zombie.core.utils.TransitionNodeProxy;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.iso.Vector2;
import zombie.iso.Vector3;
import zombie.util.IPooledObject;
import zombie.util.StringUtils;
import zombie.util.lambda.Invokers;

public final class AnimLayer implements IAnimListener {
   private final AnimLayer m_parentLayer;
   private final IAnimatable m_Character;
   private AnimState m_State;
   private AnimNode m_CurrentNode;
   private AdvancedAnimator m_parentAnimator;
   private LiveAnimNode m_currentSyncNode;
   private AnimationTrack m_currentSyncTrack;
   private final List<AnimNode> m_reusableAnimNodes;
   private final List<LiveAnimNode> m_liveAnimNodes;
   private boolean m_noAnimConditionsEventSent;
   private static final AnimEvent s_activeAnimLoopedEvent = new AnimEvent();
   private final PerformanceProbes.Invokable.Params1.IProbe updateInternal;
   private final Reusables m_reusables;
   private static final AnimEvent s_activeNonLoopedAnimFadeOutEvent;
   private static final AnimEvent s_activeAnimFinishingEvent;
   private static final AnimEvent s_activeNonLoopedAnimFinishedEvent;
   private static final AnimEvent s_noAnimConditionsPass;

   public AnimLayer(IAnimatable var1, AdvancedAnimator var2) {
      this((AnimLayer)null, var1, var2);
   }

   public AnimLayer(AnimLayer var1, IAnimatable var2, AdvancedAnimator var3) {
      this.m_State = null;
      this.m_CurrentNode = null;
      this.m_reusableAnimNodes = new ArrayList();
      this.m_liveAnimNodes = new ArrayList();
      this.m_noAnimConditionsEventSent = false;
      this.updateInternal = PerformanceProbes.create("AnimLayer.Update", this, (Invokers.Params2.ICallback)(AnimLayer::updateInternal));
      this.m_reusables = new Reusables();
      this.m_parentLayer = var1;
      this.m_Character = var2;
      this.m_parentAnimator = var3;
   }

   public String getCurrentStateName() {
      return this.m_State == null ? null : this.m_State.m_Name;
   }

   public boolean hasState() {
      return this.m_State != null;
   }

   public boolean isStateless() {
      return this.m_State == null;
   }

   public boolean isSubLayer() {
      return this.m_parentLayer != null;
   }

   public boolean isCurrentState(String var1) {
      return this.m_State != null && StringUtils.equals(this.m_State.m_Name, var1);
   }

   public AnimationMultiTrack getAnimationTrack() {
      if (this.m_Character == null) {
         return null;
      } else {
         AnimationPlayer var1 = this.m_Character.getAnimationPlayer();
         return var1 == null ? null : var1.getMultiTrack();
      }
   }

   public IAnimationVariableSource getVariableSource() {
      return this.m_Character;
   }

   public LiveAnimNode getCurrentSyncNode() {
      return this.m_currentSyncNode;
   }

   public AnimationTrack getCurrentSyncTrack() {
      return this.m_currentSyncTrack;
   }

   public void onAnimStarted(AnimationTrack var1) {
   }

   public void onLoopedAnim(AnimationTrack var1) {
      this.invokeAnimEvent(var1, s_activeAnimLoopedEvent, false);
   }

   public void onNonLoopedAnimFadeOut(AnimationTrack var1) {
      this.invokeAnimEvent(var1, s_activeAnimFinishingEvent, true);
      this.invokeAnimEvent(var1, s_activeNonLoopedAnimFadeOutEvent, true);
   }

   public void onNonLoopedAnimFinished(AnimationTrack var1) {
      this.invokeAnimEvent(var1, s_activeAnimFinishingEvent, false);
      this.invokeAnimEvent(var1, s_activeNonLoopedAnimFinishedEvent, true);
   }

   public void onTrackDestroyed(AnimationTrack var1) {
   }

   public void onNoAnimConditionsPass() {
      if (!this.m_noAnimConditionsEventSent) {
         this.invokeAnimEvent((LiveAnimNode)null, (AnimationTrack)null, s_noAnimConditionsPass);
         this.m_noAnimConditionsEventSent = true;
      }

   }

   private void invokeAnimEvent(AnimationTrack var1, AnimEvent var2, boolean var3) {
      if (this.m_parentAnimator != null) {
         int var4 = 0;

         for(int var5 = this.m_liveAnimNodes.size(); var4 < var5; ++var4) {
            LiveAnimNode var6 = (LiveAnimNode)this.m_liveAnimNodes.get(var4);
            if ((!var6.m_TransitioningOut || var3) && var6.getSourceNode().m_State == this.m_State && var6.containsMainAnimationTrack(var1)) {
               this.invokeAnimEvent(var6, var1, var2);
               break;
            }
         }

      }
   }

   protected void invokeAnimEvent(LiveAnimNode var1, AnimationTrack var2, AnimEvent var3) {
      if (this.m_parentAnimator == null) {
         DebugLog.Animation.warn("invokeAnimEvent. No listener. %s", var3.toDetailsString());
      } else {
         if (this.isRecording()) {
            this.logAnimEvent(var2, var3);
         }

         if (var3 instanceof AnimEventFlagWhileAlive) {
            AnimEventFlagWhileAlive var4 = (AnimEventFlagWhileAlive)var3;
            if (var1.incrementWhileAliveFlagOnce(var4.m_variableReference)) {
               this.m_parentAnimator.incrementWhileAliveFlag(var4.m_variableReference);
            }
         }

         this.m_parentAnimator.OnAnimEvent(this, var3);
      }
   }

   public void decrementWhileAliveFlags(LiveAnimNode var1) {
      ArrayList var2 = var1.getWhileAliveFlags();

      for(int var3 = 0; var3 < var2.size(); ++var3) {
         AnimationVariableReference var4 = (AnimationVariableReference)var2.get(var3);
         this.m_parentAnimator.decrementWhileAliveFlag(var4);
      }

      var2.clear();
   }

   public String GetDebugString() {
      String var1 = this.m_Character.getAdvancedAnimator().animSet.m_Name;
      if (this.m_State != null) {
         var1 = var1 + "/" + this.m_State.m_Name;
         if (this.m_CurrentNode != null) {
            var1 = var1 + "/" + this.m_CurrentNode.m_Name + ": " + this.m_CurrentNode.m_AnimName;
         }
      }

      String var2 = "State: " + var1;

      LiveAnimNode var4;
      for(Iterator var3 = this.m_liveAnimNodes.iterator(); var3.hasNext(); var2 = var2 + "\n  Node: " + var4.getSourceNode().m_Name) {
         var4 = (LiveAnimNode)var3.next();
      }

      AnimationMultiTrack var6 = this.getAnimationTrack();
      if (var6 != null) {
         var2 = var2 + "\n  AnimTrack:";
         Iterator var7 = var6.getTracks().iterator();

         while(var7.hasNext()) {
            AnimationTrack var5 = (AnimationTrack)var7.next();
            if (var5.m_animLayer == this) {
               var2 = var2 + "\n    Anim: " + var5.getName() + " Weight: " + var5.BlendDelta;
            }
         }
      }

      return var2;
   }

   public void Reset() {
      AnimationMultiTrack var1 = this.getAnimationTrack();
      IPooledObject.release(this.m_liveAnimNodes);
      this.m_noAnimConditionsEventSent = false;
      this.m_State = null;
   }

   public boolean TransitionTo(AnimState var1, boolean var2) {
      if (this.getLiveAnimNodes().isEmpty()) {
         DebugLog.AnimationDetailed.debugln("TransitionTo: No Live Nodes");
      }

      DebugLog.AnimationDetailed.debugln("TransitionTo: from Anime <%s> to State <%s>", this.getLiveAnimNodes().isEmpty() ? "NoAnim" : ((LiveAnimNode)this.getLiveAnimNodes().get(0)).getName(), var1 != null ? var1.m_Name : "NoState");
      AnimationMultiTrack var3 = this.getAnimationTrack();
      if (var3 == null) {
         if (this.m_Character == null) {
            DebugLog.General.error("AnimationTrack is null. Character is null.");
            this.m_State = null;
            return false;
         } else if (this.m_Character.getAnimationPlayer() == null) {
            DebugLog.General.error("AnimationTrack is null. Character ModelInstance.AnimPlayer is null.");
            this.m_State = null;
            return false;
         } else {
            DebugLog.General.error("AnimationTrack is null. Unknown reason.");
            return false;
         }
      } else if (var1 == this.m_State && !var2) {
         return true;
      } else {
         if (DebugOptions.instance.Animation.AnimLayer.LogStateChanges.getValue()) {
            String var4 = this.m_parentLayer == null ? "" : AnimState.getStateName(this.m_parentLayer.m_State) + " | ";
            String var5 = String.format("State: %s%s => %s", var4, AnimState.getStateName(this.m_State), AnimState.getStateName(var1));
            DebugLog.General.debugln(var5);
            if (this.m_Character instanceof IsoGameCharacter) {
               ((IsoGameCharacter)this.m_Character).setSayLine(var5);
            }
         }

         this.m_State = var1;
         this.m_noAnimConditionsEventSent = false;

         for(int var6 = 0; var6 < this.m_liveAnimNodes.size(); ++var6) {
            LiveAnimNode var7 = (LiveAnimNode)this.m_liveAnimNodes.get(var6);
            var7.m_TransitioningOut = true;
         }

         return true;
      }
   }

   public void UpdateLiveAnimNodes() {
      this.removeFadedOutNodes();
      this.updateNodeActiveFlags();
   }

   public void Update(float var1) {
      this.updateInternal.invoke(var1);
   }

   private void updateInternal(float var1) {
      LiveAnimNode var2 = this.getHighestLiveNode();
      this.m_currentSyncNode = var2;
      this.m_currentSyncTrack = null;
      if (var2 != null) {
         int var3 = 0;

         for(int var4 = this.m_liveAnimNodes.size(); var3 < var4; ++var3) {
            LiveAnimNode var5 = (LiveAnimNode)this.m_liveAnimNodes.get(var3);
            var5.update(var1);
         }

         IAnimatable var25 = this.m_Character;
         this.updateMaximumTwist(var25);
         boolean var26 = DebugOptions.instance.Animation.AnimLayer.AllowAnimNodeOverride.getValue() && var25.getVariableBoolean("dbgForceAnim") && var25.getVariableBoolean("dbgForceAnimScalars");
         String var27 = var26 ? var25.getVariableString("dbgForceAnimNodeName") : null;
         AnimationTrack var6 = this.findSyncTrack(var2);
         this.m_currentSyncTrack = var6;
         float var7 = var6 != null ? var6.getCurrentTimeFraction() : -1.0F;
         IGrappleable var8 = this.m_Character.getGrappleable();
         int var9 = 0;

         for(int var10 = this.m_liveAnimNodes.size(); var9 < var10; ++var9) {
            LiveAnimNode var11 = (LiveAnimNode)this.m_liveAnimNodes.get(var9);
            float var12 = 1.0F;
            int var13 = 0;

            for(int var14 = var11.getPlayingTrackCount(); var13 < var14; ++var13) {
               AnimationTrack var15 = var11.getPlayingTrackAt(var13);
               if (var15.IsPlaying) {
                  if (var6 != null && var15.SyncTrackingEnabled && var15.isLooping() && var15 != var6) {
                     var15.moveCurrentTimeValueToFraction(var7);
                  }

                  if (var15.IsPrimary) {
                     var12 = var15.getDuration();
                     var11.m_NodeAnimTime = var15.getCurrentTimeValue();
                  }
               }
            }

            this.updateInternalGrapple(var8, var11);
            float var19;
            if (this.m_parentAnimator != null && var11.getSourceNode().m_Events.size() > 0) {
               float var28 = var11.m_NodeAnimTime / var12;
               float var30 = var11.m_PrevNodeAnimTime / var12;
               List var32 = var11.getSourceNode().m_Events;
               int var16 = 0;

               for(int var17 = var32.size(); var16 < var17; ++var16) {
                  AnimEvent var18 = (AnimEvent)var32.get(var16);
                  if (var18.m_Time == AnimEvent.AnimEventTime.Percentage) {
                     var19 = var18.m_TimePc;
                     if (var30 < var19 && var19 <= var28) {
                        this.invokeAnimEvent(var11, (AnimationTrack)null, var18);
                     } else {
                        if (!var11.isLooped() && var28 < var19) {
                           break;
                        }

                        if (var11.isLooped() && var30 > var28) {
                           if (var30 < var19 && var19 <= var28 + 1.0F) {
                              this.invokeAnimEvent(var11, (AnimationTrack)null, var18);
                           } else if (var30 > var19 && var19 <= var28) {
                              this.invokeAnimEvent(var11, (AnimationTrack)null, var18);
                           }
                        }
                     }
                  }
               }
            }

            if (var11.getPlayingTrackCount() != 0) {
               boolean var29 = var26 && StringUtils.equalsIgnoreCase(var11.getSourceNode().m_Name, var27);
               String var31 = var29 ? "dbgForceScalar" : var11.getSourceNode().m_Scalar;
               String var33 = var29 ? "dbgForceScalar2" : var11.getSourceNode().m_Scalar2;
               float var34 = var11.getTransitionInWeight();
               var11.setTransitionInBlendDelta(var34);
               var34 = var11.getWeight();
               float var35 = var34;
               AnimationTrack var36 = var11.m_RunningRagdollTrack;
               if (var36 != null) {
                  var19 = PZMath.clamp(var25.getVariableFloat(var31, 1.0F), 0.0F, 1.0F);
                  var36.BlendDelta = var36.blendFieldWeight * var34 * var19;
                  var35 = var34 - var36.blendFieldWeight * var34;
               }

               if (var11.hasMainAnimationTracks()) {
                  float var20;
                  if (var11.m_isBlendField) {
                     var19 = var25.getVariableFloat(var31, 0.0F);
                     var20 = var25.getVariableFloat(var33, 0.0F);
                     this.applyBlendField(var11, var35, var19, var20);
                  } else {
                     var19 = PZMath.clamp(var25.getVariableFloat(var31, 1.0F), 0.0F, 1.0F);
                     var20 = var35 * var19;
                     int var21 = var11.getMainAnimationTracksCount();
                     float var22 = var20 / (float)var21;

                     for(int var23 = 0; var23 < var21; ++var23) {
                        AnimationTrack var24 = var11.getMainAnimationTrackAt(var23);
                        var24.BlendDelta = var22;
                     }
                  }
               }
            }
         }

         if (this.isRecording()) {
            this.logBlendWeights();
            this.logCurrentState();
         }

      }
   }

   private void updateInternalGrapple(IGrappleable var1, LiveAnimNode var2) {
      if (var1 != null) {
         if (var1.isGrappling() && var2.isGrappler()) {
            this.updateInternalWhileGrappling(var1, var2);
         }

         if (var1.isBeingGrappled()) {
            this.updateInternalWhileGrappled(var1, var2);
         }

      }
   }

   private void updateInternalWhileGrappling(IGrappleable var1, LiveAnimNode var2) {
      if (!var1.isGrappling()) {
         DebugLog.Grapple.warn("This Grappleable is not currently grappling: %s", var1);
      } else if (!var2.isGrappler()) {
         DebugLog.Grapple.warn("This Grappleable's sourceNode is not a grappler: %s, sourceNode: %s", var1, var2.getSourceNode());
      } else {
         String var3 = var2.getMatchingGrappledAnimNode();
         int var4 = 0;
         int var5 = 0;

         float var8;
         for(int var6 = var2.getPlayingTrackCount(); var5 < var6; ++var5) {
            AnimationTrack var7 = var2.getPlayingTrackAt(var5);
            if (var7.IsPlaying && var7.isGrappler()) {
               ++var4;
               if (var4 > 1) {
                  DebugLog.Grapple.warn("More than one AnimNode is grappling. The node '%s' is being overwritten by node '%s'.", var1.getSharedGrappleAnimNode(), var2.getName());
               }

               var8 = var7.getCurrentTrackTime();
               var1.setSharedGrappleAnimNode(var2.getName());
               var1.setSharedGrappleAnimTime(var8);
               IGrappleable var9 = var1.getGrapplingTarget();
               var9.setSharedGrappleAnimNode(var3);
               var9.setSharedGrappleAnimTime(var8);
               var1.setGrappleoffsetBehaviour(var2.getGrapplerOffsetBehaviour());
               var1.setGrapplePosOffsetForward(var2.getGrappleOffsetForward());
               var1.setGrappleRotOffsetYaw(var2.getGrappledOffsetYaw());
            }
         }

         float var18;
         float var20;
         switch (var1.getGrappleOffsetBehaviour()) {
            case Grappler:
               IGrappleable var17 = var1.getGrapplingTarget();
               var18 = var1.getGrapplePosOffsetForward();
               var20 = var1.getGrappleRotOffsetYaw();
               this.performOffsetOfGrappleable(var1, var17, var18, var20, true);
               break;
            case None_TweenInGrappler:
               Vector3 var14;
               if (var2.isTweeningInGrappleFinished()) {
                  var14 = var1.getTargetGrapplePos(this.m_reusables.grappledPos);
                  this.setGrappleablePosition(var1, var14, true);
               } else {
                  float var22;
                  if (!var2.isTweeningInGrapple()) {
                     var14 = this.m_reusables.currentPos;
                     var1.getPosition(var14);
                     var2.setGrappleTweenStartPos(var14);
                     var2.setTweeningInGrapple(true);
                     var2.setTweeningInGrappleFinished(false);
                     Vector3 var15 = this.m_reusables.grappledPos;
                     Vector2 var19 = this.m_reusables.animForwardDirection;
                     IGrappleable var21 = var1.getGrapplingTarget();
                     var22 = var1.getGrapplePosOffsetForward();
                     float var10 = var1.getGrappleRotOffsetYaw();
                     this.calculateOffsetsForGrappleable(var21, var22, var10, var19, var15);
                     var1.setTargetGrapplePos(var15);
                     var1.setTargetGrappleRotation(var19);
                  }

                  float var16 = var1.getSharedGrappleAnimTime();
                  var18 = var2.getGrappleTweenInTime();
                  var20 = PZMath.min(var16, var18);
                  var8 = var20 / var18;
                  var22 = PZMath.lerpFunc_EaseOutInQuad(var8);
                  Vector3 var23 = this.m_reusables.tweenedPos;
                  Vector3 var11 = var2.getGrappleTweenStartPos(this.m_reusables.grappleTweenStartPos);
                  Vector3 var12 = var1.getTargetGrapplePos(this.m_reusables.grappledPos);
                  PZMath.lerp(var23, var11, var12, var22);
                  Vector2 var24 = this.m_reusables.tweenedRot;
                  Vector2 var25 = var1.getAnimForwardDirection(this.m_reusables.animForwardDirection);
                  Vector2 var13 = var1.getTargetGrappleRotation(this.m_reusables.animTargetForwardDirection);
                  PZMath.lerp(var24, var25, var13, var22);
                  var24.normalize();
                  this.setGrappleablePosAndRotation(var1, var23, var24, true);
                  if (var16 >= var18) {
                     var2.setTweeningInGrappleFinished(true);
                  }
               }
               break;
            default:
               var1.setGrappleDeferredOffset(0.0F, 0.0F, 0.0F);
         }

      }
   }

   private void updateInternalWhileGrappled(IGrappleable var1, LiveAnimNode var2) {
      if (!var1.isBeingGrappled()) {
         DebugLog.Grapple.warn("This Grappleable is not being grappled: %s", var1);
      } else {
         String var3 = var1.getSharedGrappleAnimNode();
         float var4 = var1.getSharedGrappleAnimTime();
         if (var2.getName().equalsIgnoreCase(var3)) {
            int var5 = 0;

            for(int var6 = var2.getPlayingTrackCount(); var5 < var6; ++var5) {
               AnimationTrack var7 = var2.getPlayingTrackAt(var5);
               if (var7.IsPlaying && var7.IsPrimary) {
                  var7.moveCurrentTimeValueTo(var4);
                  var2.m_NodeAnimTime = var7.getCurrentTimeValue();
               }
            }

            float var10;
            float var11;
            switch (var1.getGrappleOffsetBehaviour()) {
               case Grappled_TweenOutToNone:
                  var10 = var1.getSharedGrappleAnimTime();
                  var11 = var2.getGrappleTweenInTime();
                  if (var10 <= var11) {
                     float var13 = var1.getGrapplePosOffsetForward();
                     float var8 = var1.getGrappleRotOffsetYaw();
                     IGrappleable var9 = var1.getGrappledBy();
                     this.performOffsetOfGrappleable(var1, var9, var13, var8, true);
                  } else {
                     var1.setGrappleDeferredOffset(0.0F, 0.0F, 0.0F);
                  }
                  break;
               case Grappled:
                  var10 = var1.getGrapplePosOffsetForward();
                  var11 = var1.getGrappleRotOffsetYaw();
                  IGrappleable var12 = var1.getGrappledBy();
                  this.performOffsetOfGrappleable(var1, var12, var10, var11, false);
                  break;
               default:
                  var1.setGrappleDeferredOffset(0.0F, 0.0F, 0.0F);
            }

         }
      }
   }

   private void performOffsetOfGrappleable(IGrappleable var1, IGrappleable var2, float var3, float var4, boolean var5) {
      Vector3 var6 = this.m_reusables.grappledPos;
      Vector2 var7 = this.m_reusables.animForwardDirection;
      this.calculateOffsetsForGrappleable(var2, var3, var4, var7, var6);
      this.setGrappleablePosAndRotation(var1, var6, var7, var5);
   }

   private void setGrappleablePosAndRotation(IGrappleable var1, Vector3 var2, Vector2 var3, boolean var4) {
      var1.setTargetAndCurrentDirection(var3);
      this.setGrappleablePosition(var1, var2, var4);
   }

   private void setGrappleablePosition(IGrappleable var1, Vector3 var2, boolean var3) {
      if (var3) {
         Vector3 var4 = this.m_reusables.currentPos;
         var1.getPosition(var4);
         float var5 = var2.x - var4.x;
         float var6 = var2.y - var4.y;
         float var7 = var2.z - var4.z;
         var1.setGrappleDeferredOffset(var5, var6, var7);
      } else {
         var1.setGrappleDeferredOffset(0.0F, 0.0F, 0.0F);
         var1.setPosition(var2.x, var2.y, var2.z);
      }

   }

   private void calculateOffsetsForGrappleable(IGrappleable var1, float var2, float var3, Vector2 var4, Vector3 var5) {
      Vector3 var6 = this.m_reusables.grappledByPos;
      var1.getAnimForwardDirection(var4);
      var4.rotate(0.017453292F * var3);
      var1.getPosition(var6);
      var5.x = var6.x + var4.x * var2;
      var5.y = var6.y + var4.y * var2;
      var5.z = var6.z;
   }

   private void updateMaximumTwist(IAnimationVariableSource var1) {
      IAnimationVariableSlot var2 = var1.getVariable("maxTwist");
      if (var2 != null) {
         float var3 = var2.getValueFloat();
         float var4 = 0.0F;
         float var5 = 1.0F;

         for(int var6 = this.m_liveAnimNodes.size() - 1; var6 >= 0; --var6) {
            LiveAnimNode var7 = (LiveAnimNode)this.m_liveAnimNodes.get(var6);
            float var8 = var7.getWeight();
            if (var5 <= 0.0F) {
               break;
            }

            float var9 = PZMath.clamp(var8, 0.0F, var5);
            var5 -= var9;
            float var10 = PZMath.clamp(var7.getSourceNode().m_maxTorsoTwist, 0.0F, 70.0F);
            var4 += var10 * var9;
         }

         if (var5 > 0.0F) {
            var4 += var3 * var5;
         }

         var2.setValue(var4);
      }
   }

   public void updateNodeActiveFlags() {
      for(int var1 = 0; var1 < this.m_liveAnimNodes.size(); ++var1) {
         LiveAnimNode var2 = (LiveAnimNode)this.m_liveAnimNodes.get(var1);
         var2.setActive(false);
      }

      AnimState var7 = this.m_State;
      if (var7 != null) {
         IAnimatable var8 = this.m_Character;
         if (!var8.getVariableBoolean("AnimLocked")) {
            List var3 = var7.getAnimNodes(var8, this.m_reusableAnimNodes);
            if (var3.isEmpty()) {
               this.onNoAnimConditionsPass();
            } else {
               this.m_noAnimConditionsEventSent = false;
            }

            int var4 = 0;

            for(int var5 = var3.size(); var4 < var5; ++var4) {
               AnimNode var6 = (AnimNode)var3.get(var4);
               this.getOrCreateLiveNode(var6);
            }

         }
      }
   }

   public void FindTransitioningLiveAnimNode(TransitionNodeProxy var1, boolean var2) {
      for(int var3 = 0; var3 < this.m_liveAnimNodes.size(); ++var3) {
         LiveAnimNode var4 = (LiveAnimNode)this.m_liveAnimNodes.get(var3);
         boolean var5;
         int var6;
         TransitionNodeProxy.NodeLayerPair var7;
         if (var4.isNew() && var4.wasActivated()) {
            var5 = false;

            for(var6 = 0; var6 < var1.m_allNewNodes.size(); ++var6) {
               var7 = (TransitionNodeProxy.NodeLayerPair)var1.m_allNewNodes.get(var6);
               if (var7.liveAnimNode.getSourceNode() == var4.getSourceNode()) {
                  var5 = true;
                  break;
               }
            }

            if (!var5) {
               DebugLog.AnimationDetailed.debugln("** NEW ** newNode: <%s>; Layer: <%s>", var4.getName(), var2 ? "RootLayer" : "NoneRootLayer");
               var1.m_allNewNodes.add(new TransitionNodeProxy.NodeLayerPair(var4, this));
            } else {
               DebugLog.AnimationDetailed.debugln("** SKIPPED ** newNode: <%s>; Layer: <%s>", var4.getName(), var2 ? "RootLayer" : "NoneRootLayer");
            }
         } else if (var4.wasDeactivated() && var2 || !var2 && var4.m_TransitioningOut) {
            var5 = false;

            for(var6 = 0; var6 < var1.m_allOutgoingNodes.size(); ++var6) {
               var7 = (TransitionNodeProxy.NodeLayerPair)var1.m_allOutgoingNodes.get(var6);
               if (var7.liveAnimNode.getSourceNode() == var4.getSourceNode()) {
                  var5 = true;
                  break;
               }
            }

            if (!var5) {
               DebugLog.AnimationDetailed.debugln("** NEW ** oldNode: <%s>; Layer: <%s>", var4.getName(), var2 ? "RootLayer" : "NoneRootLayer");
               var1.m_allOutgoingNodes.add(new TransitionNodeProxy.NodeLayerPair(var4, this));
            } else {
               DebugLog.AnimationDetailed.debugln("** SKIPPED ** oldNode: <%s>; Layer: <%s>", var4.getName(), var2 ? "RootLayer" : "NoneRootLayer");
            }
         }
      }

   }

   public AnimationTrack startTransitionAnimation(TransitionNodeProxy.TransitionNodeProxyData var1) {
      if (StringUtils.isNullOrWhitespace(var1.m_transitionOut.m_AnimName)) {
         if (DebugLog.isEnabled(DebugType.Animation)) {
            DebugLog.Animation.println("  TransitionTo found: %s -> <no anim> -> %s", var1.m_OldAnimNode.getName(), var1.m_NewAnimNode.getName());
         }

         return null;
      } else {
         float var2 = var1.m_transitionOut.m_speedScale;
         if (var2 == 1.0F / 0.0F) {
            var2 = var1.m_NewAnimNode.getSpeedScale(this.m_Character);
         }

         StartAnimTrackParameters var3 = StartAnimTrackParameters.alloc();
         var3.animName = var1.m_transitionOut.m_AnimName;
         var3.subLayerBoneWeights = var1.m_OldAnimNode.getSubStateBoneWeights();
         var3.speedScale = var2;
         var3.deferredBoneName = var1.getDeferredBoneName();
         var3.deferredBoneAxis = var1.getDeferredBoneAxis();
         var3.useDeferredRotation = var1.getUseDeferredRotation();
         var3.useDeferredMovement = var1.getUseDeferredMovement();
         var3.deferredRotationScale = var1.getDeferredRotationScale();
         var3.priority = var1.m_OldAnimNode.getPriority();
         AnimationTrack var4 = this.startTrackGeneric(var3);
         var3.release();
         if (var4 == null) {
            if (DebugLog.isEnabled(DebugType.Animation)) {
               DebugLog.Animation.println("  TransitionTo failed to play transition track: %s -> %s -> %s", var1.m_OldAnimNode.getName(), var1.m_transitionOut.m_AnimName, var1.m_NewAnimNode.getName());
            }

            return null;
         } else {
            if (DebugLog.isEnabled(DebugType.Animation)) {
               DebugLog.Animation.println("  TransitionTo found: %s -> %s -> %s", var1.m_OldAnimNode.getName(), var1.m_transitionOut.m_AnimName, var1.m_NewAnimNode.getName());
            }

            return var4;
         }
      }
   }

   public void removeFadedOutNodes() {
      for(int var1 = this.m_liveAnimNodes.size() - 1; var1 >= 0; --var1) {
         LiveAnimNode var2 = (LiveAnimNode)this.m_liveAnimNodes.get(var1);
         if (!var2.isActive() && (!var2.isTransitioningIn() || !(var2.getTransitionInWeight() > 0.01F)) && !(var2.getWeight() > 0.01F)) {
            this.removeLiveNodeAt(var1);
         }
      }

   }

   public void render() {
      IAnimatable var1 = this.m_Character;
      boolean var2 = DebugOptions.instance.Animation.AnimLayer.AllowAnimNodeOverride.getValue() && var1.getVariableBoolean("dbgForceAnim") && var1.getVariableBoolean("dbgForceAnimScalars");
      String var3 = var2 ? var1.getVariableString("dbgForceAnimNodeName") : null;
      int var4 = 0;

      for(int var5 = this.m_liveAnimNodes.size(); var4 < var5; ++var4) {
         LiveAnimNode var6 = (LiveAnimNode)this.m_liveAnimNodes.get(var4);
         if (var6.getMainAnimationTracksCount() > 1) {
            boolean var7 = var2 && StringUtils.equalsIgnoreCase(var6.getSourceNode().m_Name, var3);
            String var8 = var7 ? "dbgForceScalar" : var6.getSourceNode().m_Scalar;
            String var9 = var7 ? "dbgForceScalar2" : var6.getSourceNode().m_Scalar2;
            float var10 = var1.getVariableFloat(var8, 0.0F);
            float var11 = var1.getVariableFloat(var9, 0.0F);
            if (var6.isActive()) {
               var6.getSourceNode().m_picker.render(var10, var11);
            }
         }
      }

   }

   private void logBlendWeights() {
      AnimationPlayerRecorder var1 = this.m_Character.getAnimationPlayer().getRecorder();
      int var2 = 0;

      for(int var3 = this.m_liveAnimNodes.size(); var2 < var3; ++var2) {
         LiveAnimNode var4 = (LiveAnimNode)this.m_liveAnimNodes.get(var2);
         var1.logAnimNode(var4);
      }

   }

   private void logCurrentState() {
      AnimationPlayerRecorder var1 = this.m_Character.getAnimationPlayer().getRecorder();
      var1.logAnimState(this.m_State);
   }

   private void logAnimEvent(AnimationTrack var1, AnimEvent var2) {
      AnimationPlayerRecorder var3 = this.m_Character.getAnimationPlayer().getRecorder();
      var3.logAnimEvent(var1, var2);
   }

   private void removeLiveNodeAt(int var1) {
      synchronized(this.m_liveAnimNodes) {
         LiveAnimNode var3 = (LiveAnimNode)this.m_liveAnimNodes.get(var1);
         DebugLog.AnimationDetailed.debugln("RemoveLiveNode: %s", var3.getName());
         this.m_liveAnimNodes.remove(var1);
         var3.release();
      }
   }

   private void applyBlendField(LiveAnimNode var1, float var2, float var3, float var4) {
      if (var1.isActive()) {
         AnimNode var5 = var1.getSourceNode();
         Anim2DBlendPicker var6 = var5.m_picker;
         Anim2DBlendPicker.PickResults var7 = var6.Pick(var3, var4);
         Anim2DBlend var8 = var7.node1;
         Anim2DBlend var9 = var7.node2;
         Anim2DBlend var10 = var7.node3;
         if (Float.isNaN(var7.scale1)) {
            var7.scale1 = 0.5F;
         }

         if (Float.isNaN(var7.scale2)) {
            var7.scale2 = 0.5F;
         }

         if (Float.isNaN(var7.scale3)) {
            var7.scale3 = 0.5F;
         }

         float var11 = var7.scale1;
         float var12 = var7.scale2;
         float var13 = var7.scale3;

         for(int var14 = 0; var14 < var1.getMainAnimationTracksCount(); ++var14) {
            Anim2DBlend var15 = (Anim2DBlend)var5.m_2DBlends.get(var14);
            AnimationTrack var16 = var1.getMainAnimationTrackAt(var14);
            if (var15 == var8) {
               var16.blendFieldWeight = AnimationPlayer.lerpBlendWeight(var16.blendFieldWeight, var11, 0.15F);
            } else if (var15 == var9) {
               var16.blendFieldWeight = AnimationPlayer.lerpBlendWeight(var16.blendFieldWeight, var12, 0.15F);
            } else if (var15 == var10) {
               var16.blendFieldWeight = AnimationPlayer.lerpBlendWeight(var16.blendFieldWeight, var13, 0.15F);
            } else {
               var16.blendFieldWeight = AnimationPlayer.lerpBlendWeight(var16.blendFieldWeight, 0.0F, 0.15F);
            }

            if (var16.blendFieldWeight < 1.0E-4F) {
               var16.blendFieldWeight = 0.0F;
            }

            var16.blendFieldWeight = PZMath.clamp(var16.blendFieldWeight, 0.0F, 1.0F);
         }
      }

      for(int var17 = 0; var17 < var1.getMainAnimationTracksCount(); ++var17) {
         AnimationTrack var18 = var1.getMainAnimationTrackAt(var17);
         var18.BlendDelta = var18.blendFieldWeight * var2;
      }

   }

   private void getOrCreateLiveNode(AnimNode var1) {
      LiveAnimNode var2 = this.findLiveNode(var1);
      if (var2 != null) {
         var2.setActive(true);
      } else {
         var2 = LiveAnimNode.alloc(this, var1, this.getDepth());
         this.startLiveNodeTracks(var2);
         var2.setActive(true);
         this.m_liveAnimNodes.add(var2);
      }
   }

   private LiveAnimNode findLiveNode(AnimNode var1) {
      LiveAnimNode var2 = null;
      int var3 = 0;

      for(int var4 = this.m_liveAnimNodes.size(); var3 < var4; ++var3) {
         LiveAnimNode var5 = (LiveAnimNode)this.m_liveAnimNodes.get(var3);
         if (!var5.m_TransitioningOut) {
            if (var5.getSourceNode() == var1) {
               var2 = var5;
               break;
            }

            if (var5.getSourceNode().m_State == var1.m_State && var5.getSourceNode().m_Name.equals(var1.m_Name)) {
               var2 = var5;
               break;
            }
         }
      }

      return var2;
   }

   private void startLiveNodeTracks(LiveAnimNode var1) {
      AnimNode var2 = var1.getSourceNode();
      float var3 = var2.getSpeedScale(this.m_Character);
      float var4 = Rand.Next(0.0F, 1.0F);
      float var5 = var2.m_SpeedScaleRandomMultiplierMin;
      float var6 = var2.m_SpeedScaleRandomMultiplierMax;
      float var7 = PZMath.lerp(var5, var6, var4);
      float var8 = var3 * var7;
      float var9 = var2.m_chanceToRagdoll;
      boolean var10 = var2.isRagdoll();
      boolean var11 = var10 && Rand.NextBoolFromChance(var9);
      float var12 = PZMath.max(var2.m_ragdollStartTimeMin, 0.0F);
      float var13 = PZMath.max(var2.m_ragdollStartTimeMax, var12);
      float var14 = Rand.Next(var12, var13);
      boolean var15 = var11 && PZMath.equal(var14, 0.0F, 0.01F);
      boolean var16 = !var15 && !var2.m_2DBlends.isEmpty();
      var1.m_isBlendField = var16;
      if (var11) {
         this.startRagdollTrack(var1, var8, var14);
      }

      if (!var15) {
         if (!var16) {
            this.startPrimaryAnimationTrack(var1, var8);
         } else {
            int var17 = 0;

            for(int var18 = var2.m_2DBlends.size(); var17 < var18; ++var17) {
               Anim2DBlend var19 = (Anim2DBlend)var2.m_2DBlends.get(var17);
               String var20 = var19.m_AnimName;
               if (StringUtils.equalsIgnoreCase(var20, var2.m_AnimName)) {
                  this.startPrimaryAnimationTrack(var1, var8);
               } else {
                  this.startBlendFieldTrack(var1, var20, var8);
               }
            }

         }
      }
   }

   private void startRagdollTrack(LiveAnimNode var1, float var2, float var3) {
      this.startTrackGeneric(var1, "Ragdoll_" + var1.getName(), false, var2, true, var3);
   }

   private void startBlendFieldTrack(LiveAnimNode var1, String var2, float var3) {
      this.startTrackGeneric(var1, var2, false, var3, false, -1.0F);
   }

   private void startPrimaryAnimationTrack(LiveAnimNode var1, float var2) {
      var1.selectRandomAnim();
      String var3 = var1.getAnimName();
      this.startTrackGeneric(var1, var3, true, var2, false, -1.0F);
   }

   private void startTrackGeneric(LiveAnimNode var1, String var2, boolean var3, float var4, boolean var5, float var6) {
      AnimNode var7 = var1.getSourceNode();
      StartAnimTrackParameters var8 = StartAnimTrackParameters.alloc();
      var8.animName = var2;
      var8.isPrimary = var3;
      var8.isRagdoll = var5;
      var8.ragdollStartTime = var6;
      var8.ragdollMaxTime = var7.getRagdollMaxTime();
      var8.subLayerBoneWeights = var7.m_SubStateBoneWeights;
      var8.syncTrackingEnabled = var7.m_SyncTrackingEnabled;
      var8.aimingIKLeftArm = var7.m_IKAimingLeftArm;
      var8.aimingIKRightArm = var7.m_IKAimingRightArm;
      var8.speedScale = var4;
      var8.initialWeight = var1.getWeight();
      var8.isLooped = var1.isLooped();
      var8.isReversed = var7.m_AnimReverse;
      var8.deferredBoneName = var7.getDeferredBoneName();
      var8.deferredBoneAxis = var7.getDeferredBoneAxis();
      var8.useDeferredMovement = var7.m_useDeferredMovement;
      var8.useDeferredRotation = var7.m_useDeferedRotation;
      var8.deferredRotationScale = var7.m_deferredRotationScale;
      var8.priority = var7.getPriority();
      var8.matchingGrappledAnimNode = var7.getMatchingGrappledAnimNode();
      AnimationTrack var9 = this.startTrackGeneric(var8);
      if (var9 != null) {
         var9.addListener(var1);
         var1.addMainTrack(var9);
      }

      var8.release();
   }

   private AnimationTrack startTrackGeneric(StartAnimTrackParameters var1) {
      AnimationPlayer var2 = this.m_Character.getAnimationPlayer();
      return !var2.isReady() ? null : var2.play(var1, this);
   }

   public int getDepth() {
      return this.m_parentLayer != null ? this.m_parentLayer.getDepth() + 1 : 0;
   }

   private LiveAnimNode getHighestLiveNode() {
      if (this.m_liveAnimNodes.isEmpty()) {
         return null;
      } else {
         LiveAnimNode var1 = (LiveAnimNode)this.m_liveAnimNodes.get(0);

         for(int var2 = this.m_liveAnimNodes.size() - 1; var2 >= 0; --var2) {
            LiveAnimNode var3 = (LiveAnimNode)this.m_liveAnimNodes.get(var2);
            if (var3.getWeight() > var1.getWeight()) {
               var1 = var3;
            }
         }

         return var1;
      }
   }

   private AnimationTrack findSyncTrack(LiveAnimNode var1) {
      AnimationTrack var2 = null;
      if (this.m_parentLayer != null) {
         var2 = this.m_parentLayer.getCurrentSyncTrack();
         if (var2 != null) {
            return var2;
         }
      }

      int var3 = 0;

      for(int var4 = var1.getPlayingTrackCount(); var3 < var4; ++var3) {
         AnimationTrack var5 = var1.getPlayingTrackAt(var3);
         if (var5.SyncTrackingEnabled && var5.hasClip() && (var2 == null || var5.BlendDelta > var2.BlendDelta)) {
            var2 = var5;
         }
      }

      return var2;
   }

   public String getDebugNodeName() {
      String var1 = this.m_Character.getAdvancedAnimator().animSet.m_Name;
      if (this.m_State != null) {
         var1 = var1 + "/" + this.m_State.m_Name;
         if (this.m_CurrentNode != null) {
            var1 = var1 + "/" + this.m_CurrentNode.m_Name + ": " + this.m_CurrentNode.m_AnimName;
         } else if (!this.m_liveAnimNodes.isEmpty()) {
            for(int var2 = 0; var2 < this.m_liveAnimNodes.size(); ++var2) {
               LiveAnimNode var3 = (LiveAnimNode)this.m_liveAnimNodes.get(var2);
               if (this.m_State.m_Nodes.contains(var3.getSourceNode())) {
                  var1 = var1 + "/" + var3.getName();
                  break;
               }
            }
         }
      }

      return var1;
   }

   public List<LiveAnimNode> getLiveAnimNodes() {
      return this.m_liveAnimNodes;
   }

   public boolean isRecording() {
      return this.m_Character.getAdvancedAnimator().isRecording();
   }

   static {
      s_activeAnimLoopedEvent.m_TimePc = 1.0F;
      s_activeAnimLoopedEvent.m_EventName = "ActiveAnimLooped";
      s_activeNonLoopedAnimFadeOutEvent = new AnimEvent();
      s_activeNonLoopedAnimFadeOutEvent.m_TimePc = 1.0F;
      s_activeNonLoopedAnimFadeOutEvent.m_EventName = "NonLoopedAnimFadeOut";
      s_activeAnimFinishingEvent = new AnimEvent();
      s_activeAnimFinishingEvent.m_Time = AnimEvent.AnimEventTime.End;
      s_activeAnimFinishingEvent.m_EventName = "ActiveAnimFinishing";
      s_activeNonLoopedAnimFinishedEvent = new AnimEvent();
      s_activeNonLoopedAnimFinishedEvent.m_Time = AnimEvent.AnimEventTime.End;
      s_activeNonLoopedAnimFinishedEvent.m_EventName = "ActiveAnimFinished";
      s_noAnimConditionsPass = new AnimEvent();
      s_noAnimConditionsPass.m_Time = AnimEvent.AnimEventTime.End;
      s_noAnimConditionsPass.m_EventName = "NoAnimConditionsPass";
   }

   private static final class Reusables {
      public final Vector2 animForwardDirection = new Vector2();
      public final Vector2 animTargetForwardDirection = new Vector2();
      public final Vector2 tweenedRot = new Vector2();
      public final Vector3 grappledPos = new Vector3();
      public final Vector3 grappledByPos = new Vector3();
      public final Vector3 currentPos = new Vector3();
      public final Vector3 grappleTweenStartPos = new Vector3();
      public final Vector3 tweenedPos = new Vector3();

      private Reusables() {
      }
   }
}
