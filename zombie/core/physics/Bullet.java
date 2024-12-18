package zombie.core.physics;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.util.vector.Quaternion;
import zombie.GameWindow;
import zombie.asset.AssetPath;
import zombie.characters.IsoPlayer;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkLevel;
import zombie.iso.IsoWorld;
import zombie.network.GameServer;
import zombie.network.MPStatistic;
import zombie.network.ServerMap;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.PhysicsShapeScript;
import zombie.scripting.objects.VehicleScript;
import zombie.vehicles.BaseVehicle;

public class Bullet {
   public static final byte TO_ADD_VEHICLE = 4;
   public static final byte TO_SCROLL_CHUNKMAP = 5;
   public static final byte TO_ACTIVATE_CHUNKMAP = 6;
   public static final byte TO_INIT_WORLD = 7;
   public static final byte TO_UPDATE_CHUNK = 8;
   public static final byte TO_DEBUG_DRAW_WORLD = 9;
   public static final byte TO_STEP_SIMULATION = 10;
   public static final byte TO_UPDATE_PLAYER_LIST = 12;
   public static final byte TO_END = -1;
   public static ByteBuffer cmdBuf;
   public static final HashMap<String, Integer> physicsShapeNameToIndex = new HashMap();

   public Bullet() {
   }

   public static void init() {
      String var0 = "";
      if ("1".equals(System.getProperty("zomboid.debuglibs.bullet"))) {
         DebugLog.General.debugln("***** Loading debug version of PZBullet");
         var0 = "d";
      }

      String var1 = "";
      if (GameServer.bServer && GameWindow.OSValidator.isUnix()) {
         var1 = "NoOpenGL";
      }

      if (System.getProperty("os.name").contains("OS X")) {
         loadLibrary("PZBullet");
      } else if (System.getProperty("sun.arch.data.model").equals("64")) {
         loadLibrary("PZBullet" + var1 + "64" + var0);
      } else {
         loadLibrary("PZBullet" + var1 + "32" + var0);
      }

      cmdBuf = ByteBuffer.allocateDirect(4096);
      cmdBuf.order(ByteOrder.LITTLE_ENDIAN);
      DebugLog.General.debugln("Initializing logging...");
      initPZBullet();
   }

   private static void loadLibrary(String var0) {
      DebugLog.General.debugln("Loading library: %s", var0);
      System.loadLibrary(var0);
   }

   private static native void ToBullet(ByteBuffer var0);

   public static void CatchToBullet(ByteBuffer var0) {
      try {
         MPStatistic.getInstance().Bullet.Start();
         ToBullet(var0);
         MPStatistic.getInstance().Bullet.End();
      } catch (RuntimeException var2) {
         var2.printStackTrace();
      }

   }

   public static native void initPZBullet();

   public static native boolean isWorldInit();

   public static native void initWorld(int var0, int var1, boolean var2);

   public static native void destroyWorld();

   public static native void activateChunkMap(int var0, int var1, int var2, int var3);

   public static native void deactivateChunkMap(int var0);

   public static void initWorld(int var0, int var1, int var2, int var3, int var4) {
      MPStatistic.getInstance().Bullet.Start();
      initWorld(var0, var1, GameServer.bServer);
      activateChunkMap(0, var2, var3, var4);
      MPStatistic.getInstance().Bullet.End();
   }

   public static void startLoadingPhysicsMeshes() {
      ArrayList var0 = ScriptManager.instance.getAllPhysicsShapes();

      for(int var1 = 0; var1 < var0.size(); ++var1) {
         PhysicsShapeScript var2 = (PhysicsShapeScript)var0.get(var1);
         PhysicsShape.PhysicsShapeAssetParams var3 = new PhysicsShape.PhysicsShapeAssetParams();
         var3.postProcess = var2.postProcess;
         var3.bAllMeshes = var2.bAllMeshes;
         PhysicsShape var10000 = (PhysicsShape)PhysicsShapeAssetManager.instance.load(new AssetPath(var2.meshName), var3);
      }

   }

   public static void initPhysicsMeshes() {
      physicsShapeNameToIndex.clear();
      WorldSimulation.instance.create();
      clearPhysicsMeshes();
      ArrayList var0 = ScriptManager.instance.getAllPhysicsShapes();
      int var1 = 0;

      int var5;
      for(int var2 = 0; var2 < var0.size(); ++var2) {
         PhysicsShapeScript var3 = (PhysicsShapeScript)var0.get(var2);
         PhysicsShape var4 = (PhysicsShape)PhysicsShapeAssetManager.instance.getAssetTable().get(var3.meshName);
         if (var4 != null && var4.isReady()) {
            for(var5 = 0; var5 < var4.meshes.size(); ++var5) {
               PhysicsShape.OneMesh var6 = (PhysicsShape.OneMesh)var4.meshes.get(var5);
               boolean var7 = var4.meshes.size() > 1;
               definePhysicsMesh(var1, var7, transformPhysicsMeshPoints(var3.translate, var3.rotate, var3.scale, var6.m_transform, var6.m_points, false));
            }

            physicsShapeNameToIndex.put(var3.getScriptObjectFullType(), var1);
            ++var1;
         }
      }

      ArrayList var14 = ScriptManager.instance.getAllVehicleScripts();

      for(int var15 = 0; var15 < var14.size(); ++var15) {
         VehicleScript var16 = (VehicleScript)var14.get(var15);

         for(var5 = 0; var5 < var16.getPhysicsShapeCount(); ++var5) {
            VehicleScript.PhysicsShape var17 = var16.getPhysicsShape(var5);
            if (var17.type == 3) {
               PhysicsShapeScript var18 = ScriptManager.instance.getPhysicsShape(var17.physicsShapeScript);
               if (var18 != null) {
                  PhysicsShape var8 = (PhysicsShape)PhysicsShapeAssetManager.instance.getAssetTable().get(var18.meshName);
                  if (var8 != null && var8.isReady()) {
                     for(int var9 = 0; var9 < var8.meshes.size(); ++var9) {
                        PhysicsShape.OneMesh var10 = (PhysicsShape.OneMesh)var8.meshes.get(var9);
                        if (var10.m_points.length != 0) {
                           Matrix4f var11 = BaseVehicle.allocMatrix4f().scaling(var16.getModelScale());
                           postMultiplyTranslateRotateScale(var18.translate, var18.rotate, var18.scale, var11);
                           var10.m_transform.transpose();
                           var11.mul(var10.m_transform);
                           var10.m_transform.transpose();
                           float[] var12 = transformPhysicsMeshPoints(var11, var10.m_points, false);
                           byte var13 = 1;
                           defineVehiclePhysicsMesh(var16.getFullName(), var13 + var5, var12);
                           BaseVehicle.releaseMatrix4f(var11);
                        }
                     }
                  }
               }
            }
         }
      }

   }

   public static float[] transformPhysicsMeshPoints(Vector3f var0, Vector3f var1, float var2, Matrix4f var3, float[] var4, boolean var5) {
      Matrix4f var6 = translationRotateScale(var0, var1, var2, BaseVehicle.allocMatrix4f());
      if (var3 != null) {
         var3.transpose();
         var6.mul(var3);
         var3.transpose();
      }

      Vector3f var7 = new Vector3f();
      float[] var8 = new float[var4.length];

      for(int var9 = 0; var9 < var4.length; var9 += 3) {
         float var10 = var4[var9];
         float var11 = var4[var9 + 1];
         float var12 = var4[var9 + 2];
         var6.transformPosition(var10, var11, var12, var7);
         var8[var9] = var7.x;
         var8[var9 + 1] = var5 ? var7.z : var7.y;
         var8[var9 + 2] = var5 ? var7.y : var7.z;
      }

      BaseVehicle.releaseMatrix4f(var6);
      return var8;
   }

   public static float[] transformPhysicsMeshPoints(Matrix4f var0, float[] var1, boolean var2) {
      Vector3f var3 = new Vector3f();
      float[] var4 = new float[var1.length];

      for(int var5 = 0; var5 < var1.length; var5 += 3) {
         float var6 = var1[var5];
         float var7 = var1[var5 + 1];
         float var8 = var1[var5 + 2];
         var0.transformPosition(var6, var7, var8, var3);
         var4[var5] = var3.x;
         var4[var5 + 1] = var2 ? var3.z : var3.y;
         var4[var5 + 2] = var2 ? var3.y : var3.z;
      }

      return var4;
   }

   public static Matrix4f translationRotateScale(Vector3f var0, Vector3f var1, float var2, Matrix4f var3) {
      Quaternionf var4 = BaseVehicle.allocQuaternionf();
      var4.rotationXYZ(var1.x * 0.017453292F, var1.y * 0.017453292F, var1.z * 0.017453292F);
      var3.translationRotateScale(var0, var4, var2);
      BaseVehicle.releaseQuaternionf(var4);
      return var3;
   }

   public static Matrix4f postMultiplyTranslateRotateScale(Vector3f var0, Vector3f var1, float var2, Matrix4f var3) {
      Matrix4f var4 = translationRotateScale(var0, var1, var2, BaseVehicle.allocMatrix4f());
      var3.mul(var4);
      BaseVehicle.releaseMatrix4f(var4);
      return var3;
   }

   public static void updatePlayerList(ArrayList<IsoPlayer> var0) {
      cmdBuf.clear();
      cmdBuf.put((byte)12);
      cmdBuf.putShort((short)var0.size());
      Iterator var1 = var0.iterator();

      while(var1.hasNext()) {
         IsoPlayer var2 = (IsoPlayer)var1.next();
         cmdBuf.putInt(var2.OnlineID);
         cmdBuf.putInt(PZMath.fastfloor(var2.getX()));
         cmdBuf.putInt(PZMath.fastfloor(var2.getY()));
      }

      cmdBuf.put((byte)-1);
      cmdBuf.put((byte)-1);
      CatchToBullet(cmdBuf);
   }

   public static void beginUpdateChunk(IsoChunk var0, int var1) {
      cmdBuf.clear();
      cmdBuf.put((byte)8);
      cmdBuf.putShort((short)var0.wx);
      cmdBuf.putShort((short)var0.wy);
      cmdBuf.putShort((short)var0.minLevel);
      cmdBuf.putShort((short)var0.maxLevel);
      cmdBuf.putShort((short)var1);
   }

   public static void updateChunk(int var0, int var1, int var2, byte[] var3) {
      cmdBuf.put((byte)var0);
      cmdBuf.put((byte)var1);
      cmdBuf.put((byte)var2);

      for(int var4 = 0; var4 < var2; ++var4) {
         cmdBuf.put(var3[var4]);
      }

   }

   public static void endUpdateChunk() {
      if (cmdBuf.position() != 11) {
         cmdBuf.put((byte)-1);
         cmdBuf.put((byte)-1);
         CatchToBullet(cmdBuf);
      }
   }

   public static native void scrollChunkMap(int var0, int var1);

   public static void scrollChunkMapLeft(int var0) {
      MPStatistic.getInstance().Bullet.Start();
      scrollChunkMap(var0, 0);
      MPStatistic.getInstance().Bullet.End();
   }

   public static void scrollChunkMapRight(int var0) {
      MPStatistic.getInstance().Bullet.Start();
      scrollChunkMap(var0, 1);
      MPStatistic.getInstance().Bullet.End();
   }

   public static void scrollChunkMapUp(int var0) {
      MPStatistic.getInstance().Bullet.Start();
      scrollChunkMap(var0, 2);
      MPStatistic.getInstance().Bullet.End();
   }

   public static void scrollChunkMapDown(int var0) {
      MPStatistic.getInstance().Bullet.Start();
      scrollChunkMap(var0, 3);
      MPStatistic.getInstance().Bullet.End();
   }

   public static void setVehicleActive(BaseVehicle var0, boolean var1) {
      var0.isActive = var1;
      setVehicleActive(var0.getId(), var1);
   }

   public static int setVehicleStatic(BaseVehicle var0, boolean var1) {
      var0.isStatic = var1;
      return setVehicleStatic(var0.getId(), var1);
   }

   public static boolean updatePhysicsForLevelIfNeeded(int var0, int var1, int var2) {
      IsoChunk var3 = GameServer.bServer ? ServerMap.instance.getChunk(var0, var1) : IsoWorld.instance.CurrentCell.getChunk(var0, var1);
      if (var3 == null) {
         return false;
      } else {
         IsoChunkLevel var4 = var3.getLevelData(var2);
         if (var4 == null) {
            return false;
         } else if (!var4.physicsCheck) {
            return false;
         } else {
            var4.physicsCheck = false;
            var3.updatePhysicsForLevel(var2);
            return true;
         }
      }
   }

   public static native void setChunkMinMaxLevel(int var0, int var1, int var2, int var3);

   public static native void addVehicle(int var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7, String var8);

   public static native void removeVehicle(int var0);

   public static native void controlVehicle(int var0, float var1, float var2, float var3);

   public static native void setVehicleActive(int var0, boolean var1);

   public static native void applyCentralForceToVehicle(int var0, float var1, float var2, float var3);

   public static native void applyTorqueToVehicle(int var0, float var1, float var2, float var3);

   public static native void teleportVehicle(int var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7);

   public static native void setTireInflation(int var0, int var1, float var2);

   public static native void setTireRemoved(int var0, int var1, boolean var2);

   public static native void stepSimulation(float var0, int var1, float var2);

   public static native int getVehicleCount();

   public static native int getVehiclePhysics(int var0, float[] var1);

   public static native int getOwnVehiclePhysics(int var0, float[] var1);

   public static native int setOwnVehiclePhysics(int var0, float[] var1);

   public static native int setVehicleParams(int var0, float[] var1);

   public static native int setVehicleMass(int var0, float var1);

   public static native int getObjectPhysics(float[] var0);

   public static native void createServerCell(int var0, int var1);

   public static native void removeServerCell(int var0, int var1);

   public static native int addPhysicsObject(float var0, float var1);

   public static native void defineVehicleScript(String var0, float[] var1);

   public static native void defineVehiclePhysicsMesh(String var0, int var1, float[] var2);

   public static native void setVehicleVelocityMultiplier(int var0, float var1, float var2);

   public static native int setVehicleStatic(int var0, boolean var1);

   public static native int addHingeConstraint(int var0, int var1, float var2, float var3, float var4, float var5, float var6, float var7);

   public static native int addPointConstraint(int var0, int var1, float var2, float var3, float var4, float var5, float var6, float var7);

   public static native int addRopeConstraint(int var0, int var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8);

   public static native void removeConstraint(int var0);

   public static native void clearPhysicsMeshes();

   public static native void definePhysicsMesh(int var0, boolean var1, float[] var2);

   public static native void initializeRagdollPose(int var0, float[] var1, float var2, float var3, float var4);

   public static native void initializeRagdollSkeleton(int var0, int[] var1);

   public static native void addRagdoll(int var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7);

   public static void addRagdoll(int var0, org.lwjgl.util.vector.Vector3f var1, Quaternion var2) {
      addRagdoll(var0, var1.x, var1.y, var1.z, var2.x, var2.y, var2.z, var2.w);
   }

   public static native void removeRagdoll(int var0);

   public static native int simulateRagdoll(int var0, float[] var1);

   public static native void getCorrectedWorldSpace(int var0, float[] var1);

   public static native void setRagdollLocalTransformRotation(int var0, float var1, float var2, float var3, float var4);

   public static native void updateRagdoll(int var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7);

   public static void updateRagdoll(int var0, org.lwjgl.util.vector.Vector3f var1, Quaternion var2) {
      updateRagdoll(var0, var1.x, var1.y, var1.z, var2.x, var2.y, var2.z, var2.w);
   }

   public static native void updateRagdollSkeletonTransforms(int var0, int var1, float[] var2);

   public static native void updateRagdollSkeletonPreviousTransforms(int var0, int var1, float var2, float[] var3);

   public static native int getRagdollSimulationState(int var0);

   public static native void resetSkeletonPose(int var0);

   public static native void setRagdollActive(int var0, boolean var1);

   public static native void drawDebugSingleBone(int var0, boolean var1);

   public static native void drawDebugRagdollSkeleton(int var0, boolean var1, boolean var2);

   public static native void drawDebugRagdollBodyParts(int var0, boolean var1, boolean var2);

   public static native void highlightRagdollBodyPart(int var0, int var1);

   public static native void applyForce(int var0, int var1, float[] var2);

   public static native void applyImpulse(int var0, int var1, float[] var2);

   public static native void detachConstraint(int var0, int var1);

   public static native void updateBallistics(int var0, float var1, float var2, float var3);

   public static native void updateBallisticsRotation(int var0, float var1, float var2, float var3, float var4);

   public static native void setBallisticsSize(int var0, float var1);

   public static native void setBallisticsColor(int var0, float var1, float var2, float var3);

   public static native int getBallisticsTargets(int var0, float var1, int var2, float[] var3);

   public static native int getBallisticsTargetsSpreadData(int var0, float var1, float var2, float var3, int var4, int var5, float[] var6);

   public static native int getBallisticsCameraTargets(int var0, float var1, int var2, boolean var3, float[] var4);

   public static native void setBallisticsRange(int var0, float var1);

   public static native void removeBallistics(int var0);

   public static native void updateBallisticsAimPosition(float var0, float var1, float var2);

   public static native void updateBallisticsAimRotation(float var0, float var1, float var2, float var3);

   public static native void updateBallisticsAimQuaternion(float var0, float var1, float var2, float var3);

   public static native void updateBallisticsAimRotate(float var0, float var1, float var2, float var3);

   public static native void updateBallisticsTargetSkeleton(int var0, int var1, float[] var2);

   public static native void updateBallisticsTarget(int var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7, boolean var8);

   public static native void setBallisticsTargetAxis(int var0, float var1, float var2, float var3);

   public static native int addBallisticsTarget(int var0);

   public static native int removeBallisticsTarget(int var0);

   public static native int getTargetedBodyPart(int var0);

   public static native float getBodyPartFriction(int var0, int var1);

   public static native float getBodyPartRollingFriction(int var0, int var1);

   public static native void setBodyPartFriction(int var0, int var1, float var2, float var3);

   public static native void setRagdollFriction(float var0, float var1);

   public static native void setRagdollMass(float var0);

   public static native boolean checkWheelCollision(int var0, int var1, int var2);

   public static native boolean defineRagdollConstraints(float[] var0, boolean var1);

   public static native boolean defineRagdollAnchors(float[] var0, boolean var1);
}
