package zombie.audio;

import java.util.ArrayList;
import zombie.characters.IsoPlayer;
import zombie.characters.Moodles.MoodleType;
import zombie.core.math.PZMath;
import zombie.util.StringUtils;

public final class MusicThreatStatuses {
   private final IsoPlayer m_player;
   private final ArrayList<MusicThreatStatus> m_statuses = new ArrayList();
   private float m_intensity = 0.0F;

   public MusicThreatStatuses(IsoPlayer var1) {
      this.m_player = var1;
   }

   public MusicThreatStatus setStatus(String var1, float var2) {
      MusicThreatStatus var3 = this.findStatusById(var1);
      if (var3 != null) {
         var3.setIntensity(var2);
         return var3;
      } else {
         var3 = new MusicThreatStatus(var1, var2);
         this.m_statuses.add(var3);
         return var3;
      }
   }

   public void clear() {
      this.m_statuses.clear();
   }

   public int getStatusCount() {
      return this.m_statuses.size();
   }

   public MusicThreatStatus getStatusByIndex(int var1) {
      return (MusicThreatStatus)this.m_statuses.get(var1);
   }

   public MusicThreatStatus findStatusById(String var1) {
      for(int var2 = 0; var2 < this.m_statuses.size(); ++var2) {
         MusicThreatStatus var3 = (MusicThreatStatus)this.m_statuses.get(var2);
         if (StringUtils.equalsIgnoreCase(var3.getId(), var1)) {
            return var3;
         }
      }

      return null;
   }

   private float calculateIntensity() {
      float var1 = 0.0F;

      for(int var2 = 0; var2 < this.m_statuses.size(); ++var2) {
         MusicThreatStatus var3 = (MusicThreatStatus)this.m_statuses.get(var2);
         var1 += var3.getIntensity() * MusicThreatConfig.getInstance().getStatusIntensity(var3.getId());
      }

      return var1;
   }

   public void update() {
      this.setStatus("MoodlePanic", (float)this.m_player.getMoodleLevel(MoodleType.Panic) / 4.0F);
      this.setStatus("PlayerHealth", 1.0F - this.m_player.getHealth());
      this.setStatus("ZombiesVisible", PZMath.clamp((float)this.m_player.getStats().MusicZombiesVisible / 100.0F, 0.0F, 1.0F));
      this.setStatus("ZombiesTargeting.DistantNotMoving", PZMath.clamp((float)this.m_player.getStats().MusicZombiesTargeting_DistantNotMoving / 100.0F, 0.0F, 1.0F));
      this.setStatus("ZombiesTargeting.NearbyNotMoving", PZMath.clamp((float)this.m_player.getStats().MusicZombiesTargeting_NearbyNotMoving / 100.0F, 0.0F, 1.0F));
      this.setStatus("ZombiesTargeting.DistantMoving", PZMath.clamp((float)this.m_player.getStats().MusicZombiesTargeting_DistantMoving / 100.0F, 0.0F, 1.0F));
      this.setStatus("ZombiesTargeting.NearbyMoving", PZMath.clamp((float)this.m_player.getStats().MusicZombiesTargeting_NearbyMoving / 100.0F, 0.0F, 1.0F));

      for(int var1 = 0; var1 < this.m_statuses.size(); ++var1) {
         MusicThreatStatus var2 = (MusicThreatStatus)this.m_statuses.get(var1);
      }

      this.m_intensity = this.calculateIntensity();
   }

   public float getIntensity() {
      return this.m_intensity;
   }
}
