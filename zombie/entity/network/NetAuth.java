package zombie.entity.network;

public enum NetAuth {
   Denied,
   ClientOnly,
   ServerOnly,
   ClientAndServer;

   private NetAuth() {
   }
}
