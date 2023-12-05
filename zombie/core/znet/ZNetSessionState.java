package zombie.core.znet;

public class ZNetSessionState {
   boolean bConnectionActive;
   boolean bConnecting;
   boolean bUsingRelay;
   long eP2PSessionError;
   long bytesQueuedForSend;
   long packetsQueuedForSend;
   long remoteIP;
   long remotePort;

   public ZNetSessionState() {
   }

   public String getDescription() {
      return "session active=\"" + this.bConnectionActive + "\" connecting=\"" + this.bConnecting + "\" relay=\"" + this.bUsingRelay + "\" error=\"" + this.eP2PSessionError + "\" bytes=\"" + this.bytesQueuedForSend + "\" packets=\"" + this.packetsQueuedForSend + "\" ip=\"" + (255L & this.remoteIP >> 24) + "." + (255L & this.remoteIP >> 16) + "." + (255L & this.remoteIP >> 8) + "." + (255L & this.remoteIP) + "\" port=\"" + this.remotePort + "\"";
   }
}
