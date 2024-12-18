package zombie.network.packets.actions;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.PacketSetting;
import zombie.network.fields.Square;
import zombie.network.id.ObjectID;
import zombie.network.id.ObjectIDManager;
import zombie.network.id.ObjectIDType;
import zombie.network.packets.INetworkPacket;

@PacketSetting(
   ordering = 0,
   priority = 1,
   reliability = 3,
   requiredCapability = Capability.LoginOnServer,
   handlingType = 2
)
public class BecomeCorpsePacket implements INetworkPacket {
   protected final ObjectID objectID;
   protected final Square square;
   private static final byte Animal = 3;
   private static final byte Player = 2;
   private static final byte Zombie = 1;
   private static final byte Unknown = 0;

   public BecomeCorpsePacket() {
      this.objectID = ObjectIDManager.createObjectID(ObjectIDType.DeadBody);
      this.square = new Square();
   }

   public void set(Object var1) {
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
   }

   public void write(ByteBufferWriter var1) {
   }
}
