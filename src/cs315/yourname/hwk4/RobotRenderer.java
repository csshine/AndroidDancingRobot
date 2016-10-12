package cs315.yourname.hwk4;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.Stack;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;

/**
 * This class represents a custom OpenGL renderer--all of our "drawing" logic goes here.
 * 
 * @author Joel; code adapted from Google and LearnOpenGLES
 * @author Kramer Canfield
 * @version Fall 2013
 * @version October 18, 2013
 */
public class RobotRenderer implements GLSurfaceView.Renderer 
{
	private static final String TAG = "RUBIX Renderer"; //for logging/debugging

	//some constants given our model specifications
	private final int POSITION_DATA_SIZE = 3;	
	private final int NORMAL_DATA_SIZE = 3;
	private final int COLOR_DATA_SIZE = 4; //in case we may want it!
	private final int BYTES_PER_FLOAT = 4;
	
	//Matrix storage
	private float[] mModelMatrix = new float[16]; //to store current model matrix
	private float[] mViewMatrix = new float[16];
	private float[] mProjectionMatrix = new float[16];
	private float[] mMVMatrix = new float[16]; //to store the current modelview matrix
	private float[] mMVPMatrix = new float[16]; //combined MVP matrix
	private float[] mTempMatrix = new float[16]; //temporary matrix for transformations, if needed

	//Buffer for model data
	private final FloatBuffer mCubeData;
	private final int mCubeVertexCount; //vertex count for the buffer
	
	private final FloatBuffer mSphereData;
	private final int mSphereVertexCount; //vertex count for the buffer
	

	private Stack<float[]> roboStack; //the stack of all of the parts of the robot, used for relative locations of the parts of the robot
	
	//dancing rotation angles
	private float shoulderAngle;
	private float elbowAngle;

	private float rightHipAngle;
	private float rightKneeAngle;
	private float leftHipAngle;
	private float leftKneeAngle;
	
	private long _time;//make time an instance variable for easier access in dancing animations
	
	private boolean isDancing;//for animation control
	
	private final float[] mColorRed;
	private final float[] mColorBlue;
	private final float[] mColorGrey;
	private final float[] mColorGreen;

	//axis points (for debugging)
	private final FloatBuffer mAxisBuffer;
	private final int mAxisCount;
	private final float[] lightNormal = {0,0,3};
	
	/**
	 * OpenGL Handles
	 * These are C-style "pointers" (int representing a memory address) to particular blocks of data.
	 * We pass the pointers around instead of the data to increase efficiency (and because OpenGL is
	 * C-based, and that's how they do things).
	 */
	private int mPerVertexProgramHandle; //our "program" (OpenGL state) for drawing (uses some lighting!)
	private int mMVMatrixHandle; //the combined ModelView matrix
	private int mMVPMatrixHandle; //the combined ModelViewProjection matrix
	private int mPositionHandle; //the position of a vertex
	private int mNormalHandle; //the position of a vertex
	private int mColorHandle; //the color to paint the model
	
	//define the source code for the vertex shader
	private final String perVertexShaderCode = 
		    "uniform mat4 uMVMatrix;" + 	// A constant representing the modelview matrix. Used for calculating lights/shading
			"uniform mat4 uMVPMatrix;" +	// A constant representing the combined modelview/projection matrix. We use this for positioning
			"attribute vec4 aPosition;" +	// Per-vertex position information we will pass in
			"attribute vec3 aNormal;" +		// Per-vertex normal information we will pass in.
			"attribute vec4 aColor;" +		// Per-vertex color information we will pass in.
			"varying vec4 vColor;"  + 		//out : the ultimate color of the vertex
			"vec3 lightPos = vec3(0.0,0.0,3.0);" + //the position of the light
			"void main() {" +
			"  vec3 modelViewVertex = vec3(uMVMatrix * aPosition);" + 			//position modified by modelview
			"  vec3 modelViewNormal = normalize(vec3(uMVMatrix * vec4(aNormal, 0.0)));" +	//normal modified by modelview
			"  vec3 lightVector = normalize(lightPos - modelViewVertex);" +		//the normalized vector between the light and the vertex
			"  float diffuse = max(dot(modelViewNormal, lightVector), 0.1);" +	//the amount of diffuse light to give (based on angle between light and normal)
			"  vColor = aColor * diffuse;"+ 									//scale the color by the light factor and set to output
			"  gl_PointSize = 3.0;" +		//for drawing points
			"  gl_Position = uMVPMatrix * aPosition;" + //gl_Position is built-in variable for the transformed vertex's position.
			"}";

	private final String fragmentShaderCode = 
			"precision mediump float;" + 	//don't need high precision
			"varying vec4 vColor;" + 		//color for the fragment; this was output from the vertexShader
			"void main() {" +
			"  gl_FragColor = vColor;" + 	//gl_fragColor is built-in variable for color of fragment
			"}";

	
	/**
	 * Constructor should initialize any data we need, such as model data
	 */
	public RobotRenderer(Context context)
	{	
		/**
		 * Initialize our model data--we fetch it from the factory!
		 */
		ModelFactory models = new ModelFactory();
		
		roboStack = new Stack<float[]>();//initialize the stack

		float[] cubeData = models.getCubeData();
		mCubeVertexCount = cubeData.length/(POSITION_DATA_SIZE+NORMAL_DATA_SIZE);
		mCubeData = ByteBuffer.allocateDirect(cubeData.length * BYTES_PER_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer(); //generate buffer
		mCubeData.put(cubeData); //put the float[] into the buffer and set the position

		//more models can go here!
		
		
		
		//SPHERE
		float[] sphereData = models.getSphereData(ModelFactory.SMOOTH_SPHERE);
		mSphereVertexCount = sphereData.length/(POSITION_DATA_SIZE+NORMAL_DATA_SIZE);
		mSphereData = ByteBuffer.allocateDirect(sphereData.length * BYTES_PER_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer(); //generate buffer
		mSphereData.put(sphereData); //put the float[] into the buffer and set the position
	
		//changed original colors but left original colors, just commented out here
		//set up some example colors. Can add more as needed!
//		mColorRed = new float[] {0.8f, 0.1f, 0.1f, 1.0f};
//		mColorBlue = new float[] {0.1f, 0.1f, 0.8f, 1.0f};
//		mColorGrey = new float[] {0.8f, 0.8f, 0.8f, 1.0f};
		
		mColorRed = new float[] {0.314f, 0.0f, 0.0f, 1.0f};
		mColorBlue = new float[] {0.0f, 0.15f, 0.445f, 1.0f};
		mColorGrey = new float[] {0.15f, 0.15f, 0.15f, 1.0f};		
		mColorGreen = new float[] {0.0f, 0.2f, 0.0f, 1.0f};
		
		
		//axis
		float[] axisData = models.getCoordinateAxis();
		mAxisCount = axisData.length/POSITION_DATA_SIZE;
		mAxisBuffer = ByteBuffer.allocateDirect(axisData.length * BYTES_PER_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer(); //generate buffer
		mAxisBuffer.put(axisData); //put the float[] into the buffer and set the position
		
		

	}

	/**
	 * This method is called when the rendering surface is first created; more initializing stuff goes here.
	 * I put OpenGL initialization here (with more generic model initialization in the Renderer constructor).
	 * 
	 * Note that the GL10 parameter is unused; this parameter acts sort of like the Graphics2D context for
	 * doing GLES 1.0 operations. But we don't use that class for GLES 2.0+; but in order to keep Android 
	 * working and backwards compatible, the method has the same signature so the unused object is passed in.
	 */
	@Override
	public void onSurfaceCreated(GL10 unused, EGLConfig config) 
	{
		//flags to enable depth work
		GLES20.glEnable(GLES20.GL_CULL_FACE); //remove back faces
		GLES20.glEnable(GLES20.GL_DEPTH_TEST); //enable depth testing
		GLES20.glDepthFunc(GLES20.GL_LEQUAL);
		
		// Set the background clear color
		GLES20.glClearColor(0.2f, 0.2f, 0.2f, 1.0f); //Currently a dark grey so we can make sure things are working

		//This is a good place to compile the shaders from Strings into actual executables. We use a helper method for that
		int vertexShaderHandle = GLUtilities.compileShader(GLES20.GL_VERTEX_SHADER, perVertexShaderCode); //get pointers to the executables		
		int fragmentShaderHandle = GLUtilities.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
		mPerVertexProgramHandle = GLUtilities.createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle); //and then we throw them into a program

		//Get pointers to the shader's variables (for use elsewhere)
		mMVPMatrixHandle = GLES20.glGetUniformLocation(mPerVertexProgramHandle, "uMVPMatrix");
		mMVMatrixHandle = GLES20.glGetUniformLocation(mPerVertexProgramHandle, "uMVMatrix");
		mPositionHandle = GLES20.glGetAttribLocation(mPerVertexProgramHandle, "aPosition");
		mNormalHandle = GLES20.glGetAttribLocation(mPerVertexProgramHandle, "aNormal");
		mColorHandle = GLES20.glGetAttribLocation(mPerVertexProgramHandle, "aColor");
	}

	/**
	 * Called whenever the surface changes (i.e., size due to rotation). Put initialization stuff that
	 * depends on the size here!
	 */
	@Override
	public void onSurfaceChanged(GL10 unused, int width, int height) 
	{
		GLES20.glViewport(0, 0, width, height); // Set the OpenGL viewport (basically the canvas) to the same size as the surface.

		/**
		 * Set up the View and Projection matrixes. These matter more for when we're actually constructing
		 * 3D models, rather than 2D models in a 3D world.
		 */
		
		//Set View Matrix
		Matrix.setLookAtM(mViewMatrix, 0, 
				0.0f, 0.0f, 5.0f, //eye's location
				0.0f, 0.0f, -1.0f, //direction we're looking at
				0.0f, 1.0f, 0.0f //direction that is "up" from our head
				); //this gets compiled into the proper matrix automatically

		//Set Projection Matrix. We will talk about this more in the future
		final float ratio = (float) width / height; //aspect ratio
		final float left = -ratio;
		final float right = ratio;
		final float bottom = -1;
		final float top = 1;
		final float near = 1.0f;
		final float far = 50.0f;
		Matrix.frustumM(mProjectionMatrix, 0, left, right, bottom, top, near, far);
	}
	
	/**
	 * This method is for changing whether or not the robot is dancing.
	 */
	public void controlAnimation()
	{
		isDancing = !isDancing;//switch whether or not the robot is dancing
	}

	/**
	 * This is like our "onDraw" method; it says what to do each frame
	 */
	@Override
	public void onDrawFrame(GL10 unused) 
	{
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT); //start by clearing the screen for each frame

		GLES20.glUseProgram(mPerVertexProgramHandle); //tell OpenGL to use the shader program we've compiled
	
		/*
		 * THE ALGORITHM
		 * SAVE current reference frame (push my frame onto the stack)
		 * apply GLOBAL transformations we want children to INHERIT (animation, see below)
		 * RECURSE: draw each child component //this is slightly different from class--post-order traversal rather than pre-order
		 * apply LOCAL transformations we do NOT want children to inherit (local rotations, etc.)
		 * DRAW this model's primitives
		 * RESTORE my parent's frame (by popping my frame off the stack!)
		 */
		
		_time = SystemClock.uptimeMillis() % 10000L;

		shoulderAngle = (20.0f / 10000.0f) * ((int) _time);
		elbowAngle = (30.0f / 10000.0f) * ((int) _time);
		
		//I had to make different variables for right and left to get the timing right
		rightHipAngle = (180.0f / 10000.0f) * ((int) _time);
    	rightKneeAngle = -rightHipAngle;
    	leftHipAngle = Math.abs(90-rightHipAngle);
    	leftKneeAngle = -leftHipAngle;
		
		Matrix.setIdentityM(mTempMatrix, 0);//set the identity
		drawTorso();//start the recursive drawing process

		//drawAxis(); //so we have guides on coordinate axes, for debugging
	}				

	/**
	 * This method draws the torso. It acts like the rest of the drawRobotPart methods and acts recursively to draw each of the "children" then itself.
	 */
	public void drawTorso()
	{
		// Do a complete rotation every 10 seconds.
	
		float angleInDegrees = (360.0f / 10000.0f) * ((int) _time);
				
		float[] saved = new float[16];//save a copy of the parent to the stack so we can get to it later
		System.arraycopy(mTempMatrix, 0, saved, 0, 16);
		roboStack.push(saved);
		//Matrix.rotateM(mTempMatrix, 0, angleInDegrees, 0.0f, 1.0f, 0.0f);//UNCOMMENT THIS LINE TO WATCH THE ROBOT SPIN AROUND WHILE IT DANCES
		//RECURSE
		drawRightShoulder();
		drawLeftShoulder();
		drawHead();
		drawRightHip();
		drawLeftHip();
		Matrix.scaleM(mTempMatrix, 0, 1.0f, 1.0f, 0.5f);//local transformations
		drawPackedTriangleBuffer(mCubeData, mCubeVertexCount, mTempMatrix, mColorRed); //draw the triangle with the given model matrix
		mTempMatrix = roboStack.pop();//pop off of the stack
	}

	
	
	/**
	 * This method draws the head
	 */
	public void drawHead()
	{
		float[] saved = new float[16];
		System.arraycopy(mTempMatrix, 0, saved, 0, 16);
		roboStack.push(saved);
		Matrix.translateM(mTempMatrix, 0, 0.0f, 1.6f, 0.0f);
		Matrix.scaleM(mTempMatrix, 0, 0.8f, 0.8f, 0.8f);
		drawPackedTriangleBuffer(mSphereData, mSphereVertexCount, mTempMatrix, mColorGrey);
		mTempMatrix = roboStack.pop();
	}
	
	/**
	 * This method will draw the right shoulder and it's "child" robot parts.
	 */
	public void drawRightShoulder()
	{
		float[] saved = new float[16];
		System.arraycopy(mTempMatrix, 0, saved, 0, 16);
		roboStack.push(saved);
		Matrix.translateM(mTempMatrix, 0, -0.9f, 0.5f, 0.0f);
		if(isDancing)//only draw dancing animation if the robot should be dancing
		{
			Matrix.rotateM(mTempMatrix, 0, shoulderAngle, -1.0f, 0.0f, 0.0f);
		}
		drawRightUpperArm();
		Matrix.scaleM(mTempMatrix, 0, 0.5f, 0.5f, 0.5f);
		drawPackedTriangleBuffer(mSphereData, mSphereVertexCount, mTempMatrix, mColorGreen);
		mTempMatrix = roboStack.pop();
	}
	
	public void drawRightUpperArm()
	{
		float[] saved = new float[16];
		System.arraycopy(mTempMatrix, 0, saved, 0, 16);
		roboStack.push(saved);
		Matrix.translateM(mTempMatrix, 0, -0.4f, 0.0f, 1.0f);
		drawRightElbow();
		Matrix.scaleM(mTempMatrix, 0, 0.25f, 0.25f, 0.6f);
		Matrix.rotateM(mTempMatrix, 0, 30.0f, 0.0f, 0.0f, 1.0f);
		drawPackedTriangleBuffer(mCubeData, mCubeVertexCount, mTempMatrix, mColorGrey);
		mTempMatrix = roboStack.pop();
	}
	
	public void drawRightElbow()
	{
		float[] saved = new float[16];
		System.arraycopy(mTempMatrix, 0, saved, 0, 16);
		roboStack.push(saved);
		Matrix.translateM(mTempMatrix, 0, 0.0f, 0.0f, 0.6f);
		Matrix.rotateM(mTempMatrix, 0, 90.0f, 1.0f, 0.0f, 0.0f);
		if(isDancing)
		{
		if(_time<5000)
		{
			Matrix.rotateM(mTempMatrix, 0, elbowAngle, 0.0f, 1.0f, 0.0f);
		}
		else if(5000<_time && _time<10000)
		{
			Matrix.rotateM(mTempMatrix, 0, -elbowAngle, 0.0f, 1.0f, 0.0f);
		}
		}
		drawRightLowerArm();
		Matrix.scaleM(mTempMatrix, 0, 0.2f, 0.2f, 0.2f);
		drawPackedTriangleBuffer(mSphereData, mSphereVertexCount, mTempMatrix, mColorGreen);
		mTempMatrix = roboStack.pop();
	}
	public void drawRightLowerArm()
	{
		float[] saved = new float[16];
		System.arraycopy(mTempMatrix, 0, saved, 0, 16);
		roboStack.push(saved);
		Matrix.translateM(mTempMatrix, 0, 0.0f, 0.0f, 0.75f);
		drawRightHand();
		Matrix.scaleM(mTempMatrix, 0, 0.2f, 0.2f, 0.6f);
		drawPackedTriangleBuffer(mCubeData, mCubeVertexCount, mTempMatrix, mColorBlue);
		mTempMatrix = roboStack.pop();
		//END right lower arm
	}
	public void drawRightHand()
	{
		//START right hand
		float[] saved = new float[16];
		System.arraycopy(mTempMatrix, 0, saved, 0, 16);
		roboStack.push(saved);
		Matrix.translateM(mTempMatrix, 0, 0.0f, 0.0f, 0.6f);
		Matrix.scaleM(mTempMatrix, 0, 0.2f, 0.2f, 0.2f);
		drawPackedTriangleBuffer(mSphereData, mSphereVertexCount, mTempMatrix, mColorGreen);
		mTempMatrix = roboStack.pop();
		//END right hand
		
	}
	
	
	/**
	 * This method will draw the left shoulder and it's "child" robot parts.
	 */
	public void drawLeftShoulder()
	{
		//START left shoulder
		float[] saved = new float[16];
		System.arraycopy(mTempMatrix, 0, saved, 0, 16);
		roboStack.push(saved);
		Matrix.translateM(mTempMatrix, 0, 0.9f, 0.5f, 0.0f);
		if(isDancing)
		{
			Matrix.rotateM(mTempMatrix, 0, shoulderAngle, -1.0f, 0.0f, 0.0f);
		}
		drawLeftUpperArm();
		Matrix.scaleM(mTempMatrix, 0, 0.5f, 0.5f, 0.5f);
		drawPackedTriangleBuffer(mSphereData, mSphereVertexCount, mTempMatrix, mColorGreen);
		mTempMatrix = roboStack.pop();
		//END left shoulder
	}
	
	public void drawLeftUpperArm()
	{
		//START left upper arm
		float[] saved = new float[16];
		System.arraycopy(mTempMatrix, 0, saved, 0, 16);
		roboStack.push(saved);
		Matrix.translateM(mTempMatrix, 0, 0.4f, 0.0f, 1.0f);
		drawLeftElbow();
		Matrix.scaleM(mTempMatrix, 0, 0.25f, 0.25f, 0.6f);
		Matrix.rotateM(mTempMatrix, 0, -30.0f, 0.0f, 0.0f, 1.0f);
		drawPackedTriangleBuffer(mCubeData, mCubeVertexCount, mTempMatrix, mColorGrey);
		mTempMatrix = roboStack.pop();
		//END left upper arm
	}
	
	public void drawLeftElbow()
	{
		//START left elbow
		
		float[] saved = new float[16];
		System.arraycopy(mTempMatrix, 0, saved, 0, 16);
		roboStack.push(saved);
		Matrix.translateM(mTempMatrix, 0, 0.0f, 0.0f, 0.6f);
		Matrix.rotateM(mTempMatrix, 0, 90.0f, 1.0f, 0.0f, 0.0f);
		if(isDancing){
		if(_time<5000)
		{
			Matrix.rotateM(mTempMatrix, 0, elbowAngle, 0.0f, 1.0f, 0.0f);
		}
		else if(5000<_time && _time<10000)
		{
			Matrix.rotateM(mTempMatrix, 0, -elbowAngle, 0.0f, 1.0f, 0.0f);
		}
		}
		drawLeftLowerArm();
		Matrix.scaleM(mTempMatrix, 0, 0.2f, 0.2f, 0.2f);
		drawPackedTriangleBuffer(mSphereData, mSphereVertexCount, mTempMatrix, mColorGreen);
		mTempMatrix = roboStack.pop();
		//END left elbow
	}
	public void drawLeftLowerArm()
	{
		//START left lower arm
		float[] saved = new float[16];
		System.arraycopy(mTempMatrix, 0, saved, 0, 16);
		roboStack.push(saved);
		Matrix.translateM(mTempMatrix, 0, 0.0f, 0.0f, 0.75f);
		drawLeftHand();
		Matrix.scaleM(mTempMatrix, 0, 0.2f, 0.2f, 0.6f);
		drawPackedTriangleBuffer(mCubeData, mCubeVertexCount, mTempMatrix, mColorBlue);
		mTempMatrix = roboStack.pop();
		//END left lower arm
	}
	
	public void drawLeftHand()
	{
		//START left hand
		float[] saved = new float[16];
		System.arraycopy(mTempMatrix, 0, saved, 0, 16);
		roboStack.push(saved);
		Matrix.translateM(mTempMatrix, 0, 0.0f, 0.0f, 0.6f);
		Matrix.scaleM(mTempMatrix, 0, 0.2f, 0.2f, 0.2f);
		drawPackedTriangleBuffer(mSphereData, mSphereVertexCount, mTempMatrix, mColorGreen);
		mTempMatrix = roboStack.pop();
		//END left hand
	}
	
	public void drawRightHip()
	{
		float[] saved = new float[16];
		System.arraycopy(mTempMatrix, 0, saved, 0, 16);
		roboStack.push(saved);
		Matrix.translateM(mTempMatrix, 0, -0.4f, -1.2f, 0.0f);
		//Log.d("dance", "_time = "+_time);
		//Log.d("dance", "angle = "+hipAngle);
		if(isDancing){
		if(_time<2500)
		{
			Matrix.rotateM(mTempMatrix, 0, rightHipAngle, -1.0f, 0.0f, 0.0f);
		}
		else if(2500<_time && _time<=5000)
		{
			Matrix.rotateM(mTempMatrix, 0, -rightHipAngle, 1.0f, 0.0f, 0.0f);
		}
		}
		drawRightUpperLeg();
		Matrix.scaleM(mTempMatrix, 0, 0.3f, 0.3f, 0.3f);
		drawPackedTriangleBuffer(mSphereData, mSphereVertexCount, mTempMatrix, mColorGreen);
		mTempMatrix = roboStack.pop();
	}
	
	public void drawRightUpperLeg()
	{
		float[] saved = new float[16];
		System.arraycopy(mTempMatrix, 0, saved, 0, 16);
		roboStack.push(saved);
		Matrix.translateM(mTempMatrix, 0, 0.0f, -0.6f, 0.0f);
		drawRightKnee();
		Matrix.scaleM(mTempMatrix, 0, 0.25f, 0.6f, 0.25f);
		drawPackedTriangleBuffer(mCubeData, mCubeVertexCount, mTempMatrix, mColorGrey);
		mTempMatrix = roboStack.pop();
	}
	
	public void drawRightKnee()
	{
		float[] saved = new float[16];
		System.arraycopy(mTempMatrix, 0, saved, 0, 16);
		roboStack.push(saved);
		Matrix.translateM(mTempMatrix, 0, 0.0f, -0.6f, 0.0f);
		if(isDancing){
			if(_time<2500)
			{
				Matrix.rotateM(mTempMatrix, 0, rightKneeAngle, -1.0f, 0.0f, 0.0f);
			}
			else if(2500<_time && _time<=5000)
			{
				Matrix.rotateM(mTempMatrix, 0, -rightKneeAngle, 1.0f, 0.0f, 0.0f);
			}
		}
		drawRightLowerLeg();
		Matrix.scaleM(mTempMatrix, 0, 0.2f, 0.2f, 0.2f);
		drawPackedTriangleBuffer(mSphereData, mSphereVertexCount, mTempMatrix, mColorGreen);
		mTempMatrix = roboStack.pop();
	}
	
	public void drawRightLowerLeg()
	{
		float[] saved = new float[16];
		System.arraycopy(mTempMatrix, 0, saved, 0, 16);
		roboStack.push(saved);
		Matrix.translateM(mTempMatrix, 0, 0.0f, -0.7f, 0.0f);
		Matrix.scaleM(mTempMatrix, 0, 0.2f, 0.6f, 0.2f);
		drawPackedTriangleBuffer(mCubeData, mCubeVertexCount, mTempMatrix, mColorBlue);
		mTempMatrix = roboStack.pop();
	}
	
	

	public void drawLeftHip()
	{
		float[] saved = new float[16];
		System.arraycopy(mTempMatrix, 0, saved, 0, 16);
		roboStack.push(saved);
		Matrix.translateM(mTempMatrix, 0, 0.4f, -1.2f, 0.0f);
		if(isDancing){
		if(5000<_time && _time<7500)
		{
			Matrix.rotateM(mTempMatrix, 0, leftHipAngle, -1.0f, 0.0f, 0.0f);
		}
		else if(7500<_time && _time<=10000)
		{
			Matrix.rotateM(mTempMatrix, 0, -leftHipAngle, 1.0f, 0.0f, 0.0f);
		}
		}
		drawLeftUpperLeg();
		Matrix.scaleM(mTempMatrix, 0, 0.3f, 0.3f, 0.3f);
		drawPackedTriangleBuffer(mSphereData, mSphereVertexCount, mTempMatrix, mColorGreen);
		mTempMatrix = roboStack.pop();
	}
	
	public void drawLeftUpperLeg()
	{
		float[] saved = new float[16];
		System.arraycopy(mTempMatrix, 0, saved, 0, 16);
		roboStack.push(saved);
		Matrix.translateM(mTempMatrix, 0, 0.0f, -0.6f, 0.0f);
		drawLeftKnee();
		Matrix.scaleM(mTempMatrix, 0, 0.25f, 0.6f, 0.25f);
		drawPackedTriangleBuffer(mCubeData, mCubeVertexCount, mTempMatrix, mColorGrey);
		mTempMatrix = roboStack.pop();
	}
	
	public void drawLeftKnee()
	{
		float[] saved = new float[16];
		System.arraycopy(mTempMatrix, 0, saved, 0, 16);
		roboStack.push(saved);
		Matrix.translateM(mTempMatrix, 0, 0.0f, -0.6f, 0.0f);
		if(isDancing){
			if(5000<_time && _time<7500)
			{
				Matrix.rotateM(mTempMatrix, 0, leftKneeAngle, -1.0f, 0.0f, 0.0f);
			}
			else if(7500<_time && _time<=10000)
			{
				Matrix.rotateM(mTempMatrix, 0, -leftKneeAngle, 1.0f, 0.0f, 0.0f);
			}
		}
		drawLeftLowerLeg();
		Matrix.scaleM(mTempMatrix, 0, 0.2f, 0.2f, 0.2f);
		drawPackedTriangleBuffer(mSphereData, mSphereVertexCount, mTempMatrix, mColorGreen);
		mTempMatrix = roboStack.pop();
	}
	
	public void drawLeftLowerLeg()
	{
		float[] saved = new float[16];
		System.arraycopy(mTempMatrix, 0, saved, 0, 16);
		roboStack.push(saved);
		Matrix.translateM(mTempMatrix, 0, 0.0f, -0.7f, 0.0f);
		Matrix.scaleM(mTempMatrix, 0, 0.2f, 0.6f, 0.2f);
		drawPackedTriangleBuffer(mCubeData, mCubeVertexCount, mTempMatrix, mColorBlue);
		mTempMatrix = roboStack.pop();
	}
	
	
	
	/**
	 * Draws a triangle buffer with the given modelMatrix and single color. 
	 * Note the view matrix is defined per program.
	 */			
	private void drawPackedTriangleBuffer(FloatBuffer buffer, int vertexCount, float[] modelMatrix, float[] color)
	{		
		//Calculate MV and MVPMatrix. Note written as MVP, but really P*V*M
		Matrix.multiplyMM(mMVMatrix, 0, mViewMatrix, 0, modelMatrix, 0);  //"M * V"
		Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVMatrix, 0); //"MV * P"

		GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mMVMatrix, 0); //put combined matrixes in the shader variables
		GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);

		
		final int stride = (POSITION_DATA_SIZE + NORMAL_DATA_SIZE) * BYTES_PER_FLOAT; //how big of steps we take through the buffer
		
		buffer.position(0); //reset buffer start to 0 (where the position data starts)
		GLES20.glVertexAttribPointer(mPositionHandle, POSITION_DATA_SIZE, GLES20.GL_FLOAT, false, stride, buffer); //note the stride lets us step over the normal data!
		GLES20.glEnableVertexAttribArray(mPositionHandle);

		buffer.position(POSITION_DATA_SIZE); //shift pointer to where the normal data starts
		GLES20.glVertexAttribPointer(mNormalHandle, NORMAL_DATA_SIZE, GLES20.GL_FLOAT, false, stride, buffer); //note the stride lets us step over the position data!
		GLES20.glEnableVertexAttribArray(mNormalHandle);

		//put color data in the shader variable
		GLES20.glVertexAttrib4fv(mColorHandle, color, 0);

		//This the OpenGL command to draw the specified number of vertices (as triangles; that is, every 3 coordinates). 
		GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);
	}		

	
	//draws the coordinate axis (for debugging)
	private void drawAxis()
	{
		Matrix.setIdentityM(mModelMatrix, 0);
		Matrix.multiplyMM(mMVMatrix, 0, mModelMatrix, 0, mViewMatrix, 0);  //M * V
		Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVMatrix, 0); //P * MV 

		GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mMVMatrix, 0);
		GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);

		// Pass in the position information
		mAxisBuffer.position(0); //reset buffer start to 0 (just in case)
		GLES20.glVertexAttribPointer(mPositionHandle, POSITION_DATA_SIZE, GLES20.GL_FLOAT, false, 0, mAxisBuffer); 
		GLES20.glEnableVertexAttribArray(mPositionHandle);
		GLES20.glDisableVertexAttribArray(mNormalHandle); //turn off the buffer version of normals
		GLES20.glVertexAttrib3fv(mNormalHandle, lightNormal, 0); //pass particular normal (so points are bright)

		//GLES20.glDisableVertexAttribArray(mColorHandle); //just in case it was enabled earlier
		GLES20.glVertexAttrib4fv(mColorHandle, mColorGrey, 0); //put color in the shader variable
		
		GLES20.glDrawArrays(GLES20.GL_POINTS, 0, mAxisCount); //draw the axis (as points!)
	}

}