package zombie.network.fields;

import java.nio.ByteBuffer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.packets.IDescriptor;

public interface INetworkPacketField extends IDescriptor {
   void parse(ByteBuffer var1, UdpConnection var2);

   void write(ByteBufferWriter var1);

   default int getPacketSizeBytes() {
      return 0;
   }

   default boolean isConsistent(UdpConnection var1) {
      return true;
   }
}
