package zombie.worldMap;

import java.util.ArrayList;
import java.util.HashMap;
import zombie.characters.IsoPlayer;

public final class WorldMapRemotePlayers {
   public static final WorldMapRemotePlayers instance = new WorldMapRemotePlayers();
   private final ArrayList<WorldMapRemotePlayer> playerList = new ArrayList();
   private final HashMap<Short, WorldMapRemotePlayer> playerLookup = new HashMap();

   public WorldMapRemotePlayers() {
   }

   public WorldMapRemotePlayer getOrCreatePlayerByID(short var1) {
      WorldMapRemotePlayer var2 = (WorldMapRemotePlayer)this.playerLookup.get(var1);
      if (var2 == null) {
         var2 = new WorldMapRemotePlayer(var1);
         this.playerList.add(var2);
         this.playerLookup.put(var1, var2);
      }

      return var2;
   }

   public WorldMapRemotePlayer getOrCreatePlayer(IsoPlayer var1) {
      return this.getOrCreatePlayerByID(var1.OnlineID);
   }

   public WorldMapRemotePlayer getPlayerByID(short var1) {
      return (WorldMapRemotePlayer)this.playerLookup.get(var1);
   }

   public ArrayList<WorldMapRemotePlayer> getPlayers() {
      return this.playerList;
   }

   public void removePlayerByID(short var1) {
      this.playerList.removeIf((var1x) -> {
         return var1x.getOnlineID() == var1;
      });
      this.playerLookup.remove(var1);
   }

   public void Reset() {
      this.playerList.clear();
      this.playerLookup.clear();
   }
}
