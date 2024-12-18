package zombie.erosion.categories;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.erosion.ErosionData;
import zombie.erosion.obj.ErosionObjOverlay;
import zombie.erosion.obj.ErosionObjOverlaySprites;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.worldgen.biomes.IBiome;

public final class WallCracks extends ErosionCategory {
   private ArrayList<ErosionObjOverlay> objs = new ArrayList();
   private static final int DIRNW = 0;
   private static final int DIRN = 1;
   private static final int DIRW = 2;
   private ArrayList<ArrayList<Integer>> objsRef = new ArrayList();
   private ArrayList<ArrayList<Integer>> botRef = new ArrayList();
   private ArrayList<ArrayList<Integer>> topRef = new ArrayList();
   private int[] spawnChance = new int[100];

   public WallCracks() {
   }

   public boolean replaceExistingObject(IsoGridSquare var1, ErosionData.Square var2, ErosionData.Chunk var3, IBiome var4, boolean var5, boolean var6) {
      return false;
   }

   public boolean validateSpawn(IsoGridSquare var1, ErosionData.Square var2, ErosionData.Chunk var3, IBiome var4, boolean var5, boolean var6, boolean var7) {
      if (!var5) {
         return false;
      } else {
         int var8 = var2.noiseMainInt;
         int var9 = this.spawnChance[var8];
         if (var9 == 0) {
            return false;
         } else if (var2.rand(var1.x, var1.y, 101) >= var9) {
            return false;
         } else {
            boolean var10 = true;
            IsoObject var12 = this.validWall(var1, true, false);
            String var11;
            if (var12 != null) {
               var11 = var12.getSprite().getName();
               if (var11 != null && var11.startsWith("fencing")) {
                  var12 = null;
               }
            }

            IsoObject var13 = this.validWall(var1, false, false);
            if (var13 != null) {
               var11 = var13.getSprite().getName();
               if (var11 != null && var11.startsWith("fencing")) {
                  var13 = null;
               }
            }

            byte var20;
            if (var12 != null && var13 != null) {
               var20 = 0;
            } else if (var12 != null) {
               var20 = 1;
            } else {
               if (var13 == null) {
                  return false;
               }

               var20 = 2;
            }

            boolean var14 = var8 < 35 && var2.magicNum > 0.3F;
            CategoryData var15 = (CategoryData)this.setCatModData(var2);
            var15.gameObj = (Integer)((ArrayList)this.objsRef.get(var20)).get(var2.rand(var1.x, var1.y, ((ArrayList)this.objsRef.get(var20)).size()));
            var15.alpha = 0.0F;
            var15.spawnTime = var8;
            if (var14) {
               IsoGridSquare var16 = IsoWorld.instance.CurrentCell.getGridSquare(var1.getX(), var1.getY(), var1.getZ() + 1);
               if (var16 != null) {
                  IsoObject var17 = this.validWall(var16, var20 == 1, false);
                  if (var17 != null) {
                     int var18 = var2.rand(var1.x, var1.y, ((ArrayList)this.botRef.get(var20)).size());
                     var15.gameObj = (Integer)((ArrayList)this.botRef.get(var20)).get(var18);
                     CategoryData var19 = new CategoryData();
                     var19.gameObj = (Integer)((ArrayList)this.topRef.get(var20)).get(var18);
                     var19.alpha = 0.0F;
                     var19.spawnTime = var15.spawnTime;
                     var15.hasTop = var19;
                  }
               }
            }

            return true;
         }
      }
   }

   public void update(IsoGridSquare var1, ErosionData.Square var2, ErosionCategory.Data var3, ErosionData.Chunk var4, int var5) {
      CategoryData var6 = (CategoryData)var3;
      if (var5 >= var6.spawnTime && !var6.doNothing) {
         if (var6.gameObj >= 0 && var6.gameObj < this.objs.size()) {
            ErosionObjOverlay var7 = (ErosionObjOverlay)this.objs.get(var6.gameObj);
            float var8 = var6.alpha;
            float var9 = (float)(var5 - var6.spawnTime) / 100.0F;
            if (var9 > 1.0F) {
               var9 = 1.0F;
            }

            if (var9 < 0.0F) {
               var9 = 0.0F;
            }

            if (var9 != var8) {
               IsoObject var10 = null;
               IsoObject var11 = this.validWall(var1, true, false);
               IsoObject var12 = this.validWall(var1, false, false);
               if (var11 != null && var12 != null) {
                  var10 = var11;
               } else if (var11 != null) {
                  var10 = var11;
               } else if (var12 != null) {
                  var10 = var12;
               }

               if (var10 != null) {
                  int var13 = var6.curID;
                  byte var14 = 0;
                  int var15 = var7.setOverlay(var10, var13, var14, 0, var9);
                  if (var15 >= 0) {
                     var6.alpha = var9;
                     var6.curID = var15;
                  }
               } else {
                  var6.doNothing = true;
               }

               if (var6.hasTop != null) {
                  IsoGridSquare var16 = IsoWorld.instance.CurrentCell.getGridSquare(var1.getX(), var1.getY(), var1.getZ() + 1);
                  if (var16 != null) {
                     this.update(var16, var2, var6.hasTop, var4, var5);
                  }
               }
            }
         } else {
            var6.doNothing = true;
         }

      }
   }

   public void init() {
      for(int var1 = 0; var1 < 100; ++var1) {
         this.spawnChance[var1] = var1 <= 50 ? 100 : 0;
      }

      String var7 = "d_wallcracks_1_";
      int[] var2 = new int[]{2, 2, 2, 1, 1, 1, 0, 0, 0};

      int var3;
      for(var3 = 0; var3 < 3; ++var3) {
         this.objsRef.add(new ArrayList());
         this.topRef.add(new ArrayList());
         this.botRef.add(new ArrayList());
      }

      for(var3 = 0; var3 < var2.length; ++var3) {
         for(int var4 = 0; var4 <= 7; ++var4) {
            int var5 = var4 * 9 + var3;
            ErosionObjOverlaySprites var6 = new ErosionObjOverlaySprites(1, "WallCracks");
            var6.setSprite(0, var7 + var5, 0);
            this.objs.add(new ErosionObjOverlay(var6, 60, true));
            ((ArrayList)this.objsRef.get(var2[var3])).add(this.objs.size() - 1);
            if (var4 == 0) {
               ((ArrayList)this.botRef.get(var2[var3])).add(this.objs.size() - 1);
            } else if (var4 == 1) {
               ((ArrayList)this.topRef.get(var2[var3])).add(this.objs.size() - 1);
            }
         }
      }

   }

   protected ErosionCategory.Data allocData() {
      return new CategoryData();
   }

   public void getObjectNames(ArrayList<String> var1) {
      for(int var2 = 0; var2 < this.objs.size(); ++var2) {
         if (((ErosionObjOverlay)this.objs.get(var2)).name != null && !var1.contains(((ErosionObjOverlay)this.objs.get(var2)).name)) {
            var1.add(((ErosionObjOverlay)this.objs.get(var2)).name);
         }
      }

   }

   private static final class CategoryData extends ErosionCategory.Data {
      public int gameObj;
      public int spawnTime;
      public int curID = -999999;
      public float alpha;
      public CategoryData hasTop;

      private CategoryData() {
      }

      public void save(ByteBuffer var1) {
         super.save(var1);
         var1.put((byte)this.gameObj);
         var1.putShort((short)this.spawnTime);
         var1.putInt(this.curID);
         var1.putFloat(this.alpha);
         if (this.hasTop != null) {
            var1.put((byte)1);
            var1.put((byte)this.hasTop.gameObj);
            var1.putShort((short)this.hasTop.spawnTime);
            var1.putInt(this.hasTop.curID);
            var1.putFloat(this.hasTop.alpha);
         } else {
            var1.put((byte)0);
         }

      }

      public void load(ByteBuffer var1, int var2) {
         super.load(var1, var2);
         this.gameObj = var1.get();
         this.spawnTime = var1.getShort();
         this.curID = var1.getInt();
         this.alpha = var1.getFloat();
         boolean var3 = var1.get() == 1;
         if (var3) {
            this.hasTop = new CategoryData();
            this.hasTop.gameObj = var1.get();
            this.hasTop.spawnTime = var1.getShort();
            this.hasTop.curID = var1.getInt();
            this.hasTop.alpha = var1.getFloat();
         }

      }
   }
}
