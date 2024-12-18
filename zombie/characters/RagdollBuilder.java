package zombie.characters;

import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;
import zombie.core.physics.Bullet;
import zombie.core.physics.RagdollBodyPart;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.animation.AnimationClip;
import zombie.core.skinnedmodel.animation.Keyframe;
import zombie.core.skinnedmodel.model.AnimationAsset;
import zombie.core.skinnedmodel.model.SkeletonBone;
import zombie.core.skinnedmodel.model.SkinningBone;
import zombie.core.skinnedmodel.model.SkinningBoneHierarchy;
import zombie.debug.DebugLog;
import zombie.network.GameServer;

public final class RagdollBuilder {
   public static final RagdollBuilder instance = new RagdollBuilder();
   private static float[] boneTransformData = new float[245];
   private boolean initialized = false;
   private AnimationAsset tPoseAnimationAsset = null;
   public AnimationClip tPoseAnimationClip = null;
   public int leftUpperLegBoneIndex = 0;
   public int rightUpperLegBoneIndex = 0;
   public int pelvisBoneIndex = 0;
   public int headBoneIndex = 0;
   private float mass = 70.0F;
   private float friction = 1.5F;
   private float rollingFriction = 0.5F;
   private static int[] boneHierarchy = new int[245];
   private final Vector3f s_position = new Vector3f();
   private final Quaternion s_rotation = new Quaternion();

   public RagdollBuilder() {
   }

   public boolean isInitialized() {
      return this.initialized;
   }

   public boolean Initialize() {
      if (this.tPoseAnimationAsset == null) {
         this.tPoseAnimationAsset = ModelManager.instance.getAnimationAsset("bob/bob_tpose");
         if (this.tPoseAnimationAsset == null) {
            return false;
         }
      }

      if (this.tPoseAnimationClip == null) {
         this.tPoseAnimationClip = ModelManager.instance.getAnimationClip("Bob_TPose");
         if (this.tPoseAnimationClip == null) {
            return false;
         }
      }

      this.initialized = true;
      int var1 = this.getNumBones();
      if (boneHierarchy.length > var1 * 7) {
         boneHierarchy = new int[var1 * 7];
      }

      if (boneTransformData.length > var1 * 7) {
         boneTransformData = new float[var1 * 7];
      }

      this.getSkinningDataSkeletonHierarchy();
      if (!GameServer.bServer) {
         Bullet.initializeRagdollSkeleton(var1, boneHierarchy);
      }

      this.initializeRagdollPose();
      this.leftUpperLegBoneIndex = this.tPoseAnimationAsset.skinningData.getBone("Bip01_L_Thigh").Index;
      this.rightUpperLegBoneIndex = this.tPoseAnimationAsset.skinningData.getBone("Bip01_R_Thigh").Index;
      this.pelvisBoneIndex = this.tPoseAnimationAsset.skinningData.getBone("Bip01_Pelvis").Index;
      this.headBoneIndex = this.tPoseAnimationAsset.skinningData.getBone("Bip01_Head").Index;
      return this.initialized;
   }

   private void getSkinningDataSkeletonHierarchy() {
      SkeletonBone[] var1 = SkeletonBone.all();
      SkinningBoneHierarchy var2 = this.getSkeletonBoneHiearchy();
      SkeletonBone[] var3 = var1;
      int var4 = var1.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         SkeletonBone var6 = var3[var5];
         SkinningBone var7 = var2.getBone(var6);
         if (var7 == null) {
            boneHierarchy[var6.index()] = -1;
         } else {
            SkeletonBone var8 = var7.getParentSkeletonBone();
            boneHierarchy[var6.index()] = var8.index();
         }
      }

   }

   public void initializeRagdollPose() {
      this.tPoseAnimationClip = ModelManager.instance.getAnimationClip("Bob_TPose");
      if (!GameServer.bServer) {
         this.getBoneTransforms();
         Bullet.initializeRagdollPose(this.getNumBones(), boneTransformData, 0.0F, 0.0F, 0.0F);
      }
   }

   private void getBoneTransforms() {
      int var1 = 0;
      SkeletonBone[] var2 = SkeletonBone.all();
      SkinningBoneHierarchy var3 = this.getSkeletonBoneHiearchy();
      SkeletonBone[] var4 = var2;
      int var5 = var2.length;

      for(int var6 = 0; var6 < var5; ++var6) {
         SkeletonBone var7 = var4[var6];
         SkinningBone var8 = var3.getBone(var7);
         if (var8 == null) {
            boneTransformData[var1++] = 0.0F;
            boneTransformData[var1++] = 0.0F;
            boneTransformData[var1++] = 0.0F;
            boneTransformData[var1++] = 0.0F;
            boneTransformData[var1++] = 0.0F;
            boneTransformData[var1++] = 0.0F;
            boneTransformData[var1++] = 1.0F;
         } else {
            int var9 = var8.Index;
            Keyframe[] var10 = this.tPoseAnimationClip.getBoneFramesAt(var9);
            Keyframe var11 = var10[1];
            var11.get(this.s_position, this.s_rotation, (Vector3f)null);
            Vector3f var10000 = this.s_position;
            var10000.x *= -1.0F;
            var10000 = this.s_position;
            var10000.y *= -1.0F;
            var10000 = this.s_position;
            var10000.z *= -1.0F;
            boneTransformData[var1++] = this.s_position.x * 1.5F;
            boneTransformData[var1++] = this.s_position.y * 1.5F;
            boneTransformData[var1++] = this.s_position.z * 1.5F;
            boneTransformData[var1++] = this.s_rotation.x;
            boneTransformData[var1++] = this.s_rotation.y;
            boneTransformData[var1++] = this.s_rotation.z;
            boneTransformData[var1++] = this.s_rotation.w;
         }
      }

   }

   public boolean isSkeletonBoneHiearchyInitalized() {
      return this.tPoseAnimationAsset != null;
   }

   public SkinningBoneHierarchy getSkeletonBoneHiearchy() {
      return this.tPoseAnimationAsset.skinningData.getSkeletonBoneHiearchy();
   }

   private int getNumBones() {
      return SkeletonBone.count();
   }

   public void setFriction(int var1, float var2, float var3) {
      for(int var4 = 0; var4 < RagdollBodyPart.BODYPART_COUNT.ordinal(); ++var4) {
         float var5 = Bullet.getBodyPartFriction(var1, var4);
         DebugLog.Physics.println("%s Friction: %f", RagdollBodyPart.values()[var4].name(), var5);
         float var6 = Bullet.getBodyPartRollingFriction(var1, var4);
         DebugLog.Physics.println("%s Rolling Friction: %f", RagdollBodyPart.values()[var4].name(), var6);
         Bullet.setBodyPartFriction(var1, var4, var2, var3);
      }

   }

   public float getMass() {
      return this.mass;
   }

   public void setMass(float var1) {
      if (this.mass != var1) {
         Bullet.setRagdollMass(var1);
      }

      this.mass = var1;
   }

   public float getFriction() {
      return this.friction;
   }

   public float getRollingFriction() {
      return this.rollingFriction;
   }

   public void setFriction(float var1, float var2) {
      if (this.friction != var1 || this.rollingFriction != var2) {
         Bullet.setRagdollFriction(var1, var2);
      }

      this.friction = var1;
      this.rollingFriction = var2;
   }
}
