package zombie.audio;

import java.util.ArrayList;
import java.util.HashMap;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.GameTime;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.BodyDamage.BodyDamage;
import zombie.characters.BodyDamage.BodyPartType;
import zombie.util.StringUtils;
import zombie.util.Type;

public final class MusicIntensityConfig {
   private static MusicIntensityConfig instance = null;
   private final ArrayList<Event> m_events = new ArrayList();
   private final HashMap<String, Event> m_eventById = new HashMap();

   public MusicIntensityConfig() {
   }

   public static MusicIntensityConfig getInstance() {
      if (instance == null) {
         instance = new MusicIntensityConfig();
      }

      return instance;
   }

   public void initEvents(KahluaTableImpl var1) {
      this.m_events.clear();
      this.m_eventById.clear();
      KahluaTableIterator var2 = var1.iterator();

      while(var2.advance()) {
         String var3 = var2.getKey().toString();
         if (!"VERSION".equalsIgnoreCase(var3)) {
            KahluaTableImpl var4 = (KahluaTableImpl)var2.getValue();
            Event var5 = new Event();
            var5.m_id = StringUtils.discardNullOrWhitespace(var4.rawgetStr("id"));
            var5.m_intensity = var4.rawgetFloat("intensity");
            var5.m_duration = (long)var4.rawgetInt("duration");
            Object var6 = var4.rawget("multiple");
            if (var6 instanceof Boolean) {
               var5.m_bMultiple = (Boolean)var6;
            }

            if (var5.m_id != null) {
               if (this.m_eventById.containsKey(var5.m_id)) {
                  this.m_events.remove(this.m_eventById.get(var5.m_id));
               }

               this.m_events.add(var5);
               this.m_eventById.put(var5.m_id, var5);
            }
         }
      }

   }

   public MusicIntensityEvent triggerEvent(String var1, MusicIntensityEvents var2) {
      Event var3 = (Event)this.m_eventById.get(var1);
      return var3 == null ? null : var2.addEvent(var3.m_id, var3.m_intensity, var3.m_duration, var3.m_bMultiple);
   }

   public void checkHealthPanelVisible(IsoGameCharacter var1) {
      IsoPlayer var2 = (IsoPlayer)Type.tryCastTo(var1, IsoPlayer.class);
      if (var2 != null) {
         this.checkHealthPanel_SeeBite(var2);
      }
   }

   private void checkHealthPanel_SeeBite(IsoPlayer var1) {
      Object var2 = var1.getMusicIntensityEventModData("HealthPanel_SeeBite");
      if (var2 == null) {
         BodyDamage var3 = var1.getBodyDamage();
         boolean var4 = false;

         for(int var5 = 0; var5 < BodyPartType.ToIndex(BodyPartType.MAX); ++var5) {
            if (var3.IsBitten(var5)) {
               var4 = true;
               break;
            }
         }

         if (var4) {
            var1.setMusicIntensityEventModData("HealthPanel_SeeBite", GameTime.getInstance().getWorldAgeHours());
            var1.triggerMusicIntensityEvent("HealthPanel_SeeBite");
         }
      }
   }

   public void restoreToFullHealth(IsoGameCharacter var1) {
      IsoPlayer var2 = (IsoPlayer)Type.tryCastTo(var1, IsoPlayer.class);
      if (var2 != null && var2.hasModData()) {
         var2.setMusicIntensityEventModData("HealthPanel_SeeBite", (Object)null);
      }
   }

   private static final class Event {
      String m_id;
      float m_intensity;
      long m_duration;
      boolean m_bMultiple = true;

      private Event() {
      }
   }
}
