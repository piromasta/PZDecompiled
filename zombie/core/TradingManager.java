package zombie.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Optional;
import zombie.characters.IsoPlayer;
import zombie.core.raknet.UdpConnection;
import zombie.inventory.InventoryItem;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.savefile.ServerPlayerDB;

public class TradingManager {
   private static TradingManager instance;
   final ArrayList<Trading> tradings = new ArrayList();
   final ArrayList<Trading> finalisingTradings = new ArrayList();
   final ArrayList<Trading> finalisedTradings = new ArrayList();

   public TradingManager() {
   }

   public static TradingManager getInstance() {
      if (instance == null) {
         instance = new TradingManager();
      }

      return instance;
   }

   public void addNewTrading(IsoPlayer var1, IsoPlayer var2) {
      this.tradings.add(new Trading(var1, var2));
   }

   public void addItem(IsoPlayer var1, InventoryItem var2) {
      Iterator var3 = this.tradings.iterator();

      while(var3.hasNext()) {
         Trading var4 = (Trading)var3.next();
         if (var4.playerA == var1) {
            var4.playerAItems.add(var2);
            break;
         }

         if (var4.playerB == var1) {
            var4.playerBItems.add(var2);
            break;
         }
      }

   }

   public void removeItem(IsoPlayer var1, int var2) {
      Iterator var3 = this.tradings.iterator();

      while(var3.hasNext()) {
         Trading var4 = (Trading)var3.next();
         if (var4.playerA == var1) {
            var4.playerAItems.removeIf((var1x) -> {
               return var1x.getID() == var2;
            });
            break;
         }

         if (var4.playerB == var1) {
            var4.playerAItems.removeIf((var1x) -> {
               return var1x.getID() == var2;
            });
            break;
         }
      }

   }

   public void dealSealStatusChanged(IsoPlayer var1, boolean var2) {
      Iterator var3 = this.tradings.iterator();

      while(var3.hasNext()) {
         Trading var4 = (Trading)var3.next();
         if (var4.playerA == var1) {
            var4.playerAAgreement = var2;
            break;
         }

         if (var4.playerB == var1) {
            var4.playerBAgreement = var2;
            break;
         }
      }

   }

   public void cancelTrading(IsoPlayer var1) {
      this.tradings.removeIf((var1x) -> {
         return var1x.playerA == var1 || var1x.playerB == var1;
      });
   }

   public void finishTrading(IsoPlayer var1) {
      Optional var2 = this.tradings.stream().filter((var1x) -> {
         return var1x.playerA == var1 || var1x.playerB == var1;
      }).findFirst();
      if (var2.isPresent()) {
         synchronized(this.finalisingTradings) {
            this.finalisingTradings.add((Trading)var2.get());
         }

         this.tradings.remove(var2.get());
      }

   }

   public void update() {
      Iterator var1 = this.finalisingTradings.iterator();

      while(true) {
         while(var1.hasNext()) {
            Trading var2 = (Trading)var1.next();
            var2.time = System.currentTimeMillis();
            UdpConnection var3 = GameServer.getConnectionFromPlayer(var2.playerA);
            byte var4 = var3.getPlayerIndex(var2.playerA);
            UdpConnection var5 = GameServer.getConnectionFromPlayer(var2.playerB);
            byte var6 = var5.getPlayerIndex(var2.playerB);
            if (var3 != null && var3.isFullyConnected() && var5 != null && var5.isFullyConnected() && var4 != -1 && var6 != -1) {
               var2.time = System.currentTimeMillis();
               Iterator var7 = var2.playerAItems.iterator();

               InventoryItem var8;
               while(var7.hasNext()) {
                  var8 = (InventoryItem)var7.next();
                  var2.playerA.getInventory().Remove(var8);
               }

               INetworkPacket.send(var3, PacketTypes.PacketType.RemoveInventoryItemFromContainer, var2.playerA.getInventory(), var2.playerAItems);
               var2.time = System.currentTimeMillis();
               var7 = var2.playerBItems.iterator();

               while(var7.hasNext()) {
                  var8 = (InventoryItem)var7.next();
                  var2.playerB.getInventory().Remove(var8);
               }

               INetworkPacket.send(var5, PacketTypes.PacketType.RemoveInventoryItemFromContainer, var2.playerB.getInventory(), var2.playerBItems);
               var2.time = System.currentTimeMillis();
               var7 = var2.playerBItems.iterator();

               while(var7.hasNext()) {
                  var8 = (InventoryItem)var7.next();
                  var2.playerA.getInventory().AddItem(var8);
               }

               INetworkPacket.send(var3, PacketTypes.PacketType.AddInventoryItemToContainer, var2.playerA.getInventory(), var2.playerBItems);
               var2.time = System.currentTimeMillis();
               var7 = var2.playerAItems.iterator();

               while(var7.hasNext()) {
                  var8 = (InventoryItem)var7.next();
                  var2.playerB.getInventory().AddItem(var8);
               }

               INetworkPacket.send(var5, PacketTypes.PacketType.AddInventoryItemToContainer, var2.playerB.getInventory(), var2.playerAItems);
               ServerPlayerDB.getInstance().serverUpdateNetworkCharacter(var2.playerA, var4, var3);
               ServerPlayerDB.getInstance().serverUpdateNetworkCharacter(var2.playerB, var6, var5);
               this.finalisedTradings.add(var2);
            } else {
               this.finalisedTradings.add(var2);
            }
         }

         if (!this.finalisedTradings.isEmpty()) {
            synchronized(this.finalisingTradings) {
               this.finalisingTradings.removeAll(this.finalisedTradings);
            }
         }

         return;
      }
   }

   private class Trading {
      long time = System.currentTimeMillis();
      IsoPlayer playerA;
      IsoPlayer playerB;
      ArrayList<InventoryItem> playerAItems;
      ArrayList<InventoryItem> playerBItems;
      boolean playerAAgreement;
      boolean playerBAgreement;

      public Trading(IsoPlayer var2, IsoPlayer var3) {
         this.playerA = var2;
         this.playerB = var3;
         this.playerAItems = new ArrayList();
         this.playerBItems = new ArrayList();
         this.playerAAgreement = false;
         this.playerBAgreement = false;
      }
   }
}
