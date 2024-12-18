package zombie.network.characters;

public class AttackRateChecker {
   public long timestamp;
   public short target;
   public short count;

   public AttackRateChecker() {
   }

   public void set(long var1, short var3, short var4) {
      this.timestamp = var1;
      this.target = var3;
      this.count = var4;
   }

   public void reset() {
      this.set(0L, (short)-1, (short)0);
   }

   public float check(short var1, int var2) {
      float var3 = 1.0F;
      long var4 = this.timestamp;
      short var6 = this.count;
      if (this.target == var1) {
         ++var6;
         if (var6 >= var2) {
            long var7 = System.currentTimeMillis();
            var3 = (float)(var7 - var4) / 1000.0F;
            var4 = var7;
         }
      } else {
         var6 = 1;
      }

      this.set(var4, var1, var6);
      return var3;
   }
}
