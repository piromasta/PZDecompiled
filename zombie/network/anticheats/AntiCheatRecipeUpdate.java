package zombie.network.anticheats;

import zombie.core.raknet.UdpConnection;

public class AntiCheatRecipeUpdate extends AbstractAntiCheat {
   public AntiCheatRecipeUpdate() {
   }

   public boolean update(UdpConnection var1) {
      super.update(var1);
      PacketValidator var2 = var1.validator;
      if (var2.checksumIntervalCheck()) {
         var2.checksumSend(false, false);
      }

      return !var2.checksumTimeoutCheck();
   }

   public interface IAntiCheatUpdate {
      void checksumSend(boolean var1, boolean var2);

      boolean checksumIntervalCheck();

      boolean checksumTimeoutCheck();

      void checksumTimeoutReset();
   }
}
