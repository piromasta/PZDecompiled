package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.core.Transaction;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.PacketSetting;
import zombie.network.anticheats.AntiCheat;
import zombie.network.anticheats.AntiCheatTransaction;

@PacketSetting(
   ordering = 1,
   priority = 1,
   reliability = 3,
   requiredCapability = Capability.LoginOnServer,
   handlingType = 3,
   anticheats = {AntiCheat.Transaction}
)
public class ItemTransactionPacket extends Transaction implements INetworkPacket, AntiCheatTransaction.IAntiCheat {
   public ItemTransactionPacket() {
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
   }

   public void write(ByteBufferWriter var1) {
   }

   public Transaction.TransactionState getState() {
      return null;
   }

   public byte getConsistent() {
      return 0;
   }
}
