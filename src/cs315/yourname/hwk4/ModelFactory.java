package cs315.yourname.hwk4;

import java.util.ArrayList;

import android.util.Log;

/**
 * A separate class for storing/calculating model information. Refactored for readability.
 * Instantiate this class and then call public methods to fetch pre-defined arrays of coordinates for models.
 * 
 * @author Joel
 * @version Oct 1, 2013
 */
public class ModelFactory
{
	public static final String TAG = "Models";
	
	//Constructor; initializes data arrays that are programmatically defined.
	public ModelFactory()
	{
		cubeVertexNormalData = packCubeData();
		sphereVertexDataRough = generateSphereVertices(1);
		sphereVertexDataSmooth = generateSphereVertices(2);
	}
	

	/**
	 * Returns an array representing the vertices of a cube (with corners at (-1,-1,-1) and (1,1,1))
	 * @return
	 */
	public float[] getCubeVertices()
	{
		return cubeVertexData;
	}

	/**
	 * Returns an array representing the normal vectors for a cube. Each face has a shared normal.
	 * @return
	 */
	public float[] getCubeNormals()
	{
		return cubeNormalData;
	}

	/**
	 * Returns an array representing a coloring of the cube; each face is given a different (base) color.
	 * This array uses vec4 elements to represent color.
	 * @return
	 */
	public float[] getCubeColors()
	{
		return cubeColorData;
	}

	/**
	 * Returns an array representing the "edges" of a cube--these are duples that represent the start and end of each line.
	 * Can be drawn using glDrawArrays(GL_LINES, ...)
	 * @return
	 */
	public float[] getCubeEdges()
	{
		return cubeEdgeData;
	}

	/**
	 * Returns a "packed" array that contains both the position and normal data for a cube (with corners at (-1,-1,-1) and (1,1,1)).
	 * Each vertex has the the format:
	 * 	{ posX, posY, posZ, normalX, normalY, normalZ }
	 * You can pass this single buffer to two different variables in a shader by specifying the stride (see example code).
	 * @return
	 */
	public float[] getCubeData()
	{
		return cubeVertexNormalData;
	}

	//constants representing a "rough" or "smooth" sphere (rougher spheres have fewer triangles)
	public static final int ROUGH_SPHERE = 1;
	public static final int SMOOTH_SPHERE = 1;

	/**
	 * Returns an array representing the vertices of a unit sphere (a sphere with radius of 1).
	 * This is an "icosphere", or a sphere based on an icosahedron (see http://goo.gl/bLLYbO).
	 * @param detailLevel A constant (either ROUGH_SPHERE or SMOOTH_SPHERE) for how many triangles should be included. A ROUGH_SPHERE has around 80 triangles, a SMOOTH_SPHERE has about 1000
	 * @return
	 */
	public float[] getSphereVertices(int detailLevel)
	{
		if(detailLevel == SMOOTH_SPHERE)
			return sphereVertexDataSmooth;
		else
			return sphereVertexDataRough;
	}

	/**
	 * Returns a "packed" array that contains both the position and normal data for a unit sphere (a sphere with radius 1).
	 * Each vertex has the the format:
	 * 	{ posX, posY, posZ, normalX, normalY, normalZ }
	 * You can pass this single buffer to two different variables in a shader by specifying the stride (see example code).
	 * This is an "icosphere", or a sphere based on an icosahedron (see http://goo.gl/bLLYbO).
	 * @param detailLevel A constant (either ROUGH_SPHERE or SMOOTH_SPHERE) for how many triangles should be included. A ROUGH_SPHERE has around 80 triangles, a SMOOTH_SPHERE has about 1000
	 * @return
	 */
	public float[] getSphereData(int detailLevel)
	{
		//this method actually does the packing; unlike cube methods that use pre-packed variables.
		float[] sphere;
		if(detailLevel == SMOOTH_SPHERE) //which sphere are we packing
			sphere = sphereVertexDataSmooth;
		else
			sphere = sphereVertexDataRough;

		//pack (double) the sphere data!
		final float[] packedData = new float[sphere.length*2];
		int numVertices = sphere.length/3;
		for(int p=0; p<numVertices; p++) //counting points
		{
			packedData[p*6+0] = sphere[p*3+0]; //might be cleaner way to write this loop, but works.
			packedData[p*6+1] = sphere[p*3+1];
			packedData[p*6+2] = sphere[p*3+2];
			packedData[p*6+3] = sphere[p*3+0];
			packedData[p*6+4] = sphere[p*3+1];
			packedData[p*6+5] = sphere[p*3+2];
		}
		return packedData;
	}

	public float[] getCoordinateAxis()
	{
		return coordinateAxisData;
	}
	
	//instance variables for the data (some are defined programmatically through methods called in the constructor)
	private final float[] cubeVertexNormalData;
	private final float[] sphereVertexDataRough;
	private final float[] sphereVertexDataSmooth;
	
	private final float[] cubeVertexData = {
		//front face
		-1.0f, 1.0f, 1.0f,		-1.0f, -1.0f, 1.0f,		1.0f, 1.0f, 1.0f, 
		-1.0f, -1.0f, 1.0f, 	1.0f, -1.0f, 1.0f,		1.0f, 1.0f, 1.0f,

		//right face
		1.0f, 1.0f, 1.0f,		1.0f, -1.0f, 1.0f,		1.0f, 1.0f, -1.0f,
		1.0f, -1.0f, 1.0f,		1.0f, -1.0f, -1.0f,		1.0f, 1.0f, -1.0f,

		//back face
		1.0f, 1.0f, -1.0f,		1.0f, -1.0f, -1.0f,		-1.0f, 1.0f, -1.0f,
		1.0f, -1.0f, -1.0f,		-1.0f, -1.0f, -1.0f,	-1.0f, 1.0f, -1.0f,

		//left face
		-1.0f, 1.0f, -1.0f,		-1.0f, -1.0f, -1.0f,	-1.0f, 1.0f, 1.0f, 
		-1.0f, -1.0f, -1.0f,	-1.0f, -1.0f, 1.0f, 	-1.0f, 1.0f, 1.0f, 

		//top face
		-1.0f, 1.0f, -1.0f,		-1.0f, 1.0f, 1.0f, 		1.0f, 1.0f, -1.0f, 
		-1.0f, 1.0f, 1.0f, 		1.0f, 1.0f, 1.0f, 		1.0f, 1.0f, -1.0f,

		//bottom face
		1.0f, -1.0f, -1.0f,		1.0f, -1.0f, 1.0f, 		-1.0f, -1.0f, -1.0f,
		1.0f, -1.0f, 1.0f, 		-1.0f, -1.0f, 1.0f,		-1.0f, -1.0f, -1.0f,	
	};

	private final float[] cubeNormalData = {
		// Front face
		0.0f, 0.0f, 1.0f,				
		0.0f, 0.0f, 1.0f,
		0.0f, 0.0f, 1.0f,
		0.0f, 0.0f, 1.0f,				
		0.0f, 0.0f, 1.0f,
		0.0f, 0.0f, 1.0f,

		// Right face 
		1.0f, 0.0f, 0.0f,				
		1.0f, 0.0f, 0.0f,
		1.0f, 0.0f, 0.0f,
		1.0f, 0.0f, 0.0f,				
		1.0f, 0.0f, 0.0f,
		1.0f, 0.0f, 0.0f,

		// Back face 
		0.0f, 0.0f, -1.0f,				
		0.0f, 0.0f, -1.0f,
		0.0f, 0.0f, -1.0f,
		0.0f, 0.0f, -1.0f,				
		0.0f, 0.0f, -1.0f,
		0.0f, 0.0f, -1.0f,

		// Left face 
		-1.0f, 0.0f, 0.0f,				
		-1.0f, 0.0f, 0.0f,
		-1.0f, 0.0f, 0.0f,
		-1.0f, 0.0f, 0.0f,				
		-1.0f, 0.0f, 0.0f,
		-1.0f, 0.0f, 0.0f,

		// Top face 
		0.0f, 1.0f, 0.0f,			
		0.0f, 1.0f, 0.0f,
		0.0f, 1.0f, 0.0f,
		0.0f, 1.0f, 0.0f,				
		0.0f, 1.0f, 0.0f,
		0.0f, 1.0f, 0.0f,

		// Bottom face 
		0.0f, -1.0f, 0.0f,			
		0.0f, -1.0f, 0.0f,
		0.0f, -1.0f, 0.0f,
		0.0f, -1.0f, 0.0f,				
		0.0f, -1.0f, 0.0f,
		0.0f, -1.0f, 0.0f		
	};

	private final float[] cubeEdgeData = {
		-1.0f, -1.0f, -1.0f, 	-1.0f, -1.0f, 1.0f, //top
		-1.0f, -1.0f, 1.0f,		1.0f, -1.0f, 1.0f,
		1.0f, -1.0f, 1.0f,		1.0f, -1.0f, -1.0f,		
		1.0f, -1.0f, -1.0f,		-1.0f, -1.0f, -1.0f,

		-1.0f, 1.0f, -1.0f, 	-1.0f, 1.0f, 1.0f, //bottom
		-1.0f, 1.0f, 1.0f,		1.0f, 1.0f, 1.0f,
		1.0f, 1.0f, 1.0f,		1.0f, 1.0f, -1.0f,		
		1.0f, 1.0f, -1.0f,		-1.0f, 1.0f, -1.0f,

		-1.0f, 1.0f, -1.0f,		-1.0f, -1.0f, -1.0f, //mid
		-1.0f, 1.0f, 1.0f,		-1.0f, -1.0f, 1.0f,
		1.0f, 1.0f, 1.0f,		1.0f, -1.0f, 1.0f,		
		1.0f, 1.0f, -1.0f,		1.0f, -1.0f, -1.0f,
	};

	private final float[] cubeColorData = {
		//front face
		0.0f, 0.0f, 1.0f, 1.0f,
		0.0f, 0.0f, 1.0f, 1.0f,
		0.0f, 0.0f, 1.0f, 1.0f,
		0.0f, 0.0f, 1.0f, 1.0f,
		0.0f, 0.0f, 1.0f, 1.0f,
		0.0f, 0.0f, 1.0f, 1.0f,

		//right face
		1.0f, 0.0f, 0.0f, 1.0f,
		1.0f, 0.0f, 0.0f, 1.0f,
		1.0f, 0.0f, 0.0f, 1.0f,
		1.0f, 0.0f, 0.0f, 1.0f,
		1.0f, 0.0f, 0.0f, 1.0f,
		1.0f, 0.0f, 0.0f, 1.0f,

		//back face
		1.0f, 1.0f, 0.0f, 1.0f,
		1.0f, 1.0f, 0.0f, 1.0f,
		1.0f, 1.0f, 0.0f, 1.0f,
		1.0f, 1.0f, 0.0f, 1.0f,
		1.0f, 1.0f, 0.0f, 1.0f,
		1.0f, 1.0f, 0.0f, 1.0f,

		//left face
		0.0f, 1.0f, 1.0f, 1.0f,
		0.0f, 1.0f, 1.0f, 1.0f,
		0.0f, 1.0f, 1.0f, 1.0f,
		0.0f, 1.0f, 1.0f, 1.0f,
		0.0f, 1.0f, 1.0f, 1.0f,
		0.0f, 1.0f, 1.0f, 1.0f,

		//top face
		0.0f, 1.0f, 0.0f, 1.0f,
		0.0f, 1.0f, 0.0f, 1.0f,
		0.0f, 1.0f, 0.0f, 1.0f,
		0.0f, 1.0f, 0.0f, 1.0f,
		0.0f, 1.0f, 0.0f, 1.0f,
		0.0f, 1.0f, 0.0f, 1.0f,

		//bottom face
		1.0f, 0.0f, 1.0f, 1.0f,
		1.0f, 0.0f, 1.0f, 1.0f,
		1.0f, 0.0f, 1.0f, 1.0f,
		1.0f, 0.0f, 1.0f, 1.0f,
		1.0f, 0.0f, 1.0f, 1.0f,
		1.0f, 0.0f, 1.0f, 1.0f,
	};

	//helper method to pack cube data into single array. Currently includes only Vertex and Normal data
	private float[] packCubeData()
	{
		final float[] packedData = new float[cubeVertexData.length*2];
		
		int numVertices = cubeVertexData.length/3;
		for(int p=0; p<numVertices; p++) //counting points
		{
			packedData[p*6+0] = cubeVertexData[p*3+0];
			packedData[p*6+1] = cubeVertexData[p*3+1];
			packedData[p*6+2] = cubeVertexData[p*3+2];
			packedData[p*6+3] = cubeNormalData[p*3+0];
			packedData[p*6+4] = cubeNormalData[p*3+1];
			packedData[p*6+5] = cubeNormalData[p*3+2];
		}
		
		return packedData;
	}

	//This method generates an icosphere (a sphere made from subdividing an icosahedron). Unit size.
	//Code adapted from the Red Book (http://www.glprogramming.com/red/chapter02.html#name8)
	private float[] generateSphereVertices(int divisions)
	{
		final float X = .525731112119133606f; //coordinates to produce unit icosahedron
		final float Z = .850650808352039932f;

		//initial icosahedron data
		float[][] icoData = {    
				{-X, 0.0f, Z}, {X, 0.0f, Z}, {-X, 0.0f, -Z}, {X, 0.0f, -Z},    
				{0.0f, Z, X}, {0.0f, Z, -X}, {0.0f, -Z, X}, {0.0f, -Z, -X},    
				{Z, X, 0.0f}, {-Z, X, 0.0f}, {Z, -X, 0.0f}, {-Z, -X, 0.0f}
		};

		final int[][] icoIndices = { 
				{0,4,1}, {0,9,4}, {9,5,4}, {4,5,8}, {4,8,1},    
				{8,10,1}, {8,3,10}, {5,3,8}, {5,2,3}, {2,7,3},    
				{7,10,3}, {7,6,10}, {7,11,6}, {11,0,6}, {0,1,6}, 
				{6,1,10}, {9,0,11}, {9,11,2}, {9,2,5}, {7,2,11} 
		};

		ArrayList<float[]> sphereVertices = new ArrayList<float[]>(); //for easy aggregation, memory be damned
		
		//subdivide!
		for (int i = 0; i < 20; i++) { //go through the icosahedron points
			subdivide(icoData[icoIndices[i][0]],       
					  icoData[icoIndices[i][1]],       
					  icoData[icoIndices[i][2]],
					  divisions, //depth of recursion
					  sphereVertices);
		}

		//convert ArrayList of vertices into single array
		float[][] svArray = sphereVertices.toArray(new float[0][0]); //sphereVerticesArray
		int numVertices = svArray.length;
		float[] sphereVertexCoords = new float[numVertices*3];//342*3];
		int p = 0;
		for(int i=0; i<numVertices; i++)
		{
			sphereVertexCoords[p] = svArray[i][2]; //also reversing order here, at the end
			sphereVertexCoords[p+1] = svArray[i][1];
			sphereVertexCoords[p+2] = svArray[i][0];
			p += 3;
		}
		
		//drawing the icosahedron (no subdivisions)
		//		float[] sphereVertexCoords = new float[20*3*3];//342*3];
		//		int p = 0; //where we are in the coords array
		//		//convert from indexed to non-indexed!
		//		for(int i=0; i<icoIndices.length; i++){
		//			for(int j=0; j<icoIndices[i].length; j++)
		//			{
		//				float[] v = icoData[icoIndices[i][j]];
		//
		//				//this is "drawTriangle"
		//				sphereVertexCoords[p+0] = v[2]; //indicies are clockwise; have to reverse
		//				sphereVertexCoords[p+1] = v[1];
		//				sphereVertexCoords[p+2] = v[0];
		//				p += 3;
		//			}
		//		}

		return sphereVertexCoords;
	}

	//subdivides the given triangle with 3 new vertices)
	//params are the 3 vertices of the triangle, depth of recursion, and growing list of final vertices
	private void subdivide(float[] v1, float[] v2, float[] v3, int depth, ArrayList<float[]> vertices)
	{
		if(depth == 0) //if at the bottom
		{
			//Log.d(TAG,"Adding 3 new vertices at #"+vertices.size());
			vertices.add(v1);
			vertices.add(v2);
			vertices.add(v3);
			return; //stop recursing
		}

		float[] v12 = new float[3]; //new vertices
		float[] v23 = new float[3];
		float[] v31 = new float[3];

		for (int i = 0; i < 3; i++) { 
			v12[i] = v1[i]+v2[i]; //find the middle vector of these guys (we will scale it back later)
			v23[i] = v2[i]+v3[i];
			v31[i] = v3[i]+v1[i];
		} 		

		normalize(v12); //scale down to fit our unit sphere
		normalize(v23); 
		normalize(v31);
		subdivide(v1, v12, v31, depth-1, vertices); //subdvide and increment counter...
		subdivide(v2, v23, v12, depth-1, vertices);
		subdivide(v3, v31, v23, depth-1, vertices);
		subdivide(v12, v23, v31, depth-1, vertices);
	}

	//performs and in-place normalization of the given vec3
	private void normalize(float[] v) 
	{    
		double d = Math.sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2]);
		if (d == 0.0) {
			Log.e(TAG, "zero length vector");
			return;
		}
		v[0] /= d; v[1] /= d; v[2] /= d;
	}

	private float[] coordinateAxisData = {
			0,0,0,
			1,0,0,
			0,1,0,
	};

}
