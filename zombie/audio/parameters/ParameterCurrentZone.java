package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoObject;

public final class ParameterCurrentZone extends FMODLocalParameter {
   private final IsoObject object;
   private IsoMetaGrid.Zone metaZone;
   private Zone zone;

   public ParameterCurrentZone(IsoObject var1) {
      super("CurrentZone");
      this.zone = ParameterCurrentZone.Zone.None;
      this.object = var1;
   }

   public float calculateCurrentValue() {
      IsoGridSquare var1 = this.object.getSquare();
      if (var1 == null) {
         this.zone = ParameterCurrentZone.Zone.None;
         return (float)this.zone.label;
      } else if (var1.zone == this.metaZone) {
         return (float)this.zone.label;
      } else {
         this.metaZone = var1.zone;
         if (this.metaZone == null) {
            this.zone = ParameterCurrentZone.Zone.None;
            return (float)this.zone.label;
         } else {
            Zone var10001;
            switch (this.metaZone.type) {
               case "DeepForest":
                  var10001 = ParameterCurrentZone.Zone.DeepForest;
                  break;
               case "Farm":
                  var10001 = ParameterCurrentZone.Zone.Farm;
                  break;
               case "Forest":
                  var10001 = ParameterCurrentZone.Zone.Forest;
                  break;
               case "Nav":
                  var10001 = ParameterCurrentZone.Zone.Nav;
                  break;
               case "TownZone":
                  var10001 = ParameterCurrentZone.Zone.Town;
                  break;
               case "TrailerPark":
                  var10001 = ParameterCurrentZone.Zone.TrailerPark;
                  break;
               case "Vegitation":
                  var10001 = ParameterCurrentZone.Zone.Vegetation;
                  break;
               default:
                  var10001 = ParameterCurrentZone.Zone.None;
            }

            this.zone = var10001;
            return (float)this.zone.label;
         }
      }
   }

   static enum Zone {
      None(0),
      DeepForest(1),
      Farm(2),
      Forest(3),
      Nav(4),
      Town(5),
      TrailerPark(6),
      Vegetation(7);

      final int label;

      private Zone(int var3) {
         this.label = var3;
      }
   }
}
