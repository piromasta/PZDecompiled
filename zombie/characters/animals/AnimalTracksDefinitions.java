package zombie.characters.animals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.Lua.LuaManager;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.iso.IsoDirections;

public class AnimalTracksDefinitions {
   public static HashMap<String, AnimalTracksDefinitions> tracksDefinitions;
   public String type;
   public HashMap<String, AnimalTracksType> tracks;
   public HashMap<String, Integer> trackChance;
   public int skillToIdentify;
   public String trackType;
   public int chanceToFindTrack;

   public AnimalTracksDefinitions() {
   }

   public static AnimalTracksType getRandomTrack(String var0, String var1) {
      AnimalTracksDefinitions var2 = (AnimalTracksDefinitions)getTracksDefinition().get(var0);
      if (var2 == null) {
         DebugLog.Animal.debugln("Couldn't find animal tracks definition for animal: " + var0);
         return null;
      } else {
         int var3 = (Integer)var2.trackChance.get(var1);
         if (!Rand.NextBool(var3 * 8)) {
            return null;
         } else {
            int var4 = 0;
            ArrayList var5 = new ArrayList();
            Iterator var6 = var2.tracks.keySet().iterator();

            while(var6.hasNext()) {
               String var7 = (String)var6.next();
               AnimalTracksType var8 = (AnimalTracksType)var2.tracks.get(var7);
               if (var8.actionType.equalsIgnoreCase(var1)) {
                  var5.add(var8);
                  var4 += var8.chanceToSpawn;
               }
            }

            AnimalTracksType var11 = null;
            int var12 = Rand.Next(var4);
            int var9 = 0;

            for(int var10 = 0; var10 < var5.size(); ++var10) {
               var11 = (AnimalTracksType)var5.get(var10);
               if (var11.chanceToSpawn + var9 >= var12) {
                  break;
               }

               var9 += var11.chanceToSpawn;
               var11 = null;
            }

            return var11;
         }
      }
   }

   public static AnimalTracksType getTrackType(String var0, String var1) {
      AnimalTracksDefinitions var2 = (AnimalTracksDefinitions)getTracksDefinition().get(var0);
      if (var2 == null) {
         return null;
      } else {
         AnimalTracksType var3 = (AnimalTracksType)var2.tracks.get(var1);
         return var3;
      }
   }

   public static HashMap<String, AnimalTracksDefinitions> getTracksDefinition() {
      if (tracksDefinitions == null) {
         loadTracksDefinitions();
      }

      return tracksDefinitions;
   }

   public static void loadTracksDefinitions() {
      if (tracksDefinitions == null) {
         tracksDefinitions = new HashMap();
         KahluaTableImpl var0 = (KahluaTableImpl)LuaManager.env.rawget("AnimalTracksDefinitions");
         if (var0 != null) {
            KahluaTableImpl var1 = (KahluaTableImpl)var0.rawget("animallist");
            KahluaTableIterator var2 = var1.iterator();

            while(var2.advance()) {
               AnimalTracksDefinitions var3 = new AnimalTracksDefinitions();
               var3.type = var2.getKey().toString();
               tracksDefinitions.put(var3.type, var3);
               KahluaTableIterator var4 = ((KahluaTableImpl)var2.getValue()).iterator();

               while(var4.advance()) {
                  String var5 = var4.getKey().toString();
                  Object var6 = var4.getValue();
                  String var7 = var6.toString().trim();
                  if ("skillToIdentify".equalsIgnoreCase(var5)) {
                     var3.skillToIdentify = Float.valueOf(var7).intValue();
                  }

                  if ("chanceToFindTrack".equalsIgnoreCase(var5)) {
                     var3.chanceToFindTrack = Float.valueOf(var7).intValue();
                  }

                  if ("trackType".equalsIgnoreCase(var5)) {
                     var3.trackType = var7;
                  }

                  if ("tracks".equalsIgnoreCase(var5)) {
                     var3.loadTracks((KahluaTableImpl)var6);
                  }

                  if ("trackChance".equalsIgnoreCase(var5)) {
                     var3.loadTrackChance((KahluaTableImpl)var6);
                  }
               }
            }

         }
      }
   }

   private void loadTrackChance(KahluaTableImpl var1) {
      this.trackChance = new HashMap();
      KahluaTableIterator var2 = var1.iterator();

      while(var2.advance()) {
         String var3 = var2.getKey().toString();
         Object var4 = var2.getValue();
         String var5 = var4.toString().trim();
         this.trackChance.put(var3.trim(), Float.valueOf(var5).intValue());
      }

   }

   private void loadTracks(KahluaTableImpl var1) {
      this.tracks = new HashMap();
      KahluaTableIterator var2 = var1.iterator();

      while(var2.advance()) {
         AnimalTracksType var3 = new AnimalTracksType();
         var3.type = var2.getKey().toString();
         this.tracks.put(var3.type, var3);
         KahluaTableIterator var4 = ((KahluaTableImpl)var2.getValue()).iterator();

         while(var4.advance()) {
            String var5 = var4.getKey().toString();
            Object var6 = var4.getValue();
            String var7 = var6.toString().trim();
            if ("name".equalsIgnoreCase(var5)) {
               var3.name = var7;
            }

            if ("sprite".equalsIgnoreCase(var5)) {
               var3.sprite = var7;
            }

            if ("actionType".equalsIgnoreCase(var5)) {
               var3.actionType = var7;
            }

            if ("chanceToFindTrack".equalsIgnoreCase(var5)) {
               var3.chanceToFindTrack = Float.valueOf(var7).intValue();
            }

            if ("chanceToSpawn".equalsIgnoreCase(var5)) {
               var3.chanceToSpawn = Float.valueOf(var7).intValue();
            }

            if ("minSkill".equalsIgnoreCase(var5)) {
               var3.minSkill = Float.valueOf(var7).intValue();
            }

            if ("needDir".equalsIgnoreCase(var5)) {
               var3.needDir = Boolean.valueOf(var7);
            }

            if ("item".equalsIgnoreCase(var5)) {
               var3.item = var7;
            }

            if ("sprites".equalsIgnoreCase(var5)) {
               var3.loadSprites((KahluaTableImpl)var6);
            }
         }
      }

   }

   public class AnimalTracksType {
      public String type;
      public String name;
      public boolean needDir;
      public String sprite;
      public String actionType;
      public int chanceToFindTrack;
      public int minSkill;
      public int chanceToSpawn;
      public String item;
      public HashMap<IsoDirections, String> sprites;

      public AnimalTracksType() {
      }

      private void loadSprites(KahluaTableImpl var1) {
         this.sprites = new HashMap();
         KahluaTableIterator var2 = var1.iterator();

         while(var2.advance()) {
            String var3 = var2.getKey().toString();
            Object var4 = var2.getValue();
            String var5 = var4.toString().trim();
            this.sprites.put(IsoDirections.fromString(var3.trim()), var5);
         }

      }
   }
}
