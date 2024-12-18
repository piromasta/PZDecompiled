package zombie.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import zombie.GameTime;
import zombie.characters.Capability;
import zombie.characters.Faction;
import zombie.characters.IsoPlayer;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.iso.areas.SafeHouse;
import zombie.network.packets.INetworkPacket;
import zombie.util.StringUtils;

public final class WarManager {
   private static final ArrayList<War> wars = new ArrayList();
   private static final ArrayList<War> temp = new ArrayList();

   private WarManager() {
   }

   public static ArrayList<War> getWarRelevent(IsoPlayer var0) {
      temp.clear();
      if (var0.role != null && var0.role.haveCapability(Capability.CanGoInsideSafehouses)) {
         temp.addAll(wars);
      } else {
         Iterator var1 = wars.iterator();

         while(var1.hasNext()) {
            War var2 = (War)var1.next();
            if (var2.isRelevant(var0.getUsername())) {
               temp.add(var2);
            }
         }
      }

      return temp;
   }

   public static War getWarNearest(IsoPlayer var0) {
      War var1 = null;
      Iterator var2 = getWarRelevent(var0).iterator();

      while(true) {
         War var3;
         do {
            if (!var2.hasNext()) {
               return var1;
            }

            var3 = (War)var2.next();
         } while(var1 != null && var3.timestamp >= var1.timestamp);

         var1 = var3;
      }
   }

   public static War getWar(int var0, String var1) {
      Iterator var2 = wars.iterator();

      War var3;
      do {
         if (!var2.hasNext()) {
            return null;
         }

         var3 = (War)var2.next();
      } while(var3.onlineID != var0 || !var3.attacker.equals(var1));

      return var3;
   }

   public static boolean isWarClaimed(int var0) {
      Iterator var1 = wars.iterator();

      War var2;
      do {
         if (!var1.hasNext()) {
            return true;
         }

         var2 = (War)var1.next();
      } while(var2.onlineID != var0);

      return false;
   }

   public static boolean isWarClaimed(String var0) {
      Iterator var1 = wars.iterator();

      War var2;
      do {
         if (!var1.hasNext()) {
            return false;
         }

         var2 = (War)var1.next();
      } while(!var2.attacker.equals(var0));

      return true;
   }

   public static boolean isWarStarted(int var0, String var1) {
      Iterator var2 = wars.iterator();

      War var3;
      do {
         if (!var2.hasNext()) {
            return false;
         }

         var3 = (War)var2.next();
      } while(var3.onlineID != var0 || !WarManager.State.Started.equals(var3.state));

      return var3.isRelevant(var1);
   }

   public static void removeWar(int var0, String var1) {
      wars.removeIf((var2) -> {
         return var2.onlineID == var0 && (StringUtils.isNullOrEmpty(var1) || var2.attacker.equals(var1));
      });
   }

   public static void clear() {
      wars.clear();
   }

   public static void sendWarToPlayer(IsoPlayer var0) {
      Iterator var1 = wars.iterator();

      while(var1.hasNext()) {
         War var2 = (War)var1.next();
         INetworkPacket.send(var0, PacketTypes.PacketType.WarSync, var2);
      }

   }

   public static void updateWar(int var0, String var1, State var2, long var3) {
      Iterator var5 = wars.iterator();

      War var6;
      do {
         if (!var5.hasNext()) {
            War var7 = new War(var0, var1, var2, var3);
            wars.add(var7);
            return;
         }

         var6 = (War)var5.next();
      } while(var6.onlineID != var0 || !var6.attacker.equals(var1));

      var6.setState(var2);
      if (GameClient.bClient || GameServer.bServer && WarManager.State.Ended.equals(var2)) {
         var6.setTimestamp(var3);
      }

   }

   public static void update() {
      long var0 = GameTime.getServerTimeMills();
      Iterator var2 = wars.iterator();

      while(var2.hasNext()) {
         War var3 = (War)var2.next();
         if (var0 >= var3.timestamp && var3.state != null) {
            switch (var3.state) {
               case Claimed:
               case Refused:
                  SafeHouse.hitPoint(var3.onlineID);
                  break;
               case Accepted:
               case Canceled:
                  var3.setTimestamp(var0 + getWarDuration());
                  break;
               case Ended:
                  var2.remove();
            }

            if (var3.state.next != null) {
               var3.setState(var3.state.next);
            }
         }
      }

   }

   public static long getWarDuration() {
      return (long)ServerOptions.instance.WarDuration.getValue() * 1000L;
   }

   public static long getStartDelay() {
      return (long)ServerOptions.instance.WarStartDelay.getValue() * 1000L;
   }

   public static class War {
      private static final HashMap<State, State> transitions = new HashMap<State, State>() {
         {
            this.put(WarManager.State.Accepted, WarManager.State.Claimed);
            this.put(WarManager.State.Canceled, WarManager.State.Claimed);
            this.put(WarManager.State.Refused, WarManager.State.Claimed);
            this.put(WarManager.State.Started, WarManager.State.Accepted);
            this.put(WarManager.State.Blocked, WarManager.State.Canceled);
         }
      };
      private final int onlineID;
      private final String attacker;
      private State state;
      private long timestamp;

      public War(int var1, String var2, State var3, long var4) {
         this.state = WarManager.State.Ended;
         this.onlineID = var1;
         this.attacker = var2;
         this.setTimestamp(var4);
         this.setState(var3);
      }

      public int getOnlineID() {
         return this.onlineID;
      }

      public String getAttacker() {
         return this.attacker;
      }

      public String getDefender() {
         SafeHouse var1 = SafeHouse.getSafeHouse(this.onlineID);
         return var1 != null ? var1.getOwner() : "";
      }

      public State getState() {
         return this.state;
      }

      public boolean isValidState(State var1) {
         return ((State)transitions.get(var1)).equals(this.state);
      }

      public void setState(State var1) {
         DebugLog.Multiplayer.debugln("War id=%d state=%s->%s", this.onlineID, this.state, var1);
         this.state = var1;
         INetworkPacket.sendToAll(PacketTypes.PacketType.WarSync, (UdpConnection)null, this);
      }

      public long getTimestamp() {
         return this.timestamp;
      }

      public void setTimestamp(long var1) {
         DebugLog.Multiplayer.debugln("War id=%d time=%d", this.onlineID, var1 - GameTime.getServerTimeMills());
         this.timestamp = var1;
      }

      public String getTime() {
         String var1 = "00:00:00";
         long var2 = GameTime.getServerTimeMills();
         if (var2 > 0L) {
            long var4 = this.timestamp - var2;
            if (var4 > 0L) {
               long var6 = var4 / 1000L % 60L;
               long var8 = var4 / 60000L % 60L;
               long var10 = var4 / 3600000L;
               var1 = String.format("%02d:%02d:%02d", var10, var8, var6);
            }
         }

         return var1;
      }

      private boolean isRelevant(String var1) {
         if (var1 != null) {
            if (this.attacker.equals(var1)) {
               return true;
            }

            Faction var2 = Faction.getPlayerFaction(this.attacker);
            if (var2 != null && (var2.isOwner(var1) || var2.isMember(var1))) {
               return true;
            }

            SafeHouse var3 = SafeHouse.getSafeHouse(this.onlineID);
            if (var3 != null) {
               String var4 = var3.getOwner();
               if (var4.equals(var1)) {
                  return true;
               }

               Faction var5 = Faction.getPlayerFaction(var4);
               if (var5 != null && (var5.isOwner(var1) || var5.isMember(var1))) {
                  return true;
               }

               if (var3.playerAllowed(var1)) {
                  return true;
               }

               return false;
            }
         }

         return false;
      }
   }

   public static enum State {
      Ended((State)null),
      Started(Ended),
      Blocked(Ended),
      Refused(Ended),
      Claimed(Ended),
      Accepted(Started),
      Canceled(Blocked);

      private final State next;

      private State(State var3) {
         this.next = var3;
      }

      public static State valueOf(int var0) {
         State[] var1 = values();
         int var2 = var1.length;

         for(int var3 = 0; var3 < var2; ++var3) {
            State var4 = var1[var3];
            if (var4.ordinal() == var0) {
               return var4;
            }
         }

         return Ended;
      }
   }
}
