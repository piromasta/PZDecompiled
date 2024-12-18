package zombie.characters.animals;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.UUID;
import org.joml.Vector2f;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.core.random.Rand;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;

public final class VirtualAnimal {
   float m_x;
   float m_y;
   float m_z;
   final Vector2f m_forwardDirection = new Vector2f();
   final ArrayList<IsoAnimal> m_animals = new ArrayList();
   VirtualAnimalState m_state;
   boolean m_bMoveForwardOnZone = Rand.NextBool(2);
   String m_currentZoneAction = "Follow";
   public double m_nextRestTime = -1.0;
   public double m_nextEatTime = -1.0;
   public double id = 0.0;
   public String migrationGroup;
   public float speed = 1.0F;
   public int timeToEat = 0;
   public int timeToSleep = 0;
   public int trackChance = 200;
   public int poopChance = 200;
   public int brokenTwigsChance = 200;
   public int herbGrazeChance = 200;
   public int furChance = 200;
   public int flatHerbChance = 200;
   public double m_wakeTime = 0.0;
   public double m_eatStartTime = 0.0;
   public ArrayList<Integer> sleepPeriodStart = new ArrayList();
   public ArrayList<Integer> sleepPeriodEnd = new ArrayList();
   public ArrayList<Integer> eatPeriodStart = new ArrayList();
   public ArrayList<Integer> eatPeriodEnd = new ArrayList();
   public boolean debugForceSleep = false;
   public boolean debugForceEat = false;
   public AnimalZone zone = null;
   private boolean m_bRemoved = false;

   public VirtualAnimal() {
      this.id = (double)Rand.Next(9999999) + 100000.0;
   }

   public float getX() {
      return this.m_x;
   }

   public void setX(float var1) {
      this.m_x = var1;
   }

   public float getY() {
      return this.m_y;
   }

   public void setY(float var1) {
      this.m_y = var1;
   }

   public float getZ() {
      return this.m_z;
   }

   public void setZ(float var1) {
      this.m_z = var1;
   }

   public void setState(VirtualAnimalState var1) {
      this.m_state = var1;
   }

   public VirtualAnimalState getState() {
      return this.m_state;
   }

   void update() {
      if (MigrationGroupDefinitions.getMigrationDefs().get(this.migrationGroup) != null) {
         if (this.m_state == null) {
            this.m_state = new VirtualAnimalState.StateFollow(this);
            AnimalZone var1 = AnimalZones.getInstance().getClosestZone(this.m_x, this.m_y, (String)null);
            if (var1 != null) {
               this.m_currentZoneAction = var1.getAction();
            }
         }

         this.m_state.update();
      }
   }

   void save(ByteBuffer var1) throws IOException {
      var1.putDouble(this.id);
      if (this.zone != null) {
         var1.putLong(this.zone.id.getMostSignificantBits());
         var1.putLong(this.zone.id.getLeastSignificantBits());
      } else {
         var1.putLong(0L);
         var1.putLong(0L);
      }

      var1.putFloat(this.m_x);
      var1.putFloat(this.m_y);
      var1.putFloat(this.m_z);
      var1.putFloat(this.m_forwardDirection.x);
      var1.putFloat(this.m_forwardDirection.y);
      GameWindow.WriteString(var1, this.migrationGroup);
      var1.putDouble(this.m_nextEatTime);
      var1.putDouble(this.m_nextRestTime);
      var1.putShort((short)this.m_animals.size());

      for(int var2 = 0; var2 < this.m_animals.size(); ++var2) {
         IsoAnimal var3 = (IsoAnimal)this.m_animals.get(var2);
         var3.save(var1, false);
      }

   }

   void load(ByteBuffer var1, int var2) throws IOException {
      this.id = var1.getDouble();
      UUID var3 = new UUID(var1.getLong(), var1.getLong());
      this.zone = var3.getMostSignificantBits() == 0L && var3.getLeastSignificantBits() == 0L ? null : (AnimalZone)IsoWorld.instance.MetaGrid.animalZoneHandler.getZone(var3);
      this.m_x = var1.getFloat();
      this.m_y = var1.getFloat();
      this.m_z = var1.getFloat();
      this.m_forwardDirection.x = var1.getFloat();
      this.m_forwardDirection.y = var1.getFloat();
      this.migrationGroup = GameWindow.ReadString(var1);
      this.m_nextEatTime = var1.getDouble();
      this.m_nextRestTime = var1.getDouble();
      short var4 = var1.getShort();

      for(int var5 = 0; var5 < var4; ++var5) {
         IsoObject var6 = IsoObject.factoryFromFileInput(IsoWorld.instance.CurrentCell, var1);
         if (!(var6 instanceof IsoAnimal)) {
            throw new RuntimeException("expected IsoAnimal here");
         }

         IsoAnimal var7 = (IsoAnimal)var6;
         var7.load(var1, var2);
         IsoWorld.instance.CurrentCell.getAddList().remove(var7);
         IsoWorld.instance.CurrentCell.getObjectList().remove(var7);
         this.m_animals.add(var7);
      }

   }

   public void forceRest() {
      this.debugForceSleep = true;
   }

   public void forceEat() {
      this.debugForceEat = true;
   }

   public void forceWakeUp() {
      this.m_wakeTime = 0.0;
   }

   public void forceStopEat() {
      this.m_eatStartTime = -100000.0;
   }

   public boolean isEating() {
      return this.m_state instanceof VirtualAnimalState.StateEat;
   }

   public boolean isSleeping() {
      return this.m_state instanceof VirtualAnimalState.StateSleep;
   }

   public boolean isTimeToSleep() {
      if (this.debugForceSleep) {
         return true;
      } else if (!this.sleepPeriodStart.isEmpty() && !this.sleepPeriodEnd.isEmpty() && this.sleepPeriodStart.size() == this.sleepPeriodEnd.size()) {
         for(int var1 = 0; var1 < this.sleepPeriodStart.size(); ++var1) {
            int var2 = GameTime.getInstance().getHour();
            int var3 = (Integer)this.sleepPeriodStart.get(var1);
            int var4 = (Integer)this.sleepPeriodEnd.get(var1);
            if (var3 < var4) {
               if (var2 >= var3 && var2 < var4) {
                  return true;
               }

               if (var2 >= var4 && var2 < var3) {
                  return true;
               }
            }
         }

         return false;
      } else {
         return GameTime.getInstance().getWorldAgeHours() > this.m_nextRestTime;
      }
   }

   public boolean isTimeToEat() {
      if (this.debugForceEat) {
         return true;
      } else if (!this.eatPeriodStart.isEmpty() && !this.eatPeriodEnd.isEmpty() && this.eatPeriodStart.size() == this.eatPeriodEnd.size()) {
         for(int var1 = 0; var1 < this.eatPeriodStart.size(); ++var1) {
            int var2 = GameTime.getInstance().getHour();
            int var3 = (Integer)this.eatPeriodStart.get(var1);
            int var4 = (Integer)this.eatPeriodEnd.get(var1);
            if (var3 < var4) {
               if (var2 >= var3 && var2 < var4) {
                  return true;
               }

               if (var2 >= var4 && var2 < var3) {
                  return true;
               }
            }
         }

         return false;
      } else {
         return GameTime.getInstance().getWorldAgeHours() > this.m_nextEatTime;
      }
   }

   public String getNextSleepPeriod() {
      int var1 = 100000;
      if (this.isTimeToSleep()) {
         return this.getEndSleepPeriod();
      } else {
         int var10001;
         if (!this.sleepPeriodStart.isEmpty() && !this.sleepPeriodEnd.isEmpty() && this.sleepPeriodStart.size() == this.sleepPeriodEnd.size()) {
            for(int var2 = 0; var2 < this.sleepPeriodStart.size(); ++var2) {
               int var3 = GameTime.getInstance().getHour();
               int var4 = (Integer)this.sleepPeriodStart.get(var2);
               if (var4 < var3) {
                  var4 += 24;
               }

               if (var4 - var3 < var1) {
                  var1 = var4 - var3;
               }
            }

            int var10000 = var1 * 60;
            var10001 = GameTime.getInstance().getHour();
            return "in " + (var10000 - (var10001 + GameTime.getInstance().getMinutes())) + " mins";
         } else {
            var10001 = (int)Math.floor((this.m_nextRestTime - GameTime.getInstance().getWorldAgeHours()) * 60.0);
            return "in " + Math.max(0, var10001 + 1) + " mins";
         }
      }
   }

   public String getEndSleepPeriod() {
      int var1 = 100000;
      if (this.debugForceSleep) {
         return "now";
      } else if (!this.sleepPeriodStart.isEmpty() && !this.sleepPeriodEnd.isEmpty() && this.sleepPeriodStart.size() == this.sleepPeriodEnd.size()) {
         for(int var2 = 0; var2 < this.sleepPeriodStart.size(); ++var2) {
            int var3 = GameTime.getInstance().getHour();
            int var4 = (Integer)this.sleepPeriodEnd.get(var2);
            if (var4 > var3 && var4 - var3 < var1) {
               var1 = var4 - var3;
            }
         }

         int var10000 = var1 * 60;
         int var10001 = GameTime.getInstance().getHour();
         return "now, ends in " + (var10000 - (var10001 + GameTime.getInstance().getMinutes())) + " mins";
      } else {
         return "now";
      }
   }

   public String getEndEatPeriod() {
      int var1 = 100000;
      if (this.debugForceEat) {
         return "now";
      } else if (!this.eatPeriodStart.isEmpty() && !this.eatPeriodEnd.isEmpty() && this.eatPeriodStart.size() == this.eatPeriodEnd.size()) {
         for(int var2 = 0; var2 < this.eatPeriodStart.size(); ++var2) {
            int var3 = GameTime.getInstance().getHour();
            int var4 = (Integer)this.eatPeriodEnd.get(var2);
            if (var4 > var3 && var4 - var3 < var1) {
               var1 = var4 - var3;
            }
         }

         int var10000 = var1 * 60;
         int var10001 = GameTime.getInstance().getHour();
         return "now, ends in " + (var10000 - (var10001 + GameTime.getInstance().getMinutes())) + " mins";
      } else {
         return "now";
      }
   }

   public String getNextEatPeriod() {
      int var1 = 100000;
      if (this.isTimeToEat()) {
         return this.getEndEatPeriod();
      } else {
         int var10001;
         if (!this.eatPeriodStart.isEmpty() && !this.eatPeriodEnd.isEmpty() && this.eatPeriodStart.size() == this.eatPeriodEnd.size()) {
            for(int var2 = 0; var2 < this.eatPeriodStart.size(); ++var2) {
               int var3 = GameTime.getInstance().getHour();
               int var4 = (Integer)this.eatPeriodStart.get(var2);
               if (var4 < var3) {
                  var4 += 24;
               }

               if (var4 - var3 < var1) {
                  var1 = var4 - var3;
               }
            }

            int var10000 = var1 * 60;
            var10001 = GameTime.getInstance().getHour();
            return "in " + (var10000 - (var10001 + GameTime.getInstance().getMinutes())) + " mins";
         } else {
            var10001 = (int)Math.floor((this.m_nextEatTime - GameTime.getInstance().getWorldAgeHours()) * 60.0);
            return "in " + Math.max(0, var10001 + 1) + " mins";
         }
      }
   }

   public void setRemoved(boolean var1) {
      this.m_bRemoved = var1;
   }

   public boolean isRemoved() {
      return this.m_bRemoved;
   }

   public IsoAnimal findAnimalById(int var1) {
      for(int var2 = 0; var2 < this.m_animals.size(); ++var2) {
         IsoAnimal var3 = (IsoAnimal)this.m_animals.get(var2);
         if (var3.getAnimalID() == var1) {
            return var3;
         }
      }

      return null;
   }
}
