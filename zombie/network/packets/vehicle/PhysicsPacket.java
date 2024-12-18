package zombie.network.packets.vehicle;

import java.nio.ByteBuffer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.physics.Bullet;
import zombie.core.physics.WorldSimulation;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.fields.INetworkPacketField;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehicleInterpolationData;
import zombie.vehicles.VehicleManager;

public class PhysicsPacket extends VehicleInterpolationData implements INetworkPacketField {
   private static final float[] buffer = new float[27];
   protected short id;
   protected float force;
   private BaseVehicle vehicle;
   private boolean hasAuth;

   public PhysicsPacket() {
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
      this.id = var1.getShort();
      this.time = var1.getLong();
      this.force = var1.getFloat();
      this.x = var1.getFloat();
      this.y = var1.getFloat();
      this.z = var1.getFloat();
      this.qx = var1.getFloat();
      this.qy = var1.getFloat();
      this.qz = var1.getFloat();
      this.qw = var1.getFloat();
      this.vx = var1.getFloat();
      this.vy = var1.getFloat();
      this.vz = var1.getFloat();
      this.engineSpeed = var1.getFloat();
      this.throttle = var1.getFloat();
      this.setNumWheels(var1.getShort());

      for(int var3 = 0; var3 < this.wheelsCount; ++var3) {
         this.wheelSteering[var3] = var1.getFloat();
         this.wheelRotation[var3] = var1.getFloat();
         this.wheelSkidInfo[var3] = var1.getFloat();
         this.wheelSuspensionLength[var3] = var1.getFloat();
      }

      this.vehicle = VehicleManager.instance.getVehicleByID(this.id);
      if (this.vehicle != null) {
         this.hasAuth = this.vehicle.hasAuthorization(var2);
      }

   }

   public void write(ByteBufferWriter var1) {
      var1.putShort(this.id);
      var1.putLong(this.time);
      var1.putFloat(this.force);
      var1.putFloat(this.x);
      var1.putFloat(this.y);
      var1.putFloat(this.z);
      var1.putFloat(this.qx);
      var1.putFloat(this.qy);
      var1.putFloat(this.qz);
      var1.putFloat(this.qw);
      var1.putFloat(this.vx);
      var1.putFloat(this.vy);
      var1.putFloat(this.vz);
      var1.putFloat(this.engineSpeed);
      var1.putFloat(this.throttle);
      var1.putShort(this.wheelsCount);

      for(int var2 = 0; var2 < this.wheelsCount; ++var2) {
         var1.putFloat(this.wheelSteering[var2]);
         var1.putFloat(this.wheelRotation[var2]);
         var1.putFloat(this.wheelSkidInfo[var2]);
         var1.putFloat(this.wheelSuspensionLength[var2]);
      }

   }

   public boolean set(BaseVehicle var1) {
      if (Bullet.getOwnVehiclePhysics(var1.VehicleID, buffer) != 0) {
         return false;
      } else {
         this.id = var1.getId();
         this.time = WorldSimulation.instance.time;
         this.force = var1.getForce();
         int var2 = 0;
         this.x = buffer[var2++];
         this.y = buffer[var2++];
         this.z = buffer[var2++];
         this.qx = buffer[var2++];
         this.qy = buffer[var2++];
         this.qz = buffer[var2++];
         this.qw = buffer[var2++];
         this.vx = buffer[var2++];
         this.vy = buffer[var2++];
         this.vz = buffer[var2++];
         this.engineSpeed = (float)var1.getEngineSpeed();
         this.throttle = var1.throttle;
         this.wheelsCount = (short)((int)buffer[var2++]);

         for(int var3 = 0; var3 < this.wheelsCount; ++var3) {
            this.wheelSteering[var3] = buffer[var2++];
            this.wheelRotation[var3] = buffer[var2++];
            this.wheelSkidInfo[var3] = buffer[var2++];
            this.wheelSuspensionLength[var3] = buffer[var2++];
         }

         return true;
      }
   }

   public boolean isConsistent(UdpConnection var1) {
      return this.vehicle != null && (GameClient.bClient && !this.hasAuth || GameServer.bServer && this.hasAuth);
   }

   public void process(UdpConnection var1) {
      if (this.isConsistent(var1)) {
         if (GameClient.bClient) {
            this.vehicle.interpolation.interpolationDataAdd(this.vehicle, this);
         } else if (GameServer.bServer) {
            this.vehicle.setClientForce(this.force);
            this.vehicle.setX(this.x);
            this.vehicle.setY(this.y);
            this.vehicle.setZ(this.z);
            this.vehicle.savedRot.x = this.qx;
            this.vehicle.savedRot.y = this.qy;
            this.vehicle.savedRot.z = this.qz;
            this.vehicle.savedRot.w = this.qw;
            this.vehicle.jniTransform.origin.set(this.vehicle.getX() - WorldSimulation.instance.offsetX, this.vehicle.getZ(), this.vehicle.getY() - WorldSimulation.instance.offsetY);
            this.vehicle.jniTransform.setRotation(this.vehicle.savedRot);
            this.vehicle.jniLinearVelocity.x = this.vx;
            this.vehicle.jniLinearVelocity.y = this.vy;
            this.vehicle.jniLinearVelocity.z = this.vz;
            this.vehicle.engineSpeed = (double)this.engineSpeed;
            this.vehicle.throttle = this.throttle;
            this.setNumWheels(this.wheelsCount);

            for(int var2 = 0; var2 < this.wheelsCount; ++var2) {
               this.vehicle.wheelInfo[var2].steering = this.wheelSteering[var2];
               this.vehicle.wheelInfo[var2].rotation = this.wheelRotation[var2];
               this.vehicle.wheelInfo[var2].skidInfo = this.wheelSkidInfo[var2];
               this.vehicle.wheelInfo[var2].suspensionLength = this.wheelSuspensionLength[var2];
            }
         }
      } else if (GameClient.bClient) {
         VehicleManager.instance.sendRequestGetFull(this.id, PacketTypes.PacketType.Vehicles);
      }

   }

   public boolean isRelevant(UdpConnection var1) {
      return var1.RelevantTo(this.x, this.y);
   }
}
