package zombie.network.packets.connection;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.PacketSetting;
import zombie.network.anticheats.AntiCheat;
import zombie.network.anticheats.AntiCheatServerCustomizationDDOS;
import zombie.network.packets.INetworkPacket;

@PacketSetting(
   ordering = 2,
   priority = 1,
   reliability = 3,
   requiredCapability = Capability.None,
   handlingType = 5,
   anticheats = {AntiCheat.ServerCustomizationDDOS}
)
public class ServerCustomizationPacket implements INetworkPacket, AntiCheatServerCustomizationDDOS.IAntiCheat {
   public ServerCustomizationPacket() {
   }

   public long getLastConnect() {
      return -1L;
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
   }

   public void write(ByteBufferWriter var1) {
   }

   public static enum Data {
      ServerImageIcon,
      ServerImageLoginScreen,
      ServerImageLoadingScreen,
      Done;

      private Data() {
      }
   }
}
