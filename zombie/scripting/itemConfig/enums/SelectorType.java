package zombie.scripting.itemConfig.enums;

public enum SelectorType {
   Default(false),
   Zone(true),
   Room(true),
   Container(true),
   Tile(true),
   WorldAge(false),
   Situated(false),
   Vehicle(true),
   OnCreate(false),
   None(false);

   private final boolean allowChaining;

   private SelectorType(boolean var3) {
      this.allowChaining = var3;
   }

   public boolean isAllowChaining() {
      return this.allowChaining;
   }
}
