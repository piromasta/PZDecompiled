package zombie.audio;

import java.util.ArrayList;
import java.util.HashMap;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.core.math.PZMath;
import zombie.util.StringUtils;

public final class MusicThreatConfig {
   private static MusicThreatConfig instance = null;
   private final ArrayList<Status> m_statusList = new ArrayList();
   private final HashMap<String, Status> m_statusById = new HashMap();

   public MusicThreatConfig() {
   }

   public static MusicThreatConfig getInstance() {
      if (instance == null) {
         instance = new MusicThreatConfig();
      }

      return instance;
   }

   public void initStatuses(KahluaTableImpl var1) {
      this.m_statusList.clear();
      this.m_statusById.clear();
      KahluaTableIterator var2 = var1.iterator();

      while(var2.advance()) {
         String var3 = var2.getKey().toString();
         if (!"VERSION".equalsIgnoreCase(var3)) {
            KahluaTableImpl var4 = (KahluaTableImpl)var2.getValue();
            Status var5 = new Status();
            var5.m_id = StringUtils.discardNullOrWhitespace(var4.rawgetStr("id"));
            var5.m_intensity = var4.rawgetFloat("intensity");
            if (var5.m_id != null && !(var5.m_intensity <= 0.0F)) {
               if (this.m_statusById.containsKey(var5.m_id)) {
                  this.m_statusList.remove(this.m_statusById.get(var5.m_id));
               }

               this.m_statusList.add(var5);
               this.m_statusById.put(var5.m_id, var5);
            }
         }
      }

   }

   public int getStatusCount() {
      return this.m_statusList.size();
   }

   public String getStatusIdByIndex(int var1) {
      return ((Status)this.m_statusList.get(var1)).m_id;
   }

   public float getStatusIntensityByIndex(int var1) {
      return ((Status)this.m_statusList.get(var1)).m_intensity;
   }

   public float getStatusIntensity(String var1) {
      Status var2 = (Status)this.m_statusById.get(var1);
      return var2 == null ? 0.0F : var2.m_intensity;
   }

   public void setStatusIntensityOverride(String var1, float var2) {
      Status var3 = (Status)this.m_statusById.get(var1);
      if (var3 != null) {
         var3.m_intensityOverride = var2 < 0.0F ? 0.0F / 0.0F : PZMath.clamp(var2, 0.0F, 1.0F);
      }
   }

   public float getStatusIntensityOverride(String var1) {
      Status var2 = (Status)this.m_statusById.get(var1);
      return var2 == null ? 0.0F : var2.m_intensityOverride;
   }

   public boolean isStatusIntensityOverridden(String var1) {
      Status var2 = (Status)this.m_statusById.get(var1);
      if (var2 == null) {
         return false;
      } else {
         return !Float.isNaN(var2.m_intensityOverride);
      }
   }

   private static final class Status {
      String m_id;
      float m_intensity;
      float m_intensityOverride = 0.0F / 0.0F;

      private Status() {
      }
   }
}
