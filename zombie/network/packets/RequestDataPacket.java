package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.core.Translator;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.ServerWorldDatabase;

@PacketSetting(
   ordering = 0,
   priority = 1,
   reliability = 2,
   requiredCapability = Capability.LoginOnServer,
   handlingType = 7
)
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
   }

   public void setRequest(RequestID var1) {
   }

   public void setPartData(RequestID var1, ByteBuffer var2) {
   }

   public void setPartDataParameters(int var1, int var2) {
   }

   public void setACK(RequestID var1) {
   }

   public void sendConnectingDetails(UdpConnection var1, ServerWorldDatabase.LogonResult var2) {
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
   }

   public void write(ByteBufferWriter var1) {
   }

   public void processServer(PacketTypes.PacketType var1, UdpConnection var2) {
   }

   private void doSendRequest(UdpConnection var1) {
   }

   private void doProcessRequest(UdpConnection var1) {
   }

   public void processClientLoading(UdpConnection var1) {
   }

   public void processClient(UdpConnection var1) {
   }

   public boolean isConsistent(UdpConnection var1) {
      return this.type != RequestDataPacket.RequestType.None;
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
      RadioData,
      WorldMap;

      private RequestID() {
      }

      public String getDescriptor() {
         return Translator.getText("IGUI_RequestID_" + this.name());
      }
   }
}
