package zombie.iso;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Set;
import zombie.Lua.LuaEventManager;
import zombie.characters.IsoGameCharacter;
import zombie.characters.animals.AnimalZone;
import zombie.core.math.PZMath;
import zombie.iso.objects.IsoMannequin;
import zombie.iso.worldgen.zones.WorldGenZone;
import zombie.iso.zones.RoomTone;
import zombie.iso.zones.Trigger;
import zombie.iso.zones.VehicleZone;
import zombie.iso.zones.Zone;

public final class IsoMetaCell {
   public final ArrayList<VehicleZone> vehicleZones = new ArrayList();
   private final IsoMetaChunk[] ChunkMap;
   public LotHeader info;
   public final ArrayList<Trigger> triggers;
   private int wx;
   private int wy;
   private final ArrayList<AnimalZone> animalZones;
   private boolean bAnimalZonesGenerated;
   public final ArrayList<IsoMannequin.MannequinZone> mannequinZones;
   public ArrayList<WorldGenZone> worldGenZones;
   public final ArrayList<RoomTone> roomTones;

   public IsoMetaCell(int var1, int var2) {
      this.ChunkMap = new IsoMetaChunk[IsoCell.CellSizeInChunks * IsoCell.CellSizeInChunks];
      this.info = null;
      this.triggers = new ArrayList();
      this.wx = 0;
      this.wy = 0;
      this.animalZones = new ArrayList();
      this.bAnimalZonesGenerated = false;
      this.mannequinZones = new ArrayList();
      this.worldGenZones = null;
      this.roomTones = new ArrayList();
      this.wx = var1;
      this.wy = var2;
   }

   public int getX() {
      return this.wx;
   }

   public int getY() {
      return this.wy;
   }

   public void addTrigger(BuildingDef var1, int var2, int var3, String var4) {
      this.triggers.add(new Trigger(var1, var2, var3, var4));
   }

   public void checkTriggers() {
      IsoGameCharacter var1 = IsoCamera.getCameraCharacter();
      if (var1 != null) {
         int var2 = PZMath.fastfloor(var1.getX());
         int var3 = PZMath.fastfloor(var1.getY());

         for(int var4 = 0; var4 < this.triggers.size(); ++var4) {
            Trigger var5 = (Trigger)this.triggers.get(var4);
            if (var2 >= var5.def.x - var5.triggerRange && var2 <= var5.def.x2 + var5.triggerRange && var3 >= var5.def.y - var5.triggerRange && var3 <= var5.def.y2 + var5.triggerRange) {
               if (!var5.triggered) {
                  LuaEventManager.triggerEvent("OnTriggerNPCEvent", var5.type, var5.data, var5.def);
               }

               LuaEventManager.triggerEvent("OnMultiTriggerNPCEvent", var5.type, var5.data, var5.def);
               var5.triggered = true;
            }
         }

      }
   }

   public IsoMetaChunk getChunk(int var1, int var2) {
      return var2 < IsoCell.CellSizeInChunks && var1 < IsoCell.CellSizeInChunks && var1 >= 0 && var2 >= 0 ? this.getChunk(var2 * IsoCell.CellSizeInChunks + var1) : null;
   }

   public IsoMetaChunk getChunk(int var1) {
      if (!this.hasChunk(var1)) {
         this.ChunkMap[var1] = new IsoMetaChunk();
         int var2 = var1 % IsoCell.CellSizeInChunks;
         int var3 = var1 / IsoCell.CellSizeInChunks;
         int var4 = LotHeader.getZombieIntensityForChunk(this.info, var2, var3);
         this.ChunkMap[var1].setZombieIntensity((byte)(var4 >= 0 ? var4 : 0));
      }

      return this.ChunkMap[var1];
   }

   public boolean hasChunk(int var1, int var2) {
      return var2 < IsoCell.CellSizeInChunks && var1 < IsoCell.CellSizeInChunks && var1 >= 0 && var2 >= 0 ? this.hasChunk(var2 * IsoCell.CellSizeInChunks + var1) : false;
   }

   public boolean hasChunk(int var1) {
      return this.ChunkMap[var1] != null;
   }

   public void clearChunk(int var1) {
      if (this.ChunkMap[var1].getRoomsSize() == 0 && this.ChunkMap[var1].getZonesSize() == 0) {
         this.getChunk(var1).Dispose();
         this.ChunkMap[var1] = null;
      }

   }

   public void addZone(Zone var1, int var2, int var3) {
      int var4 = var1.x / 8;
      int var5 = var1.y / 8;
      int var6 = (var1.x + var1.w) / 8;
      if ((var1.x + var1.w) % 8 == 0) {
         --var6;
      }

      int var7 = (var1.y + var1.h) / 8;
      if ((var1.y + var1.h) % 8 == 0) {
         --var7;
      }

      var4 = PZMath.clamp(var4, var2 / 8, (var2 + IsoCell.CellSizeInSquares) / 8);
      var5 = PZMath.clamp(var5, var3 / 8, (var3 + IsoCell.CellSizeInSquares) / 8);
      var6 = PZMath.clamp(var6, var2 / 8, (var2 + IsoCell.CellSizeInSquares) / 8 - 1);
      var7 = PZMath.clamp(var7, var3 / 8, (var3 + IsoCell.CellSizeInSquares) / 8 - 1);

      for(int var8 = var5; var8 <= var7; ++var8) {
         for(int var9 = var4; var9 <= var6; ++var9) {
            if (var1.intersects(var9 * 8, var8 * 8, var1.z, 8, 8)) {
               int var10 = var9 - var2 / 8 + (var8 - var3 / 8) * IsoCell.CellSizeInChunks;
               this.getChunk(var10).addZone(var1);
            }
         }
      }

   }

   public void removeZone(Zone var1) {
      int var2 = (var1.x + var1.w) / 8;
      if ((var1.x + var1.w) % 8 == 0) {
         --var2;
      }

      int var3 = (var1.y + var1.h) / 8;
      if ((var1.y + var1.h) % 8 == 0) {
         --var3;
      }

      int var4 = this.wx * IsoCell.CellSizeInSquares;
      int var5 = this.wy * IsoCell.CellSizeInSquares;

      for(int var6 = var1.y / 8; var6 <= var3; ++var6) {
         for(int var7 = var1.x / 8; var7 <= var2; ++var7) {
            if (var7 >= var4 / 8 && var7 < (var4 + IsoCell.CellSizeInSquares) / 8 && var6 >= var5 / 8 && var6 < (var5 + IsoCell.CellSizeInSquares) / 8) {
               int var8 = var7 - var4 / 8 + (var6 - var5 / 8) * IsoCell.CellSizeInChunks;
               if (this.hasChunk(var8)) {
                  this.getChunk(var8).removeZone(var1);
                  this.clearChunk(var8);
               }
            }
         }
      }

   }

   public void addRoom(RoomDef var1, int var2, int var3) {
      int var4 = var1.x2 / 8;
      if (var1.x2 % 8 == 0) {
         --var4;
      }

      int var5 = var1.y2 / 8;
      if (var1.y2 % 8 == 0) {
         --var5;
      }

      for(int var6 = var1.y / 8; var6 <= var5; ++var6) {
         for(int var7 = var1.x / 8; var7 <= var4; ++var7) {
            if (var7 >= var2 / 8 && var7 < (var2 + IsoCell.CellSizeInSquares) / 8 && var6 >= var3 / 8 && var6 < (var3 + IsoCell.CellSizeInSquares) / 8) {
               int var8 = var7 - var2 / 8 + (var6 - var3 / 8) * IsoCell.CellSizeInChunks;
               this.getChunk(var8).addRoom(var1);
            }
         }
      }

   }

   public void addRooms(ArrayList<RoomDef> var1, int var2, int var3) {
      for(int var4 = 0; var4 < var1.size(); ++var4) {
         RoomDef var5 = (RoomDef)var1.get(var4);
         this.addRoom(var5, var2, var3);
      }

   }

   public void getZonesUnique(Set<Zone> var1) {
      for(int var2 = 0; var2 < this.ChunkMap.length; ++var2) {
         if (this.hasChunk(var2)) {
            this.getChunk(var2).getZonesUnique(var1);
         }
      }

   }

   public void getZonesIntersecting(int var1, int var2, int var3, int var4, int var5, ArrayList<Zone> var6) {
      int var7 = (var1 + var4) / 8;
      if ((var1 + var4) % 8 == 0) {
         --var7;
      }

      int var8 = (var2 + var5) / 8;
      if ((var2 + var5) % 8 == 0) {
         --var8;
      }

      int var9 = this.wx * IsoCell.CellSizeInSquares;
      int var10 = this.wy * IsoCell.CellSizeInSquares;

      for(int var11 = var2 / 8; var11 <= var8; ++var11) {
         for(int var12 = var1 / 8; var12 <= var7; ++var12) {
            if (var12 >= var9 / 8 && var12 < (var9 + IsoCell.CellSizeInSquares) / 8 && var11 >= var10 / 8 && var11 < (var10 + IsoCell.CellSizeInSquares) / 8) {
               int var13 = var12 - var9 / 8 + (var11 - var10 / 8) * IsoCell.CellSizeInChunks;
               if (this.hasChunk(var13)) {
                  this.getChunk(var13).getZonesIntersecting(var1, var2, var3, var4, var5, var6);
               }
            }
         }
      }

   }

   public void getBuildingsIntersecting(int var1, int var2, int var3, int var4, ArrayList<BuildingDef> var5) {
      int var6 = (var1 + var3) / 8;
      if ((var1 + var3) % 8 == 0) {
         --var6;
      }

      int var7 = (var2 + var4) / 8;
      if ((var2 + var4) % 8 == 0) {
         --var7;
      }

      int var8 = this.wx * IsoCell.CellSizeInSquares;
      int var9 = this.wy * IsoCell.CellSizeInSquares;

      for(int var10 = var2 / 8; var10 <= var7; ++var10) {
         for(int var11 = var1 / 8; var11 <= var6; ++var11) {
            if (var11 >= var8 / 8 && var11 < (var8 + IsoCell.CellSizeInSquares) / 8 && var10 >= var9 / 8 && var10 < (var9 + IsoCell.CellSizeInSquares) / 8) {
               int var12 = var11 - var8 / 8 + (var10 - var9 / 8) * IsoCell.CellSizeInChunks;
               if (this.hasChunk(var12)) {
                  this.getChunk(var12).getBuildingsIntersecting(var1, var2, var3, var4, var5);
               }
            }
         }
      }

   }

   public void getRoomsIntersecting(int var1, int var2, int var3, int var4, ArrayList<RoomDef> var5) {
      int var6 = (var1 + var3) / 8;
      if ((var1 + var3) % 8 == 0) {
         --var6;
      }

      int var7 = (var2 + var4) / 8;
      if ((var2 + var4) % 8 == 0) {
         --var7;
      }

      int var8 = this.wx * IsoCell.CellSizeInSquares;
      int var9 = this.wy * IsoCell.CellSizeInSquares;

      for(int var10 = var2 / 8; var10 <= var7; ++var10) {
         for(int var11 = var1 / 8; var11 <= var6; ++var11) {
            if (var11 >= var8 / 8 && var11 < (var8 + IsoCell.CellSizeInSquares) / 8 && var10 >= var9 / 8 && var10 < (var9 + IsoCell.CellSizeInSquares) / 8) {
               int var12 = var11 - var8 / 8 + (var10 - var9 / 8) * IsoCell.CellSizeInChunks;
               if (this.hasChunk(var12)) {
                  this.getChunk(var12).getRoomsIntersecting(var1, var2, var3, var4, var5);
               }
            }
         }
      }

   }

   public void checkAnimalZonesGenerated(int var1, int var2) {
      if (!this.bAnimalZonesGenerated) {
         this.bAnimalZonesGenerated = true;
         IsoWorld.instance.getZoneGenerator().genAnimalsPath(var1, var2);
      }
   }

   public void Dispose() {
      for(int var1 = 0; var1 < this.ChunkMap.length; ++var1) {
         if (this.hasChunk(var1)) {
            this.getChunk(var1).Dispose();
            this.ChunkMap[var1] = null;
         }
      }

      this.info = null;
      this.animalZones.clear();
      this.mannequinZones.clear();
      if (this.worldGenZones != null) {
         this.worldGenZones.clear();
      }

      this.roomTones.clear();
   }

   public void save(ByteBuffer var1) {
      var1.put((byte)(this.bAnimalZonesGenerated ? 1 : 0));
   }

   public void load(IsoMetaGrid var1, ByteBuffer var2, int var3) {
      this.bAnimalZonesGenerated = var2.get() == 1;
   }

   public int getAnimalZonesSize() {
      return this.animalZones.size();
   }

   public AnimalZone getAnimalZone(int var1) {
      return (AnimalZone)this.animalZones.get(var1);
   }

   public void addAnimalZone(AnimalZone var1) {
      this.animalZones.add(var1);
   }

   public void clearAnimalZones() {
      this.animalZones.clear();
   }
}
