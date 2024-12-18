package zombie.characters.animals;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Map;
import org.joml.Vector2f;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameWindow;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.iso.IsoUtils;
import zombie.iso.zones.Zone;
import zombie.util.SharedStrings;

public final class AnimalZone extends Zone {
   String m_animalType = null;
   public String m_action = null;
   public ArrayList<AnimalZoneJunction> m_junctions;
   boolean m_bSpawnedAnimals = false;
   boolean m_spawnAnimal = true;

   public AnimalZone() {
   }

   public AnimalZone(String var1, String var2, int var3, int var4, int var5, int var6, int var7, KahluaTable var8) {
      super(var1, var2, var3, var4, var5, var6, var7);
      if (var8 != null) {
         Object var9 = var8.rawget("Action");
         if (var9 instanceof String) {
            this.m_action = (String)var9;
         }

         var9 = var8.rawget("AnimalType");
         if (var9 instanceof String) {
            this.m_animalType = (String)var9;
         }

         var9 = var8.rawget("SpawnAnimals");
         if (var9 instanceof Boolean) {
            this.m_spawnAnimal = (Boolean)var9;
         }
      }

   }

   public AnimalZone(String var1, String var2, int var3, int var4, int var5, int var6, int var7, String var8, String var9, boolean var10) {
      super(var1, var2, var3, var4, var5, var6, var7);
      this.m_action = var8;
      this.m_animalType = var9;
      this.m_spawnAnimal = var10;
   }

   public void save(ByteBuffer var1) {
      super.save(var1);
      GameWindow.WriteStringUTF(var1, this.m_action);
      GameWindow.WriteStringUTF(var1, this.m_animalType);
      var1.put((byte)(this.m_spawnAnimal ? 1 : 0));
      var1.put((byte)(this.m_bSpawnedAnimals ? 1 : 0));
   }

   public void save(ByteBuffer var1, Map<String, Integer> var2) {
      super.save(var1, var2);
      GameWindow.WriteStringUTF(var1, this.m_action);
      GameWindow.WriteStringUTF(var1, this.m_animalType);
      var1.put((byte)(this.m_spawnAnimal ? 1 : 0));
      var1.put((byte)(this.m_bSpawnedAnimals ? 1 : 0));
   }

   public AnimalZone load(ByteBuffer var1, int var2, Map<Integer, String> var3, SharedStrings var4) {
      super.load(var1, var2, var3, var4);
      this.m_action = GameWindow.ReadStringUTF(var1);
      this.m_animalType = GameWindow.ReadStringUTF(var1);
      this.m_spawnAnimal = var1.get() == 1;
      this.m_bSpawnedAnimals = var1.get() == 1;
      return this;
   }

   public AnimalZone load(ByteBuffer var1, int var2) {
      super.load(var1, var2);
      this.m_action = GameWindow.ReadStringUTF(var1);
      this.m_animalType = GameWindow.ReadStringUTF(var1);
      this.m_spawnAnimal = var1.get() == 1;
      this.m_bSpawnedAnimals = var1.get() == 1;
      return this;
   }

   public void Dispose() {
      super.Dispose();
      this.m_animalType = null;
      this.m_action = null;
      this.m_junctions = null;
      this.m_bSpawnedAnimals = false;
      this.m_spawnAnimal = true;
   }

   public int getIndexOfPoint(int var1, int var2) {
      for(int var3 = 0; var3 < this.points.size(); var3 += 2) {
         int var4 = this.points.get(var3);
         int var5 = this.points.get(var3 + 1);
         if (var4 == var1 && var5 == var2) {
            return var3 / 2;
         }
      }

      return -1;
   }

   public String getAction() {
      return this.m_action;
   }

   public void addJunctionsWithOtherZone(AnimalZone var1) {
      for(int var2 = 0; var2 < this.points.size(); var2 += 2) {
         int var3 = this.points.get(var2);
         int var4 = this.points.get(var2 + 1);
         int var5 = var1.getIndexOfPoint(var3, var4);
         if (var5 != -1) {
            this.addJunction(var2 / 2, var1, var5);
            var1.addJunction(var5, this, var2 / 2);
         }
      }

   }

   boolean hasJunction(int var1, AnimalZone var2, int var3) {
      if (this.m_junctions == null) {
         return false;
      } else {
         for(int var4 = 0; var4 < this.m_junctions.size(); ++var4) {
            AnimalZoneJunction var5 = (AnimalZoneJunction)this.m_junctions.get(var4);
            if (var5.m_pointIndexSelf == var1 && var5.m_zoneOther == var2 && var5.m_pointIndexOther == var3) {
               return true;
            }
         }

         return false;
      }
   }

   public void addJunction(int var1, AnimalZone var2, int var3) {
      if (!this.hasJunction(var1, var2, var3)) {
         if (var1 >= 0 && var1 < this.points.size() / 2) {
            if (var2 != null && var2 != this && var3 >= 0 && var3 < var2.points.size() / 2) {
               if (this.m_junctions == null) {
                  this.m_junctions = new ArrayList();
               }

               this.m_junctions.add(new AnimalZoneJunction(this, var1, var2, var3));
            }
         }
      }
   }

   public void addJunction(AnimalZoneJunction var1) {
      if (this.m_junctions == null) {
         this.m_junctions = new ArrayList();
      }

      if (!this.m_junctions.contains(var1)) {
         this.m_junctions.add(var1);
      }
   }

   public void getJunctionsBetween(float var1, float var2, ArrayList<AnimalZoneJunction> var3) {
      var3.clear();
      float var4 = this.getPolylineLength();

      for(int var5 = 0; var5 < this.m_junctions.size(); ++var5) {
         AnimalZoneJunction var6 = (AnimalZoneJunction)this.m_junctions.get(var5);
         if (var6.m_distanceFromStart / var4 >= var1 && var6.m_distanceFromStart / var4 <= var2) {
            var3.add(var6);
         }
      }

   }

   public float getClosedPolylineLength() {
      if (this.isPolyline() && this.points.size() >= 6) {
         float var1 = 0.0F;

         for(int var2 = 0; var2 < this.points.size(); var2 += 2) {
            float var3 = (float)this.points.get(var2) + 0.5F;
            float var4 = (float)this.points.get(var2 + 1) + 0.5F;
            float var5 = (float)this.points.get((var2 + 2) % this.points.size()) + 0.5F;
            float var6 = (float)this.points.get((var2 + 3) % this.points.size()) + 0.5F;
            var1 += Vector2f.length(var5 - var3, var6 - var4);
         }

         return var1;
      } else {
         return 0.0F;
      }
   }

   int getPolylineSegment(float var1) {
      var1 = PZMath.clampFloat(var1, 0.0F, 1.0F);
      float var2 = this.getPolylineLength();
      if (var2 <= 0.0F) {
         return -1;
      } else {
         float var3 = var2 * var1;
         float var4 = 0.0F;

         for(int var5 = 0; var5 < this.points.size() - 2; var5 += 2) {
            float var6 = (float)this.points.get(var5) + 0.5F;
            float var7 = (float)this.points.get(var5 + 1) + 0.5F;
            float var8 = (float)this.points.get((var5 + 2) % this.points.size()) + 0.5F;
            float var9 = (float)this.points.get((var5 + 3) % this.points.size()) + 0.5F;
            float var10 = Vector2f.length(var8 - var6, var9 - var7);
            if (var4 + var10 >= var3) {
               return var5;
            }

            var4 += var10;
         }

         return -1;
      }
   }

   public boolean getPointOnPolyline(float var1, Vector2f var2) {
      var1 = PZMath.clampFloat(var1, 0.0F, 1.0F);
      var2.set(0.0F);
      float var3 = this.getPolylineLength();
      if (var3 <= 0.0F) {
         return false;
      } else {
         float var4 = var3 * var1;
         float var5 = 0.0F;

         for(int var6 = 0; var6 < this.points.size() - 2; var6 += 2) {
            float var7 = (float)this.points.get(var6) + 0.5F;
            float var8 = (float)this.points.get(var6 + 1) + 0.5F;
            float var9 = (float)this.points.get((var6 + 2) % this.points.size()) + 0.5F;
            float var10 = (float)this.points.get((var6 + 3) % this.points.size()) + 0.5F;
            float var11 = Vector2f.length(var9 - var7, var10 - var8);
            if (var5 + var11 >= var4) {
               float var12 = (var4 - var5) / var11;
               var2.set(var7 + (var9 - var7) * var12, var8 + (var10 - var8) * var12);
               return true;
            }

            var5 += var11;
         }

         return false;
      }
   }

   boolean pickRandomPointOnPolyline(Vector2f var1) {
      return this.getPointOnPolyline(Rand.Next(0.0F, 1.0F), var1);
   }

   public float getClosestPointOnPolyline(float var1, float var2, Vector2f var3) {
      if (!this.isPolyline() && this.points.size() < 6) {
         return -1.0F;
      } else {
         float var4 = 3.4028235E38F;
         float var5 = 0.0F;
         float var6 = 0.0F;

         for(int var7 = 0; var7 < this.points.size() - 2; var7 += 2) {
            float var8 = (float)this.points.get(var7) + 0.5F;
            float var9 = (float)this.points.get(var7 + 1) + 0.5F;
            float var10 = (float)this.points.get((var7 + 2) % this.points.size()) + 0.5F;
            float var11 = (float)this.points.get((var7 + 3) % this.points.size()) + 0.5F;
            float var12 = Vector2f.distance(var8, var9, var10, var11);
            double var13 = (double)((var1 - var8) * (var10 - var8) + (var2 - var9) * (var11 - var9)) / (Math.pow((double)(var10 - var8), 2.0) + Math.pow((double)(var11 - var9), 2.0));
            double var15 = (double)var8 + var13 * (double)(var10 - var8);
            double var17 = (double)var9 + var13 * (double)(var11 - var9);
            if (var13 <= 0.0) {
               var15 = (double)var8;
               var17 = (double)var9;
               var13 = 0.0;
            } else if (var13 >= 1.0) {
               var15 = (double)var10;
               var17 = (double)var11;
               var13 = 1.0;
            }

            float var19 = IsoUtils.DistanceToSquared(var1, var2, (float)var15, (float)var17);
            if (var19 < var4) {
               var4 = var19;
               var3.set(var15, var17);
               var5 = var6 + (float)(var13 * (double)var12);
            }

            var6 += var12;
         }

         return var5 / var6;
      }
   }

   public float getDistanceOfPointFromStart(int var1) {
      float var2 = 0.0F;

      for(int var3 = 0; var3 < var1 * 2; var3 += 2) {
         float var4 = (float)this.points.get(var3) + 0.5F;
         float var5 = (float)this.points.get(var3 + 1) + 0.5F;
         float var6 = (float)this.points.get((var3 + 2) % this.points.size()) + 0.5F;
         float var7 = (float)this.points.get((var3 + 3) % this.points.size()) + 0.5F;
         var2 += Vector2f.length(var6 - var4, var7 - var5);
      }

      return var2;
   }

   public boolean getDirectionOnPolyline(float var1, Vector2f var2) {
      int var3 = this.getPolylineSegment(var1);
      if (var3 == -1) {
         return false;
      } else {
         float var4 = (float)this.points.get(var3) + 0.5F;
         float var5 = (float)this.points.get(var3 + 1) + 0.5F;
         float var6 = (float)this.points.get((var3 + 2) % this.points.size()) + 0.5F;
         float var7 = (float)this.points.get((var3 + 3) % this.points.size()) + 0.5F;
         var2.set(var6 - var4, var7 - var5).normalize();
         return true;
      }
   }

   public String toString() {
      StringBuilder var1 = new StringBuilder(super.toString());
      var1.append("{");
      var1.append("action='").append(this.m_action).append('\'');
      var1.append(", animal_type=").append(this.m_animalType).append('\'');
      var1.append("}");
      return var1.toString();
   }
}
