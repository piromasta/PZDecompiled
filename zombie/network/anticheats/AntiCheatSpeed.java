package zombie.network.anticheats;

import zombie.characters.Capability;
import zombie.characters.NetworkCharacterAI;
import zombie.core.raknet.UdpConnection;
import zombie.network.ServerOptions;
import zombie.network.fields.IMovable;
import zombie.network.packets.INetworkPacket;
import zombie.network.packets.character.PlayerPacket;

public class AntiCheatSpeed extends AbstractAntiCheat {
   private static final int MAX_SPEED = 10;

   public AntiCheatSpeed() {
   }

   public String validate(UdpConnection var1, INetworkPacket var2) {
      String var3 = super.validate(var1, var2);
      IAntiCheat var4 = (IAntiCheat)var2;
      if (var2 instanceof PlayerPacket var5) {
         if (!var5.getPlayer().isDead()) {
            ((NetworkCharacterAI.SpeedChecker)var4.getMovable()).set(var5.realx, var5.realy, var5.getPlayer().isSeatedInVehicle());
         }
      }

      if (!var1.role.haveCapability(Capability.TeleportToPlayer) && !var1.role.haveCapability(Capability.TeleportToCoordinates) && !var1.role.haveCapability(Capability.TeleportPlayerToAnotherPlayer) && !var1.role.haveCapability(Capability.UseFastMoveCheat)) {
         float var6 = var4.getMovable().isVehicle() ? (float)ServerOptions.instance.SpeedLimit.getValue() : 10.0F;
         return var4.getMovable().getSpeed() > var6 ? String.format("speed=%f > limit=%f", var4.getMovable().getSpeed(), var6) : var3;
      } else {
         return var3;
      }
   }

   public interface IAntiCheat {
      IMovable getMovable();
   }
}
