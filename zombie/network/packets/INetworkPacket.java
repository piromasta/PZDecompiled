package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;

public interface INetworkPacket {
   void parse(ByteBuffer var1, UdpConnection var2);

   void write(ByteBufferWriter var1);

   default int getPacketSizeBytes() {
      return 0;
   }

   default boolean isConsistent() {
      return true;
   }

   default String getDescription() {
      return this.getClass().getSimpleName();
   }

   default void log(UdpConnection var1, String var2) {
   }
}
