package zombie;

import java.util.ArrayList;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.math.PZMath;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoWorld;
import zombie.network.GameServer;
import zombie.vehicles.BaseVehicle;

public final class MovingObjectUpdateScheduler {
   public static final MovingObjectUpdateScheduler instance = new MovingObjectUpdateScheduler();
   final MovingObjectUpdateSchedulerUpdateBucket fullSimulation = new MovingObjectUpdateSchedulerUpdateBucket(1);
   final MovingObjectUpdateSchedulerUpdateBucket halfSimulation = new MovingObjectUpdateSchedulerUpdateBucket(2);
   final MovingObjectUpdateSchedulerUpdateBucket quarterSimulation = new MovingObjectUpdateSchedulerUpdateBucket(4);
   final MovingObjectUpdateSchedulerUpdateBucket eighthSimulation = new MovingObjectUpdateSchedulerUpdateBucket(8);
   final MovingObjectUpdateSchedulerUpdateBucket sixteenthSimulation = new MovingObjectUpdateSchedulerUpdateBucket(16);
   long frameCounter;
   private boolean isEnabled = true;

   public MovingObjectUpdateScheduler() {
   }

   public long getFrameCounter() {
      return this.frameCounter;
   }

   public void startFrame() {
      ++this.frameCounter;
      this.fullSimulation.clear();
      this.halfSimulation.clear();
      this.quarterSimulation.clear();
      this.eighthSimulation.clear();
      this.sixteenthSimulation.clear();
      ArrayList var1 = IsoWorld.instance.getCell().getObjectList();

      for(int var2 = 0; var2 < var1.size(); ++var2) {
         IsoMovingObject var3 = (IsoMovingObject)var1.get(var2);
         if (!GameServer.bServer || !(var3 instanceof IsoZombie)) {
            boolean var4 = false;
            boolean var5 = false;
            float var6 = 1.0E8F;
            int var7 = 2147483647;
            boolean var8 = false;

            int var9;
            for(var9 = 0; var9 < IsoPlayer.numPlayers; ++var9) {
               IsoPlayer var10 = IsoPlayer.players[var9];
               if (var10 != null) {
                  if (var3.getCurrentSquare() == null) {
                     var3.setCurrent(IsoWorld.instance.getCell().getGridSquare((double)var3.getX(), (double)var3.getY(), (double)var3.getZ()));
                  }

                  if (var10 == var3) {
                     var8 = true;
                  }

                  if (var3.getCurrentSquare() != null) {
                     if (var3.getCurrentSquare().isCouldSee(var9)) {
                        var4 = true;
                     }

                     if (var3.getCurrentSquare().isCanSee(var9)) {
                        var5 = true;
                     }

                     float var11 = var3.DistTo(var10);
                     if (var11 < var6) {
                        var6 = var11;
                     }
                  }

                  int var12 = (int)PZMath.abs((float)(PZMath.fastfloor(var3.getZ()) - PZMath.fastfloor(var10.getZ())));
                  var7 = PZMath.min(var12, var7);
               }
            }

            var9 = 3;
            if (!var5) {
               --var9;
            }

            if (!var4 && var6 > 10.0F) {
               --var9;
            }

            if (var6 > 30.0F) {
               --var9;
            }

            if (var6 > 60.0F) {
               --var9;
            }

            if (var6 > 80.0F) {
               --var9;
            }

            if (!var5 && var7 > 1) {
               var9 = -1;
            }

            if (var3 instanceof IsoPlayer) {
               var9 = 3;
            }

            if (var3 instanceof BaseVehicle) {
               var9 = 3;
            }

            if (GameServer.bServer) {
               var9 = 3;
            }

            if (var8) {
               var9 = 3;
            }

            if (!this.isEnabled) {
               var9 = 3;
            }

            if (var9 == 3) {
               this.fullSimulation.add(var3);
            }

            if (var9 == 2) {
               this.halfSimulation.add(var3);
            }

            if (var9 == 1) {
               this.quarterSimulation.add(var3);
            }

            if (var9 == 0) {
               this.eighthSimulation.add(var3);
            }

            if (var9 < 0) {
               this.sixteenthSimulation.add(var3);
            }
         }
      }

   }

   public void update() {
      GameTime.getInstance().PerObjectMultiplier = 1.0F;
      this.fullSimulation.update((int)this.frameCounter);
      this.halfSimulation.update((int)this.frameCounter);
      this.quarterSimulation.update((int)this.frameCounter);
      this.eighthSimulation.update((int)this.frameCounter);
      this.sixteenthSimulation.update((int)this.frameCounter);
   }

   public void postupdate() {
      GameTime.getInstance().PerObjectMultiplier = 1.0F;
      this.fullSimulation.postupdate((int)this.frameCounter);
      this.halfSimulation.postupdate((int)this.frameCounter);
      this.quarterSimulation.postupdate((int)this.frameCounter);
      this.eighthSimulation.postupdate((int)this.frameCounter);
      this.sixteenthSimulation.postupdate((int)this.frameCounter);
   }

   public void updateAnimation() {
      GameTime.getInstance().PerObjectMultiplier = 1.0F;
      this.fullSimulation.updateAnimation((int)this.frameCounter);
      this.halfSimulation.updateAnimation((int)this.frameCounter);
      this.quarterSimulation.updateAnimation((int)this.frameCounter);
      this.eighthSimulation.updateAnimation((int)this.frameCounter);
      this.sixteenthSimulation.updateAnimation((int)this.frameCounter);
   }

   public boolean isEnabled() {
      return this.isEnabled;
   }

   public void setEnabled(boolean var1) {
      this.isEnabled = var1;
   }

   public void removeObject(IsoMovingObject var1) {
      this.fullSimulation.removeObject(var1);
      this.halfSimulation.removeObject(var1);
      this.quarterSimulation.removeObject(var1);
      this.eighthSimulation.removeObject(var1);
      this.sixteenthSimulation.removeObject(var1);
   }

   public ArrayList<IsoMovingObject> getBucket() {
      return this.fullSimulation.getBucket((int)this.frameCounter);
   }
}
