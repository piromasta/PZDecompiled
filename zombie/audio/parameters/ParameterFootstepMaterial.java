package zombie.audio.parameters;

import fmod.fmod.FMODManager;
import zombie.audio.FMODLocalParameter;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.math.PZMath;
import zombie.core.properties.PropertyContainer;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.util.list.PZArrayList;

public final class ParameterFootstepMaterial extends FMODLocalParameter {
   private final IsoGameCharacter character;

   public ParameterFootstepMaterial(IsoGameCharacter var1) {
      super("FootstepMaterial");
      this.character = var1;
   }

   public float calculateCurrentValue() {
      return (float)this.getMaterial().label;
   }

   private FootstepMaterial getMaterial() {
      if (FMODManager.instance.getNumListeners() == 1) {
         for(int var1 = 0; var1 < IsoPlayer.numPlayers; ++var1) {
            IsoPlayer var2 = IsoPlayer.players[var1];
            if (var2 != null && var2 != this.character && !var2.Traits.Deaf.isSet()) {
               if (PZMath.fastfloor(var2.getZ()) < PZMath.fastfloor(this.character.getZ())) {
                  return ParameterFootstepMaterial.FootstepMaterial.Upstairs;
               }
               break;
            }
         }
      }

      Object var10 = null;
      IsoObject var11 = null;
      IsoObject var3 = null;
      IsoGridSquare var4 = this.character.getCurrentSquare();
      if (var4 != null) {
         if (IsoWorld.instance.CurrentCell.gridSquareIsSnow(var4.x, var4.y, var4.z)) {
            return ParameterFootstepMaterial.FootstepMaterial.Snow;
         }

         PZArrayList var5 = var4.getObjects();

         for(int var6 = 0; var6 < var5.size(); ++var6) {
            IsoObject var7 = (IsoObject)var5.get(var6);
            if (!(var7 instanceof IsoWorldInventoryObject)) {
               PropertyContainer var8 = var7.getProperties();
               if (var8 != null) {
                  if (var8.Is(IsoFlagType.solidfloor)) {
                     ;
                  }

                  if (var7.isStairsObject()) {
                     var11 = var7;
                  }

                  if (var8.Is("FootstepMaterial")) {
                     var3 = var7;
                  }
               }
            }
         }
      }

      if (var3 != null) {
         try {
            String var12 = var3.getProperties().Val("FootstepMaterial");
            return ParameterFootstepMaterial.FootstepMaterial.valueOf(var12);
         } catch (IllegalArgumentException var9) {
            boolean var13 = true;
         }
      }

      return var11 != null ? ParameterFootstepMaterial.FootstepMaterial.Wood : ParameterFootstepMaterial.FootstepMaterial.Concrete;
   }

   static enum FootstepMaterial {
      Upstairs(0),
      BrokenGlass(1),
      Concrete(2),
      Grass(3),
      Gravel(4),
      Puddle(5),
      Snow(6),
      Wood(7),
      Carpet(8),
      Dirt(9),
      Sand(10),
      Ceramic(11),
      Metal(12);

      final int label;

      private FootstepMaterial(int var3) {
         this.label = var3;
      }
   }
}
