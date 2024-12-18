package zombie.commands.serverCommands;

import java.sql.SQLException;
import java.util.Iterator;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.characters.Role;
import zombie.characters.Roles;
import zombie.commands.CommandArgs;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.RequiredCapability;
import zombie.core.logger.LoggerManager;
import zombie.core.raknet.UdpConnection;
import zombie.core.znet.SteamGameServer;
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
@RequiredCapability(
   requiredCapability = Capability.ChangeAccessLevel
)
public class SetAccessLevelCommand extends CommandBase {
   public SetAccessLevelCommand(String var1, Role var2, String var3, UdpConnection var4) {
      super(var1, var2, var3, var4);
   }

   protected String Command() throws SQLException {
      String var1 = this.getCommandArg(0);
      String var2 = "none".equals(this.getCommandArg(1)) ? "" : this.getCommandArg(1);
      return update(this.getExecutorUsername(), this.connection, var1, var2);
   }

   public static String update(String var0, UdpConnection var1, String var2, String var3) throws SQLException {
      if ((var1 == null || !var1.isCoopHost) && !ServerWorldDatabase.instance.containsUser(var2) && var1 != null) {
         return "User \"" + var2 + "\" is not in the whitelist, use /adduser first";
      } else {
         IsoPlayer var4 = GameServer.getPlayerByUserName(var2);
         Role var5 = Roles.getRole(var3.trim().toLowerCase());
         if (var1 != null && var1.role.haveCapability(Capability.ChangeAccessLevel) && var1.role.rightLevel() < var5.rightLevel()) {
            return "You do not have sufficient rights to set this access level.";
         } else if (var5 == null) {
            String var9 = "";

            Role var8;
            for(Iterator var10 = Roles.getRoles().iterator(); var10.hasNext(); var9 = var9 + var8.getName()) {
               var8 = (Role)var10.next();
               if (!var9.isEmpty()) {
                  var9 = var9 + ", ";
               }
            }

            String var10000 = var3.trim().toLowerCase();
            return "Access Level '" + var10000 + "' unknown, list of access level: " + var9;
         } else {
            if (var4 != null) {
               if (var4.networkAI != null) {
                  var4.networkAI.setCheckAccessLevelDelay(5000L);
               }

               UdpConnection var6 = GameServer.getConnectionFromPlayer(var4);
               Role var7 = null;
               if (var6 != null) {
                  var7 = var6.role;
               }

               if (var7 != var5) {
                  if (var5.haveCapability(Capability.AdminChat) && !var7.haveCapability(Capability.AdminChat)) {
                     ChatServer.getInstance().joinAdminChat(var4.OnlineID);
                  } else if (!var5.haveCapability(Capability.AdminChat) && var7.haveCapability(Capability.AdminChat)) {
                     ChatServer.getInstance().leaveAdminChat(var4.OnlineID);
                  }
               }

               if (!var5.haveCapability(Capability.ToggleInvisibleHimself) && var7.haveCapability(Capability.ToggleInvisibleHimself)) {
                  var4.setGhostMode(false);
               }

               if (!var5.haveCapability(Capability.ToggleNoclipHimself) && var7.haveCapability(Capability.ToggleNoclipHimself)) {
                  var4.setNoClip(false);
               }

               if (!var5.haveCapability(Capability.ToggleGodModHimself) && var7.haveCapability(Capability.ToggleGodModHimself)) {
                  var4.setGodMod(false);
               }

               var4.setRole(var5);
               if (var6 != null) {
                  var6.role = var5;
               }

               if (!var5.haveCapability(Capability.HideFromSteamUserList) && var7.haveCapability(Capability.HideFromSteamUserList)) {
                  SteamGameServer.AddPlayer(var4);
               }

               if (var5.haveCapability(Capability.HideFromSteamUserList) && !var7.haveCapability(Capability.HideFromSteamUserList)) {
                  SteamGameServer.RemovePlayer(var4);
               }

               if (var5.haveCapability(Capability.ToggleInvisibleHimself) && !var7.haveCapability(Capability.ToggleInvisibleHimself)) {
                  var4.setGhostMode(true);
               }

               if (var5.haveCapability(Capability.ToggleNoclipHimself) && !var7.haveCapability(Capability.ToggleNoclipHimself)) {
                  var4.setNoClip(true);
               }

               if (var5.haveCapability(Capability.ToggleGodModHimself) && !var7.haveCapability(Capability.ToggleGodModHimself)) {
                  var4.setGodMod(true);
               }

               GameServer.sendPlayerExtraInfo(var4, (UdpConnection)null);
            }

            LoggerManager.getLogger("admin").write(var0 + " granted " + var5.getName() + " access level on " + var2);
            return ServerWorldDatabase.instance.setRole(var2, var5);
         }
      }
   }
}
