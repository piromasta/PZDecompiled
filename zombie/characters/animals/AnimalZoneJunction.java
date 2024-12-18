package zombie.characters.animals;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.UUID;
import zombie.iso.IsoWorld;

public final class AnimalZoneJunction {
   public final AnimalZone m_zoneSelf;
   public final int m_pointIndexSelf;
   public final AnimalZone m_zoneOther;
   public final int m_pointIndexOther;
   public final float m_distanceFromStart;

   public AnimalZoneJunction(AnimalZone var1, int var2, AnimalZone var3, int var4) {
      this.m_zoneSelf = var1;
      this.m_pointIndexSelf = var2;
      this.m_zoneOther = var3;
      this.m_pointIndexOther = var4;
      this.m_distanceFromStart = var1.getDistanceOfPointFromStart(var2);
   }

   public void save(ByteBuffer var1) {
      var1.putInt(this.m_pointIndexSelf);
      var1.putInt(this.m_pointIndexOther);
      var1.putLong(this.m_zoneSelf.id.getMostSignificantBits());
      var1.putLong(this.m_zoneSelf.id.getLeastSignificantBits());
      var1.putLong(this.m_zoneOther.id.getMostSignificantBits());
      var1.putLong(this.m_zoneOther.id.getLeastSignificantBits());
   }

   public static AnimalZoneJunction load(ByteBuffer var0, int var1) {
      int var2 = var0.getInt();
      int var3 = var0.getInt();
      UUID var4 = new UUID(var0.getLong(), var0.getLong());
      UUID var5 = new UUID(var0.getLong(), var0.getLong());
      AnimalZone var6 = (AnimalZone)IsoWorld.instance.MetaGrid.animalZoneHandler.getZone(var4);
      AnimalZone var7 = (AnimalZone)IsoWorld.instance.MetaGrid.animalZoneHandler.getZone(var5);
      return new AnimalZoneJunction(var6, var2, var7, var3);
   }

   public int getX() {
      return this.m_zoneSelf.points.get(this.m_pointIndexSelf * 2);
   }

   public int getY() {
      return this.m_zoneSelf.points.get(this.m_pointIndexSelf * 2 + 1);
   }

   public void getJunctionsAtSamePoint(ArrayList<AnimalZoneJunction> var1) {
      var1.clear();

      for(int var2 = 0; var2 < this.m_zoneSelf.m_junctions.size(); ++var2) {
         AnimalZoneJunction var3 = (AnimalZoneJunction)this.m_zoneSelf.m_junctions.get(var2);
         if (var3.m_pointIndexSelf == this.m_pointIndexSelf) {
            var1.add(var3);
         }
      }

   }

   public boolean isFirstPointOnZone1() {
      return this.m_pointIndexSelf == 0;
   }

   public boolean isLastPointOnZone1() {
      return this.m_pointIndexSelf == this.m_zoneSelf.points.size() / 2 - 1;
   }

   public boolean isFirstPointOnZone2() {
      return this.m_pointIndexOther == 0;
   }

   public boolean isLastPointOnZone2() {
      return this.m_pointIndexOther == this.m_zoneOther.points.size() / 2 - 1;
   }
}
