package zombie.debug;

public enum DebugType {
   Packet,
   NetworkFileDebug,
   Network,
   General,
   Lua,
   Mod,
   Sound,
   Zombie,
   Combat,
   Objects,
   Fireplace,
   Radio,
   MapLoading,
   Clothing,
   Animation,
   Asset,
   Script,
   Shader,
   Input,
   Recipe,
   ActionSystem,
   IsoRegion,
   UnitTests,
   FileIO,
   Multiplayer,
   Ownership,
   Death,
   Damage,
   Statistic,
   Vehicle,
   Voice,
   Checksum;

   private DebugType() {
   }

   public static boolean Do(DebugType var0) {
      return DebugLog.isEnabled(var0);
   }
}
