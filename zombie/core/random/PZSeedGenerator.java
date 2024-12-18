package zombie.core.random;

import org.uncommons.maths.random.SecureRandomSeedGenerator;
import org.uncommons.maths.random.SeedException;
import org.uncommons.maths.random.SeedGenerator;

public class PZSeedGenerator implements SeedGenerator {
   private static final SeedGenerator[] GENERATORS = new SeedGenerator[]{new SecureRandomSeedGenerator()};

   public PZSeedGenerator() {
   }

   public byte[] generateSeed(int var1) {
      SeedGenerator[] var2 = GENERATORS;
      int var3 = var2.length;
      int var4 = 0;

      while(var4 < var3) {
         SeedGenerator var5 = var2[var4];

         try {
            byte[] var6 = var5.generateSeed(var1);
            return var6;
         } catch (SeedException var7) {
            ++var4;
         }
      }

      throw new IllegalStateException("All available seed generation strategies failed.");
   }
}
