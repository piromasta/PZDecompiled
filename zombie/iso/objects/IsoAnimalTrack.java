package zombie.iso.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.characters.IsoPlayer;
import zombie.characters.animals.AnimalTracks;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;

public class IsoAnimalTrack extends IsoObject {
   public boolean glow = false;
   private AnimalTracks track;

   public IsoAnimalTrack(IsoCell var1) {
      super(var1);
   }

   public IsoAnimalTrack(IsoGridSquare var1, String var2, AnimalTracks var3) {
      super((IsoGridSquare)var1, (String)var2, (String)null);
      var1.AddSpecialObject(this);
      this.track = var3;
   }

   public String getObjectName() {
      return "IsoAnimalTrack";
   }

   public void glow(IsoPlayer var1) {
      this.setOutlineHighlight(var1.PlayerIndex, true);
      this.setHighlighted(true, false);
      this.setOutlineHighlightCol(var1.PlayerIndex, 1.0F, 1.0F, 1.0F, 1.0F);
      this.setHighlightColor(1.0F, 1.0F, 1.0F, 1.0F);
      this.setOutlineHighlight(true);
      this.setOutlineHighlightCol(1.0F, 1.0F, 0.0F, 1.0F);
   }

   public void stopGlow(IsoPlayer var1) {
      this.setOutlineHighlight(var1.PlayerIndex, false);
   }

   public AnimalTracks getAnimalTracks() {
      return this.track;
   }

   public void save(ByteBuffer var1, boolean var2) throws IOException {
      super.save(var1, var2);
      this.track.save(var1);
   }

   public void load(ByteBuffer var1, int var2, boolean var3) throws IOException {
      super.load(var1, var2, var3);
      this.track = new AnimalTracks();
      this.track.load(var1, var2);
   }
}
