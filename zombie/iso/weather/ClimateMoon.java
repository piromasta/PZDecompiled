package zombie.iso.weather;

public final class ClimateMoon {
   private static final int[] day_year = new int[]{-1, -1, 30, 58, 89, 119, 150, 180, 211, 241, 272, 303, 333};
   private static final String[] moon_phase_name = new String[]{"New", "Waxing crescent", "First quarter", "Waxing gibbous", "Full", "Waning gibbous", "Third quarter", "Waning crescent"};
   private static final float[] units = new float[]{0.0F, 0.25F, 0.5F, 0.75F, 1.0F, 0.75F, 0.5F, 0.25F};
   private int last_year;
   private int last_month;
   private int last_day;
   private int current_phase = 0;
   private float current_float = 0.0F;
   private static final ClimateMoon instance = new ClimateMoon();

   public ClimateMoon() {
   }

   public static ClimateMoon getInstance() {
      return instance;
   }

   public void updatePhase(int var1, int var2, int var3) {
      if (var1 != this.last_year || var2 != this.last_month || var3 != this.last_day) {
         this.last_year = var1;
         this.last_month = var2;
         this.last_day = var3;
         this.current_phase = this.getMoonPhase(var1, var2, var3);
         if (this.current_phase > 7) {
            this.current_phase = 7;
         }

         if (this.current_phase < 0) {
            this.current_phase = 0;
         }

         this.current_float = units[this.current_phase];
      }

   }

   public String getPhaseName() {
      return moon_phase_name[this.current_phase];
   }

   public float getMoonFloat() {
      return this.current_float;
   }

   public int getCurrentMoonPhase() {
      return this.current_phase;
   }

   private int getMoonPhase(int var1, int var2, int var3) {
      if (var2 < 0 || var2 > 12) {
         var2 = 0;
      }

      int var7 = var3 + day_year[var2];
      if (var2 > 2 && this.isLeapYearP(var1)) {
         ++var7;
      }

      int var5 = var1 / 100 + 1;
      int var8 = var1 % 19 + 1;
      int var6 = (11 * var8 + 20 + (8 * var5 + 5) / 25 - 5 - (3 * var5 / 4 - 12)) % 30;
      if (var6 <= 0) {
         var6 += 30;
      }

      if (var6 == 25 && var8 > 11 || var6 == 24) {
         ++var6;
      }

      int var4 = ((var7 + var6) * 6 + 11) % 177 / 22 & 7;
      return var4;
   }

   private int daysInMonth(int var1, int var2) {
      int var3 = 31;
      switch (var1) {
         case 2:
            var3 = this.isLeapYearP(var2) ? 29 : 28;
         case 3:
         case 5:
         case 7:
         case 8:
         case 10:
         default:
            break;
         case 4:
         case 6:
         case 9:
         case 11:
            var3 = 30;
      }

      return var3;
   }

   private boolean isLeapYearP(int var1) {
      return var1 % 4 == 0 && (var1 % 400 == 0 || var1 % 100 != 0);
   }

   public void Reset() {
      this.current_float = 0.0F;
      this.current_phase = 0;
      this.last_year = this.last_month = this.last_day = 0;
   }
}
