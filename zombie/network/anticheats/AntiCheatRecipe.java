package zombie.network.anticheats;

import zombie.core.raknet.UdpConnection;
import zombie.network.packets.INetworkPacket;

public class AntiCheatRecipe extends AbstractAntiCheat {
   public AntiCheatRecipe() {
   }

   public String validate(UdpConnection var1, INetworkPacket var2) {
      String var3 = super.validate(var1, var2);
      var1.validator.checksumTimeoutReset();
      IAntiCheat var4 = (IAntiCheat)var2;
      return var4.getClientChecksum() != var4.getServerChecksum() ? "invalid checksum" : var3;
   }

   public interface IAntiCheat {
      long getClientChecksum();

      long getServerChecksum();
   }
}
