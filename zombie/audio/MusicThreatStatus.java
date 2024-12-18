package zombie.audio;

public final class MusicThreatStatus {
   private final String m_id;
   private float m_intensity;

   public MusicThreatStatus(String var1, float var2) {
      this.m_id = var1;
      this.m_intensity = var2;
   }

   public String getId() {
      return this.m_id;
   }

   public float getIntensity() {
      return MusicThreatConfig.getInstance().isStatusIntensityOverridden(this.getId()) ? MusicThreatConfig.getInstance().getStatusIntensityOverride(this.getId()) : this.m_intensity;
   }

   public void setIntensity(float var1) {
      this.m_intensity = var1;
   }
}
