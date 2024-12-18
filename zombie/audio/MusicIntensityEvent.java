package zombie.audio;

public final class MusicIntensityEvent {
   private final String m_id;
   private final float m_intensity;
   private final long m_durationMS;
   private long m_elapsedTimeMS;

   public MusicIntensityEvent(String var1, float var2, long var3) {
      this.m_id = var1;
      this.m_intensity = var2;
      this.m_durationMS = var3;
   }

   public String getId() {
      return this.m_id;
   }

   public float getIntensity() {
      return this.m_intensity;
   }

   public long getDuration() {
      return this.m_durationMS;
   }

   public long getElapsedTime() {
      return this.m_elapsedTimeMS;
   }

   public void setElapsedTime(long var1) {
      this.m_elapsedTimeMS = var1;
   }
}
