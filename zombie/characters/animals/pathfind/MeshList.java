package zombie.characters.animals.pathfind;

import java.util.ArrayList;

public final class MeshList {
   ArrayList<Mesh> m_meshes = new ArrayList();
   int z;

   public MeshList() {
   }

   int size() {
      return this.m_meshes.size();
   }

   Mesh get(int var1) {
      return (Mesh)this.m_meshes.get(var1);
   }

   int indexOf(Mesh var1) {
      return this.m_meshes.indexOf(var1);
   }

   int getTriangleAt(float var1, float var2) {
      for(int var3 = 0; var3 < this.size(); ++var3) {
         int var4 = this.get(var3).getTriangleAt(var1, var2);
         if (var4 != -1) {
            return var3 << 16 | var4;
         }
      }

      return -1;
   }

   Mesh getMeshAt(float var1, float var2, int var3) {
      for(int var4 = 0; var4 < this.size(); ++var4) {
         int var5 = this.get(var4).getTriangleAt(var1, var2);
         if (var5 != -1) {
            return this.get(var4);
         }
      }

      return null;
   }
}
