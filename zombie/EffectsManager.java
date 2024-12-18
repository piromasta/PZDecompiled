package zombie;

import org.joml.Matrix4f;
import zombie.characters.IsoGameCharacter;
import zombie.core.math.PZMath;
import zombie.iso.IsoCamera;
import zombie.iso.IsoLightSource;
import zombie.iso.IsoWorld;

public final class EffectsManager {
   private static EffectsManager effectsManager;

   public static EffectsManager getInstance() {
      if (effectsManager == null) {
         effectsManager = new EffectsManager();
      }

      return effectsManager;
   }

   public EffectsManager() {
   }

   public void render() {
   }

   public void startMuzzleFlash(IsoGameCharacter var1) {
      float var2 = GameTime.getInstance().getNight() * 0.8F;
      if (var1.isInARoom()) {
         var2 = PZMath.max(var2, 1.0F - var1.getCurrentSquare().getLightLevel(IsoCamera.frameState.playerIndex));
      }

      var2 = Math.max(var2, 0.2F);
      IsoLightSource var3 = new IsoLightSource(PZMath.fastfloor(var1.getX()), PZMath.fastfloor(var1.getY()), PZMath.fastfloor(var1.getZ()), 0.8F * var2, 0.8F * var2, 0.6F * var2, 18, 6);
      IsoWorld.instance.CurrentCell.getLamppostPositions().add(var3);
      var1.setMuzzleFlashDuration(System.currentTimeMillis());
   }

   public void startImpactEffect(float var1, float var2, float var3) {
      Matrix4f var4 = new Matrix4f();
      var4.translation(var1, var2, var3);
   }
}
