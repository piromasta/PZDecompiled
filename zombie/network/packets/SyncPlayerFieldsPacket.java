package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.PacketSetting;

@PacketSetting(
   ordering = 0,
   priority = 1,
   reliability = 2,
   requiredCapability = Capability.LoginOnServer,
   handlingType = 2
)
public class SyncPlayerFieldsPacket implements INetworkPacket {
   public static final byte PF_Recipes = 1;
   public static final byte PF_Traits = 2;
   public static final byte PF_AlreadyReadBook = 4;
   public static final byte PF_BodyDamage = 8;
   public static final byte PF_Reading = 16;
   public static final byte PF_Count = 5;

   public SyncPlayerFieldsPacket() {
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
   }

   public void write(ByteBufferWriter var1) {
   }
}
