package zombie.commands.serverCommands;

import zombie.commands.AltCommandArgs;
import zombie.commands.CommandArgs;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.CommandNames;
import zombie.commands.RequiredRight;
import zombie.core.logger.LoggerManager;
import zombie.core.logger.ZLogger;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameServer;
import zombie.network.ServerOptions;
import zombie.network.ServerWorldDatabase;
import zombie.network.Userlog;

@CommandNames({@CommandName(
   name = "kick"
), @CommandName(
   name = "kickuser"
)})
@AltCommandArgs({@CommandArgs(
   required = {"(.+)"}
), @CommandArgs(
   required = {"(.+)", "-r", "(.+)"}
)})
@CommandHelp(
   helpText = "UI_ServerOptionDesc_Kick"
)
@RequiredRight(
   requiredRights = 56
)
public class KickUserCommand extends CommandBase {
   private String reason = "";

   public KickUserCommand(String var1, String var2, String var3, UdpConnection var4) {
      super(var1, var2, var3, var4);
   }

   protected String Command() {
      String var1 = this.getCommandArg(0);
      if (this.hasOptionalArg(1)) {
         this.reason = this.getCommandArg(1);
      }

      ZLogger var10000 = LoggerManager.getLogger("admin");
      String var10001 = this.getExecutorUsername();
      var10000.write(var10001 + " kicked user " + var1);
      ServerWorldDatabase.instance.addUserlog(var1, Userlog.UserlogType.Kicked, this.reason, this.getExecutorUsername(), 1);
      boolean var2 = false;

      for(int var3 = 0; var3 < GameServer.udpEngine.connections.size(); ++var3) {
         UdpConnection var4 = (UdpConnection)GameServer.udpEngine.connections.get(var3);

         for(int var5 = 0; var5 < 4; ++var5) {
            if (var1.equals(var4.usernames[var5])) {
               var2 = true;
               if ("".equals(this.reason)) {
                  GameServer.kick(var4, "UI_Policy_Kick", (String)null);
               } else {
                  GameServer.kick(var4, "You have been kicked from this server for the following reason: " + this.reason, (String)null);
               }

               var4.forceDisconnect("command-kick");
               GameServer.addDisconnect(var4);
               break;
            }
         }
      }

      if (var2 && ServerOptions.instance.BanKickGlobalSound.getValue()) {
         GameServer.PlaySoundAtEveryPlayer("RumbleThunder");
      }

      if (var2) {
         return "User " + var1 + " kicked.";
      } else {
         return "User " + var1 + " doesn't exist.";
      }
   }
}
