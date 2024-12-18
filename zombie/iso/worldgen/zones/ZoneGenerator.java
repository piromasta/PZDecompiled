package zombie.iso.worldgen.zones;

import gnu.trove.list.array.TIntArrayList;
import java.awt.Rectangle;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaManager;
import zombie.characters.animals.AnimalManagerWorker;
import zombie.characters.animals.AnimalZone;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.inventory.ItemConfigurator;
import zombie.iso.IsoCell;
import zombie.iso.IsoMetaCell;
import zombie.iso.IsoMetaChunk;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoWorld;
import zombie.iso.enums.MetaCellPresence;
import zombie.iso.worldgen.WGParams;
import zombie.iso.worldgen.WGReader;
import zombie.iso.worldgen.maps.BiomeMap;
import zombie.iso.zones.ZoneGeometryType;

public class ZoneGenerator {
   private final BiomeMap map;
   private final List<AnimalsPathConfig> animalsPathConfig;

   public ZoneGenerator(BiomeMap var1) {
      this.map = var1;
      WGReader var2 = new WGReader();
      this.animalsPathConfig = var2.loadAnimalsPath((KahluaTable)LuaManager.env.rawget("animals_path_config"));
   }

   public void genForaging(int var1, int var2) {
      IsoMetaGrid var3 = IsoWorld.instance.getMetaGrid();
      int var4 = PZMath.fastfloor((float)var1 / (float)IsoCell.CellSizeInChunks);
      int var5 = PZMath.fastfloor((float)var2 / (float)IsoCell.CellSizeInChunks);
      if (var3.hasCellData(var4, var5).equals(MetaCellPresence.NOT_LOADED)) {
         var3.setCellData(var4, var5, new IsoMetaCell(var4, var5));
      }

      IsoMetaChunk var6 = var3.getChunkData(var1, var2);
      if (var3.hasCellData(var4, var5).equals(MetaCellPresence.LOADED) && !var6.doesHaveForaging()) {
         int var7 = var4 * IsoCell.CellSizeInSquares;
         int var8 = var5 * IsoCell.CellSizeInSquares;
         int var9 = PZMath.fastfloor((float)var7 / 8.0F);
         int var10 = PZMath.fastfloor((float)var8 / 8.0F);
         HashMap var11 = new HashMap();

         int var18;
         for(int var12 = 0; var12 < IsoCell.CellSizeInChunks; ++var12) {
            for(int var13 = 0; var13 < IsoCell.CellSizeInChunks; ++var13) {
               int var14 = var13 * IsoCell.CellSizeInChunks + var12;

               Set var15;
               try {
                  int[] var16 = this.map.getZones(var9 + var12, var10 + var13);
                  var15 = (Set)Arrays.stream(var16).distinct().collect(HashSet::new, HashSet::add, AbstractCollection::addAll);
               } catch (ArrayIndexOutOfBoundsException var26) {
                  var15 = Set.of(255);
               } catch (NullPointerException var27) {
                  var15 = Set.of(255);
               }

               boolean var32 = var3.getChunkData(var12 + var9, var13 + var10).doesHaveForaging();

               for(Iterator var17 = var15.iterator(); var17.hasNext(); ((Boolean[])var11.computeIfAbsent(var18, (var0) -> {
                  Boolean[] var1 = new Boolean[IsoCell.CellSizeInChunks * IsoCell.CellSizeInChunks];
                  Arrays.fill(var1, true);
                  return var1;
               }))[var14] = var32) {
                  var18 = (Integer)var17.next();
               }
            }
         }

         Iterator var28 = var11.keySet().iterator();

         while(var28.hasNext()) {
            Integer var29 = (Integer)var28.next();
            ItemConfigurator.registerZone(this.map.getZoneName(var29));
         }

         var28 = var11.entrySet().iterator();

         while(true) {
            label122:
            while(var28.hasNext()) {
               Map.Entry var30 = (Map.Entry)var28.next();
               Integer var31 = (Integer)var30.getKey();
               if (this.map.getZoneName(var31) == null) {
                  DebugLog.log("Zone " + var31 + " not found in ZONE_MAP");
               } else {
                  Boolean[] var33 = (Boolean[])var30.getValue();
                  int var34 = 0;

                  while(true) {
                     while(true) {
                        if (Arrays.stream(var33).allMatch((var0) -> {
                           return var0;
                        })) {
                           continue label122;
                        }

                        int var35 = 0;
                        var18 = 0;
                        boolean var19 = false;

                        int var20;
                        for(var20 = 0; var20 < IsoCell.CellSizeInChunks; ++var20) {
                           if (!var33[var34 * IsoCell.CellSizeInChunks + var20]) {
                              var35 = var20;
                              var19 = true;
                              break;
                           }
                        }

                        if (!var19) {
                           ++var34;
                        } else {
                           for(var20 = var35; var20 <= IsoCell.CellSizeInChunks; ++var20) {
                              if (var20 == IsoCell.CellSizeInChunks || var33[var34 * IsoCell.CellSizeInChunks + var20]) {
                                 var18 = var20 - 1;
                                 break;
                              }
                           }

                           int var21 = IsoCell.CellSizeInChunks;

                           int var22;
                           int var23;
                           label106:
                           for(var22 = var34 + 1; var22 < IsoCell.CellSizeInChunks; ++var22) {
                              for(var23 = var35; var23 <= var18; ++var23) {
                                 if (var33[var22 * IsoCell.CellSizeInChunks + var23]) {
                                    var21 = var22;
                                    break label106;
                                 }
                              }
                           }

                           IsoWorld.instance.getMetaGrid().registerZone("", this.map.getZoneName(var31), (var35 + var9) * 8, (var34 + var10) * 8, 0, (var18 + 1 - var35) * 8, (var21 - var34) * 8);

                           for(var22 = 0; var22 < IsoCell.CellSizeInChunks; ++var22) {
                              for(var23 = 0; var23 < IsoCell.CellSizeInChunks; ++var23) {
                                 int var25 = var23 * IsoCell.CellSizeInChunks + var22;
                                 var33[var25] = var33[var25] | var3.getChunkData(var22 + var9, var23 + var10).doesHaveZone(this.map.getZoneName(var31));
                              }
                           }

                           var34 = 0;
                        }
                     }
                  }
               }
            }

            return;
         }
      }
   }

   public void genAnimalsPath(int var1, int var2) {
      IsoMetaGrid var3 = IsoWorld.instance.getMetaGrid();
      int var4 = PZMath.fastfloor((float)var1 / (float)IsoCell.CellSizeInChunks);
      int var5 = PZMath.fastfloor((float)var2 / (float)IsoCell.CellSizeInChunks);
      if (var3.hasCellData(var4, var5).equals(MetaCellPresence.NOT_LOADED)) {
         var3.setCellData(var4, var5, new IsoMetaCell(var4, var5));
      }

      IsoMetaCell var6 = var3.getCellData(var4, var5);
      if (var3.hasCellData(var4, var5).equals(MetaCellPresence.LOADED)) {
         var3.addCellToSave(var6);
         if (var6.getAnimalZonesSize() == 0) {
            int var7 = var4 * IsoCell.CellSizeInSquares;
            int var8 = var5 * IsoCell.CellSizeInSquares;
            ArrayList var9 = new ArrayList();

            for(int var10 = 0; var10 < this.animalsPathConfig.size(); ++var10) {
               AnimalsPathConfig var11 = (AnimalsPathConfig)this.animalsPathConfig.get(var10);
               Random var12 = WGParams.instance.getRandom(var4, var5, (long)(var11.getNameHash() + var10));
               byte var13 = 0;
               String var14 = "";
               String var15 = "Animal";
               String var16 = "Follow";
               String var17 = var11.animalType();
               int var18 = var11.points()[0];
               int var19 = var11.points().length > 1 ? var11.points()[1] : var11.points()[0];
               int var20 = var11.radius()[0];
               int var21 = var11.radius().length > 1 ? var11.radius()[1] : var11.radius()[0];
               int var22 = var11.extension()[0];
               int var23 = var11.extension().length > 1 ? var11.extension()[1] : var11.extension()[0];
               if (var18 >= 0 && (var18 != 0 || var19 != 0) && var20 >= 0 && (var20 != 0 || var21 != 0) && var22 >= 0 && (var22 != 0 || var23 != 0)) {
                  label145:
                  for(int var24 = 0; var24 < var11.count(); ++var24) {
                     if (!(var12.nextFloat() > var11.chance())) {
                        int var25 = 2147483647;
                        int var26 = 2147483647;
                        int var27 = -2147483648;
                        int var28 = -2147483648;
                        int var29 = var21 + var12.nextInt(Math.max(IsoCell.CellSizeInSquares - 2 * var21, 1));
                        int var30 = var21 + var12.nextInt(Math.max(IsoCell.CellSizeInSquares - 2 * var21, 1));
                        int var31 = var12.nextInt(var19 - var18 + 1) + var18;
                        int var32 = var12.nextInt(360);
                        ArrayList var33 = new ArrayList();
                        TIntArrayList var34 = new TIntArrayList();
                        int var35 = 0;

                        int var36;
                        int var37;
                        for(var36 = 0; var36 < var31; ++var36) {
                           var37 = var12.nextInt(var21 - var20 + 1) + var20;
                           double var38 = Math.toRadians(360.0 / (double)var31 * (double)var36 + (double)var32);
                           int var40 = (int)Math.min(Math.max((double)var37 * Math.cos(var38) + (double)var29 + (double)var7, (double)(var7 + 1)), (double)(var7 + IsoCell.CellSizeInSquares - 2));
                           int var41 = (int)Math.min(Math.max((double)var37 * Math.sin(var38) + (double)var30 + (double)var8, (double)(var8 + 1)), (double)(var8 + IsoCell.CellSizeInSquares - 2));
                           var34.add(var40);
                           var34.add(var41);
                           var25 = Math.min(var25, var40);
                           var27 = Math.max(var27, var40);
                           var26 = Math.min(var26, var41);
                           var28 = Math.max(var28, var41);
                           if (var12.nextFloat() < var11.extensionChance() || var36 == var31 - 2 && var35 == 0 || var36 == var31 - 1 && var35 == 1) {
                              AnimalZone var42 = this.getExtensionZone(var14, var15, var35 % 2 == 0 ? "Eat" : "Sleep", var13, var40, var41, var12, var22, var23);
                              var33.add(var42);
                              ++var35;
                           }
                        }

                        for(var36 = 0; var36 < var34.size(); var36 += 2) {
                           var37 = var34.get(var36);
                           int var45 = var34.get(var36 + 1);
                           Iterator var39 = var9.iterator();

                           while(var39.hasNext()) {
                              Rectangle var47 = (Rectangle)var39.next();
                              if (var47.contains(var37, var45)) {
                                 continue label145;
                              }
                           }

                           IsoMetaChunk var46 = var6.getChunk((var37 - var7) / 8, (var45 - var8) / 8);
                           if (var46.doesHaveZone("TrailerPark") || var46.doesHaveZone("TownZone") || var46.doesHaveZone("Vegitation") || var46.doesHaveZone("Water") || !var46.doesHaveForaging()) {
                              continue label145;
                           }
                        }

                        var9.add(new Rectangle(var25, var26, var27 - var25, var28 - var26));
                        AnimalZone var43 = new AnimalZone(var14, var15, var25, var26, 0, var27 - var25 + 1, var28 - var26 + 1, var16, var17, true);
                        var43.geometryType = ZoneGeometryType.Polyline;
                        var43.points.addAll(var34);
                        var43.polylineWidth = var13;
                        AnimalZone var44 = this.getLoopingZone(var14, var15, var16, var17, var13, var34);
                        var3.registerAnimalZone(var43);
                        var3.registerAnimalZone(var44);
                        Objects.requireNonNull(var3);
                        var33.forEach(var3::registerAnimalZone);
                     }
                  }
               }
            }

            if (var6.getAnimalZonesSize() != 0) {
               AnimalManagerWorker.getInstance().allocCell(var4, var5);
            }

         }
      }
   }

   private AnimalZone getLoopingZone(String var1, String var2, String var3, String var4, int var5, TIntArrayList var6) {
      TIntArrayList var7 = new TIntArrayList();
      var7.add(var6.get(var6.size() - 2));
      var7.add(var6.get(var6.size() - 1));
      var7.add(var6.get(0));
      var7.add(var6.get(1));
      AnimalZone var8 = new AnimalZone(var1, var2, var7.get(0), var7.get(1), 0, var7.get(2) - var7.get(0) + 1, var7.get(3) - var7.get(1) + 1, var3, var4, false);
      var8.geometryType = ZoneGeometryType.Polyline;
      var8.points.addAll(var7);
      var8.polylineWidth = var5;
      return var8;
   }

   private AnimalZone getExtensionZone(String var1, String var2, String var3, int var4, int var5, int var6, Random var7, int var8, int var9) {
      TIntArrayList var10 = new TIntArrayList();
      int var11 = var7.nextInt(360);
      int var12 = var7.nextInt(var9 - var8 + 1) + var8;
      var10.add(var5);
      var10.add(var6);
      var10.add((int)((double)var12 * Math.cos((double)var11)) + var5);
      var10.add((int)((double)var12 * Math.sin((double)var11)) + var6);
      AnimalZone var13 = new AnimalZone(var1, var2, var10.get(0), var10.get(1), 0, var10.get(2) - var10.get(0) + 1, var10.get(3) - var10.get(1) + 1, var3, (String)null, false);
      var13.geometryType = ZoneGeometryType.Polyline;
      var13.points.addAll(var10);
      var13.polylineWidth = var4;
      return var13;
   }
}
