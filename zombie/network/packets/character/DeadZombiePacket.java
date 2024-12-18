package zombie.network.packets.character;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoMovingObject;
import zombie.network.PacketSetting;
import zombie.network.packets.INetworkPacket;

@PacketSetting(
   ordering = 0,
   priority = 1,
   reliability = 3,
   requiredCapability = Capability.LoginOnServer,
   handlingType = 3
)
public class DeadZombiePacket extends DeadCharacterPacket implements INetworkPacket {
   public DeadZombiePacket() {
   }

   public void set(Object... var1) {
   }

   public IsoMovingObject getZombie() {
      return null;
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
   }

   public void write(ByteBufferWriter var1) {
   }
}
