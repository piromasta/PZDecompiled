package zombie.vehicles;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import org.joml.Quaternionf;
import zombie.GameTime;
import zombie.core.physics.WorldSimulation;
import zombie.network.MPStatistics;

public class VehicleInterpolation {
   int delay;
   int history;
   int delayTarget;
   boolean buffering;
   float[] lastBuf1;
   boolean wasNull = false;
   long lastTime = -1L;
   long lastTimeA = -1L;
   long recountedTime;
   boolean highPing = false;
   boolean wasHighPing = false;
   private static final ArrayDeque<VehicleInterpolationData> pool = new ArrayDeque();
   private static final List<VehicleInterpolationData> outdated = new ArrayList();
   TreeSet<VehicleInterpolationData> buffer = new TreeSet();
   private static final Quaternionf tempQuaternionA = new Quaternionf();
   private static final Quaternionf tempQuaternionB = new Quaternionf();
   private static final VehicleInterpolationData temp = new VehicleInterpolationData();
   private byte getPointUpdateTimeout = 0;
   private final byte getPointUpdatePer = 10;

   VehicleInterpolation() {
      this.reset();
      this.delay = 500;
      this.history = 800;
      this.delayTarget = this.delay;
   }

   public void reset() {
      this.buffering = true;
      this.clear();
   }

   public void clear() {
      if (!this.buffer.isEmpty()) {
         pool.addAll(this.buffer);
         this.buffer.clear();
         outdated.clear();
      }

   }

   public void update(long var1) {
      temp.time = var1 - (long)this.delay;
      VehicleInterpolationData var3 = (VehicleInterpolationData)this.buffer.floor(temp);
      Iterator var4 = this.buffer.iterator();

      while(var4.hasNext()) {
         VehicleInterpolationData var5 = (VehicleInterpolationData)var4.next();
         if (var1 - var5.time > (long)this.history && var5 != var3) {
            outdated.add(var5);
         }
      }

      List var10000 = outdated;
      TreeSet var10001 = this.buffer;
      Objects.requireNonNull(var10001);
      var10000.forEach(var10001::remove);
      pool.addAll(outdated);
      outdated.clear();
      if (this.buffer.isEmpty()) {
         this.buffering = true;
      }

   }

   void getPointUpdate() {
      byte var10002 = this.getPointUpdateTimeout;
      this.getPointUpdateTimeout = (byte)(var10002 + 1);
      if (var10002 >= 10) {
         float var1 = WorldSimulation.instance.periodSec * 10.0F;
         this.getPointUpdateTimeout = 0;
         int var2 = Integer.parseInt(MPStatistics.getLuaStatus().rawget("lastPing").toString());
         boolean var3 = var2 > 290;
         if (this.highPing && !var3) {
            this.wasHighPing = true;
         }

         if (var3) {
            this.wasHighPing = false;
         }

         this.highPing = var3;
         int var4;
         if (this.delay != this.delayTarget) {
            var4 = Math.max(1, (int)(500.0F * var1 / 3.0F));
            int var5;
            if (this.delay < this.delayTarget) {
               if (this.wasHighPing) {
                  this.wasHighPing = false;
               }

               if (this.highPing) {
                  var4 = this.delayTarget - this.delay;
               }

               var5 = Math.min(this.delayTarget - this.delay, var4);
               this.delay += var5;
               this.history += var5;
            } else {
               if (this.wasHighPing) {
                  var4 = (int)((float)var4 / 8.0F);
                  if (var4 < 1) {
                     var4 = 1;
                  }
               }

               var5 = Math.min(this.delay - this.delayTarget, var4);
               this.delay -= var5;
               this.history -= var5;
            }
         }

         var4 = 500;
         if (this.highPing) {
            var4 = (int)((float)var4 * 3.0F);
         }

         if (this.delayTarget != var4) {
            if (this.delayTarget < var4) {
               this.delayTarget += Math.max(1, (int)((float)var4 * var1 / 10.0F));
               if (this.highPing) {
                  this.delayTarget = var4;
               }
            } else {
               this.delayTarget -= Math.max(1, (int)((float)var4 * var1 / 10.0F));
            }
         }

         if (this.wasHighPing && !this.highPing && Math.abs(this.delay - this.delayTarget) < 10 && Math.abs(var4 - this.delayTarget) < 10) {
            this.wasHighPing = false;
            this.delayTarget = 500;
         }

      }
   }

   private void interpolationDataCurrentAdd(BaseVehicle var1) {
      VehicleInterpolationData var2 = pool.isEmpty() ? new VehicleInterpolationData() : (VehicleInterpolationData)pool.pop();
      var2.time = GameTime.getServerTimeMills() - (long)this.delay;
      var2.x = var1.jniTransform.origin.x + WorldSimulation.instance.offsetX;
      var2.y = var1.jniTransform.origin.z + WorldSimulation.instance.offsetY;
      var2.z = var1.jniTransform.origin.y;
      Quaternionf var3 = var1.jniTransform.getRotation(new Quaternionf());
      var2.qx = var3.x;
      var2.qy = var3.y;
      var2.qz = var3.z;
      var2.qw = var3.w;
      var2.vx = var1.jniLinearVelocity.x;
      var2.vy = var1.jniLinearVelocity.y;
      var2.vz = var1.jniLinearVelocity.z;
      var2.engineSpeed = (float)var1.engineSpeed;
      var2.throttle = var1.throttle;
      var2.setNumWheels((short)var1.wheelInfo.length);

      for(int var4 = 0; var4 < var2.wheelsCount; ++var4) {
         var2.wheelSteering[var4] = var1.wheelInfo[var4].steering;
         var2.wheelRotation[var4] = var1.wheelInfo[var4].rotation;
         var2.wheelSkidInfo[var4] = var1.wheelInfo[var4].skidInfo;
         if (var1.wheelInfo[var4].suspensionLength > 0.0F) {
            var2.wheelSuspensionLength[var4] = var1.wheelInfo[var4].suspensionLength;
         } else {
            var2.wheelSuspensionLength[var4] = 0.3F;
         }
      }

      this.buffer.add(var2);
   }

   public void interpolationDataAdd(BaseVehicle var1, VehicleInterpolationData var2) {
      VehicleInterpolationData var3 = pool.isEmpty() ? new VehicleInterpolationData() : (VehicleInterpolationData)pool.pop();
      var3.copy(var2);
      if (this.buffer.isEmpty()) {
         this.interpolationDataCurrentAdd(var1);
      }

      this.buffer.add(var3);
      this.update(GameTime.getServerTimeMills());
   }

   public void interpolationDataAdd(ByteBuffer var1, long var2, float var4, float var5, float var6, long var7) {
      VehicleInterpolationData var9 = pool.isEmpty() ? new VehicleInterpolationData() : (VehicleInterpolationData)pool.pop();
      var9.time = var2;
      var9.x = var4;
      var9.y = var5;
      var9.z = var6;
      var9.qx = var1.getFloat();
      var9.qy = var1.getFloat();
      var9.qz = var1.getFloat();
      var9.qw = var1.getFloat();
      var9.vx = var1.getFloat();
      var9.vy = var1.getFloat();
      var9.vz = var1.getFloat();
      var9.engineSpeed = var1.getFloat();
      var9.throttle = var1.getFloat();
      var9.setNumWheels(var1.getShort());

      for(int var10 = 0; var10 < var9.wheelsCount; ++var10) {
         var9.wheelSteering[var10] = var1.getFloat();
         var9.wheelRotation[var10] = var1.getFloat();
         var9.wheelSkidInfo[var10] = var1.getFloat();
         var9.wheelSuspensionLength[var10] = var1.getFloat();
      }

      this.buffer.add(var9);
      this.update(var7);
   }

   public boolean interpolationDataGet(float[] var1, float[] var2) {
      long var3 = WorldSimulation.instance.time - (long)this.delay;
      return this.interpolationDataGet(var1, var2, var3);
   }

   public VehicleInterpolationData getLastAddedInterpolationPoint() {
      try {
         return (VehicleInterpolationData)this.buffer.last();
      } catch (Exception var2) {
         return null;
      }
   }

   public void setDelayLength(float var1) {
      this.delayTarget = (int)(var1 * 500.0F);
   }

   public boolean isDelayLengthIncreased() {
      return this.delayTarget > 500;
   }

   public boolean interpolationDataGet(float[] var1, float[] var2, long var3) {
      this.getPointUpdate();
      temp.time = var3;
      VehicleInterpolationData var5 = (VehicleInterpolationData)this.buffer.higher(temp);
      VehicleInterpolationData var6 = (VehicleInterpolationData)this.buffer.floor(temp);
      if (this.buffering) {
         if (this.buffer.size() < 2 || var5 == null || var6 == null) {
            return false;
         }

         this.buffering = false;
      } else if (this.buffer.isEmpty()) {
         this.reset();
         return false;
      }

      int var7 = 0;
      if (var5 == null) {
         if (var6 == null) {
            this.reset();
            return false;
         } else {
            this.wasNull = true;
            this.lastTimeA = -1L;
            this.lastTime = var6.time;
            this.recountedTime = this.lastTime;
            var2[0] = var6.engineSpeed;
            var2[1] = var6.throttle;
            var1[var7++] = var6.x;
            var1[var7++] = var6.y;
            var1[var7++] = var6.z;
            var1[var7++] = var6.qx;
            var1[var7++] = var6.qy;
            var1[var7++] = var6.qz;
            var1[var7++] = var6.qw;
            var1[var7++] = var6.vx;
            var1[var7++] = var6.vy;
            var1[var7++] = var6.vz;
            var1[var7++] = (float)var6.wheelsCount;

            int var11;
            for(var11 = 0; var11 < var6.wheelsCount; ++var11) {
               var1[var7++] = var6.wheelSteering[var11];
               var1[var7++] = var6.wheelRotation[var11];
               var1[var7++] = var6.wheelSkidInfo[var11];
               var1[var7++] = var6.wheelSuspensionLength[var11];
            }

            this.lastBuf1 = new float[var1.length];

            for(var11 = 0; var11 < var1.length; ++var11) {
               this.lastBuf1[var11] = var1[var11];
            }

            this.reset();
            return true;
         }
      } else if (var6 != null && (Math.abs(var5.time - var6.time) >= 10L || this.wasNull) && (!this.wasNull || var5.time - this.lastTime >= 10L)) {
         if (this.lastTimeA == -1L) {
            this.lastTimeA = var6.time;
         }

         if (this.lastTimeA != var6.time && this.wasNull) {
            this.lastTimeA = var6.time;
            this.wasNull = false;
         }

         float var8;
         if (this.wasNull) {
            if (var3 - this.recountedTime > 20L) {
               long var9 = (var5.time - this.lastTime) / 7L;
               this.recountedTime += var9 > 20L ? var9 : 20L;
            } else {
               this.recountedTime = var3;
            }

            var8 = (float)(this.recountedTime - this.lastTime) / (float)(var5.time - this.lastTime);
         } else {
            var8 = (float)(var3 - var6.time) / (float)(var5.time - var6.time);
         }

         float var12 = var8;
         var2[0] = (var5.engineSpeed - var6.engineSpeed) * var8 + var6.engineSpeed;
         var2[1] = (var5.throttle - var6.throttle) * var8 + var6.throttle;
         int var10;
         if (this.wasNull) {
            var1[var7] = (var5.x - this.lastBuf1[var7]) * var8 + this.lastBuf1[var7];
            ++var7;
            var1[var7] = (var5.y - this.lastBuf1[var7]) * var8 + this.lastBuf1[var7];
            ++var7;
            var1[var7] = (var5.z - this.lastBuf1[var7]) * var8 + this.lastBuf1[var7];
            ++var7;
            tempQuaternionA.set(var6.qx, var6.qy, var6.qz, var6.qw);
            tempQuaternionB.set(var5.qx, var5.qy, var5.qz, var5.qw);
            tempQuaternionA.nlerp(tempQuaternionB, var8);
            var1[var7++] = tempQuaternionA.x;
            var1[var7++] = tempQuaternionA.y;
            var1[var7++] = tempQuaternionA.z;
            var1[var7++] = tempQuaternionA.w;
            var1[var7] = (var5.vx - this.lastBuf1[var7]) * var8 + this.lastBuf1[var7];
            ++var7;
            var1[var7] = (var5.vy - this.lastBuf1[var7]) * var8 + this.lastBuf1[var7];
            ++var7;
            var1[var7] = (var5.vz - this.lastBuf1[var7]) * var8 + this.lastBuf1[var7];
            ++var7;
            var1[var7++] = (float)var5.wheelsCount;

            for(var10 = 0; var10 < var5.wheelsCount; ++var10) {
               var1[var7++] = (var5.wheelSteering[var10] - var6.wheelSteering[var10]) * var12 + var6.wheelSteering[var10];
               var1[var7++] = (var5.wheelRotation[var10] - var6.wheelRotation[var10]) * var12 + var6.wheelRotation[var10];
               var1[var7++] = (var5.wheelSkidInfo[var10] - var6.wheelSkidInfo[var10]) * var12 + var6.wheelSkidInfo[var10];
               var1[var7++] = (var5.wheelSuspensionLength[var10] - var6.wheelSuspensionLength[var10]) * var12 + var6.wheelSuspensionLength[var10];
            }
         } else {
            var1[var7++] = (var5.x - var6.x) * var8 + var6.x;
            var1[var7++] = (var5.y - var6.y) * var8 + var6.y;
            var1[var7++] = (var5.z - var6.z) * var8 + var6.z;
            tempQuaternionA.set(var6.qx, var6.qy, var6.qz, var6.qw);
            tempQuaternionB.set(var5.qx, var5.qy, var5.qz, var5.qw);
            tempQuaternionA.nlerp(tempQuaternionB, var8);
            var1[var7++] = tempQuaternionA.x;
            var1[var7++] = tempQuaternionA.y;
            var1[var7++] = tempQuaternionA.z;
            var1[var7++] = tempQuaternionA.w;
            var1[var7++] = (var5.vx - var6.vx) * var8 + var6.vx;
            var1[var7++] = (var5.vy - var6.vy) * var8 + var6.vy;
            var1[var7++] = (var5.vz - var6.vz) * var8 + var6.vz;
            var1[var7++] = (float)var5.wheelsCount;

            for(var10 = 0; var10 < var5.wheelsCount; ++var10) {
               var1[var7++] = (var5.wheelSteering[var10] - var6.wheelSteering[var10]) * var12 + var6.wheelSteering[var10];
               var1[var7++] = (var5.wheelRotation[var10] - var6.wheelRotation[var10]) * var12 + var6.wheelRotation[var10];
               var1[var7++] = (var5.wheelSkidInfo[var10] - var6.wheelSkidInfo[var10]) * var12 + var6.wheelSkidInfo[var10];
               var1[var7++] = (var5.wheelSuspensionLength[var10] - var6.wheelSuspensionLength[var10]) * var12 + var6.wheelSuspensionLength[var10];
            }
         }

         this.wasNull = false;
         return true;
      } else {
         return false;
      }
   }
}
