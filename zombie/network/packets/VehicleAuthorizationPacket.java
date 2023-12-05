package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehicleManager;

public class VehicleAuthorizationPacket implements INetworkPacket {
   short vehicleId = -1;
   BaseVehicle.Authorization authorization;
   short authorizationPlayer;

   public VehicleAuthorizationPacket() {
      this.authorization = BaseVehicle.Authorization.Server;
      this.authorizationPlayer = -1;
   }

   public void set(BaseVehicle var1, UdpConnection var2) {
      BaseVehicle.ServerVehicleState var3 = var1.connectionState[var2.index];
      var3.setAuthorization(var1);
      this.authorization = var1.netPlayerAuthorization;
      this.authorizationPlayer = var1.netPlayerId;
      this.vehicleId = var1.getId();
   }

   public void process() {
      BaseVehicle var1 = VehicleManager.instance.getVehicleByID(this.vehicleId);
      if (var1 != null) {
         DebugLog.Vehicle.trace("vehicle=%d netPlayerAuthorization=%s netPlayerId=%d", var1.getId(), this.authorization.name(), this.authorizationPlayer);
         var1.netPlayerFromServerUpdate(this.authorization, this.authorizationPlayer);
      }

   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
      this.vehicleId = var1.getShort();
      this.authorization = BaseVehicle.Authorization.valueOf(var1.get());
      this.authorizationPlayer = var1.getShort();
   }

   public void write(ByteBufferWriter var1) {
      var1.putShort(this.vehicleId);
      var1.putByte(this.authorization.index());
      var1.putShort(this.authorizationPlayer);
   }

   public String getDescription() {
      return null;
   }
}
