package zombie.commands.serverCommands;

import java.sql.SQLException;
import zombie.characters.IsoPlayer;
import zombie.commands.CommandArgs;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.PlayerType;
import zombie.commands.RequiredRight;
import zombie.core.logger.LoggerManager;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameServer;
import zombie.network.ServerWorldDatabase;
import zombie.network.chat.ChatServer;

@CommandName(
   name = "setaccesslevel"
)
@CommandArgs(
   required = {"(.+)", "(\\w+)"}
)
@CommandHelp(
   helpText = "UI_ServerOptionDesc_SetAccessLevel"
)
@RequiredRight(
   requiredRights = 48
)
public class SetAccessLevelCommand extends CommandBase {
   public SetAccessLevelCommand(String var1, String var2, String var3, UdpConnection var4) {
      super(var1, var2, var3, var4);
   }

   protected String Command() throws SQLException {
      String var1 = this.getCommandArg(0);
      String var2 = "none".equals(this.getCommandArg(1)) ? "" : this.getCommandArg(1);
      return update(this.getExecutorUsername(), this.connection, var1, var2);
   }

   static String update(String var0, UdpConnection var1, String var2, String var3) throws SQLException {
      if ((var1 == null || !var1.isCoopHost) && !ServerWorldDatabase.instance.containsUser(var2) && var1 != null) {
         return "User \"" + var2 + "\" is not in the whitelist, use /adduser first";
      } else {
         IsoPlayer var4 = GameServer.getPlayerByUserName(var2);
         byte var5 = PlayerType.fromString(var3.trim().toLowerCase());
         if (var1 != null && var1.accessLevel == 16 && var5 == 32) {
            return "Moderators can't set Admin access level";
         } else if (var5 == 0) {
            return "Access Level '" + var5 + "' unknown, list of access level: player, admin, moderator, overseer, gm, observer";
         } else {
            if (var4 != null) {
               if (var4.networkAI != null) {
                  var4.networkAI.setCheckAccessLevelDelay(5000L);
               }

               UdpConnection var6 = GameServer.getConnectionFromPlayer(var4);
               byte var7;
               if (var6 != null) {
                  var7 = var6.accessLevel;
               } else {
                  var7 = PlayerType.fromString(var4.accessLevel.toLowerCase());
               }

               if (var7 != var5) {
                  if (var5 == 32) {
                     ChatServer.getInstance().joinAdminChat(var4.OnlineID);
                  } else if (var7 == 32 && var5 != 32) {
                     ChatServer.getInstance().leaveAdminChat(var4.OnlineID);
                  }
               }

               if (var7 != 1 && var5 == 1) {
                  var4.setGhostMode(false);
                  var4.setNoClip(false);
               }

               var4.accessLevel = PlayerType.toString(var5);
               if (var6 != null) {
                  var6.accessLevel = var5;
               }

               if ((var5 & 62) != 0) {
                  var4.setGodMod(true);
                  var4.setGhostMode(true);
                  var4.setInvisible(true);
               } else {
                  var4.setGodMod(false);
                  var4.setGhostMode(false);
                  var4.setInvisible(false);
               }

               GameServer.sendPlayerExtraInfo(var4, (UdpConnection)null);
            }

            LoggerManager.getLogger("admin").write(var0 + " granted " + var5 + " access level on " + var2);
            return var1 != null && var1.isCoopHost ? "Your access level is now: " + var5 : ServerWorldDatabase.instance.setAccessLevel(var2, PlayerType.toString(var5));
         }
      }
   }
}
