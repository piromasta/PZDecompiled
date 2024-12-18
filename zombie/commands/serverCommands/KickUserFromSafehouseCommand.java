package zombie.commands.serverCommands;

import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.characters.Role;
import zombie.commands.CommandArgs;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.RequiredCapability;
import zombie.core.logger.LoggerManager;
import zombie.core.logger.ZLogger;
import zombie.core.raknet.UdpConnection;
import zombie.iso.areas.SafeHouse;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.ServerOptions;
import zombie.network.packets.INetworkPacket;

@CommandName(
   name = "kickfromsafehouse"
)
@CommandArgs(
   required = {"(.+)", "(.+)"}
)
@CommandHelp(
   helpText = "UI_ServerOptionDesc_KickFromSafehouse"
)
@RequiredCapability(
   requiredCapability = Capability.CanSetupSafehouses
)
public class KickUserFromSafehouseCommand extends CommandBase {
   public KickUserFromSafehouseCommand(String var1, Role var2, String var3, UdpConnection var4) {
      super(var1, var2, var3, var4);
   }

   protected String Command() {
      String var1 = this.getCommandArg(0);
      String var2 = this.getCommandArg(1);

      try {
         SafeHouse var3 = SafeHouse.getSafeHouse(var2);
         if (var3 == null) {
            return "safehouse is null";
         } else {
            var3.removePlayer(var1);
            INetworkPacket.sendToAll(PacketTypes.PacketType.SafehouseSync, (UdpConnection)null, var3, false);
            if (!ServerOptions.instance.SafehouseAllowTrepass.getValue()) {
               IsoPlayer var4 = GameServer.getPlayerByUserName(var1);
               if (var4 != null && var4.getX() >= (float)var3.getX() && var4.getX() < (float)var3.getX2() && var4.getY() >= (float)var3.getY() && var4.getY() < (float)var3.getY2()) {
                  UdpConnection var5 = GameServer.getConnectionFromPlayer(var4);
                  if (var5 != null) {
                     GameServer.sendTeleport(var4, (float)var3.getX() - 1.0F, (float)var3.getY() - 1.0F, 0.0F);
                     if (var4.isAsleep()) {
                        INetworkPacket.processPacketOnServer(PacketTypes.PacketType.WakeUpPlayer, (UdpConnection)null, var4);
                     }
                  }
               }
            }

            ZLogger var10000 = LoggerManager.getLogger(this.getExecutorUsername());
            String var10001 = this.getExecutorUsername();
            var10000.write(var10001 + " kicked user " + var1.trim() + " from safehouse " + var2.trim());
            return "Player " + var1 + " kicked from a safehouse " + var2;
         }
      } catch (Exception var6) {
         var6.printStackTrace();
         return "exception occurs";
      }
   }
}
