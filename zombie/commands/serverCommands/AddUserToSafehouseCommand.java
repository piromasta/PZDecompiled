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
import zombie.debug.DebugLog;
import zombie.iso.areas.SafeHouse;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;

@CommandName(
   name = "addtosafehouse"
)
@CommandArgs(
   required = {"(.+)", "(.+)"}
)
@CommandHelp(
   helpText = "UI_ServerOptionDesc_AddToSafehouse"
)
@RequiredCapability(
   requiredCapability = Capability.CanSetupSafehouses
)
public class AddUserToSafehouseCommand extends CommandBase {
   public AddUserToSafehouseCommand(String var1, Role var2, String var3, UdpConnection var4) {
      super(var1, var2, var3, var4);
   }

   protected String Command() {
      String var1 = this.getCommandArg(0);
      String var2 = this.getCommandArg(1);
      ZLogger var10000 = LoggerManager.getLogger(this.getExecutorUsername());
      String var10001 = this.getExecutorUsername();
      var10000.write(var10001 + " invited user " + var1.trim() + " to safehouse " + var2.trim());

      try {
         IsoPlayer var3 = GameServer.getPlayerByUserName(var1);
         if (var3 == null) {
            String var9 = String.format("Cannot find player \"%s\"!", var1);
            DebugLog.Multiplayer.debugln(var9);
            return var9;
         } else {
            UdpConnection var4 = GameServer.getConnectionFromPlayer(var3);
            if (var4 == null) {
               String var10 = String.format("Cannot find connection for player \"%s\"!", var1);
               DebugLog.Multiplayer.debugln(var10);
               return var10;
            } else {
               SafeHouse var5 = SafeHouse.getSafeHouse(var2);
               if (var5 == null) {
                  String var11 = String.format("Cannot find safehouse \"%s\"!", var2);
                  DebugLog.Multiplayer.debugln(var11);
                  return var11;
               } else {
                  IsoPlayer var6 = GameServer.getPlayerByUserName(this.getExecutorUsername());
                  if (var6 == null) {
                     String var7 = String.format("Cannot find host player \"%s\"!", this.getExecutorUsername());
                     DebugLog.Multiplayer.debugln(var7);
                     return var7;
                  } else {
                     INetworkPacket.send(var4, PacketTypes.PacketType.SafehouseInvite, var5, var6, var1);
                     return "Safehouse invite sent to " + var1;
                  }
               }
            }
         }
      } catch (Exception var8) {
         var8.printStackTrace();
         return "exception occurs";
      }
   }
}
