package zombie.network.anticheats;

import zombie.core.raknet.UdpConnection;

public class AntiCheatTimeUpdate extends AbstractAntiCheat {
   public AntiCheatTimeUpdate() {
   }

   public boolean update(UdpConnection var1) {
      super.update(var1);
      PacketValidator var2 = var1.validator;
      if (var2.gametimeIntervalCheck()) {
         var2.gametimeSend();
      }

      return !var2.gametimeTimeoutCheck();
   }

   public interface IAntiCheatUpdate {
      void gametimeSend();

      boolean gametimeIntervalCheck();

      boolean gametimeTimeoutCheck();

      void gametimeTimeoutReset();
   }
}
