package zombie.commands.serverCommands;

import java.sql.SQLException;
import zombie.characters.Capability;
import zombie.characters.Role;
import zombie.commands.AltCommandArgs;
import zombie.commands.CommandArgs;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.RequiredCapability;
import zombie.core.logger.LoggerManager;
import zombie.core.logger.ZLogger;
import zombie.core.raknet.UdpConnection;
import zombie.core.znet.SteamUtils;
import zombie.network.GameServer;
import zombie.network.ServerOptions;
import zombie.network.ServerWorldDatabase;
import zombie.network.Userlog;

@CommandName(
   name = "banuser"
)
@AltCommandArgs({@CommandArgs(
   required = {"(.+)"},
   argName = "Ban User Only"
), @CommandArgs(
   required = {"(.+)", "-ip"},
   argName = "Ban User And IP"
), @CommandArgs(
   required = {"(.+)", "-r", "(.+)"},
   argName = "Ban User And Supply Reason"
), @CommandArgs(
   required = {"(.+)", "-ip", "-r", "(.+)"},
   argName = "Ban User And IP And Supply Reason"
)})
@CommandHelp(
   helpText = "UI_ServerOptionDesc_BanUser"
)
@RequiredCapability(
   requiredCapability = Capability.BanUnbanUser
)
public class BanUserCommand extends CommandBase {
   private String reason = "";
   public static final String banUser = "Ban User Only";
   public static final String banWithIP = "Ban User And IP";
   public static final String banWithReason = "Ban User And Supply Reason";
   public static final String banWithReasonIP = "Ban User And IP And Supply Reason";

   public BanUserCommand(String var1, Role var2, String var3, UdpConnection var4) {
      super(var1, var2, var3, var4);
   }

   protected String Command() throws SQLException {
      String var1 = this.getCommandArg(0);
      if (this.hasOptionalArg(1)) {
         this.reason = this.getCommandArg(1);
      }

      boolean var2 = false;
      switch (this.argsName) {
         case "Ban User And IP":
         case "Ban User And IP And Supply Reason":
            var2 = true;
         default:
            var3 = ServerWorldDatabase.instance.banUser(var1, true);
            ServerWorldDatabase.instance.addUserlog(var1, Userlog.UserlogType.Banned, this.reason, this.getExecutorUsername(), 1);
            ZLogger var10000 = LoggerManager.getLogger("admin");
            String var10001 = this.getExecutorUsername();
            var10000.write(var10001 + " banned user " + var1 + (this.reason != null ? this.reason : ""), "IMPORTANT");
            boolean var8 = false;

            for(int var5 = 0; var5 < GameServer.udpEngine.connections.size(); ++var5) {
               UdpConnection var6 = (UdpConnection)GameServer.udpEngine.connections.get(var5);
               if (var6.username.equals(var1)) {
                  var8 = true;
                  if (SteamUtils.isSteamModeEnabled()) {
                     LoggerManager.getLogger("admin").write(this.getExecutorUsername() + " banned steamid " + var6.steamID + "(" + var6.username + ")" + (this.reason != null ? this.reason : ""), "IMPORTANT");
                     String var7 = SteamUtils.convertSteamIDToString(var6.steamID);
                     ServerWorldDatabase.instance.banSteamID(var7, this.reason, true);
                  }

                  if (var2) {
                     LoggerManager.getLogger("admin").write(this.getExecutorUsername() + " banned ip " + var6.ip + "(" + var6.username + ")" + (this.reason != null ? this.reason : ""), "IMPORTANT");
                     ServerWorldDatabase.instance.banIp(var6.ip, var1, this.reason, true);
                  }

                  if ("".equals(this.reason)) {
                     GameServer.kick(var6, "UI_Policy_Ban", (String)null);
                  } else {
                     GameServer.kick(var6, "UI_Policy_BanReason", this.reason);
                  }

                  var6.forceDisconnect("command-ban-ip");
                  break;
               }
            }

            if (var8 && ServerOptions.instance.BanKickGlobalSound.getValue()) {
               GameServer.PlaySoundAtEveryPlayer("Thunder");
            }

            return var3;
      }
   }
}
