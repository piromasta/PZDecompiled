package zombie.network.anticheats;

import zombie.characters.IsoGameCharacter;
import zombie.core.raknet.UdpConnection;
import zombie.network.packets.INetworkPacket;

public class AntiCheatOwner extends AbstractAntiCheat {
   public AntiCheatOwner() {
   }

   public String validate(UdpConnection var1, INetworkPacket var2) {
      String var3 = super.validate(var1, var2);
      IAntiCheat var4 = (IAntiCheat)var2;
      IsoGameCharacter var5 = var4.getCharacter();
      return var5.getOwner() != var1 ? "invalid owner" : var3;
   }

   public interface IAntiCheat {
      IsoGameCharacter getCharacter();
   }
}
