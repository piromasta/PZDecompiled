package zombie.characters.animals.datas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.core.math.PZMath;
import zombie.util.StringUtils;
import zombie.util.Type;

public class AnimalBreed {
   public String name = null;
   public ArrayList<String> texture = null;
   public String textureMale = null;
   public String textureBaby = null;
   public int minWeightBonus = 0;
   public int maxWeightBonus = 0;
   public String milkType = null;
   public String woolType = null;
   public HashMap<String, ForcedGenes> forcedGenes;
   public String invIconMale = null;
   public String invIconFemale = null;
   public String invIconBaby = null;
   public String invIconMaleDead = null;
   public String invIconFemaleDead = null;
   public String invIconBabyDead = null;
   public String leather = null;
   public String headItem = null;
   public String featherItem = null;
   public int maxFeather = 0;
   private final HashMap<String, Sound> sounds = new HashMap();

   public AnimalBreed() {
   }

   public String getName() {
      return this.name;
   }

   public String getMilkType() {
      return this.milkType;
   }

   public void loadForcedGenes(KahluaTableImpl var1) {
      this.forcedGenes = new HashMap();
      KahluaTableIterator var2 = var1.iterator();

      while(var2.advance()) {
         ForcedGenes var3 = new ForcedGenes();
         var3.name = var2.getKey().toString().toLowerCase();
         KahluaTableIterator var4 = ((KahluaTableImpl)var2.getValue()).iterator();

         while(var4.advance()) {
            String var5 = var4.getKey().toString();
            String var6 = var4.getValue().toString();
            if ("minValue".equalsIgnoreCase(var5)) {
               var3.minValue = Float.valueOf(var6);
            }

            if ("maxValue".equalsIgnoreCase(var5)) {
               var3.maxValue = Float.valueOf(var6);
            }
         }

         this.forcedGenes.put(var3.name, var3);
      }

   }

   public void loadSounds(KahluaTableImpl var1) {
      this.sounds.clear();
      KahluaTableIterator var2 = var1.iterator();

      while(var2.advance()) {
         KahluaTableImpl var3 = (KahluaTableImpl)Type.tryCastTo(var2.getValue(), KahluaTableImpl.class);
         if (var3 != null) {
            Sound var4 = new Sound();
            var4.id = var2.getKey().toString().toLowerCase(Locale.ENGLISH);
            var4.soundName = var3.getString("name");
            var4.intervalMin = var3.rawgetInt("intervalMin");
            var4.intervalMax = var3.rawgetInt("intervalMax");
            var4.slot = StringUtils.discardNullOrWhitespace(var3.getString("slot"));
            var4.priority = PZMath.max(var3.rawgetInt("priority"), 0);
            this.sounds.put(var4.id, var4);
         }
      }

   }

   public Sound getSound(String var1) {
      return StringUtils.isNullOrWhitespace(var1) ? null : (Sound)this.sounds.get(var1.toLowerCase(Locale.ENGLISH));
   }

   public boolean isSoundDefined(String var1) {
      return this.getSound(var1) != null;
   }

   public boolean isSoundUndefined(String var1) {
      return this.getSound(var1) == null;
   }

   public String getFeatherItem() {
      return this.featherItem;
   }

   public String getWoolType() {
      return this.woolType;
   }

   public static final class ForcedGenes {
      public String name;
      public float minValue = 0.0F;
      public float maxValue = 0.0F;

      public ForcedGenes() {
      }
   }

   public static final class Sound {
      public String id;
      public String soundName;
      public int intervalMin = -1;
      public int intervalMax = -1;
      public String slot;
      public int priority = 0;

      public Sound() {
      }

      public boolean isIntervalValid() {
         return this.intervalMin >= 0 && this.intervalMin <= this.intervalMax;
      }
   }
}
