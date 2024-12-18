package zombie.inventory.types;

public interface IAlarmClock {
   void stopRinging();

   void setAlarmSet(boolean var1);

   boolean isAlarmSet();

   void setHour(int var1);

   void setMinute(int var1);

   void setForceDontRing(int var1);

   int getHour();

   int getMinute();
}
