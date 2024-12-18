package zombie.core.znet;

public class SteamRemotePlay {
   public SteamRemotePlay() {
   }

   private static native int n_GetSessionCount();

   public static int GetSessionCount() {
      return SteamUtils.isSteamModeEnabled() ? n_GetSessionCount() : 0;
   }
}
