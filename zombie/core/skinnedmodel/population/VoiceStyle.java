package zombie.core.skinnedmodel.population;

import zombie.util.StringUtils;

public class VoiceStyle {
   public String name = "";
   public String prefix = "";
   public int voiceType = 0;
   public int bodyTypeDefault = 1;

   public VoiceStyle() {
   }

   public boolean isValid() {
      return !StringUtils.isNullOrWhitespace(this.prefix);
   }

   public String getName() {
      return this.name;
   }

   public String getPrefix() {
      return this.prefix;
   }

   public int getBodyTypeDefault() {
      return this.bodyTypeDefault;
   }

   public int getVoiceType() {
      return this.voiceType;
   }
}
