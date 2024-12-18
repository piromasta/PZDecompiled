package zombie.scripting.itemConfig.enums;

public enum RootType {
   Attribute(true),
   FluidContainer(true),
   LuaFunc(false);

   private final boolean requiresId;

   private RootType(boolean var3) {
      this.requiresId = var3;
   }

   public boolean isRequiresId() {
      return this.requiresId;
   }
}
