package zombie.core.physics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import zombie.GameTime;
import zombie.characters.IsoPlayer;
import zombie.core.profiling.PerformanceProfileProbe;
import zombie.core.textures.TextureDraw;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoWorld;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.MPStatistic;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehicleManager;

public final class WorldSimulation {
   public static WorldSimulation instance = new WorldSimulation();
   public static final boolean LEVEL_ZERO_ONLY = true;
   public HashMap<Integer, IsoMovingObject> physicsObjectMap = new HashMap();
   public boolean created = false;
   public float offsetX = 0.0F;
   public float offsetY = 0.0F;
   public long time;
   private final ArrayList<BaseVehicle> collideVehicles = new ArrayList(4);
   private final Vector3f tempVector3f = new Vector3f();
   private final Vector3f tempVector3f_2 = new Vector3f();
   private final Transform tempTransform = new Transform();
   private final Quaternionf javaxQuat4f = new Quaternionf();
   private final float[] ff = new float[8192];
   private final float[] wheelSteer = new float[4];
   private final float[] wheelRotation = new float[4];
   private final float[] wheelSkidInfo = new float[4];
   private final float[] wheelSuspensionLength = new float[4];

   public WorldSimulation() {
   }

   public void create() {
      if (!this.created) {
         this.offsetX = (float)(IsoWorld.instance.MetaGrid.getMinX() * 300);
         this.offsetY = (float)(IsoWorld.instance.MetaGrid.getMinY() * 300);
         this.time = GameTime.getServerTimeMills();
         IsoChunkMap var1 = IsoWorld.instance.CurrentCell.ChunkMap[0];
         Bullet.initWorld((int)this.offsetX, (int)this.offsetY, var1.getWorldXMin(), var1.getWorldYMin(), IsoChunkMap.ChunkGridWidth);

         for(int var2 = 0; var2 < 4; ++var2) {
            this.wheelSteer[var2] = 0.0F;
            this.wheelRotation[var2] = 0.0F;
            this.wheelSkidInfo[var2] = 0.0F;
            this.wheelSuspensionLength[var2] = 0.0F;
         }

         this.created = true;
      }
   }

   public void destroy() {
      Bullet.destroyWorld();
   }

   private void updatePhysic(float var1) {
      MPStatistic.getInstance().Bullet.Start();
      Bullet.stepSimulation(var1, 2, 0.016666668F);
      MPStatistic.getInstance().Bullet.End();
      this.time = GameTime.getServerTimeMills();
   }

   public void update() {
      WorldSimulation.s_performance.worldSimulationUpdate.invokeAndMeasure(this, WorldSimulation::updateInternal);
   }

   private void updateInternal() {
      if (this.created) {
         this.updatePhysic(GameTime.instance.getRealworldSecondsSinceLastUpdate());
         this.collideVehicles.clear();
         int var1 = Bullet.getVehicleCount();
         int var2 = 0;

         int var3;
         int var4;
         int var5;
         int var6;
         float var7;
         float var8;
         float var9;
         while(var2 < var1) {
            MPStatistic.getInstance().Bullet.Start();
            var3 = Bullet.getVehiclePhysics(var2, this.ff);
            MPStatistic.getInstance().Bullet.End();
            if (var3 <= 0) {
               break;
            }

            var2 += var3;
            var4 = 0;

            for(var5 = 0; var5 < var3; ++var5) {
               var6 = (int)this.ff[var4++];
               var7 = this.ff[var4++];
               var8 = this.ff[var4++];
               var9 = this.ff[var4++];
               this.tempTransform.origin.set(var7, var8, var9);
               float var10 = this.ff[var4++];
               float var11 = this.ff[var4++];
               float var12 = this.ff[var4++];
               float var13 = this.ff[var4++];
               this.javaxQuat4f.set(var10, var11, var12, var13);
               this.tempTransform.setRotation(this.javaxQuat4f);
               float var14 = this.ff[var4++];
               float var15 = this.ff[var4++];
               float var16 = this.ff[var4++];
               this.tempVector3f.set(var14, var15, var16);
               float var17 = this.ff[var4++];
               float var18 = this.ff[var4++];
               int var19 = (int)this.ff[var4++];

               int var20;
               for(var20 = 0; var20 < var19; ++var20) {
                  this.wheelSteer[var20] = this.ff[var4++];
                  this.wheelRotation[var20] = this.ff[var4++];
                  this.wheelSkidInfo[var20] = this.ff[var4++];
                  this.wheelSuspensionLength[var20] = this.ff[var4++];
               }

               var20 = (int)(var7 * 100.0F + var8 * 100.0F + var9 * 100.0F + var10 * 100.0F + var11 * 100.0F + var12 * 100.0F + var13 * 100.0F);
               BaseVehicle var21 = VehicleManager.instance.getVehicleByID((short)var6);
               if (var21 != null && (!GameClient.bClient || !var21.isNetPlayerAuthorization(BaseVehicle.Authorization.Remote) && !var21.isNetPlayerAuthorization(BaseVehicle.Authorization.RemoteCollide))) {
                  if (var21.VehicleID == var6 && var18 > 0.5F) {
                     this.collideVehicles.add(var21);
                     var21.authSimulationHash = var20;
                  }

                  if (GameClient.bClient && var21.isNetPlayerAuthorization(BaseVehicle.Authorization.LocalCollide)) {
                     if (var21.authSimulationHash != var20) {
                        var21.authSimulationTime = System.currentTimeMillis();
                        var21.authSimulationHash = var20;
                     }

                     if (System.currentTimeMillis() - var21.authSimulationTime > 1000L) {
                        VehicleManager.instance.sendCollide(var21, var21.getDriver(), false);
                        var21.authSimulationTime = 0L;
                     }
                  }

                  if (!var21.isNetPlayerAuthorization(BaseVehicle.Authorization.Remote) || !var21.isNetPlayerAuthorization(BaseVehicle.Authorization.RemoteCollide)) {
                     if (GameClient.bClient && var21.isNetPlayerAuthorization(BaseVehicle.Authorization.Server)) {
                        var21.jniSpeed = 0.0F;
                     } else {
                        var21.jniSpeed = var17;
                     }
                  }

                  if (!GameClient.bClient || !var21.isNetPlayerAuthorization(BaseVehicle.Authorization.Server) && !var21.isNetPlayerAuthorization(BaseVehicle.Authorization.Remote) && !var21.isNetPlayerAuthorization(BaseVehicle.Authorization.RemoteCollide)) {
                     if (this.compareTransform(this.tempTransform, var21.getPoly().t)) {
                        var21.polyDirty = true;
                     }

                     var21.jniTransform.set(this.tempTransform);
                     var21.jniLinearVelocity.set(this.tempVector3f);
                     var21.jniIsCollide = var18 > 0.5F;

                     for(int var22 = 0; var22 < var19; ++var22) {
                        var21.wheelInfo[var22].steering = this.wheelSteer[var22];
                        var21.wheelInfo[var22].rotation = this.wheelRotation[var22];
                        var21.wheelInfo[var22].skidInfo = this.wheelSkidInfo[var22];
                        var21.wheelInfo[var22].suspensionLength = this.wheelSuspensionLength[var22];
                     }
                  }
               }
            }
         }

         if (GameClient.bClient) {
            IsoPlayer var23 = IsoPlayer.players[IsoPlayer.getPlayerIndex()];
            if (var23 != null) {
               BaseVehicle var26 = var23.getVehicle();
               if (var26 != null && var26.isNetPlayerId(var23.getOnlineID()) && this.collideVehicles.contains(var26)) {
                  Iterator var24 = this.collideVehicles.iterator();

                  while(var24.hasNext()) {
                     BaseVehicle var25 = (BaseVehicle)var24.next();
                     if (var25.DistTo(var26) < 8.0F && var25.isNetPlayerAuthorization(BaseVehicle.Authorization.Server)) {
                        VehicleManager.instance.sendCollide(var25, var23, true);
                        var25.authorizationClientCollide(var23);
                     }
                  }
               }
            }
         }

         MPStatistic.getInstance().Bullet.Start();
         var3 = Bullet.getObjectPhysics(this.ff);
         MPStatistic.getInstance().Bullet.End();
         var4 = 0;

         for(var5 = 0; var5 < var3; ++var5) {
            var6 = (int)this.ff[var4++];
            var7 = this.ff[var4++];
            var8 = this.ff[var4++];
            var9 = this.ff[var4++];
            var7 += this.offsetX;
            var9 += this.offsetY;
            IsoMovingObject var27 = (IsoMovingObject)this.physicsObjectMap.get(var6);
            if (var27 != null) {
               var27.removeFromSquare();
               var27.setX(var7 + 0.18F);
               var27.setY(var9);
               var27.setZ(Math.max(0.0F, var8 / 3.0F / 0.82F));
               var27.setCurrent(IsoWorld.instance.getCell().getGridSquare((double)var27.getX(), (double)var27.getY(), (double)var27.getZ()));
            }
         }

      }
   }

   private boolean compareTransform(Transform var1, Transform var2) {
      if (!(Math.abs(var1.origin.x - var2.origin.x) > 0.01F) && !(Math.abs(var1.origin.z - var2.origin.z) > 0.01F) && (int)var1.origin.y == (int)var2.origin.y) {
         byte var3 = 2;
         var1.basis.getColumn(var3, this.tempVector3f_2);
         float var4 = this.tempVector3f_2.x;
         float var5 = this.tempVector3f_2.z;
         var2.basis.getColumn(var3, this.tempVector3f_2);
         float var6 = this.tempVector3f_2.x;
         float var7 = this.tempVector3f_2.z;
         return Math.abs(var4 - var6) > 0.001F || Math.abs(var5 - var7) > 0.001F;
      } else {
         return true;
      }
   }

   public int setOwnVehiclePhysics(int var1, float[] var2) {
      return Bullet.setOwnVehiclePhysics(var1, var2);
   }

   public void activateChunkMap(int var1) {
      this.create();
      IsoChunkMap var2 = IsoWorld.instance.CurrentCell.ChunkMap[var1];
      if (!GameServer.bServer) {
         Bullet.activateChunkMap(var1, var2.getWorldXMin(), var2.getWorldYMin(), IsoChunkMap.ChunkGridWidth);
      }
   }

   public void deactivateChunkMap(int var1) {
      if (this.created) {
         Bullet.deactivateChunkMap(var1);
      }
   }

   public void scrollGroundLeft(int var1) {
      if (this.created) {
         Bullet.scrollChunkMapLeft(var1);
      }
   }

   public void scrollGroundRight(int var1) {
      if (this.created) {
         Bullet.scrollChunkMapRight(var1);
      }
   }

   public void scrollGroundUp(int var1) {
      if (this.created) {
         Bullet.scrollChunkMapUp(var1);
      }
   }

   public void scrollGroundDown(int var1) {
      if (this.created) {
         Bullet.scrollChunkMapDown(var1);
      }
   }

   public static TextureDraw.GenericDrawer getDrawer(int var0) {
      PhysicsDebugRenderer var1 = PhysicsDebugRenderer.alloc();
      var1.init(IsoPlayer.players[var0]);
      return var1;
   }

   private static class s_performance {
      static final PerformanceProfileProbe worldSimulationUpdate = new PerformanceProfileProbe("WorldSimulation.update");

      private s_performance() {
      }
   }
}
