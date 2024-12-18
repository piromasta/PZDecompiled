package zombie.network.packets.service;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.PacketSetting;
import zombie.network.packets.INetworkPacket;

@PacketSetting(
   ordering = 0,
   priority = 1,
   reliability = 0,
   requiredCapability = Capability.ConnectWithDebug,
   handlingType = 3
)
public class PopmanDebugCommandPacket implements INetworkPacket {
   public PopmanDebugCommandPacket() {
   }

   public boolean setSpawnTimeToZero(Object... var1) {
      return false;
   }

   public boolean setClearZombies(Object... var1) {
      return false;
   }

   public boolean setSpawnNow(Object... var1) {
      return false;
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
   }

   public void write(ByteBufferWriter var1) {
   }
}
