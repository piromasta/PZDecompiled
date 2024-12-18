package zombie.util.hash;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import java.nio.charset.StandardCharsets;

public class PZHash {
   private static final int FNV1_32_INIT = -2128831035;
   private static final int FNV1_PRIME_32 = 16777619;
   private static final long FNV1_64_INIT = -3750763034362895579L;
   private static final long FNV1_PRIME_64 = 1099511628211L;

   public PZHash() {
   }

   public static long sha256_64(String var0) {
      HashCode var1 = Hashing.sha256().hashString(var0, StandardCharsets.UTF_8);
      return var1.asLong();
   }

   public static int fnv_32(String var0) {
      byte[] var1 = var0.getBytes();
      return fnv_32(var1, var1.length);
   }

   public static int fnv_32(byte[] var0) {
      return fnv_32(var0, var0.length);
   }

   public static int fnv_32(byte[] var0, int var1) {
      int var2 = -2128831035;

      for(int var3 = 0; var3 < var1; ++var3) {
         var2 ^= var0[var3] & 255;
         var2 *= 16777619;
      }

      return var2;
   }

   public static long fnv_64(String var0) {
      byte[] var1 = var0.getBytes();
      return fnv_64(var1, var1.length);
   }

   public static long fnv_64(byte[] var0) {
      return fnv_64(var0, var0.length);
   }

   public static long fnv_64(byte[] var0, int var1) {
      long var2 = -3750763034362895579L;

      for(int var4 = 0; var4 < var1; ++var4) {
         var2 ^= (long)(var0[var4] & 255);
         var2 *= 1099511628211L;
      }

      return var2;
   }

   public static int murmur_32(String var0) {
      byte[] var1 = var0.getBytes();
      return murmur_32(var1, var1.length);
   }

   public static int murmur_32(byte[] var0, int var1) {
      return murmur_32(var0, var1, 0);
   }

   public static int murmur_32(byte[] var0, int var1, int var2) {
      int var3 = 1540483477;
      byte var4 = 24;
      int var5 = var2 ^ var1;
      int var7 = var1 >> 2;

      int var8;
      int var9;
      for(var8 = 0; var8 < var7; ++var8) {
         var9 = var8 << 2;
         int var10 = var0[var9 + 3];
         var10 <<= 8;
         var10 |= var0[var9 + 2] & 255;
         var10 <<= 8;
         var10 |= var0[var9 + 1] & 255;
         var10 <<= 8;
         var10 |= var0[var9 + 0] & 255;
         var10 *= var3;
         var10 ^= var10 >>> var4;
         var10 *= var3;
         var5 *= var3;
         var5 ^= var10;
      }

      var8 = var7 << 2;
      var9 = var1 - var8;
      if (var9 != 0) {
         if (var9 >= 3) {
            var5 ^= var0[var1 - 3] << 16;
         }

         if (var9 >= 2) {
            var5 ^= var0[var1 - 2] << 8;
         }

         if (var9 >= 1) {
            var5 ^= var0[var1 - 1];
         }

         var5 *= var3;
      }

      var5 ^= var5 >>> 13;
      var5 *= var3;
      var5 ^= var5 >>> 15;
      return var5;
   }

   public static long murmur_64(String var0) {
      byte[] var1 = var0.getBytes();
      return murmur_64(var1, var1.length);
   }

   public static long murmur_64(byte[] var0, int var1) {
      return murmur_64(var0, var1, -512093083);
   }

   public static long murmur_64(byte[] var0, int var1, int var2) {
      long var3 = (long)var2 & 4294967295L ^ (long)var1 * -4132994306676758123L;
      int var5 = var1 / 8;

      for(int var6 = 0; var6 < var5; ++var6) {
         int var7 = var6 * 8;
         long var8 = ((long)var0[var7 + 0] & 255L) + (((long)var0[var7 + 1] & 255L) << 8) + (((long)var0[var7 + 2] & 255L) << 16) + (((long)var0[var7 + 3] & 255L) << 24) + (((long)var0[var7 + 4] & 255L) << 32) + (((long)var0[var7 + 5] & 255L) << 40) + (((long)var0[var7 + 6] & 255L) << 48) + (((long)var0[var7 + 7] & 255L) << 56);
         var8 *= -4132994306676758123L;
         var8 ^= var8 >>> 47;
         var8 *= -4132994306676758123L;
         var3 ^= var8;
         var3 *= -4132994306676758123L;
      }

      switch (var1 % 8) {
         case 7:
            var3 ^= (long)(var0[(var1 & -8) + 6] & 255) << 48;
         case 6:
            var3 ^= (long)(var0[(var1 & -8) + 5] & 255) << 40;
         case 5:
            var3 ^= (long)(var0[(var1 & -8) + 4] & 255) << 32;
         case 4:
            var3 ^= (long)(var0[(var1 & -8) + 3] & 255) << 24;
         case 3:
            var3 ^= (long)(var0[(var1 & -8) + 2] & 255) << 16;
         case 2:
            var3 ^= (long)(var0[(var1 & -8) + 1] & 255) << 8;
         case 1:
            var3 ^= (long)(var0[var1 & -8] & 255);
            var3 *= -4132994306676758123L;
         default:
            var3 ^= var3 >>> 47;
            var3 *= -4132994306676758123L;
            var3 ^= var3 >>> 47;
            return var3;
      }
   }
}
