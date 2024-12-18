package zombie.network.fields;

import java.nio.ByteBuffer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehicleManager;

public class Vehicle extends IDShort implements IPositional, INetworkPacketField {
   protected BaseVehicle vehicle;

   public Vehicle() {
   }

   public void set(BaseVehicle var1) {
      super.setID(var1.getId());
      this.vehicle = var1;
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
      super.parse(var1, var2);
      this.vehicle = VehicleManager.instance.getVehicleByID(this.getID());
   }

   public void write(ByteBufferWriter var1) {
      super.write(var1);
   }

   public boolean isConsistent(UdpConnection var1) {
      return super.isConsistent(var1) && this.vehicle != null;
   }

   public BaseVehicle getVehicle() {
      return this.vehicle;
   }

   public float getX() {
      return this.vehicle.getX();
   }

   public float getY() {
      return this.vehicle.getY();
   }
}
