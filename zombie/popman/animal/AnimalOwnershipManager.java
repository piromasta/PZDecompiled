package zombie.popman.animal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import zombie.characters.IsoPlayer;
import zombie.characters.NetworkCharacterAI;
import zombie.characters.animals.IsoAnimal;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameServer;

public class AnimalOwnershipManager {
   private static final AnimalOwnershipManager instance = new AnimalOwnershipManager();
   private static final HashMap<Long, HashSet<Short>> ownerships = new HashMap();
   private static int owned = 0;

   public static AnimalOwnershipManager getInstance() {
      return instance;
   }

   private AnimalOwnershipManager() {
   }

   public HashSet<Short> getOwnership(UdpConnection var1) {
      return (HashSet)ownerships.computeIfAbsent(var1.getConnectedGUID(), (var0) -> {
         return new HashSet();
      });
   }

   public void setOwnershipClient(UdpConnection var1, HashSet<Short> var2) {
      owned = var2.size();
      HashSet var3 = new HashSet();
      Iterator var4 = this.getOwnership(var1).iterator();

      Short var5;
      while(var4.hasNext()) {
         var5 = (Short)var4.next();
         if (!var2.contains(var5)) {
            var3.add(var5);
         }
      }

      var4 = var3.iterator();

      while(var4.hasNext()) {
         var5 = (Short)var4.next();
         this.getOwnership(var1).remove(var5);
         IsoAnimal var6 = AnimalInstanceManager.getInstance().get(var5);
         if (var6 != null) {
            var6.getNetworkCharacterAI().setOwnership((UdpConnection)null);
         }
      }

      HashSet var9 = new HashSet();
      Iterator var10 = var2.iterator();

      while(var10.hasNext()) {
         Short var12 = (Short)var10.next();
         if (!this.getOwnership(var1).contains(var12)) {
            var9.add(var12);
         }
      }

      HashSet var11 = new HashSet();
      Iterator var13 = var9.iterator();

      while(var13.hasNext()) {
         Short var7 = (Short)var13.next();
         IsoAnimal var8 = AnimalInstanceManager.getInstance().get(var7);
         if (var8 != null) {
            this.getOwnership(var1).add(var7);
            var8.getNetworkCharacterAI().setOwnership(var1);
            var8.getNetworkCharacterAI().getAnimalPacket().pfbData.restore(var8);
         } else {
            var11.add(var7);
         }
      }

      AnimalSynchronizationManager.getInstance().setRequested(var1, var11);
   }

   public void update() {
      owned = 0;
      ownerships.values().forEach(HashSet::clear);
      Iterator var1 = AnimalInstanceManager.getInstance().getAnimals().iterator();

      while(var1.hasNext()) {
         IsoAnimal var2 = (IsoAnimal)var1.next();
         UdpConnection var3 = this.updateOwnership(var2.getNetworkCharacterAI());
         if (var3 != null) {
            this.getOwnership(var3).add(var2.getOnlineID());
            ++owned;
         }
      }

   }

   private UdpConnection updateOwnership(NetworkCharacterAI var1) {
      if (var1.isOwnershipOnServer()) {
         return this.setOwnershipServer(var1, (UdpConnection)null);
      } else if (var1.getOwnership().isBlocked(1000)) {
         return var1.getOwnership().getConnection();
      } else {
         IsoPlayer var2 = var1.getRelatedPlayer();
         UdpConnection var3;
         if (var2 != null) {
            var3 = GameServer.getConnectionFromPlayer(var2);
            if (var3 != null && var3.isFullyConnected() && !GameServer.isDelayedDisconnect(var3)) {
               return this.setOwnershipServer(var1, var3);
            }

            var2 = null;
         }

         var3 = var1.getOwnership().getConnection();
         float var4 = 1.0F / 0.0F;
         if (var3 != null) {
            var4 = var3.getRelevantAndDistance(var1.getX(), var1.getY(), var1.getZ());
         }

         Iterator var5 = GameServer.udpEngine.connections.iterator();

         while(true) {
            UdpConnection var6;
            do {
               do {
                  if (!var5.hasNext()) {
                     if (var3 != null && !var3.RelevantTo(var1.getX(), var1.getY(), (float)((var3.ReleventRange - 2) * 10))) {
                        var3 = null;
                     }

                     return this.setOwnershipServer(var1, var3);
                  }

                  var6 = (UdpConnection)var5.next();
               } while(var6 == var3);
            } while(GameServer.isDelayedDisconnect(var6));

            IsoPlayer[] var7 = var6.players;
            int var8 = var7.length;

            for(int var9 = 0; var9 < var8; ++var9) {
               IsoPlayer var10 = var7[var9];
               if (var10 != null && var10.isAlive()) {
                  float var11 = var10.getRelevantAndDistance(var1.getX(), var1.getY(), (float)(var6.ReleventRange - 2));
                  if (!Float.isInfinite(var11) && var4 > var11 * 1.618034F) {
                     var3 = var6;
                     var4 = var11;
                  }
               }
            }
         }
      }
   }

   public UdpConnection setOwnershipServer(NetworkCharacterAI var1, UdpConnection var2) {
      if (var1.isDead()) {
         if (var1.getOwnership().getConnection() == null) {
            var1.becomeCorpse();
         }
      } else if (var1.getOwnership().getConnection() != var2) {
         AnimalSynchronizationManager.getInstance().setExtraUpdate(var1.getOwnership().getConnection());
         AnimalSynchronizationManager.getInstance().setExtraUpdate(var2);
      }

      var1.setOwnership(var2);
      return var2;
   }

   public int getOwned() {
      return owned;
   }

   public UdpConnection getOwner(IsoAnimal var1) {
      return GameServer.bServer ? var1.getNetworkCharacterAI().getOwnership().getConnection() : null;
   }

   public IsoPlayer getOwnership(IsoAnimal var1) {
      if (GameServer.bServer) {
         UdpConnection var2 = this.getOwner(var1);
         if (var2 != null) {
            return GameServer.getPlayerFromConnection(var2, 0);
         }
      }

      return null;
   }
}
