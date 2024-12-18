package zombie.iso;

import zombie.characters.IsoGameCharacter;
import zombie.core.math.PZMath;

public final class LosUtil {
   public static int XSIZE = 200;
   public static int YSIZE = 200;
   public static int ZSIZE = 16;
   public static byte[][][][] cachedresults;
   public static boolean[] cachecleared;

   public LosUtil() {
   }

   public static void init(int var0, int var1) {
      XSIZE = Math.min(var0, 200);
      YSIZE = Math.min(var1, 200);
      cachedresults = new byte[XSIZE][YSIZE][ZSIZE][4];
   }

   public static TestResults lineClear(IsoCell var0, int var1, int var2, int var3, int var4, int var5, int var6, boolean var7) {
      return lineClear(var0, var1, var2, var3, var4, var5, var6, var7, 10000);
   }

   public static TestResults lineClear(IsoCell var0, int var1, int var2, int var3, int var4, int var5, int var6, boolean var7, int var8) {
      if (var6 == var3 - 1) {
         IsoGridSquare var9 = var0.getGridSquare(var4, var5, var6);
         if (var9 != null && var9.HasElevatedFloor()) {
            var6 = var3;
         }
      }

      TestResults var25 = LosUtil.TestResults.Clear;
      int var10 = var5 - var2;
      int var11 = var4 - var1;
      int var12 = var6 - var3;
      float var13 = 0.5F;
      float var14 = 0.5F;
      IsoGridSquare var18 = var0.getGridSquare(var1, var2, var3);
      int var19 = 0;
      boolean var20 = false;
      int var16;
      int var17;
      float var21;
      float var22;
      IsoGridSquare var23;
      TestResults var24;
      if (Math.abs(var11) > Math.abs(var10) && Math.abs(var11) > Math.abs(var12)) {
         var21 = (float)var10 / (float)var11;
         var22 = (float)var12 / (float)var11;
         var13 += (float)var2;
         var14 += (float)var3;
         var11 = var11 < 0 ? -1 : 1;
         var21 *= (float)var11;

         for(var22 *= (float)var11; var1 != var4; var20 = false) {
            var1 += var11;
            var13 += var21;
            var14 += var22;
            var23 = var0.getGridSquare(var1, PZMath.fastfloor(var13), PZMath.fastfloor(var14));
            if (var23 != null && var18 != null) {
               var24 = var23.testVisionAdjacent(var18.getX() - var23.getX(), var18.getY() - var23.getY(), var18.getZ() - var23.getZ(), true, var7);
               if (var24 == LosUtil.TestResults.ClearThroughWindow) {
                  var20 = true;
               }

               if (var24 == LosUtil.TestResults.Blocked || var25 == LosUtil.TestResults.Clear || var24 == LosUtil.TestResults.ClearThroughWindow && var25 == LosUtil.TestResults.ClearThroughOpenDoor) {
                  var25 = var24;
               } else if (var24 == LosUtil.TestResults.ClearThroughClosedDoor && var25 == LosUtil.TestResults.ClearThroughOpenDoor) {
                  var25 = var24;
               }

               if (var25 == LosUtil.TestResults.Blocked) {
                  return LosUtil.TestResults.Blocked;
               }

               if (var20) {
                  if (var19 > var8) {
                     return LosUtil.TestResults.Blocked;
                  }

                  var19 = 0;
               }
            }

            var18 = var23;
            var16 = PZMath.fastfloor(var13);
            var17 = PZMath.fastfloor(var14);
            ++var19;
         }
      } else {
         int var15;
         if (Math.abs(var10) >= Math.abs(var11) && Math.abs(var10) > Math.abs(var12)) {
            var21 = (float)var11 / (float)var10;
            var22 = (float)var12 / (float)var10;
            var13 += (float)var1;
            var14 += (float)var3;
            var10 = var10 < 0 ? -1 : 1;
            var21 *= (float)var10;

            for(var22 *= (float)var10; var2 != var5; var20 = false) {
               var2 += var10;
               var13 += var21;
               var14 += var22;
               var23 = var0.getGridSquare(PZMath.fastfloor(var13), var2, PZMath.fastfloor(var14));
               if (var23 != null && var18 != null) {
                  var24 = var23.testVisionAdjacent(var18.getX() - var23.getX(), var18.getY() - var23.getY(), var18.getZ() - var23.getZ(), true, var7);
                  if (var24 == LosUtil.TestResults.ClearThroughWindow) {
                     var20 = true;
                  }

                  if (var24 == LosUtil.TestResults.Blocked || var25 == LosUtil.TestResults.Clear || var24 == LosUtil.TestResults.ClearThroughWindow && var25 == LosUtil.TestResults.ClearThroughOpenDoor) {
                     var25 = var24;
                  } else if (var24 == LosUtil.TestResults.ClearThroughClosedDoor && var25 == LosUtil.TestResults.ClearThroughOpenDoor) {
                     var25 = var24;
                  }

                  if (var25 == LosUtil.TestResults.Blocked) {
                     return LosUtil.TestResults.Blocked;
                  }

                  if (var20) {
                     if (var19 > var8) {
                        return LosUtil.TestResults.Blocked;
                     }

                     var19 = 0;
                  }
               }

               var18 = var23;
               var15 = PZMath.fastfloor(var13);
               var17 = PZMath.fastfloor(var14);
               ++var19;
            }
         } else {
            var21 = (float)var11 / (float)var12;
            var22 = (float)var10 / (float)var12;
            var13 += (float)var1;
            var14 += (float)var2;
            var12 = var12 < 0 ? -1 : 1;
            var21 *= (float)var12;

            for(var22 *= (float)var12; var3 != var6; var20 = false) {
               var3 += var12;
               var13 += var21;
               var14 += var22;
               var23 = var0.getGridSquare(PZMath.fastfloor(var13), PZMath.fastfloor(var14), var3);
               if (var23 != null && var18 != null) {
                  var24 = var23.testVisionAdjacent(var18.getX() - var23.getX(), var18.getY() - var23.getY(), var18.getZ() - var23.getZ(), true, var7);
                  if (var24 == LosUtil.TestResults.ClearThroughWindow) {
                     var20 = true;
                  }

                  if (var24 != LosUtil.TestResults.Blocked && var25 != LosUtil.TestResults.Clear && (var24 != LosUtil.TestResults.ClearThroughWindow || var25 != LosUtil.TestResults.ClearThroughOpenDoor)) {
                     if (var24 == LosUtil.TestResults.ClearThroughClosedDoor && var25 == LosUtil.TestResults.ClearThroughOpenDoor) {
                        var25 = var24;
                     }
                  } else {
                     var25 = var24;
                  }

                  if (var25 == LosUtil.TestResults.Blocked) {
                     return LosUtil.TestResults.Blocked;
                  }

                  if (var20) {
                     if (var19 > var8) {
                        return LosUtil.TestResults.Blocked;
                     }

                     var19 = 0;
                  }
               }

               var18 = var23;
               var15 = PZMath.fastfloor(var13);
               var16 = PZMath.fastfloor(var14);
               ++var19;
            }
         }
      }

      return var25;
   }

   public static boolean lineClearCollide(int var0, int var1, int var2, int var3, int var4, int var5, boolean var6) {
      IsoCell var7 = IsoWorld.instance.CurrentCell;
      int var8 = var1 - var4;
      int var9 = var0 - var3;
      int var10 = var2 - var5;
      float var11 = 0.5F;
      float var12 = 0.5F;
      IsoGridSquare var16 = var7.getGridSquare(var3, var4, var5);
      int var14;
      int var15;
      float var17;
      float var18;
      IsoGridSquare var19;
      boolean var20;
      if (Math.abs(var9) > Math.abs(var8) && Math.abs(var9) > Math.abs(var10)) {
         var17 = (float)var8 / (float)var9;
         var18 = (float)var10 / (float)var9;
         var11 += (float)var4;
         var12 += (float)var5;
         var9 = var9 < 0 ? -1 : 1;
         var17 *= (float)var9;

         for(var18 *= (float)var9; var3 != var0; var15 = PZMath.fastfloor(var12)) {
            var3 += var9;
            var11 += var17;
            var12 += var18;
            var19 = var7.getGridSquare(var3, PZMath.fastfloor(var11), PZMath.fastfloor(var12));
            if (var19 != null && var16 != null) {
               var20 = var19.CalculateCollide(var16, false, false, true, true);
               if (!var6 && var19.isDoorBlockedTo(var16)) {
                  var20 = true;
               }

               if (var20) {
                  return true;
               }
            }

            var16 = var19;
            var14 = PZMath.fastfloor(var11);
         }
      } else {
         int var13;
         if (Math.abs(var8) >= Math.abs(var9) && Math.abs(var8) > Math.abs(var10)) {
            var17 = (float)var9 / (float)var8;
            var18 = (float)var10 / (float)var8;
            var11 += (float)var3;
            var12 += (float)var5;
            var8 = var8 < 0 ? -1 : 1;
            var17 *= (float)var8;

            for(var18 *= (float)var8; var4 != var1; var15 = PZMath.fastfloor(var12)) {
               var4 += var8;
               var11 += var17;
               var12 += var18;
               var19 = var7.getGridSquare(PZMath.fastfloor(var11), var4, PZMath.fastfloor(var12));
               if (var19 != null && var16 != null) {
                  var20 = var19.CalculateCollide(var16, false, false, true, true);
                  if (!var6 && var19.isDoorBlockedTo(var16)) {
                     var20 = true;
                  }

                  if (var20) {
                     return true;
                  }
               }

               var16 = var19;
               var13 = PZMath.fastfloor(var11);
            }
         } else {
            var17 = (float)var9 / (float)var10;
            var18 = (float)var8 / (float)var10;
            var11 += (float)var3;
            var12 += (float)var4;
            var10 = var10 < 0 ? -1 : 1;
            var17 *= (float)var10;

            for(var18 *= (float)var10; var5 != var2; var14 = PZMath.fastfloor(var12)) {
               var5 += var10;
               var11 += var17;
               var12 += var18;
               var19 = var7.getGridSquare(PZMath.fastfloor(var11), PZMath.fastfloor(var12), var5);
               if (var19 != null && var16 != null) {
                  var20 = var19.CalculateCollide(var16, false, false, true, true);
                  if (var20) {
                     return true;
                  }
               }

               var16 = var19;
               var13 = PZMath.fastfloor(var11);
            }
         }
      }

      return false;
   }

   public static int lineClearCollideCount(IsoGameCharacter var0, IsoCell var1, int var2, int var3, int var4, int var5, int var6, int var7) {
      int var8 = 0;
      int var9 = var3 - var6;
      int var10 = var2 - var5;
      int var11 = var4 - var7;
      float var12 = 0.5F;
      float var13 = 0.5F;
      IsoGridSquare var17 = var1.getGridSquare(var5, var6, var7);
      int var15;
      int var16;
      float var18;
      float var19;
      IsoGridSquare var20;
      boolean var21;
      if (Math.abs(var10) > Math.abs(var9) && Math.abs(var10) > Math.abs(var11)) {
         var18 = (float)var9 / (float)var10;
         var19 = (float)var11 / (float)var10;
         var12 += (float)var6;
         var13 += (float)var7;
         var10 = var10 < 0 ? -1 : 1;
         var18 *= (float)var10;

         for(var19 *= (float)var10; var5 != var2; var16 = PZMath.fastfloor(var13)) {
            var5 += var10;
            var12 += var18;
            var13 += var19;
            var20 = var1.getGridSquare(var5, PZMath.fastfloor(var12), PZMath.fastfloor(var13));
            if (var20 != null && var17 != null) {
               var21 = var17.testCollideAdjacent(var0, var20.getX() - var17.getX(), var20.getY() - var17.getY(), var20.getZ() - var17.getZ());
               if (var21) {
                  return var8;
               }
            }

            ++var8;
            var17 = var20;
            var15 = PZMath.fastfloor(var12);
         }
      } else {
         int var14;
         if (Math.abs(var9) >= Math.abs(var10) && Math.abs(var9) > Math.abs(var11)) {
            var18 = (float)var10 / (float)var9;
            var19 = (float)var11 / (float)var9;
            var12 += (float)var5;
            var13 += (float)var7;
            var9 = var9 < 0 ? -1 : 1;
            var18 *= (float)var9;

            for(var19 *= (float)var9; var6 != var3; var16 = PZMath.fastfloor(var13)) {
               var6 += var9;
               var12 += var18;
               var13 += var19;
               var20 = var1.getGridSquare(PZMath.fastfloor(var12), var6, PZMath.fastfloor(var13));
               if (var20 != null && var17 != null) {
                  var21 = var17.testCollideAdjacent(var0, var20.getX() - var17.getX(), var20.getY() - var17.getY(), var20.getZ() - var17.getZ());
                  if (var21) {
                     return var8;
                  }
               }

               ++var8;
               var17 = var20;
               var14 = PZMath.fastfloor(var12);
            }
         } else {
            var18 = (float)var10 / (float)var11;
            var19 = (float)var9 / (float)var11;
            var12 += (float)var5;
            var13 += (float)var6;
            var11 = var11 < 0 ? -1 : 1;
            var18 *= (float)var11;

            for(var19 *= (float)var11; var7 != var4; var15 = PZMath.fastfloor(var13)) {
               var7 += var11;
               var12 += var18;
               var13 += var19;
               var20 = var1.getGridSquare(PZMath.fastfloor(var12), PZMath.fastfloor(var13), var7);
               if (var20 != null && var17 != null) {
                  var21 = var17.testCollideAdjacent(var0, var20.getX() - var17.getX(), var20.getY() - var17.getY(), var20.getZ() - var17.getZ());
                  if (var21) {
                     return var8;
                  }
               }

               ++var8;
               var17 = var20;
               var14 = PZMath.fastfloor(var12);
            }
         }
      }

      return var8;
   }

   public static TestResults lineClearCached(IsoCell var0, int var1, int var2, int var3, int var4, int var5, int var6, boolean var7, int var8) {
      if (var3 == var6 - 1) {
         IsoGridSquare var9 = var0.getGridSquare(var1, var2, var3);
         if (var9 != null && var9.HasElevatedFloor()) {
            var3 = var6;
         }
      }

      int var32 = var4;
      int var10 = var5;
      int var11 = var6;
      int var12 = var2 - var5;
      int var13 = var1 - var4;
      int var14 = var3 - var6;
      int var15 = var13 + XSIZE / 2;
      int var16 = var12 + YSIZE / 2;
      int var17 = var14 + ZSIZE / 2;
      if (var15 >= 0 && var16 >= 0 && var17 >= 0 && var15 < XSIZE && var16 < YSIZE && var17 < ZSIZE) {
         TestResults var18 = LosUtil.TestResults.Clear;
         byte var19 = 1;
         if (cachedresults[var15][var16][var17][var8] != 0) {
            if (cachedresults[var15][var16][var17][var8] == 1) {
               var18 = LosUtil.TestResults.Clear;
            }

            if (cachedresults[var15][var16][var17][var8] == 2) {
               var18 = LosUtil.TestResults.ClearThroughOpenDoor;
            }

            if (cachedresults[var15][var16][var17][var8] == 3) {
               var18 = LosUtil.TestResults.ClearThroughWindow;
            }

            if (cachedresults[var15][var16][var17][var8] == 4) {
               var18 = LosUtil.TestResults.Blocked;
            }

            if (cachedresults[var15][var16][var17][var8] == 5) {
               var18 = LosUtil.TestResults.ClearThroughClosedDoor;
            }

            return var18;
         } else {
            float var20 = 0.5F;
            float var21 = 0.5F;
            IsoGridSquare var25 = var0.getGridSquare(var4, var5, var6);
            int var23;
            int var24;
            float var26;
            float var27;
            IsoGridSquare var28;
            int var29;
            int var30;
            int var31;
            if (Math.abs(var13) > Math.abs(var12) && Math.abs(var13) > Math.abs(var14)) {
               var26 = (float)var12 / (float)var13;
               var27 = (float)var14 / (float)var13;
               var20 += (float)var5;
               var21 += (float)var6;
               var13 = var13 < 0 ? -1 : 1;
               var26 *= (float)var13;

               for(var27 *= (float)var13; var4 != var1; var24 = PZMath.fastfloor(var21)) {
                  var4 += var13;
                  var20 += var26;
                  var21 += var27;
                  var28 = var0.getGridSquare(var4, PZMath.fastfloor(var20), PZMath.fastfloor(var21));
                  if (var28 != null && var25 != null) {
                     if (var19 != 4 && var28.testVisionAdjacent(var25.getX() - var28.getX(), var25.getY() - var28.getY(), var25.getZ() - var28.getZ(), true, var7) == LosUtil.TestResults.Blocked) {
                        var19 = 4;
                     }

                     var29 = var4 - var32;
                     var30 = PZMath.fastfloor(var20) - var10;
                     var31 = PZMath.fastfloor(var21) - var11;
                     var29 += XSIZE / 2;
                     var30 += YSIZE / 2;
                     var31 += ZSIZE / 2;
                     if (cachedresults[var29][var30][var31][var8] == 0) {
                        cachedresults[var29][var30][var31][var8] = (byte)var19;
                     }
                  } else {
                     var29 = var4 - var32;
                     var30 = PZMath.fastfloor(var20) - var10;
                     var31 = PZMath.fastfloor(var21) - var11;
                     var29 += XSIZE / 2;
                     var30 += YSIZE / 2;
                     var31 += ZSIZE / 2;
                     if (cachedresults[var29][var30][var31][var8] == 0) {
                        cachedresults[var29][var30][var31][var8] = (byte)var19;
                     }
                  }

                  var25 = var28;
                  var23 = PZMath.fastfloor(var20);
               }
            } else {
               int var22;
               if (Math.abs(var12) >= Math.abs(var13) && Math.abs(var12) > Math.abs(var14)) {
                  var26 = (float)var13 / (float)var12;
                  var27 = (float)var14 / (float)var12;
                  var20 += (float)var4;
                  var21 += (float)var6;
                  var12 = var12 < 0 ? -1 : 1;
                  var26 *= (float)var12;

                  for(var27 *= (float)var12; var5 != var2; var24 = PZMath.fastfloor(var21)) {
                     var5 += var12;
                     var20 += var26;
                     var21 += var27;
                     var28 = var0.getGridSquare(PZMath.fastfloor(var20), var5, PZMath.fastfloor(var21));
                     if (var28 != null && var25 != null) {
                        if (var19 != 4 && var28.testVisionAdjacent(var25.getX() - var28.getX(), var25.getY() - var28.getY(), var25.getZ() - var28.getZ(), true, var7) == LosUtil.TestResults.Blocked) {
                           var19 = 4;
                        }

                        var29 = PZMath.fastfloor(var20) - var32;
                        var30 = PZMath.fastfloor((float)var5) - var10;
                        var31 = PZMath.fastfloor(var21) - var11;
                        var29 += XSIZE / 2;
                        var30 += YSIZE / 2;
                        var31 += ZSIZE / 2;
                        if (cachedresults[var29][var30][var31][var8] == 0) {
                           cachedresults[var29][var30][var31][var8] = (byte)var19;
                        }
                     } else {
                        var29 = PZMath.fastfloor(var20) - var32;
                        var30 = PZMath.fastfloor((float)var5) - var10;
                        var31 = PZMath.fastfloor(var21) - var11;
                        var29 += XSIZE / 2;
                        var30 += YSIZE / 2;
                        var31 += ZSIZE / 2;
                        if (cachedresults[var29][var30][var31][var8] == 0) {
                           cachedresults[var29][var30][var31][var8] = (byte)var19;
                        }
                     }

                     var25 = var28;
                     var22 = PZMath.fastfloor(var20);
                  }
               } else {
                  var26 = (float)var13 / (float)var14;
                  var27 = (float)var12 / (float)var14;
                  var20 += (float)var4;
                  var21 += (float)var5;
                  var14 = var14 < 0 ? -1 : 1;
                  var26 *= (float)var14;

                  for(var27 *= (float)var14; var6 != var3; var23 = PZMath.fastfloor(var21)) {
                     var6 += var14;
                     var20 += var26;
                     var21 += var27;
                     var28 = var0.getGridSquare(PZMath.fastfloor(var20), PZMath.fastfloor(var21), var6);
                     if (var28 != null && var25 != null) {
                        if (var19 != 4 && var28.testVisionAdjacent(var25.getX() - var28.getX(), var25.getY() - var28.getY(), var25.getZ() - var28.getZ(), true, var7) == LosUtil.TestResults.Blocked) {
                           var19 = 4;
                        }

                        var29 = PZMath.fastfloor(var20) - var32;
                        var30 = PZMath.fastfloor(var21) - var10;
                        var31 = PZMath.fastfloor((float)var6) - var11;
                        var29 += XSIZE / 2;
                        var30 += YSIZE / 2;
                        var31 += ZSIZE / 2;
                        if (cachedresults[var29][var30][var31][var8] == 0) {
                           cachedresults[var29][var30][var31][var8] = (byte)var19;
                        }
                     } else {
                        var29 = PZMath.fastfloor(var20) - var32;
                        var30 = PZMath.fastfloor(var21) - var10;
                        var31 = PZMath.fastfloor((float)var6) - var11;
                        var29 += XSIZE / 2;
                        var30 += YSIZE / 2;
                        var31 += ZSIZE / 2;
                        if (cachedresults[var29][var30][var31][var8] == 0) {
                           cachedresults[var29][var30][var31][var8] = (byte)var19;
                        }
                     }

                     var25 = var28;
                     var22 = PZMath.fastfloor(var20);
                  }
               }
            }

            if (var19 == 1) {
               cachedresults[var15][var16][var17][var8] = (byte)var19;
               return LosUtil.TestResults.Clear;
            } else if (var19 == 2) {
               cachedresults[var15][var16][var17][var8] = (byte)var19;
               return LosUtil.TestResults.ClearThroughOpenDoor;
            } else if (var19 == 3) {
               cachedresults[var15][var16][var17][var8] = (byte)var19;
               return LosUtil.TestResults.ClearThroughWindow;
            } else if (var19 == 4) {
               cachedresults[var15][var16][var17][var8] = (byte)var19;
               return LosUtil.TestResults.Blocked;
            } else if (var19 == 5) {
               cachedresults[var15][var16][var17][var8] = (byte)var19;
               return LosUtil.TestResults.ClearThroughClosedDoor;
            } else {
               return LosUtil.TestResults.Blocked;
            }
         }
      } else {
         return LosUtil.TestResults.Blocked;
      }
   }

   public static IsoGridSquareCollisionData getFirstBlockingIsoGridSquare(IsoCell var0, int var1, int var2, int var3, int var4, int var5, int var6, boolean var7) {
      Vector3 var8 = new Vector3();
      short var9 = 10000;
      IsoGridSquareCollisionData var10 = new IsoGridSquareCollisionData();
      if (var6 == var3 - 1) {
         IsoGridSquare var11 = var0.getGridSquare(var4, var5, var6);
         if (var11 != null && var11.HasElevatedFloor()) {
            var6 = var3;
         }
      }

      TestResults var31 = LosUtil.TestResults.Clear;
      int var12 = var5 - var2;
      int var13 = var4 - var1;
      int var14 = var6 - var3;
      float var15 = 0.5F;
      float var16 = 0.5F;
      IsoGridSquare var20 = var0.getGridSquare(var1, var2, var3);
      int var21 = 0;
      boolean var22 = false;
      int var18;
      int var19;
      float var23;
      float var24;
      IsoGridSquare var25;
      int var26;
      int var27;
      int var28;
      TestResults var29;
      IsoGridSquare var30;
      if (Math.abs(var13) > Math.abs(var12) && Math.abs(var13) > Math.abs(var14)) {
         var23 = (float)var12 / (float)var13;
         var24 = (float)var14 / (float)var13;
         var15 += (float)var2;
         var16 += (float)var3;
         var13 = var13 < 0 ? -1 : 1;
         var23 *= (float)var13;

         for(var24 *= (float)var13; var1 != var4; var22 = false) {
            var1 += var13;
            var15 += var23;
            var16 += var24;
            var25 = var0.getGridSquare(var1, PZMath.fastfloor(var15), PZMath.fastfloor(var16));
            if (var25 != null && var20 != null) {
               var26 = var20.getX() - var25.getX();
               var27 = var20.getY() - var25.getY();
               var28 = var20.getZ() - var25.getZ();
               var8.x = (float)(var25.getX() + var20.getX()) * 0.5F;
               var8.y = (float)(var25.getY() + var20.getY()) * 0.5F;
               var8.z = (float)(var25.getZ() + var20.getZ()) * 0.5F;
               var25.getFirstBlocking(var10, var26, var27, var28, true, var7);
               var29 = var10.testResults;
               if (var29 == LosUtil.TestResults.ClearThroughWindow) {
                  var22 = true;
               }

               if (var29 != LosUtil.TestResults.Blocked && var31 != LosUtil.TestResults.Clear && (var29 != LosUtil.TestResults.ClearThroughWindow || var31 != LosUtil.TestResults.ClearThroughOpenDoor)) {
                  if (var29 == LosUtil.TestResults.ClearThroughClosedDoor && var31 == LosUtil.TestResults.ClearThroughOpenDoor) {
                     var31 = var29;
                  }
               } else {
                  var31 = var29;
               }

               if (var31 == LosUtil.TestResults.Blocked) {
                  var30 = null;
                  if (var26 < 0) {
                     var8.x -= (float)var26;
                     var8.y -= (float)var27;
                     var8.z -= (float)var28;
                     var30 = IsoCell.getInstance().getGridSquare(var25.getX(), var25.getY(), var25.getZ());
                  } else {
                     var30 = IsoCell.getInstance().getGridSquare(var20.getX(), var20.getY(), var20.getZ());
                  }

                  var10.isoGridSquare = var30;
                  var10.hitPosition.set(var8.x, var8.y, var8.z);
                  var10.testResults = LosUtil.TestResults.Blocked;
                  return var10;
               }

               if (var22) {
                  if (var21 > var9) {
                     var10.isoGridSquare = var20;
                     var10.hitPosition.set(var8.x, var8.y, var8.z);
                     var10.testResults = LosUtil.TestResults.Blocked;
                     return var10;
                  }

                  var21 = 0;
               }
            }

            var20 = var25;
            var18 = PZMath.fastfloor(var15);
            var19 = PZMath.fastfloor(var16);
            ++var21;
         }
      } else {
         int var17;
         if (Math.abs(var12) >= Math.abs(var13) && Math.abs(var12) > Math.abs(var14)) {
            var23 = (float)var13 / (float)var12;
            var24 = (float)var14 / (float)var12;
            var15 += (float)var1;
            var16 += (float)var3;
            var12 = var12 < 0 ? -1 : 1;
            var23 *= (float)var12;

            for(var24 *= (float)var12; var2 != var5; var22 = false) {
               var2 += var12;
               var15 += var23;
               var16 += var24;
               var25 = var0.getGridSquare(PZMath.fastfloor(var15), var2, PZMath.fastfloor(var16));
               if (var25 != null && var20 != null) {
                  var26 = var20.getX() - var25.getX();
                  var27 = var20.getY() - var25.getY();
                  var28 = var20.getZ() - var25.getZ();
                  var8.x = (float)(var20.getX() + var25.getX()) * 0.5F;
                  var8.y = (float)(var20.getY() + var25.getY()) * 0.5F;
                  var8.z = (float)(var20.getZ() + var25.getZ()) * 0.5F;
                  var25.getFirstBlocking(var10, var26, var27, var28, true, var7);
                  var29 = var10.testResults;
                  if (var29 == LosUtil.TestResults.ClearThroughWindow) {
                     var22 = true;
                  }

                  if (var29 == LosUtil.TestResults.Blocked || var31 == LosUtil.TestResults.Clear || var29 == LosUtil.TestResults.ClearThroughWindow && var31 == LosUtil.TestResults.ClearThroughOpenDoor) {
                     var31 = var29;
                  } else if (var29 == LosUtil.TestResults.ClearThroughClosedDoor && var31 == LosUtil.TestResults.ClearThroughOpenDoor) {
                     var31 = var29;
                  }

                  if (var31 == LosUtil.TestResults.Blocked) {
                     var30 = null;
                     if (var27 < 0) {
                        var8.x -= (float)var26;
                        var8.y -= (float)var27;
                        var8.z -= (float)var28;
                        var30 = IsoCell.getInstance().getGridSquare(var25.getX(), var25.getY(), var25.getZ());
                     } else {
                        var30 = IsoCell.getInstance().getGridSquare(var20.getX(), var20.getY(), var20.getZ());
                     }

                     var10.isoGridSquare = var30;
                     var10.hitPosition.set(var8.x, var8.y, var8.z);
                     var10.testResults = LosUtil.TestResults.Blocked;
                     return var10;
                  }

                  if (var22) {
                     if (var21 > var9) {
                        var10.isoGridSquare = var20;
                        var10.hitPosition.set(var8.x, var8.y, var8.z);
                        var10.testResults = LosUtil.TestResults.Blocked;
                        return var10;
                     }

                     var21 = 0;
                  }
               }

               var20 = var25;
               var17 = PZMath.fastfloor(var15);
               var19 = PZMath.fastfloor(var16);
               ++var21;
            }
         } else {
            var23 = (float)var13 / (float)var14;
            var24 = (float)var12 / (float)var14;
            var15 += (float)var1;
            var16 += (float)var2;
            var14 = var14 < 0 ? -1 : 1;
            var23 *= (float)var14;

            for(var24 *= (float)var14; var3 != var6; var22 = false) {
               var3 += var14;
               var15 += var23;
               var16 += var24;
               var25 = var0.getGridSquare(PZMath.fastfloor(var15), PZMath.fastfloor(var16), var3);
               if (var25 != null && var20 != null) {
                  var26 = var20.getX() - var25.getX();
                  var27 = var20.getY() - var25.getY();
                  var28 = var20.getZ() - var25.getZ();
                  var8.x = (float)(var20.getX() + var25.getX()) * 0.5F;
                  var8.y = (float)(var20.getY() + var25.getY()) * 0.5F;
                  var8.z = (float)(var20.getZ() + var25.getZ()) * 0.5F;
                  var25.getFirstBlocking(var10, var26, var27, var28, true, var7);
                  var29 = var10.testResults;
                  if (var29 == LosUtil.TestResults.ClearThroughWindow) {
                     var22 = true;
                  }

                  if (var29 == LosUtil.TestResults.Blocked || var31 == LosUtil.TestResults.Clear || var29 == LosUtil.TestResults.ClearThroughWindow && var31 == LosUtil.TestResults.ClearThroughOpenDoor) {
                     var31 = var29;
                  } else if (var29 == LosUtil.TestResults.ClearThroughClosedDoor && var31 == LosUtil.TestResults.ClearThroughOpenDoor) {
                     var31 = var29;
                  }

                  if (var31 == LosUtil.TestResults.Blocked) {
                     var10.isoGridSquare = var20;
                     var10.hitPosition.set(var8.x, var8.y, var8.z);
                     var10.testResults = LosUtil.TestResults.Blocked;
                     return var10;
                  }

                  if (var22) {
                     if (var21 > var9) {
                        var10.isoGridSquare = var20;
                        var10.hitPosition.set(var8.x, var8.y, var8.z);
                        var10.testResults = LosUtil.TestResults.Blocked;
                        return var10;
                     }

                     var21 = 0;
                  }
               }

               var20 = var25;
               var17 = PZMath.fastfloor(var15);
               var18 = PZMath.fastfloor(var16);
               ++var21;
            }
         }
      }

      var10.isoGridSquare = null;
      var10.hitPosition.set(0.0F, 0.0F, 0.0F);
      var10.testResults = var31;
      return var10;
   }

   static {
      cachedresults = new byte[XSIZE][YSIZE][ZSIZE][4];
      cachecleared = new boolean[4];

      for(int var0 = 0; var0 < 4; ++var0) {
         cachecleared[var0] = true;
      }

   }

   public static enum TestResults {
      Clear,
      ClearThroughOpenDoor,
      ClearThroughWindow,
      Blocked,
      ClearThroughClosedDoor;

      private TestResults() {
      }
   }
}
