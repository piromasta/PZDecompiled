package zombie.core.opengl;

import gnu.trove.map.hash.TIntObjectHashMap;

public final class ShaderPrograms {
   private static ShaderPrograms instance = null;
   private final TIntObjectHashMap<ShaderProgram> m_programByID = new TIntObjectHashMap();

   public static ShaderPrograms getInstance() {
      if (instance == null) {
         instance = new ShaderPrograms();
      }

      return instance;
   }

   private ShaderPrograms() {
      this.m_programByID.setAutoCompactionFactor(0.0F);
   }

   public void registerProgram(ShaderProgram var1) {
      if (var1.getShaderID() != 0) {
         this.m_programByID.put(var1.getShaderID(), var1);
      }
   }

   public void unregisterProgram(ShaderProgram var1) {
      if (var1.getShaderID() != 0) {
         this.m_programByID.remove(var1.getShaderID());
      }
   }

   public ShaderProgram getProgramByID(int var1) {
      return (ShaderProgram)this.m_programByID.get(var1);
   }
}
