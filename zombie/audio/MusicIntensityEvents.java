package zombie.audio;

import java.util.ArrayList;
import zombie.core.math.PZMath;
import zombie.util.StringUtils;

public final class MusicIntensityEvents {
   private final ArrayList<MusicIntensityEvent> m_events = new ArrayList();
   private long m_updateTimeMS = -1L;
   private float m_intensity = 0.0F;

   public MusicIntensityEvents() {
   }

   public MusicIntensityEvent addEvent(String var1, float var2, long var3, boolean var5) {
      MusicIntensityEvent var6;
      if (!var5) {
         var6 = this.findEventById(var1);
         if (var6 != null) {
            var6.setElapsedTime(0L);
            return var6;
         }
      }

      var6 = new MusicIntensityEvent(var1, var2, var3);
      this.m_events.add(var6);
      return var6;
   }

   public void clear() {
      this.m_events.clear();
   }

   public int getEventCount() {
      return this.m_events.size();
   }

   public MusicIntensityEvent getEventByIndex(int var1) {
      return (MusicIntensityEvent)this.m_events.get(var1);
   }

   public MusicIntensityEvent findEventById(String var1) {
      for(int var2 = 0; var2 < this.m_events.size(); ++var2) {
         MusicIntensityEvent var3 = (MusicIntensityEvent)this.m_events.get(var2);
         if (StringUtils.equalsIgnoreCase(var3.getId(), var1)) {
            return var3;
         }
      }

      return null;
   }

   private float calculateIntensity() {
      float var1 = 50.0F;

      for(int var2 = 0; var2 < this.m_events.size(); ++var2) {
         MusicIntensityEvent var3 = (MusicIntensityEvent)this.m_events.get(var2);
         var1 += var3.getIntensity();
      }

      return PZMath.clamp(var1, 0.0F, 100.0F);
   }

   public void update() {
      long var1 = System.currentTimeMillis();
      long var3 = var1 - this.m_updateTimeMS;
      this.m_updateTimeMS = var1;

      for(int var5 = 0; var5 < this.m_events.size(); ++var5) {
         MusicIntensityEvent var6 = (MusicIntensityEvent)this.m_events.get(var5);
         if (var6.getDuration() <= 0L && var6.getElapsedTime() > 0L) {
            this.m_events.remove(var5--);
         } else {
            var6.setElapsedTime(var6.getElapsedTime() + var3);
            if (var6.getDuration() > 0L && var6.getElapsedTime() >= var6.getDuration()) {
               this.m_events.remove(var5--);
            }
         }
      }

      this.m_intensity = this.calculateIntensity();
   }

   public float getIntensity() {
      return this.m_intensity;
   }
}
