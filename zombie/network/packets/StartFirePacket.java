package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.PacketSetting;
import zombie.network.anticheats.AntiCheat;
import zombie.network.anticheats.AntiCheatFire;
import zombie.network.anticheats.AntiCheatSmoke;
import zombie.network.fields.Square;

@PacketSetting(
   ordering = 0,
   priority = 1,
   reliability = 2,
   requiredCapability = Capability.LoginOnServer,
   handlingType = 2,
   anticheats = {AntiCheat.Fire, AntiCheat.Smoke}
)
public class StartFirePacket implements INetworkPacket, AntiCheatFire.IAntiCheat, AntiCheatSmoke.IAntiCheat {
   public StartFirePacket() {
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
   }

   public void write(ByteBufferWriter var1) {
   }

   public boolean getSmoke() {
      return false;
   }

   public boolean getIgnition() {
      return false;
   }

   public Square getSquare() {
      return null;
   }
}
