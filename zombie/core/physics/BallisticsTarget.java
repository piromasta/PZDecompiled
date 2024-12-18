package zombie.core.physics;

import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;
import zombie.characters.IsoGameCharacter;
import zombie.core.Core;
import zombie.core.skinnedmodel.HelperFunctions;
import zombie.debug.DebugOptions;
import zombie.inventory.types.HandWeapon;
import zombie.network.GameServer;
import zombie.util.IPooledObject;
import zombie.util.Pool;
import zombie.util.PooledObject;

public class BallisticsTarget extends PooledObject {
   public static float[] boneTransformData = new float[245];
   private static final Pool<IPooledObject> ballisticsTargetPool = new Pool(BallisticsTarget::new);
   private boolean isInitialized = false;
   private boolean addedToWorld = false;
   private int numberOfBones = -1;
   private IsoGameCharacter isoGameCharacter = null;
   private CombatDamageData combatDamageData = null;
   private boolean combatDamageDataProcessed = true;
   private int releaseFrame = 0;

   private BallisticsTarget() {
   }

   public static Pool<IPooledObject> getBallisticsTargetPool() {
      return ballisticsTargetPool;
   }

   public static BallisticsTarget alloc(IsoGameCharacter var0) {
      if (var0 == null) {
         return null;
      } else {
         BallisticsTarget var1 = (BallisticsTarget)ballisticsTargetPool.alloc();
         var1.isoGameCharacter = var0;
         return var1;
      }
   }

   public int getID() {
      return this.isoGameCharacter == null ? -1 : this.isoGameCharacter.getID();
   }

   public void setIsoGameCharacter(IsoGameCharacter var1) {
      this.isoGameCharacter = var1;
   }

   public boolean isValidIsoGameCharacter() {
      return this.isoGameCharacter != null;
   }

   private boolean initialize() {
      if (this.isInitialized) {
         return true;
      } else if (this.isoGameCharacter.getAnimationPlayer() != null && this.isoGameCharacter.getAnimationPlayer().m_boneTransforms != null && this.isoGameCharacter.getAnimationPlayer().m_boneTransforms[0] != null) {
         this.numberOfBones = this.isoGameCharacter.getAnimationPlayer().m_boneTransforms.length;
         if (boneTransformData.length < this.numberOfBones * 7) {
            boneTransformData = new float[this.numberOfBones * 7];
         }

         return true;
      } else {
         return false;
      }
   }

   public boolean update() {
      if (!this.isInitialized) {
         this.isInitialized = this.initialize();
         return false;
      } else {
         if (this.combatDamageData == null) {
            if (this.releaseFrame > 0) {
               --this.releaseFrame;
            }

            if (this.releaseFrame == 0) {
               return true;
            }
         }

         if (GameServer.bServer) {
            return false;
         } else {
            this.addToWorld();
            float var1 = this.isoGameCharacter.getDirectionAngle();
            Bullet.setBallisticsTargetAxis(this.getID(), -1.5707964F, -1.5707964F + this.isoGameCharacter.getAnimationPlayer().getAngle(), 3.1415927F);
            float var2 = this.isoGameCharacter.getX();
            float var3 = this.isoGameCharacter.getY();
            float var4 = this.isoGameCharacter.getZ();
            boolean var5 = true;
            RagdollController var6 = this.isoGameCharacter.getRagdollController();
            if (var6 != null) {
               var2 = var6.getPelvisPositionX();
               var3 = var6.getPelvisPositionY();
               var4 = var6.getPelvisPositionZ();
               var5 = var6.isUpright();
            }

            Bullet.updateBallisticsTarget(this.isoGameCharacter.getID(), var2, var4 * 2.46F, var3, 0.0F, (float)Math.sin((double)(var1 * 0.5F)), 0.0F, (float)Math.cos((double)(var1 * 0.5F)), false);
            this.updateSkeleton();
            return false;
         }
      }
   }

   public void add() {
      this.releaseFrame = 2;
      if (this.addToWorld()) {
         this.initialize();
         float var1 = this.isoGameCharacter.getDirectionAngle();
         Bullet.setBallisticsTargetAxis(this.getID(), -1.5707964F, -1.5707964F + this.isoGameCharacter.getAnimationPlayer().getAngle(), 3.1415927F);
         float var2 = this.isoGameCharacter.getX();
         float var3 = this.isoGameCharacter.getY();
         float var4 = this.isoGameCharacter.getZ();
         boolean var5 = true;
         RagdollController var6 = this.isoGameCharacter.getRagdollController();
         if (var6 != null) {
            var2 = var6.getPelvisPositionX();
            var3 = var6.getPelvisPositionY();
            var4 = var6.getPelvisPositionZ();
            var5 = var6.isUpright();
         }

         int var7 = this.isoGameCharacter.getID();
         Bullet.updateBallisticsTarget(var7, var2, var4 * 2.46F, var3, 0.0F, (float)Math.sin((double)(var1 * 0.5F)), 0.0F, (float)Math.cos((double)(var1 * 0.5F)), false);
         this.updateSkeleton();
      }
   }

   private void getBoneTransforms() {
      int var1 = 0;
      Vector3f var2 = HelperFunctions.allocVector3f();
      Quaternion var3 = HelperFunctions.allocQuaternion();

      for(int var4 = 0; var4 < this.numberOfBones; ++var4) {
         this.isoGameCharacter.getAnimationPlayer().m_boneTransforms[var4].getPosition(var2);
         this.isoGameCharacter.getAnimationPlayer().m_boneTransforms[var4].getRotation(var3);
         boneTransformData[var1++] = -var2.x * 1.5F;
         boneTransformData[var1++] = -var2.y * 1.5F;
         boneTransformData[var1++] = -var2.z * 1.5F;
         boneTransformData[var1++] = var3.x;
         boneTransformData[var1++] = var3.y;
         boneTransformData[var1++] = var3.z;
         boneTransformData[var1++] = var3.w;
      }

      HelperFunctions.releaseVector3f(var2);
      HelperFunctions.releaseQuaternion(var3);
   }

   private void updateSkeleton() {
      this.getBoneTransforms();
      Bullet.updateBallisticsTargetSkeleton(this.getID(), this.numberOfBones, boneTransformData);
   }

   private void reset() {
      this.isoGameCharacter = null;
      this.addedToWorld = false;
      this.isInitialized = false;
      this.combatDamageDataProcessed = true;
   }

   public void releaseTarget() {
      this.removeFromWorld();
      this.reset();
      if (!this.isFree()) {
         this.release();
      }

   }

   public void debugRender() {
      if (Core.bDebug && DebugOptions.instance.PhysicsRenderBallisticsTargets.getValue()) {
         PhysicsDebugRenderer.addBallisticsRender(this);
      }

   }

   private boolean addToWorld() {
      int var1 = this.getID();
      if (!this.addedToWorld) {
         this.debugRender();
         Bullet.addBallisticsTarget(var1);
         this.addedToWorld = true;
         return true;
      } else {
         return false;
      }
   }

   private void removeFromWorld() {
      int var1 = this.getID();
      if (this.addedToWorld) {
         Bullet.removeBallisticsTarget(var1);
         this.addedToWorld = false;
      }

   }

   public void release(int var1) {
      this.releaseFrame = var1;
   }

   public void setCombatDamageDataProcessed(boolean var1) {
      this.combatDamageDataProcessed = var1;
   }

   public boolean getCombatDamageDateProcessed() {
      return this.combatDamageDataProcessed;
   }

   public CombatDamageData getCombatDamageData() {
      if (this.combatDamageData == null) {
         this.combatDamageData = new CombatDamageData();
      }

      return this.combatDamageData;
   }

   public class CombatDamageData {
      public String event;
      public IsoGameCharacter target;
      public IsoGameCharacter attacker;
      public RagdollBodyPart bodyPart;
      public HandWeapon handWeapon;

      public CombatDamageData() {
      }
   }
}
