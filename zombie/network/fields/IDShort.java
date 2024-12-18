package zombie.network.fields;

import java.nio.ByteBuffer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;

public abstract class IDShort implements INetworkPacketField {
   private short ID;

   public IDShort() {
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
      this.ID = var1.getShort();
   }

   public void write(ByteBufferWriter var1) {
      var1.putShort(this.ID);
   }

   public void write(ByteBuffer var1) {
      var1.putShort(this.ID);
   }

   public boolean isConsistent(UdpConnection var1) {
      return this.ID != -1;
   }

   public void setID(short var1) {
      this.ID = var1;
   }

   public short getID() {
      return this.ID;
   }
}
