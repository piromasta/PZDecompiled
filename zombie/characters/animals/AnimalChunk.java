package zombie.characters.animals;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.GameTime;
import zombie.popman.ObjectPool;

public final class AnimalChunk {
   int m_x;
   int m_y;
   final ArrayList<VirtualAnimal> m_animals = new ArrayList();
   public AnimalCell cell;
   public final ArrayList<AnimalTracks> m_animalTracks = new ArrayList();
   int tracks_update_timer = 0;
   static final ObjectPool<AnimalChunk> pool = new ObjectPool(AnimalChunk::new);

   public AnimalChunk() {
   }

   AnimalChunk init(int var1, int var2) {
      this.m_x = var1;
      this.m_y = var2;
      return this;
   }

   void save(ByteBuffer var1) throws IOException {
      var1.putShort((short)this.m_animals.size());

      int var2;
      for(var2 = 0; var2 < this.m_animals.size(); ++var2) {
         VirtualAnimal var3 = (VirtualAnimal)this.m_animals.get(var2);
         var3.save(var1);
      }

      var1.putShort((short)this.m_animalTracks.size());

      for(var2 = 0; var2 < this.m_animalTracks.size(); ++var2) {
         AnimalTracks var4 = (AnimalTracks)this.m_animalTracks.get(var2);
         var4.save(var1);
      }

   }

   public void updateTracks() {
      if (!this.m_animalTracks.isEmpty()) {
         if (this.tracks_update_timer <= 0) {
            this.tracks_update_timer = 5000;

            for(int var1 = 0; var1 < this.m_animalTracks.size(); ++var1) {
               AnimalTracks var2 = (AnimalTracks)this.m_animalTracks.get(var1);
               if (var2.getTrackAgeDays() >= 4) {
                  this.m_animalTracks.remove(var1);
                  --var1;
               }
            }
         }

         this.tracks_update_timer -= (int)GameTime.getInstance().getMultiplier();
      }
   }

   void save(ByteBuffer var1, ArrayList<VirtualAnimal> var2) throws IOException {
      var1.putShort((short)(this.m_animals.size() + var2.size()));

      int var3;
      VirtualAnimal var4;
      for(var3 = 0; var3 < this.m_animals.size(); ++var3) {
         var4 = (VirtualAnimal)this.m_animals.get(var3);
         var4.save(var1);
      }

      for(var3 = 0; var3 < var2.size(); ++var3) {
         var4 = (VirtualAnimal)var2.get(var3);
         var4.save(var1);
      }

      var1.putShort((short)this.m_animalTracks.size());

      for(var3 = 0; var3 < this.m_animalTracks.size(); ++var3) {
         AnimalTracks var5 = (AnimalTracks)this.m_animalTracks.get(var3);
         var5.save(var1);
      }

   }

   void load(ByteBuffer var1, int var2) throws IOException {
      short var3 = var1.getShort();

      int var4;
      for(var4 = 0; var4 < var3; ++var4) {
         VirtualAnimal var5 = new VirtualAnimal();
         var5.load(var1, var2);
         MigrationGroupDefinitions.initValueFromDef(var5);
         this.m_animals.add(var5);

         for(int var6 = 0; var6 < var5.m_animals.size(); ++var6) {
            IsoAnimal var7 = (IsoAnimal)var5.m_animals.get(var6);
            this.cell.m_animalList.add(var7);
            if (var7.attachBackToMother > 0) {
               this.cell.m_animalListToReattach.add(var7);
            }
         }
      }

      var3 = var1.getShort();

      for(var4 = 0; var4 < var3; ++var4) {
         AnimalTracks var8 = new AnimalTracks();
         var8.load(var1, var2);
         this.m_animalTracks.add(var8);
      }

      if (!this.m_animals.isEmpty() || !this.m_animalTracks.isEmpty()) {
         AnimalZones.addAnimalChunk(this);
      }

   }

   public ArrayList<VirtualAnimal> getVirtualAnimals() {
      return this.m_animals;
   }

   public ArrayList<AnimalTracks> getAnimalsTracks() {
      return this.m_animalTracks;
   }

   public void deleteTracks() {
      if (this.m_animals.isEmpty() && !this.m_animalTracks.isEmpty()) {
         AnimalZones.removeAnimalChunk(this);
      }

      this.m_animalTracks.clear();
   }

   public void addTracksStr(VirtualAnimal var1, String var2) {
      AnimalTracksDefinitions.AnimalTracksType var3 = (AnimalTracksDefinitions.AnimalTracksType)((AnimalTracksDefinitions)AnimalTracksDefinitions.tracksDefinitions.get(var1.migrationGroup)).tracks.get(var2);
      this.addTracks(var1, var3);
   }

   public void addTracks(VirtualAnimal var1, AnimalTracksDefinitions.AnimalTracksType var2) {
      float var3 = AnimalZones.getClosestZoneDist(var1.m_x, var1.m_y);
      if (var3 != -1.0F && !(var3 > 20.0F)) {
         this.m_animalTracks.add(AnimalTracks.addAnimalTrack(var1, var2));
         if (this.m_animals.size() + this.m_animalTracks.size() == 1) {
            AnimalZones.addAnimalChunk(this);
         }

      }
   }

   public VirtualAnimal findAnimalByID(double var1) {
      if (var1 == 0.0) {
         return null;
      } else {
         for(int var3 = 0; var3 < this.m_animals.size(); ++var3) {
            VirtualAnimal var4 = (VirtualAnimal)this.m_animals.get(var3);
            if (var4.id != 0.0 && var4.id == var1) {
               return var4;
            }
         }

         return null;
      }
   }

   static AnimalChunk alloc() {
      return (AnimalChunk)pool.alloc();
   }

   void release() {
      if (this.m_animals.size() + this.m_animalTracks.size() > 0) {
         AnimalZones.removeAnimalChunk(this);
      }

      this.m_animals.clear();
      this.m_animalTracks.clear();
      pool.release((Object)this);
   }
}
