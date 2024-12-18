package zombie.network.anticheats;

import zombie.core.Transaction;
import zombie.core.raknet.UdpConnection;
import zombie.network.packets.INetworkPacket;

public class AntiCheatTransaction extends AbstractAntiCheat {
   public AntiCheatTransaction() {
   }

   public String validate(UdpConnection var1, INetworkPacket var2) {
      String var3 = super.validate(var1, var2);
      IAntiCheat var4 = (IAntiCheat)var2;
      Transaction.TransactionState var5 = var4.getState();
      if (!Transaction.TransactionState.Accept.equals(var5) && !Transaction.TransactionState.Done.equals(var5)) {
         return Transaction.TransactionState.Request.equals(var5) && var4.getConsistent() == -1 ? "invalid request" : var3;
      } else {
         return "invalid state";
      }
   }

   public interface IAntiCheat {
      Transaction.TransactionState getState();

      byte getConsistent();
   }
}
