package zombie.network.fields;

import java.nio.ByteBuffer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;

public abstract class IDInteger implements INetworkPacketField {
   protected int ID;

   public IDInteger() {
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
      this.ID = var1.getInt();
   }

   public void write(ByteBufferWriter var1) {
      var1.putInt(this.ID);
   }

   public boolean isConsistent(UdpConnection var1) {
      return this.ID != -1;
   }

   public void setID(int var1) {
      this.ID = var1;
   }

   public int getID() {
      return this.ID;
   }
}
