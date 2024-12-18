package zombie.network.packets.character;

import java.nio.ByteBuffer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.packets.INetworkPacket;

public abstract class DeadCharacterPacket implements INetworkPacket {
   public DeadCharacterPacket() {
   }

   public void process() {
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
   }

   public void write(ByteBufferWriter var1) {
   }
}
