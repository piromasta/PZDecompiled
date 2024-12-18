package zombie.network.anticheats;

import java.util.Arrays;
import zombie.core.raknet.UdpConnection;
import zombie.network.fields.CharacterField;
import zombie.network.packets.INetworkPacket;

public class AntiCheatTarget extends AbstractAntiCheat {
   public AntiCheatTarget() {
   }

   public String validate(UdpConnection var1, INetworkPacket var2) {
      String var3 = super.validate(var1, var2);
      IAntiCheat var4 = (IAntiCheat)var2;
      return Arrays.stream(var1.players).noneMatch((var1x) -> {
         return var1x.getOnlineID() == var4.getTargetCharacter().getID();
      }) ? "invalid target" : var3;
   }

   public interface IAntiCheat {
      CharacterField getTargetCharacter();
   }
}
