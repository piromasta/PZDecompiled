package zombie.entity;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.entity.network.EntityPacketData;
import zombie.entity.network.EntityPacketType;
import zombie.network.PacketSetting;
import zombie.network.packets.INetworkPacket;

@PacketSetting(
   ordering = 0,
   priority = 1,
   reliability = 3,
   requiredCapability = Capability.GeneralCheats,
   handlingType = 3
)
public class GameEntityNetwork implements INetworkPacket {
   public GameEntityNetwork() {
   }

   public static void sendPacketDataTo(IsoPlayer var0, EntityPacketData var1, GameEntity var2, Object var3) {
   }

   public static void sendPacketData(EntityPacketData var0, GameEntity var1, Object var2, Object var3, boolean var4) {
   }

   public static EntityPacketData createPacketData(EntityPacketType var0) {
      return null;
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
   }

   public void write(ByteBufferWriter var1) {
   }
}
