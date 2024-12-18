package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.characters.Safety;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;

@PacketSetting(
   ordering = 0,
   priority = 1,
   reliability = 2,
   requiredCapability = Capability.LoginOnServer,
   handlingType = 3
)
public class SafetyPacket extends Safety implements INetworkPacket {
   private short id;
   private IsoPlayer player;

   public SafetyPacket(Safety var1) {
   }

   public SafetyPacket() {
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
   }

   public void write(ByteBufferWriter var1) {
   }

   public int getPacketSizeBytes() {
      return 1;
   }

   public boolean isConsistent(UdpConnection var1) {
      return false;
   }

   public void processServer(PacketTypes.PacketType var1, UdpConnection var2) {
   }

   public void processClient(UdpConnection var1) {
   }
}
