package zombie.core.skinnedmodel.advancedanimation;

import java.util.ArrayList;
import java.util.List;
import zombie.core.math.PZMath;
import zombie.core.math.Vector3;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.animation.AnimationMultiTrack;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.core.skinnedmodel.animation.BoneAxis;
import zombie.core.skinnedmodel.animation.IAnimListener;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.util.IPooledObject;
import zombie.util.Lambda;
import zombie.util.Pool;
import zombie.util.PooledObject;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;
import zombie.util.list.PZEmptyIterable;

public class LiveAnimNode extends PooledObject implements IAnimListener {
   private AnimNode m_sourceNode;
   private AnimLayer m_animLayer;
   private boolean m_active;
   private boolean m_wasActive;
   boolean m_TransitioningOut;
   private float m_Weight;
   private float m_RawWeight;
   private boolean m_isNew;
   private int m_layerIdx;
   public boolean m_isBlendField = false;
   public AnimationTrack m_RunningRagdollTrack = null;
   private final TransitionIn m_transitionIn = new TransitionIn();
   private final List<AnimationTrack> m_AnimationTracks = new ArrayList();
   final List<AnimationTrack> m_RagdollTracks = new ArrayList();
   float m_NodeAnimTime;
   float m_PrevNodeAnimTime;
   private boolean m_blendingIn;
   private boolean m_blendingOut;
   private AnimTransition m_transitionOut;
   private boolean m_bTweeningInGrapple = false;
   private boolean m_bTweeningInGrappleFinished = false;
   private final Vector3 m_grappleTweenStartPos = new Vector3();
   private final ArrayList<AnimationVariableReference> m_whileAliveFlags = new ArrayList();
   private static final Pool<LiveAnimNode> s_pool = new Pool(LiveAnimNode::new);
   private String m_cachedRandomAnim = "";

   protected LiveAnimNode() {
   }

   public static LiveAnimNode alloc(AnimLayer var0, AnimNode var1, int var2) {
      synchronized(s_pool) {
         LiveAnimNode var3 = (LiveAnimNode)s_pool.alloc();
         var3.reset();
         var3.m_sourceNode = var1;
         var3.m_animLayer = var0;
         var3.m_layerIdx = var2;
         return var3;
      }
   }

   private void reset() {
      this.decrementWhileAliveFlags();
      this.m_sourceNode = null;
      this.m_animLayer = null;
      this.m_active = false;
      this.m_wasActive = false;
      this.m_TransitioningOut = false;
      this.m_Weight = 0.0F;
      this.m_RawWeight = 0.0F;
      this.m_isNew = true;
      this.m_layerIdx = -1;
      this.m_isBlendField = false;
      this.m_transitionIn.reset();
      this.m_AnimationTracks.clear();
      this.m_RagdollTracks.clear();
      this.m_RunningRagdollTrack = null;
      this.m_NodeAnimTime = 0.0F;
      this.m_PrevNodeAnimTime = 0.0F;
      this.m_blendingIn = false;
      this.m_blendingOut = false;
      this.m_transitionOut = null;
      this.m_bTweeningInGrapple = false;
      this.m_bTweeningInGrappleFinished = false;
      this.m_whileAliveFlags.clear();
      this.m_cachedRandomAnim = "";
   }

   public void onReleased() {
      this.removeAllTracks();
      this.reset();
   }

   public String getName() {
      return this.m_sourceNode.m_Name;
   }

   public boolean isBlendingOut() {
      return this.m_blendingOut;
   }

   public boolean isTransitioningIn() {
      return this.m_transitionIn.m_active && this.m_transitionIn.m_track != null;
   }

   public void startTransitionIn(LiveAnimNode var1, AnimTransition var2, AnimationTrack var3) {
      this.startTransitionIn(var1.getSourceNode(), var2, var3);
   }

   public void startTransitionIn(AnimNode var1, AnimTransition var2, AnimationTrack var3) {
      if (this.m_transitionIn.m_track != null) {
         DebugLog.Animation.debugln("Removing existing TransitioningIn track: %s. Replaced by: %s", this.m_transitionIn.m_track.getName(), var3 != null ? var3.getName() : "null");
         this.stopTransitionIn();
      }

      this.m_transitionIn.m_active = var3 != null;
      this.m_transitionIn.m_transitionedFrom = var1.m_Name;
      this.m_transitionIn.m_data = var2;
      this.m_transitionIn.m_track = var3;
      this.m_transitionIn.m_weight = 0.0F;
      this.m_transitionIn.m_rawWeight = 0.0F;
      this.m_transitionIn.m_blendingIn = true;
      this.m_transitionIn.m_blendingOut = false;
      this.m_transitionIn.m_time = 0.0F;
      if (this.m_transitionIn.m_track != null) {
         this.m_transitionIn.m_track.addListener(this);
      }

      this.setMainTracksPlaying(false);
   }

   public void stopTransitionIn() {
      this.removeTrack(this.m_transitionIn.m_track);
      this.m_transitionIn.reset();
   }

   private void removeTrack(AnimationTrack var1) {
      AnimationMultiTrack var2 = this.m_animLayer.getAnimationTrack();
      if (var2 != null) {
         var2.removeTrack(var1);
      }

   }

   public void removeAllTracks() {
      AnimationMultiTrack var1 = this.m_animLayer.getAnimationTrack();
      if (var1 != null) {
         var1.removeTracks(this.m_AnimationTracks);
         var1.removeTracks(this.m_RagdollTracks);
         var1.removeTrack(this.m_RunningRagdollTrack);
         var1.removeTrack(this.getTransitionInTrack());
      } else {
         IPooledObject.release(this.m_AnimationTracks);
         IPooledObject.release(this.m_RagdollTracks);
         Pool.tryRelease((IPooledObject)this.m_RunningRagdollTrack);
         Pool.tryRelease((IPooledObject)this.getTransitionInTrack());
      }

   }

   public void setTransitionOut(AnimTransition var1) {
      this.m_transitionOut = var1;
   }

   public void update(float var1) {
      this.m_isNew = false;
      if (this.m_active != this.m_wasActive) {
         this.m_blendingIn = this.m_active;
         this.m_blendingOut = !this.m_active;
         if (this.m_transitionIn.m_active) {
            this.m_transitionIn.m_blendingIn = this.m_active;
            this.m_transitionIn.m_blendingOut = !this.m_active;
         }

         this.m_wasActive = this.m_active;
      }

      boolean var2 = this.isMainAnimActive();
      if (this.isTransitioningIn()) {
         this.updateTransitioningIn(var1);
      }

      boolean var3 = this.isMainAnimActive();
      if (var3) {
         if (this.m_blendingOut && this.m_sourceNode.m_StopAnimOnExit) {
            this.setMainTracksPlaying(false);
         } else {
            this.setMainTracksPlaying(true);
         }
      } else {
         this.setMainTracksPlaying(false);
      }

      if (var3) {
         boolean var4 = !var2;
         if (var4 && this.isLooped()) {
            float var5 = this.getMainInitialRewindTime();
            PZArrayUtil.forEach(this.m_AnimationTracks, Lambda.consumer(var5, AnimationTrack::scaledRewind));
         }

         if (this.m_blendingIn) {
            this.updateBlendingIn(var1);
         } else if (this.m_blendingOut) {
            this.updateBlendingOut(var1);
         }

         this.m_PrevNodeAnimTime = this.m_NodeAnimTime;
         this.m_NodeAnimTime += var1;
         if (!this.m_transitionIn.m_active && this.m_transitionIn.m_track != null && this.m_transitionIn.m_track.BlendDelta <= 0.0F) {
            this.stopTransitionIn();
         }

         this.updateRagdollTracks();
      }
   }

   private void updateRagdollTracks() {
      if (!this.m_RagdollTracks.isEmpty()) {
         if (this.m_RunningRagdollTrack == null) {
            AnimationTrack var1 = null;

            for(int var2 = 0; var2 < this.m_RagdollTracks.size(); ++var2) {
               AnimationTrack var3 = (AnimationTrack)this.m_RagdollTracks.get(var2);
               if (!(var3.m_ragdollStartTime > this.m_NodeAnimTime)) {
                  if (var1 == null) {
                     var1 = var3;
                  } else if (var3.m_ragdollStartTime < var1.m_ragdollStartTime) {
                     var1 = var3;
                  }
               }
            }

            if (var1 != null) {
               this.m_RunningRagdollTrack = var1;
               this.m_RunningRagdollTrack.blendFieldWeight = 0.0F;
            }
         }

         if (this.m_RunningRagdollTrack != null) {
            if (this.m_AnimationTracks.isEmpty()) {
               this.m_RunningRagdollTrack.blendFieldWeight = 1.0F;
            } else if (this.m_RunningRagdollTrack.blendFieldWeight != 1.0F) {
               AnimationTrack var6 = this.m_RunningRagdollTrack;
               float var7 = this.m_NodeAnimTime - var6.m_ragdollStartTime;
               float var4 = this.m_sourceNode.m_BlendTime;
               float var5;
               if (var7 > 0.0F && var7 <= var4) {
                  var5 = var7 / var4;
               } else if (var7 > var4) {
                  var5 = 1.0F;
               } else {
                  var5 = 0.0F;
               }

               this.m_RunningRagdollTrack.blendFieldWeight = PZMath.max(this.m_RunningRagdollTrack.blendFieldWeight, var5);
            }
         }
      }
   }

   private void updateTransitioningIn(float var1) {
      float var2 = this.m_transitionIn.m_track.SpeedDelta;
      float var3 = this.m_transitionIn.m_track.getDuration();
      this.m_transitionIn.m_time = this.m_transitionIn.m_track.getCurrentTimeValue();
      if (!(this.m_transitionIn.m_time >= var3) && !DebugOptions.instance.Animation.DisableAnimationBlends.getValue()) {
         if (!this.m_transitionIn.m_blendingOut) {
            boolean var4 = AnimCondition.pass(this.m_animLayer.getVariableSource(), this.m_transitionIn.m_data.m_Conditions);
            if (!var4) {
               this.m_transitionIn.m_blendingIn = false;
               this.m_transitionIn.m_blendingOut = true;
            }
         }

         float var8 = this.getTransitionInBlendOutTime() * var2;
         if (this.m_transitionIn.m_time >= var3 - var8) {
            this.m_transitionIn.m_blendingIn = false;
            this.m_transitionIn.m_blendingOut = true;
         }

         float var5;
         float var6;
         float var7;
         if (this.m_transitionIn.m_blendingIn) {
            var5 = this.getTransitionInBlendInTime() * var2;
            var6 = this.incrementBlendTime(this.m_transitionIn.m_rawWeight, var5, var1 * var2);
            var7 = PZMath.clamp(var6 / var5, 0.0F, 1.0F);
            this.m_transitionIn.m_rawWeight = var7;
            this.m_transitionIn.m_weight = PZMath.lerpFunc_EaseOutInQuad(var7);
            this.m_transitionIn.m_blendingIn = var6 < var5;
            this.m_transitionIn.m_active = var6 < var3;
         }

         if (this.m_transitionIn.m_blendingOut) {
            var5 = this.getTransitionInBlendOutTime() * var2;
            var6 = this.incrementBlendTime(1.0F - this.m_transitionIn.m_rawWeight, var5, var1 * var2);
            var7 = PZMath.clamp(1.0F - var6 / var5, 0.0F, 1.0F);
            this.m_transitionIn.m_rawWeight = var7;
            this.m_transitionIn.m_weight = PZMath.lerpFunc_EaseOutInQuad(var7);
            this.m_transitionIn.m_blendingOut = var6 < var5;
            this.m_transitionIn.m_active = this.m_transitionIn.m_blendingOut;
         }

      } else {
         this.stopTransitionIn();
      }
   }

   public void addMainTrack(AnimationTrack var1) {
      if (!this.isLooped() && !this.m_sourceNode.m_StopAnimOnExit && this.m_sourceNode.m_EarlyTransitionOut) {
         float var2 = this.getBlendOutTime();
         if (var2 > 0.0F && Float.isFinite(var2)) {
            var1.earlyBlendOutTime = var2;
            var1.triggerOnNonLoopedAnimFadeOutEvent = true;
         }
      }

      if (var1.isRagdoll()) {
         this.m_RagdollTracks.add(var1);
      } else {
         this.m_AnimationTracks.add(var1);
      }

   }

   private void setMainTracksPlaying(boolean var1) {
      Lambda.forEachFrom(PZArrayUtil::forEach, (List)this.m_AnimationTracks, var1, (var0, var1x) -> {
         var0.IsPlaying = var1x;
      });
      if (this.m_RunningRagdollTrack != null) {
         this.m_RunningRagdollTrack.IsPlaying = var1;
      }

   }

   private void updateBlendingIn(float var1) {
      float var2 = this.getBlendInTime();
      if (!(var2 <= 0.0F) && !DebugOptions.instance.Animation.DisableAnimationBlends.getValue()) {
         float var3 = this.incrementBlendTime(this.m_RawWeight, var2, var1);
         float var4 = PZMath.clamp(var3 / var2, 0.0F, 1.0F);
         this.m_RawWeight = var4;
         this.m_Weight = PZMath.lerpFunc_EaseOutInQuad(var4);
         this.m_blendingIn = var3 < var2;
      } else {
         this.stopBlendingIn();
      }
   }

   private void updateBlendingOut(float var1) {
      float var2 = this.getBlendOutTime();
      if (!(var2 <= 0.0F) && !DebugOptions.instance.Animation.DisableAnimationBlends.getValue()) {
         float var3 = this.incrementBlendTime(1.0F - this.m_RawWeight, var2, var1);
         float var4 = PZMath.clamp(1.0F - var3 / var2, 0.0F, 1.0F);
         this.m_RawWeight = var4;
         this.m_Weight = PZMath.lerpFunc_EaseOutInQuad(var4);
         this.m_blendingOut = var3 < var2;
      } else {
         this.stopBlendingOut();
      }
   }

   private void stopBlendingOut() {
      this.setWeightsToZero();
      this.m_blendingOut = false;
   }

   private void stopBlendingIn() {
      this.setWeightsToFull();
      this.m_blendingIn = false;
   }

   public void setWeightsToZero() {
      this.m_Weight = 0.0F;
      this.m_RawWeight = 0.0F;
   }

   public void setWeightsToFull() {
      this.m_Weight = 1.0F;
      this.m_RawWeight = 1.0F;
   }

   private float incrementBlendTime(float var1, float var2, float var3) {
      float var4 = var1 * var2;
      return var4 + var3;
   }

   public float getTransitionInBlendInTime() {
      return this.m_transitionIn.m_data != null && this.m_transitionIn.m_data.m_blendInTime != 1.0F / 0.0F ? this.m_transitionIn.m_data.m_blendInTime : 0.0F;
   }

   public float getMainInitialRewindTime() {
      float var1 = 0.0F;
      float var2;
      if (this.m_sourceNode.m_randomAdvanceFraction > 0.0F) {
         var2 = Rand.Next(0.0F, this.m_sourceNode.m_randomAdvanceFraction);
         var1 = var2 * this.getMaxDuration();
      }

      if (this.m_transitionIn.m_data == null) {
         return 0.0F - var1;
      } else {
         var2 = this.getTransitionInBlendOutTime();
         float var3 = this.m_transitionIn.m_data.m_SyncAdjustTime;
         return this.m_transitionIn.m_track != null ? var2 - var3 : var2 - var3 - var1;
      }
   }

   private float getMaxDuration() {
      float var1 = 0.0F;
      int var2 = 0;

      int var3;
      AnimationTrack var4;
      float var5;
      for(var3 = this.m_AnimationTracks.size(); var2 < var3; ++var2) {
         var4 = (AnimationTrack)this.m_AnimationTracks.get(var2);
         var5 = var4.getDuration();
         var1 = PZMath.max(var5, var1);
      }

      var2 = 0;

      for(var3 = this.m_RagdollTracks.size(); var2 < var3; ++var2) {
         var4 = (AnimationTrack)this.m_RagdollTracks.get(var2);
         var5 = var4.getDuration();
         var1 = PZMath.max(var5, var1);
      }

      return var1;
   }

   public float getTransitionInBlendOutTime() {
      return this.getBlendInTime();
   }

   public float getBlendInTime() {
      if (this.m_transitionIn.m_data == null) {
         return this.m_sourceNode.m_BlendTime;
      } else if (this.m_transitionIn.m_track != null && this.m_transitionIn.m_data.m_blendOutTime != 1.0F / 0.0F) {
         return this.m_transitionIn.m_data.m_blendOutTime;
      } else {
         if (this.m_transitionIn.m_track == null) {
            if (this.m_transitionIn.m_data.m_blendInTime != 1.0F / 0.0F) {
               return this.m_transitionIn.m_data.m_blendInTime;
            }

            if (this.m_transitionIn.m_data.m_blendOutTime != 1.0F / 0.0F) {
               return this.m_transitionIn.m_data.m_blendOutTime;
            }
         }

         return this.m_sourceNode.m_BlendTime;
      }
   }

   public float getBlendOutTime() {
      if (this.m_transitionOut == null) {
         return this.m_sourceNode.getBlendOutTime();
      } else if (!StringUtils.isNullOrWhitespace(this.m_transitionOut.m_AnimName) && this.m_transitionOut.m_blendInTime != 1.0F / 0.0F) {
         return this.m_transitionOut.m_blendInTime;
      } else {
         if (StringUtils.isNullOrWhitespace(this.m_transitionOut.m_AnimName)) {
            if (this.m_transitionOut.m_blendOutTime != 1.0F / 0.0F) {
               return this.m_transitionOut.m_blendOutTime;
            }

            if (this.m_transitionOut.m_blendInTime != 1.0F / 0.0F) {
               return this.m_transitionOut.m_blendInTime;
            }
         }

         return this.m_sourceNode.getBlendOutTime();
      }
   }

   public void onAnimStarted(AnimationTrack var1) {
      this.invokeAnimStartTimeEvent(var1);
   }

   public void onLoopedAnim(AnimationTrack var1) {
      if (!this.m_TransitioningOut) {
         this.invokeAnimEndTimeEvent(var1);
      }
   }

   public void onNonLoopedAnimFadeOut(AnimationTrack var1) {
      if (DebugOptions.instance.Animation.AllowEarlyTransitionOut.getValue()) {
         this.invokeAnimEndTimeEvent(var1);
         this.m_TransitioningOut = true;
         this.decrementWhileAliveFlags();
      }
   }

   public void onNonLoopedAnimFinished(AnimationTrack var1) {
      if (!this.m_TransitioningOut) {
         this.invokeAnimEndTimeEvent(var1);
         this.decrementWhileAliveFlags();
      }
   }

   public void onTrackDestroyed(AnimationTrack var1) {
      this.m_AnimationTracks.remove(var1);
      this.m_RagdollTracks.remove(var1);
      if (this.m_RunningRagdollTrack == var1) {
         this.m_RunningRagdollTrack = null;
      }

      if (this.m_transitionIn.m_track == var1) {
         this.m_transitionIn.m_track = null;
         this.m_transitionIn.m_active = false;
         this.m_transitionIn.m_weight = 0.0F;
         this.setMainTracksPlaying(true);
      }

   }

   public void onNoAnimConditionsPass() {
   }

   private void invokeAnimStartTimeEvent(AnimationTrack var1) {
      this.invokeAnimTimeEvent(var1, AnimEvent.AnimEventTime.Start);
   }

   private void invokeAnimEndTimeEvent(AnimationTrack var1) {
      this.invokeAnimTimeEvent(var1, AnimEvent.AnimEventTime.End);
   }

   private void invokeAnimTimeEvent(AnimationTrack var1, AnimEvent.AnimEventTime var2) {
      if (this.m_sourceNode != null) {
         List var3 = this.getSourceNode().m_Events;
         int var4 = 0;

         for(int var5 = var3.size(); var4 < var5; ++var4) {
            AnimEvent var6 = (AnimEvent)var3.get(var4);
            if (var6.m_Time == var2) {
               this.m_animLayer.invokeAnimEvent(this, var1, var6);
            }
         }

      }
   }

   public AnimNode getSourceNode() {
      return this.m_sourceNode;
   }

   public boolean isIdleAnimActive() {
      return this.m_active && this.m_sourceNode.isIdleAnim();
   }

   public boolean isActive() {
      return this.m_active;
   }

   public void setActive(boolean var1) {
      if (this.m_active != var1) {
         this.m_active = var1;
      }

   }

   public boolean isLooped() {
      return this.m_sourceNode.m_Looped;
   }

   public float getWeight() {
      return this.m_Weight;
   }

   public float getTransitionInWeight() {
      return this.m_transitionIn.m_weight;
   }

   public boolean wasActivated() {
      return this.m_active != this.m_wasActive && this.m_active;
   }

   public boolean wasDeactivated() {
      return this.m_active != this.m_wasActive && this.m_wasActive;
   }

   public boolean isNew() {
      return this.m_isNew;
   }

   public int getPlayingTrackCount() {
      int var1 = 0;
      if (this.isMainAnimActive()) {
         var1 += this.m_AnimationTracks.size();
      }

      if (this.m_RunningRagdollTrack != null) {
         ++var1;
      }

      if (this.isTransitioningIn()) {
         ++var1;
      }

      return var1;
   }

   public AnimationTrack getPlayingTrackAt(int var1) {
      if (var1 < 0) {
         throw new IndexOutOfBoundsException("TrackIdx is negative. Out of bounds: " + var1);
      } else {
         int var2 = 0;
         if (this.isMainAnimActive()) {
            var2 += this.m_AnimationTracks.size();
            if (var1 < var2) {
               return (AnimationTrack)this.m_AnimationTracks.get(var1);
            }
         }

         if (this.m_RunningRagdollTrack != null) {
            ++var2;
            if (var1 < var2) {
               return this.m_RunningRagdollTrack;
            }
         }

         if (this.isTransitioningIn()) {
            ++var2;
            if (var1 < var2) {
               return this.m_transitionIn.m_track;
            }
         }

         throw new IndexOutOfBoundsException("TrackIdx out of bounds 0 - " + var2);
      }
   }

   public boolean isMainAnimActive() {
      return !this.isTransitioningIn() || this.m_transitionIn.m_blendingOut;
   }

   public String getTransitionFrom() {
      return this.m_transitionIn.m_transitionedFrom;
   }

   public void setTransitionInBlendDelta(float var1) {
      if (this.m_transitionIn.m_track != null) {
         this.m_transitionIn.m_track.BlendDelta = var1;
      }

   }

   public AnimationTrack getTransitionInTrack() {
      return this.m_transitionIn.m_track;
   }

   public int getTransitionLayerIdx() {
      return this.m_transitionIn.m_track != null ? this.m_transitionIn.m_track.getLayerIdx() : -1;
   }

   public int getLayerIdx() {
      return this.m_layerIdx;
   }

   public int getPriority() {
      return this.m_sourceNode.getPriority();
   }

   public String getDeferredBoneName() {
      return this.m_sourceNode.getDeferredBoneName();
   }

   public BoneAxis getDeferredBoneAxis() {
      return this.m_sourceNode.getDeferredBoneAxis();
   }

   public List<AnimBoneWeight> getSubStateBoneWeights() {
      return this.m_sourceNode.m_SubStateBoneWeights;
   }

   public AnimTransition findTransitionTo(IAnimationVariableSource var1, AnimNode var2) {
      return this.m_sourceNode.findTransitionTo(var1, var2);
   }

   public float getSpeedScale(IAnimationVariableSource var1) {
      return this.m_sourceNode.getSpeedScale(var1);
   }

   public boolean isGrappler() {
      return this.m_sourceNode.isGrappler();
   }

   public String getMatchingGrappledAnimNode() {
      return this.m_sourceNode.getMatchingGrappledAnimNode();
   }

   public GrappleOffsetBehaviour getGrapplerOffsetBehaviour() {
      return this.m_sourceNode.m_GrapplerOffsetBehaviour;
   }

   public float getGrappleOffsetForward() {
      return this.m_sourceNode.m_GrappleOffsetForward;
   }

   public float getGrappledOffsetYaw() {
      return this.m_sourceNode.m_GrappleOffsetYaw;
   }

   public String getAnimName() {
      return !StringUtils.isNullOrWhitespace(this.m_cachedRandomAnim) ? this.m_cachedRandomAnim : this.m_sourceNode.m_AnimName;
   }

   public void selectRandomAnim() {
      this.m_cachedRandomAnim = this.m_sourceNode.getRandomAnim();
   }

   public boolean isTweeningInGrapple() {
      return this.m_bTweeningInGrapple;
   }

   public void setTweeningInGrapple(boolean var1) {
      this.m_bTweeningInGrapple = var1;
   }

   public boolean isTweeningInGrappleFinished() {
      return this.m_bTweeningInGrappleFinished;
   }

   public void setTweeningInGrappleFinished(boolean var1) {
      this.m_bTweeningInGrappleFinished = var1;
   }

   public Vector3 getGrappleTweenStartPos(Vector3 var1) {
      var1.set(this.m_grappleTweenStartPos);
      return var1;
   }

   public void setGrappleTweenStartPos(Vector3 var1) {
      this.m_grappleTweenStartPos.set(var1);
   }

   public zombie.iso.Vector3 getGrappleTweenStartPos(zombie.iso.Vector3 var1) {
      var1.set(this.m_grappleTweenStartPos.x, this.m_grappleTweenStartPos.y, this.m_grappleTweenStartPos.z);
      return var1;
   }

   public void setGrappleTweenStartPos(zombie.iso.Vector3 var1) {
      this.m_grappleTweenStartPos.set(var1.x, var1.y, var1.z);
   }

   public float getGrappleTweenInTime() {
      return this.m_sourceNode.m_GrappleTweenInTime;
   }

   public Iterable<AnimationTrack> getMainAnimationTracks() {
      return (Iterable)(this.isMainAnimActive() ? this.m_AnimationTracks : PZEmptyIterable.getInstance());
   }

   public int getMainAnimationTracksCount() {
      return this.m_AnimationTracks.size();
   }

   public AnimationTrack getMainAnimationTrackAt(int var1) {
      return (AnimationTrack)this.m_AnimationTracks.get(var1);
   }

   public boolean containsMainAnimationTrack(AnimationTrack var1) {
      return this.m_AnimationTracks.contains(var1);
   }

   public boolean hasMainAnimationTracks() {
      return !this.m_AnimationTracks.isEmpty();
   }

   public boolean incrementWhileAliveFlagOnce(AnimationVariableReference var1) {
      return this.m_whileAliveFlags.contains(var1) ? false : this.m_whileAliveFlags.add(var1);
   }

   public ArrayList<AnimationVariableReference> getWhileAliveFlags() {
      return this.m_whileAliveFlags;
   }

   private void decrementWhileAliveFlags() {
      if (this.m_animLayer != null) {
         this.m_animLayer.decrementWhileAliveFlags(this);
      }
   }

   public boolean getUseDeferredRotation() {
      return this.m_sourceNode.m_useDeferedRotation;
   }

   public boolean getUseDeferredMovement() {
      return this.m_sourceNode.m_useDeferredMovement;
   }

   public float getDeferredRotationScale() {
      return this.m_sourceNode.m_deferredRotationScale;
   }

   private static class TransitionIn {
      private float m_time;
      private String m_transitionedFrom;
      private boolean m_active;
      private AnimationTrack m_track;
      private AnimTransition m_data;
      private float m_weight;
      private float m_rawWeight;
      private boolean m_blendingIn;
      private boolean m_blendingOut;

      private TransitionIn() {
      }

      private void reset() {
         this.m_time = 0.0F;
         this.m_transitionedFrom = null;
         this.m_active = false;
         this.m_track = null;
         this.m_data = null;
         this.m_weight = 0.0F;
         this.m_rawWeight = 0.0F;
         this.m_blendingIn = false;
         this.m_blendingOut = false;
      }
   }
}
