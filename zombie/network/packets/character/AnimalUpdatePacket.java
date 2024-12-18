package zombie.network.packets.character;

import java.nio.ByteBuffer;
import java.util.HashSet;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;

@PacketSetting(
   ordering = 0,
   priority = 2,
   reliability = 2,
   requiredCapability = Capability.LoginOnServer,
   handlingType = 3
)
public class AnimalUpdatePacket implements INetworkPacket {
   protected final HashSet<Short> requested = new HashSet();
   protected final HashSet<Short> updated = new HashSet();

   public AnimalUpdatePacket() {
   }

   public HashSet<Short> getRequested() {
      return this.requested;
   }

   public HashSet<Short> getUpdated() {
      return this.updated;
   }

   public void write(ByteBufferWriter var1) {
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
   }

   public void processServer(PacketTypes.PacketType var1, UdpConnection var2) {
   }

   public void processClient(UdpConnection var1) {
   }
}
