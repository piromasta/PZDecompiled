package zombie.core;

public class GeneralAction extends Action {
   public GeneralAction() {
   }

   float getDuration() {
      return 0.0F;
   }

   void start() {
   }

   void stop() {
   }

   boolean isValid() {
      return false;
   }

   boolean isUsingTimeout() {
      return true;
   }

   void update() {
   }

   boolean perform() {
      return false;
   }
}
