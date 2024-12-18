package zombie.worldRenderCache;

public class WorldRenderCache {
   public static WorldRenderCache instance = new WorldRenderCache();

   public WorldRenderCache() {
   }

   public boolean isAcceptingCache() {
      return true;
   }
}
