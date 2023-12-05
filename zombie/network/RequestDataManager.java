package zombie.network;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.core.Translator;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.gameStates.GameLoadingState;
import zombie.network.packets.RequestDataPacket;

public class RequestDataManager {
   public static final int smallFileSize = 1024;
   public static final int maxLargeFileSize = 52428800;
   public static final int packSize = 204800;
   private final ArrayList<RequestData> requests = new ArrayList();
   private static RequestDataManager instance;

   private RequestDataManager() {
   }

   public static RequestDataManager getInstance() {
      if (instance == null) {
         instance = new RequestDataManager();
      }

      return instance;
   }

   public void ACKWasReceived(RequestDataPacket.RequestID var1, UdpConnection var2, int var3) {
      RequestData var4 = null;

      for(int var5 = 0; var5 <= this.requests.size(); ++var5) {
         if (((RequestData)this.requests.get(var5)).connectionGUID == var2.getConnectedGUID()) {
            var4 = (RequestData)this.requests.get(var5);
            break;
         }
      }

      if (var4 != null && var4.id == var1) {
         this.sendData(var4);
      }
   }

   public void putDataForTransmit(RequestDataPacket.RequestID var1, UdpConnection var2, ByteBuffer var3) {
      RequestData var4 = new RequestData(var1, var3, var2.getConnectedGUID());
      this.requests.add(var4);
      this.sendData(var4);
   }

   public void disconnect(UdpConnection var1) {
      long var2 = System.currentTimeMillis();
      this.requests.removeIf((var3) -> {
         return var2 - var3.creationTime > 60000L || var3.connectionGUID == var1.getConnectedGUID();
      });
   }

   public void clear() {
      this.requests.clear();
   }

   private void sendData(RequestData var1) {
      var1.creationTime = System.currentTimeMillis();
      int var2 = var1.bb.limit();
      var1.realTransmittedFromLastACK = 0;
      UdpConnection var3 = GameServer.udpEngine.getActiveConnection(var1.connectionGUID);
      RequestDataPacket var4 = new RequestDataPacket();
      var4.setPartData(var1.id, var1.bb);

      while(var1.realTransmittedFromLastACK < 204800) {
         int var5 = Math.min(1024, var2 - var1.realTransmitted);
         if (var5 == 0) {
            break;
         }

         var4.setPartDataParameters(var1.realTransmitted, var5);
         ByteBufferWriter var6 = var3.startPacket();
         PacketTypes.PacketType.RequestData.doPacket(var6);
         var4.write(var6);
         PacketTypes.PacketType.RequestData.send(var3);
         var1.realTransmittedFromLastACK += var5;
         var1.realTransmitted += var5;
      }

      if (var1.realTransmitted == var2) {
         this.requests.remove(var1);
      }

   }

   public ByteBuffer receiveClientData(RequestDataPacket.RequestID var1, ByteBuffer var2, int var3, int var4) {
      RequestData var5 = null;

      for(int var6 = 0; var6 < this.requests.size(); ++var6) {
         if (((RequestData)this.requests.get(var6)).id == var1) {
            var5 = (RequestData)this.requests.get(var6);
            break;
         }
      }

      if (var5 == null) {
         var5 = new RequestData(var1, var3, 0L);
         this.requests.add(var5);
      }

      var5.bb.position(var4);
      var5.bb.put(var2.array(), 0, var2.limit());
      var5.realTransmitted += var2.limit();
      var5.realTransmittedFromLastACK += var2.limit();
      if (var5.realTransmittedFromLastACK >= 204800) {
         var5.realTransmittedFromLastACK = 0;
         RequestDataPacket var8 = new RequestDataPacket();
         var8.setACK(var5.id);
         ByteBufferWriter var7 = GameClient.connection.startPacket();
         PacketTypes.PacketType.RequestData.doPacket(var7);
         var8.write(var7);
         PacketTypes.PacketType.RequestData.send(GameClient.connection);
      }

      GameLoadingState.GameLoadingString = Translator.getText("IGUI_MP_DownloadedLargeFile", var5.realTransmitted * 100 / var3, var5.id.getDescriptor());
      if (var5.realTransmitted == var3) {
         this.requests.remove(var5);
         var5.bb.position(0);
         return var5.bb;
      } else {
         return null;
      }
   }

   static class RequestData {
      private final RequestDataPacket.RequestID id;
      private final ByteBuffer bb;
      private final long connectionGUID;
      private long creationTime = System.currentTimeMillis();
      private int realTransmitted;
      private int realTransmittedFromLastACK;

      public RequestData(RequestDataPacket.RequestID var1, ByteBuffer var2, long var3) {
         this.id = var1;
         this.bb = ByteBuffer.allocate(var2.position());
         this.bb.put(var2.array(), 0, this.bb.limit());
         this.connectionGUID = var3;
         this.realTransmitted = 0;
         this.realTransmittedFromLastACK = 0;
      }

      public RequestData(RequestDataPacket.RequestID var1, int var2, long var3) {
         this.id = var1;
         this.bb = ByteBuffer.allocate(var2);
         this.bb.clear();
         this.connectionGUID = var3;
         this.realTransmitted = 0;
         this.realTransmittedFromLastACK = 0;
      }
   }
}
