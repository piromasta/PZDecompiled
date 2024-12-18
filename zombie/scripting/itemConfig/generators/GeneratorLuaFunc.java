package zombie.scripting.itemConfig.generators;

import zombie.Lua.LuaManager;
import zombie.entity.GameEntity;
import zombie.scripting.itemConfig.RandomGenerator;
import zombie.util.StringUtils;

public class GeneratorLuaFunc extends RandomGenerator<GeneratorLuaFunc> {
   private final String luaFunc;

   public GeneratorLuaFunc(String var1, float var2) {
      if (var2 < 0.0F) {
         throw new IllegalArgumentException("Chance may not be <= 0.");
      } else if (StringUtils.isNullOrWhitespace(var1)) {
         throw new IllegalArgumentException("LuaFunc can not be null or empty.");
      } else {
         this.luaFunc = var1;
         this.setChance(var2);
      }
   }

   public boolean execute(GameEntity var1) {
      Object var2 = LuaManager.getFunctionObject(this.luaFunc);
      if (var2 != null) {
         LuaManager.caller.protectedCall(LuaManager.thread, var2, new Object[]{var1});
         return true;
      } else {
         return false;
      }
   }

   public GeneratorLuaFunc copy() {
      return new GeneratorLuaFunc(this.luaFunc, this.getChance());
   }
}
