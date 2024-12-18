package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;

public class ParameterBroadcastVoiceType extends FMODLocalParameter {
   private BroadcastVoiceType voiceType;

   public ParameterBroadcastVoiceType() {
      super("BroadcastGenre");
      this.voiceType = ParameterBroadcastVoiceType.BroadcastVoiceType.Male;
   }

   public float calculateCurrentValue() {
      return (float)this.voiceType.label;
   }

   public void setValue(BroadcastVoiceType var1) {
      this.voiceType = var1;
   }

   public static enum BroadcastVoiceType {
      Male(0),
      Female(1);

      final int label;

      private BroadcastVoiceType(int var3) {
         this.label = var3;
      }

      public int getValue() {
         return this.label;
      }
   }
}
