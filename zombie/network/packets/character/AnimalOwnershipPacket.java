package zombie.network.packets.character;

import java.nio.ByteBuffer;
import java.util.HashSet;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.PacketSetting;
import zombie.network.packets.INetworkPacket;

@PacketSetting(
   ordering = 0,
   priority = 0,
   reliability = 3,
   requiredCapability = Capability.LoginOnServer,
   handlingType = 2
)
public class AnimalOwnershipPacket implements INetworkPacket {
   private final HashSet<Short> owned = new HashSet();
   private final HashSet<Short> deleted = new HashSet();

   public AnimalOwnershipPacket() {
   }

   public HashSet<Short> getDeleted() {
      return this.deleted;
   }

   public HashSet<Short> getOwned() {
      return this.owned;
   }

   public void write(ByteBufferWriter var1) {
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
   }

   public void processClient(UdpConnection var1) {
   }
}
