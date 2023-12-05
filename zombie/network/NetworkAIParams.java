package zombie.network;

public class NetworkAIParams {
   public static final int ZOMBIE_UPDATE_INFO_BUNCH_RATE_MS = 200;
   public static final int CHARACTER_UPDATE_RATE_MS = 200;
   public static final int CHARACTER_EXTRAPOLATION_UPDATE_INTERVAL_MS = 500;
   public static final float ZOMBIE_ANTICIPATORY_UPDATE_MULTIPLIER = 0.6F;
   public static final int ZOMBIE_OWNERSHIP_INTERVAL = 2000;
   public static final int ZOMBIE_REMOVE_INTERVAL_MS = 4000;
   public static final int ZOMBIE_MAX_UPDATE_INTERVAL_MS = 3800;
   public static final int ZOMBIE_MIN_UPDATE_INTERVAL_MS = 200;
   public static final int CHARACTER_PREDICTION_INTERVAL_MS = 2000;
   public static final int ZOMBIE_TELEPORT_PLAYER = 2;
   public static final int ZOMBIE_TELEPORT_DISTANCE_SQ = 9;
   public static final int VEHICLE_SPEED_CAP = 10;
   public static final int VEHICLE_MOVING_MP_PHYSIC_UPDATE_RATE = 150;
   public static final int VEHICLE_MP_PHYSIC_UPDATE_RATE = 300;
   public static final int VEHICLE_BUFFER_DELAY_MS = 500;
   public static final int VEHICLE_BUFFER_HISTORY_MS = 800;
   public static final long TIME_VALIDATION_DELAY = 1000L;
   public static final long TIME_VALIDATION_INTERVAL = 4000L;
   public static final long TIME_VALIDATION_TIMEOUT = 10000L;
   public static final float MAX_TOWING_TRAILER_DISTANCE_SQ = 1.0F;
   public static final float MAX_TOWING_CAR_DISTANCE_SQ = 4.0F;
   public static final float MAX_RECONNECT_DISTANCE_SQ = 10.0F;
   public static final float TOWING_DISTANCE = 1.5F;
   private static boolean showConnectionInfo = false;
   private static boolean showServerInfo = false;
   private static boolean showPingInfo = false;

   public NetworkAIParams() {
   }

   public static boolean isShowConnectionInfo() {
      return showConnectionInfo;
   }

   public static void setShowConnectionInfo(boolean var0) {
      showConnectionInfo = var0;
   }

   public static boolean isShowServerInfo() {
      return showServerInfo;
   }

   public static void setShowServerInfo(boolean var0) {
      showServerInfo = var0;
   }

   public static boolean isShowPingInfo() {
      return showPingInfo;
   }

   public static void setShowPingInfo(boolean var0) {
      showPingInfo = var0;
   }

   public static void Init() {
      if (GameClient.bClient) {
         showConnectionInfo = false;
         showServerInfo = true;
         showPingInfo = true;
      }

      MPStatistics.Init();
   }
}
