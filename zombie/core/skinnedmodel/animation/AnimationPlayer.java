package zombie.core.skinnedmodel.animation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import org.joml.Math;
import org.lwjgl.util.vector.Matrix;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;
import zombie.GameProfiler;
import zombie.GameTime;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.math.PZMath;
import zombie.core.math.Vector3;
import zombie.core.physics.RagdollController;
import zombie.core.skinnedmodel.HelperFunctions;
import zombie.core.skinnedmodel.advancedanimation.AdvancedAnimator;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.debug.AnimationPlayerRecorder;
import zombie.core.skinnedmodel.animation.sharedskele.SharedSkeleAnimationRepository;
import zombie.core.skinnedmodel.animation.sharedskele.SharedSkeleAnimationTrack;
import zombie.core.skinnedmodel.model.Model;
import zombie.core.skinnedmodel.model.SkeletonBone;
import zombie.core.skinnedmodel.model.SkinningBone;
import zombie.core.skinnedmodel.model.SkinningBoneHierarchy;
import zombie.core.skinnedmodel.model.SkinningData;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.iso.Vector2;
import zombie.network.MPStatistic;
import zombie.util.IPooledObject;
import zombie.util.Lambda;
import zombie.util.Pool;
import zombie.util.PooledObject;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;

public final class AnimationPlayer extends PooledObject {
   private Model model;
   private final Matrix4f propTransforms = new Matrix4f();
   private boolean m_boneTransformsNeedFirstFrame = true;
   private float m_boneTransformsTimeDelta = -1.0F;
   public AnimatorsBoneTransform[] m_boneTransforms;
   private Matrix4f[] m_modelTransforms;
   private SkinTransformData m_skinTransformData = null;
   private SkinTransformData m_skinTransformDataPool = null;
   private SkinningData m_skinningData;
   private AnimationClip m_ragdollAnimationClip;
   private SharedSkeleAnimationRepository m_sharedSkeleAnimationRepo = null;
   private SharedSkeleAnimationTrack m_currentSharedTrack;
   private AnimationClip m_currentSharedTrackClip;
   private float m_angle;
   private float m_targetAngle;
   private boolean m_characterAllowsTwist = true;
   private float m_twistAngle;
   private float m_shoulderTwistAngle;
   private float m_shoulderTwistWeight = 1.0F;
   private float m_targetTwistAngle;
   private float m_maxTwistAngle = PZMath.degToRad(70.0F);
   private float m_excessTwist = 0.0F;
   private static final float angleStepBase = 0.15F;
   public float angleStepDelta = 1.0F;
   public float angleTwistDelta = 1.0F;
   public boolean bDoBlending = true;
   public boolean bUpdateBones = true;
   private final Vector2 m_targetDir = new Vector2();
   private final ArrayList<AnimationBoneBindingPair> m_reparentedBoneBindings = new ArrayList();
   private final List<AnimationBoneBinding> m_twistBones = new ArrayList();
   private AnimationBoneBinding m_counterRotationBone = null;
   public final ArrayList<Integer> dismembered = new ArrayList();
   private final float m_minimumValidAnimWeight = 0.001F;
   private final int m_animBlendIndexCacheSize = 32;
   private final int[] m_animBlendIndices = new int[32];
   private final float[] m_animBlendWeights = new float[32];
   private final int[] m_animBlendLayers = new int[32];
   private final int[] m_animBlendPriorities = new int[32];
   private final int m_maxLayers = 4;
   private final int[] m_layerBlendCounts = new int[4];
   private final float[] m_layerWeightTotals = new float[4];
   private int m_totalAnimBlendCount = 0;
   public AnimationPlayer parentPlayer;
   private final Vector2 m_deferredMovement = new Vector2();
   private final Object m_deferredMovementLock = new String("DeferredMovementLock");
   private final Vector2 m_deferredMovementAccum = new Vector2();
   private final Object m_deferredMovementAccumLock = new String("DeferredMovementAccumLock");
   private final Vector2 m_deferredMovementFromRagdoll = new Vector2();
   private float m_deferredRotationWeight = 0.0F;
   private float m_deferredAngleDelta = 0.0F;
   private final Vector3 m_targetGrapplePos = new Vector3();
   private final Vector2 m_targetGrappleRotation = new Vector2(1.0F, 0.0F);
   private final Vector3 m_grappleOffset = new Vector3();
   private AnimationPlayerRecorder m_recorder = null;
   private static final ThreadLocal<AnimationTrack[]> tempTracks = ThreadLocal.withInitial(() -> {
      return new AnimationTrack[0];
   });
   private static final Vector2 tempo = new Vector2();
   private RagdollController m_ragdollController;
   private IsoGameCharacter m_character;
   private static final Pool<AnimationPlayer> s_pool = new Pool(AnimationPlayer::new);
   private final AnimationMultiTrack m_multiTrack = new AnimationMultiTrack();

   private AnimationPlayer() {
   }

   public static AnimationPlayer alloc(Model var0) {
      AnimationPlayer var1 = (AnimationPlayer)s_pool.alloc();
      var1.setModel(var0);
      return var1;
   }

   public AnimationClip getAnimationClip() {
      return this.m_currentSharedTrackClip;
   }

   public static float lerpBlendWeight(float var0, float var1, float var2) {
      if (PZMath.equal(var0, var1, 1.0E-4F)) {
         return var1;
      } else {
         float var3 = 1.0F / var2;
         float var4 = GameTime.getInstance().getTimeDelta();
         float var5 = var1 - var0;
         float var6 = (float)PZMath.sign(var5);
         float var7 = var0 + var6 * var3 * var4;
         float var8 = var1 - var7;
         float var9 = (float)PZMath.sign(var8);
         if (var9 != var6) {
            var7 = var1;
         }

         return var7;
      }
   }

   public void setModel(Model var1) {
      Objects.requireNonNull(var1);
      if (var1 != this.model) {
         this.model = var1;
         this.initSkinningData();
      }
   }

   public Model getModel() {
      return this.model;
   }

   public int getNumBones() {
      return !this.isReady() ? 0 : this.m_boneTransforms.length;
   }

   public AnimatorsBoneTransform getBoneTransformAt(int var1) {
      if (var1 >= 0 && this.getNumBones() > var1) {
         return this.m_boneTransforms[var1];
      } else {
         throw new IndexOutOfBoundsException("Bone index " + var1 + " out of range. NumBones:" + this.getNumBones());
      }
   }

   public <T extends BoneTransform> T getBoneTransformAt(int var1, T var2) {
      if (var1 >= 0 && this.getNumBones() > var1) {
         var2.set((BoneTransform)this.m_boneTransforms[var1]);
         return var2;
      } else {
         throw new IndexOutOfBoundsException("Bone index " + var1 + " out of range. NumBones:" + this.getNumBones());
      }
   }

   private void initSkinningData() {
      if (this.model.isReady()) {
         SkinningData var1 = (SkinningData)this.model.Tag;
         if (var1 != null) {
            if (this.m_skinningData != var1) {
               if (this.m_skinningData != null) {
                  this.m_skinningData = null;
                  this.m_multiTrack.reset();
               }

               this.m_skinningData = var1;
               Lambda.forEachFrom(PZArrayUtil::forEach, (List)this.m_reparentedBoneBindings, this.m_skinningData, AnimationBoneBindingPair::setSkinningData);
               Lambda.forEachFrom(PZArrayUtil::forEach, (List)this.m_twistBones, this.m_skinningData, AnimationBoneBinding::setSkinningData);
               if (this.m_counterRotationBone != null) {
                  this.m_counterRotationBone.setSkinningData(this.m_skinningData);
               }

               int var2 = var1.numBones();
               this.m_modelTransforms = (Matrix4f[])PZArrayUtil.newInstance(Matrix4f.class, this.m_modelTransforms, var2, Matrix4f::new);
               this.m_boneTransforms = (AnimatorsBoneTransform[])PZArrayUtil.newInstance(AnimatorsBoneTransform.class, this.m_boneTransforms, var2, AnimatorsBoneTransform::alloc);

               for(int var3 = 0; var3 < var2; ++var3) {
                  if (this.m_boneTransforms[var3] == null) {
                     this.m_boneTransforms[var3] = AnimatorsBoneTransform.alloc();
                  }

                  this.m_boneTransforms[var3].setIdentity();
               }

               this.m_boneTransformsNeedFirstFrame = true;
            }
         }
      }
   }

   public boolean isReady() {
      this.initSkinningData();
      return this.hasSkinningData();
   }

   public boolean hasSkinningData() {
      return this.m_skinningData != null;
   }

   public void addBoneReparent(String var1, String var2) {
      if (!PZArrayUtil.contains((List)this.m_reparentedBoneBindings, Lambda.predicate(var1, var2, AnimationBoneBindingPair::matches))) {
         AnimationBoneBindingPair var3 = new AnimationBoneBindingPair(var1, var2);
         var3.setSkinningData(this.m_skinningData);
         this.m_reparentedBoneBindings.add(var3);
      }
   }

   public void setTwistBones(String... var1) {
      ArrayList var2 = AnimationPlayer.L_setTwistBones.boneNames;
      PZArrayUtil.listConvert(this.m_twistBones, var2, (var0) -> {
         return var0.boneName;
      });
      if (!PZArrayUtil.sequenceEqual((Object[])var1, var2, PZArrayUtil.Comparators::equalsIgnoreCase)) {
         this.m_twistBones.clear();
         Lambda.forEachFrom(PZArrayUtil::forEach, (Object)var1, this, (var0, var1x) -> {
            AnimationBoneBinding var2 = new AnimationBoneBinding((String)var0);
            var2.setSkinningData(var1x.m_skinningData);
            var1x.m_twistBones.add(var2);
         });
      }
   }

   public int getNumTwistBones() {
      return this.m_twistBones.size();
   }

   public AnimatorsBoneTransform getTwistBoneAt(int var1) {
      AnimationBoneBinding var2 = (AnimationBoneBinding)this.m_twistBones.get(var1);
      SkinningBone var3 = var2.getBone();
      int var4 = var3.Index;
      return this.m_boneTransforms[var4];
   }

   public String getTwistBoneNameAt(int var1) {
      return ((AnimationBoneBinding)this.m_twistBones.get(var1)).boneName;
   }

   public void setCounterRotationBone(String var1) {
      if (this.m_counterRotationBone != null && StringUtils.equals(this.m_counterRotationBone.boneName, var1)) {
      }

      this.m_counterRotationBone = new AnimationBoneBinding(var1);
      this.m_counterRotationBone.setSkinningData(this.m_skinningData);
   }

   public AnimationBoneBinding getCounterRotationBone() {
      return this.m_counterRotationBone;
   }

   public void reset() {
      this.m_multiTrack.reset();
      this.releaseRagdollController();
   }

   public void onReleased() {
      this.model = null;
      this.m_skinningData = null;
      this.propTransforms.setIdentity();
      this.m_boneTransformsNeedFirstFrame = true;
      this.m_boneTransformsTimeDelta = -1.0F;
      this.m_boneTransforms = (AnimatorsBoneTransform[])IPooledObject.tryReleaseAndBlank(this.m_boneTransforms);
      PZArrayUtil.forEach((Object[])this.m_modelTransforms, Matrix::setIdentity);
      this.resetSkinTransforms();
      this.setAngle(0.0F);
      this.setTargetAngle(0.0F);
      this.m_twistAngle = 0.0F;
      this.m_shoulderTwistAngle = 0.0F;
      this.m_targetTwistAngle = 0.0F;
      this.m_maxTwistAngle = PZMath.degToRad(70.0F);
      this.m_excessTwist = 0.0F;
      this.angleStepDelta = 1.0F;
      this.angleTwistDelta = 1.0F;
      this.bDoBlending = true;
      this.bUpdateBones = true;
      this.m_targetDir.set(0.0F, 0.0F);
      this.m_reparentedBoneBindings.clear();
      this.m_twistBones.clear();
      this.m_counterRotationBone = null;
      this.dismembered.clear();
      Arrays.fill(this.m_animBlendIndices, 0);
      Arrays.fill(this.m_animBlendWeights, 0.0F);
      Arrays.fill(this.m_animBlendLayers, 0);
      Arrays.fill(this.m_layerBlendCounts, 0);
      Arrays.fill(this.m_layerWeightTotals, 0.0F);
      this.m_totalAnimBlendCount = 0;
      this.parentPlayer = null;
      this.m_deferredMovement.set(0.0F, 0.0F);
      this.m_deferredMovementAccum.set(0.0F, 0.0F);
      this.m_deferredMovementFromRagdoll.set(0.0F, 0.0F);
      this.m_deferredRotationWeight = 0.0F;
      this.m_deferredAngleDelta = 0.0F;
      this.m_recorder = null;
      this.m_multiTrack.reset();
      this.releaseRagdollController();
      this.m_character = null;
   }

   public SkinningData getSkinningData() {
      return this.m_skinningData;
   }

   public HashMap<String, Integer> getSkinningBoneIndices() {
      return this.m_skinningData != null ? this.m_skinningData.BoneIndices : null;
   }

   public int getSkinningBoneIndex(String var1, int var2) {
      HashMap var3 = this.getSkinningBoneIndices();
      return var3 != null && var3.containsKey(var1) ? (Integer)var3.get(var1) : var2;
   }

   private synchronized SkinTransformData getSkinTransformData(SkinningData var1) {
      SkinTransformData var2;
      for(var2 = this.m_skinTransformData; var2 != null; var2 = var2.m_next) {
         if (var1 == var2.m_skinnedTo) {
            return var2;
         }
      }

      if (this.m_skinTransformDataPool != null) {
         var2 = this.m_skinTransformDataPool;
         var2.setSkinnedTo(var1);
         var2.dirty = true;
         this.m_skinTransformDataPool = this.m_skinTransformDataPool.m_next;
      } else {
         var2 = AnimationPlayer.SkinTransformData.alloc(var1);
      }

      var2.m_next = this.m_skinTransformData;
      this.m_skinTransformData = var2;
      return var2;
   }

   private synchronized void resetSkinTransforms() {
      GameProfiler.getInstance().invokeAndMeasure("resetSkinTransforms", this, AnimationPlayer::resetSkinTransformsInternal);
   }

   private void resetSkinTransformsInternal() {
      if (this.m_skinTransformDataPool != null) {
         SkinTransformData var1;
         for(var1 = this.m_skinTransformDataPool; var1.m_next != null; var1 = var1.m_next) {
         }

         var1.m_next = this.m_skinTransformData;
      } else {
         this.m_skinTransformDataPool = this.m_skinTransformData;
      }

      this.m_skinTransformData = null;
   }

   public Matrix4f GetPropBoneMatrix(int var1) {
      this.propTransforms.load(this.m_modelTransforms[var1]);
      return this.propTransforms;
   }

   public AnimationTrack startClip(AnimationClip var1, boolean var2, float var3) {
      if (var1 == null) {
         throw new NullPointerException("Supplied clip is null.");
      } else {
         AnimationTrack var4 = AnimationTrack.alloc();
         var4.startClip(var1, var2, var3);
         var4.name = var1.Name;
         var4.IsPlaying = true;
         this.m_multiTrack.addTrack(var4);
         DebugLog.AnimationDetailed.debugln("startClip: %s", var1.Name);
         return var4;
      }
   }

   public static void releaseTracks(List<AnimationTrack> var0) {
      AnimationTrack[] var1 = (AnimationTrack[])tempTracks.get();
      AnimationTrack[] var2 = (AnimationTrack[])var0.toArray(var1);
      PZArrayUtil.forEach((Object[])var2, PooledObject::release);
   }

   public AnimationTrack play(String var1, boolean var2) {
      return this.play(var1, var2, false, -1.0F);
   }

   public AnimationTrack play(String var1, boolean var2, boolean var3, float var4) {
      if (!this.isReady()) {
         DebugLog.Animation.warn("AnimationPlayer is not ready. Cannot play animation: %s%s", var1, var3 ? "(Ragdoll)" : "");
         return null;
      } else if (this.m_skinningData == null) {
         DebugLog.Animation.warn("Skinning Data not found. AnimName: %s%s", var1, var3 ? "(Ragdoll)" : "");
         return null;
      } else {
         AnimationClip var5;
         if (var3) {
            var5 = this.getOrCreateRagdollAnimationClip();
         } else {
            var5 = (AnimationClip)this.m_skinningData.AnimationClips.get(var1);
         }

         if (var5 == null) {
            DebugLog.Animation.warn("Anim Clip %snot found: %s", var3 ? "(Ragdoll)" : "", var1);
            return null;
         } else {
            AnimationTrack var6 = this.startClip(var5, var2, var4);
            return var6;
         }
      }
   }

   public AnimationTrack play(StartAnimTrackParameters var1, AnimLayer var2) {
      AnimationTrack var3 = this.play(var1.animName, var1.isLooped, var1.isRagdoll, var1.ragdollMaxTime);
      if (var3 == null) {
         return null;
      } else {
         var3.IsPrimary = var1.isPrimary;
         SkinningData var4 = this.getSkinningData();
         if (var2.isSubLayer()) {
            var3.setBoneWeights(var1.subLayerBoneWeights);
            var3.initBoneWeights(var4);
         } else {
            var3.setBoneWeights((List)null);
         }

         SkinningBone var5 = var4.getBone(var1.deferredBoneName);
         if (var5 == null) {
            DebugLog.Animation.error("Deferred bone not found: \"%s\"", var1.deferredBoneName);
         }

         var3.SpeedDelta = var1.speedScale;
         var3.SyncTrackingEnabled = var1.syncTrackingEnabled;
         var3.setIKAimingLeftArm(var1.aimingIKLeftArm);
         var3.setIKAimingRightArm(var1.aimingIKRightArm);
         var3.setDeferredBone(var5, var1.deferredBoneAxis);
         var3.setUseDeferredRotation(var1.useDeferredRotation);
         var3.setDeferredRotationScale(var1.deferredRotationScale);
         var3.BlendDelta = var1.initialWeight;
         var3.reverse = var1.isReversed;
         var3.priority = var1.priority;
         var3.m_ragdollStartTime = var1.ragdollStartTime;
         var3.setMatchingGrappledAnimNode(var1.matchingGrappledAnimNode);
         var3.setAnimLayer(var2);
         return var3;
      }
   }

   public AnimationClip getOrCreateRagdollAnimationClip() {
      if (!this.isReady()) {
         return null;
      } else {
         SkinningBoneHierarchy var1 = this.getSkeletonBoneHiearchy();
         int var2 = var1.numBones();
         if (this.m_ragdollAnimationClip == null) {
            ArrayList var3 = new ArrayList();

            for(int var4 = 0; var4 < var2; ++var4) {
               SkinningBone var5 = var1.getBoneAt(var4);
               int var6 = var5.Index;
               Keyframe var7 = new Keyframe();
               var7.Bone = var6;
               var7.Time = 0.0F;
               var7.Position = new Vector3f();
               var7.Rotation = new Quaternion();
               var7.Scale = new Vector3f();
               var3.add(var7);
               var7 = new Keyframe();
               var7.Bone = var6;
               var7.Time = 1.0F;
               var7.Position = new Vector3f();
               var7.Rotation = new Quaternion();
               var7.Scale = new Vector3f();
               var3.add(var7);
            }

            this.m_ragdollAnimationClip = new AnimationClip(1.0F, var3, "RagdollAnimationClip", true, true);
         }

         return this.m_ragdollAnimationClip;
      }
   }

   public SkinningBoneHierarchy getSkeletonBoneHiearchy() {
      return !this.isReady() ? null : this.getSkinningData().getSkeletonBoneHiearchy();
   }

   public void Update() {
      this.Update(GameTime.instance.getTimeDelta());
   }

   public void Update(float var1) {
      MPStatistic.getInstance().AnimationPlayerUpdate.Start();
      GameProfiler.getInstance().invokeAndMeasure("AnimationPlayer.Update", this, var1, AnimationPlayer::updateInternal);
      MPStatistic.getInstance().AnimationPlayerUpdate.End();
   }

   private void updateInternal(float var1) {
      if (this.isReady()) {
         this.updateRagdoll(var1);
         this.m_multiTrack.Update(var1);
         if (!this.bUpdateBones) {
            this.updateAnimation_NonVisualOnly(var1);
         } else if (this.m_multiTrack.getTrackCount() > 0) {
            SharedSkeleAnimationTrack var2 = this.determineCurrentSharedSkeleTrack();
            if (var2 != null) {
               float var3 = this.m_multiTrack.getTrackAt(0).getCurrentTrackTime();
               this.updateAnimation_SharedSkeleTrack(var2, var1, var3);
            } else {
               this.updateAnimation_StandardAnimation(var1);
            }
         }
      }
   }

   private SharedSkeleAnimationTrack determineCurrentSharedSkeleTrack() {
      if (this.m_sharedSkeleAnimationRepo == null) {
         return null;
      } else if (this.bDoBlending) {
         return null;
      } else if (!DebugOptions.instance.Animation.SharedSkeles.Enabled.getValue()) {
         return null;
      } else if (this.m_multiTrack.getTrackCount() != 1) {
         return null;
      } else if (!PZMath.equal(this.m_twistAngle, 0.0F, 114.59155F)) {
         return null;
      } else if (this.parentPlayer != null) {
         return null;
      } else {
         AnimationTrack var1 = this.m_multiTrack.getTrackAt(0);
         if (var1.isRagdoll()) {
            return null;
         } else {
            float var2 = var1.blendFieldWeight;
            if (!PZMath.equal(var2, 0.0F, 0.1F)) {
               return null;
            } else {
               AnimationClip var3 = var1.getClip();
               if (var3 == this.m_currentSharedTrackClip) {
                  return this.m_currentSharedTrack;
               } else {
                  SharedSkeleAnimationTrack var4 = this.m_sharedSkeleAnimationRepo.getTrack(var3);
                  if (var4 == null) {
                     DebugLog.Animation.debugln("Caching SharedSkeleAnimationTrack: %s", var1.getName());
                     var4 = new SharedSkeleAnimationTrack();
                     ModelTransformSampler var5 = ModelTransformSampler.alloc(this, var1);

                     try {
                        var4.set(var5, 5.0F);
                     } finally {
                        var5.release();
                     }

                     this.m_sharedSkeleAnimationRepo.setTrack(var3, var4);
                  }

                  this.m_currentSharedTrackClip = var3;
                  this.m_currentSharedTrack = var4;
                  return var4;
               }
            }
         }
      }
   }

   private void updateAnimation_NonVisualOnly(float var1) {
      this.updateMultiTrackBoneTransforms_DeferredMovementOnly();
      this.DoAngles(var1);
      this.calculateDeferredMovement();
   }

   public void setSharedAnimRepo(SharedSkeleAnimationRepository var1) {
      this.m_sharedSkeleAnimationRepo = var1;
   }

   private void updateAnimation_SharedSkeleTrack(SharedSkeleAnimationTrack var1, float var2, float var3) {
      this.updateMultiTrackBoneTransforms_DeferredMovementOnly();
      this.DoAngles(var2);
      this.calculateDeferredMovement();
      var1.moveToTime(var3);

      for(int var4 = 0; var4 < this.m_modelTransforms.length; ++var4) {
         var1.getBoneMatrix(var4, this.m_modelTransforms[var4]);
      }

      this.UpdateSkinTransforms();
   }

   private void updateAnimation_StandardAnimation(float var1) {
      if (this.parentPlayer == null) {
         this.updateMultiTrackBoneTransforms(var1);
      } else {
         this.copyBoneTransformsFromParentPlayer();
      }

      this.DoAngles(var1);
      this.calculateDeferredMovement();
      this.updateTwistBone();
      this.applyBoneReParenting();
      this.updateModelTransforms();
      this.UpdateSkinTransforms();
   }

   private void updateRagdoll(float var1) {
      if (!this.bUpdateBones) {
         this.releaseRagdollController();
      } else if (this.getIsoGameCharacter() == null) {
         this.releaseRagdollController();
      } else if (!this.m_multiTrack.containsAnyRagdollTracks()) {
         this.releaseRagdollController();
      } else {
         AnimationTrack var2 = this.m_multiTrack.getActiveRagdollTrack();
         if (var2 == null) {
            this.releaseRagdollController();
         } else {
            GameProfiler.getInstance().invokeAndMeasure("AnimationPlayer.updateRagdoll", this, var1, AnimationPlayer::updateRagdollInternal);
         }
      }
   }

   private void updateRagdollInternal(float var1) {
      if (this.m_multiTrack.anyRagdollFirstFrame()) {
         if (!this.isBoneTransformsNeedFirstFrame()) {
            DebugLog.Animation.debugln("Initiating radgoll first-frames to boneTransforms...");
            this.m_multiTrack.initRagdollTransforms((TwistableBoneTransform[])this.m_boneTransforms);
         } else {
            DebugLog.Animation.debugln("Initiating radgoll first-frames to bindPose...");
            this.m_multiTrack.initRagdollTransforms(this.m_skinningData.BindPose);
         }
      }

      RagdollController var2 = this.getOrCreateRagdollController();
      if (var2 != null) {
         this.DoAnglesWhileRagdolling(var1);
         var2.update(var1);
      }
   }

   private void copyBoneTransformsFromParentPlayer() {
      this.m_boneTransformsNeedFirstFrame = false;

      for(int var1 = 0; var1 < this.m_boneTransforms.length; ++var1) {
         this.m_boneTransforms[var1].set(this.parentPlayer.m_boneTransforms[var1]);
      }

   }

   public static float calculateAnimPlayerAngle(Vector2 var0) {
      return var0.getDirection();
   }

   public void setTargetDirection(Vector2 var1) {
      if (this.m_targetDir.x != var1.x || this.m_targetDir.y != var1.y) {
         this.setTargetAngle(calculateAnimPlayerAngle(var1));
         this.m_targetTwistAngle = PZMath.getClosestAngle(this.m_angle, this.m_targetAngle);
         float var2 = PZMath.clamp(this.m_targetTwistAngle, -this.m_maxTwistAngle, this.m_maxTwistAngle);
         this.m_excessTwist = PZMath.getClosestAngle(var2, this.m_targetTwistAngle);
         this.m_targetDir.set(var1);
      }

   }

   public void setTargetAndCurrentDirection(Vector2 var1) {
      this.setTargetAngle(calculateAnimPlayerAngle(var1));
      this.setAngleToTarget();
      this.m_targetTwistAngle = 0.0F;
      this.m_targetDir.set(var1);
   }

   public void updateForwardDirection(IsoGameCharacter var1) {
      if (var1 != null) {
         this.setTargetDirection(var1.getForwardDirection());
         this.m_characterAllowsTwist = var1.allowsTwist();
         this.m_shoulderTwistWeight = var1.getShoulderTwistWeight();
      }

   }

   public void DoAngles(float var1) {
      if (!this.isRagdolling()) {
         GameProfiler.getInstance().invokeAndMeasure("AnimationPlayer.doAngles", this, var1, AnimationPlayer::doAnglesInternal);
      }

   }

   public void DoAnglesWhileRagdolling(float var1) {
      if (this.isRagdolling()) {
         GameProfiler.getInstance().invokeAndMeasure("AnimationPlayer.doAnglesWhileRagdollingInternal", this, var1, AnimationPlayer::doAnglesWhileRagdollingInternal);
      }

   }

   private void doAnglesInternal(float var1) {
      float var2 = 0.15F * GameTime.instance.getMultiplierFromTimeDelta(var1);
      this.interpolateBodyAngle(var2);
      this.interpolateBodyTwist(var2);
      this.interpolateShoulderTwist(var2);
   }

   private void doAnglesWhileRagdollingInternal(float var1) {
      RagdollController var2 = this.getRagdollController();
      if (var2 != null) {
         if (var2.isInitialized() && var2.isSimulationDirectionCalculated()) {
            float var3 = var2.getCalculatedSimlationDirectionAngle();
            PZMath.getClosestAngle(this.m_targetAngle, var3);
            this.m_targetAngle = var3;
            this.doAnglesInternal(var1);
         }
      }
   }

   private void interpolateBodyAngle(float var1) {
      float var2 = this.m_targetAngle;
      float var3 = PZMath.getClosestAngle(this.m_angle, var2);
      if (PZMath.equal(var3, 0.0F, 0.001F)) {
         this.setAngleToTarget();
         this.m_targetTwistAngle = 0.0F;
      } else {
         float var4 = (float)PZMath.sign(var3);
         float var5 = var1 * var4 * this.angleStepDelta;
         float var6;
         if (DebugOptions.instance.Character.Debug.Animate.DeferredRotationOnly.getValue()) {
            var6 = this.m_deferredAngleDelta;
         } else if (this.m_deferredRotationWeight > 0.0F) {
            var6 = this.m_deferredAngleDelta * this.m_deferredRotationWeight + var5 * (1.0F - this.m_deferredRotationWeight);
         } else {
            var6 = var5;
         }

         float var7 = (float)PZMath.sign(var6);
         float var8 = this.m_angle;
         float var9 = var8 + var6;
         float var10 = PZMath.getClosestAngle(var9, var2);
         float var11 = (float)PZMath.sign(var10);
         if (var11 != var4 && var7 == var4) {
            this.setAngleToTarget();
            this.m_targetTwistAngle = 0.0F;
         } else {
            this.setAngle(var9);
            this.m_targetTwistAngle = var10;
         }

      }
   }

   private void interpolateBodyTwist(float var1) {
      float var2 = PZMath.wrap(this.m_targetTwistAngle, -3.1415927F, 3.1415927F);
      float var3 = PZMath.clamp(var2, -this.m_maxTwistAngle, this.m_maxTwistAngle);
      this.m_excessTwist = PZMath.getClosestAngle(var3, var2);
      float var4 = PZMath.getClosestAngle(this.m_twistAngle, var3);
      if (PZMath.equal(var4, 0.0F, 0.001F)) {
         this.m_twistAngle = var3;
      } else {
         float var5 = (float)PZMath.sign(var4);
         float var6 = var1 * var5 * this.angleTwistDelta;
         float var7 = this.m_twistAngle;
         float var8 = var7 + var6;
         float var9 = PZMath.getClosestAngle(var8, var3);
         float var10 = (float)PZMath.sign(var9);
         if (var10 == var5) {
            this.m_twistAngle = var8;
         } else {
            this.m_twistAngle = var3;
         }

      }
   }

   private void interpolateShoulderTwist(float var1) {
      float var2 = PZMath.wrap(this.m_twistAngle, -3.1415927F, 3.1415927F);
      float var3 = PZMath.getClosestAngle(this.m_shoulderTwistAngle, var2);
      if (PZMath.equal(var3, 0.0F, 0.001F)) {
         this.m_shoulderTwistAngle = var2;
      } else {
         float var4 = (float)PZMath.sign(var3);
         float var5 = var1 * var4 * this.angleTwistDelta * 0.55F;
         float var6 = this.m_shoulderTwistAngle;
         float var7 = var6 + var5;
         float var8 = PZMath.getClosestAngle(var7, var2);
         float var9 = (float)PZMath.sign(var8);
         if (var9 == var4) {
            this.m_shoulderTwistAngle = var7;
         } else {
            this.m_shoulderTwistAngle = var2;
         }

      }
   }

   private void updateTwistBone() {
      GameProfiler.getInstance().invokeAndMeasure("updateTwistBone", this, AnimationPlayer::updateTwistBoneInternal);
   }

   private void updateTwistBoneInternal() {
      if (!this.m_twistBones.isEmpty()) {
         if (!DebugOptions.instance.Character.Debug.Animate.NoBoneTwists.getValue()) {
            if (this.m_characterAllowsTwist) {
               int var1 = this.m_twistBones.size();
               int var2 = var1 - 1;
               float var3 = this.m_shoulderTwistAngle;
               if (DebugOptions.instance.Character.Debug.Animate.AlwaysAimTwist.getValue()) {
                  Vector2 var4 = IsoPlayer.getInstance().getMouseAimVector(new Vector2());
                  float var5 = calculateAnimPlayerAngle(var4);
                  var3 = PZMath.getClosestAngle(this.m_angle, var5);
                  var3 = PZMath.clamp(var3, -this.m_maxTwistAngle, this.m_maxTwistAngle);
               }

               SkinningBone var10 = ((AnimationBoneBinding)this.m_twistBones.get(var2)).getBone();
               float var6 = this.calculateDesiredTwist(var10, var3);
               float var7 = this.m_shoulderTwistWeight / (float)(var2 - 1);

               for(int var8 = 0; var8 < var2; ++var8) {
                  SkinningBone var9 = ((AnimationBoneBinding)this.m_twistBones.get(var8)).getBone();
                  this.adjustTwistBone(var9, var6 * var7 * (float)var8);
               }

               this.applyTwistBone(var10, var3, 1.0F);
            }
         }
      }
   }

   private float calculateDesiredTwist(SkinningBone var1, float var2) {
      if (var1 == null) {
         return 0.0F;
      } else {
         int var3 = var1.Index;
         int var4 = var1.Parent.Index;
         Matrix4f var5 = this.getBoneModelTransform(var4, AnimationPlayer.L_applyTwistBone.twistParentBoneTrans);
         Matrix4f var6 = Matrix4f.invert(var5, AnimationPlayer.L_applyTwistBone.twistParentBoneTransInv);
         if (var6 == null) {
            return 0.0F;
         } else {
            Matrix4f var7 = this.getBoneModelTransform(var3, AnimationPlayer.L_applyTwistBone.twistBoneTrans);
            Matrix4f var8 = AnimationPlayer.L_applyTwistBone.twistBoneNewTrans;
            var8.load(var7);
            Vector3f var9 = AnimationPlayer.L_applyTwistBone.desiredForward;
            var9.set(0.0F, 0.0F, 1.0F);
            HelperFunctions.transform(HelperFunctions.setFromAxisAngle(0.0F, 1.0F, 0.0F, var2, AnimationPlayer.L_applyTwistBone.twistTurnRot), var9, var9);
            Vector3f var10 = HelperFunctions.getZAxis(var8, AnimationPlayer.L_applyTwistBone.forward);
            var10.scale(-1.0F);
            var10.y = 0.0F;
            var10.normalise();
            float var11 = HelperFunctions.getAngle(var10.x, -var10.z, var9.x, -var9.z);
            Vector3f var12 = AnimationPlayer.L_applyTwistBone.twistRotateAxis;
            var12.set(0.0F, -1.0F, 0.0F);
            return var11;
         }
      }
   }

   private void applyTwistBone(SkinningBone var1, float var2, float var3) {
      if (var1 != null) {
         int var4 = var1.Index;
         int var5 = var1.Parent.Index;
         Matrix4f var6 = this.getBoneModelTransform(var5, AnimationPlayer.L_applyTwistBone.twistParentBoneTrans);
         Matrix4f var7 = Matrix4f.invert(var6, AnimationPlayer.L_applyTwistBone.twistParentBoneTransInv);
         if (var7 != null) {
            Matrix4f var8 = this.getBoneModelTransform(var4, AnimationPlayer.L_applyTwistBone.twistBoneTrans);
            Vector3f var9 = HelperFunctions.getPosition(var8, AnimationPlayer.L_applyTwistBone.twistBonePos);
            Matrix4f var10 = AnimationPlayer.L_applyTwistBone.twistBoneNewTrans;
            var10.load(var8);
            HelperFunctions.setPosition(var10, 0.0F, 0.0F, 0.0F);
            Vector3f var11 = AnimationPlayer.L_applyTwistBone.desiredForward;
            var11.set(0.0F, 0.0F, 1.0F);
            HelperFunctions.transform(HelperFunctions.setFromAxisAngle(0.0F, 1.0F, 0.0F, var2, AnimationPlayer.L_applyTwistBone.twistTurnRot), var11, var11);
            Vector3f var12 = HelperFunctions.getZAxis(var10, AnimationPlayer.L_applyTwistBone.forward);
            var12.scale(-1.0F);
            var12.y = 0.0F;
            var12.normalise();
            float var13 = HelperFunctions.getAngle(var12.x, -var12.z, var11.x, -var11.z);
            Vector3f var14 = AnimationPlayer.L_applyTwistBone.twistRotateAxis;
            var14.set(0.0F, -1.0F, 0.0F);
            var10.rotate(var13 * var3, var14);
            HelperFunctions.setPosition(var10, var9);
            this.m_boneTransforms[var4].Twist = PZMath.wrap(HelperFunctions.getRotationY(var10) - 3.1415927F, -3.1415927F, 3.1415927F);
            this.m_boneTransforms[var4].mul(var10, var7);
         }
      }
   }

   private void adjustTwistBone(SkinningBone var1, float var2) {
      if (var1 != null) {
         int var3 = var1.Index;
         int var4 = var1.Parent.Index;
         Matrix4f var5 = this.getBoneModelTransform(var4, AnimationPlayer.L_applyTwistBone.twistParentBoneTrans);
         Matrix4f var6 = Matrix4f.invert(var5, AnimationPlayer.L_applyTwistBone.twistParentBoneTransInv);
         if (var6 != null) {
            Matrix4f var7 = this.getBoneModelTransform(var3, AnimationPlayer.L_applyTwistBone.twistBoneTrans);
            Vector3f var8 = HelperFunctions.getPosition(var7, AnimationPlayer.L_applyTwistBone.twistBonePos);
            Matrix4f var9 = AnimationPlayer.L_applyTwistBone.twistBoneNewTrans;
            var9.load(var7);
            HelperFunctions.setPosition(var9, 0.0F, 0.0F, 0.0F);
            Vector3f var10 = AnimationPlayer.L_applyTwistBone.twistRotateAxis;
            var10.set(0.0F, -1.0F, 0.0F);
            var9.rotate(var2, var10);
            HelperFunctions.setPosition(var9, var8);
            this.m_boneTransforms[var3].Twist = PZMath.wrap(HelperFunctions.getRotationY(var9) - 3.1415927F, -3.1415927F, 3.1415927F);
            this.m_boneTransforms[var3].mul(var9, var6);
         }
      }
   }

   public void resetBoneModelTransforms() {
      if (this.m_skinningData != null && this.m_modelTransforms != null) {
         this.m_boneTransformsNeedFirstFrame = true;
         this.m_boneTransformsTimeDelta = -1.0F;
         int var1 = this.m_boneTransforms.length;

         for(int var2 = 0; var2 < var1; ++var2) {
            this.m_boneTransforms[var2].reset();
            this.m_modelTransforms[var2].setIdentity();
         }

      }
   }

   public boolean isBoneTransformsNeedFirstFrame() {
      return this.m_boneTransformsNeedFirstFrame;
   }

   private void updateMultiTrackBoneTransforms(float var1) {
      GameProfiler.getInstance().invokeAndMeasure("updateMultiTrackBoneTransforms", this, var1, AnimationPlayer::updateMultiTrackBoneTransformsInternal);
   }

   private void updateMultiTrackBoneTransformsInternal(float var1) {
      this.m_boneTransformsTimeDelta = var1;

      int var2;
      for(var2 = 0; var2 < this.m_boneTransforms.length; ++var2) {
         AnimatorsBoneTransform var3 = this.m_boneTransforms[var2];
         var3.nextFrame(var1);
      }

      for(var2 = 0; var2 < this.m_modelTransforms.length; ++var2) {
         this.m_modelTransforms[var2].setIdentity();
      }

      this.updateLayerBlendWeightings();
      if (this.m_totalAnimBlendCount != 0) {
         if (this.isRecording()) {
            this.m_recorder.logAnimWeights(this.m_multiTrack.getTracks(), this.m_animBlendIndices, this.m_animBlendWeights, this.m_deferredMovement, this.m_deferredMovementFromRagdoll);
         }

         for(var2 = 0; var2 < this.m_boneTransforms.length; ++var2) {
            if (!this.isBoneReparented(var2)) {
               this.updateBoneAnimationTransform(var2, (AnimationBoneBindingPair)null);
            }
         }

         this.m_boneTransformsNeedFirstFrame = false;
      }
   }

   private void updateLayerBlendWeightings() {
      List var1 = this.m_multiTrack.getTracks();
      int var2 = var1.size();
      PZArrayUtil.arraySet(this.m_animBlendIndices, -1);
      PZArrayUtil.arraySet(this.m_animBlendWeights, 0.0F);
      PZArrayUtil.arraySet(this.m_animBlendLayers, -1);
      PZArrayUtil.arraySet(this.m_animBlendPriorities, 0);

      int var3;
      float var5;
      int var6;
      int var7;
      for(var3 = 0; var3 < var2; ++var3) {
         AnimationTrack var4 = (AnimationTrack)var1.get(var3);
         var5 = var4.BlendDelta;
         var6 = var4.getLayerIdx();
         var7 = var4.getPriority();
         if (var6 >= 0 && var6 < 4) {
            if (!(var5 < 0.001F) && (var6 <= 0 || !var4.isFinished())) {
               int var8 = -1;

               for(int var9 = 0; var9 < this.m_animBlendIndices.length; ++var9) {
                  if (this.m_animBlendIndices[var9] == -1) {
                     var8 = var9;
                     break;
                  }

                  if (var6 <= this.m_animBlendLayers[var9]) {
                     if (var6 < this.m_animBlendLayers[var9]) {
                        var8 = var9;
                        break;
                     }

                     if (var7 <= this.m_animBlendPriorities[var9]) {
                        if (var7 < this.m_animBlendPriorities[var9]) {
                           var8 = var9;
                           break;
                        }

                        if (var5 < this.m_animBlendWeights[var9]) {
                           var8 = var9;
                           break;
                        }
                     }
                  }
               }

               if (var8 < 0) {
                  DebugLog.General.error("Buffer overflow. Insufficient anim blends in cache. More than %d animations are being blended at once. Will be truncated to %d.", this.m_animBlendIndices.length, this.m_animBlendIndices.length);
               } else {
                  PZArrayUtil.insertAt(this.m_animBlendIndices, var8, var3);
                  PZArrayUtil.insertAt(this.m_animBlendWeights, var8, var5);
                  PZArrayUtil.insertAt(this.m_animBlendLayers, var8, var6);
                  PZArrayUtil.insertAt(this.m_animBlendPriorities, var8, var7);
               }
            }
         } else {
            DebugLog.General.error("Layer index is out of range: %d. Range: 0 - %d", var6, 3);
         }
      }

      PZArrayUtil.arraySet(this.m_layerBlendCounts, 0);
      PZArrayUtil.arraySet(this.m_layerWeightTotals, 0.0F);
      this.m_totalAnimBlendCount = 0;

      int var10;
      float[] var10000;
      for(var3 = 0; var3 < this.m_animBlendIndices.length && this.m_animBlendIndices[var3] >= 0; ++var3) {
         var10 = this.m_animBlendLayers[var3];
         var10000 = this.m_layerWeightTotals;
         var10000[var10] += this.m_animBlendWeights[var3];
         int var10002 = this.m_layerBlendCounts[var10]++;
         ++this.m_totalAnimBlendCount;
      }

      if (this.m_totalAnimBlendCount != 0) {
         if (this.m_boneTransformsNeedFirstFrame) {
            var3 = this.m_animBlendLayers[0];
            var10 = this.m_layerBlendCounts[0];
            var5 = this.m_layerWeightTotals[0];
            if (var5 < 1.0F) {
               for(var6 = 0; var6 < this.m_totalAnimBlendCount; ++var6) {
                  var7 = this.m_animBlendLayers[var6];
                  if (var7 != var3) {
                     break;
                  }

                  if (var5 > 0.0F) {
                     var10000 = this.m_animBlendWeights;
                     var10000[var6] /= var5;
                  } else {
                     this.m_animBlendWeights[var6] = 1.0F / (float)var10;
                  }
               }
            }
         }

      }
   }

   private void calculateDeferredMovement() {
      GameProfiler.getInstance().invokeAndMeasure("calculateDeferredMovement", this, AnimationPlayer::calculateDeferredMovementInternal);
   }

   private void calculateDeferredMovementInternal() {
      synchronized(this.m_deferredMovementAccumLock) {
         this.calculateDeferredMovementAccumInternal(this.m_deferredMovementAccum);
         this.pushDeferredMovementAccumToDeferredMovement();
      }
   }

   private void pushDeferredMovementAccumToDeferredMovement() {
      synchronized(this.m_deferredMovementLock) {
         this.m_deferredMovement.set(this.m_deferredMovementAccum);
      }
   }

   private void calculateDeferredMovementAccumInternal(Vector2 var1) {
      List var2 = this.m_multiTrack.getTracks();
      this.m_deferredMovementFromRagdoll.set(0.0F, 0.0F);
      this.m_deferredAngleDelta = 0.0F;
      this.m_deferredRotationWeight = 0.0F;
      float var3 = 1.0F;

      for(int var4 = this.m_totalAnimBlendCount - 1; var4 >= 0 && !(var3 <= 0.001F); --var4) {
         int var5 = this.m_animBlendIndices[var4];
         AnimationTrack var6 = (AnimationTrack)var2.get(var5);
         if (!var6.isFinished()) {
            float var7 = var6.getDeferredBoneWeight();
            if (!(var7 <= 0.001F)) {
               float var8 = this.m_animBlendWeights[var4] * var7;
               if (!(var8 <= 0.001F)) {
                  float var9 = PZMath.clamp(var8, 0.0F, var3);
                  var3 -= var8;
                  var3 = Math.max(0.0F, var3);
                  if (var6.getUseDeferredMovement()) {
                     if (var6.isRagdoll()) {
                        Vector2.addScaled(this.m_deferredMovementFromRagdoll, var6.getDeferredMovementDiff(tempo), var9, this.m_deferredMovementFromRagdoll);
                     } else {
                        Vector2.addScaled(var1, var6.getDeferredMovementDiff(tempo), var9, var1);
                     }
                  }

                  if (var6.getUseDeferredRotation()) {
                     this.m_deferredAngleDelta += var6.getDeferredRotationDiff() * var9;
                     this.m_deferredRotationWeight += var9;
                  }
               }
            }
         }
      }

      this.applyRotationToDeferredMovement(var1);
      this.applyRotationToDeferredMovement(this.m_deferredMovementFromRagdoll);
      var1.x *= AdvancedAnimator.s_MotionScale;
      var1.y *= AdvancedAnimator.s_MotionScale;
      this.m_deferredAngleDelta *= AdvancedAnimator.s_RotationScale;
      Vector3 var10000 = this.m_targetGrapplePos;
      var10000.x += var1.x;
      var10000 = this.m_targetGrapplePos;
      var10000.y += var1.y;
   }

   private void applyRotationToDeferredMovement(Vector2 var1) {
      float var2 = var1.normalize();
      float var3 = this.getRenderedAngle();
      var1.rotate(var3);
      var1.setLength(-var2);
   }

   private void applyBoneReParenting() {
      GameProfiler.getInstance().invokeAndMeasure("applyBoneReParenting", this, AnimationPlayer::applyBoneReParentingInternal);
   }

   private void applyBoneReParentingInternal() {
      int var1 = 0;

      for(int var2 = this.m_reparentedBoneBindings.size(); var1 < var2; ++var1) {
         AnimationBoneBindingPair var3 = (AnimationBoneBindingPair)this.m_reparentedBoneBindings.get(var1);
         if (!var3.isValid()) {
            DebugLog.Animation.warn("Animation binding pair is not valid: %s", var3);
         } else {
            this.updateBoneAnimationTransform(var3.getBoneIdxA(), var3);
         }
      }

   }

   private void updateBoneAnimationTransform(int var1, AnimationBoneBindingPair var2) {
      this.updateBoneAnimationTransform_Internal(var1, var2);
   }

   private void updateBoneAnimationTransform_Internal(int var1, AnimationBoneBindingPair var2) {
      List var3 = this.m_multiTrack.getTracks();
      Vector3f var4 = AnimationPlayer.L_updateBoneAnimationTransform.pos;
      Quaternion var5 = AnimationPlayer.L_updateBoneAnimationTransform.rot;
      Vector3f var6 = AnimationPlayer.L_updateBoneAnimationTransform.scale;
      Keyframe var7 = AnimationPlayer.L_updateBoneAnimationTransform.key;
      int var8 = this.m_totalAnimBlendCount;
      AnimationBoneBinding var9 = this.m_counterRotationBone;
      boolean var10 = var9 != null && var9.getBone() != null && var9.getBone().Index == var1;
      var7.setIdentity();
      float var11 = 0.0F;
      boolean var12 = true;
      float var13 = 1.0F;

      for(int var14 = var8 - 1; var14 >= 0 && var13 > 0.0F && !(var13 <= 0.001F); --var14) {
         int var15 = this.m_animBlendIndices[var14];
         AnimationTrack var16 = (AnimationTrack)var3.get(var15);
         float var17 = var16.getBoneWeight(var1);
         if (!(var17 <= 0.001F)) {
            float var18 = this.m_animBlendWeights[var14] * var17;
            if (!(var18 <= 0.001F)) {
               float var19 = PZMath.clamp(var18, 0.0F, var13);
               var13 -= var18;
               var13 = Math.max(0.0F, var13);
               this.getTrackTransform(var1, var16, var2, var4, var5, var6);
               if (var10 && var16.getUseDeferredRotation()) {
                  Vector3f var20;
                  if (DebugOptions.instance.Character.Debug.Animate.ZeroCounterRotationBone.getValue()) {
                     var20 = AnimationPlayer.L_updateBoneAnimationTransform.rotAxis;
                     Matrix4f var21 = AnimationPlayer.L_updateBoneAnimationTransform.rotMat;
                     var21.setIdentity();
                     var20.set(0.0F, 1.0F, 0.0F);
                     var21.rotate(-1.5707964F, var20);
                     var20.set(1.0F, 0.0F, 0.0F);
                     var21.rotate(-1.5707964F, var20);
                     HelperFunctions.getRotation(var21, var5);
                  } else {
                     var20 = HelperFunctions.ToEulerAngles(var5, AnimationPlayer.L_updateBoneAnimationTransform.rotEulers);
                     HelperFunctions.ToQuaternion((double)var20.x, (double)var20.y, 1.5707963705062866, var5);
                  }
               }

               boolean var24 = var16.getDeferredMovementBoneIdx() == var1;
               if (var24) {
                  Vector3f var22 = var16.getCurrentDeferredCounterPosition(AnimationPlayer.L_updateBoneAnimationTransform.deferredPos);
                  var4.x += var22.x;
                  var4.y += var22.y;
                  var4.z += var22.z;
               }

               if (var12) {
                  Vector3.setScaled(var4, var19, var7.Position);
                  var7.Rotation.set(var5);
                  var11 = var19;
                  var12 = false;
               } else {
                  float var23 = var19 / (var19 + var11);
                  var11 += var19;
                  Vector3.addScaled(var7.Position, var4, var19, var7.Position);
                  PZMath.slerp(var7.Rotation, var7.Rotation, var5, var23);
               }
            }
         }
      }

      if (var13 > 0.0F && !this.m_boneTransformsNeedFirstFrame) {
         this.m_boneTransforms[var1].getPRS(var4, var5, var6);
         Vector3.addScaled(var7.Position, var4, var13, var7.Position);
         PZMath.slerp(var7.Rotation, var5, var7.Rotation, var11);
         PZMath.lerp(var7.Scale, var6, var7.Scale, var11);
      }

      this.m_boneTransforms[var1].set(var7.Position, var7.Rotation, var7.Scale);
      this.m_boneTransforms[var1].BlendWeight = var11;
   }

   private void getTrackTransform(int var1, AnimationTrack var2, AnimationBoneBindingPair var3, Vector3f var4, Quaternion var5, Vector3f var6) {
      Matrix4f var9;
      if (var1 == SkeletonBone.Bip01.index() && !var2.isRagdoll()) {
         if (!var2.m_isInitialAdjustmentCalculated) {
            var2.m_initialAdjustment.set(0.0F, 0.0F, 0.0F);
            if (this.isRagdolling() && DebugOptions.instance.Character.Debug.Animate.KeepAtOrigin.getValue()) {
               int var18 = SkeletonBone.Bip01.index();
               Matrix4f var19 = this.getBoneModelTransform(var18, new Matrix4f());
               var9 = this.getUnweightedModelTransform(var2, var18, new Matrix4f());
               HelperFunctions.getPosition(var19, new Vector3f());
               Vector3f var20 = HelperFunctions.getPosition(var9, new Vector3f());
               Vector3f.sub(var2.m_initialAdjustment, var20, var2.m_initialAdjustment);
            }

            var2.m_isInitialAdjustmentCalculated = true;
         }

         var2.get(var1, var4, var5, var6);
         var4.x += var2.m_initialAdjustment.x;
         var4.y -= var2.m_initialAdjustment.z;
      } else if (var3 == null) {
         var2.get(var1, var4, var5, var6);
      } else {
         Matrix4f var7 = AnimationPlayer.L_getTrackTransform.result;
         SkinningBone var8 = var3.getBoneA();
         var9 = getUnweightedBoneTransform(var2, var8.Index, AnimationPlayer.L_getTrackTransform.Pa);
         SkinningBone var10 = var8.Parent;
         SkinningBone var11 = var3.getBoneB();
         Matrix4f var12 = this.getBoneModelTransform(var10.Index, AnimationPlayer.L_getTrackTransform.mA);
         Matrix4f var13 = Matrix4f.invert(var12, AnimationPlayer.L_getTrackTransform.mAinv);
         Matrix4f var14 = this.getBoneModelTransform(var11.Index, AnimationPlayer.L_getTrackTransform.mB);
         Matrix4f var15 = this.getUnweightedModelTransform(var2, var10.Index, AnimationPlayer.L_getTrackTransform.umA);
         Matrix4f var16 = this.getUnweightedModelTransform(var2, var11.Index, AnimationPlayer.L_getTrackTransform.umB);
         Matrix4f var17 = Matrix4f.invert(var16, AnimationPlayer.L_getTrackTransform.umBinv);
         Matrix4f.mul(var9, var15, var7);
         Matrix4f.mul(var7, var17, var7);
         Matrix4f.mul(var7, var14, var7);
         Matrix4f.mul(var7, var13, var7);
         HelperFunctions.getPosition(var7, var4);
         HelperFunctions.getRotation(var7, var5);
         var6.set(1.0F, 1.0F, 1.0F);
      }
   }

   public boolean isBoneReparented(int var1) {
      return PZArrayUtil.contains((List)this.m_reparentedBoneBindings, Lambda.predicate(var1, (var0, var1x) -> {
         return var0.getBoneIdxA() == var1x;
      }));
   }

   private void initRagdollController() {
      if (this.m_ragdollController == null) {
         RagdollController var1 = RagdollController.alloc();
         var1.setGameCharacterObject(this.getIsoGameCharacter());
         this.m_ragdollController = var1;
      }
   }

   public boolean isRagdolling() {
      RagdollController var1 = this.getRagdollController();
      if (var1 == null) {
         return false;
      } else {
         return var1.isInitialized();
      }
   }

   public RagdollController getRagdollController() {
      return this.m_ragdollController;
   }

   private RagdollController getOrCreateRagdollController() {
      this.initRagdollController();
      return this.getRagdollController();
   }

   public void stopAll() {
      this.getMultiTrack().reset();
      this.releaseRagdollController();
   }

   public void releaseRagdollController() {
      this.m_ragdollController = (RagdollController)Pool.tryRelease((IPooledObject)this.m_ragdollController);
   }

   public AnimationClip getRagdollSimulationAnimationClip() {
      return this.m_ragdollAnimationClip;
   }

   public void setIsoGameCharacter(IsoGameCharacter var1) {
      this.m_character = var1;
   }

   public IsoGameCharacter getIsoGameCharacter() {
      return this.m_character;
   }

   public int getModelTransformsCount() {
      return PZArrayUtil.lengthOf(this.m_modelTransforms);
   }

   public Matrix4f getModelTransformAt(int var1) {
      return this.m_modelTransforms[var1];
   }

   public float getBoneTransformsTimeDelta() {
      return this.m_boneTransformsTimeDelta;
   }

   public void updateMultiTrackBoneTransforms_DeferredMovementOnly() {
      this.m_deferredMovementFromRagdoll.set(0.0F, 0.0F);
      if (this.parentPlayer == null) {
         this.updateLayerBlendWeightings();
         if (this.m_totalAnimBlendCount != 0) {
            int[] var1 = AnimationPlayer.updateMultiTrackBoneTransforms_DeferredMovementOnly.boneIndices;
            int var2 = 0;
            List var3 = this.m_multiTrack.getTracks();
            int var4 = var3.size();

            int var5;
            for(var5 = 0; var5 < var4; ++var5) {
               AnimationTrack var6 = (AnimationTrack)var3.get(var5);
               int var7 = var6.getDeferredMovementBoneIdx();
               if (var7 != -1 && !PZArrayUtil.contains(var1, var2, var7)) {
                  var1[var2++] = var7;
               }
            }

            for(var5 = 0; var5 < var2; ++var5) {
               this.updateBoneAnimationTransform(var1[var5], (AnimationBoneBindingPair)null);
            }

         }
      }
   }

   public boolean isRecording() {
      return this.m_recorder != null && this.m_recorder.isRecording();
   }

   public void setRecorder(AnimationPlayerRecorder var1) {
      this.m_recorder = var1;
   }

   public AnimationPlayerRecorder getRecorder() {
      return this.m_recorder;
   }

   public void dismember(int var1) {
      this.dismembered.add(var1);
   }

   private void updateModelTransforms() {
      GameProfiler.getInstance().invokeAndMeasure("updateModelTransforms", this, AnimationPlayer::updateModelTransformsInternal);
   }

   private void updateModelTransformsInternal() {
      this.m_boneTransforms[0].getMatrix(this.m_modelTransforms[0]);

      for(int var1 = 1; var1 < this.m_modelTransforms.length; ++var1) {
         SkinningBone var2 = this.m_skinningData.getBoneAt(var1);
         SkinningBone var3 = var2.Parent;
         BoneTransform.mul(this.m_boneTransforms[var2.Index], this.m_modelTransforms[var3.Index], this.m_modelTransforms[var2.Index]);
      }

   }

   public Matrix4f getBoneModelTransform(int var1, Matrix4f var2) {
      Matrix4f var3 = AnimationPlayer.L_getBoneModelTransform.boneTransform;
      var2.setIdentity();
      SkinningBone var4 = this.m_skinningData.getBoneAt(var1);

      for(SkinningBone var5 = var4; var5 != null; var5 = var5.Parent) {
         this.getBoneTransform(var5.Index, var3);
         Matrix4f.mul(var2, var3, var2);
      }

      return var2;
   }

   public Matrix4f getBindPoseBoneModelTransform(int var1, Matrix4f var2) {
      Matrix4f var3 = AnimationPlayer.L_getBoneModelTransform.boneTransform;
      var2.setIdentity();
      SkinningBone var4 = this.m_skinningData.getBoneAt(var1);

      for(SkinningBone var5 = var4; var5 != null; var5 = var5.Parent) {
         var3.load((Matrix4f)this.m_skinningData.BindPose.get(var5.Index));
         Matrix4f.mul(var2, var3, var2);
      }

      return var2;
   }

   public Matrix4f getBoneTransform(int var1, Matrix4f var2) {
      this.m_boneTransforms[var1].getMatrix(var2);
      return var2;
   }

   public TwistableBoneTransform getBone(int var1) {
      return this.m_boneTransforms[var1];
   }

   public Matrix4f getUnweightedModelTransform(AnimationTrack var1, int var2, Matrix4f var3) {
      Matrix4f var4 = AnimationPlayer.L_getUnweightedModelTransform.boneTransform;
      var4.setIdentity();
      var3.setIdentity();
      SkinningBone var5 = this.m_skinningData.getBoneAt(var2);

      for(SkinningBone var6 = var5; var6 != null; var6 = var6.Parent) {
         getUnweightedBoneTransform(var1, var6.Index, var4);
         Matrix4f.mul(var3, var4, var3);
      }

      return var3;
   }

   public static Matrix4f getUnweightedBoneTransform(AnimationTrack var0, int var1, Matrix4f var2) {
      Vector3f var3 = AnimationPlayer.L_getUnweightedBoneTransform.pos;
      Quaternion var4 = AnimationPlayer.L_getUnweightedBoneTransform.rot;
      Vector3f var5 = AnimationPlayer.L_getUnweightedBoneTransform.scale;
      var0.get(var1, var3, var4, var5);
      HelperFunctions.CreateFromQuaternionPositionScale(var3, var4, var5, var2);
      return var2;
   }

   public void UpdateSkinTransforms() {
      this.resetSkinTransforms();
   }

   public Matrix4f[] getSkinTransforms(SkinningData var1) {
      if (var1 == null) {
         return this.m_modelTransforms;
      } else {
         SkinTransformData var2 = this.getSkinTransformData(var1);
         Matrix4f[] var3 = var2.transforms;
         if (var2.dirty) {
            var2.checkBoneMap(this.getSkinningData());

            for(int var4 = 0; var4 < this.m_modelTransforms.length; ++var4) {
               int var5 = var2.m_boneMap[var4];
               if (var5 != -1) {
                  if (var1.BoneOffset != null && var1.BoneOffset.get(var5) != null) {
                     Matrix4f.mul((Matrix4f)var1.BoneOffset.get(var5), this.m_modelTransforms[var4], var3[var5]);
                  } else {
                     var3[var5].setIdentity();
                  }
               }
            }

            var2.dirty = false;
         }

         return var3;
      }
   }

   public Vector2 getDeferredMovement(Vector2 var1, boolean var2) {
      synchronized(this.m_deferredMovementLock) {
         var1.set(this.m_deferredMovement);
      }

      if (var2) {
         synchronized(this.m_deferredMovementAccumLock) {
            this.m_deferredMovementAccum.set(0.0F, 0.0F);
         }
      }

      return var1;
   }

   public Vector2 getDeferredMovementFromRagdoll(Vector2 var1) {
      return var1.set(this.m_deferredMovementFromRagdoll);
   }

   public float getDeferredAngleDelta() {
      return this.m_deferredAngleDelta;
   }

   public float getDeferredRotationWeight() {
      return this.m_deferredRotationWeight;
   }

   public Vector3 getTargetGrapplePos(Vector3 var1) {
      var1.set(this.m_targetGrapplePos);
      return var1;
   }

   public zombie.iso.Vector3 getTargetGrapplePos(zombie.iso.Vector3 var1) {
      var1.set(this.m_targetGrapplePos.x, this.m_targetGrapplePos.y, this.m_targetGrapplePos.z);
      return var1;
   }

   public void setTargetGrapplePos(float var1, float var2, float var3) {
      this.m_targetGrapplePos.set(var1, var2, var3);
   }

   public void setTargetGrappleRotation(float var1, float var2) {
      this.m_targetGrappleRotation.set(var1, var2);
   }

   public Vector2 getTargetGrappleRotation(Vector2 var1) {
      var1.set(this.m_targetGrappleRotation);
      return var1;
   }

   public Vector3 getGrappleOffset(Vector3 var1) {
      var1.set(this.m_grappleOffset);
      return var1;
   }

   public zombie.iso.Vector3 getGrappleOffset(zombie.iso.Vector3 var1) {
      var1.set(this.m_grappleOffset.x, this.m_grappleOffset.y, this.m_grappleOffset.z);
      return var1;
   }

   public void setGrappleOffset(float var1, float var2, float var3) {
      this.m_grappleOffset.set(var1, var2, var3);
   }

   public AnimationMultiTrack getMultiTrack() {
      return this.m_multiTrack;
   }

   public void setRecording(boolean var1) {
      this.m_recorder.setRecording(var1);
   }

   public void discardRecording() {
      if (this.m_recorder != null) {
         this.m_recorder.discardRecording();
      }

   }

   public float getRenderedAngle() {
      return this.m_angle + 1.5707964F;
   }

   public float getAngle() {
      return this.m_angle;
   }

   public void setAngle(float var1) {
      this.m_angle = var1;
   }

   public void setAngleToTarget() {
      this.setAngle(this.m_targetAngle);
   }

   public void setTargetToAngle() {
      float var1 = this.getAngle();
      this.setTargetAngle(var1);
   }

   public float getTargetAngle() {
      return this.m_targetAngle;
   }

   public void setTargetAngle(float var1) {
      this.m_targetAngle = var1;
   }

   public float getMaxTwistAngle() {
      return this.m_maxTwistAngle;
   }

   public void setMaxTwistAngle(float var1) {
      this.m_maxTwistAngle = var1;
   }

   public float getExcessTwistAngle() {
      return this.m_excessTwist;
   }

   public float getTwistAngle() {
      return this.m_twistAngle;
   }

   public float getShoulderTwistAngle() {
      return this.m_shoulderTwistAngle;
   }

   public float getTargetTwistAngle() {
      return this.m_targetTwistAngle;
   }

   public float getIKAimingLeftArmWeight() {
      return this.m_multiTrack.getIKAimingLeftArmWeight();
   }

   public float getIKAimingRightArmWeight() {
      return this.m_multiTrack.getIKAimingRightArmWeight();
   }

   private static class SkinTransformData extends PooledObject {
      public Matrix4f[] transforms;
      private SkinningData m_skinnedTo;
      public boolean dirty;
      private SkinningData m_animPlayerSkinningData;
      private int[] m_boneMap;
      private SkinTransformData m_next;
      private static final Pool<SkinTransformData> s_pool = new Pool(SkinTransformData::new);

      private SkinTransformData() {
      }

      public void setSkinnedTo(SkinningData var1) {
         if (this.m_skinnedTo != var1) {
            this.dirty = true;
            this.m_skinnedTo = var1;
            this.transforms = (Matrix4f[])PZArrayUtil.newInstance(Matrix4f.class, this.transforms, var1.numBones(), Matrix4f::new);
            this.m_animPlayerSkinningData = null;
         }
      }

      public void checkBoneMap(SkinningData var1) {
         if (this.m_animPlayerSkinningData != var1) {
            this.m_animPlayerSkinningData = var1;
            int var2 = var1.numBones();
            if (this.m_boneMap == null || this.m_boneMap.length < var2) {
               this.m_boneMap = new int[var2];
            }

            for(int var3 = 0; var3 < var2; ++var3) {
               SkinningBone var4 = var1.getBoneAt(var3);
               Integer var5 = (Integer)this.m_skinnedTo.BoneIndices.get(var4.Name);
               if (var5 == null) {
                  this.m_boneMap[var3] = -1;
               } else {
                  this.m_boneMap[var3] = var5;
               }
            }

         }
      }

      public static SkinTransformData alloc(SkinningData var0) {
         SkinTransformData var1 = (SkinTransformData)s_pool.alloc();
         var1.setSkinnedTo(var0);
         var1.dirty = true;
         return var1;
      }
   }

   private static final class L_setTwistBones {
      static final ArrayList<String> boneNames = new ArrayList();

      private L_setTwistBones() {
      }
   }

   private static class L_applyTwistBone {
      static final Matrix4f twistParentBoneTrans = new Matrix4f();
      static final Matrix4f twistParentBoneTransInv = new Matrix4f();
      static final Matrix4f twistBoneTrans = new Matrix4f();
      static final Vector3f twistBonePos = new Vector3f();
      static final Matrix4f twistBoneNewTrans = new Matrix4f();
      static final Vector3f twistRotateAxis = new Vector3f();
      static final Vector3f forward = new Vector3f();
      static final Quaternion twistTurnRot = new Quaternion();
      static final Vector3f desiredForward = new Vector3f();

      private L_applyTwistBone() {
      }
   }

   private static final class L_updateBoneAnimationTransform {
      static final Quaternion rot = new Quaternion();
      static final Vector3f pos = new Vector3f();
      static final Vector3f scale = new Vector3f();
      static final Keyframe key = new Keyframe(new Vector3f(0.0F, 0.0F, 0.0F), new Quaternion(0.0F, 0.0F, 0.0F, 1.0F), new Vector3f(1.0F, 1.0F, 1.0F));
      static final Matrix4f boneMat = new Matrix4f();
      static final Matrix4f rotMat = new Matrix4f();
      static final Vector3f rotAxis = new Vector3f(1.0F, 0.0F, 0.0F);
      static final Quaternion crRot = new Quaternion();
      static final Vector4f crRotAA = new Vector4f();
      static final Matrix4f crMat = new Matrix4f();
      static final Vector3f rotEulers = new Vector3f();
      static final Vector3f deferredPos = new Vector3f();

      private L_updateBoneAnimationTransform() {
      }
   }

   private static final class L_getTrackTransform {
      static final Matrix4f Pa = new Matrix4f();
      static final Matrix4f mA = new Matrix4f();
      static final Matrix4f mB = new Matrix4f();
      static final Matrix4f umA = new Matrix4f();
      static final Matrix4f umB = new Matrix4f();
      static final Matrix4f mAinv = new Matrix4f();
      static final Matrix4f umBinv = new Matrix4f();
      static final Matrix4f result = new Matrix4f();

      private L_getTrackTransform() {
      }
   }

   private static final class updateMultiTrackBoneTransforms_DeferredMovementOnly {
      static int[] boneIndices = new int[60];

      private updateMultiTrackBoneTransforms_DeferredMovementOnly() {
      }
   }

   private static class L_getBoneModelTransform {
      static final Matrix4f boneTransform = new Matrix4f();

      private L_getBoneModelTransform() {
      }
   }

   private static class L_getUnweightedModelTransform {
      static final Matrix4f boneTransform = new Matrix4f();

      private L_getUnweightedModelTransform() {
      }
   }

   private static class L_getUnweightedBoneTransform {
      static final Vector3f pos = new Vector3f();
      static final Quaternion rot = new Quaternion();
      static final Vector3f scale = new Vector3f();

      private L_getUnweightedBoneTransform() {
      }
   }
}
