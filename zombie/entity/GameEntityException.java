package zombie.entity;

public class GameEntityException extends Exception {
   public GameEntityException(String var1) {
      super(var1);
   }

   public GameEntityException(String var1, Throwable var2) {
      super(var1, var2);
   }

   public GameEntityException(String var1, GameEntity var2) {
      String var10001 = var2 == null ? "[null]" : var2.getExceptionCompatibleString();
      super(var10001 + " " + var1);
   }

   public GameEntityException(String var1, Throwable var2, GameEntity var3) {
      super((var3 == null ? "[null]" : var3.getExceptionCompatibleString()) + " " + var1, var2);
   }
}
