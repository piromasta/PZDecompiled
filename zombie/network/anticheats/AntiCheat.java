package zombie.network.anticheats;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Objects;
import zombie.SystemDisabler;
import zombie.characters.Capability;
import zombie.characters.IsoGameCharacter;
import zombie.core.Core;
import zombie.core.logger.LoggerManager;
import zombie.core.raknet.UdpConnection;
import zombie.core.znet.SteamUtils;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.network.GameServer;
import zombie.network.ServerOptions;
import zombie.network.ServerWorldDatabase;
import zombie.network.Userlog;
import zombie.network.chat.ChatServer;
import zombie.network.packets.INetworkPacket;

public enum AntiCheat {
   Item(AntiCheatItem.class, ServerOptions.instance.AntiCheatItem, 6),
   Transaction(AntiCheatTransaction.class, ServerOptions.instance.AntiCheatItem, 6),
   Safety(AntiCheatSafety.class, ServerOptions.instance.AntiCheatSafety, 1),
   HitDamage(AntiCheatHitDamage.class, ServerOptions.instance.AntiCheatHit, 1),
   HitLongDistance(AntiCheatHitLongDistance.class, ServerOptions.instance.AntiCheatHit, 2),
   HitShortDistance(AntiCheatHitShortDistance.class, ServerOptions.instance.AntiCheatHit, 2),
   HitWeaponAmmo(AntiCheatHitWeaponAmmo.class, ServerOptions.instance.AntiCheatHit, 1),
   HitWeaponRate(AntiCheatHitWeaponRate.class, ServerOptions.instance.AntiCheatHit, 1),
   HitWeaponRange(AntiCheatHitWeaponRange.class, ServerOptions.instance.AntiCheatHit, 1),
   Owner(AntiCheatOwner.class, ServerOptions.instance.AntiCheatHit, 4),
   Target(AntiCheatTarget.class, ServerOptions.instance.AntiCheatHit, 1),
   PacketRakNet(AntiCheatPacketRakNet.class, ServerOptions.instance.AntiCheatPacket, 1),
   PacketException(AntiCheatPacketException.class, ServerOptions.instance.AntiCheatPacket, 1),
   PacketType(AntiCheatPacketType.class, ServerOptions.instance.AntiCheatPacket, 1),
   XP(AntiCheatXP.class, ServerOptions.instance.AntiCheatXP, 2),
   XPUpdate(AntiCheatXPUpdate.class, ServerOptions.instance.AntiCheatXP, 2),
   XPPlayer(AntiCheatXPPlayer.class, ServerOptions.instance.AntiCheatXP, 1),
   Time(AntiCheatTime.class, ServerOptions.instance.AntiCheatTime, 4),
   TimeUpdate(AntiCheatTimeUpdate.class, ServerOptions.instance.AntiCheatTime, 4),
   Recipe(AntiCheatRecipe.class, ServerOptions.instance.AntiCheatRecipe, 4),
   RecipeUpdate(AntiCheatRecipeUpdate.class, ServerOptions.instance.AntiCheatRecipe, 4),
   Player(AntiCheatPlayer.class, ServerOptions.instance.AntiCheatPlayer, 4),
   PlayerUpdate(AntiCheatPlayerUpdate.class, ServerOptions.instance.AntiCheatPlayer, 4),
   Power(AntiCheatPower.class, ServerOptions.instance.AntiCheatPermission, 1),
   Role(AntiCheatRole.class, ServerOptions.instance.AntiCheatPermission, 1),
   Capability(AntiCheatCapability.class, ServerOptions.instance.AntiCheatPermission, 1),
   Fire(AntiCheatFire.class, ServerOptions.instance.AntiCheatFire, 1),
   Smoke(AntiCheatSmoke.class, ServerOptions.instance.AntiCheatFire, 1),
   SafeHousePlayer(AntiCheatSafeHousePlayer.class, ServerOptions.instance.AntiCheatSafeHouse, 1),
   SafeHouseSurviving(AntiCheatSafeHouseSurvivor.class, ServerOptions.instance.AntiCheatSafeHouse, 1),
   Speed(AntiCheatSpeed.class, ServerOptions.instance.AntiCheatMovement, 8),
   NoClip(AntiCheatNoClip.class, ServerOptions.instance.AntiCheatMovement, 4),
   Checksum(AntiCheatChecksum.class, ServerOptions.instance.AntiCheatChecksum, 1),
   ChecksumUpdate(AntiCheatChecksumUpdate.class, ServerOptions.instance.AntiCheatChecksum, 1),
   ServerCustomizationDDOS(AntiCheatServerCustomizationDDOS.class, ServerOptions.instance.AntiCheatServerCustomization, 4),
   None(AntiCheatNone.class, (ServerOptions.EnumServerOption)null, 0);

   private static final long USER_LOG_INTERVAL_MS = 1000L;
   public final AbstractAntiCheat anticheat;
   public final ServerOptions.EnumServerOption option;
   public final int maxSuspiciousCounter;

   private AntiCheat(Class var3, ServerOptions.EnumServerOption var4, int var5) {
      AbstractAntiCheat var6;
      try {
         var6 = (AbstractAntiCheat)var3.getDeclaredConstructor().newInstance();
      } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | InstantiationException var8) {
         var6 = null;
      }

      this.anticheat = var6;
      this.option = var4;
      this.maxSuspiciousCounter = var5;
   }

   public boolean isValid(UdpConnection var1, INetworkPacket var2) {
      String var3 = this.anticheat.validate(var1, var2);
      if (var3 != null) {
         this.anticheat.react(var1, var2);
         if ("".equals(var3)) {
            DebugLog.Multiplayer.warn("Anti-Cheat %s skipped: %s", this.anticheat.getClass().getSimpleName(), var3);
            log(var1, this, (Integer)var1.validator.getCounters().getOrDefault(this, 0), "'skip'");
         } else {
            DebugLog.Multiplayer.warn("Anti-Cheat %s triggered: %s", this.anticheat.getClass().getSimpleName(), var3);
            this.act(var1, "'" + var3 + "'");
         }

         return false;
      } else {
         return true;
      }
   }

   public static void update(UdpConnection var0) {
      AntiCheat[] var1 = values();
      int var2 = var1.length;

      for(int var3 = 0; var3 < var2; ++var3) {
         AntiCheat var4 = var1[var3];
         if (!var4.anticheat.update(var0) && !GameServer.isDelayedDisconnect(var0)) {
            var4.act(var0, var4.name());
         }
      }

   }

   public static void preUpdate(UdpConnection var0) {
      AntiCheat[] var1 = values();
      int var2 = var1.length;

      for(int var3 = 0; var3 < var2; ++var3) {
         AntiCheat var4 = var1[var3];
         if (!var4.anticheat.preUpdate(var0)) {
            DebugLog.Multiplayer.warn("AntiCheat %s pre-update check failed", var4.name());
         }
      }

   }

   public static void log(UdpConnection var0, AntiCheat var1, int var2, String var3) {
      try {
         String var4 = String.format("Warning: player=\"%s\" option=%s anti-cheat=%s reason=%s counter=%d/%d action=%s", var0.username, var1.option.getName(), var1.name(), var3, var2, var1.maxSuspiciousCounter, AntiCheat.Policy.name(var1.option.getValue()));
         LoggerManager.getLogger("user").write(var4);
         ChatServer.getInstance().sendMessageToAdminChat(var4);
      } catch (Exception var5) {
         DebugLog.Multiplayer.printException(var5, "Log anti-cheat error", LogSeverity.Error);
      }

   }

   public void act(UdpConnection var1, String var2) {
      if (this.isApplicable(var1)) {
         int var3 = var1.validator.report(this);
         log(var1, this, var3, var2);
         doLogUser(var1, this.option.getName(), var2);
         if (var3 >= this.maxSuspiciousCounter) {
            switch (this.option.getValue()) {
               case 1:
                  doBanUser(var1, this.option.getName(), var2);
                  break;
               case 2:
                  doKickUser(var1, this.option.getName(), var2);
            }
         }
      }

   }

   private boolean doAntiCheatProtection() {
      return !GameServer.bCoop && (!Core.bDebug || SystemDisabler.getKickInDebug());
   }

   private boolean isUntouchable(UdpConnection var1) {
      return !var1.isFullyConnected() || var1.role.haveCapability(zombie.characters.Capability.CantBeKickedByAnticheat) || Arrays.stream(var1.players).filter(Objects::nonNull).anyMatch(IsoGameCharacter::isGodMod);
   }

   private boolean isApplicable(UdpConnection var1) {
      return this.doAntiCheatProtection() && !this.isUntouchable(var1);
   }

   public static void doLogUser(UdpConnection var0, String var1, String var2) {
      long var3 = System.currentTimeMillis();
      if (var3 > var0.lastUnauthorizedPacket) {
         var0.lastUnauthorizedPacket = var3 + 1000L;
         ServerWorldDatabase.instance.addUserlog(var0.username, Userlog.UserlogType.SuspiciousActivity, var2, var1, 1);
      }

   }

   public static void doKickUser(UdpConnection var0, String var1, String var2) {
      ServerWorldDatabase.instance.addUserlog(var0.username, Userlog.UserlogType.Kicked, var2, var1, 1);
      GameServer.kick(var0, "UI_Policy_Kick", "UI_ValidationFailed");
      var0.forceDisconnect(var1);
   }

   public static void doBanUser(UdpConnection var0, String var1, String var2) {
      ServerWorldDatabase.instance.addUserlog(var0.username, Userlog.UserlogType.Banned, var2, var1, 1);

      try {
         ServerWorldDatabase.instance.banUser(var0.username, true);
         if (SteamUtils.isSteamModeEnabled()) {
            String var3 = SteamUtils.convertSteamIDToString(var0.steamID);
            ServerWorldDatabase.instance.banSteamID(var3, var1, true);
         }
      } catch (SQLException var4) {
         DebugLog.Multiplayer.printException(var4, "User ban error", LogSeverity.Error);
      }

      GameServer.kick(var0, "UI_Policy_Ban", "UI_ValidationFailed");
      var0.forceDisconnect(var1);
   }

   public static class Policy {
      public static final int Ban = 1;
      public static final int Kick = 2;
      public static final int Log = 3;

      public Policy() {
      }

      public static String name(int var0) {
         String var1;
         switch (var0) {
            case 1:
               var1 = "Ban";
               break;
            case 2:
               var1 = "Kick";
               break;
            case 3:
               var1 = "Log";
               break;
            default:
               var1 = "Unknown";
         }

         return var1;
      }
   }
}
