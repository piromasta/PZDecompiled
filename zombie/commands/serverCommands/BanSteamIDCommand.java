package zombie.commands.serverCommands;

import java.sql.SQLException;
import zombie.commands.CommandArgs;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.RequiredRight;
import zombie.core.logger.LoggerManager;
import zombie.core.raknet.UdpConnection;
import zombie.core.znet.SteamUtils;
import zombie.network.GameServer;
import zombie.network.ServerWorldDatabase;

@CommandName(
   name = "banid"
)
@CommandArgs(
   required = {"(.+)"}
)
@CommandHelp(
   helpText = "UI_ServerOptionDesc_BanSteamId"
)
@RequiredRight(
   requiredRights = 48
)
public class BanSteamIDCommand extends CommandBase {
   public BanSteamIDCommand(String var1, String var2, String var3, UdpConnection var4) {
      super(var1, var2, var3, var4);
   }

   protected String Command() throws SQLException {
      String var1 = this.getCommandArg(0);
      if (!SteamUtils.isSteamModeEnabled()) {
         return "Server is not in Steam mode";
      } else if (!SteamUtils.isValidSteamID(var1)) {
         return "Expected SteamID but got \"" + var1 + "\"";
      } else {
         LoggerManager.getLogger("admin").write(this.getExecutorUsername() + " banned SteamID " + var1, "IMPORTANT");
         ServerWorldDatabase.instance.banSteamID(var1, "", true);
         long var2 = SteamUtils.convertStringToSteamID(var1);

         for(int var4 = 0; var4 < GameServer.udpEngine.connections.size(); ++var4) {
            UdpConnection var5 = (UdpConnection)GameServer.udpEngine.connections.get(var4);
            if (var5.steamID == var2) {
               GameServer.kick(var5, "UI_Policy_Ban", (String)null);
               var5.forceDisconnect("command-ban-sid");
               break;
            }
         }

         return "SteamID " + var1 + " is now banned";
      }
   }
}
