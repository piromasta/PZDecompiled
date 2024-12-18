package zombie.characters.animals.pathfind;

import astar.AStar;
import astar.ISearchNode;
import gnu.trove.map.hash.TLongObjectHashMap;
import java.util.ArrayList;
import java.util.Iterator;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import zombie.core.math.PZMath;
import zombie.popman.ObjectPool;

public final class LowLevelAStar extends AStar {
   private MeshList meshList;
   LowLevelSearchNode initialNode;
   LowLevelGoalNode goalNode;
   TLongObjectHashMap<LowLevelSearchNode> nodeMap = new TLongObjectHashMap();
   IPathRenderer renderer = null;

   public LowLevelAStar(MeshList var1) {
      this.meshList = var1;
   }

   public void setMeshList(MeshList var1) {
      this.meshList = var1;
   }

   void findPath(float var1, float var2, int var3, float var4, float var5, int var6, ArrayList<Vector2f> var7) {
      Mesh var8 = this.meshList.getMeshAt(var1, var2, var3);
      Mesh var9 = this.meshList.getMeshAt(var4, var5, var6);
      if (var8 != null && var9 != null) {
         int var10 = var8.getTriangleAt(var1, var2);
         if (var10 != -1) {
            var10 |= var8.indexOf() << 16;
            int var11 = var9.getTriangleAt(var4, var5);
            if (var11 != -1) {
               var11 |= var9.indexOf() << 16;
               if (var8 == var9 && var10 == var11) {
                  if (var7 != null) {
                     var7.add(new Vector2f(var1, var2));
                     var7.add(new Vector2f(var4, var5));
                  }

                  this.renderer.drawLine(var1, var2, var4, var5, 0.0F, 1.0F, 0.0F, 1.0F);
               } else {
                  this.initOffMeshConnections(var9);
                  LowLevelSearchNode var12 = (LowLevelSearchNode)LowLevelSearchNode.pool.alloc();
                  var12.parent = null;
                  var12.astar = this;
                  var12.meshList = this.meshList;
                  var12.meshIdx = var10 >> 16 & '\uffff';
                  var12.triangleIdx = var10 & '\uffff';
                  var12.edgeIdx = -1;
                  var12.x = var1;
                  var12.y = var2;
                  LowLevelSearchNode var13 = (LowLevelSearchNode)LowLevelSearchNode.pool.alloc();
                  var13.parent = null;
                  var13.astar = this;
                  var13.meshList = this.meshList;
                  var13.meshIdx = var11 >> 16 & '\uffff';
                  var13.triangleIdx = var11 & '\uffff';
                  var13.edgeIdx = -1;
                  var13.x = var4;
                  var13.y = var5;
                  LowLevelGoalNode var14 = new LowLevelGoalNode();
                  var14.searchNode = var13;
                  this.initialNode = var12;
                  this.goalNode = var14;
                  Iterator var15 = this.nodeMap.valueCollection().iterator();

                  while(var15.hasNext()) {
                     LowLevelSearchNode var16 = (LowLevelSearchNode)var15.next();
                     LowLevelSearchNode.pool.release((Object)var16);
                  }

                  this.nodeMap.clear();
                  ArrayList var25 = this.shortestPath(var12, var14);
                  if (var25 != null) {
                     for(int var26 = 0; var26 < var25.size() - 1; ++var26) {
                        LowLevelSearchNode var17 = (LowLevelSearchNode)var25.get(var26);
                        LowLevelSearchNode var18 = (LowLevelSearchNode)var25.get(var26 + 1);
                        if (this.renderer != null) {
                           this.renderer.drawLine(var17.getX(), var17.getY(), var18.getX(), var18.getY(), 1.0F, 1.0F, 0.0F, 1.0F);
                           this.renderer.drawRect(var17.getX() - 0.5F, var17.getY() - 0.5F, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F, 1.0F);
                           Mesh var19 = this.meshList.get(var17.meshIdx);
                           Vector2f var20 = (Vector2f)var19.triangles.get(var17.triangleIdx);
                           Vector2f var21 = (Vector2f)var19.triangles.get(var17.triangleIdx + 1);
                           Vector2f var22 = (Vector2f)var19.triangles.get(var17.triangleIdx + 2);
                           float var23 = (var20.x + var21.x + var22.x) / 3.0F;
                           float var24 = (var20.y + var21.y + var22.y) / 3.0F;
                           this.renderer.drawRect(var23 - 0.5F, var24 - 0.5F, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F, 1.0F);
                        }

                        if (var26 > 0 && var26 < var25.size() - 2) {
                        }
                     }

                     ArrayList var27 = this.stringPull(var25);
                     if (var27 != null) {
                        int var28;
                        Vector3f var29;
                        if (var7 != null) {
                           for(var28 = 0; var28 < var27.size(); ++var28) {
                              var29 = (Vector3f)var27.get(var28);
                              var7.add(new Vector2f(var29.x, var29.y));
                           }
                        }

                        if (this.renderer != null) {
                           for(var28 = 0; var28 < var27.size() - 1; ++var28) {
                              var29 = (Vector3f)var27.get(var28);
                              Vector3f var30 = (Vector3f)var27.get(var28 + 1);
                              this.renderer.drawLine(var29.x, var29.y, var30.x, var30.y, 0.0F, 1.0F, 0.0F, 1.0F);
                              this.renderer.drawRect(var29.x - 0.5F, var29.y - 0.5F, 1.0F, 1.0F, 0.0F, 1.0F, 0.0F, 1.0F);
                           }
                        }

                        AnimalPathfind.getInstance().vector3fObjectPool.releaseAll(var27);
                     }
                  }

                  LowLevelSearchNode.pool.release((Object)var12);
                  LowLevelSearchNode.pool.release((Object)var13);
                  LowLevelSearchNode.pool.releaseAll(new ArrayList(this.nodeMap.valueCollection()));
                  this.nodeMap.clear();
               }
            }
         }
      }
   }

   public long makeKey(int var1, int var2) {
      return (long)(var1 << 16 & -65536 | var2 & '\uffff');
   }

   public int triFromKey(int var1) {
      return var1 >> 8 & '\uffff';
   }

   public int edgeFromKey(int var1) {
      return var1 & 255;
   }

   public void getSuccessors(LowLevelSearchNode var1, ArrayList<ISearchNode> var2) {
      var2.clear();
      int var3 = var1.triangleIdx;
      Mesh var4 = var1.meshList.get(var1.meshIdx);

      for(int var5 = 0; var5 < 3; ++var5) {
         int var6 = var4.adjacentTriangles.get(var3 + var5);
         if (var6 != -1) {
            int var7 = var6 >> 16 & '\uffff';
            int var8 = var6 & '\uffff';
            this.addSuccessor(var1.meshList, var1.meshIdx, var7, var8, 0.0F / 0.0F, 0.0F / 0.0F, var2);
         }
      }

      if (var1.meshList == this.goalNode.searchNode.meshList && var1.meshIdx == this.goalNode.searchNode.meshIdx && var1.triangleIdx == this.goalNode.searchNode.triangleIdx && var1 != this.goalNode.searchNode) {
         var2.add(this.goalNode.searchNode);
      }

      this.initOffMeshConnections(var1.meshList.get(var1.meshIdx));
      Iterator var9 = var4.offMeshConnections.iterator();

      while(var9.hasNext()) {
         OffMeshConnection var10 = (OffMeshConnection)var9.next();
         if (var10.triFrom == var1.triangleIdx) {
            this.addSuccessor(var10.meshTo.meshList, var10.meshTo.indexOf(), var10.triTo, var10.edgeTo, (var10.edge1.x + var10.edge2.x) / 2.0F, (var10.edge1.y + var10.edge2.y) / 2.0F, var2);
         }
      }

   }

   void initOffMeshConnections(Mesh var1) {
      if (!var1.offMeshDone) {
         var1.offMeshDone = true;

         assert var1.offMeshConnections.isEmpty();

         if (!var1.trianglesOnBoundaries.isEmpty()) {
            this.findOverlappingEdges(var1, var1.meshList);
         }

      }
   }

   LowLevelSearchNode getSearchNode(MeshList var1, int var2, int var3, int var4) {
      long var5 = this.makeKey(var2, var3);
      LowLevelSearchNode var7 = (LowLevelSearchNode)this.nodeMap.get(var5);
      if (var7 == null) {
         var7 = (LowLevelSearchNode)LowLevelSearchNode.pool.alloc();
         var7.parent = null;
         var7.astar = this;
         var7.meshList = var1;
         var7.meshIdx = var2;
         var7.triangleIdx = var3;
         var7.edgeIdx = var4;
         var7.x = 0.0F / 0.0F;
         var7.y = 0.0F / 0.0F;
         if (var4 == -1) {
            var7.x = var7.getCentroidX();
            var7.y = var7.getCentroidY();
         }

         this.nodeMap.put(var5, var7);
      }

      return var7;
   }

   void addSuccessor(MeshList var1, int var2, int var3, int var4, float var5, float var6, ArrayList<ISearchNode> var7) {
      boolean var8 = this.nodeMap.containsKey(this.makeKey(var2, var3));
      LowLevelSearchNode var9 = this.getSearchNode(var1, var2, var3, var4);
      if (!var7.contains(var9)) {
         if (!var8) {
            var9.x = var5;
            var9.y = var6;
         }

         var7.add(var9);
      }
   }

   void findOverlappingEdges(Mesh var1, MeshList var2) {
      for(int var3 = 0; var3 < var1.trianglesOnBoundaries.size(); ++var3) {
         short var4 = var1.trianglesOnBoundaries.get(var3);
         short var5 = var1.edgesOnBoundaries.get(var3);

         for(int var6 = 0; var6 < 3; ++var6) {
            if ((var5 & 1 << var6) != 0) {
               for(int var7 = 0; var7 < var2.size(); ++var7) {
                  Mesh var8 = var2.get(var7);
                  if (var1 != var8) {
                     this.findOverlappingEdges(var1, var4, var6, var8);
                  }
               }
            }
         }
      }

   }

   void findOverlappingEdges(Mesh var1, int var2, int var3, Mesh var4) {
      Vector2f var5 = (Vector2f)var1.triangles.get(var2 + var3);
      Vector2f var6 = (Vector2f)var1.triangles.get(var2 + (var3 + 1) % 3);
      Vector2f var7 = (Vector2f)AnimalPathfind.getInstance().vector2fObjectPool.alloc();
      Vector2f var8 = (Vector2f)AnimalPathfind.getInstance().vector2fObjectPool.alloc();

      for(int var9 = 0; var9 < var4.trianglesOnBoundaries.size(); ++var9) {
         short var10 = var4.trianglesOnBoundaries.get(var9);
         short var11 = var4.edgesOnBoundaries.get(var9);

         for(int var12 = 0; var12 < 3; ++var12) {
            if ((var11 & 1 << var12) != 0) {
               Vector2f var13 = (Vector2f)var4.triangles.get(var10 + var12);
               Vector2f var14 = (Vector2f)var4.triangles.get(var10 + (var12 + 1) % 3);
               if (RobustLineIntersector.computeIntersection(var5, var6, var13, var14, var7, var8) == 2) {
                  var1.addConnection(var2, var3, var4, var10, var12, var7, var8);
                  break;
               }
            }
         }
      }

      AnimalPathfind.getInstance().vector2fObjectPool.release((Object)var7);
      AnimalPathfind.getInstance().vector2fObjectPool.release((Object)var8);
   }

   private boolean anyTwoPointsEqual(Vector2f var1, Vector2f var2, Vector2f var3, Vector2f var4, Vector2f var5, Vector2f var6) {
      int var7 = 0;
      if (this.isPointEqualToAnyOf3(var1, var4, var5, var6)) {
         ++var7;
      }

      if (this.isPointEqualToAnyOf3(var2, var4, var5, var6)) {
         ++var7;
      }

      if (this.isPointEqualToAnyOf3(var3, var4, var5, var6)) {
         ++var7;
      }

      return var7 > 1;
   }

   private boolean isPointEqualToAnyOf2(Vector2f var1, Vector2f var2, Vector2f var3) {
      return this.isPointEqual(var1, var2) || this.isPointEqual(var1, var3);
   }

   private boolean isPointEqualToAnyOf3(Vector2f var1, Vector2f var2, Vector2f var3, Vector2f var4) {
      return this.isPointEqual(var1, var2) || this.isPointEqual(var1, var3) || this.isPointEqual(var1, var4);
   }

   private boolean isPointEqual(Vector2f var1, Vector2f var2) {
      return PZMath.equal(var1.x, var2.x, 0.001F) && PZMath.equal(var1.y, var2.y, 0.001F);
   }

   private boolean isPointEqual(Vector3f var1, Vector3f var2) {
      return PZMath.equal(var1.x, var2.x, 0.001F) && PZMath.equal(var1.y, var2.y, 0.001F);
   }

   public ArrayList<Vector3f> stringPull(ArrayList<ISearchNode> var1) {
      ArrayList var2 = this.getPortalEdges(var1);
      if (var2 != null && var2.size() >= 6) {
         Vector3f var3 = ((Vector3f)AnimalPathfind.getInstance().vector3fObjectPool.alloc()).set((Vector3fc)var2.get(0));
         Vector3f var4 = ((Vector3f)AnimalPathfind.getInstance().vector3fObjectPool.alloc()).set((Vector3fc)var2.get(0));
         Vector3f var5 = ((Vector3f)AnimalPathfind.getInstance().vector3fObjectPool.alloc()).set((Vector3fc)var2.get(1));
         boolean var6 = false;
         int var7 = 0;
         int var8 = 0;
         ArrayList var9 = new ArrayList();
         var9.add(((Vector3f)AnimalPathfind.getInstance().vector3fObjectPool.alloc()).set(var3));

         for(int var10 = 1; var10 < var2.size() / 2; ++var10) {
            Vector3f var11 = (Vector3f)var2.get(var10 * 2);
            Vector3f var12 = (Vector3f)var2.get(var10 * 2 + 1);
            int var13;
            if (this.triarea2(var3, var5, var12) <= 0.0F) {
               if (!this.isPointEqual(var3, var5) && !(this.triarea2(var3, var4, var12) > 0.0F)) {
                  var9.add(((Vector3f)AnimalPathfind.getInstance().vector3fObjectPool.alloc()).set(var4));
                  var3.set(var4);
                  var13 = var7;
                  var4.set(var3);
                  var5.set(var3);
                  var7 = var7;
                  var8 = var13;
                  var10 = var13;
                  continue;
               }

               var5.set(var12);
               var8 = var10;
            }

            if (this.triarea2(var3, var4, var11) >= 0.0F) {
               if (!this.isPointEqual(var3, var4) && !(this.triarea2(var3, var5, var11) < 0.0F)) {
                  var9.add(((Vector3f)AnimalPathfind.getInstance().vector3fObjectPool.alloc()).set(var5));
                  var3.set(var5);
                  var13 = var8;
                  var4.set(var3);
                  var5.set(var3);
                  var7 = var8;
                  var8 = var8;
                  var10 = var13;
               } else {
                  var4.set(var11);
                  var7 = var10;
               }
            }
         }

         var9.add(((Vector3f)AnimalPathfind.getInstance().vector3fObjectPool.alloc()).set((Vector3fc)var2.get(var2.size() - 1)));
         AnimalPathfind.getInstance().vector3fObjectPool.release((Object)var3);
         AnimalPathfind.getInstance().vector3fObjectPool.release((Object)var4);
         AnimalPathfind.getInstance().vector3fObjectPool.release((Object)var5);
         return var9;
      } else {
         return null;
      }
   }

   float triarea2(Vector3f var1, Vector3f var2, Vector3f var3) {
      float var4 = var2.x - var1.x;
      float var5 = var2.y - var1.y;
      float var6 = var3.x - var1.x;
      float var7 = var3.y - var1.y;
      return var6 * var5 - var4 * var7;
   }

   ArrayList<Vector3f> getPortalEdges(ArrayList<ISearchNode> var1) {
      ArrayList var2 = new ArrayList();
      ObjectPool var3 = AnimalPathfind.getInstance().vector2fObjectPool;
      ObjectPool var4 = AnimalPathfind.getInstance().vector3fObjectPool;
      LowLevelSearchNode var5 = (LowLevelSearchNode)var1.get(0);
      var2.add(((Vector3f)var4.alloc()).set(var5.getX(), var5.getY(), var5.getZ()));
      var2.add(((Vector3f)var4.alloc()).set(var5.getX(), var5.getY(), var5.getZ()));
      Vector2f var6 = (Vector2f)var3.alloc();
      Vector2f var7 = (Vector2f)var3.alloc();
      Vector2f var8 = (Vector2f)var3.alloc();
      Vector2f var9 = (Vector2f)var3.alloc();

      for(int var10 = 0; var10 < var1.size() - 2; ++var10) {
         LowLevelSearchNode var11 = (LowLevelSearchNode)var1.get(var10);
         LowLevelSearchNode var12 = (LowLevelSearchNode)var1.get(var10 + 1);
         if (var11.meshList.z != var12.meshList.z) {
            int var13 = (int)var12.getX();
            int var14 = (int)var12.getY();
            if (Math.abs(var12.getX() - var11.getX()) > Math.abs(var12.getY() - var11.getY())) {
               var2.add(((Vector3f)var4.alloc()).set((float)var13 + 0.0F, (float)var14 + 0.0F, var12.getZ()));
               var2.add(((Vector3f)var4.alloc()).set((float)var13 + 0.0F, (float)var14 + 1.0F, var12.getZ()));
            } else {
               var2.add(((Vector3f)var4.alloc()).set((float)var13 + 0.0F, (float)var14 + 0.0F, var12.getZ()));
               var2.add(((Vector3f)var4.alloc()).set((float)var13 + 1.0F, (float)var14 + 0.0F, var12.getZ()));
            }
         } else {
            Mesh var32 = var11.meshList.get(var11.meshIdx);
            Mesh var33 = var12.meshList.get(var12.meshIdx);
            int var15 = this.getSharedEdge(var32, var11.triangleIdx, var33, var12.triangleIdx, var6, var7, var8, var9);
            if (var15 == -1) {
               return null;
            }

            int var16 = this.unpackEdge1(var15);
            Vector2f var17 = (Vector2f)var32.triangles.get(var11.triangleIdx + var16);
            Vector2f var18 = (Vector2f)var32.triangles.get(var11.triangleIdx + (var16 + 1) % 3);
            float var19 = Vector2f.distanceSquared(var17.x, var17.y, var18.x, var18.y);
            int var20 = this.unpackEdge2(var15);
            Vector2f var21 = (Vector2f)var33.triangles.get(var12.triangleIdx + var20);
            Vector2f var22 = (Vector2f)var33.triangles.get(var12.triangleIdx + (var20 + 1) % 3);
            float var23 = Vector2f.distanceSquared(var21.x, var21.y, var22.x, var22.y);
            if (!PZMath.equal(var19, var23, 0.01F)) {
               var2.add(((Vector3f)var4.alloc()).set((var8.x + var9.x) / 2.0F, (var8.y + var9.y) / 2.0F, var11.getZ()));
               var2.add(((Vector3f)var4.alloc()).set((var8.x + var9.x) / 2.0F, (var8.y + var9.y) / 2.0F, var11.getZ()));
            } else {
               float var24 = var11.x;
               float var25 = var11.y;
               float var26 = var12.x;
               float var27 = var12.y;
               float var28 = var6.x;
               float var29 = var6.y;
               float var30 = (var26 - var24) * (var29 - var25) - (var28 - var24) * (var27 - var25);
               if (var30 > 0.0F) {
                  var2.add(((Vector3f)var4.alloc()).set(var6.x, var6.y, var11.getZ()));
                  var2.add(((Vector3f)var4.alloc()).set(var7.x, var7.y, var11.getZ()));
               } else {
                  var2.add(((Vector3f)var4.alloc()).set(var7.x, var7.y, var11.getZ()));
                  var2.add(((Vector3f)var4.alloc()).set(var6.x, var6.y, var11.getZ()));
               }
            }
         }
      }

      LowLevelSearchNode var31 = (LowLevelSearchNode)var1.get(var1.size() - 1);
      var2.add(((Vector3f)var4.alloc()).set(var31.getX(), var31.getY(), var31.getZ()));
      var2.add(((Vector3f)var4.alloc()).set(var31.getX(), var31.getY(), var31.getZ()));
      var3.release((Object)var6);
      var3.release((Object)var7);
      var3.release((Object)var8);
      var3.release((Object)var9);
      return var2;
   }

   int packEdges(int var1, int var2) {
      return var2 << 8 | var1;
   }

   int unpackEdge1(int var1) {
      return var1 & 255;
   }

   int unpackEdge2(int var1) {
      return var1 >> 8 & 255;
   }

   int getSharedEdge(Mesh var1, int var2, Mesh var3, int var4, Vector2f var5, Vector2f var6, Vector2f var7, Vector2f var8) {
      Vector2f var9 = (Vector2f)var1.triangles.get(var2);
      Vector2f var10 = (Vector2f)var1.triangles.get(var2 + 1);
      Vector2f var11 = (Vector2f)var1.triangles.get(var2 + 2);
      Vector2f var12 = (Vector2f)var3.triangles.get(var4);
      Vector2f var13 = (Vector2f)var3.triangles.get(var4 + 1);
      Vector2f var14 = (Vector2f)var3.triangles.get(var4 + 2);
      if (var5 != null) {
         var5.set(var9);
         var6.set(var10);
      }

      if (RobustLineIntersector.computeIntersection(var9, var10, var12, var13, var7, var8) == 2) {
         return this.packEdges(0, 0);
      } else if (RobustLineIntersector.computeIntersection(var9, var10, var13, var14, var7, var8) == 2) {
         return this.packEdges(0, 1);
      } else if (RobustLineIntersector.computeIntersection(var9, var10, var14, var12, var7, var8) == 2) {
         return this.packEdges(0, 2);
      } else {
         if (var5 != null) {
            var5.set(var10);
            var6.set(var11);
         }

         if (RobustLineIntersector.computeIntersection(var10, var11, var12, var13, var7, var8) == 2) {
            return this.packEdges(1, 0);
         } else if (RobustLineIntersector.computeIntersection(var10, var11, var13, var14, var7, var8) == 2) {
            return this.packEdges(1, 1);
         } else if (RobustLineIntersector.computeIntersection(var10, var11, var14, var12, var7, var8) == 2) {
            return this.packEdges(1, 2);
         } else {
            if (var5 != null) {
               var5.set(var11.x, var11.y);
               var6.set(var9.x, var9.y);
            }

            if (RobustLineIntersector.computeIntersection(var11, var9, var12, var13, var7, var8) == 2) {
               return this.packEdges(2, 0);
            } else if (RobustLineIntersector.computeIntersection(var11, var9, var13, var14, var7, var8) == 2) {
               return this.packEdges(2, 1);
            } else {
               return RobustLineIntersector.computeIntersection(var11, var9, var14, var12, var7, var8) == 2 ? this.packEdges(2, 2) : -1;
            }
         }
      }
   }
}
