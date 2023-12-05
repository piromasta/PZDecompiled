package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.characters.IsoGameCharacter;

public final class ParameterMeleeHitSurface extends FMODLocalParameter {
   private final IsoGameCharacter character;
   private Material material;

   public ParameterMeleeHitSurface(IsoGameCharacter var1) {
      super("MeleeHitSurface");
      this.material = ParameterMeleeHitSurface.Material.Default;
      this.character = var1;
   }

   public float calculateCurrentValue() {
      return (float)this.getMaterial().label;
   }

   private Material getMaterial() {
      return this.material;
   }

   public void setMaterial(Material var1) {
      this.material = var1;
   }

   public static enum Material {
      Default(0),
      Body(1),
      Fabric(2),
      Glass(3),
      Head(4),
      Metal(5),
      Plastic(6),
      Stone(7),
      Wood(8),
      GarageDoor(9),
      MetalDoor(10),
      MetalGate(11),
      PrisonMetalDoor(12),
      SlidingGlassDoor(13),
      WoodDoor(14),
      WoodGate(15),
      Tree(16);

      final int label;

      private Material(int var3) {
         this.label = var3;
      }
   }
}
