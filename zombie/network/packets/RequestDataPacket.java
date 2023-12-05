package zombie.network.packets;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.PersistentOutfits;
import zombie.SharedDescriptors;
import zombie.ZomboidFileSystem;
import zombie.core.Translator;
import zombie.core.logger.ExceptionLogger;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.iso.IsoWorld;
import zombie.network.ConnectionDetails;
import zombie.network.ConnectionManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.RequestDataManager;
import zombie.network.ServerWorldDatabase;
import zombie.radio.ZomboidRadio;
import zombie.radio.media.RecordedMedia;

public class RequestDataPacket implements INetworkPacket {
   RequestType type;
   RequestID id;
   ByteBuffer buffer = null;
   int dataSize;
   int dataSent;
   int partSize;
   public static ByteBuffer large_file_bb = ByteBuffer.allocate(52428800);

   public RequestDataPacket() {
   }

   public void setRequest() {
      this.type = RequestDataPacket.RequestType.Request;
      this.id = RequestDataPacket.RequestID.Descriptors;
   }

   public void setRequest(RequestID var1) {
      this.type = RequestDataPacket.RequestType.Request;
      this.id = var1;
   }

   public void setPartData(RequestID var1, ByteBuffer var2) {
      this.type = RequestDataPacket.RequestType.PartData;
      this.buffer = var2;
      this.id = var1;
      this.dataSize = var2.limit();
   }

   public void setPartDataParameters(int var1, int var2) {
      this.dataSent = var1;
      this.partSize = var2;
   }

   public void setACK(RequestID var1) {
      this.type = RequestDataPacket.RequestType.PartDataACK;
      this.id = var1;
   }

   public void sendConnectingDetails(UdpConnection var1, ServerWorldDatabase.LogonResult var2) {
      if (GameServer.bServer) {
         this.id = RequestDataPacket.RequestID.ConnectionDetails;
         large_file_bb.clear();
         ConnectionDetails.write(var1, var2, large_file_bb);
         this.doSendRequest(var1);
         DebugLog.Multiplayer.debugln("%s %db", this.id.name(), large_file_bb.position());
         ConnectionManager.log("send-packet", "connection-details", var1);
      }
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
      try {
         this.type = RequestDataPacket.RequestType.values()[var1.get()];
      } catch (Exception var4) {
         DebugLog.Multiplayer.printException(var4, "RequestData packet parse failed", LogSeverity.Error);
         this.type = RequestDataPacket.RequestType.None;
      }

      this.id = RequestDataPacket.RequestID.values()[var1.get()];
      if (GameClient.bClient) {
         if (this.type == RequestDataPacket.RequestType.FullData) {
            int var3 = var1.limit() - var1.position();
            large_file_bb.clear();
            large_file_bb.limit(var3);
            large_file_bb.put(var1.array(), var1.position(), var3);
            this.buffer = large_file_bb;
         } else if (this.type == RequestDataPacket.RequestType.PartData) {
            this.dataSize = var1.getInt();
            this.dataSent = var1.getInt();
            this.partSize = var1.getInt();
            large_file_bb.clear();
            large_file_bb.limit(this.partSize);
            large_file_bb.put(var1.array(), var1.position(), this.partSize);
            this.buffer = large_file_bb;
         }
      }

   }

   public void write(ByteBufferWriter var1) {
      var1.putByte((byte)this.type.ordinal());
      var1.putByte((byte)this.id.ordinal());
      if (GameServer.bServer) {
         if (this.type == RequestDataPacket.RequestType.FullData) {
            var1.bb.put(this.buffer.array(), 0, this.buffer.position());
         } else if (this.type == RequestDataPacket.RequestType.PartData) {
            var1.putInt(this.dataSize);
            var1.putInt(this.dataSent);
            var1.putInt(this.partSize);
            var1.bb.put(this.buffer.array(), this.dataSent, this.partSize);
         }
      }

   }

   public void processServer(PacketTypes.PacketType var1, UdpConnection var2) {
      if (!var2.wasInLoadingQueue && this.id != RequestDataPacket.RequestID.ConnectionDetails) {
         GameServer.kick(var2, "UI_Policy_Kick", "The server received an invalid request");
      }

      if (this.type == RequestDataPacket.RequestType.Request) {
         this.doProcessRequest(var2);
      } else if (this.type == RequestDataPacket.RequestType.PartDataACK) {
         RequestDataManager.getInstance().ACKWasReceived(this.id, var2, this.dataSent);
      }

   }

   private void doSendRequest(UdpConnection var1) {
      if (large_file_bb.position() < 1024) {
         this.type = RequestDataPacket.RequestType.FullData;
         this.buffer = large_file_bb;
         ByteBufferWriter var2 = var1.startPacket();
         PacketTypes.PacketType.RequestData.doPacket(var2);
         this.write(var2);
         PacketTypes.PacketType.RequestData.send(var1);
      } else {
         RequestDataManager.getInstance().putDataForTransmit(this.id, var1, large_file_bb);
      }

   }

   private void doProcessRequest(UdpConnection var1) {
      if (this.id == RequestDataPacket.RequestID.Descriptors) {
         try {
            large_file_bb.clear();
            PersistentOutfits.instance.save(large_file_bb);
         } catch (Exception var10) {
            var10.printStackTrace();
         }

         this.doSendRequest(var1);
      }

      if (this.id == RequestDataPacket.RequestID.PlayerZombieDescriptors) {
         SharedDescriptors.Descriptor[] var2 = SharedDescriptors.getPlayerZombieDescriptors();
         int var3 = 0;

         for(int var4 = 0; var4 < var2.length; ++var4) {
            if (var2[var4] != null) {
               ++var3;
            }
         }

         if (var3 * 2 * 1024 > large_file_bb.capacity()) {
            large_file_bb = ByteBuffer.allocate(var3 * 2 * 1024);
         }

         try {
            large_file_bb.clear();
            large_file_bb.putShort((short)var3);
            SharedDescriptors.Descriptor[] var12 = var2;
            int var5 = var2.length;

            for(int var6 = 0; var6 < var5; ++var6) {
               SharedDescriptors.Descriptor var7 = var12[var6];
               if (var7 != null) {
                  var7.save(large_file_bb);
               }
            }

            this.doSendRequest(var1);
         } catch (Exception var11) {
            var11.printStackTrace();
         }
      }

      if (this.id == RequestDataPacket.RequestID.MetaGrid) {
         try {
            large_file_bb.clear();
            IsoWorld.instance.MetaGrid.savePart(large_file_bb, 0, true);
            IsoWorld.instance.MetaGrid.savePart(large_file_bb, 1, true);
            this.doSendRequest(var1);
         } catch (Exception var9) {
            DebugLog.Multiplayer.printException(var9, "map_meta.bin could not be saved", LogSeverity.Error);
            GameServer.kick(var1, "You have been kicked from this server because map_meta.bin could not be saved.", (String)null);
            var1.forceDisconnect("save-map-meta-bin");
            GameServer.addDisconnect(var1);
         }
      }

      if (this.id == RequestDataPacket.RequestID.MapZone) {
         try {
            large_file_bb.clear();
            IsoWorld.instance.MetaGrid.saveZone(large_file_bb);
            this.doSendRequest(var1);
         } catch (Exception var8) {
            DebugLog.Multiplayer.printException(var8, "map_zone.bin could not be saved", LogSeverity.Error);
            GameServer.kick(var1, "You have been kicked from this server because map_zone.bin could not be saved.", (String)null);
            var1.forceDisconnect("save-map-zone-bin");
            GameServer.addDisconnect(var1);
         }
      }

      if (this.id == RequestDataPacket.RequestID.RadioData) {
         large_file_bb.clear();
         ZomboidRadio.getInstance().getRecordedMedia().sendRequestData(large_file_bb);
         this.doSendRequest(var1);
      }

      DebugLog.Multiplayer.debugln("%s %db", this.id.name(), large_file_bb.position());
   }

   public void process(UdpConnection var1) {
      if (this.type == RequestDataPacket.RequestType.FullData) {
         large_file_bb.position(0);
         this.doProcessData(large_file_bb);
      } else if (this.type == RequestDataPacket.RequestType.PartData) {
         large_file_bb.position(0);
         this.doProcessPart(large_file_bb);
      }

   }

   private void doProcessPart(ByteBuffer var1) {
      ByteBuffer var2 = RequestDataManager.getInstance().receiveClientData(this.id, var1, this.dataSize, this.dataSent);
      if (var2 != null) {
         this.doProcessData(var2);
      }

   }

   private void doProcessData(ByteBuffer var1) {
      if (this.id == RequestDataPacket.RequestID.ConnectionDetails) {
         ConnectionDetails.parse(var1);
      }

      if (this.id == RequestDataPacket.RequestID.Descriptors) {
         try {
            DebugLog.Multiplayer.debugln("received zombie descriptors");
            PersistentOutfits.instance.load(var1);
         } catch (IOException var5) {
            DebugLog.Multiplayer.printException(var5, "PersistentOutfits loading IO error", LogSeverity.Error);
            ExceptionLogger.logException(var5);
         } catch (Exception var6) {
            DebugLog.Multiplayer.printException(var6, "PersistentOutfits loading error", LogSeverity.Error);
         }
      }

      if (this.id == RequestDataPacket.RequestID.PlayerZombieDescriptors) {
         try {
            this.receivePlayerZombieDescriptors(var1);
         } catch (Exception var4) {
            DebugLog.Multiplayer.printException(var4, "Player zombie descriptors loading error", LogSeverity.Error);
            ExceptionLogger.logException(var4);
         }
      }

      if (this.id == RequestDataPacket.RequestID.MetaGrid) {
         this.saveToFile(var1, "map_meta.bin");
      }

      if (this.id == RequestDataPacket.RequestID.MapZone) {
         this.saveToFile(var1, "map_zone.bin");
      }

      if (this.id == RequestDataPacket.RequestID.RadioData) {
         try {
            RecordedMedia.receiveRequestData(var1);
         } catch (Exception var3) {
            DebugLog.Multiplayer.printException(var3, "Radio data loading error", LogSeverity.Error);
            ExceptionLogger.logException(var3);
         }
      }

      this.sendNextRequest(this.id);
   }

   private void sendNextRequest(RequestID var1) {
      switch (var1) {
         case Descriptors:
            this.setRequest(RequestDataPacket.RequestID.MetaGrid);
            break;
         case MetaGrid:
            this.setRequest(RequestDataPacket.RequestID.MapZone);
            break;
         case MapZone:
            this.setRequest(RequestDataPacket.RequestID.PlayerZombieDescriptors);
            break;
         case PlayerZombieDescriptors:
            this.setRequest(RequestDataPacket.RequestID.RadioData);
            break;
         case RadioData:
            GameClient.instance.setRequest(GameClient.RequestState.Complete);
      }

      if (var1 != RequestDataPacket.RequestID.RadioData) {
         ByteBufferWriter var2 = GameClient.connection.startPacket();
         PacketTypes.PacketType.RequestData.doPacket(var2);
         this.write(var2);
         PacketTypes.PacketType.RequestData.send(GameClient.connection);
      }

   }

   private void saveToFile(ByteBuffer var1, String var2) {
      File var3 = new File(ZomboidFileSystem.instance.getFileNameInCurrentSave(var2));

      try {
         FileOutputStream var4 = new FileOutputStream(var3, false);

         try {
            BufferedOutputStream var5 = new BufferedOutputStream(var4);

            try {
               var5.write(var1.array(), 0, var1.limit());
               var5.flush();
            } catch (Throwable var10) {
               try {
                  var5.close();
               } catch (Throwable var9) {
                  var10.addSuppressed(var9);
               }

               throw var10;
            }

            var5.close();
         } catch (Throwable var11) {
            try {
               var4.close();
            } catch (Throwable var8) {
               var11.addSuppressed(var8);
            }

            throw var11;
         }

         var4.close();
      } catch (IOException var12) {
         DebugLog.Multiplayer.printException(var12, "Save to the " + var2 + " file error", LogSeverity.Error);
      }

   }

   private void receivePlayerZombieDescriptors(ByteBuffer var1) throws IOException {
      short var2 = var1.getShort();
      DebugLog.Multiplayer.debugln("received " + var2 + " player-zombie descriptors");

      for(short var3 = 0; var3 < var2; ++var3) {
         SharedDescriptors.Descriptor var4 = new SharedDescriptors.Descriptor();
         var4.load(var1, 195);
         SharedDescriptors.registerPlayerZombieDescriptor(var4);
      }

   }

   public boolean isConsistent() {
      return this.type != RequestDataPacket.RequestType.None;
   }

   public String getDescription() {
      String var1 = "\n\tRequestDataPacket [";
      var1 = var1 + "type=" + this.type.name() + " | ";
      if (this.type == RequestDataPacket.RequestType.Request || this.type == RequestDataPacket.RequestType.PartDataACK) {
         var1 = var1 + "id=" + this.id.name() + "] ";
      }

      int var2;
      if (this.type == RequestDataPacket.RequestType.FullData) {
         var1 = var1 + "id=" + this.id.name() + " | ";
         var1 = var1 + "data=(size:" + this.buffer.limit() + ", data=";
         this.buffer.position(0);

         for(var2 = 0; var2 < Math.min(15, this.buffer.limit()); ++var2) {
            var1 = var1 + " 0x" + Integer.toHexString(this.buffer.get() & 255);
         }

         var1 = var1 + ".. ] ";
      }

      if (this.type == RequestDataPacket.RequestType.PartData) {
         var1 = var1 + "id=" + this.id.name() + " | ";
         var1 = var1 + "dataSize=" + this.dataSize + " | ";
         var1 = var1 + "dataSent=" + this.dataSent + " | ";
         var1 = var1 + "partSize=" + this.partSize + " | ";
         var1 = var1 + "data=(size:" + this.buffer.limit() + ", data=";
         if (this.buffer.limit() >= this.dataSize) {
            this.buffer.position(this.dataSent);
         } else {
            this.buffer.position(0);
         }

         for(var2 = 0; var2 < Math.min(15, this.buffer.limit() - this.buffer.position()); ++var2) {
            var1 = var1 + " " + Integer.toHexString(this.buffer.get() & 255);
         }

         var1 = var1 + ".. ] ";
      }

      return var1;
   }

   static enum RequestType {
      None,
      Request,
      FullData,
      PartData,
      PartDataACK;

      private RequestType() {
      }
   }

   public static enum RequestID {
      ConnectionDetails,
      Descriptors,
      MetaGrid,
      MapZone,
      PlayerZombieDescriptors,
      RadioData;

      private RequestID() {
      }

      public String getDescriptor() {
         return Translator.getText("IGUI_RequestID_" + this.name());
      }
   }
}
