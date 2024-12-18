package zombie.iso.worldgen;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import zombie.GameWindow;
import zombie.debug.DebugLog;
import zombie.iso.CellLoader;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.objects.IsoTree;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.iso.worldgen.biomes.Feature;
import zombie.iso.worldgen.biomes.FeatureType;
import zombie.iso.worldgen.biomes.IBiome;
import zombie.iso.worldgen.biomes.TileGroup;
import zombie.iso.worldgen.roads.Road;
import zombie.iso.worldgen.veins.OreVein;

public class WGTile {
   public static final String NO_TREE = "NO_TREE";
   public static final String NO_BUSH = "NO_BUSH";
   public static final String NO_GRASS = "NO_GRASS";

   public WGTile() {
   }

   public void setTiles(IBiome var1, IsoGridSquare var2, IsoChunk var3, IsoCell var4, int var5, int var6, int var7, int var8, int var9, int var10, EnumMap<FeatureType, String[]> var11, boolean var12, Random var13) {
      ArrayList var14 = new ArrayList();
      FeatureType[] var15 = FeatureType.values();
      int var16 = var15.length;

      for(int var17 = 0; var17 < var16; ++var17) {
         FeatureType var18 = var15[var17];
         String var19 = this.getBiomeTile(var1, var18, var3, var8, var9, var10, var11, var13);
         if (var19 != null) {
            if (var12) {
               IsoTree var20 = var2.getTree();
               if (var20 != null) {
                  var2.DeleteTileObject(var20);
               }

               IsoObject var21 = var2.getBush();
               if (var21 != null) {
                  var2.DeleteTileObject(var21);
               }

               if (var2.getObjects().size() - var2.getGrassLike().size() == 1) {
                  var14.add(var19);
               }
            } else {
               if (var14.size() > 1) {
                  var14.remove(1);
               }

               var14.add(var19);
            }
         }
      }

      Iterator var22 = var14.iterator();

      while(var22.hasNext()) {
         String var23 = (String)var22.next();
         this.applyTile(var23, var2, var4, var5, var6, var7, var13);
      }

   }

   public boolean setTiles(IBiome var1, FeatureType var2, IsoGridSquare var3, IsoChunk var4, IsoCell var5, int var6, int var7, int var8, int var9, int var10, int var11, EnumMap<FeatureType, String[]> var12, Random var13) {
      for(int var14 = 0; var14 < 5; ++var14) {
         String var15 = this.getBiomeTile(var1, var2, var4, var9, var10, var11, var12, var13);
         if (var15 != null) {
            if (var2 != FeatureType.TREE) {
               DebugLog.WorldGen.debugln(String.format("%10s -> %s", var2, var15));
            }

            this.applyTile(var15, var3, var5, var6, var7, var8, var13);
            return true;
         }
      }

      return false;
   }

   private String getBiomeTile(IBiome var1, FeatureType var2, IsoChunk var3, int var4, int var5, int var6, EnumMap<FeatureType, String[]> var7, Random var8) {
      String var9 = ((String[])var7.get(var2))[var4 + var5 * 8];
      if (var9 == null || var9.isEmpty()) {
         List var10 = (List)var1.getFeatures().get(var2);
         if (var10 == null || var10.isEmpty()) {
            return null;
         }

         float var11 = var8.nextFloat();
         float var12 = 0.0F;
         Feature var13 = null;
         Iterator var14 = var10.iterator();

         while(var14.hasNext()) {
            Feature var15 = (Feature)var14.next();
            var12 += var15.probability();
            if (!(var11 >= var12)) {
               var13 = var15;
               break;
            }
         }

         if (var13 == null) {
            return null;
         }

         List var18 = var13.tileGroups();
         TileGroup var19 = (TileGroup)var18.get(var8.nextInt(var18.size()));
         if (var4 + var19.sx() - 1 >= 8 || var5 + var19.sy() - 1 >= 8) {
            return null;
         }

         if (!this.checkFutureSquares(var19, var3, var4, var5, var6, var1.placement())) {
            return null;
         }

         for(int var16 = 0; var16 < var19.sx(); ++var16) {
            for(int var17 = 0; var17 < var19.sy(); ++var17) {
               ((String[])var7.get(var2))[var16 + var4 + (var17 + var5) * 8] = (String)var19.tiles().get(var16 + var17 * var19.sx());
            }
         }

         var9 = (String)var19.tiles().get(0);
      }

      return var9;
   }

   private boolean checkFutureSquares(TileGroup var1, IsoChunk var2, int var3, int var4, int var5, List<String> var6) {
      if (var1.sx() == 1 && var1.sy() == 1) {
         return true;
      } else {
         for(int var7 = 0; var7 < var1.sx(); ++var7) {
            for(int var8 = 0; var8 < var1.sy(); ++var8) {
               if (var7 != 0 || var8 != 0) {
                  IsoGridSquare var9 = var2.getGridSquare(var3 + var7, var4 + var8, var5);
                  if (var9 == null) {
                     return false;
                  }

                  IsoObject var10 = var9.getFloor();
                  if (var10 == null) {
                     return false;
                  }

                  if (var9.getObjects().size() - var9.getGrassLike().size() - var9.getBushes().size() > 1) {
                     return false;
                  }

                  if (!WGUtils.instance.canPlace(var6, var10.getSprite().getName())) {
                     return false;
                  }
               }
            }
         }

         return true;
      }
   }

   public void setTile(OreVein var1, IsoGridSquare var2, IsoCell var3, int var4, int var5, int var6, int var7, int var8, int var9, EnumMap<FeatureType, String[]> var10, Random var11) {
      List var12 = var1.getSingleFeatures();
      TileGroup var13 = (TileGroup)var12.get(var11.nextInt(var12.size()));
      this.applyTile((String)var13.tiles().get(0), var2, var3, var4, var5, var6, var11);
   }

   public void setTile(Road var1, IsoGridSquare var2, IsoCell var3, int var4, int var5, int var6, int var7, int var8, int var9, EnumMap<FeatureType, String[]> var10, Random var11) {
      List var12 = var1.getSingleFeatures();
      TileGroup var13 = (TileGroup)var12.get(var11.nextInt(var12.size()));
      this.applyTile((String)var13.tiles().get(0), var2, var3, var4, var5, var6, var11);
   }

   public void applyTile(String var1, IsoGridSquare var2, IsoCell var3, int var4, int var5, int var6, Random var7) {
      if (!var1.equals("NO_TREE") && !var1.equals("NO_BUSH") && !var1.equals("NO_GRASS")) {
         IsoSprite var8 = this.getSprite(IsoChunk.Fix2x(var1));
         CellLoader.DoTileObjectCreation(var8, var8.getType(), var2, var3, var4, var5, var6, var1);
      }
   }

   public IsoSprite getSprite(String var1) {
      IsoSprite var2 = (IsoSprite)IsoSpriteManager.instance.NamedMap.get(var1);
      if (var2 == null) {
         Logger.getLogger(GameWindow.class.getName()).log(Level.SEVERE, "Missing tile definition: " + var1);
         var2 = IsoSprite.getSprite(IsoSpriteManager.instance, (String)"carpentry_02_58", 0);
      }

      return var2;
   }

   public TileGroup getGround(IBiome var1, Random var2) {
      List var3 = (List)var1.getFeatures().get(FeatureType.GROUND);
      Feature var4 = (Feature)var3.get(var2.nextInt(var3.size()));
      List var5 = var4.tileGroups();
      return (TileGroup)var5.get(var2.nextInt(var3.size()));
   }

   public TileGroup getPlant(IBiome var1, Random var2) {
      List var3 = (List)var1.getFeatures().get(FeatureType.PLANT);
      if (var3 != null && !var3.isEmpty()) {
         Feature var4 = (Feature)var3.get(var2.nextInt(var3.size()));
         if (var2.nextFloat() > var4.probability()) {
            return null;
         } else {
            List var5 = var4.tileGroups();
            return (TileGroup)var5.get(var2.nextInt(var5.size()));
         }
      } else {
         return null;
      }
   }

   public void setGround(IsoSprite var1, IsoGridSquare var2) {
      var1.solidfloor = true;
      IsoObject var3 = var2.getFloor();
      if (var3 != null) {
         var3.clearAttachedAnimSprite();
         var3.setSprite(var1);
      }

   }

   public void deleteTiles(IsoGridSquare var1) {
      ArrayList var2 = new ArrayList();
      IsoObject[] var3 = (IsoObject[])var1.getObjects().getElements();
      int var4 = var3.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         IsoObject var6 = var3[var5];
         if (var6 != null && !var6.isFloor()) {
            var2.add(var6);
         }
      }

      Iterator var7 = var2.iterator();

      while(var7.hasNext()) {
         IsoObject var8 = (IsoObject)var7.next();
         var1.DeleteTileObject(var8);
      }

   }

   public void deleteTiles(IsoGridSquare var1, List<String> var2) {
      ArrayList var3 = new ArrayList();
      IsoObject[] var4 = (IsoObject[])var1.getObjects().getElements();
      int var5 = var4.length;

      for(int var6 = 0; var6 < var5; ++var6) {
         IsoObject var7 = var4[var6];
         if (var7 != null) {
            String var8 = var7.getSprite().name;
            if (var2.contains(var8)) {
               var3.add(var7);
            }
         }
      }

      Iterator var9 = var3.iterator();

      while(var9.hasNext()) {
         IsoObject var10 = (IsoObject)var9.next();
         var1.DeleteTileObject(var10);
      }

   }
}
