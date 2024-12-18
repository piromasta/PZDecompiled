package zombie;

public class SystemDisabler {
   public static boolean doCharacterStats = true;
   public static boolean doZombieCreation = true;
   public static boolean doSurvivorCreation = false;
   public static boolean doPlayerCreation = true;
   public static boolean doOverridePOVCharacters = true;
   public static boolean doVehiclesEverywhere = false;
   public static boolean doWorldSyncEnable = false;
   private static boolean doOverrideServerConnectDebugCheck = false;
   private static boolean doHighFriction = false;
   private static boolean doVehicleLowRider = false;
   public static boolean doEnableDetectOpenGLErrorsInTexture = false;
   public static boolean doVehiclesWithoutTextures = false;
   public static boolean zombiesDontAttack = false;
   public static boolean disableSyncInventory = true;
   public static boolean zombiesSwitchOwnershipEachUpdate = false;
   private static boolean doPrintDetailedInfo = false;
   private static boolean doMainLoopDealWithNetData = true;
   public static boolean useNetworkCharacter = false;
   private static boolean bEnableAdvancedSoundOptions = false;
   private static boolean doKickInDebug = false;
   private static boolean uncappedFPS = false;

   public SystemDisabler() {
   }

   public static void setDoCharacterStats(boolean var0) {
      doCharacterStats = var0;
   }

   public static void setDoZombieCreation(boolean var0) {
      doZombieCreation = var0;
   }

   public static void setDoSurvivorCreation(boolean var0) {
      doSurvivorCreation = var0;
   }

   public static void setDoPlayerCreation(boolean var0) {
      doPlayerCreation = var0;
   }

   public static void setOverridePOVCharacters(boolean var0) {
      doOverridePOVCharacters = var0;
   }

   public static void setVehiclesEverywhere(boolean var0) {
      doVehiclesEverywhere = var0;
   }

   public static void setWorldSyncEnable(boolean var0) {
      doWorldSyncEnable = var0;
   }

   public static boolean getOverrideServerConnectDebugCheck() {
      if (!doOverrideServerConnectDebugCheck) {
      }

      return true;
   }

   public static boolean getdoHighFriction() {
      return doHighFriction;
   }

   public static boolean getdoVehicleLowRider() {
      return doVehicleLowRider;
   }

   public static boolean printDetailedInfo() {
      return doPrintDetailedInfo;
   }

   public static boolean getDoMainLoopDealWithNetData() {
      return doMainLoopDealWithNetData;
   }

   public static void setEnableAdvancedSoundOptions(boolean var0) {
      bEnableAdvancedSoundOptions = var0;
   }

   public static boolean getEnableAdvancedSoundOptions() {
      return bEnableAdvancedSoundOptions;
   }

   public static boolean getKickInDebug() {
      return doKickInDebug;
   }

   public static void setUncappedFPS(boolean var0) {
      uncappedFPS = var0;
   }

   public static boolean getUncappedFPS() {
      return uncappedFPS;
   }

   public static void Reset() {
      doCharacterStats = true;
      doZombieCreation = true;
      doSurvivorCreation = false;
      doPlayerCreation = true;
      doOverridePOVCharacters = true;
      doVehiclesEverywhere = false;
      doWorldSyncEnable = false;
      doMainLoopDealWithNetData = true;
      bEnableAdvancedSoundOptions = false;
   }
}
