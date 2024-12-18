package zombie.core.textures;

import java.util.HashMap;
import zombie.ZomboidFileSystem;
import zombie.asset.AssetManager;
import zombie.asset.AssetPath;

public final class AnimatedTextures {
   private static final HashMap<String, AnimatedTexture> textures = new HashMap();

   public AnimatedTextures() {
   }

   public static AnimatedTexture getTexture(String var0) {
      String var1 = ZomboidFileSystem.instance.getString(var0);
      if (textures.containsKey(var1)) {
         return (AnimatedTexture)textures.get(var1);
      } else {
         Object var2 = null;
         AnimatedTextureID var3 = (AnimatedTextureID)AnimatedTextureIDAssetManager.instance.load(new AssetPath(var1), (AssetManager.AssetParams)var2);
         AnimatedTexture var4 = new AnimatedTexture(var3);
         textures.put(var1, var4);
         return var4;
      }
   }
}
