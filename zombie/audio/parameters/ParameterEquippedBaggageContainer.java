package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.characters.IsoGameCharacter;

public final class ParameterEquippedBaggageContainer extends FMODLocalParameter {
   private final IsoGameCharacter character;
   private ContainerType containerType;

   public ParameterEquippedBaggageContainer(IsoGameCharacter var1) {
      super("EquippedBaggageContainer");
      this.containerType = ParameterEquippedBaggageContainer.ContainerType.None;
      this.character = var1;
   }

   public float calculateCurrentValue() {
      return (float)this.containerType.label;
   }

   public void setContainerType(ContainerType var1) {
      this.containerType = var1;
   }

   public void setContainerType(String var1) {
      if (var1 != null) {
         try {
            this.containerType = ParameterEquippedBaggageContainer.ContainerType.valueOf(var1);
         } catch (IllegalArgumentException var3) {
         }

      }
   }

   public static enum ContainerType {
      None(0),
      HikingBag(1),
      DuffleBag(2),
      PlasticBag(3),
      SchoolBag(4),
      ToteBag(5),
      GarbageBag(6);

      public final int label;

      private ContainerType(int var3) {
         this.label = var3;
      }
   }
}
