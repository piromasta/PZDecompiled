package zombie.core.physics;

import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;
import zombie.GameTime;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.RagdollBuilder;
import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.HelperFunctions;
import zombie.core.skinnedmodel.animation.AnimationClip;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.animation.AnimatorsBoneTransform;
import zombie.core.skinnedmodel.animation.BoneTransform;
import zombie.core.skinnedmodel.animation.Keyframe;
import zombie.core.skinnedmodel.model.Model;
import zombie.core.skinnedmodel.model.SkeletonBone;
import zombie.core.skinnedmodel.model.SkinningBone;
import zombie.core.skinnedmodel.model.SkinningBoneHierarchy;
import zombie.iso.Vector2;
import zombie.iso.Vector3;
import zombie.util.Pool;
import zombie.util.PooledObject;
import zombie.util.Type;

public final class RagdollController extends PooledObject {
   public static final float BulletZtoPZWorldScale = 0.4065F;
   public static final float MovementThreshold = 0.01F;
   public static final float MovementThresholdTime = 1.5F;
   private final float[] m_floatBuffer = new float[245];
   private boolean m_isInitialized = false;
   private boolean m_isUpright = true;
   private boolean m_isOnBack = false;
   private final Vector3 m_headPosition = new Vector3();
   private final Vector3 m_pelvisPosition = new Vector3();
   private final Vector3 m_previousHeadPosition = new Vector3();
   private final Vector3 m_previousPelvisPosition = new Vector3();
   private boolean m_addedToWorld = false;
   private final RagdollControllerDebugRenderer.DebugDrawSettings m_debugDrawSettings = new RagdollControllerDebugRenderer.DebugDrawSettings();
   private int m_simulationState = -1;
   private IsoGameCharacter m_gameCharacterObject = null;
   private final RagdollStateData m_ragdollStateData = new RagdollStateData();
   private Keyframe[] m_keyframesForBone = null;
   private final Vector3f m_ragdollWorldPosition = new Vector3f();
   private final Quaternion m_ragdollWorldRotation = new Quaternion();
   private final Quaternion m_ragdollLocalRotation = new Quaternion();
   private final Reusables_Quaternions l_quaternions = new Reusables_Quaternions();
   private final Reusables_Vector3f l_vector3fs = new Reusables_Vector3f();
   private final float simulationTimeoutDecayFactor = 10.0F;
   private static final Pool<RagdollController> ragdollControllerPool = new Pool(RagdollController::new);

   private RagdollController() {
   }

   public static RagdollController alloc() {
      return (RagdollController)ragdollControllerPool.alloc();
   }

   public RagdollStateData getRagdollStateData() {
      return this.m_ragdollStateData;
   }

   public boolean isIsoPlayer() {
      return Type.tryCastTo(this.getGameCharacterObject(), IsoPlayer.class) != null;
   }

   public boolean isSimulationSleeping() {
      return this.m_simulationState == RagdollController.SimulationState.ISLAND_SLEEPING.ordinal();
   }

   public boolean isSimulationActive() {
      return this.m_ragdollStateData.isSimulating;
   }

   public IsoGameCharacter getGameCharacterObject() {
      return this.m_gameCharacterObject;
   }

   public void setGameCharacterObject(IsoGameCharacter var1) {
      this.m_gameCharacterObject = var1;
   }

   public int getID() {
      return this.getGameCharacterObject().getID();
   }

   public RagdollControllerDebugRenderer.DebugDrawSettings getDebugDrawSettings() {
      return this.m_debugDrawSettings;
   }

   public boolean isInitialized() {
      return this.m_isInitialized;
   }

   public boolean isUpright() {
      return this.m_isUpright;
   }

   public void setUpright(boolean var1) {
      this.m_isUpright = var1;
   }

   public boolean isOnBack() {
      return this.m_isOnBack;
   }

   public void setOnBack(boolean var1) {
      this.m_isOnBack = var1;
   }

   public Vector3 getHeadPosition(Vector3 var1) {
      var1.set(this.m_headPosition);
      return var1;
   }

   public void setHeadPosition(Vector3 var1) {
      this.m_headPosition.set(var1);
   }

   public Vector3 getPelvisPosition(Vector3 var1) {
      var1.set(this.m_pelvisPosition);
      return var1;
   }

   public float getPelvisPositionX() {
      return this.m_pelvisPosition.x;
   }

   public float getPelvisPositionY() {
      return this.m_pelvisPosition.y;
   }

   public float getPelvisPositionZ() {
      return this.m_pelvisPosition.z;
   }

   public void setPelvisPosition(Vector3 var1) {
      this.m_pelvisPosition.set(var1);
   }

   private boolean initialize() {
      if (this.isInitialized()) {
         return true;
      } else if (this.getAnimationPlayer() != null && this.getAnimationPlayer().isReady()) {
         this.m_ragdollStateData.reset();
         this.m_ragdollStateData.isSimulating = true;
         this.addToWorld();
         this.updateRagdollSkeleton();
         this.setActive(true);
         return true;
      } else {
         return false;
      }
   }

   private void reset() {
      this.removeFromWorld();
      this.m_ragdollStateData.reset();
      this.m_isUpright = true;
      this.m_isOnBack = false;
      this.m_simulationState = -1;
      this.m_gameCharacterObject.setRagdollFall(false);
      this.m_isInitialized = false;
   }

   public void reinitialize() {
      this.reset();
      this.m_isInitialized = this.initialize();
   }

   public static Vector3f pzSpaceToBulletSpace(Vector3f var0) {
      float var1 = var0.x;
      float var2 = var0.y;
      float var3 = var0.z;
      var0.x = var1;
      var0.y = var3 * 2.46F;
      var0.z = var2;
      return var0;
   }

   public void setActive(boolean var1) {
      this.updateRagdollWorldTransform();
      Bullet.setRagdollActive(this.getID(), var1);
   }

   public void addToWorld() {
      if (!this.m_addedToWorld) {
         this.calculateRagdollWorldTransform(this.m_ragdollWorldPosition, this.m_ragdollWorldRotation);
         Bullet.addRagdoll(this.getID(), this.m_ragdollWorldPosition, this.m_ragdollWorldRotation);
         this.m_addedToWorld = true;
      }
   }

   private void removeFromWorld() {
      if (this.m_addedToWorld) {
         Bullet.removeRagdoll(this.getID());
         this.m_addedToWorld = false;
      }
   }

   public void updateRagdollSkeleton() {
      int var1 = this.getID();
      this.setRagdollLocalRotation();
      this.updateRagdollWorldTransform();
      this.uploadAnimationBoneTransformsToRagdoll(var1);
      this.uploadAnimationBonePreviousTransformsToRagdoll(var1);
   }

   private void uploadAnimationBoneTransformsToRagdoll(int var1) {
      this.getBoneTransformsFromAnimation(this.m_floatBuffer);
      Bullet.updateRagdollSkeletonTransforms(var1, this.getNumberOfBones(), this.m_floatBuffer);
   }

   private void uploadAnimationBonePreviousTransformsToRagdoll(int var1) {
      AnimationPlayer var2 = this.getAnimationPlayer();
      float var3 = var2.getBoneTransformsTimeDelta();
      if (!(var3 <= 0.0F)) {
         this.getBoneTransformVelocitiesFromAnimation(this.m_floatBuffer);
         Bullet.updateRagdollSkeletonPreviousTransforms(var1, this.getNumberOfBones(), var3, this.m_floatBuffer);
      }
   }

   public void update(float var1) {
      if (!this.isInitialized()) {
         this.m_isInitialized = this.initialize();
      } else {
         RagdollStateData var2 = this.getRagdollStateData();
         this.simulateRagdoll();
         this.updateSimulationStateID();
         this.calculateSimulationData(var1);
         this.simulateHitReaction();
         this.updateSimulationTimeout(var2);
         RagdollControllerDebugRenderer.updateDebug(this);
      }
   }

   private void updateSimulationTimeout(RagdollStateData var1) {
      boolean var2 = false;
      if (var1.simulationTimeout > 0.0F) {
         var1.simulationTimeout -= GameTime.getInstance().getTimeDelta();
      }

      if (!var1.isSimulationMovement && var1.simulationTimeout <= 0.0F) {
         var2 = true;
      }

      if (this.isSimulationSleeping() || var2) {
         var1.isSimulating = false;
         this.setActive(false);
      }

   }

   private void simulateHitReaction() {
      BallisticsTarget var1 = this.m_gameCharacterObject.getBallisticsTarget();
      if (var1 != null && !var1.getCombatDamageDateProcessed()) {
         BallisticsTarget.CombatDamageData var2 = var1.getCombatDamageData();
         if (var2 != null && var2.bodyPart != RagdollBodyPart.BODYPART_COUNT) {
            RagdollJoint var3 = RagdollJoint.JOINT_COUNT;
            String var4 = this.m_gameCharacterObject.getHitReaction();
            RagdollSettingsManager var5 = RagdollSettingsManager.getInstance();
            if (var5.isForcedHitReaction()) {
               var4 = var5.getForcedHitReactionLocationAsShotLocation();
            }

            RagdollBodyPart var6 = var2.bodyPart;
            this.m_gameCharacterObject.setHitReaction("");
            float var7 = 0.0F;
            float var8 = 0.0F;
            if (var5.isForcedHitReaction()) {
               RagdollSettingsManager.HitReactionSetting var9 = var5.getHitReactionSetting(0);
               var7 = var5.getGlobalImpulseSetting();
               var8 = var5.getGlobalUpImpulseSetting();
               if (!var9.isEnableAdmin()) {
                  boolean var10 = var5.getEnabledSetting(var6);
                  if (var10) {
                     var7 = var5.getImpulseSetting(var6);
                     var8 = var5.getUpImpulseSetting(var6);
                  }
               }
            } else {
               var7 = var5.getSandboxHitReactionImpulseStrength();
               var8 = var5.getSandboxHitReactionUpImpulseStrength();
            }

            Vector3 var12 = new Vector3();
            Vector3 var13 = new Vector3();
            Vector3 var11 = new Vector3();
            var2.target.getPosition(var13);
            var2.attacker.getPosition(var11);
            var13.sub(var11, var12);
            var12.normalize();
            this.m_floatBuffer[0] = var12.x * var7;
            this.m_floatBuffer[1] = var8;
            this.m_floatBuffer[2] = var12.y * var7;
            this.m_floatBuffer[3] = 0.0F;
            this.m_floatBuffer[4] = 0.0F;
            this.m_floatBuffer[5] = 0.0F;
            Bullet.applyImpulse(this.m_gameCharacterObject.getID(), var6.ordinal(), this.m_floatBuffer);
            var1.setCombatDamageDataProcessed(true);
         }
      }
   }

   private void simulateHitReaction0() {
      if (this.m_gameCharacterObject.hasHitReaction()) {
         boolean var1 = false;
         RagdollJoint var2 = RagdollJoint.JOINT_COUNT;
         String var3 = this.m_gameCharacterObject.getHitReaction();
         RagdollSettingsManager var4 = RagdollSettingsManager.getInstance();
         if (var4.isForcedHitReaction()) {
            var3 = var4.getForcedHitReactionLocationAsShotLocation();
         }

         RagdollBodyPart var5 = RagdollBodyPart.BODYPART_PELVIS;
         switch (var3) {
            case "ShotBelly":
            case "ShotBellyStep":
               var5 = RagdollBodyPart.BODYPART_PELVIS;
               if (var1) {
                  var2 = RagdollJoint.JOINT_PELVIS_SPINE;
               }
               break;
            case "ShotChest":
            case "ShotChestR":
            case "ShotChestL":
               var5 = RagdollBodyPart.BODYPART_SPINE;
               if (var1) {
                  var2 = RagdollJoint.JOINT_SPINE_HEAD;
               }
               break;
            case "ShotLegR":
               var5 = RagdollBodyPart.BODYPART_RIGHT_UPPER_LEG;
               if (var1) {
                  var2 = RagdollJoint.JOINT_RIGHT_KNEE;
               }
               break;
            case "ShotLegL":
               var5 = RagdollBodyPart.BODYPART_LEFT_UPPER_LEG;
               if (var1) {
                  var2 = RagdollJoint.JOINT_LEFT_KNEE;
               }
               break;
            case "ShotShoulderStepR":
               var5 = RagdollBodyPart.BODYPART_RIGHT_UPPER_ARM;
               if (var1) {
                  var2 = RagdollJoint.JOINT_RIGHT_ELBOW;
               }
               break;
            case "ShotShoulderStepL":
               var5 = RagdollBodyPart.BODYPART_LEFT_UPPER_ARM;
               if (var1) {
                  var2 = RagdollJoint.JOINT_LEFT_ELBOW;
               }
               break;
            case "ShotHeadFwd":
            case "ShotHeadBwd":
            case "ShotHeadFwd02":
               var5 = RagdollBodyPart.BODYPART_HEAD;
               if (var1) {
                  var2 = RagdollJoint.JOINT_SPINE_HEAD;
               }
               break;
            default:
               return;
         }

         this.m_gameCharacterObject.setHitReaction("");
         float var6 = 0.0F;
         float var15 = 0.0F;
         if (var4.isForcedHitReaction()) {
            RagdollSettingsManager.HitReactionSetting var8 = var4.getHitReactionSetting(0);
            var6 = var4.getGlobalImpulseSetting();
            var15 = var4.getGlobalUpImpulseSetting();
            if (!var8.isEnableAdmin()) {
               boolean var9 = var4.getEnabledSetting(var5);
               if (var9) {
                  var6 = var4.getImpulseSetting(var5);
                  var15 = var4.getUpImpulseSetting(var5);
               }
            }
         } else {
            var6 = var4.getSandboxHitReactionImpulseStrength();
            var15 = var4.getSandboxHitReactionUpImpulseStrength();
         }

         Vector3 var13 = new Vector3();
         BallisticsTarget var14 = this.m_gameCharacterObject.getBallisticsTarget();
         if (var14 != null) {
            Vector3 var10 = new Vector3();
            Vector3 var11 = new Vector3();
            BallisticsTarget.CombatDamageData var12 = var14.getCombatDamageData();
            var12.target.getPosition(var10);
            var12.attacker.getPosition(var11);
            var10.sub(var11, var13);
            var13.normalize();
         }

         this.m_floatBuffer[0] = var13.x * var6;
         this.m_floatBuffer[1] = var13.z * var15;
         this.m_floatBuffer[2] = var13.y * var6;
         this.m_floatBuffer[3] = 0.0F;
         this.m_floatBuffer[4] = 0.0F;
         this.m_floatBuffer[5] = 0.0F;
         Bullet.applyImpulse(this.m_gameCharacterObject.getID(), var5.ordinal(), this.m_floatBuffer);
         if (var1 & var2 != RagdollJoint.JOINT_COUNT) {
            Bullet.detachConstraint(this.m_gameCharacterObject.getID(), var2.ordinal());
            this.m_gameCharacterObject.getAnimationPlayer().dismember(this.getJointAssociatedBone(var2).ordinal());
         }

      }
   }

   private SkeletonBone getJointAssociatedBone(RagdollJoint var1) {
      SkeletonBone var2 = SkeletonBone.Dummy01;
      switch (var1) {
         case JOINT_PELVIS_SPINE:
            var2 = SkeletonBone.Bip01_Pelvis;
            break;
         case JOINT_SPINE_HEAD:
            var2 = SkeletonBone.Bip01_Neck;
            break;
         case JOINT_LEFT_HIP:
            var2 = SkeletonBone.Bip01_L_Thigh;
            break;
         case JOINT_LEFT_KNEE:
            var2 = SkeletonBone.Bip01_L_Calf;
            break;
         case JOINT_RIGHT_HIP:
            var2 = SkeletonBone.Bip01_R_Thigh;
            break;
         case JOINT_RIGHT_KNEE:
            var2 = SkeletonBone.Bip01_R_Calf;
            break;
         case JOINT_LEFT_SHOULDER:
            var2 = SkeletonBone.Bip01_L_UpperArm;
            break;
         case JOINT_LEFT_ELBOW:
            var2 = SkeletonBone.Bip01_L_Forearm;
            break;
         case JOINT_RIGHT_SHOULDER:
            var2 = SkeletonBone.Bip01_R_UpperArm;
            break;
         case JOINT_RIGHT_ELBOW:
            var2 = SkeletonBone.Bip01_R_Forearm;
      }

      return var2;
   }

   public void debugRender() {
   }

   public void simulateRagdoll() {
      this.updateRagdollWorldTransform();
      this.setRagdollLocalRotation();
      int var1 = Bullet.simulateRagdoll(this.getID(), this.m_floatBuffer);
      this.setBoneTransformsToAnimation(this.m_floatBuffer, var1);
   }

   private void setRagdollLocalRotation() {
      Quaternion var1 = this.getRagdollLocalRotation(this.m_ragdollLocalRotation);
      Bullet.setRagdollLocalTransformRotation(this.getID(), var1.x, var1.y, var1.z, var1.w);
   }

   private void updateRagdollWorldTransform() {
      int var1 = this.getID();
      this.calculateRagdollWorldTransform(this.m_ragdollWorldPosition, this.m_ragdollWorldRotation);
      Bullet.updateRagdoll(var1, this.m_ragdollWorldPosition, this.m_ragdollWorldRotation);
   }

   private void calculateRagdollWorldTransform(Vector3f var1, Quaternion var2) {
      IsoGameCharacter var3 = this.getGameCharacterObject();
      pzSpaceToBulletSpace(var3.getPosition(var1));
      float var4 = var3.getAnimAngleRadians();
      this.calculateRagdollWorldRotation(var4, var2);
   }

   private Quaternion calculateRagdollWorldRotation(float var1, Quaternion var2) {
      Quaternion var3 = PZMath.setFromAxisAngle(0.0F, 1.0F, 0.0F, -var1, this.l_quaternions.yAxis);
      var2.set(var3);
      return var2;
   }

   private Quaternion getRagdollLocalRotation(Quaternion var1) {
      Quaternion var2 = PZMath.setFromAxisAngle(1.0F, 0.0F, 0.0F, 3.1415927F, this.l_quaternions.xAxis);
      Quaternion var3 = PZMath.setFromAxisAngle(0.0F, 0.0F, 1.0F, 3.1415927F, this.l_quaternions.zAxis);
      Quaternion.mul(var3, var2, var1);
      Quaternion var4 = PZMath.setFromAxisAngle(0.0F, 0.0F, 1.0F, -1.5707964F, this.l_quaternions.yawAdjust);
      Quaternion.mul(var1, var4, var1);
      return var1;
   }

   public void updateSimulationStateID() {
      this.m_simulationState = Bullet.getRagdollSimulationState(this.getID());
   }

   private void setBoneTransformsToAnimation(float[] var1, int var2) {
      AnimationClip var3 = this.getRagdollSimulationAnimationClip();
      if (var3 != null) {
         SkinningBoneHierarchy var4 = this.getSkeletonBoneHierarchy();
         if (var4 != null) {
            Vector3f var5 = HelperFunctions.allocVector3f();
            Quaternion var6 = HelperFunctions.allocQuaternion();
            Vector3f var7 = HelperFunctions.allocVector3f(1.0F, 1.0F, 1.0F);
            SkeletonBone[] var8 = SkeletonBone.all();
            int var9 = 0;

            for(int var10 = 0; var10 < var2; ++var10) {
               SkeletonBone var11 = var8[var10];
               SkinningBone var12 = var4.getBone(var11);
               var5.x = var1[var9++] / 1.5F;
               var5.y = var1[var9++] / 1.5F;
               var5.z = var1[var9++] / 1.5F;
               float var13 = var1[var9++];
               float var14 = var1[var9++];
               float var15 = var1[var9++];
               float var16 = var1[var9++];
               var6.set(var13, var14, var15, var16);
               if (var12 != null) {
                  var5.x *= -1.0F;
                  var5.y *= -1.0F;
                  var5.z *= -1.0F;
                  int var17 = var12.Index;
                  this.setBoneKeyframePRS(var3, var17, var5, var6, var7);
               }
            }

            HelperFunctions.releaseVector3f(var5);
            HelperFunctions.releaseQuaternion(var6);
            HelperFunctions.releaseVector3f(var7);
         }
      }
   }

   private void getBoneTransformsFromAnimation(float[] var1) {
      AnimationPlayer var2 = this.getAnimationPlayer();
      SkinningBoneHierarchy var3 = this.getSkeletonBoneHierarchy();
      Vector3f var4 = HelperFunctions.allocVector3f();
      Quaternion var5 = HelperFunctions.allocQuaternion();
      int var6 = 0;
      SkeletonBone[] var7 = SkeletonBone.all();
      int var8 = var7.length;

      for(int var9 = 0; var9 < var8; ++var9) {
         SkeletonBone var10 = var7[var9];
         SkinningBone var11 = var3.getBone(var10);
         if (var11 != null) {
            int var12 = var11.Index;
            AnimatorsBoneTransform var13 = var2.getBoneTransformAt(var12);
            var13.getPosition(var4);
            var13.getRotation(var5);
         } else {
            var4.set(0.0F, 0.0F, 0.0F);
            var5.setIdentity();
         }

         var4.x *= -1.0F;
         var4.y *= -1.0F;
         var4.z *= -1.0F;
         var1[var6++] = var4.x * 1.5F;
         var1[var6++] = var4.y * 1.5F;
         var1[var6++] = var4.z * 1.5F;
         var1[var6++] = var5.x;
         var1[var6++] = var5.y;
         var1[var6++] = var5.z;
         var1[var6++] = var5.w;
      }

      HelperFunctions.releaseVector3f(var4);
      HelperFunctions.releaseQuaternion(var5);
   }

   private void getBoneTransformVelocitiesFromAnimation(float[] var1) {
      AnimationPlayer var2 = this.getAnimationPlayer();
      SkinningBoneHierarchy var3 = this.getSkeletonBoneHierarchy();
      Vector3f var4 = HelperFunctions.allocVector3f();
      Quaternion var5 = HelperFunctions.allocQuaternion();
      Vector3f var6 = HelperFunctions.allocVector3f();
      Quaternion var7 = HelperFunctions.allocQuaternion();
      Vector3f var8 = HelperFunctions.allocVector3f();
      Quaternion var9 = HelperFunctions.allocQuaternion();
      BoneTransform var10 = BoneTransform.alloc();
      int var11 = 0;
      SkeletonBone[] var12 = SkeletonBone.all();
      int var13 = var12.length;

      for(int var14 = 0; var14 < var13; ++var14) {
         SkeletonBone var15 = var12[var14];
         SkinningBone var16 = var3.getBone(var15);
         if (var16 != null) {
            int var17 = var16.Index;
            AnimatorsBoneTransform var18 = var2.getBoneTransformAt(var17);
            var18.getPosition(var6);
            var18.getRotation(var7);
            var18.getPreviousTransform(var10);
            var10.getPosition(var4);
            var10.getRotation(var5);
            float var19 = var18.getTimeDelta();
            var8.x = (var6.x - var4.x) / var19;
            var8.y = (var6.y - var4.y) / var19;
            var8.z = (var6.z - var4.z) / var19;
            var9.x = (var7.x - var5.x) / var19;
            var9.y = (var7.y - var5.y) / var19;
            var9.z = (var7.z - var5.z) / var19;
            var9.w = (var7.w - var5.w) / var19;
         } else {
            var8.set(0.0F, 0.0F, 0.0F);
            var9.setIdentity();
         }

         var8.x *= -1.0F;
         var8.y *= -1.0F;
         var8.z *= -1.0F;
         var1[var11++] = var8.x * 1.5F;
         var1[var11++] = var8.y * 1.5F;
         var1[var11++] = var8.z * 1.5F;
         var1[var11++] = var9.x;
         var1[var11++] = var9.y;
         var1[var11++] = var9.z;
         var1[var11++] = var9.w;
      }

      HelperFunctions.releaseVector3f(var4);
      HelperFunctions.releaseQuaternion(var5);
      HelperFunctions.releaseVector3f(var6);
      HelperFunctions.releaseQuaternion(var7);
      HelperFunctions.releaseVector3f(var8);
      HelperFunctions.releaseQuaternion(var9);
      var10.release();
   }

   private Keyframe[] getKeyframesForBone(int var1) {
      AnimationClip var2 = this.getRagdollSimulationAnimationClip();
      if (var2 == null) {
         return null;
      } else {
         SkinningBoneHierarchy var3 = this.getSkeletonBoneHierarchy();
         if (var3 == null) {
            return null;
         } else {
            Keyframe[] var4 = this.getKeyframesForBone(var2, var1);
            return var4;
         }
      }
   }

   private Keyframe[] getKeyframesForBone(AnimationClip var1, int var2) {
      this.m_keyframesForBone = var1.getKeyframesForBone(var2, this.m_keyframesForBone);

      assert this.m_keyframesForBone.length == 2;

      assert this.m_keyframesForBone[1].Bone == var2;

      return this.m_keyframesForBone;
   }

   private Keyframe getKeyframeForBone(AnimationClip var1, int var2) {
      Keyframe[] var3 = this.getKeyframesForBone(var1, var2);
      Keyframe var4 = var3[1];
      return var4;
   }

   private void setBoneKeyframePRS(AnimationClip var1, int var2, Vector3f var3, Quaternion var4, Vector3f var5) {
      Keyframe[] var6 = this.getKeyframesForBone(var1, var2);
      Keyframe var7 = var6[0];
      Keyframe var8 = var6[1];

      assert var7.Bone == var2 && var8.Bone == var2;

      var7.set(var8.Position, var8.Rotation, var8.Scale);
      var8.set(var3, var4, var5);
      if (var2 == SkeletonBone.Translation_Data.index()) {
         if (PZMath.equal(var3.lengthSquared(), 0.0F, 0.25F)) {
            var8.Position.scale(var3.length() / 0.5F);
         }

         var7.setIdentity();
      }

   }

   private AnimationClip getRagdollSimulationAnimationClip() {
      AnimationPlayer var1 = this.getAnimationPlayer();
      if (var1 == null) {
         return null;
      } else {
         AnimationClip var2 = var1.getRagdollSimulationAnimationClip();
         return var2;
      }
   }

   private SkinningBoneHierarchy getSkeletonBoneHierarchy() {
      AnimationPlayer var1 = this.getAnimationPlayer();
      return var1 == null ? null : var1.getSkeletonBoneHiearchy();
   }

   public void onReleased() {
      this.reset();
   }

   public int getNumberOfBones() {
      return SkeletonBone.count();
   }

   public AnimationPlayer getAnimationPlayer() {
      IsoGameCharacter var1 = this.getGameCharacterObject();
      return var1 == null ? null : var1.getAnimationPlayer();
   }

   public boolean isSimulationDirectionCalculated() {
      return this.m_ragdollStateData.isCalculated;
   }

   public Vector2 getCalculatedSimulationDirection(Vector2 var1) {
      var1.set(this.m_ragdollStateData.simulationDirection);
      return var1;
   }

   public float getCalculatedSimlationDirectionAngle() {
      return this.m_ragdollStateData.simulationDirection.getDirection();
   }

   private void calculateSimulationData(float var1) {
      this.m_previousHeadPosition.set(this.m_headPosition);
      this.m_previousPelvisPosition.set(this.m_pelvisPosition);
      IsoGameCharacter var2 = this.getGameCharacterObject();
      Model.BoneToWorldCoords(var2, RagdollBuilder.instance.headBoneIndex, this.m_headPosition);
      Model.BoneToWorldCoords(var2, RagdollBuilder.instance.pelvisBoneIndex, this.m_pelvisPosition);
      if (!this.m_ragdollStateData.isCalculated) {
         this.m_previousHeadPosition.set(this.m_headPosition);
         this.m_previousPelvisPosition.set(this.m_pelvisPosition);
      }

      this.m_ragdollStateData.isSimulationMovement = false;
      float var3 = this.m_previousHeadPosition.distanceTo(this.m_headPosition);
      float var4 = this.m_previousPelvisPosition.distanceTo(this.m_pelvisPosition);
      if (!(var3 > 0.01F) && !(var4 > 0.01F)) {
         if (this.isSimulationSleeping()) {
            RagdollStateData var10000 = this.m_ragdollStateData;
            var10000.simulationTimeout -= var1 * 10.0F;
         }
      } else {
         if (this.m_ragdollStateData.simulationTimeout < 1.5F) {
            this.m_ragdollStateData.simulationTimeout = 1.5F;
         }

         this.m_ragdollStateData.isSimulationMovement = true;
      }

      Vector3 var5 = new Vector3();
      Model.BoneToWorldCoords((IsoGameCharacter)var2, 0, var5);
      this.m_isUpright = this.m_headPosition.z > var5.z + 0.6F - 0.2F;
      var2.setOnFloor(!this.m_isUpright);
      Model.BoneZDirectionToWorldCoords(var2, RagdollBuilder.instance.pelvisBoneIndex, this.m_ragdollStateData.pelvisDirection, 0.5F);
      this.m_isOnBack = this.m_pelvisPosition.z < this.m_ragdollStateData.pelvisDirection.z;
      if (this.m_isOnBack) {
         this.m_ragdollStateData.simulationDirection.x = this.m_pelvisPosition.x - this.m_headPosition.x;
         this.m_ragdollStateData.simulationDirection.y = this.m_pelvisPosition.y - this.m_headPosition.y;
      } else {
         this.m_ragdollStateData.simulationDirection.x = this.m_headPosition.x - this.m_pelvisPosition.x;
         this.m_ragdollStateData.simulationDirection.y = this.m_headPosition.y - this.m_pelvisPosition.y;
      }

      this.m_ragdollStateData.isCalculated = true;
      this.m_ragdollStateData.simulationDirection.normalize();
      var2.setFallOnFront(!this.m_isOnBack);
      var2.setRagdollFall(true);
   }

   private static class Reusables_Quaternions {
      final Quaternion xAxis = new Quaternion();
      final Quaternion yAxis = new Quaternion();
      final Quaternion zAxis = new Quaternion();
      final Quaternion yawAdjust = new Quaternion();
      final Quaternion characterTwistRotation = new Quaternion();
      final Quaternion targetTwistRotation = new Quaternion();

      private Reusables_Quaternions() {
      }
   }

   private static class Reusables_Vector3f {
      final Vector3f zero = new Vector3f(0.0F, 0.0F, 0.0F);

      private Reusables_Vector3f() {
      }
   }

   public static enum SimulationState {
      UNKNOWN,
      ACTIVE_TAG,
      ISLAND_SLEEPING,
      WANTS_DEACTIVATION,
      DISABLE_DEACTIVATION,
      DISABLE_SIMULATION;

      private SimulationState() {
      }
   }
}
