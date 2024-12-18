package zombie.network.fields;

import java.nio.ByteBuffer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.areas.SafeHouse;

public class SafehouseID extends IDInteger implements INetworkPacketField {
   private SafeHouse safeHouse;

   public SafehouseID() {
   }

   public void set(SafeHouse var1) {
      super.setID(var1.getOnlineID());
      this.safeHouse = var1;
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
      super.parse(var1, var2);
      this.safeHouse = SafeHouse.getSafeHouse(this.getID());
   }

   public void write(ByteBufferWriter var1) {
      super.write(var1);
   }

   public boolean isConsistent(UdpConnection var1) {
      return super.isConsistent(var1) && this.getSafehouse() != null;
   }

   public String toString() {
      return String.valueOf(this.getID());
   }

   public SafeHouse getSafehouse() {
      return this.safeHouse;
   }
}
