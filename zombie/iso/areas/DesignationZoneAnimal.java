package zombie.iso.areas;

import java.util.ArrayList;
import zombie.characters.Position3D;
import zombie.characters.animals.IsoAnimal;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.Food;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoFeedingTrough;
import zombie.iso.objects.IsoHutch;
import zombie.iso.objects.IsoWorldInventoryObject;

public final class DesignationZoneAnimal extends DesignationZone {
   public static final ArrayList<DesignationZoneAnimal> designationAnimalZoneList = new ArrayList();
   public static final String ZONE_TYPE = "AnimalZone";
   public static float ZONECOLORR = 0.2F;
   public static float ZONECOLORG = 0.2F;
   public static float ZONECOLORB = 0.9F;
   public static float ZONESELECTEDCOLORR = 0.2F;
   public static float ZONESELECTEDCOLORG = 0.8F;
   public static float ZONESELECTEDCOLORB = 0.9F;
   public ArrayList<IsoAnimal> animals = new ArrayList();
   public ArrayList<IsoFeedingTrough> troughs = new ArrayList();
   public ArrayList<IsoHutch> hutchs = new ArrayList();
   public ArrayList<IsoWorldInventoryObject> foodOnGround = new ArrayList();
   public ArrayList<Position3D> roofAreas = new ArrayList();
   public static final String FENCE_WEST = "fencing_01_20";
   public static final String FENCE_NORTH = "fencing_01_21";
   public static final String FENCE_NORTHCORNER = "fencing_01_18";

   public DesignationZoneAnimal(String var1, int var2, int var3, int var4, int var5, int var6) {
      super("AnimalZone", var1, var2, var3, var4, var5, var6);
      designationAnimalZoneList.add(this);
      this.check();
   }

   public static ArrayList<DesignationZoneAnimal> getAllDZones(ArrayList<DesignationZoneAnimal> var0, DesignationZoneAnimal var1, DesignationZoneAnimal var2) {
      ArrayList var3 = var0;
      if (var0 == null) {
         var3 = new ArrayList();
      }

      ArrayList var4 = new ArrayList();
      DesignationZoneAnimal var5 = var1;
      if (var1 == null) {
         return var3;
      } else {
         if (!var3.contains(var1)) {
            var3.add(var1);
         }

         int var6;
         DesignationZoneAnimal var7;
         for(var6 = var1.x; var6 < var5.x + var5.w; ++var6) {
            var7 = getZone(var6, var5.y - 1, var5.z);
            if (var7 != null && !var3.contains(var7) && var7 != var2) {
               var3.add(var7);
               var4.add(var7);
            }

            var7 = getZone(var6, var5.y + var5.w, var5.z);
            if (var7 != null && !var3.contains(var7) && var7 != var2) {
               var3.add(var7);
               var4.add(var7);
            }
         }

         for(var6 = var5.y; var6 < var5.y + var5.h; ++var6) {
            var7 = getZone(var5.x - 1, var6, var5.z);
            if (var7 != null && !var3.contains(var7) && var7 != var2) {
               var3.add(var7);
               var4.add(var7);
            }

            var7 = getZone(var5.x + var5.w, var6, var5.z);
            if (var7 != null && !var3.contains(var7) && var7 != var2) {
               var3.add(var7);
               var4.add(var7);
            }
         }

         for(var6 = 0; var6 < var4.size(); ++var6) {
            if (var4.get(var6) != var1) {
               var3 = getAllDZones(var3, (DesignationZoneAnimal)var4.get(var6), var5);
            }
         }

         return var3;
      }
   }

   public void createSurroundingFence() {
   }

   public static void addFoodOnGround(IsoWorldInventoryObject var0, IsoGridSquare var1) {
      if (var0.getItem() instanceof Food || var0.getItem() instanceof DrainableComboItem || var0.getItem().isPureWater(true)) {
         DesignationZoneAnimal var2 = getZone(var1.getX(), var1.getY(), var1.getZ());
         if (var2 != null && !var2.foodOnGround.contains(var0)) {
            var2.foodOnGround.add(var0);
         }

      }
   }

   public void check() {
      if (this.isFullyStreamed()) {
         if (IsoWorld.instance.CurrentCell == null) {
            lastUpdate = 0L;
         } else {
            this.animals.clear();
            this.hutchs.clear();
            this.foodOnGround.clear();
            this.roofAreas.clear();

            for(int var1 = this.x; var1 < this.x + this.w; ++var1) {
               for(int var2 = this.y; var2 < this.y + this.h; ++var2) {
                  for(int var3 = 0; var3 < 8; ++var3) {
                     IsoGridSquare var4 = IsoWorld.instance.CurrentCell.getGridSquare(var1, var2, var3);
                     if (var4 != null) {
                        int var5;
                        for(var5 = 0; var5 < var4.getAnimals().size(); ++var5) {
                           IsoAnimal var6 = (IsoAnimal)var4.getAnimals().get(var5);
                           if (!this.animals.contains(var6)) {
                              this.animals.add(var6);
                           }

                           var6.dZone = this;
                           var6.setWild(false);
                        }

                        for(var5 = 0; var5 < var4.getObjects().size(); ++var5) {
                           IsoObject var8 = (IsoObject)var4.getObjects().get(var5);
                           if ((var8.isFloor() || var4.haveRoof || var4.HasSlopedRoof()) && var3 > 0) {
                              this.roofAreas.add(new Position3D(var1, var2, var3));
                           }

                           if (var8 instanceof IsoWorldInventoryObject && (((IsoWorldInventoryObject)var8).getItem() instanceof Food || ((IsoWorldInventoryObject)var8).getItem() instanceof DrainableComboItem || ((IsoWorldInventoryObject)var8).getItem().isPureWater(true))) {
                              this.foodOnGround.add((IsoWorldInventoryObject)var8);
                           }

                           if (var8 instanceof IsoFeedingTrough) {
                              IsoFeedingTrough var7 = (IsoFeedingTrough)var8;
                              if (var7.getLinkedY() == 0 && !this.troughs.contains(var7)) {
                                 this.troughs.add(var7);
                              }
                           }

                           if (var8 instanceof IsoHutch) {
                              IsoHutch var9 = (IsoHutch)var8;
                              if (!var9.isSlave() && !this.hutchs.contains(var9)) {
                                 this.hutchs.add(var9);
                                 var9.reforceUpdate();
                              }
                           }
                        }
                     }
                  }
               }
            }

            this.reAttachAnimal();
         }
      }
   }

   private void reAttachAnimal() {
      int var1;
      int var2;
      if (!this.troughs.isEmpty()) {
         for(var1 = 0; var1 < this.troughs.size(); ++var1) {
            ((IsoFeedingTrough)this.troughs.get(var1)).linkedAnimals.clear();

            for(var2 = 0; var2 < this.animals.size(); ++var2) {
               ((IsoFeedingTrough)this.troughs.get(var1)).addLinkedAnimal((IsoAnimal)this.animals.get(var2));
            }
         }
      }

      if (!this.hutchs.isEmpty()) {
         for(var1 = 0; var1 < this.hutchs.size(); ++var1) {
            ((IsoHutch)this.hutchs.get(var1)).animalOutside.clear();

            for(var2 = 0; var2 < this.animals.size(); ++var2) {
               ((IsoHutch)this.hutchs.get(var1)).animalOutside.add((IsoAnimal)this.animals.get(var2));
            }
         }
      }

   }

   public void doMeta(int var1) {
      this.check();

      int var2;
      IsoAnimal var3;
      for(var2 = 0; var2 < this.animals.size(); ++var2) {
         var3 = (IsoAnimal)this.animals.get(var2);
         if (!var3.isBaby()) {
            ((IsoAnimal)this.animals.get(var2)).updateStatsAway(var1);
         }
      }

      for(var2 = 0; var2 < this.animals.size(); ++var2) {
         var3 = (IsoAnimal)this.animals.get(var2);
         if (var3.isBaby()) {
            ((IsoAnimal)this.animals.get(var2)).updateStatsAway(var1);
         }
      }

      for(var2 = 0; var2 < this.hutchs.size(); ++var2) {
         ((IsoHutch)this.hutchs.get(var2)).doMeta(var1);
      }

      for(var2 = 0; var2 < this.animals.size(); ++var2) {
         var3 = (IsoAnimal)this.animals.get(var2);
         var3.forceWanderNow();
      }

   }

   public static String getType() {
      return "AnimalZone";
   }

   public static ArrayList<DesignationZoneAnimal> getAllZones() {
      return designationAnimalZoneList;
   }

   public static DesignationZoneAnimal getZone(int var0, int var1, int var2) {
      for(int var3 = 0; var3 < designationAnimalZoneList.size(); ++var3) {
         DesignationZoneAnimal var4 = (DesignationZoneAnimal)designationAnimalZoneList.get(var3);
         if (var0 >= var4.x && var0 < var4.x + var4.w && var1 >= var4.y && var1 < var4.y + var4.h && var4.z == var2) {
            return var4;
         }
      }

      return null;
   }

   public static DesignationZoneAnimal getZone(int var0, int var1) {
      for(int var2 = 0; var2 < designationAnimalZoneList.size(); ++var2) {
         DesignationZoneAnimal var3 = (DesignationZoneAnimal)designationAnimalZoneList.get(var2);
         if (var0 >= var3.x && var0 < var3.x + var3.w && var1 >= var3.y && var1 < var3.y + var3.h) {
            return var3;
         }
      }

      return null;
   }

   public static void removeZone(DesignationZoneAnimal var0) {
      ArrayList var1 = getAllDZones((ArrayList)null, var0, (DesignationZoneAnimal)null);

      for(int var2 = 0; var2 < var1.size(); ++var2) {
         designationAnimalZoneList.remove(var1.get(var2));
         allZones.remove(var1.get(var2));
      }

   }

   public static void removeFoodFromGround(IsoWorldInventoryObject var0) {
      DesignationZoneAnimal var1 = getZone(var0.getSquare().getX(), var0.getSquare().getY(), var0.getSquare().getZ());
      if (var1 != null) {
         var1.foodOnGround.remove(var0);
      }

   }

   public void removeAnimal(IsoAnimal var1) {
      this.animals.remove(var1);
   }

   public ArrayList<IsoAnimal> getAnimals() {
      return this.animals;
   }

   public ArrayList<IsoFeedingTrough> getTroughs() {
      return this.troughs;
   }

   public ArrayList<IsoHutch> getHutchs() {
      return this.hutchs;
   }

   public ArrayList<IsoAnimal> getAnimalsConnected() {
      ArrayList var1 = new ArrayList();
      ArrayList var2 = getAllDZones((ArrayList)null, this, (DesignationZoneAnimal)null);

      for(int var3 = 0; var3 < var2.size(); ++var3) {
         for(int var4 = 0; var4 < ((DesignationZoneAnimal)var2.get(var3)).animals.size(); ++var4) {
            if (!((IsoAnimal)((DesignationZoneAnimal)var2.get(var3)).animals.get(var4)).isOnHook() && !var1.contains(((DesignationZoneAnimal)var2.get(var3)).animals.get(var4))) {
               var1.add((IsoAnimal)((DesignationZoneAnimal)var2.get(var3)).animals.get(var4));
            }
         }
      }

      return var1;
   }

   public ArrayList<IsoFeedingTrough> getTroughsConnected() {
      ArrayList var1 = new ArrayList();
      ArrayList var2 = getAllDZones((ArrayList)null, this, (DesignationZoneAnimal)null);

      for(int var3 = 0; var3 < var2.size(); ++var3) {
         var1.addAll(((DesignationZoneAnimal)var2.get(var3)).troughs);
      }

      return var1;
   }

   public ArrayList<IsoHutch> getHutchsConnected() {
      ArrayList var1 = new ArrayList();
      ArrayList var2 = getAllDZones((ArrayList)null, this, (DesignationZoneAnimal)null);

      for(int var3 = 0; var3 < var2.size(); ++var3) {
         var1.addAll(((DesignationZoneAnimal)var2.get(var3)).hutchs);
      }

      return var1;
   }

   public ArrayList<IsoWorldInventoryObject> getFoodOnGround() {
      return this.foodOnGround;
   }

   public ArrayList<IsoWorldInventoryObject> getFoodOnGroundConnected() {
      ArrayList var1 = new ArrayList();
      ArrayList var2 = getAllDZones((ArrayList)null, this, (DesignationZoneAnimal)null);

      for(int var3 = 0; var3 < var2.size(); ++var3) {
         var1.addAll(((DesignationZoneAnimal)var2.get(var3)).foodOnGround);
      }

      return var1;
   }

   public int getFullZoneSize() {
      int var1 = 0;
      ArrayList var2 = getAllDZones((ArrayList)null, this, (DesignationZoneAnimal)null);

      for(int var3 = 0; var3 < var2.size(); ++var3) {
         DesignationZoneAnimal var4 = (DesignationZoneAnimal)var2.get(var3);
         var1 += var4.w * var4.h;
      }

      return var1;
   }

   public static void addNewRoof(int var0, int var1, int var2) {
      DesignationZoneAnimal var3 = getZone(var0, var1);
      if (var3 != null && var2 > var3.z) {
         var3.roofAreas.add(new Position3D(var0, var1, var2));
      }
   }

   public ArrayList<Position3D> getRoofAreas() {
      return this.roofAreas;
   }

   public ArrayList<Position3D> getRoofAreasConnected() {
      ArrayList var1 = new ArrayList();
      ArrayList var2 = getAllDZones((ArrayList)null, this, (DesignationZoneAnimal)null);

      for(int var3 = 0; var3 < var2.size(); ++var3) {
         var1.addAll(((DesignationZoneAnimal)var2.get(var3)).roofAreas);
      }

      return var1;
   }

   public static void Reset() {
      designationAnimalZoneList.clear();
   }
}
