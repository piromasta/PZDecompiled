package zombie.popman.animal;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import zombie.characters.animals.IsoAnimal;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.core.utils.UpdateLimit;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.character.AnimalOwnershipPacket;
import zombie.network.packets.character.AnimalUpdatePacket;
import zombie.network.packets.character.AnimalUpdateReliablePacket;
import zombie.network.packets.character.AnimalUpdateUnreliablePacket;

public class AnimalSynchronizationManager {
   private static final AnimalSynchronizationManager instance = new AnimalSynchronizationManager();
   private static final HashMap<Long, HashSet<Short>> requests = new HashMap();
   private static final HashSet<Short> receivedFromClients = new HashSet();
   private static final HashSet<Short> receivedToSend = new HashSet();
   private static final HashSet<Short> deletedByServer = new HashSet();
   private static final HashMap<Long, HashSet<Short>> deletedToSend = new HashMap();
   private static final HashSet<Short> temp = new HashSet();
   private static final HashSet<UdpConnection> extraUpdate = new HashSet();
   private static final UpdateLimit sendAsReliable = new UpdateLimit(2000L);
   private static final UpdateLimit sendAnimalTimer = new UpdateLimit(1000L);

   public static AnimalSynchronizationManager getInstance() {
      return instance;
   }

   private AnimalSynchronizationManager() {
   }

   public HashSet<Short> getDeleted(UdpConnection var1) {
      return (HashSet)deletedToSend.computeIfAbsent(var1.getConnectedGUID(), (var0) -> {
         return new HashSet();
      });
   }

   public void setExtraUpdate(UdpConnection var1) {
      if (var1 != null) {
         extraUpdate.add(var1);
      }

   }

   public void setReceived(HashSet<Short> var1) {
      receivedFromClients.addAll(var1);
   }

   public void setReceived(Short var1) {
      receivedFromClients.add(var1);
   }

   public void setRequested(UdpConnection var1, HashSet<Short> var2) {
      HashSet var3 = (HashSet)requests.computeIfAbsent(var1.getConnectedGUID(), (var0) -> {
         return new HashSet();
      });
      var3.clear();
      var3.addAll(var2);
   }

   public void update() {
      temp.clear();
      synchronized(deletedByServer) {
         temp.addAll(deletedByServer);
         deletedByServer.clear();
      }

      deletedToSend.forEach((var0, var1x) -> {
         var1x.addAll(temp);
      });
      synchronized(receivedFromClients) {
         receivedFromClients.removeAll(temp);
         receivedToSend.addAll(receivedFromClients);
         receivedFromClients.clear();
      }

      boolean var1 = sendAsReliable.Check();
      if (var1) {
         sendAsReliable.Reset();
      }

      Iterator var2 = GameServer.udpEngine.connections.iterator();

      while(var2.hasNext()) {
         UdpConnection var3 = (UdpConnection)var2.next();
         if (var3 != null && var3.isFullyConnected()) {
            this.sendToClient(var3, var1);
         }
      }

      receivedToSend.clear();
   }

   public void updateInternal() {
      if (sendAnimalTimer.Check()) {
         sendAnimalTimer.Reset(200L);
         boolean var1 = sendAsReliable.Check();
         if (var1) {
            sendAsReliable.Reset();
         }

         getInstance().sendUpdateToServer(GameClient.connection, var1);
      }

   }

   private void sendUpdateToServer(UdpConnection var1, boolean var2) {
      PacketTypes.PacketType var3;
      Object var4;
      if (var2) {
         var3 = PacketTypes.PacketType.AnimalUpdateReliable;
         var4 = (AnimalUpdateReliablePacket)PacketTypes.getPacket(var3, var1);
      } else {
         var3 = PacketTypes.PacketType.AnimalUpdateUnreliable;
         var4 = (AnimalUpdateUnreliablePacket)PacketTypes.getPacket(var3, var1);
      }

      HashSet var5 = ((AnimalUpdatePacket)var4).getUpdated();
      var5.clear();
      var5.addAll(AnimalOwnershipManager.getInstance().getOwnership(GameClient.connection));
      HashSet var6 = ((AnimalUpdatePacket)var4).getRequested();
      var6.clear();
      var6.addAll((Collection)requests.computeIfAbsent(var1.getConnectedGUID(), (var0) -> {
         return new HashSet();
      }));
      if (!var5.isEmpty() || !var6.isEmpty()) {
         ByteBufferWriter var7 = var1.startPacket();
         var3.doPacket(var7);
         ((AnimalUpdatePacket)var4).write(var7);
         var3.send(var1);
         requests.clear();
      }

   }

   private void sendOwnershipToClient(UdpConnection var1) {
      AnimalOwnershipPacket var2 = (AnimalOwnershipPacket)PacketTypes.getPacket(PacketTypes.PacketType.AnimalOwnership, var1);
      HashSet var3 = AnimalOwnershipManager.getInstance().getOwnership(var1);
      HashSet var4;
      if (var3 != null) {
         var4 = var2.getOwned();
         var4.clear();
         var4.addAll(var3);
      }

      var4 = var2.getDeleted();
      var4.clear();
      var4.addAll(this.getDeleted(var1));
      ByteBufferWriter var5 = var1.startPacket();
      PacketTypes.PacketType.AnimalOwnership.doPacket(var5);
      var2.write(var5);
      PacketTypes.PacketType.AnimalOwnership.send(var1);
      this.getDeleted(var1).clear();
   }

   private void sendUpdateToClient(UdpConnection var1, boolean var2) {
      PacketTypes.PacketType var3;
      Object var4;
      if (var2) {
         var3 = PacketTypes.PacketType.AnimalUpdateReliable;
         var4 = (AnimalUpdateReliablePacket)PacketTypes.getPacket(var3, var1);
      } else {
         var3 = PacketTypes.PacketType.AnimalUpdateUnreliable;
         var4 = (AnimalUpdateUnreliablePacket)PacketTypes.getPacket(var3, var1);
      }

      HashSet var5 = ((AnimalUpdatePacket)var4).getUpdated();
      var5.clear();
      Iterator var6 = receivedToSend.iterator();

      while(var6.hasNext()) {
         short var7 = (Short)var6.next();
         IsoAnimal var8 = AnimalInstanceManager.getInstance().get(var7);
         if (var8 != null && var8.getNetworkCharacterAI().isValid(var1)) {
            var5.add(var7);
         }
      }

      HashSet var9 = ((AnimalUpdatePacket)var4).getRequested();
      var9.clear();
      var9.addAll((Collection)requests.computeIfAbsent(var1.getConnectedGUID(), (var0) -> {
         return new HashSet();
      }));
      if (!var5.isEmpty() || !var9.isEmpty()) {
         ByteBufferWriter var10 = var1.startPacket();
         var3.doPacket(var10);
         ((AnimalUpdatePacket)var4).write(var10);
         var3.send(var1);
         ((HashSet)requests.computeIfAbsent(var1.getConnectedGUID(), (var0) -> {
            return new HashSet();
         })).clear();
      }

   }

   private void sendToClient(UdpConnection var1, boolean var2) {
      boolean var3 = extraUpdate.contains(var1);
      if (var3 || var1.timerOwnershipAnimal.check()) {
         var1.timerOwnershipAnimal.reset(1000L);
         this.sendOwnershipToClient(var1);
      }

      if (var3 || var1.timerUpdateAnimal.check()) {
         var1.timerUpdateAnimal.reset(100L);
         this.sendUpdateToClient(var1, var2);
      }

      extraUpdate.remove(var1);
   }

   public void delete(short var1) {
      deletedByServer.add(var1);
   }
}
