package zombie.entity.util;

import java.util.Arrays;

public class BitSet {
   long[] bits = new long[]{0L};

   public BitSet() {
   }

   public BitSet(int var1) {
      this.checkCapacity(var1 >>> 6);
   }

   public BitSet(BitSet var1) {
      this.bits = new long[var1.bits.length];
      System.arraycopy(var1.bits, 0, this.bits, 0, var1.bits.length);
   }

   public boolean get(int var1) {
      int var2 = var1 >>> 6;
      if (var2 >= this.bits.length) {
         return false;
      } else {
         return (this.bits[var2] & 1L << (var1 & 63)) != 0L;
      }
   }

   public boolean getAndClear(int var1) {
      int var2 = var1 >>> 6;
      if (var2 >= this.bits.length) {
         return false;
      } else {
         long var3 = this.bits[var2];
         long[] var10000 = this.bits;
         var10000[var2] &= ~(1L << (var1 & 63));
         return this.bits[var2] != var3;
      }
   }

   public boolean getAndSet(int var1) {
      int var2 = var1 >>> 6;
      this.checkCapacity(var2);
      long var3 = this.bits[var2];
      long[] var10000 = this.bits;
      var10000[var2] |= 1L << (var1 & 63);
      return this.bits[var2] == var3;
   }

   public void set(int var1) {
      int var2 = var1 >>> 6;
      this.checkCapacity(var2);
      long[] var10000 = this.bits;
      var10000[var2] |= 1L << (var1 & 63);
   }

   public void flip(int var1) {
      int var2 = var1 >>> 6;
      this.checkCapacity(var2);
      long[] var10000 = this.bits;
      var10000[var2] ^= 1L << (var1 & 63);
   }

   private void checkCapacity(int var1) {
      if (var1 >= this.bits.length) {
         long[] var2 = new long[var1 + 1];
         System.arraycopy(this.bits, 0, var2, 0, this.bits.length);
         this.bits = var2;
      }

   }

   public void clear(int var1) {
      int var2 = var1 >>> 6;
      if (var2 < this.bits.length) {
         long[] var10000 = this.bits;
         var10000[var2] &= ~(1L << (var1 & 63));
      }
   }

   public void clear() {
      Arrays.fill(this.bits, 0L);
   }

   public int numBits() {
      return this.bits.length << 6;
   }

   public int length() {
      long[] var1 = this.bits;

      for(int var2 = var1.length - 1; var2 >= 0; --var2) {
         long var3 = var1[var2];
         if (var3 != 0L) {
            for(int var5 = 63; var5 >= 0; --var5) {
               if ((var3 & 1L << (var5 & 63)) != 0L) {
                  return (var2 << 6) + var5 + 1;
               }
            }
         }
      }

      return 0;
   }

   public boolean notEmpty() {
      return !this.isEmpty();
   }

   public boolean isEmpty() {
      long[] var1 = this.bits;
      int var2 = var1.length;

      for(int var3 = 0; var3 < var2; ++var3) {
         if (var1[var3] != 0L) {
            return false;
         }
      }

      return true;
   }

   public int nextSetBit(int var1) {
      long[] var2 = this.bits;
      int var3 = var1 >>> 6;
      int var4 = var2.length;
      if (var3 >= var4) {
         return -1;
      } else {
         long var5 = var2[var3];
         int var7;
         if (var5 != 0L) {
            for(var7 = var1 & 63; var7 < 64; ++var7) {
               if ((var5 & 1L << (var7 & 63)) != 0L) {
                  return (var3 << 6) + var7;
               }
            }
         }

         ++var3;

         for(; var3 < var4; ++var3) {
            if (var3 != 0) {
               var5 = var2[var3];
               if (var5 != 0L) {
                  for(var7 = 0; var7 < 64; ++var7) {
                     if ((var5 & 1L << (var7 & 63)) != 0L) {
                        return (var3 << 6) + var7;
                     }
                  }
               }
            }
         }

         return -1;
      }
   }

   public int nextClearBit(int var1) {
      long[] var2 = this.bits;
      int var3 = var1 >>> 6;
      int var4 = var2.length;
      if (var3 >= var4) {
         return var2.length << 6;
      } else {
         long var5 = var2[var3];

         int var7;
         for(var7 = var1 & 63; var7 < 64; ++var7) {
            if ((var5 & 1L << (var7 & 63)) == 0L) {
               return (var3 << 6) + var7;
            }
         }

         ++var3;

         while(var3 < var4) {
            if (var3 == 0) {
               return var3 << 6;
            }

            var5 = var2[var3];

            for(var7 = 0; var7 < 64; ++var7) {
               if ((var5 & 1L << (var7 & 63)) == 0L) {
                  return (var3 << 6) + var7;
               }
            }

            ++var3;
         }

         return var2.length << 6;
      }
   }

   public void and(BitSet var1) {
      int var2 = Math.min(this.bits.length, var1.bits.length);

      int var3;
      for(var3 = 0; var2 > var3; ++var3) {
         long[] var10000 = this.bits;
         var10000[var3] &= var1.bits[var3];
      }

      if (this.bits.length > var2) {
         var3 = var2;

         for(int var4 = this.bits.length; var4 > var3; ++var3) {
            this.bits[var3] = 0L;
         }
      }

   }

   public void andNot(BitSet var1) {
      int var2 = 0;
      int var3 = this.bits.length;

      for(int var4 = var1.bits.length; var2 < var3 && var2 < var4; ++var2) {
         long[] var10000 = this.bits;
         var10000[var2] &= ~var1.bits[var2];
      }

   }

   public void or(BitSet var1) {
      int var2 = Math.min(this.bits.length, var1.bits.length);

      int var3;
      for(var3 = 0; var2 > var3; ++var3) {
         long[] var10000 = this.bits;
         var10000[var3] |= var1.bits[var3];
      }

      if (var2 < var1.bits.length) {
         this.checkCapacity(var1.bits.length);
         var3 = var2;

         for(int var4 = var1.bits.length; var4 > var3; ++var3) {
            this.bits[var3] = var1.bits[var3];
         }
      }

   }

   public void xor(BitSet var1) {
      int var2 = Math.min(this.bits.length, var1.bits.length);

      int var3;
      for(var3 = 0; var2 > var3; ++var3) {
         long[] var10000 = this.bits;
         var10000[var3] ^= var1.bits[var3];
      }

      if (var2 < var1.bits.length) {
         this.checkCapacity(var1.bits.length);
         var3 = var2;

         for(int var4 = var1.bits.length; var4 > var3; ++var3) {
            this.bits[var3] = var1.bits[var3];
         }
      }

   }

   public boolean intersects(BitSet var1) {
      long[] var2 = this.bits;
      long[] var3 = var1.bits;

      for(int var4 = Math.min(var2.length, var3.length) - 1; var4 >= 0; --var4) {
         if ((var2[var4] & var3[var4]) != 0L) {
            return true;
         }
      }

      return false;
   }

   public boolean containsAll(BitSet var1) {
      long[] var2 = this.bits;
      long[] var3 = var1.bits;
      int var4 = var3.length;
      int var5 = var2.length;

      int var6;
      for(var6 = var5; var6 < var4; ++var6) {
         if (var3[var6] != 0L) {
            return false;
         }
      }

      for(var6 = Math.min(var5, var4) - 1; var6 >= 0; --var6) {
         if ((var2[var6] & var3[var6]) != var3[var6]) {
            return false;
         }
      }

      return true;
   }

   public int hashCode() {
      int var1 = this.length() >>> 6;
      int var2 = 0;

      for(int var3 = 0; var1 >= var3; ++var3) {
         var2 = 127 * var2 + (int)(this.bits[var3] ^ this.bits[var3] >>> 32);
      }

      return var2;
   }

   public boolean equals(Object var1) {
      if (this == var1) {
         return true;
      } else if (var1 == null) {
         return false;
      } else if (this.getClass() != var1.getClass()) {
         return false;
      } else {
         BitSet var2 = (BitSet)var1;
         long[] var3 = var2.bits;
         int var4 = Math.min(this.bits.length, var3.length);

         for(int var5 = 0; var4 > var5; ++var5) {
            if (this.bits[var5] != var3[var5]) {
               return false;
            }
         }

         if (this.bits.length == var3.length) {
            return true;
         } else {
            return this.length() == var2.length();
         }
      }
   }
}
