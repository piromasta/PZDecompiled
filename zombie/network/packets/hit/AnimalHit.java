package zombie.network.packets.hit;

import java.nio.ByteBuffer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;

public abstract class AnimalHit implements HitCharacter {
   public AnimalHit() {
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
   }

   public void write(ByteBufferWriter var1) {
   }
}
