package zombie.core.skinnedmodel.model.jassimp;

import jassimp.AiMesh;
import org.joml.Vector3f;
import zombie.core.skinnedmodel.model.VertexBufferObject;

public final class ImportedStaticMesh {
   VertexBufferObject.VertexArray verticesUnskinned = null;
   int[] elements = null;
   final Vector3f minXYZ = new Vector3f(3.4028235E38F);
   final Vector3f maxXYZ = new Vector3f(-3.4028235E38F);

   public ImportedStaticMesh(AiMesh var1) {
      this.processAiScene(var1);
   }

   private void processAiScene(AiMesh var1) {
      int var2 = var1.getNumVertices();
      int var3 = 0;

      for(int var4 = 0; var4 < 8; ++var4) {
         if (var1.hasTexCoords(var4)) {
            ++var3;
         }
      }

      VertexBufferObject.VertexFormat var12 = new VertexBufferObject.VertexFormat(3 + var3);
      var12.setElement(0, VertexBufferObject.VertexType.VertexArray, 12);
      var12.setElement(1, VertexBufferObject.VertexType.NormalArray, 12);
      var12.setElement(2, VertexBufferObject.VertexType.TangentArray, 12);

      for(int var5 = 0; var5 < var3; ++var5) {
         var12.setElement(3 + var5, VertexBufferObject.VertexType.TextureCoordArray, 8);
      }

      var12.calculate();
      this.verticesUnskinned = new VertexBufferObject.VertexArray(var12, var2);
      Vector3f var13 = new Vector3f();

      int var6;
      for(var6 = 0; var6 < var2; ++var6) {
         float var7 = var1.getPositionX(var6);
         float var8 = var1.getPositionY(var6);
         float var9 = var1.getPositionZ(var6);
         this.minXYZ.min(var13.set(var7, var8, var9));
         this.maxXYZ.max(var13.set(var7, var8, var9));
         this.verticesUnskinned.setElement(var6, 0, var1.getPositionX(var6), var1.getPositionY(var6), var1.getPositionZ(var6));
         if (var1.hasNormals()) {
            this.verticesUnskinned.setElement(var6, 1, var1.getNormalX(var6), var1.getNormalY(var6), var1.getNormalZ(var6));
         } else {
            this.verticesUnskinned.setElement(var6, 1, 0.0F, 1.0F, 0.0F);
         }

         if (var1.hasTangentsAndBitangents()) {
            this.verticesUnskinned.setElement(var6, 2, var1.getTangentX(var6), var1.getTangentY(var6), var1.getTangentZ(var6));
         } else {
            this.verticesUnskinned.setElement(var6, 2, 0.0F, 0.0F, 1.0F);
         }

         if (var3 > 0) {
            int var10 = 0;

            for(int var11 = 0; var11 < 8; ++var11) {
               if (var1.hasTexCoords(var11)) {
                  this.verticesUnskinned.setElement(var6, 3 + var10, var1.getTexCoordU(var6, var11), 1.0F - var1.getTexCoordV(var6, var11));
                  ++var10;
               }
            }
         }
      }

      var6 = var1.getNumFaces();
      this.elements = new int[var6 * 3];

      for(int var14 = 0; var14 < var6; ++var14) {
         this.elements[var14 * 3 + 2] = var1.getFaceVertex(var14, 0);
         this.elements[var14 * 3 + 1] = var1.getFaceVertex(var14, 1);
         this.elements[var14 * 3 + 0] = var1.getFaceVertex(var14, 2);
      }

   }
}
