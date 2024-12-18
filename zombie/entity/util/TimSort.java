package zombie.entity.util;

import java.util.Arrays;
import java.util.Comparator;

class TimSort<T> {
   private static final int MIN_MERGE = 32;
   private T[] a;
   private Comparator<? super T> c;
   private static final int MIN_GALLOP = 7;
   private int minGallop = 7;
   private static final int INITIAL_TMP_STORAGE_LENGTH = 256;
   private T[] tmp;
   private int tmpCount;
   private int stackSize = 0;
   private final int[] runBase;
   private final int[] runLen;
   private static final boolean DEBUG = false;

   TimSort() {
      this.tmp = new Object[256];
      this.runBase = new int[40];
      this.runLen = new int[40];
   }

   public void doSort(T[] var1, Comparator<T> var2, int var3, int var4) {
      this.stackSize = 0;
      rangeCheck(var1.length, var3, var4);
      int var5 = var4 - var3;
      if (var5 >= 2) {
         int var6;
         if (var5 < 32) {
            var6 = countRunAndMakeAscending(var1, var3, var4, var2);
            binarySort(var1, var3, var4, var3 + var6, var2);
         } else {
            this.a = var1;
            this.c = var2;
            this.tmpCount = 0;
            var6 = minRunLength(var5);

            int var8;
            do {
               int var7 = countRunAndMakeAscending(var1, var3, var4, var2);
               if (var7 < var6) {
                  var8 = var5 <= var6 ? var5 : var6;
                  binarySort(var1, var3, var3 + var8, var3 + var7, var2);
                  var7 = var8;
               }

               this.pushRun(var3, var7);
               this.mergeCollapse();
               var3 += var7;
               var5 -= var7;
            } while(var5 != 0);

            this.mergeForceCollapse();
            this.a = null;
            this.c = null;
            Object[] var10 = this.tmp;
            var8 = 0;

            for(int var9 = this.tmpCount; var8 < var9; ++var8) {
               var10[var8] = null;
            }

         }
      }
   }

   private TimSort(T[] var1, Comparator<? super T> var2) {
      this.a = var1;
      this.c = var2;
      int var3 = var1.length;
      Object[] var4 = new Object[var3 < 512 ? var3 >>> 1 : 256];
      this.tmp = var4;
      int var5 = var3 < 120 ? 5 : (var3 < 1542 ? 10 : (var3 < 119151 ? 19 : 40));
      this.runBase = new int[var5];
      this.runLen = new int[var5];
   }

   static <T> void sort(T[] var0, Comparator<? super T> var1) {
      sort(var0, 0, var0.length, var1);
   }

   static <T> void sort(T[] var0, int var1, int var2, Comparator<? super T> var3) {
      if (var3 == null) {
         Arrays.sort(var0, var1, var2);
      } else {
         rangeCheck(var0.length, var1, var2);
         int var4 = var2 - var1;
         if (var4 >= 2) {
            if (var4 < 32) {
               int var9 = countRunAndMakeAscending(var0, var1, var2, var3);
               binarySort(var0, var1, var2, var1 + var9, var3);
            } else {
               TimSort var5 = new TimSort(var0, var3);
               int var6 = minRunLength(var4);

               do {
                  int var7 = countRunAndMakeAscending(var0, var1, var2, var3);
                  if (var7 < var6) {
                     int var8 = var4 <= var6 ? var4 : var6;
                     binarySort(var0, var1, var1 + var8, var1 + var7, var3);
                     var7 = var8;
                  }

                  var5.pushRun(var1, var7);
                  var5.mergeCollapse();
                  var1 += var7;
                  var4 -= var7;
               } while(var4 != 0);

               var5.mergeForceCollapse();
            }
         }
      }
   }

   private static <T> void binarySort(T[] var0, int var1, int var2, int var3, Comparator<? super T> var4) {
      if (var3 == var1) {
         ++var3;
      }

      while(var3 < var2) {
         Object var5 = var0[var3];
         int var6 = var1;
         int var7 = var3;

         int var8;
         while(var6 < var7) {
            var8 = var6 + var7 >>> 1;
            if (var4.compare(var5, var0[var8]) < 0) {
               var7 = var8;
            } else {
               var6 = var8 + 1;
            }
         }

         var8 = var3 - var6;
         switch (var8) {
            case 2:
               var0[var6 + 2] = var0[var6 + 1];
            case 1:
               var0[var6 + 1] = var0[var6];
               break;
            default:
               System.arraycopy(var0, var6, var0, var6 + 1, var8);
         }

         var0[var6] = var5;
         ++var3;
      }

   }

   private static <T> int countRunAndMakeAscending(T[] var0, int var1, int var2, Comparator<? super T> var3) {
      int var4 = var1 + 1;
      if (var4 == var2) {
         return 1;
      } else {
         if (var3.compare(var0[var4++], var0[var1]) >= 0) {
            while(var4 < var2 && var3.compare(var0[var4], var0[var4 - 1]) >= 0) {
               ++var4;
            }
         } else {
            while(var4 < var2 && var3.compare(var0[var4], var0[var4 - 1]) < 0) {
               ++var4;
            }

            reverseRange(var0, var1, var4);
         }

         return var4 - var1;
      }
   }

   private static void reverseRange(Object[] var0, int var1, int var2) {
      --var2;

      while(var1 < var2) {
         Object var3 = var0[var1];
         var0[var1++] = var0[var2];
         var0[var2--] = var3;
      }

   }

   private static int minRunLength(int var0) {
      int var1;
      for(var1 = 0; var0 >= 32; var0 >>= 1) {
         var1 |= var0 & 1;
      }

      return var0 + var1;
   }

   private void pushRun(int var1, int var2) {
      this.runBase[this.stackSize] = var1;
      this.runLen[this.stackSize] = var2;
      ++this.stackSize;
   }

   private void mergeCollapse() {
      while(true) {
         int var1;
         label38: {
            if (this.stackSize > 1) {
               var1 = this.stackSize - 2;
               if (var1 >= 1 && this.runLen[var1 - 1] <= this.runLen[var1] + this.runLen[var1 + 1] || var1 >= 2 && this.runLen[var1 - 2] <= this.runLen[var1] + this.runLen[var1 - 1]) {
                  if (this.runLen[var1 - 1] < this.runLen[var1 + 1]) {
                     --var1;
                  }
                  break label38;
               }

               if (this.runLen[var1] <= this.runLen[var1 + 1]) {
                  break label38;
               }
            }

            return;
         }

         this.mergeAt(var1);
      }
   }

   private void mergeForceCollapse() {
      int var1;
      for(; this.stackSize > 1; this.mergeAt(var1)) {
         var1 = this.stackSize - 2;
         if (var1 > 0 && this.runLen[var1 - 1] < this.runLen[var1 + 1]) {
            --var1;
         }
      }

   }

   private void mergeAt(int var1) {
      int var2 = this.runBase[var1];
      int var3 = this.runLen[var1];
      int var4 = this.runBase[var1 + 1];
      int var5 = this.runLen[var1 + 1];
      this.runLen[var1] = var3 + var5;
      if (var1 == this.stackSize - 3) {
         this.runBase[var1 + 1] = this.runBase[var1 + 2];
         this.runLen[var1 + 1] = this.runLen[var1 + 2];
      }

      --this.stackSize;
      int var6 = gallopRight(this.a[var4], this.a, var2, var3, 0, this.c);
      var2 += var6;
      var3 -= var6;
      if (var3 != 0) {
         var5 = gallopLeft(this.a[var2 + var3 - 1], this.a, var4, var5, var5 - 1, this.c);
         if (var5 != 0) {
            if (var3 <= var5) {
               this.mergeLo(var2, var3, var4, var5);
            } else {
               this.mergeHi(var2, var3, var4, var5);
            }

         }
      }
   }

   private static <T> int gallopLeft(T var0, T[] var1, int var2, int var3, int var4, Comparator<? super T> var5) {
      int var6 = 0;
      int var7 = 1;
      int var8;
      if (var5.compare(var0, var1[var2 + var4]) > 0) {
         var8 = var3 - var4;

         while(var7 < var8 && var5.compare(var0, var1[var2 + var4 + var7]) > 0) {
            var6 = var7;
            var7 = (var7 << 1) + 1;
            if (var7 <= 0) {
               var7 = var8;
            }
         }

         if (var7 > var8) {
            var7 = var8;
         }

         var6 += var4;
         var7 += var4;
      } else {
         var8 = var4 + 1;

         while(var7 < var8 && var5.compare(var0, var1[var2 + var4 - var7]) <= 0) {
            var6 = var7;
            var7 = (var7 << 1) + 1;
            if (var7 <= 0) {
               var7 = var8;
            }
         }

         if (var7 > var8) {
            var7 = var8;
         }

         int var9 = var6;
         var6 = var4 - var7;
         var7 = var4 - var9;
      }

      ++var6;

      while(var6 < var7) {
         var8 = var6 + (var7 - var6 >>> 1);
         if (var5.compare(var0, var1[var2 + var8]) > 0) {
            var6 = var8 + 1;
         } else {
            var7 = var8;
         }
      }

      return var7;
   }

   private static <T> int gallopRight(T var0, T[] var1, int var2, int var3, int var4, Comparator<? super T> var5) {
      int var6 = 1;
      int var7 = 0;
      int var8;
      if (var5.compare(var0, var1[var2 + var4]) < 0) {
         var8 = var4 + 1;

         while(var6 < var8 && var5.compare(var0, var1[var2 + var4 - var6]) < 0) {
            var7 = var6;
            var6 = (var6 << 1) + 1;
            if (var6 <= 0) {
               var6 = var8;
            }
         }

         if (var6 > var8) {
            var6 = var8;
         }

         int var9 = var7;
         var7 = var4 - var6;
         var6 = var4 - var9;
      } else {
         var8 = var3 - var4;

         while(var6 < var8 && var5.compare(var0, var1[var2 + var4 + var6]) >= 0) {
            var7 = var6;
            var6 = (var6 << 1) + 1;
            if (var6 <= 0) {
               var6 = var8;
            }
         }

         if (var6 > var8) {
            var6 = var8;
         }

         var7 += var4;
         var6 += var4;
      }

      ++var7;

      while(var7 < var6) {
         var8 = var7 + (var6 - var7 >>> 1);
         if (var5.compare(var0, var1[var2 + var8]) < 0) {
            var6 = var8;
         } else {
            var7 = var8 + 1;
         }
      }

      return var6;
   }

   private void mergeLo(int var1, int var2, int var3, int var4) {
      Object[] var5 = this.a;
      Object[] var6 = this.ensureCapacity(var2);
      System.arraycopy(var5, var1, var6, 0, var2);
      int var7 = 0;
      int var9 = var1 + 1;
      int var8 = var3 + 1;
      var5[var1] = var5[var3];
      --var4;
      if (var4 == 0) {
         System.arraycopy(var6, var7, var5, var9, var2);
      } else if (var2 == 1) {
         System.arraycopy(var5, var8, var5, var9, var4);
         var5[var9 + var4] = var6[var7];
      } else {
         Comparator var10 = this.c;
         int var11 = this.minGallop;

         label81:
         while(true) {
            int var12 = 0;
            int var13 = 0;

            do {
               if (var10.compare(var5[var8], var6[var7]) < 0) {
                  var5[var9++] = var5[var8++];
                  ++var13;
                  var12 = 0;
                  --var4;
                  if (var4 == 0) {
                     break label81;
                  }
               } else {
                  var5[var9++] = var6[var7++];
                  ++var12;
                  var13 = 0;
                  --var2;
                  if (var2 == 1) {
                     break label81;
                  }
               }
            } while((var12 | var13) < var11);

            do {
               var12 = gallopRight(var5[var8], var6, var7, var2, 0, var10);
               if (var12 != 0) {
                  System.arraycopy(var6, var7, var5, var9, var12);
                  var9 += var12;
                  var7 += var12;
                  var2 -= var12;
                  if (var2 <= 1) {
                     break label81;
                  }
               }

               var5[var9++] = var5[var8++];
               --var4;
               if (var4 == 0) {
                  break label81;
               }

               var13 = gallopLeft(var6[var7], var5, var8, var4, 0, var10);
               if (var13 != 0) {
                  System.arraycopy(var5, var8, var5, var9, var13);
                  var9 += var13;
                  var8 += var13;
                  var4 -= var13;
                  if (var4 == 0) {
                     break label81;
                  }
               }

               var5[var9++] = var6[var7++];
               --var2;
               if (var2 == 1) {
                  break label81;
               }

               --var11;
            } while(var12 >= 7 | var13 >= 7);

            if (var11 < 0) {
               var11 = 0;
            }

            var11 += 2;
         }

         this.minGallop = var11 < 1 ? 1 : var11;
         if (var2 == 1) {
            System.arraycopy(var5, var8, var5, var9, var4);
            var5[var9 + var4] = var6[var7];
         } else {
            if (var2 == 0) {
               throw new IllegalArgumentException("Comparison method violates its general contract!");
            }

            System.arraycopy(var6, var7, var5, var9, var2);
         }

      }
   }

   private void mergeHi(int var1, int var2, int var3, int var4) {
      Object[] var5 = this.a;
      Object[] var6 = this.ensureCapacity(var4);
      System.arraycopy(var5, var3, var6, 0, var4);
      int var7 = var1 + var2 - 1;
      int var8 = var4 - 1;
      int var9 = var3 + var4 - 1;
      var5[var9--] = var5[var7--];
      --var2;
      if (var2 == 0) {
         System.arraycopy(var6, 0, var5, var9 - (var4 - 1), var4);
      } else if (var4 == 1) {
         var9 -= var2;
         var7 -= var2;
         System.arraycopy(var5, var7 + 1, var5, var9 + 1, var2);
         var5[var9] = var6[var8];
      } else {
         Comparator var10 = this.c;
         int var11 = this.minGallop;

         label81:
         while(true) {
            int var12 = 0;
            int var13 = 0;

            do {
               if (var10.compare(var6[var8], var5[var7]) < 0) {
                  var5[var9--] = var5[var7--];
                  ++var12;
                  var13 = 0;
                  --var2;
                  if (var2 == 0) {
                     break label81;
                  }
               } else {
                  var5[var9--] = var6[var8--];
                  ++var13;
                  var12 = 0;
                  --var4;
                  if (var4 == 1) {
                     break label81;
                  }
               }
            } while((var12 | var13) < var11);

            do {
               var12 = var2 - gallopRight(var6[var8], var5, var1, var2, var2 - 1, var10);
               if (var12 != 0) {
                  var9 -= var12;
                  var7 -= var12;
                  var2 -= var12;
                  System.arraycopy(var5, var7 + 1, var5, var9 + 1, var12);
                  if (var2 == 0) {
                     break label81;
                  }
               }

               var5[var9--] = var6[var8--];
               --var4;
               if (var4 == 1) {
                  break label81;
               }

               var13 = var4 - gallopLeft(var5[var7], var6, 0, var4, var4 - 1, var10);
               if (var13 != 0) {
                  var9 -= var13;
                  var8 -= var13;
                  var4 -= var13;
                  System.arraycopy(var6, var8 + 1, var5, var9 + 1, var13);
                  if (var4 <= 1) {
                     break label81;
                  }
               }

               var5[var9--] = var5[var7--];
               --var2;
               if (var2 == 0) {
                  break label81;
               }

               --var11;
            } while(var12 >= 7 | var13 >= 7);

            if (var11 < 0) {
               var11 = 0;
            }

            var11 += 2;
         }

         this.minGallop = var11 < 1 ? 1 : var11;
         if (var4 == 1) {
            var9 -= var2;
            var7 -= var2;
            System.arraycopy(var5, var7 + 1, var5, var9 + 1, var2);
            var5[var9] = var6[var8];
         } else {
            if (var4 == 0) {
               throw new IllegalArgumentException("Comparison method violates its general contract!");
            }

            System.arraycopy(var6, 0, var5, var9 - (var4 - 1), var4);
         }

      }
   }

   private T[] ensureCapacity(int var1) {
      this.tmpCount = Math.max(this.tmpCount, var1);
      if (this.tmp.length < var1) {
         int var2 = var1 | var1 >> 1;
         var2 |= var2 >> 2;
         var2 |= var2 >> 4;
         var2 |= var2 >> 8;
         var2 |= var2 >> 16;
         ++var2;
         if (var2 < 0) {
            var2 = var1;
         } else {
            var2 = Math.min(var2, this.a.length >>> 1);
         }

         Object[] var3 = new Object[var2];
         this.tmp = var3;
      }

      return this.tmp;
   }

   private static void rangeCheck(int var0, int var1, int var2) {
      if (var1 > var2) {
         throw new IllegalArgumentException("fromIndex(" + var1 + ") > toIndex(" + var2 + ")");
      } else if (var1 < 0) {
         throw new ArrayIndexOutOfBoundsException(var1);
      } else if (var2 > var0) {
         throw new ArrayIndexOutOfBoundsException(var2);
      }
   }
}
