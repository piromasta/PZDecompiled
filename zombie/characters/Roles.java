package zombie.characters;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import zombie.GameWindow;
import zombie.ZomboidFileSystem;
import zombie.commands.serverCommands.SetAccessLevelCommand;
import zombie.core.Color;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoWorld;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.network.packets.RolesEditPacket;

public class Roles {
   private static ArrayList<Role> roles = new ArrayList();
   private static Role defaultForBanned = null;
   private static Role defaultForNewUser = null;
   private static Role defaultForUser = null;
   private static Role defaultForPriorityUser = null;
   private static Role defaultForObserver = null;
   private static Role defaultForGM = null;
   private static Role defaultForOverseer = null;
   private static Role defaultForModerator = null;
   private static Role defaultForAdmin = null;

   public Roles() {
   }

   public static void init() {
      String var10000 = ZomboidFileSystem.instance.getCacheDir();
      String var0 = var10000 + File.separator + "Server" + File.separator + GameServer.ServerName + ".roles";
      File var1 = new File(var0);
      if (var1.exists()) {
         try {
            FileInputStream var2 = new FileInputStream(var1);

            try {
               ByteBuffer var3 = ByteBuffer.allocate((int)var1.length());
               var3.clear();
               int var4 = var2.read(var3.array());
               var3.limit(var4);
               int var5 = var3.getInt();
               load(var3, var5);
            } catch (Throwable var7) {
               try {
                  var2.close();
               } catch (Throwable var6) {
                  var7.addSuppressed(var6);
               }

               throw var7;
            }

            var2.close();
         } catch (FileNotFoundException var8) {
            var8.printStackTrace();
         } catch (IOException var9) {
            var9.printStackTrace();
         }
      } else {
         roles.clear();
         addStatic();
      }

   }

   public static void save() {
      String var10000 = ZomboidFileSystem.instance.getCacheDir();
      String var0 = var10000 + File.separator + "Server" + File.separator + GameServer.ServerName + ".roles";
      File var1 = new File(var0);
      ByteBuffer var2 = ByteBuffer.allocate(100000);
      var2.putInt(IsoWorld.getWorldVersion());
      save(var2);

      try {
         FileOutputStream var3 = new FileOutputStream(var1);

         try {
            var3.write(var2.array());
         } catch (Throwable var7) {
            try {
               var3.close();
            } catch (Throwable var6) {
               var7.addSuppressed(var6);
            }

            throw var7;
         }

         var3.close();
      } catch (IOException var8) {
         var8.printStackTrace();
      }

   }

   public static ArrayList<Role> getRoles() {
      return roles;
   }

   public static Role getDefaultForBanned() {
      return defaultForBanned;
   }

   public static Role getDefaultForNewUser() {
      return defaultForNewUser;
   }

   public static Role getDefaultForUser() {
      return defaultForUser;
   }

   public static Role getDefaultForPriorityUser() {
      return defaultForPriorityUser;
   }

   public static Role getDefaultForObserver() {
      return defaultForObserver;
   }

   public static Role getDefaultForGM() {
      return defaultForGM;
   }

   public static Role getDefaultForOverseer() {
      return defaultForOverseer;
   }

   public static Role getDefaultForModerator() {
      return defaultForModerator;
   }

   public static Role getDefaultForAdmin() {
      return defaultForAdmin;
   }

   public static void addRole(String var0) {
      if (GameClient.bClient) {
         INetworkPacket.send(PacketTypes.PacketType.RolesEdit, RolesEditPacket.Command.AddRole, var0);
      } else {
         Role var1 = getRole(var0);
         if (var1 == null && roles.size() < 255) {
            var1 = new Role(var0);
            roles.add(var1);
            if (GameServer.bServer) {
               INetworkPacket.sendToAll(PacketTypes.PacketType.Roles, (UdpConnection)null);
            }

         }
      }
   }

   public static void deleteRole(String var0, String var1) {
      Role var2 = getRole(var0);
      if (!var2.isReadOnly()) {
         for(int var3 = 0; var3 < GameServer.Players.size(); ++var3) {
            IsoPlayer var4 = (IsoPlayer)GameServer.Players.get(var3);
            if (var4.getRole() == var2) {
               try {
                  SetAccessLevelCommand.update(var1, (UdpConnection)null, var4.getUsername(), "user");
               } catch (SQLException var6) {
                  var6.printStackTrace();
               }
            }
         }

         if (var2 != null) {
            if (defaultForBanned == var2) {
               defaultForBanned = getRole("banned");
            }

            if (defaultForNewUser == var2) {
               defaultForNewUser = getRole("user");
            }

            if (defaultForUser == var2) {
               defaultForUser = getRole("user");
            }

            if (defaultForPriorityUser == var2) {
               defaultForPriorityUser = getRole("priority");
            }

            if (defaultForObserver == var2) {
               defaultForObserver = getRole("observer");
            }

            if (defaultForGM == var2) {
               defaultForGM = getRole("gm");
            }

            if (defaultForOverseer == var2) {
               defaultForOverseer = getRole("gm");
            }

            if (defaultForModerator == var2) {
               defaultForModerator = getRole("moderator");
            }

            roles.remove(var2);
         }

         if (GameClient.bClient) {
            INetworkPacket.send(PacketTypes.PacketType.RolesEdit, RolesEditPacket.Command.DeleteRole, var0);
         } else {
            if (GameServer.bServer) {
               INetworkPacket.sendToAll(PacketTypes.PacketType.Roles, (UdpConnection)null);
            }

         }
      }
   }

   public static void setDefaultRoleFor(String var0, String var1) {
      if (GameClient.bClient) {
         INetworkPacket.send(PacketTypes.PacketType.RolesEdit, RolesEditPacket.Command.SetDefaultRole, var0, var1);
      } else {
         Role var2 = getRole(var1);
         if (var2 != null) {
            if ("banned".equals(var0)) {
               defaultForBanned = var2;
            }

            if ("newUser".equals(var0)) {
               defaultForNewUser = var2;
            }

            if ("user".equals(var0)) {
               defaultForUser = var2;
            }

            if ("priorityUser".equals(var0)) {
               defaultForPriorityUser = var2;
            }

            if ("observer".equals(var0)) {
               defaultForObserver = var2;
            }

            if ("gm".equals(var0)) {
               defaultForGM = var2;
            }

            if ("overseer".equals(var0)) {
               defaultForOverseer = var2;
            }

            if ("moderator".equals(var0)) {
               defaultForModerator = var2;
            }
         }

         if (GameServer.bServer) {
            INetworkPacket.sendToAll(PacketTypes.PacketType.Roles, (UdpConnection)null);
         }

      }
   }

   public static void setupRole(String var0, String var1, Color var2, ArrayList<Capability> var3) {
      if (GameClient.bClient) {
         INetworkPacket.send(PacketTypes.PacketType.RolesEdit, RolesEditPacket.Command.SetupRole, var0, var1, var2, var3);
      } else {
         Role var4 = getRole(var0);
         if (var4 != null) {
            var4.setDescription(var1);
            var4.setColor(var2);
            var4.setDescription(var1);
            var4.cleanCapability();
            Iterator var5 = var3.iterator();

            while(var5.hasNext()) {
               Capability var6 = (Capability)var5.next();
               var4.addCapability(var6);
            }
         }

         if (GameServer.bServer) {
            INetworkPacket.sendToAll(PacketTypes.PacketType.Roles, (UdpConnection)null);
         }

      }
   }

   public static Role getRole(String var0) {
      Iterator var1 = roles.iterator();

      Role var2;
      do {
         if (!var1.hasNext()) {
            return null;
         }

         var2 = (Role)var1.next();
      } while(!var2.getName().equals(var0));

      return var2;
   }

   public static void setRoles(ArrayList<Role> var0, Role var1, Role var2, Role var3, Role var4, Role var5, Role var6, Role var7, Role var8, Role var9) {
      roles.clear();
      roles.addAll(var0);
      defaultForBanned = var1;
      defaultForNewUser = var2;
      defaultForUser = var3;
      defaultForPriorityUser = var4;
      defaultForObserver = var5;
      defaultForGM = var6;
      defaultForOverseer = var7;
      defaultForModerator = var8;
      defaultForAdmin = var9;
   }

   private static void addStatic() {
      Role var0 = new Role("banned");
      var0.setColor(new Color(0.5F, 0.5F, 0.5F));
      var0.setDescription("Can't login on server.");
      var0.setReadOnly();
      roles.add(var0);
      Role var1 = new Role("user");
      var1.addCapability(Capability.LoginOnServer);
      var1.setColor(new Color(0.9F, 0.9F, 0.9F));
      var1.setDescription("Have no capabilities.");
      var1.setReadOnly();
      roles.add(var1);
      Role var2 = new Role("priority");
      var2.addCapability(Capability.LoginOnServer);
      var2.addCapability(Capability.PriorityLogin);
      var2.addCapability(Capability.CantBeKickedIfTooLaggy);
      var2.setColor(new Color(0.9F, 0.9F, 0.9F));
      var2.setDescription("Have login priority");
      var2.setReadOnly();
      roles.add(var2);
      Role var3 = new Role("observer");
      var3.addCapability(Capability.LoginOnServer);
      var3.addCapability(Capability.PriorityLogin);
      var3.addCapability(Capability.CantBeKickedIfTooLaggy);
      var3.addCapability(Capability.ToggleGodModHimself);
      var3.addCapability(Capability.ToggleInvisibleHimself);
      var3.addCapability(Capability.ToggleNoclipHimself);
      var3.addCapability(Capability.SeePlayersConnected);
      var3.addCapability(Capability.TeleportToPlayer);
      var3.addCapability(Capability.TeleportToCoordinates);
      var3.addCapability(Capability.SeePublicServerOptions);
      var3.addCapability(Capability.CanOpenLockedDoors);
      var3.addCapability(Capability.CanGoInsideSafehouses);
      var3.addCapability(Capability.CanAlwaysJoinServer);
      var3.addCapability(Capability.CanTalkEvenBeingInvisible);
      var3.addCapability(Capability.SeesInvisiblePlayers);
      var3.addCapability(Capability.ToggleCantBeHitByPlayers);
      var3.addCapability(Capability.LogDirectlyInvisibleOrInvincible);
      var3.addCapability(Capability.CanSeePlayersStats);
      var3.addCapability(Capability.CanSeeMessageForAdmin);
      var3.addCapability(Capability.CantBeKickedByAnticheat);
      var3.addCapability(Capability.CantBeKickedByUser);
      var3.addCapability(Capability.CantBeBannedByAnticheat);
      var3.addCapability(Capability.CantBeBannedByUser);
      var3.addCapability(Capability.SeeWorldMap);
      var3.addCapability(Capability.UIManagerProcessCommands);
      var3.setColor(new Color(0.0F, 0.6F, 1.0F));
      var3.setDescription("Can use teleport, god mode, go inside safehouse. But he can't add xp, items and make another change.");
      var3.setReadOnly();
      roles.add(var3);
      Role var4 = new Role("gm");
      var4.addCapability(Capability.LoginOnServer);
      var4.addCapability(Capability.PriorityLogin);
      var4.addCapability(Capability.CantBeKickedIfTooLaggy);
      var4.addCapability(Capability.ToggleGodModHimself);
      var4.addCapability(Capability.ToggleGodModEveryone);
      var4.addCapability(Capability.ToggleInvisibleHimself);
      var4.addCapability(Capability.ToggleInvisibleEveryone);
      var4.addCapability(Capability.ToggleNoclipHimself);
      var4.addCapability(Capability.ToggleNoclipEveryone);
      var4.addCapability(Capability.SeePlayersConnected);
      var4.addCapability(Capability.TeleportToPlayer);
      var4.addCapability(Capability.TeleportToCoordinates);
      var4.addCapability(Capability.TeleportPlayerToAnotherPlayer);
      var4.addCapability(Capability.SeePublicServerOptions);
      var4.addCapability(Capability.CanOpenLockedDoors);
      var4.addCapability(Capability.CanGoInsideSafehouses);
      var4.addCapability(Capability.CanAlwaysJoinServer);
      var4.addCapability(Capability.CanTalkEvenBeingInvisible);
      var4.addCapability(Capability.SeesInvisiblePlayers);
      var4.addCapability(Capability.ToggleCantBeHitByPlayers);
      var4.addCapability(Capability.LogDirectlyInvisibleOrInvincible);
      var4.addCapability(Capability.CanSeePlayersStats);
      var4.addCapability(Capability.CanSeeMessageForAdmin);
      var4.addCapability(Capability.CantBeKickedByAnticheat);
      var4.addCapability(Capability.CantBeKickedByUser);
      var4.addCapability(Capability.CantBeBannedByAnticheat);
      var4.addCapability(Capability.CantBeBannedByUser);
      var4.addCapability(Capability.SeeWorldMap);
      var4.addCapability(Capability.UIManagerProcessCommands);
      var4.addCapability(Capability.MakeEventsAlarmGunshot);
      var4.addCapability(Capability.StartStopRain);
      var4.addCapability(Capability.AddItem);
      var4.addCapability(Capability.AddXP);
      var4.addCapability(Capability.SeeDB);
      var4.addCapability(Capability.SeeNetworkUsers);
      var4.setColor(new Color(1.0F, 0.6F, 0.0F));
      var4.setDescription("Can use teleport, god mode, add xp, items and make another change.");
      var4.setReadOnly();
      roles.add(var4);
      Role var5 = new Role("moderator");
      Capability[] var6 = Capability.values();
      int var7 = var6.length;

      int var8;
      for(var8 = 0; var8 < var7; ++var8) {
         Capability var9 = var6[var8];
         var5.addCapability(var9);
      }

      var5.removeCapability(Capability.SaveWorld);
      var5.removeCapability(Capability.QuitWorld);
      var5.removeCapability(Capability.ChangeAndReloadServerOptions);
      var5.removeCapability(Capability.SendPulse);
      var5.removeCapability(Capability.ReloadLuaFiles);
      var5.removeCapability(Capability.BypassLuaChecksum);
      var5.removeCapability(Capability.RolesWrite);
      var5.removeCapability(Capability.ConnectWithDebug);
      var5.setColor(new Color(0.2F, 1.0F, 0.2F));
      var5.setDescription("Can make all except edit roles, reload lua files, change server options.");
      var5.setReadOnly();
      roles.add(var5);
      Role var11 = new Role("admin");
      Capability[] var12 = Capability.values();
      var8 = var12.length;

      for(int var13 = 0; var13 < var8; ++var13) {
         Capability var10 = var12[var13];
         var11.addCapability(var10);
      }

      var11.setColor(new Color(1.0F, 0.2F, 0.2F));
      var11.setDescription("Have all capabilities.");
      var11.setReadOnly();
      roles.add(var11);
      defaultForBanned = var0;
      defaultForNewUser = var1;
      defaultForUser = var1;
      defaultForPriorityUser = var2;
      defaultForObserver = var3;
      defaultForGM = var4;
      defaultForOverseer = var4;
      defaultForModerator = var5;
      defaultForAdmin = var11;
   }

   private static void save(ByteBuffer var0) {
      byte var1 = 0;
      Iterator var2 = roles.iterator();

      Role var3;
      while(var2.hasNext()) {
         var3 = (Role)var2.next();
         if (!var3.isReadOnly()) {
            ++var1;
         }
      }

      var0.put(var1);
      var2 = roles.iterator();

      while(var2.hasNext()) {
         var3 = (Role)var2.next();
         if (!var3.isReadOnly()) {
            var3.save(var0);
         }
      }

      GameWindow.WriteStringUTF(var0, defaultForBanned.getName());
      GameWindow.WriteStringUTF(var0, defaultForNewUser.getName());
      GameWindow.WriteStringUTF(var0, defaultForUser.getName());
      GameWindow.WriteStringUTF(var0, defaultForPriorityUser.getName());
      GameWindow.WriteStringUTF(var0, defaultForObserver.getName());
      GameWindow.WriteStringUTF(var0, defaultForGM.getName());
      GameWindow.WriteStringUTF(var0, defaultForModerator.getName());
      GameWindow.WriteStringUTF(var0, defaultForAdmin.getName());
   }

   private static void load(ByteBuffer var0, int var1) {
      roles.clear();
      addStatic();
      byte var2 = var0.get();

      for(int var3 = 0; var3 < var2; ++var3) {
         Role var4 = new Role("");
         var4.load(var0, var1);
         roles.add(var4);
      }

      Role var5 = getRole(GameWindow.ReadString(var0));
      defaultForBanned = var5 == null ? defaultForBanned : var5;
      var5 = getRole(GameWindow.ReadString(var0));
      defaultForNewUser = var5 == null ? defaultForNewUser : var5;
      var5 = getRole(GameWindow.ReadString(var0));
      defaultForUser = var5 == null ? defaultForUser : var5;
      var5 = getRole(GameWindow.ReadString(var0));
      defaultForPriorityUser = var5 == null ? defaultForPriorityUser : var5;
      var5 = getRole(GameWindow.ReadString(var0));
      defaultForObserver = var5 == null ? defaultForObserver : var5;
      var5 = getRole(GameWindow.ReadString(var0));
      defaultForGM = var5 == null ? defaultForGM : var5;
      var5 = getRole(GameWindow.ReadString(var0));
      defaultForModerator = var5 == null ? defaultForModerator : var5;
      var5 = getRole(GameWindow.ReadString(var0));
      defaultForAdmin = var5 == null ? defaultForAdmin : var5;
   }

   static {
      addStatic();
   }
}
