package zombie.debug.options;

import zombie.debug.BooleanDebugOption;

public final class Network extends OptionGroup {
   public final Client Client;
   public final Server Server;
   public final PublicServerUtil PublicServerUtil;

   public Network() {
      this.Client = new Client(this.Group);
      this.Server = new Server(this.Group);
      this.PublicServerUtil = new PublicServerUtil(this.Group);
   }

   public final class Client extends OptionGroup {
      public final BooleanDebugOption MainLoop = this.newDebugOnlyOption("MainLoop", true);
      public final BooleanDebugOption UpdateZombiesFromPacket = this.newDebugOnlyOption("UpdateZombiesFromPacket", true);
      public final BooleanDebugOption SyncIsoObject = this.newDebugOnlyOption("SyncIsoObject", true);

      public Client(IDebugOptionGroup var2) {
         super(var2, "Client");
      }
   }

   public final class Server extends OptionGroup {
      public final BooleanDebugOption SyncIsoObject = this.newDebugOnlyOption("SyncIsoObject", true);

      public Server(IDebugOptionGroup var2) {
         super(var2, "Server");
      }
   }

   public final class PublicServerUtil extends OptionGroup {
      public final BooleanDebugOption Enabled = this.newDebugOnlyOption("Enabled", true);

      public PublicServerUtil(IDebugOptionGroup var2) {
         super(var2, "PublicServerUtil");
      }
   }
}
