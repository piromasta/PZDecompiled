package zombie.network.anticheats;

import zombie.characters.Capability;
import zombie.core.raknet.UdpConnection;

public class AntiCheatCapability extends AbstractAntiCheat {
   public AntiCheatCapability() {
   }

   public static boolean validate(UdpConnection var0, Capability var1) {
      return var0.role.haveCapability(var1);
   }
}
