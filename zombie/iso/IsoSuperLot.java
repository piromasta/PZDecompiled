package zombie.iso;

import java.util.ArrayList;
import zombie.popman.ObjectPool;

public class IsoSuperLot {
   public static final ObjectPool<IsoSuperLot> pool = new ObjectPool(IsoSuperLot::new);
   public int levels = 8;
   private int wxOld;
   private int wyOld;
   IsoLot[][] lots = new IsoLot[2][2];
   ArrayList<String>[][][] squares = new ArrayList[20][20][8];

   public IsoSuperLot() {
   }

   public static synchronized void put(IsoSuperLot var0) {
      var0.squares = new ArrayList[20][20][8];
      pool.release((Object)var0);
   }

   public static synchronized IsoSuperLot get(Integer var0, Integer var1, Integer var2, Integer var3, IsoChunk var4) {
      IsoSuperLot var5 = (IsoSuperLot)pool.alloc();
      var5.load(var0, var1, var2, var3, var4);
      return var5;
   }

   public ArrayList<String> getSquareFromNewLotSize(int var1, int var2, int var3) {
      return this.getSquare(var1 - this.wxOld * 10, var2 - this.wyOld * 10, var3);
   }

   private ArrayList<String> getSquare(int var1, int var2, int var3) {
      return this.squares[var1][var2][var3];
   }

   private void load(Integer var1, Integer var2, Integer var3, Integer var4, IsoChunk var5) {
      int var6 = var3 * 8;
      int var7 = var4 * 8;
      int var8 = var6 / 10;
      int var9 = var7 / 10;
      this.wxOld = var8;
      this.wyOld = var9;
      int var10 = (var6 + 8) / 10;
      int var11 = (var7 + 8) / 10;
      int var12 = var10 - var8;
      int var13 = var11 - var9;

      for(int var14 = 0; var14 <= var12; ++var14) {
         for(int var15 = 0; var15 <= var13; ++var15) {
            int var16 = (var8 + var14) / 30;
            int var17 = (var9 + var15) / 30;
            IsoLot var18 = IsoLot.get((MapFiles)IsoLot.MapFiles.get(0), var16, var17, var8 + var14, var9 + var15, var5);
            this.PlaceLot(var18, var14, var15);
         }
      }

   }

   private void PlaceLot(IsoLot var1, int var2, int var3) {
      for(int var4 = 0; var4 < 10; ++var4) {
         for(int var5 = 0; var5 < 10; ++var5) {
            for(int var6 = 0; var6 < 8; ++var6) {
               int var7 = var4 + var5 * 10 + var6 * 100;
               int var8 = var1.m_offsetInData[var7];
               if (var8 != -1) {
                  int var9 = var1.m_data.getQuick(var8);
                  if (var9 > 0) {
                     for(int var10 = 0; var10 < var9; ++var10) {
                        String var11 = (String)var1.info.tilesUsed.get(var1.m_data.get(var8 + 1 + var10));
                        if (this.squares[var4 + var2 * 10][var5 + var3 * 10][var6] == null) {
                           this.squares[var4 + var2 * 10][var5 + var3 * 10][var6] = new ArrayList();
                        }

                        this.squares[var4 + var2 * 10][var5 + var3 * 10][var6].add(var11);
                     }
                  }
               }
            }
         }
      }

   }
}
