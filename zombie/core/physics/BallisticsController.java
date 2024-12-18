package zombie.core.physics;

import org.joml.AxisAngle4f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import zombie.characters.IsoGameCharacter;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.skinnedmodel.model.ModelInstance;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.debug.LineDrawer;
import zombie.input.Mouse;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IsoUtils;
import zombie.iso.Vector3;
import zombie.network.GameServer;
import zombie.scripting.objects.ModelAttachment;
import zombie.util.Pool;
import zombie.util.PooledObject;

public final class BallisticsController extends PooledObject {
   private static final Vector3f gcTempVector3f = new Vector3f();
   private static final Matrix4f gcAttachmentMatrix = new Matrix4f();
   private static final Matrix4f gcTransform = new Matrix4f();
   private static final Matrix4f gcCharacterMatrix = new Matrix4f();
   private static final float angleX = 0.7853982F;
   private static final float angleY = -0.5235988F;
   private static final float angleZ = 0.0F;
   private static final int maxBallisticsTargets = 20;
   private static final int maxBallisticsTargetsArraySize = 80;
   private static final int maxBallisticsCameraTargets = 10;
   public static final int maxBallisticsCameraTargetsArraySize = 50;
   private static final int maxBallisticsSpreadLocations = 9;
   public static final int maxBallisticsSpreadLocationsArraySize = 36;
   private final Vector3 muzzlePosition = new Vector3();
   private final Vector3 muzzleDirection = new Vector3();
   private final AxisAngle4f muzzleRotation = new AxisAngle4f(0.0F, 0.0F, 1.0F, 0.0F);
   private final float[] ballisticsTargets = new float[80];
   private final float[] ballisticsCameraTargets = new float[50];
   private final float[] ballisticsSpreadData = new float[36];
   private final float[] cachedBallisticsTargets = new float[80];
   private final float[] cachedBallisticsCameraTargets = new float[50];
   private final float[] cachedBallisticsSpreadData = new float[36];
   private int numberOfTargets = 0;
   private int numberOfSpreadData = 0;
   private int numberOfCameraTargets = 0;
   private int cachedNumberOfTargets = 0;
   private int cachedNumberOfSpreadData = 0;
   private int cachedNumberOfCameraTargets = 0;
   private boolean isInitialized = false;
   private IsoGameCharacter isoGameCharacter = null;
   private static final Quaternionf pzCameraRotation = new Quaternionf();
   private static final Pool<BallisticsController> controllerPool = new Pool(BallisticsController::new);

   public static BallisticsController alloc() {
      return (BallisticsController)controllerPool.alloc();
   }

   private BallisticsController() {
   }

   public int getID() {
      return this.isoGameCharacter.getID();
   }

   public void setIsoGameCharacter(IsoGameCharacter var1) {
      this.isoGameCharacter = var1;
   }

   public void update() {
      if (!this.isInitialized) {
         this.initialize();
      }

      if (this.isoGameCharacter == null) {
         DebugType.Ballistics.debugln("isoGameCharacter == null");
      } else if (this.isoGameCharacter.primaryHandModel == null) {
         DebugType.Ballistics.debugln("isoGameCharacter.primaryHandModel == null");
      } else if (this.isoGameCharacter.primaryHandModel.m_modelScript == null) {
         DebugType.Ballistics.debugln("isoGameCharacter.primaryHandModel.m_modelScript == null");
      } else {
         ModelAttachment var1 = this.isoGameCharacter.primaryHandModel.m_modelScript.getAttachmentById("muzzle");
         if (var1 == null) {
            DebugType.Ballistics.debugln("muzzle attachment == null");
         } else {
            this.muzzleRotation.angle = -this.isoGameCharacter.getLookAngleRadians();
            this.calculateMuzzlePosition();
            float var2 = (float)Mouse.getX();
            float var3 = (float)Mouse.getY();
            if (!GameServer.bServer) {
               Bullet.updateBallistics(this.isoGameCharacter.getID(), this.muzzlePosition.x, this.muzzlePosition.z * 2.46F, this.muzzlePosition.y);
               Bullet.updateBallisticsRotation(this.isoGameCharacter.getID(), this.muzzleRotation.x, this.muzzleRotation.y, this.muzzleRotation.z, this.muzzleRotation.angle + 1.5707964F);
               Bullet.updateBallisticsAimPosition(IsoUtils.XToIso(var2, var3, 0.0F), 0.0F, IsoUtils.YToIso(var2, var3, 0.0F));
            } else {
               DebugLog.General.printStackTrace("Bullet is disabled on server");
            }

            if (!this.isInitialized) {
               if (Core.bDebug) {
                  if (!GameServer.bServer) {
                     Bullet.setBallisticsSize(this.isoGameCharacter.getID(), 0.025F);
                     Bullet.setBallisticsColor(this.isoGameCharacter.getID(), 0.0F, 0.0F, 1.0F);
                  } else {
                     DebugLog.General.printStackTrace("Bullet is disabled on server");
                  }
               }

               pzCameraRotation.rotationYXZ(0.7853982F, -0.5235988F, 0.0F);
               if (!GameServer.bServer) {
                  Bullet.updateBallisticsAimQuaternion(pzCameraRotation.x, pzCameraRotation.y, pzCameraRotation.z, pzCameraRotation.w);
               } else {
                  DebugLog.General.printStackTrace("Bullet is disabled on server");
               }

               this.isInitialized = true;
            }

         }
      }
   }

   public void debugRender() {
      if (Core.bDebug && DebugOptions.instance.PhysicsRenderBallisticsControllers.getValue() && this.isoGameCharacter.isAiming()) {
         PhysicsDebugRenderer.addBallisticsRender(this);
      }

   }

   private void calculateMuzzlePosition() {
      ModelInstance var1 = this.isoGameCharacter.primaryHandModel;
      ModelAttachment var2 = this.isoGameCharacter.primaryHandModel.m_modelScript.getAttachmentById("muzzle");
      var1.getAttachmentWorldPosition(var2, this.muzzlePosition, this.muzzleDirection);
   }

   private boolean initialize() {
      if (this.isInitialized) {
         return true;
      } else {
         pzCameraRotation.rotationYXZ(0.7853982F, -0.5235988F, 0.0F);
         if (!GameServer.bServer) {
            Bullet.updateBallisticsAimQuaternion(pzCameraRotation.x, pzCameraRotation.y, pzCameraRotation.z, pzCameraRotation.w);
         } else {
            DebugLog.General.printStackTrace("Bullet is disabled on server");
         }

         return true;
      }
   }

   public void setRange(float var1) {
      if (!GameServer.bServer) {
         Bullet.setBallisticsRange(this.isoGameCharacter.getID(), var1);
      } else {
         DebugLog.General.printStackTrace("Bullet is disabled on server");
      }

   }

   public void getTargets(float var1) {
      if (!GameServer.bServer) {
         this.numberOfTargets = Bullet.getBallisticsTargets(this.isoGameCharacter.getID(), var1, 20, this.ballisticsTargets);
         System.arraycopy(this.ballisticsTargets, 0, this.cachedBallisticsTargets, 0, 80);
      } else {
         DebugLog.General.printStackTrace("Bullet is disabled on server");
         this.numberOfTargets = 0;
      }

      this.cachedNumberOfTargets = this.numberOfTargets;
   }

   public float[] getBallisticsSpreadData() {
      return this.ballisticsSpreadData;
   }

   public float[] getBallisticsTargets() {
      return this.ballisticsTargets;
   }

   public float[] getCachedBallisticsTargets() {
      return this.cachedBallisticsTargets;
   }

   public float[] getCachedBallisticsTargetSpreadData() {
      return this.cachedBallisticsSpreadData;
   }

   public void getSpreadData(float var1, float var2, float var3, int var4) {
      if (!GameServer.bServer) {
         this.numberOfSpreadData = Bullet.getBallisticsTargetsSpreadData(this.isoGameCharacter.getID(), var1, var2, var3, var4, 9, this.ballisticsSpreadData);
         System.arraycopy(this.ballisticsSpreadData, 0, this.cachedBallisticsSpreadData, 0, 36);
      } else {
         DebugLog.General.printStackTrace("Bullet is disabled on server");
         this.numberOfSpreadData = 0;
      }

      this.cachedNumberOfSpreadData = this.numberOfSpreadData;
   }

   public void getCameraTargets(float var1, boolean var2) {
      if (!GameServer.bServer) {
         this.numberOfCameraTargets = Bullet.getBallisticsCameraTargets(this.isoGameCharacter.getID(), var1, 10, var2, this.ballisticsCameraTargets);
         System.arraycopy(this.ballisticsCameraTargets, 0, this.cachedBallisticsCameraTargets, 0, 50);
      } else {
         DebugLog.General.printStackTrace("Bullet is disabled on server");
         this.numberOfCameraTargets = 0;
      }

      this.cachedNumberOfCameraTargets = this.numberOfCameraTargets;
   }

   public float[] getCameraTargets() {
      return this.ballisticsCameraTargets;
   }

   public boolean isValidTarget(int var1) {
      return this.isTarget(var1) || this.isCameraTarget(var1) || this.isSpreadTarget(var1);
   }

   public boolean isValidCachedTarget(int var1) {
      return this.isCachedTarget(var1) || this.isCachedCameraTarget(var1) || this.isCachedSpreadTarget(var1);
   }

   public boolean isTarget(int var1) {
      boolean var2 = false;

      for(int var3 = 0; var3 < this.numberOfTargets; ++var3) {
         int var4 = var3 * 4;
         if (this.ballisticsTargets[var4] == (float)var1) {
            return true;
         }
      }

      return false;
   }

   public boolean isCachedTarget(int var1) {
      boolean var2 = false;

      for(int var3 = 0; var3 < this.cachedNumberOfTargets; ++var3) {
         int var4 = var3 * 4;
         if (this.cachedBallisticsTargets[var4] == (float)var1) {
            return true;
         }
      }

      return false;
   }

   public boolean isCameraTarget(int var1) {
      boolean var2 = false;

      for(int var3 = 0; var3 < this.numberOfCameraTargets; ++var3) {
         int var4 = var3 * 5;
         if (this.ballisticsCameraTargets[var4] == (float)var1) {
            return true;
         }
      }

      return false;
   }

   public boolean isCachedCameraTarget(int var1) {
      boolean var2 = false;

      for(int var3 = 0; var3 < this.cachedNumberOfCameraTargets; ++var3) {
         int var4 = var3 * 5;
         if (this.cachedBallisticsCameraTargets[var4] == (float)var1) {
            return true;
         }
      }

      return false;
   }

   public int getTargetedBodyPart(int var1) {
      boolean var2 = false;

      for(int var3 = 0; var3 < this.numberOfCameraTargets; ++var3) {
         int var4 = var3 * 5;
         if (this.ballisticsCameraTargets[var4] == (float)var1) {
            return (int)this.ballisticsCameraTargets[var4 + 4];
         }
      }

      return RagdollBodyPart.BODYPART_COUNT.ordinal();
   }

   public int getCachedTargetedBodyPart(int var1) {
      boolean var2 = false;

      for(int var3 = 0; var3 < this.cachedNumberOfCameraTargets; ++var3) {
         int var4 = var3 * 5;
         if (this.cachedBallisticsCameraTargets[var4] == (float)var1) {
            return (int)this.cachedBallisticsCameraTargets[var4 + 4];
         }
      }

      return RagdollBodyPart.BODYPART_COUNT.ordinal();
   }

   public boolean isSpreadTarget(int var1) {
      boolean var2 = false;

      for(int var3 = 0; var3 < this.numberOfSpreadData; ++var3) {
         int var4 = var3 * 4;
         if (this.ballisticsSpreadData[var4] == (float)var1) {
            return true;
         }
      }

      return false;
   }

   public boolean isCachedSpreadTarget(int var1) {
      boolean var2 = false;

      for(int var3 = 0; var3 < this.cachedNumberOfSpreadData; ++var3) {
         int var4 = var3 * 4;
         if (this.cachedBallisticsSpreadData[var4] == (float)var1) {
            return true;
         }
      }

      return false;
   }

   public boolean hasSpreadData() {
      return this.numberOfSpreadData != 0;
   }

   public int getNumberOfSpreadData() {
      return this.numberOfSpreadData;
   }

   public int getNumberOfCachedSpreadData() {
      return this.cachedNumberOfSpreadData;
   }

   private void removeFromWorld() {
      if (!GameServer.bServer) {
         Bullet.removeBallistics(this.getID());
      } else {
         DebugLog.General.printStackTrace("Bullet is disabled on server");
      }

   }

   public void releaseController() {
      this.removeFromWorld();
      this.reset();
      this.release();
   }

   public void postUpdate() {
      this.numberOfTargets = 0;
      this.numberOfSpreadData = 0;
      this.numberOfCameraTargets = 0;
   }

   private void reset() {
      this.numberOfTargets = 0;
      this.numberOfSpreadData = 0;
      this.numberOfCameraTargets = 0;
      this.isInitialized = false;
   }

   public int getNumberOfCameraTargets() {
      return this.numberOfCameraTargets;
   }

   public int spreadCount(int var1) {
      int var2 = 0;
      boolean var3 = false;

      for(int var4 = 0; var4 < this.numberOfSpreadData; ++var4) {
         int var5 = var4 * 4;
         if (this.ballisticsSpreadData[var5] == (float)var1) {
            ++var2;
         }
      }

      return var2;
   }

   public int cachedSpreadCount(int var1) {
      int var2 = 0;
      boolean var3 = false;

      for(int var4 = 0; var4 < this.cachedNumberOfSpreadData; ++var4) {
         int var5 = var4 * 4;
         if (this.cachedBallisticsSpreadData[var5] == (float)var1) {
            ++var2;
         }
      }

      return var2;
   }

   public void clearCacheTargets() {
      this.cachedNumberOfTargets = 0;
      this.cachedNumberOfCameraTargets = 0;
      this.cachedNumberOfSpreadData = 0;
   }

   public int getNumberOfTargets() {
      return this.numberOfTargets;
   }

   public int getCachedNumberOfTargets() {
      return this.cachedNumberOfTargets;
   }

   public boolean hasBallisticsTarget() {
      boolean var1 = this.cachedNumberOfCameraTargets > 0;
      return var1;
   }

   public void renderlast() {
      Vector3 var1 = new Vector3();
      Vector3 var2 = new Vector3();
      Vector3 var3 = new Vector3();
      HandWeapon var4 = this.isoGameCharacter.getAttackingWeapon();
      float var5 = var4.getMaxRange() * var4.getRangeMod(this.isoGameCharacter);
      ModelInstance var6 = this.isoGameCharacter.primaryHandModel;
      ModelAttachment var7 = this.isoGameCharacter.primaryHandModel.m_modelScript.getAttachmentById("muzzle");
      var6.getAttachmentWorldPosition(var7, var1, var2);
      var3.set(var1.x + var2.x * var5, var1.y + var2.y * var5, var1.z + var2.z * var5);
      LineDrawer.DrawIsoLine(var1.x, var1.y, var1.z, var3.x, var3.y, var3.z, Color.cyan.r, Color.cyan.g, Color.cyan.b, 1.0F, 1);
      LineDrawer.DrawIsoCircle(var1.x, var1.y, var1.z, 0.1F, 16, Color.green.r, Color.green.g, Color.green.b, 1.0F);
   }

   public Vector3 getMuzzlePosition() {
      return this.muzzlePosition;
   }
}
