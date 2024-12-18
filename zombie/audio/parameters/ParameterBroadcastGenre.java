package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;

public class ParameterBroadcastGenre extends FMODLocalParameter {
   private BroadcastGenre genre;

   public ParameterBroadcastGenre() {
      super("BroadcastGenre");
      this.genre = ParameterBroadcastGenre.BroadcastGenre.Generic;
   }

   public float calculateCurrentValue() {
      return (float)this.genre.label;
   }

   public void setValue(BroadcastGenre var1) {
      this.genre = var1;
   }

   public static enum BroadcastGenre {
      Generic(0),
      News(1),
      EntertainmentNews(2),
      Drama(3),
      KidsShow(4),
      Sports(5),
      MilitaryRadio(6),
      AmateurRadio(7),
      Commercial(8),
      MusicDJ(9),
      GenericVoices(10),
      FranticMilitary(11);

      final int label;

      private BroadcastGenre(int var3) {
         this.label = var3;
      }

      public int getValue() {
         return this.label;
      }
   }
}
